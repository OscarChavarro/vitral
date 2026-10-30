#ifndef __TCP__
#define __TCP__
#include "lib/vitralKernel/SpinLock.h"
#include "net/ByteOrder.h"
#include "net/Ipv4.h"
#include "net/Be16.h"
#include "net/Be32.h"
#include "net/Ipv4Header.h"
#include "net/TcpHeader.h"

class Tcp {
public:
    static constexpr unsigned MAX_SOCKETS = 64;
    static constexpr unsigned BACKLOG = 16;
    static constexpr size_t RX_BUFFER = 8192;
    static constexpr size_t TX_BUFFER = 16384;
    static constexpr uint32_t MAX_FLIGHT = 8192;
    static constexpr unsigned MAX_RETRIES = 8;
    static constexpr uint64_t TIME_WAIT_MS = 10000;

    static void init();
    static void timer();

    static int64_t listen(uint16_t port);
    static int64_t accept(int64_t listener);
    static int64_t receive(int64_t socket, void *buffer, size_t size);
    static int64_t send(int64_t socket, const void *buffer, size_t size);
    static int64_t close(int64_t socket);
    static int *eventWord(int64_t socket);
    static bool peer(int64_t socket, Ipv4Address &address, uint16_t &port);

    static unsigned connectionCount();

private:
    enum class State : uint8_t { Free, Listen, SynReceived, Established, FinWait1, FinWait2, CloseWait, Closing, LastAck,
                                 TimeWait, Closed };
    class Socket {
    public:
        State state;
        bool appClosed;
        bool finQueued;
        bool finSent;
        bool finReceived;
        int error;
        int event;
        NetDevice *device;
        Ipv4Address localAddress;
        Ipv4Address remoteAddress;
        uint16_t localPort;
        uint16_t remotePort;
        uint16_t mss;
        uint32_t iss;
        uint32_t irs;
        uint32_t sndUna;
        uint32_t sndNxt;
        uint32_t sndMax;
        uint32_t sndWnd;
        uint32_t rcvNxt;
        uint32_t finSeq;
        uint8_t *rx;
        uint8_t *tx;
        size_t rxHead;
        size_t rxCount;
        size_t txHead;
        size_t txCount;
        uint64_t retransmitAt;
        unsigned retries;
        uint64_t timeWaitUntil;
        int listener;
        int pending[BACKLOG];
        unsigned pendingCount;
    };

    static Socket sockets[MAX_SOCKETS];
    static SpinLock lock;
    static uint32_t isnCounter;

    static Socket *get(int64_t handle);
    static Socket *find(const Ipv4Address &local, uint16_t localPort, const Ipv4Address &remote, uint16_t remotePort);
    static Socket *listenerOn(uint16_t port);
    static int allocate();
    static void release(Socket &s);
    static void wake(Socket &s);
    static void input(NetDevice &device, PacketBuffer *packet, const Ipv4Header &ip);
    static void segment(Socket &s, const TcpHeader &tcp, const uint8_t *data, size_t length);
    static void newConnection(NetDevice &device, const Ipv4Header &ip, const TcpHeader &tcp, const uint8_t *options, size_t optionBytes);
    static void process(Socket &s);
    static void transmit(Socket &s);
    static void sendAck(Socket &s);
    static void sendSynAck(Socket &s);
    static void emit(Socket &s, uint32_t sequence, uint8_t flags, const uint8_t *data, size_t length);
    static void emitRaw(NetDevice &device, const Ipv4Address &source, const Ipv4Address &destination, uint16_t sourcePort,
                        uint16_t destinationPort, uint32_t sequence, uint32_t acknowledgment, uint8_t flags,
                        uint16_t window, const uint8_t *data, size_t length, bool mssOption, uint16_t mss);
    static void reset(NetDevice &device, const Ipv4Header &ip, const TcpHeader &tcp, size_t payload);
    static void armTimer(Socket &s);
    static void abort(Socket &s, int error);
    static void closed(Socket &s);
    static uint16_t advertisedWindow(const Socket &s);
    static bool before(uint32_t a, uint32_t b) { return (int32_t)(a - b) < 0; }
};

#endif

#ifndef __UART__
#define __UART__
#include <stddef.h>
#include "kernel/TrapFrame.h"
#include "lib/vitralKernel/SpinLock.h"

// COM1 at 115200 baud, 8N1, written by polling. initCom1() first checks that a 16450/16550 answers at the port (scratch
// register and loopback test), so a machine without COM1 never waits on it. A port that drains much more slowly than
// its baud rate (a QEMU serial console that cannot keep up, a stalled line) must not hold back the kernel either: when
// a write takes about three times as long as 115200 baud need, or one character waits WAIT_LIMIT polls, COM1 is
// released and ready() becomes false for good. Received bytes are kept by the IRQ 4 handler that enableReceive()
// installs; read() also drains the receiver itself, so input works even if that interrupt never comes.
class Uart {
  public:
    static constexpr unsigned BAUD = 115200;

    static bool initCom1();
    static bool ready() { return enabled; }
    // '\n' goes out as CR LF, as the kernel log needs.
    static void write(const char *text, size_t length);
    // Every byte as it is: for a terminal, whose line discipline already did the output processing.
    static void writeRaw(const char *text, size_t length);
    // For a kernel panic: does not take the lock, which the interrupted code may hold.
    static void writeUnlocked(const char *text, size_t length);
    // Received bytes; also after a slow line released the output.
    static bool enableReceive();
    static size_t read(char *buffer, size_t size);
    static bool readable();

  private:
    static constexpr unsigned COM1 = 0x3F8;
    static constexpr unsigned DATA = 0;
    static constexpr unsigned INTERRUPT_ENABLE = 1;
    static constexpr unsigned FIFO_CONTROL = 2;
    static constexpr unsigned LINE_CONTROL = 3;
    static constexpr unsigned MODEM_CONTROL = 4;
    static constexpr unsigned LINE_STATUS = 5;
    static constexpr unsigned INTERRUPT_IDENTIFICATION = 2;
    static constexpr unsigned SCRATCH = 7;
    static constexpr unsigned IRQ = 4;
    static constexpr unsigned RECEIVE_INTERRUPT = 0x01;
    static constexpr unsigned RECEIVE_SIZE = 1024;
    static constexpr unsigned DLAB = 0x80;
    static constexpr unsigned DATA_READY = 0x01;
    static constexpr unsigned TRANSMITTER_EMPTY = 0x20;
    static constexpr unsigned MODEM_LOOPBACK = 0x1E;
    static constexpr unsigned MODEM_NORMAL = 0x0B;
    static constexpr unsigned CLOCK = 115200;
    static constexpr unsigned DIVISOR = CLOCK / BAUD;
    static constexpr unsigned WAIT_LIMIT = 100000;

    // The time spent in a write is measured in rounds of PIT channel 2, the PC speaker's counter, which every PC has
    // and which works before the clock is calibrated and on CPUs without a time stamp counter. Mode 0 with the
    // largest count: its output (port 0x61 bit 5) rises after 65535 / 1193182 s, about 55 ms.
    static constexpr unsigned PIT_CHANNEL2 = 0x42;
    static constexpr unsigned PIT_COMMAND = 0x43;
    static constexpr unsigned SYSTEM_CONTROL = 0x61;
    static constexpr unsigned CHANNEL2_GATE = 0x01;
    static constexpr unsigned SPEAKER_DATA = 0x02;
    static constexpr unsigned CHANNEL2_OUTPUT = 0x20;
    static constexpr unsigned CHANNEL2_MODE0 = 0xB0;
    // 115200 baud send about 630 characters in a round; a write may last 2 rounds plus one per 200 characters.
    static constexpr unsigned SPARE_ROUNDS = 2;
    static constexpr unsigned CHARACTERS_PER_ROUND = 200;

    static bool enabled;
    static bool found;
    static bool receiving;
    static char received[RECEIVE_SIZE];
    static unsigned receiveHead;
    static unsigned receiveTail;
    static SpinLock receiveLock;
    static unsigned rounds;
    static unsigned allowedRounds;
    static SpinLock lock;

    static bool present();
    static void startRound();
    static bool waitForTransmitter();
    static void send(const char *text, size_t length, bool crlf);
    static void drainReceiver();
    static TrapFrame *onIrq(TrapFrame *frame);
};

#endif

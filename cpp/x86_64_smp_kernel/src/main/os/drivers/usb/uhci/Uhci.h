#ifndef __UHCI__
#define __UHCI__
#include <stdint.h>
#include "drivers/pci/Pci.h"
#include "drivers/usb/UsbHostController.h"
#include "lib/vitralKernel/SpinLock.h"

// A UHCI (USB 1.1) host controller. Every frame of its list points to one skeleton queue head followed by a queue
// head per interrupt pipe, each with a single transfer descriptor that poll() rearms, and then by the queue head of
// control transfers. Interrupt endpoints are therefore polled every frame, whatever their bInterval asks for: the
// bandwidth of a few keyboards and mice is negligible. The schedule lives in DMA memory below 4 GB.
class Uhci : public UsbHostController {
public:
    static Uhci *create(const Pci::Location &at, unsigned number, unsigned &irq);
    ~Uhci() override;

    const char *name() const override { return nameText; }
    unsigned portCount() const override { return ports; }
    UsbPortStatus portStatus(unsigned port) override;
    void clearConnectChange(unsigned port) override;
    bool resetPort(unsigned port) override;
    void disablePort(unsigned port) override;
    int controlTransfer(uint8_t address, UsbSpeed speed, unsigned maxPacket, const UsbSetupPacket &setup,
                        void *data) override;
    void *openInterruptPipe(uint8_t address, UsbSpeed speed, uint8_t endpoint, unsigned maxPacket,
                            UsbReportHandler handler, void *context) override;
    void closeInterruptPipe(void *pipe) override;
    bool poll() override;
    bool takeFault() override;

private:
    class Td {
    public:
        volatile uint32_t link;
        volatile uint32_t status;
        volatile uint32_t token;
        volatile uint32_t buffer;
        uint32_t reserved[4];
    };

    class Qh {
    public:
        volatile uint32_t link;
        volatile uint32_t element;
        uint32_t reserved[2];
    };

    class Pipe {
    public:
        bool used;
        bool dead;
        uint8_t address;
        uint8_t endpoint;
        uint8_t toggle;
        bool lowSpeed;
        unsigned maxPacket;
        unsigned errors;
        Qh *qh;
        Td *td;
        uint8_t *buffer;
        UsbReportHandler handler;
        void *context;
    };

    static constexpr uint16_t USBCMD = 0x00;
    static constexpr uint16_t USBSTS = 0x02;
    static constexpr uint16_t USBINTR = 0x04;
    static constexpr uint16_t FRNUM = 0x06;
    static constexpr uint16_t FRBASEADD = 0x08;
    static constexpr uint16_t SOFMOD = 0x0C;
    static constexpr uint16_t PORTSC = 0x10;
    static constexpr uint8_t PCI_LEGACY_SUPPORT = 0xC0;
    static constexpr uint16_t LEGACY_CLEAR = 0x8F00;
    static constexpr uint16_t LEGACY_PIRQ = 0x2000;

    static constexpr uint16_t COMMAND_RUN = 0x0001;
    static constexpr uint16_t COMMAND_RESET = 0x0002;
    static constexpr uint16_t COMMAND_CONFIGURED = 0x0040;
    static constexpr uint16_t COMMAND_MAX_PACKET_64 = 0x0080;
    static constexpr uint16_t STATUS_INTERRUPT = 0x0001;
    static constexpr uint16_t STATUS_ERROR = 0x0002;
    static constexpr uint16_t STATUS_SYSTEM_ERROR = 0x0008;
    static constexpr uint16_t STATUS_PROCESS_ERROR = 0x0010;
    static constexpr uint16_t STATUS_HALTED = 0x0020;
    static constexpr uint16_t INTERRUPT_TIMEOUT_CRC = 0x0001;
    static constexpr uint16_t INTERRUPT_ON_COMPLETE = 0x0004;

    static constexpr uint16_t PORT_CONNECTED = 0x0001;
    static constexpr uint16_t PORT_CONNECT_CHANGE = 0x0002;
    static constexpr uint16_t PORT_ENABLED = 0x0004;
    static constexpr uint16_t PORT_ENABLE_CHANGE = 0x0008;
    static constexpr uint16_t PORT_ALWAYS_ONE = 0x0080;
    static constexpr uint16_t PORT_LOW_SPEED = 0x0100;
    static constexpr uint16_t PORT_RESET = 0x0200;
    static constexpr uint16_t PORT_WRITABLE = 0x1244;

    static constexpr uint32_t LINK_TERMINATE = 1;
    static constexpr uint32_t LINK_QH = 2;
    static constexpr uint32_t LINK_DEPTH_FIRST = 4;
    static constexpr uint32_t TD_SHORT_PACKET = 1u << 29;
    static constexpr uint32_t TD_ERROR_LIMIT = 3u << 27;
    static constexpr uint32_t TD_LOW_SPEED = 1u << 26;
    static constexpr uint32_t TD_INTERRUPT = 1u << 24;
    static constexpr uint32_t TD_ACTIVE = 1u << 23;
    static constexpr uint32_t TD_ERRORS = 0x00760000;
    static constexpr uint8_t PID_SETUP = 0x2D;
    static constexpr uint8_t PID_IN = 0x69;
    static constexpr uint8_t PID_OUT = 0xE1;

    static constexpr unsigned PAGES = 3;
    static constexpr unsigned PAGE = 4096;
    static constexpr unsigned FRAMES = 1024;
    static constexpr unsigned CONTROL_TDS = 120;
    static constexpr unsigned DATA_SIZE = 1024;
    static constexpr unsigned PIPE_AREA = 1152;
    static constexpr unsigned PIPE_STRIDE = 128;
    static constexpr unsigned PIPE_BUFFER = 64;
    static constexpr unsigned MAX_PIPES = 16;
    static constexpr unsigned MAX_PORTS = 7;
    static constexpr unsigned CONTROL_TIMEOUT_MS = 1000;

    uint16_t io;
    unsigned ports;
    char nameText[12];
    uint8_t *memory;
    uint32_t *frameList;
    Qh *interruptSkeleton;
    Qh *controlQh;
    Td *controlTds;
    uint8_t *setupBuffer;
    uint8_t *dataBuffer;
    Pipe pipes[MAX_PIPES];
    SpinLock lock;
    volatile bool fault;

    Uhci(uint16_t ioBase, unsigned number);
    bool initialize();
    uint16_t portRegister(unsigned port) const { return (uint16_t)(io + PORTSC + (port - 1) * 2); }
    void arm(Pipe &pipe);
    static uint32_t physical(const volatile void *p) { return (uint32_t)(uintptr_t)p; }
    static uint32_t token(uint8_t pid, uint8_t address, uint8_t endpoint, unsigned toggle, unsigned length);
    static unsigned actualLength(uint32_t status) { return ((status & 0x7FF) + 1) & 0x7FF; }
    static unsigned expectedLength(uint32_t token) { return ((token >> 21) + 1) & 0x7FF; }
};

#endif

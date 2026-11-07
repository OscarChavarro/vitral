#ifndef __USB_HOST_CONTROLLER__
#define __USB_HOST_CONTROLLER__
#include <stddef.h>
#include <stdint.h>

enum class UsbSpeed : uint8_t { Low, Full, High };

// The 8 bytes of a SETUP stage (USB 2.0, 9.3).
class UsbSetupPacket {
public:
    uint8_t requestType;
    uint8_t request;
    uint16_t value;
    uint16_t index;
    uint16_t length;
} __attribute__((packed));

class UsbPortStatus {
public:
    bool connected;
    bool enabled;
    bool connectChanged;
    UsbSpeed speed;
};

// Receives each report of an interrupt IN endpoint, in interrupt context.
using UsbReportHandler = void (*)(void *context, const uint8_t *data, unsigned length);

// What the USB core needs from a host controller driver. Ports are numbered from 1. Control transfers are issued one
// at a time by the core (at boot, then from its monitor thread) and complete by polling; interrupt pipes are serviced
// by poll(), which the controller's interrupt and the clock tick call.
class UsbHostController {
public:
    static constexpr unsigned MAX_ADDRESS = 127;

    virtual ~UsbHostController() {}
    virtual const char *name() const = 0;
    virtual unsigned portCount() const = 0;
    virtual UsbPortStatus portStatus(unsigned port) = 0;
    virtual void clearConnectChange(unsigned port) = 0;
    virtual bool resetPort(unsigned port) = 0;
    virtual void disablePort(unsigned port) = 0;
    // Bytes moved in the data stage, or -1 when the transfer fails or times out.
    virtual int controlTransfer(uint8_t address, UsbSpeed speed, unsigned maxPacket, const UsbSetupPacket &setup,
                                void *data) = 0;
    // A handle for closeInterruptPipe(), or null when the controller has no room for another pipe.
    virtual void *openInterruptPipe(uint8_t address, UsbSpeed speed, uint8_t endpoint, unsigned maxPacket,
                                    UsbReportHandler handler, void *context) = 0;
    virtual void closeInterruptPipe(void *pipe) = 0;
    // Services finished transfers; true when the controller had something to report (its interrupt was pending).
    virtual bool poll() = 0;
    // True once after the controller stopped on a host system or process error.
    virtual bool takeFault() = 0;

    unsigned number() const { return numberValue; }
    void setNumber(unsigned n) { numberValue = n; }
    uint8_t allocateAddress();
    void releaseAddress(uint8_t address);

protected:
    UsbHostController() : numberValue(0), addresses{1, 0, 0, 0} {}

private:
    unsigned numberValue;
    uint32_t addresses[4];
};

#endif

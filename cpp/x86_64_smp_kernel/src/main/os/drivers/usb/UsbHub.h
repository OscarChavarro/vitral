#ifndef __USB_HUB__
#define __USB_HUB__
#include <stdint.h>
#include "drivers/usb/UsbDevice.h"

// An external hub. Its ports are powered when it is attached and then polled with GET_STATUS by scan(), at boot and
// from the core's monitor thread, instead of through its status change endpoint.
class UsbHub : public UsbInterfaceDriver {
public:
    static UsbHub *attach(UsbDevice &device);
    ~UsbHub() override;
    const char *kind() const override { return "hub"; }
    void scan();

private:
    static constexpr unsigned MAX_PORTS = 15;
    static constexpr uint8_t TYPE_CLASS_DEVICE = 0xA0;
    static constexpr uint8_t TYPE_CLASS_OTHER_IN = 0xA3;
    static constexpr uint8_t TYPE_CLASS_OTHER_OUT = 0x23;
    static constexpr uint8_t DESCRIPTOR_HUB = 0x29;
    static constexpr uint16_t FEATURE_PORT_RESET = 4;
    static constexpr uint16_t FEATURE_PORT_POWER = 8;
    static constexpr uint16_t FEATURE_C_PORT_CONNECTION = 16;
    static constexpr uint16_t FEATURE_C_PORT_ENABLE = 17;
    static constexpr uint16_t FEATURE_C_PORT_RESET = 20;
    static constexpr uint16_t STATUS_CONNECTION = 0x0001;
    static constexpr uint16_t STATUS_ENABLE = 0x0002;
    static constexpr uint16_t STATUS_RESET = 0x0010;
    static constexpr uint16_t STATUS_LOW_SPEED = 0x0200;
    static constexpr uint16_t STATUS_HIGH_SPEED = 0x0400;
    static constexpr uint16_t CHANGE_CONNECTION = 0x0001;
    static constexpr uint16_t CHANGE_ENABLE = 0x0002;
    static constexpr uint16_t CHANGE_RESET = 0x0010;

    UsbDevice &device;
    unsigned ports;
    unsigned powerDelayMs;
    bool connected[MAX_PORTS + 1];

    UsbHub(UsbDevice &hubDevice, unsigned portCount, unsigned powerDelay);
    bool portStatus(unsigned port, uint16_t &status, uint16_t &change);
    bool setFeature(unsigned port, uint16_t feature);
    bool clearFeature(unsigned port, uint16_t feature);
    bool resetPort(unsigned port, UsbSpeed &speed);
};

#endif

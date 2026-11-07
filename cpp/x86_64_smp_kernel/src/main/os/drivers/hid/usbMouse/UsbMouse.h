#ifndef __USB_MOUSE__
#define __USB_MOUSE__
#include <stdint.h>
#include "drivers/usb/UsbDevice.h"

// A HID mouse in boot protocol: buttons, x and y, and the wheel when the report has a fourth byte. Its motion goes to
// MouseDevice like the PS/2 mouse's.
class UsbMouse : public UsbInterfaceDriver {
public:
    static UsbMouse *attach(UsbDevice &device, uint8_t interface, uint8_t endpoint, unsigned maxPacket);
    ~UsbMouse() override;
    const char *kind() const override { return "mouse"; }

private:
    static constexpr uint8_t REQUEST_SET_IDLE = 0x0A;
    static constexpr uint8_t REQUEST_SET_PROTOCOL = 0x0B;

    UsbDevice &device;
    void *pipe;

    explicit UsbMouse(UsbDevice &mouseDevice) : device(mouseDevice), pipe(nullptr) {}
    static void onReport(void *context, const uint8_t *data, unsigned length);
};

#endif

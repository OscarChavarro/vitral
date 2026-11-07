#ifndef __USB_KEYBOARD__
#define __USB_KEYBOARD__
#include <stdint.h>
#include "drivers/usb/UsbDevice.h"
#include "lib/vitralKernel/SpinLock.h"

// A HID keyboard in boot protocol. Each 8-byte report (modifiers, reserved, six key usages) is compared with the
// previous one and the differences become PS/2 set 1 scancodes for Keyboard, so both keyboards behave the same.
// USB keyboards do not repeat: a held key is repeated here from the clock tick.
class UsbKeyboard : public UsbInterfaceDriver {
public:
    static UsbKeyboard *attach(UsbDevice &device, uint8_t interface, uint8_t endpoint, unsigned maxPacket);
    static void repeatTick();
    ~UsbKeyboard() override;
    const char *kind() const override { return "keyboard"; }

private:
    static constexpr unsigned MAX_KEYBOARDS = 4;
    static constexpr unsigned REPORT = 8;
    static constexpr unsigned REPEAT_DELAY_TICKS = 9;
    static constexpr uint8_t REQUEST_SET_IDLE = 0x0A;
    static constexpr uint8_t REQUEST_SET_PROTOCOL = 0x0B;
    static constexpr uint8_t ROLL_OVER = 0x01;

    static UsbKeyboard *instances[MAX_KEYBOARDS];
    static SpinLock instancesLock;

    UsbDevice &device;
    void *pipe;
    uint8_t previous[REPORT];
    uint16_t repeatCode;
    uint64_t repeatAt;

    explicit UsbKeyboard(UsbDevice &keyboardDevice);
    static void onReport(void *context, const uint8_t *data, unsigned length);
    static uint16_t scancode(uint8_t usage);
    static void send(uint16_t code, bool release);
    void handle(const uint8_t *report);
};

#endif

#include "drivers/hid/usbKeyboard/UsbKeyboard.h"
#include "drivers/keyboard/Keyboard.h"
#include "drivers/usb/Usb.h"
#include "kernel/Clock.h"
#include "kernel/KernelLog.h"
#include "lib/vitralKernel/LockGuard.h"

UsbKeyboard *UsbKeyboard::instances[UsbKeyboard::MAX_KEYBOARDS];
SpinLock UsbKeyboard::instancesLock;

static constexpr uint16_t E0 = 0xE000;

// PS/2 set 1 make codes of the HID keyboard usages 0x00 to 0x65; E0 marks the extended ones, 0 the unmapped ones.
static const uint16_t USB_KEYBOARD_SCANCODES[0x66] = {
    0, 0, 0, 0, 0x1E, 0x30, 0x2E, 0x20, 0x12, 0x21, 0x22, 0x23, 0x17, 0x24, 0x25, 0x26,
    0x32, 0x31, 0x18, 0x19, 0x10, 0x13, 0x1F, 0x14, 0x16, 0x2F, 0x11, 0x2D, 0x15, 0x2C, 0x02, 0x03,
    0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x1C, 0x01, 0x0E, 0x0F, 0x39, 0x0C, 0x0D, 0x1A,
    0x1B, 0x2B, 0x2B, 0x27, 0x28, 0x29, 0x33, 0x34, 0x35, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, 0x40,
    0x41, 0x42, 0x43, 0x44, 0x57, 0x58, E0 | 0x37, 0x46, 0, E0 | 0x52, E0 | 0x47, E0 | 0x49, E0 | 0x53,
    E0 | 0x4F, E0 | 0x51, E0 | 0x4D, E0 | 0x4B, E0 | 0x50, E0 | 0x48, 0x45, E0 | 0x35, 0x37, 0x4A, 0x4E,
    E0 | 0x1C, 0x4F, 0x50, 0x51, 0x4B, 0x4C, 0x4D, 0x47, 0x48, 0x49, 0x52, 0x53, 0x56, E0 | 0x5D,
};

// Left control, shift, alt and GUI, then the right ones: bits 0 to 7 of the modifier byte.
static const uint16_t USB_KEYBOARD_MODIFIERS[8] = {
    0x1D, 0x2A, 0x38, E0 | 0x5B, E0 | 0x1D, 0x36, E0 | 0x38, E0 | 0x5C,
};

UsbKeyboard::UsbKeyboard(UsbDevice &keyboardDevice)
    : device(keyboardDevice), pipe(nullptr), repeatCode(0), repeatAt(0) {
    for (uint8_t &b : previous) {
        b = 0;
    }
}

UsbKeyboard *
UsbKeyboard::attach(UsbDevice &device, uint8_t interface, uint8_t endpoint, unsigned maxPacket) {
    device.control(0x21, REQUEST_SET_PROTOCOL, 0, interface, 0, nullptr);
    device.control(0x21, REQUEST_SET_IDLE, 0, interface, 0, nullptr);
    UsbKeyboard *keyboard = new UsbKeyboard(device);
    {
        LockGuard guard(instancesLock);
        for (UsbKeyboard *&slot : instances) {
            if (!slot) {
                slot = keyboard;
                break;
            }
        }
    }
    keyboard->pipe = device.controller->openInterruptPipe(device.address, device.speed, endpoint, maxPacket, onReport,
                                                          keyboard);
    if (!keyboard->pipe) {
        kprintf("USB keyboard: no room for another interrupt pipe on %s\n", device.controller->name());
        delete keyboard;
        return nullptr;
    }
    kprintf("USB keyboard: \"%s\" (interface %u, endpoint %u), boot protocol\n", device.name, (uint32_t)interface,
            (uint32_t)endpoint);
    return keyboard;
}

UsbKeyboard::~UsbKeyboard() {
    if (pipe) {
        device.controller->closeInterruptPipe(pipe);
    }
    LockGuard guard(instancesLock);
    for (UsbKeyboard *&slot : instances) {
        if (slot == this) {
            slot = nullptr;
        }
    }
    for (unsigned bit = 0; bit < 8; bit++) {
        if (previous[0] & (1u << bit)) {
            send(USB_KEYBOARD_MODIFIERS[bit], true);
        }
    }
}

uint16_t
UsbKeyboard::scancode(uint8_t usage) {
    unsigned count = sizeof(USB_KEYBOARD_SCANCODES) / sizeof(USB_KEYBOARD_SCANCODES[0]);
    return usage < count ? USB_KEYBOARD_SCANCODES[usage] : 0;
}

void
UsbKeyboard::send(uint16_t code, bool release) {
    if (code & E0) {
        Keyboard::injectScancode(0xE0);
    }
    Keyboard::injectScancode((uint8_t)((code & 0x7F) | (release ? 0x80 : 0)));
}

void
UsbKeyboard::onReport(void *context, const uint8_t *data, unsigned length) {
    if (length >= 3) {
        uint8_t report[REPORT] = {0, 0, 0, 0, 0, 0, 0, 0};
        for (unsigned i = 0; i < length && i < REPORT; i++) {
            report[i] = data[i];
        }
        ((UsbKeyboard *)context)->handle(report);
    }
}

static bool
usbKeyboardContains(const uint8_t *report, uint8_t usage) {
    for (unsigned i = 2; i < 8; i++) {
        if (report[i] == usage) {
            return true;
        }
    }
    return false;
}

void
UsbKeyboard::handle(const uint8_t *report) {
    if (report[2] == ROLL_OVER) {
        return;
    }
    uint8_t changed = (uint8_t)(report[0] ^ previous[0]);
    for (unsigned bit = 0; bit < 8; bit++) {
        if (changed & (1u << bit)) {
            send(USB_KEYBOARD_MODIFIERS[bit], !(report[0] & (1u << bit)));
        }
    }
    for (unsigned i = 2; i < REPORT; i++) {
        uint8_t usage = previous[i];
        if (usage > ROLL_OVER && !usbKeyboardContains(report, usage) && scancode(usage)) {
            send(scancode(usage), true);
            if (repeatCode == scancode(usage)) {
                repeatCode = 0;
            }
        }
    }
    for (unsigned i = 2; i < REPORT; i++) {
        uint8_t usage = report[i];
        if (usage > ROLL_OVER && !usbKeyboardContains(previous, usage) && scancode(usage)) {
            send(scancode(usage), false);
            repeatCode = scancode(usage);
            repeatAt = Clock::ticks() + REPEAT_DELAY_TICKS;
        }
    }
    for (unsigned i = 0; i < REPORT; i++) {
        previous[i] = report[i];
    }
}

void
UsbKeyboard::repeatTick() {
    LockGuard guard(instancesLock);
    uint64_t now = Clock::ticks();
    for (UsbKeyboard *keyboard : instances) {
        if (keyboard && keyboard->repeatCode && now >= keyboard->repeatAt) {
            send(keyboard->repeatCode, false);
            keyboard->repeatAt = now + 1;
        }
    }
}

#include "drivers/hid/usbMouse/UsbMouse.h"
#include "drivers/hid/MouseDevice.h"
#include "kernel/KernelLog.h"

UsbMouse *
UsbMouse::attach(UsbDevice &device, uint8_t interface, uint8_t endpoint, unsigned maxPacket) {
    device.control(0x21, REQUEST_SET_PROTOCOL, 0, interface, 0, nullptr);
    device.control(0x21, REQUEST_SET_IDLE, 0, interface, 0, nullptr);
    UsbMouse *mouse = new UsbMouse(device);
    mouse->pipe = device.controller->openInterruptPipe(device.address, device.speed, endpoint, maxPacket, onReport,
                                                       mouse);
    if (!mouse->pipe) {
        kprintf("USB mouse: no room for another interrupt pipe on %s\n", device.controller->name());
        delete mouse;
        return nullptr;
    }
    kprintf("USB mouse: \"%s\" (interface %u, endpoint %u), boot protocol, /dev/input/mice\n", device.name,
            (uint32_t)interface, (uint32_t)endpoint);
    return mouse;
}

UsbMouse::~UsbMouse() {
    if (pipe) {
        device.controller->closeInterruptPipe(pipe);
    }
}

// HID counts y and the wheel downwards and upwards; MouseDevice takes y downwards and the wheel as the PS/2 mouse
// reports it, positive towards the user.
void
UsbMouse::onReport(void *, const uint8_t *data, unsigned length) {
    if (length < 3) {
        return;
    }
    uint8_t buttons = data[0] & (MouseDevice::LEFT | MouseDevice::RIGHT | MouseDevice::MIDDLE);
    if (data[0] & 0x08) {
        buttons |= MouseDevice::FOURTH;
    }
    if (data[0] & 0x10) {
        buttons |= MouseDevice::FIFTH;
    }
    int wheel = length >= 4 ? -(int8_t)data[3] : 0;
    MouseDevice::report((int8_t)data[1], (int8_t)data[2], wheel, buttons);
}

#include "drivers/usb/UsbHub.h"
#include "drivers/usb/Usb.h"
#include "kernel/KernelLog.h"

UsbHub::UsbHub(UsbDevice &hubDevice, unsigned portCount, unsigned powerDelay)
    : device(hubDevice), ports(portCount), powerDelayMs(powerDelay) {
    for (bool &c : connected) {
        c = false;
    }
}

UsbHub *
UsbHub::attach(UsbDevice &device) {
    uint8_t descriptor[9];
    if (device.control(TYPE_CLASS_DEVICE, Usb::REQUEST_GET_DESCRIPTOR, DESCRIPTOR_HUB << 8, 0, sizeof(descriptor),
                       descriptor) < 7) {
        kprintf("USB hub: \"%s\" has no hub descriptor\n", device.name);
        return nullptr;
    }
    unsigned ports = descriptor[2] > MAX_PORTS ? MAX_PORTS : descriptor[2];
    unsigned powerDelay = descriptor[5] * 2u;
    UsbHub *hub = new UsbHub(device, ports, powerDelay < 100 ? 100 : powerDelay);
    for (unsigned port = 1; port <= ports; port++) {
        hub->setFeature(port, FEATURE_PORT_POWER);
    }
    Usb::delayMs(hub->powerDelayMs);
    kprintf("USB hub: %u ports at address %u\n", ports, (uint32_t)device.address);
    return hub;
}

UsbHub::~UsbHub() {
    for (unsigned port = 1; port <= ports; port++) {
        if (connected[port]) {
            Usb::detach(*device.controller, &device, port);
        }
    }
}

bool
UsbHub::portStatus(unsigned port, uint16_t &status, uint16_t &change) {
    uint8_t answer[4];
    if (device.control(TYPE_CLASS_OTHER_IN, Usb::REQUEST_GET_STATUS, 0, (uint16_t)port, 4, answer) < 4) {
        return false;
    }
    status = (uint16_t)(answer[0] | answer[1] << 8);
    change = (uint16_t)(answer[2] | answer[3] << 8);
    return true;
}

bool
UsbHub::setFeature(unsigned port, uint16_t feature) {
    return device.control(TYPE_CLASS_OTHER_OUT, Usb::REQUEST_SET_FEATURE, feature, (uint16_t)port, 0, nullptr) >= 0;
}

bool
UsbHub::clearFeature(unsigned port, uint16_t feature) {
    return device.control(TYPE_CLASS_OTHER_OUT, Usb::REQUEST_CLEAR_FEATURE, feature, (uint16_t)port, 0, nullptr) >= 0;
}

bool
UsbHub::resetPort(unsigned port, UsbSpeed &speed) {
    if (!setFeature(port, FEATURE_PORT_RESET)) {
        return false;
    }
    uint16_t status = 0;
    uint16_t change = 0;
    for (unsigned waited = 0; waited < 500; waited += 10) {
        Usb::delayMs(10);
        if (portStatus(port, status, change) && !(status & STATUS_RESET) && (change & CHANGE_RESET)) {
            break;
        }
    }
    clearFeature(port, FEATURE_C_PORT_RESET);
    if (!(status & STATUS_ENABLE)) {
        return false;
    }
    speed = (status & STATUS_LOW_SPEED) ? UsbSpeed::Low : (status & STATUS_HIGH_SPEED) ? UsbSpeed::High
                                                                                          : UsbSpeed::Full;
    Usb::delayMs(10);
    return true;
}

void
UsbHub::scan() {
    for (unsigned port = 1; port <= ports; port++) {
        uint16_t status;
        uint16_t change;
        if (!portStatus(port, status, change)) {
            continue;
        }
        if (change & CHANGE_CONNECTION) {
            clearFeature(port, FEATURE_C_PORT_CONNECTION);
        }
        if (change & CHANGE_ENABLE) {
            clearFeature(port, FEATURE_C_PORT_ENABLE);
        }
        bool present = status & STATUS_CONNECTION;
        if (connected[port] && (!present || (change & CHANGE_CONNECTION))) {
            Usb::detach(*device.controller, &device, port);
            connected[port] = false;
        }
        if (present && !connected[port]) {
            connected[port] = true;
            Usb::delayMs(100);
            UsbSpeed speed = UsbSpeed::Full;
            if (!resetPort(port, speed)) {
                kprintf("USB hub %u: port %u does not enable after reset\n", (uint32_t)device.address, port);
                continue;
            }
            if (speed == UsbSpeed::High) {
                kprintf("USB hub %u: port %u has a high speed device, not supported\n", (uint32_t)device.address,
                        port);
                continue;
            }
            Usb::attach(*device.controller, &device, port, speed);
        }
    }
}

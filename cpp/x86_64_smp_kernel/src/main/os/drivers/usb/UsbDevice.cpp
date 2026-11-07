#include "drivers/usb/UsbDevice.h"

int
UsbDevice::control(uint8_t requestType, uint8_t request, uint16_t value, uint16_t index, uint16_t length,
                   void *data) {
    UsbSetupPacket setup = {requestType, request, value, index, length};
    return controller->controlTransfer(address, speed, maxPacket0, setup, data);
}

const char *
UsbDevice::speedName() const {
    return speed == UsbSpeed::Low ? "low" : speed == UsbSpeed::High ? "high" : "full";
}

uint8_t
UsbHostController::allocateAddress() {
    for (unsigned address = 1; address <= MAX_ADDRESS; address++) {
        if (!(addresses[address / 32] & (1u << (address % 32)))) {
            addresses[address / 32] |= 1u << (address % 32);
            return (uint8_t)address;
        }
    }
    return 0;
}

void
UsbHostController::releaseAddress(uint8_t address) {
    if (address > 0 && address <= MAX_ADDRESS) {
        addresses[address / 32] &= ~(1u << (address % 32));
    }
}

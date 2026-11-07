#ifndef __USB_DEVICE__
#define __USB_DEVICE__
#include <stdint.h>
#include "drivers/usb/UsbHostController.h"

// A class driver bound to one interface (or to the whole device, for a hub). The core deletes it when the device goes.
class UsbInterfaceDriver {
public:
    virtual ~UsbInterfaceDriver() {}
    virtual const char *kind() const = 0;
};

class UsbHub;

// One enumerated device: where it is attached, its address and what the core bound to it.
class UsbDevice {
public:
    static constexpr unsigned MAX_DRIVERS = 4;
    static constexpr unsigned NAME_SIZE = 48;

    UsbHostController *controller;
    UsbDevice *parent;
    uint8_t port;
    uint8_t address;
    UsbSpeed speed;
    uint8_t maxPacket0;
    uint16_t vendor;
    uint16_t product;
    uint8_t deviceClass;
    bool inUse;
    UsbHub *hub;
    UsbInterfaceDriver *drivers[MAX_DRIVERS];
    char name[NAME_SIZE];

    // Returns the bytes moved in the data stage, or -1.
    int control(uint8_t requestType, uint8_t request, uint16_t value, uint16_t index, uint16_t length, void *data);
    const char *speedName() const;
};

#endif

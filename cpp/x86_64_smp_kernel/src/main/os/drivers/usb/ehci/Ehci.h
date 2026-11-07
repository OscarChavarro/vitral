#ifndef __EHCI__
#define __EHCI__
#include <stdint.h>
#include "drivers/pci/Pci.h"

// There is no EHCI driver: release() takes the controller from the BIOS (which stops its legacy keyboard emulation),
// halts and resets it and clears CONFIGFLAG, so that every port belongs to a companion UHCI or OHCI controller. Full
// and low speed devices then work there, also behind a USB 2.0 hub, whose transaction translators are not needed.
class Ehci {
public:
    static void release(const Pci::Location &at);

private:
    static constexpr uint32_t LEGACY_SUPPORT_ID = 1;
    static constexpr uint32_t BIOS_OWNED = 1u << 16;
    static constexpr uint32_t OS_OWNED = 1u << 24;
    static constexpr unsigned USBCMD = 0x00;
    static constexpr unsigned USBSTS = 0x04;
    static constexpr unsigned USBINTR = 0x08;
    static constexpr unsigned CONFIGFLAG = 0x40;
    static constexpr unsigned PORTSC = 0x44;
    static constexpr uint32_t COMMAND_RUN = 1u << 0;
    static constexpr uint32_t COMMAND_RESET = 1u << 1;
    static constexpr uint32_t STATUS_HALTED = 1u << 12;
    static constexpr uint32_t PORT_CHANGE_BITS = (1u << 1) | (1u << 3) | (1u << 5);
    static constexpr uint32_t PORT_POWER = 1u << 12;
    static constexpr uint32_t PORT_OWNER = 1u << 13;

    static void takeFromBios(const Pci::Location &at, uint8_t capability);
};

#endif

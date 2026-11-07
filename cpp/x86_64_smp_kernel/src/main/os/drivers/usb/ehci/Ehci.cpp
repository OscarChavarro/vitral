#include "drivers/usb/ehci/Ehci.h"
#include "drivers/usb/Usb.h"
#include "kernel/KernelLog.h"
#include "kernel/Paging.h"

static uint32_t
readEhciRegister(volatile uint8_t *base, unsigned offset) {
    return *(volatile uint32_t *)(base + offset);
}

static void
writeEhciRegister(volatile uint8_t *base, unsigned offset, uint32_t value) {
    *(volatile uint32_t *)(base + offset) = value;
}

void
Ehci::takeFromBios(const Pci::Location &at, uint8_t capability) {
    uint32_t legacy = Pci::read32(at, capability);
    if (legacy & BIOS_OWNED) {
        Pci::write32(at, capability, legacy | OS_OWNED);
        for (unsigned waited = 0; waited < 1000 && (Pci::read32(at, capability) & BIOS_OWNED); waited += 10) {
            Usb::delayMs(10);
        }
        if (Pci::read32(at, capability) & BIOS_OWNED) {
            kprintf("USB: the BIOS does not release the EHCI controller, taking it anyway\n");
            Pci::write32(at, capability, (Pci::read32(at, capability) & ~BIOS_OWNED) | OS_OWNED);
        }
    }
    // USBLEGCTLSTS: no SMI sources, and the status bits that are set are cleared.
    Pci::write32(at, (uint8_t)(capability + 4), 0);
    Pci::write32(at, (uint8_t)(capability + 4), 0xE03F0000u);
}

void
Ehci::release(const Pci::Location &at) {
    uint64_t address;
    uint64_t size;
    if (!Pci::memoryBar(at, 0, address, size) || address + size > Paging::mappedBytes()) {
        kprintf("USB: EHCI controller at PCI %u:%u.%u has no usable registers\n", (uint32_t)at.bus,
                (uint32_t)at.device, (uint32_t)at.function);
        return;
    }
    uint16_t command = Pci::read16(at, Pci::COMMAND);
    Pci::write16(at, Pci::COMMAND, (uint16_t)(command | Pci::COMMAND_MEMORY));
    volatile uint8_t *capabilities = (volatile uint8_t *)(uintptr_t)address;
    uint32_t structural = readEhciRegister(capabilities, 4);
    uint32_t parameters = readEhciRegister(capabilities, 8);
    uint8_t capability = (uint8_t)(parameters >> 8);
    for (unsigned count = 0; capability >= 0x40 && count < 16; count++) {
        if ((Pci::read32(at, capability) & 0xFF) == LEGACY_SUPPORT_ID) {
            takeFromBios(at, capability);
        }
        capability = Pci::read8(at, (uint8_t)(capability + 1));
    }

    volatile uint8_t *operational = capabilities + capabilities[0];
    writeEhciRegister(operational, USBINTR, 0);
    writeEhciRegister(operational, USBCMD, readEhciRegister(operational, USBCMD) & ~COMMAND_RUN);
    for (unsigned waited = 0; waited < 20 && !(readEhciRegister(operational, USBSTS) & STATUS_HALTED); waited++) {
        Usb::delayMs(1);
    }
    writeEhciRegister(operational, USBCMD, COMMAND_RESET);
    for (unsigned waited = 0; waited < 250 && (readEhciRegister(operational, USBCMD) & COMMAND_RESET); waited++) {
        Usb::delayMs(1);
    }
    writeEhciRegister(operational, USBINTR, 0);
    writeEhciRegister(operational, CONFIGFLAG, 0);
    unsigned ports = structural & 0x0F;
    bool powerSwitching = structural & (1u << 4);
    for (unsigned port = 0; port < ports; port++) {
        uint32_t status = readEhciRegister(operational, PORTSC + port * 4) & ~PORT_CHANGE_BITS;
        writeEhciRegister(operational, PORTSC + port * 4, status | PORT_OWNER | (powerSwitching ? PORT_POWER : 0));
    }
    kprintf("USB: EHCI controller at PCI %u:%u.%u released, its %u ports routed to the USB 1.1 controllers\n",
            (uint32_t)at.bus, (uint32_t)at.device, (uint32_t)at.function, ports);
}

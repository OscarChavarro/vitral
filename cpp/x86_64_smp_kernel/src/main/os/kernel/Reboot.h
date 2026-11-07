#ifndef __REBOOT__
#define __REBOOT__
#include <stdint.h>

// Restarts the machine. The root filesystem lives in memory, so there is nothing to write back first.
class Reboot {
public:
    // Tries, in order: the ACPI reset register, the keyboard controller reset line, the PCI reset port 0xCF9 and a
    // triple fault, which every x86 CPU turns into a reset.
    [[noreturn]] static void restart();

private:
    static constexpr uint16_t KBC_STATUS = 0x64;
    static constexpr uint8_t KBC_INPUT_FULL = 0x02;
    static constexpr uint8_t KBC_PULSE_RESET = 0xFE;
    static constexpr uint16_t PCI_RESET = 0xCF9;
    static constexpr uint8_t PCI_RESET_SYSTEM = 0x02;
    static constexpr uint8_t PCI_RESET_CPU = 0x04;
    static constexpr unsigned SETTLE_US = 50000;

    // Port 0x80 writes take about a microsecond each, without depending on a timer.
    static void delay(unsigned microseconds);
    static void keyboardController();
    static void pciResetPort();
    [[noreturn]] static void tripleFault();
};

#endif

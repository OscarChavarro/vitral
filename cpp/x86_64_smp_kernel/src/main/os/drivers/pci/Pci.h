#ifndef __PCI__
#define __PCI__
#include <stdint.h>

class Pci {
public:
    class Location {
    public:
        uint8_t bus;
        uint8_t device;
        uint8_t function;
    };

    static constexpr uint8_t VENDOR_ID = 0x00;
    static constexpr uint8_t DEVICE_ID = 0x02;
    static constexpr uint8_t COMMAND = 0x04;
    static constexpr uint8_t STATUS = 0x06;
    static constexpr uint8_t CLASS_CODE = 0x0B;
    static constexpr uint8_t HEADER_TYPE = 0x0E;
    static constexpr uint8_t BAR0 = 0x10;
    static constexpr uint8_t INTERRUPT_LINE = 0x3C;
    static constexpr uint16_t COMMAND_IO = 1;
    static constexpr uint16_t COMMAND_MEMORY = 2;
    static constexpr uint16_t COMMAND_BUS_MASTER = 4;
    static constexpr uint16_t COMMAND_INTERRUPT_DISABLE = 1u << 10;

    static uint32_t read32(const Location &at, uint8_t offset);
    static uint8_t read8(const Location &at, uint8_t offset);
    static uint16_t read16(const Location &at, uint8_t offset);
    static void write32(const Location &at, uint8_t offset, uint32_t value);
    static void write16(const Location &at, uint8_t offset, uint16_t value);

    static bool find(uint16_t vendor, uint16_t device, Location &found);

    static bool memoryBar(const Location &at, unsigned index, uint64_t &address, uint64_t &size);
    static bool enableMsi(const Location &at, uint8_t vector, uint32_t apicId);

private:
    static constexpr uint16_t ADDRESS_PORT = 0xCF8;
    static constexpr uint16_t DATA_PORT = 0xCFC;
    static uint32_t address(const Location &at, uint8_t offset);
};

#endif

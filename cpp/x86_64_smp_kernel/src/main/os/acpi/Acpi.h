#ifndef __ACPI__
#define __ACPI__
#include <stdint.h>

class Acpi {
public:
    static bool processorApicIds(uint32_t *ids, unsigned max, unsigned &count);
    // The FADT reset register when the firmware offers one in I/O space (ACPI 2.0 and later).
    static bool resetRegister(uint16_t &port, uint8_t &value);

private:
    static uint16_t rd16(const uint8_t *p) { return p[0] | (p[1] << 8); }
    static uint32_t rd32(const uint8_t *p) { return rd16(p) | ((uint32_t)rd16(p + 2) << 16); }
    static bool sigIs(const uint8_t *p, const char *sig, unsigned n);
    static bool checksumOk(const uint8_t *p, uint32_t len);
    static const uint8_t *findRsdp();
    static const uint8_t *findTable(const uint8_t *rsdp, const char *sig);

    static constexpr uint32_t FADT_FLAGS = 112;
    static constexpr uint32_t FADT_RESET_REG = 116;
    static constexpr uint32_t FADT_RESET_VALUE = 128;
    static constexpr uint32_t FADT_RESET_REG_SUP = 1u << 10;
    static constexpr uint8_t GAS_SYSTEM_IO = 1;
};

#endif

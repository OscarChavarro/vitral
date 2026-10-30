#ifndef __ACPI__
#define __ACPI__
#include <stdint.h>

class Acpi {
public:
    static bool processorApicIds(uint32_t *ids, unsigned max, unsigned &count);

private:
    static uint16_t rd16(const uint8_t *p) { return p[0] | (p[1] << 8); }
    static uint32_t rd32(const uint8_t *p) { return rd16(p) | ((uint32_t)rd16(p + 2) << 16); }
    static bool sigIs(const uint8_t *p, const char *sig, unsigned n);
    static bool checksumOk(const uint8_t *p, uint32_t len);
    static const uint8_t *findRsdp();
    static const uint8_t *findTable(const uint8_t *rsdp, const char *sig);
};

#endif

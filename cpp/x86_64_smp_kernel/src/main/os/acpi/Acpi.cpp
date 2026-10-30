#include "acpi/Acpi.h"

bool
Acpi::sigIs(const uint8_t *p, const char *sig, unsigned n) {
    for (unsigned i = 0; i < n; i++) {
        if (p[i] != (uint8_t)sig[i]) {
            return false;
        }
    }
    return true;
}

bool
Acpi::checksumOk(const uint8_t *p, uint32_t len) {
    uint8_t sum = 0;
    for (uint32_t i = 0; i < len; i++) {
        sum += p[i];
    }
    return sum == 0;
}

const uint8_t *
Acpi::findRsdp() {
    uint32_t ebda = (uint32_t)rd16((const uint8_t *)0x40E) << 4;
    class Range {
    public:
        uint32_t start;
        uint32_t end;
    } ranges[] = {{ebda, ebda + 1024}, {0xE0000, 0x100000}};
    for (const Range &r : ranges) {
        if (r.start < 0x400) {
            continue;
        }
        for (uint32_t a = r.start; a + 20 <= r.end; a += 16) {
            const uint8_t *p = (const uint8_t *)(uintptr_t)a;
            if (sigIs(p, "RSD PTR ", 8) && checksumOk(p, 20)) {
                return p;
            }
        }
    }
    return nullptr;
}

const uint8_t *
Acpi::findTable(const uint8_t *rsdp, const char *sig) {
    uint64_t root = 0;
    unsigned entry = 4;
    if (rsdp[15] >= 2 && rd32(rsdp + 28) == 0 && rd32(rsdp + 24) != 0) {
        root = rd32(rsdp + 24);
        entry = 8;
    } else {
        root = rd32(rsdp + 16);
    }
    if (root == 0) {
        return nullptr;
    }
    const uint8_t *sdt = (const uint8_t *)(uintptr_t)root;
    uint32_t len = rd32(sdt + 4);
    if (len < 36 || !checksumOk(sdt, len)) {
        return nullptr;
    }
    for (uint32_t off = 36; off + entry <= len; off += entry) {
        if (entry == 8 && rd32(sdt + off + 4) != 0) {
            continue;
        }
        const uint8_t *t = (const uint8_t *)(uintptr_t)rd32(sdt + off);
        uint32_t tlen = rd32(t + 4);
        if (sigIs(t, sig, 4) && tlen >= 36 && checksumOk(t, tlen)) {
            return t;
        }
    }
    return nullptr;
}

bool
Acpi::processorApicIds(uint32_t *ids, unsigned max, unsigned &count) {
    count = 0;
    const uint8_t *rsdp = findRsdp();
    if (!rsdp) {
        return false;
    }
    const uint8_t *madt = findTable(rsdp, "APIC");
    if (!madt) {
        return false;
    }
    uint32_t len = rd32(madt + 4);
    for (uint32_t off = 44; off + 2 <= len;) {
        const uint8_t *e = madt + off;
        unsigned type = e[0];
        unsigned elen = e[1];
        if (elen < 2 || off + elen > len) {
            break;
        }
        uint32_t id = 0;
        uint32_t flags = 0;
        bool cpu = false;
        if (type == 0 && elen >= 8) {
            id = e[3];
            flags = rd32(e + 4);
            cpu = true;
        } else if (type == 9 && elen >= 16) {
            id = rd32(e + 4);
            flags = rd32(e + 8);
            cpu = true;
        }
        if (cpu && (flags & 1)) {
            bool dup = false;
            for (unsigned i = 0; i < count && i < max; i++) {
                dup |= ids[i] == id;
            }
            if (!dup) {
                if (count < max) {
                    ids[count] = id;
                }
                count++;
            }
        }
        off += elen;
    }
    return true;
}

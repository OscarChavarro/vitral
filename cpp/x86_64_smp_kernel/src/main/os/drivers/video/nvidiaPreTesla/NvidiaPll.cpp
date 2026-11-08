#include "drivers/video/nvidiaPreTesla/NvidiaPll.h"

static const NvidiaPll::Limits G73_VPLL = {
    100000, 300000, 500000, 1000000, 2000, 27000, 25000, 75000, 8, 255, 1, 255, 8, 50, 1, 255, 7, 0,
};

static uint32_t
nvPllRead8(const volatile uint8_t *rom, size_t at) {
    return rom[at];
}

static uint32_t
nvPllRead16(const volatile uint8_t *rom, size_t at) {
    return (uint32_t)rom[at] | (uint32_t)rom[at + 1] << 8;
}

static uint32_t
nvPllRead32(const volatile uint8_t *rom, size_t at) {
    return nvPllRead16(rom, at) | nvPllRead16(rom, at + 2) << 16;
}

static uint32_t
nvPllDistance(uint32_t a, uint32_t b) {
    return a > b ? a - b : b - a;
}

const NvidiaPll::Limits &
NvidiaPll::fallback() {
    return G73_VPLL;
}

bool
NvidiaPll::fromVideoBios(const volatile uint8_t *rom, size_t bytes, Limits &limits, uint8_t &chipVersion,
                         uint32_t &biosVersion) {
    if (bytes < 512 || rom[0] != 0x55 || rom[1] != 0xAA) {
        return false;
    }
    size_t image = (size_t)rom[2] * 512;
    if (image < bytes) {
        bytes = image;
    }
    static const uint8_t BIT_SIGNATURE[6] = {0xFF, 0xB8, 'B', 'I', 'T', 0};
    size_t bit = 0;
    for (size_t at = 0; at + 12 < bytes && bit == 0; at++) {
        bool match = true;
        for (unsigned i = 0; i < 6 && match; i++) {
            match = rom[at + i] == BIT_SIGNATURE[i];
        }
        if (match) {
            bit = at;
        }
    }
    if (bit == 0) {
        return false;
    }
    unsigned headerBytes = nvPllRead8(rom, bit + 8);
    unsigned entryBytes = nvPllRead8(rom, bit + 9);
    unsigned entries = nvPllRead8(rom, bit + 10);
    size_t table = 0;
    chipVersion = 0;
    biosVersion = 0;
    for (unsigned i = 0; i < entries; i++) {
        size_t entry = bit + headerBytes + (size_t)i * entryBytes;
        if (entry + 6 > bytes) {
            break;
        }
        uint32_t id = nvPllRead8(rom, entry);
        uint32_t version = nvPllRead8(rom, entry + 1);
        uint32_t length = nvPllRead16(rom, entry + 2);
        size_t offset = nvPllRead16(rom, entry + 4);
        if (offset + length > bytes) {
            continue;
        }
        if (id == 'i' && length >= 4) {
            chipVersion = (uint8_t)nvPllRead8(rom, offset + 2);
            biosVersion = nvPllRead32(rom, offset);
        } else if (id == 'C' && version == 1 && length >= 10) {
            table = nvPllRead16(rom, offset + 8);
        } else if (id == 'C' && version == 2 && length >= 4) {
            table = nvPllRead32(rom, offset);
        }
    }
    if (table == 0 || table + 4 > bytes) {
        return false;
    }
    // Versions 0x20 and 0x21 of the PLL limits table: entries keyed by register, the first being the default.
    uint32_t version = nvPllRead8(rom, table);
    uint32_t header = nvPllRead8(rom, table + 1);
    uint32_t length = nvPllRead8(rom, table + 2);
    uint32_t count = nvPllRead8(rom, table + 3);
    if ((version != 0x20 && version != 0x21) || length < 0x1F || count == 0) {
        return false;
    }
    size_t data = table + header;
    for (uint32_t i = 0; i < count; i++) {
        size_t entry = table + header + (size_t)i * length;
        if (entry + length <= bytes && nvPllRead32(rom, entry) == 0x680508) {
            data = entry;
        }
    }
    if (data + length > bytes) {
        return false;
    }
    limits.vco1MinKhz = nvPllRead16(rom, data + 4) * 1000;
    limits.vco1MaxKhz = nvPllRead16(rom, data + 6) * 1000;
    limits.vco2MinKhz = nvPllRead16(rom, data + 8) * 1000;
    limits.vco2MaxKhz = nvPllRead16(rom, data + 10) * 1000;
    limits.input1MinKhz = nvPllRead16(rom, data + 12) * 1000;
    limits.input2MinKhz = nvPllRead16(rom, data + 14) * 1000;
    limits.input1MaxKhz = nvPllRead16(rom, data + 16) * 1000;
    limits.input2MaxKhz = nvPllRead16(rom, data + 18) * 1000;
    limits.n1Min = (uint8_t)nvPllRead8(rom, data + 20);
    limits.n1Max = (uint8_t)nvPllRead8(rom, data + 21);
    limits.m1Min = (uint8_t)nvPllRead8(rom, data + 22);
    limits.m1Max = (uint8_t)nvPllRead8(rom, data + 23);
    limits.n2Min = (uint8_t)nvPllRead8(rom, data + 24);
    limits.n2Max = (uint8_t)nvPllRead8(rom, data + 25);
    limits.m2Min = (uint8_t)nvPllRead8(rom, data + 26);
    limits.m2Max = (uint8_t)nvPllRead8(rom, data + 27);
    limits.log2PMax = (uint8_t)nvPllRead8(rom, data + 29);
    if (chipVersion < 0x60 && limits.log2PMax > 6) {
        limits.log2PMax = 6;
    }
    limits.referenceKhz = length > 0x22 ? nvPllRead32(rom, data + 31) : 0;
    return limits.vco1MaxKhz != 0 && limits.m1Min != 0 && limits.n1Min <= limits.n1Max;
}

bool
NvidiaPll::calculate(const Limits &limits, uint32_t referenceKhz, uint8_t chipVersion, uint32_t khz, bool twoStages,
                   Coefficients &out) {
    uint32_t crystal = limits.referenceKhz ? limits.referenceKhz : referenceKhz;
    if (crystal == 0 || khz == 0) {
        return false;
    }
    if (twoStages && limits.vco2MaxKhz != 0) {
        return dual(limits, crystal, chipVersion, khz, out);
    }
    return single(limits, crystal, chipVersion, khz, out);
}

bool
NvidiaPll::single(const Limits &limits, uint32_t crystal, uint8_t chipVersion, uint32_t khz, Coefficients &out) {
    uint32_t minVco = limits.vco1MinKhz;
    uint32_t maxVco = limits.vco1MaxKhz;
    unsigned maxM = limits.m1Max;
    // Older BIOS versions (below 0x40) need smaller reference dividers at high clocks.
    if (chipVersion < 0x40) {
        if (khz > 150000) {
            maxM = 6;
        }
        if (khz > 200000) {
            maxM = 4;
        }
        if (khz > 340000) {
            maxM = 2;
        }
    }
    unsigned maxLog2P = limits.log2PMax;
    if ((uint64_t)khz * (1u << maxLog2P) < minVco) {
        minVco = khz * maxLog2P;
        maxVco = minVco * 2;
    }
    if (khz + khz / 200 > maxVco) {
        maxVco = khz + khz / 200;
    }
    uint32_t bestDelta = 0xFFFFFFFFu;
    bool found = false;
    for (unsigned log2P = 0; log2P <= maxLog2P; log2P++) {
        uint32_t p = 1u << log2P;
        uint32_t clockP = khz * p;
        if (clockP < minVco) {
            continue;
        }
        if (clockP > maxVco) {
            break;
        }
        for (unsigned m = limits.m1Min; m <= maxM; m++) {
            if (crystal / m < limits.input1MinKhz) {
                break;
            }
            if (crystal / m > limits.input1MaxKhz) {
                continue;
            }
            uint32_t n = (clockP * m + crystal / 2) / crystal;
            if (n < limits.n1Min) {
                continue;
            }
            if (n > limits.n1Max) {
                break;
            }
            uint32_t calculated = ((n * crystal + p / 2) / p + m / 2) / m;
            uint32_t delta = nvPllDistance(calculated, khz);
            if (delta < bestDelta) {
                bestDelta = delta;
                found = true;
                out.n1 = (uint8_t)n;
                out.m1 = (uint8_t)m;
                out.n2 = 1;
                out.m2 = 1;
                out.log2P = (uint8_t)log2P;
                out.twoStages = false;
                out.khz = calculated;
                if (delta == 0) {
                    return true;
                }
            }
        }
    }
    return found;
}

bool
NvidiaPll::dual(const Limits &limits, uint32_t crystal, uint8_t chipVersion, uint32_t khz, Coefficients &out) {
    uint32_t maxVco2 = limits.vco2MaxKhz;
    bool fixedGain2 = limits.m2Min == limits.m2Max && limits.n2Min == limits.n2Max;
    uint32_t vco2 = (maxVco2 - maxVco2 / 200) / 2;
    unsigned log2P = 0;
    while (log2P < limits.log2PMax && khz <= (vco2 >> log2P)) {
        log2P++;
    }
    uint32_t clockP = khz << log2P;
    if (maxVco2 < khz + khz / 200) {
        maxVco2 = khz + khz / 200;
    }
    uint32_t bestDelta = 0xFFFFFFFFu;
    bool found = false;
    for (unsigned m1 = limits.m1Min; m1 <= limits.m1Max; m1++) {
        if (crystal / m1 < limits.input1MinKhz) {
            break;
        }
        if (crystal / m1 > limits.input1MaxKhz) {
            continue;
        }
        for (unsigned n1 = limits.n1Min; n1 <= limits.n1Max; n1++) {
            uint32_t clock1 = crystal * n1 / m1;
            if (clock1 < limits.vco1MinKhz) {
                continue;
            }
            if (clock1 > limits.vco1MaxKhz) {
                break;
            }
            for (unsigned m2 = limits.m2Min; m2 <= limits.m2Max; m2++) {
                if (clock1 / m2 < limits.input2MinKhz) {
                    break;
                }
                if (clock1 / m2 > limits.input2MaxKhz) {
                    continue;
                }
                uint32_t n2 = (clockP * m2 + clock1 / 2) / clock1;
                if (n2 < limits.n2Min) {
                    continue;
                }
                if (n2 > limits.n2Max) {
                    break;
                }
                uint32_t clock2 = clock1;
                if (!fixedGain2) {
                    if (chipVersion < 0x60 && (n2 / m2 < 4 || n2 / m2 > 10)) {
                        continue;
                    }
                    clock2 = clock1 * n2 / m2;
                    if (clock2 < limits.vco2MinKhz) {
                        break;
                    }
                    if (clock2 > maxVco2) {
                        continue;
                    }
                }
                uint32_t calculated = clock2 >> log2P;
                uint32_t delta = nvPllDistance(calculated, khz);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    found = true;
                    out.n1 = (uint8_t)n1;
                    out.m1 = (uint8_t)m1;
                    out.n2 = (uint8_t)n2;
                    out.m2 = (uint8_t)m2;
                    out.log2P = (uint8_t)log2P;
                    out.twoStages = true;
                    out.khz = calculated;
                    if (delta == 0) {
                        return true;
                    }
                }
            }
        }
    }
    return found;
}

bool
NvidiaPll::calculateNv04(uint32_t crystalKhz, uint32_t khz, Coefficients &out) {
    if (crystalKhz == 0 || khz == 0) {
        return false;
    }
    unsigned lowM = crystalKhz == 13500 ? 7 : 8;
    unsigned highM = crystalKhz == 13500 ? 13 : 14;
    uint32_t bestDelta = 0xFFFFFFFFu;
    bool found = false;
    for (unsigned log2P = 0; log2P <= 4; log2P++) {
        uint32_t vco = khz << log2P;
        if (vco < 128000 || vco > 350000) {
            continue;
        }
        for (unsigned m = lowM; m <= highM; m++) {
            uint32_t n = vco * m / crystalKhz;
            if (n == 0 || n > 255) {
                continue;
            }
            uint32_t calculated = (crystalKhz * n / m) >> log2P;
            uint32_t delta = nvPllDistance(calculated, khz);
            if (delta < bestDelta) {
                bestDelta = delta;
                found = true;
                out.n1 = (uint8_t)n;
                out.m1 = (uint8_t)m;
                out.n2 = 1;
                out.m2 = 1;
                out.log2P = (uint8_t)log2P;
                out.twoStages = false;
                out.khz = calculated;
            }
        }
    }
    return found;
}

#ifndef __CPU_MASK__
#define __CPU_MASK__
#include <stdint.h>

class CpuMask {
public:
    static constexpr unsigned MAX = 256;

    constexpr CpuMask() : bits{0, 0, 0, 0} {}
    static CpuMask all() {
        CpuMask m;
        for (auto &w : m.bits) {
            w = ~0ull;
        }
        return m;
    }
    static CpuMask only(unsigned cpu) {
        CpuMask m;
        m.set(cpu);
        return m;
    }

    void set(unsigned cpu) {
        if (cpu < MAX) {
            bits[cpu >> 6] |= 1ull << (cpu & 63);
        }
    }
    void clear(unsigned cpu) {
        if (cpu < MAX) {
            bits[cpu >> 6] &= ~(1ull << (cpu & 63));
        }
    }
    bool test(unsigned cpu) const { return cpu < MAX && (bits[cpu >> 6] >> (cpu & 63)) & 1; }
    uint64_t low64() const { return bits[0]; }
    void copyTo(unsigned long *out) const {
        for (unsigned i = 0; i < 4; i++) {
            out[i] = bits[i];
        }
    }
    static CpuMask from(const unsigned long *in) {
        CpuMask m;
        for (unsigned i = 0; i < 4; i++) {
            m.bits[i] = in[i];
        }
        return m;
    }
    bool empty() const { return !(bits[0] | bits[1] | bits[2] | bits[3]); }

private:
    uint64_t bits[4];
};

#endif

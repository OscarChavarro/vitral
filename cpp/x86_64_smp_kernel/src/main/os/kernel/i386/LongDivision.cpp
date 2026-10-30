#include <stdint.h>

// 64-bit division for the i386 build: the compiler calls these helpers (libgcc's names) for every uint64_t / and %,
// which the kernel, the libraries and the programs use freely. Not built for x86_64, where they are instructions.
class LongDivision {
public:
    static uint64_t divideUnsigned(uint64_t dividend, uint64_t divisor) asm("__udivdi3");
    static uint64_t moduloUnsigned(uint64_t dividend, uint64_t divisor) asm("__umoddi3");
    static int64_t divideSigned(int64_t dividend, int64_t divisor) asm("__divdi3");
    static int64_t moduloSigned(int64_t dividend, int64_t divisor) asm("__moddi3");
    static uint64_t divideModulo(uint64_t dividend, uint64_t divisor, uint64_t *remainder) asm("__udivmoddi4");

private:
    static uint64_t divide(uint64_t dividend, uint64_t divisor, uint64_t &remainder);
};

uint64_t
LongDivision::divide(uint64_t dividend, uint64_t divisor, uint64_t &remainder) {
    if (divisor == 0) {
        asm volatile("divl %0" : : "r"(0u) : "eax", "edx");  // #DE, as the x86_64 instruction raises
    }
    if ((divisor >> 32) == 0) {
        uint32_t d = (uint32_t)divisor;
        uint32_t high = (uint32_t)(dividend >> 32);
        uint32_t low = (uint32_t)dividend;
        uint32_t quotientHigh = high / d;
        uint32_t rest = high % d;
        uint32_t quotientLow;
        asm("divl %4" : "=a"(quotientLow), "=d"(rest) : "a"(low), "d"(rest), "rm"(d));
        remainder = rest;
        return ((uint64_t)quotientHigh << 32) | quotientLow;
    }
    uint64_t quotient = 0;
    uint64_t rest = 0;
    for (int bit = 63; bit >= 0; bit--) {
        bool carry = (rest >> 63) != 0;
        rest = (rest << 1) | ((dividend >> bit) & 1);
        if (carry || rest >= divisor) {
            rest -= divisor;
            quotient |= (uint64_t)1 << bit;
        }
    }
    remainder = rest;
    return quotient;
}

uint64_t
LongDivision::divideUnsigned(uint64_t dividend, uint64_t divisor) {
    uint64_t remainder;
    return divide(dividend, divisor, remainder);
}

uint64_t
LongDivision::moduloUnsigned(uint64_t dividend, uint64_t divisor) {
    uint64_t remainder;
    divide(dividend, divisor, remainder);
    return remainder;
}

int64_t
LongDivision::divideSigned(int64_t dividend, int64_t divisor) {
    uint64_t remainder;
    uint64_t magnitude = divide(dividend < 0 ? -(uint64_t)dividend : (uint64_t)dividend,
                                divisor < 0 ? -(uint64_t)divisor : (uint64_t)divisor, remainder);
    return (dividend < 0) != (divisor < 0) ? -(int64_t)magnitude : (int64_t)magnitude;
}

int64_t
LongDivision::moduloSigned(int64_t dividend, int64_t divisor) {
    uint64_t remainder;
    divide(dividend < 0 ? -(uint64_t)dividend : (uint64_t)dividend,
           divisor < 0 ? -(uint64_t)divisor : (uint64_t)divisor, remainder);
    return dividend < 0 ? -(int64_t)remainder : (int64_t)remainder;
}

uint64_t
LongDivision::divideModulo(uint64_t dividend, uint64_t divisor, uint64_t *remainder) {
    uint64_t rest;
    uint64_t quotient = divide(dividend, divisor, rest);
    if (remainder) {
        *remainder = rest;
    }
    return quotient;
}

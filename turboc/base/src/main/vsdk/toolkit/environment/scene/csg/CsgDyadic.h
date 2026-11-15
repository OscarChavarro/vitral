#ifndef __CSG_DYADIC__
#define __CSG_DYADIC__

#include "vsdk/toolkit/common/VSDK.h"
// Signed exact dyadic number. Fixed-capacity binary limbs avoid heap allocation;
// every operation reports failure and leaves its output unchanged on overflow.
//
// C++ port note (turboc, internals differ from cpp, plan decisions D2/D5):
// the cpp version keeps 1100 limbs of 32 bits and works with 64-bit
// intermediates; this one keeps 16-bit limbs (16 by 16 bit products fit the
// 32-bit `unsigned long` of every legacy compiler, Turbo C++ 3.0 included)
// and a 32-bit `long` exponent. The 32-bit targets keep the 35200 bits of
// the cpp version (2200 limbs); under Turbo C++, whose stack is 32 KB, the
// capacity is reduced to 24 limbs (384 bits): the solid certificates hold
// up to 64 of these numbers at a time. A value that does not fit reports
// failure, as an overflow of the cpp version does, so the certificates
// degrade to "not proven".
class CsgDyadic {
private:
#ifdef __TURBOC__
    enum { CAPACITY = 24 };
#else
    enum { CAPACITY = 2200 };
#endif
    unsigned short limbs[CAPACITY];
    int length;
    long exponent;
    int sign;
    BOOLEAN normalize();
    static BOOLEAN shifted(const CsgDyadic& value, long exponent, unsigned short* result);
public:
    CsgDyadic();
    static BOOLEAN fromDouble(double value, CsgDyadic& result);
    static BOOLEAN add(const CsgDyadic& a, const CsgDyadic& b, CsgDyadic& result);
    static BOOLEAN subtract(const CsgDyadic& a, const CsgDyadic& b, CsgDyadic& result);
    static BOOLEAN multiply(const CsgDyadic& a, const CsgDyadic& b, CsgDyadic& result);
    int getSign() const;
    BOOLEAN equals(const CsgDyadic& other) const;
    void negate();
};

#endif

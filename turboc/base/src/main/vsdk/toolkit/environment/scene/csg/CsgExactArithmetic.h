#ifndef __CSG_EXACT_ARITHMETIC__
#define __CSG_EXACT_ARITHMETIC__

#include "vsdk/toolkit/common/VSDK.h"
// Exact comparisons of finite binary64 expressions using dyadic integer limbs.
// Unsupported arithmetic environments or out-of-range intermediates return false.
//
// C++ port note (turboc, internals differ from cpp, plan decision D2): the
// cpp version adds the products in 140 limbs of 32 bits with 64-bit
// intermediates; this one covers the same 4480 bits with 280 limbs of 16
// bits, whose products and carries fit a 32-bit `unsigned long`. The
// significands are taken apart with frexp/ldexp, that are exact.
class CsgExactArithmetic {
private:
    enum { LIMBS = 280 };
    static BOOLEAN decompose(double value, unsigned short* significand, int& exponent);
    static BOOLEAN addProduct(unsigned short* positive, unsigned short* negative,
        double a, double b, BOOLEAN negate);
    static BOOLEAN addWord(unsigned short* target, int index, unsigned long word);
    static void clear(unsigned short* positive, unsigned short* negative);
    static BOOLEAN same(const unsigned short* positive, const unsigned short* negative);
public:
    static BOOLEAN equalSumProducts(const double* a, const double* b, const double* c, const double* d, int count);
    static BOOLEAN equalProducts(double a, double b, double c, double d);
    static BOOLEAN equalAffine(double a, double b, double c, double d, double e, double f);
};

#endif

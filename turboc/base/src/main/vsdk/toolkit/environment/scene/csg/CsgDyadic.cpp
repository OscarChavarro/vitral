#include <math.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"
#include "vsdk/toolkit/environment/scene/csg/CsgDyadic.h"

// Exponent range of the cpp version (its 32-bit `int`)
static const long EXPONENT_MAX = 2147483647L;
static const long EXPONENT_MIN = -2147483647L - 1L;

CsgDyadic::CsgDyadic() : length(0), exponent(0), sign(0)
{
    int i;
    for ( i = 0; i < CAPACITY; ++i ) limbs[i] = 0;
}

BOOLEAN CsgDyadic::normalize()
{
    int i;
    while (length>0 && limbs[length-1]==0) --length;
    if (!length) { exponent=0; sign=0; return TRUE; }
    int words=0; while (words<length && limbs[words]==0) ++words;
    if (words) {
        for (i=words;i<length;++i) limbs[i-words]=limbs[i];
        for (i=length-words;i<length;++i) limbs[i]=0;
        length-=words;
        if (exponent>EXPONENT_MAX-16L*words) return FALSE;
        exponent+=16L*words;
    }
    while ((limbs[0]&1U)==0) {
        unsigned short carry=0;
        for (i=length-1;i>=0;--i) {
            const unsigned short next=(unsigned short)(limbs[i]&1U);
            limbs[i]=(unsigned short)((limbs[i]>>1)|(carry<<15)); carry=next;
        }
        if (exponent==EXPONENT_MAX) return FALSE;
        ++exponent;
        while (length>0 && limbs[length-1]==0) --length;
    }
    return TRUE;
}

BOOLEAN CsgDyadic::fromDouble(double value, CsgDyadic& result)
{
    if (!FloatingInterval::isSupportedEnvironment() || !Double::isFinite(value)) return FALSE;
    CsgDyadic candidate;
    if (value!=0) {
        // value = significand * 2^(exp-52), exp as ilogb (the cpp
        // getExponent), also for subnormal values
        const double absolute=Math::abs(value);
        int exp=0;
        const double mantissa=frexp(absolute,&exp);
        exp--;
        double significand=ldexp(mantissa,53);
        if (!Double::isFinite(significand) || significand<4503599627370496.0 ||
            significand>=9007199254740992.0) return FALSE;
        int i;
        for (i=0;i<4;++i) {
            // Exact: the quotient by a power of two and its floor
            const double high=floor(significand/65536.0);
            candidate.limbs[i]=(unsigned short)(significand-high*65536.0);
            significand=high;
        }
        candidate.length=4; candidate.exponent=(long)exp-52L; candidate.sign=value<0 ? -1 : 1;
        if (!candidate.normalize()) return FALSE;
    }
    result=candidate; return TRUE;
}

BOOLEAN CsgDyadic::shifted(const CsgDyadic& value, long targetExponent, unsigned short* result)
{
    // The difference of two exponents of 32 bits, without overflow
    if (value.exponent<targetExponent) return FALSE;
    if (targetExponent<0 && value.exponent>EXPONENT_MAX+targetExponent) return FALSE;
    const long difference=value.exponent-targetExponent;
    const long words=difference/16; const int bits=(int)(difference%16);
    if (value.length && (words>=CAPACITY || words+value.length+(bits?1:0)>CAPACITY)) return FALSE;
    int i;
    for (i=0;i<CAPACITY;++i) result[i]=0;
    for (i=0;i<value.length;++i) {
        const unsigned long part=((unsigned long)value.limbs[i])<<bits;
        result[(int)words+i] |= (unsigned short)(part&0xffffUL);
        if (bits) result[(int)words+i+1] |= (unsigned short)(part>>16);
    }
    return TRUE;
}

BOOLEAN CsgDyadic::add(const CsgDyadic& a, const CsgDyadic& b, CsgDyadic& result)
{
    int i;
    if (!a.sign) { result=b; return TRUE; }
    if (!b.sign) { result=a; return TRUE; }
    const long common=a.exponent<b.exponent ? a.exponent : b.exponent;
    unsigned short x[CAPACITY],y[CAPACITY];
    if (!shifted(a,common,x) || !shifted(b,common,y)) return FALSE;
    CsgDyadic candidate; candidate.exponent=common;
    if (a.sign==b.sign) {
        unsigned long carry=0;
        for (i=0;i<CAPACITY;++i) {
            const unsigned long sum=(unsigned long)x[i]+y[i]+carry;
            candidate.limbs[i]=(unsigned short)(sum&0xffffUL); carry=sum>>16;
        }
        if (carry) return FALSE;
        candidate.sign=a.sign;
    } else {
        int comparison=0;
        for (i=CAPACITY-1;i>=0;--i) if (x[i]!=y[i]) { comparison=x[i]>y[i] ? 1 : -1; break; }
        if (!comparison) { result=candidate; return TRUE; }
        const unsigned short* large=comparison>0?x:y; const unsigned short* small=comparison>0?y:x;
        unsigned long borrow=0;
        for (i=0;i<CAPACITY;++i) {
            const unsigned long lhs=large[i], rhs=(unsigned long)small[i]+borrow;
            candidate.limbs[i]=(unsigned short)((lhs-rhs)&0xffffUL); borrow=lhs<rhs ? 1 : 0;
        }
        candidate.sign=comparison>0 ? a.sign : b.sign;
    }
    candidate.length=CAPACITY; if (!candidate.normalize()) return FALSE; result=candidate; return TRUE;
}

BOOLEAN CsgDyadic::subtract(const CsgDyadic& a, const CsgDyadic& b, CsgDyadic& result)
{ CsgDyadic opposite=b; opposite.negate(); return add(a,opposite,result); }

BOOLEAN CsgDyadic::multiply(const CsgDyadic& a, const CsgDyadic& b, CsgDyadic& result)
{
    int i, j;
    CsgDyadic candidate;
    if (!a.sign || !b.sign) { result=candidate; return TRUE; }
    if (a.length+b.length-1>CAPACITY) return FALSE;
    for (i=0;i<a.length;++i) {
        // (2^16-1)^2 + 2 (2^16-1) = 2^32 - 1: no 32-bit overflow
        unsigned long carry=0;
        for (j=0;j<b.length;++j) {
            const int k=i+j;
            if (k>=CAPACITY) return FALSE;
            const unsigned long value=(unsigned long)a.limbs[i]*b.limbs[j]+candidate.limbs[k]+carry;
            candidate.limbs[k]=(unsigned short)(value&0xffffUL); carry=value>>16;
        }
        int k=i+b.length;
        while (carry) {
            if (k>=CAPACITY) return FALSE;
            const unsigned long value=(unsigned long)candidate.limbs[k]+carry;
            candidate.limbs[k]=(unsigned short)(value&0xffffUL); carry=value>>16; ++k;
        }
    }
    if ((b.exponent>0 && a.exponent>EXPONENT_MAX-b.exponent) ||
        (b.exponent<0 && a.exponent<EXPONENT_MIN-b.exponent)) return FALSE;
    candidate.exponent=a.exponent+b.exponent; candidate.sign=a.sign*b.sign;
    candidate.length=a.length+b.length; if (candidate.length>CAPACITY) candidate.length=CAPACITY;
    if (!candidate.normalize()) return FALSE; result=candidate; return TRUE;
}

int CsgDyadic::getSign() const { return sign; }

BOOLEAN CsgDyadic::equals(const CsgDyadic& other) const
{
    int i;
    if (sign!=other.sign || exponent!=other.exponent || length!=other.length) return FALSE;
    for (i=0;i<length;++i) if (limbs[i]!=other.limbs[i]) return FALSE;
    return TRUE;
}

void CsgDyadic::negate() { sign=-sign; }

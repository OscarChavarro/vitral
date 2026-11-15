#include <math.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExactArithmetic.h"

/**
Odd integer significand (as four 16-bit limbs, lowest first) and exponent of
the lowest bit of a value; frexp gives the exponent of ilogb (the cpp
getExponent) plus one, also for subnormal values.
*/
BOOLEAN CsgExactArithmetic::decompose(double value, unsigned short* significand, int& exponent)
{
    int i;
    if (!Double::isFinite(value)) return FALSE;
    for (i=0;i<4;++i) significand[i]=0;
    if (value==0) { exponent=0; return TRUE; }
    const double magnitude=Math::abs(value);
    int binaryExponent=0;
    const double mantissa=frexp(magnitude,&binaryExponent);
    binaryExponent--;
    double integer=ldexp(mantissa,53);
    if (!Double::isFinite(integer) || integer<4503599627370496.0 || integer>=9007199254740992.0)
        return FALSE;
    exponent=binaryExponent-52;
    while (fmod(integer,2.0)==0.0) { integer=integer/2.0; ++exponent; }
    for (i=0;i<4;++i) {
        const double high=floor(integer/65536.0);
        significand[i]=(unsigned short)(integer-high*65536.0);
        integer=high;
    }
    return TRUE;
}

BOOLEAN CsgExactArithmetic::addWord(unsigned short* target, int index, unsigned long word)
{
    while (word!=0) {
        if (index<0 || index>=LIMBS) return FALSE;
        const unsigned long sum=target[index]+word;
        target[index]=(unsigned short)(sum&0xffffUL);
        word=sum>>16;
        ++index;
    }
    return TRUE;
}

BOOLEAN CsgExactArithmetic::addProduct(unsigned short* positive, unsigned short* negative,
    double a, double b, BOOLEAN negate)
{
    int i, j;
    unsigned short x[4], y[4];
    int ex=0,ey=0;
    if (!decompose(a,x,ex) || !decompose(b,y,ey)) return FALSE;
    if ((x[0]|x[1]|x[2]|x[3])==0 || (y[0]|y[1]|y[2]|y[3])==0) return TRUE;
    unsigned long product[8];
    for (i=0;i<8;++i) product[i]=0;
    for (i=0;i<4;++i) {
        unsigned long carry=0;
        for (j=0;j<4;++j) {
            const unsigned long value=(unsigned long)x[i]*y[j]+product[i+j]+carry;
            product[i+j]=value&0xffffUL;
            carry=value>>16;
        }
        product[i+4]=carry;
    }
    const int offset=ex+ey+2148;
    if (offset<0) return FALSE;
    const int wordOffset=offset/16, shift=offset%16;
    unsigned short* target=((a<0)!=(b<0))!=(negate!=FALSE) ? negative : positive;
    for (i=0;i<8;++i) {
        const unsigned long shifted=product[i]<<shift;
        if (!addWord(target,wordOffset+i,shifted&0xffffUL) ||
            !addWord(target,wordOffset+i+1,shifted>>16)) return FALSE;
    }
    return TRUE;
}

void CsgExactArithmetic::clear(unsigned short* positive, unsigned short* negative)
{
    int i;
    for (i=0;i<LIMBS;++i) { positive[i]=0; negative[i]=0; }
}

BOOLEAN CsgExactArithmetic::same(const unsigned short* positive, const unsigned short* negative)
{
    int i;
    for (i=0;i<LIMBS;++i) if (positive[i]!=negative[i]) return FALSE;
    return TRUE;
}

BOOLEAN CsgExactArithmetic::equalSumProducts(const double* a, const double* b,
    const double* c, const double* d, int count)
{
    int i;
    if (!a || !b || !c || !d || count<=0 || !FloatingInterval::isSupportedEnvironment()) return FALSE;
    unsigned short positive[LIMBS], negative[LIMBS];
    clear(positive,negative);
    for (i=0;i<count;++i)
        if (!addProduct(positive,negative,a[i],b[i],FALSE) ||
            !addProduct(positive,negative,c[i],d[i],TRUE)) return FALSE;
    return same(positive,negative);
}

BOOLEAN CsgExactArithmetic::equalProducts(double a, double b, double c, double d)
{
    if (!FloatingInterval::isSupportedEnvironment()) return FALSE;
    unsigned short positive[LIMBS], negative[LIMBS];
    clear(positive,negative);
    if (!addProduct(positive,negative,a,b,FALSE) || !addProduct(positive,negative,c,d,TRUE)) return FALSE;
    return same(positive,negative);
}

BOOLEAN CsgExactArithmetic::equalAffine(double a, double b, double c, double d, double e, double f)
{
    if (!FloatingInterval::isSupportedEnvironment()) return FALSE;
    unsigned short positive[LIMBS], negative[LIMBS];
    clear(positive,negative);
    if (!addProduct(positive,negative,a,b,FALSE) || !addProduct(positive,negative,c,1,FALSE) ||
        !addProduct(positive,negative,d,e,TRUE) || !addProduct(positive,negative,f,1,TRUE)) return FALSE;
    return same(positive,negative);
}

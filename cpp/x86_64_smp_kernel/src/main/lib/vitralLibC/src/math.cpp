#include <math.h>
#include "lib/vitralKernel/Cpu.h"
#include "lib/vitralLibC/math_dispatch.h"

typedef long double Real;

static const Real LN2 = 0.693147180559945309417232121458176568L;
static const Real PI = 3.14159265358979323846264338327950288L;
static const Real TWO_PI = 6.28318530717958647692528676655900577L;

static inline Real
truncated(Real x) {
    unsigned short saved;
    unsigned short changed;
    asm volatile("fnstcw %0" : "=m"(saved));
    changed = (unsigned short)((saved & 0xF3FF) | 0x0C00);
    asm volatile("fldcw %0" : : "m"(changed));
    asm volatile("frndint" : "+t"(x));
    asm volatile("fldcw %0" : : "m"(saved));
    return x;
}

static inline Real
roundedWith(Real x, unsigned short mode) {
    unsigned short saved;
    unsigned short changed;
    asm volatile("fnstcw %0" : "=m"(saved));
    changed = (unsigned short)((saved & 0xF3FF) | (mode << 10));
    asm volatile("fldcw %0" : : "m"(changed));
    asm volatile("frndint" : "+t"(x));
    asm volatile("fldcw %0" : : "m"(saved));
    return x;
}

static inline Real
nearest(Real x) {
    asm volatile("frndint" : "+t"(x));
    return x;
}

static inline Real
remainderOf(Real x, Real y, bool nearestQuotient) {
    if (nearestQuotient) {
        asm volatile("1: fprem1; fnstsw %%ax; testb $4, %%ah; jnz 1b; fstp %%st(1)" : "+t"(x) : "u"(y) : "ax", "st(1)", "cc");
    } else {
        asm volatile("1: fprem; fnstsw %%ax; testb $4, %%ah; jnz 1b; fstp %%st(1)" : "+t"(x) : "u"(y) : "ax", "st(1)", "cc");
    }
    return x;
}

static inline Real
scaleBy(Real x, Real exponent) {
    asm volatile("fscale; fstp %%st(1)" : "+t"(x) : "u"(exponent) : "st(1)");
    return x;
}

static inline Real
log2Of(Real x) {
    Real result;
    asm volatile("fyl2x" : "=t"(result) : "0"(x), "u"((Real)1) : "st(1)");
    return result;
}

static inline Real
twoToThe(Real x) {
    Real whole = nearest(x);
    Real fraction = x - whole;
    asm volatile("f2xm1" : "+t"(fraction));
    return scaleBy(fraction + 1, whole);
}

static inline bool
isInteger(Real x) { return x == truncated(x); }
static inline bool
isOdd(Real y) { return isInteger(y) && remainderOf(__builtin_fabsl(y), 2, false) == 1; }

Real
sqrtl(Real x) {
    asm("fsqrt" : "+t"(x));
    return x;
}

Real
fabsl(Real x) { return __builtin_fabsl(x); }
Real
copysignl(Real x, Real y) { return __builtin_copysignl(x, y); }
Real
floorl(Real x) { return roundedWith(x, 1); }
Real
ceill(Real x) { return roundedWith(x, 2); }
Real
truncl(Real x) { return truncated(x); }
Real
rintl(Real x) { return nearest(x); }
Real
nearbyintl(Real x) { return nearest(x); }

Real
roundl(Real x) {
    Real whole = truncated(x);
    if (__builtin_fabsl(x - whole) >= 0.5L) {
        whole += x < 0 ? -1 : 1;
    }
    return whole;
}

Real
fmodl(Real x, Real y) {
    if (y == 0 || __builtin_isinf(x) || __builtin_isnan(x) || __builtin_isnan(y)) {
        return __builtin_nanl("");
    }
    if (__builtin_isinf(y)) {
        return x;
    }
    return remainderOf(x, y, false);
}

Real
remainderl(Real x, Real y) {
    if (y == 0 || __builtin_isinf(x) || __builtin_isnan(x) || __builtin_isnan(y)) {
        return __builtin_nanl("");
    }
    if (__builtin_isinf(y)) {
        return x;
    }
    return remainderOf(x, y, true);
}

static Real
reduce(Real x) {
    if (__builtin_fabsl(x) > 1e18L) {
        return remainderOf(x, TWO_PI, false);
    }
    return x;
}

Real
sinl(Real x) {
    if (__builtin_isinf(x) || __builtin_isnan(x)) {
        return __builtin_nanl("");
    }
    x = reduce(x);
    asm("fsin" : "+t"(x));
    return x;
}

Real
cosl(Real x) {
    if (__builtin_isinf(x) || __builtin_isnan(x)) {
        return __builtin_nanl("");
    }
    x = reduce(x);
    asm("fcos" : "+t"(x));
    return x;
}

Real
tanl(Real x) {
    if (__builtin_isinf(x) || __builtin_isnan(x)) {
        return __builtin_nanl("");
    }
    x = reduce(x);
    Real one;
    asm("fptan" : "=t"(one), "=u"(x) : "0"(x));
    return x;
}

Real
atan2l(Real y, Real x) {
    Real result;
    asm volatile("fpatan" : "=t"(result) : "0"(x), "u"(y) : "st(1)");
    return result;
}

Real
atanl(Real x) { return atan2l(x, 1); }

Real
asinl(Real x) {
    if (__builtin_fabsl(x) > 1) {
        return __builtin_nanl("");
    }
    return atan2l(x, sqrtl((1 - x) * (1 + x)));
}

Real
acosl(Real x) {
    if (__builtin_fabsl(x) > 1) {
        return __builtin_nanl("");
    }
    return atan2l(sqrtl((1 - x) * (1 + x)), x);
}

Real
expl(Real x) {
    if (__builtin_isnan(x)) {
        return x;
    }
    if (x > 11356) {
        return __builtin_infl();
    }
    if (x < -11400) {
        return 0;
    }
    Real log2e;
    asm("fldl2e" : "=t"(log2e));
    return twoToThe(x * log2e);
}

Real
exp2l(Real x) {
    if (__builtin_isnan(x)) {
        return x;
    }
    if (x > 16384) {
        return __builtin_infl();
    }
    if (x < -16500) {
        return 0;
    }
    return twoToThe(x);
}

Real
expm1l(Real x) {
    if (__builtin_isnan(x)) {
        return x;
    }
    if (__builtin_fabsl(x) < 0.25L) {
        Real log2e;
        asm("fldl2e" : "=t"(log2e));
        Real t = x * log2e;
        asm("f2xm1" : "+t"(t));
        return t;
    }
    return expl(x) - 1;
}

Real
logl(Real x) {
    if (__builtin_isnan(x) || x < 0) {
        return __builtin_nanl("");
    }
    if (x == 0) {
        return -__builtin_infl();
    }
    return log2Of(x) * LN2;
}

Real
log2l(Real x) {
    if (__builtin_isnan(x) || x < 0) {
        return __builtin_nanl("");
    }
    if (x == 0) {
        return -__builtin_infl();
    }
    return log2Of(x);
}

Real
log10l(Real x) {
    if (__builtin_isnan(x) || x < 0) {
        return __builtin_nanl("");
    }
    if (x == 0) {
        return -__builtin_infl();
    }
    Real lg2;
    asm("fldlg2" : "=t"(lg2));
    return log2Of(x) * lg2;
}

Real
log1pl(Real x) {
    if (__builtin_isnan(x) || x < -1) {
        return __builtin_nanl("");
    }
    if (x == -1) {
        return -__builtin_infl();
    }
    if (__builtin_fabsl(x) < 0.29L) {
        Real result;
        asm volatile("fyl2xp1" : "=t"(result) : "0"(x), "u"(LN2) : "st(1)");
        return result;
    }
    return logl(1 + x);
}

Real
powl(Real x, Real y) {
    if (y == 0 || x == 1) {
        return 1;
    }
    if (__builtin_isnan(x) || __builtin_isnan(y)) {
        return __builtin_nanl("");
    }
    bool oddInteger = isOdd(y);
    if (x == 0) {
        if (y < 0) {
            return oddInteger ? copysignl(__builtin_infl(), x) : __builtin_infl();
        }
        return oddInteger ? x : 0;
    }
    if (__builtin_isinf(y)) {
        Real a = __builtin_fabsl(x);
        if (a == 1) {
            return 1;
        }
        return (a < 1) == (y > 0) ? 0 : __builtin_infl();
    }
    if (__builtin_isinf(x)) {
        if (x > 0) {
            return y > 0 ? __builtin_infl() : 0;
        }
        if (y > 0) {
            return oddInteger ? -__builtin_infl() : __builtin_infl();
        }
        return oddInteger ? -0.0L : 0;
    }
    if (x < 0 && !isInteger(y)) {
        return __builtin_nanl("");
    }
    Real a = __builtin_fabsl(x);
    Real result;
    if (isInteger(y) && __builtin_fabsl(y) <= 1024) {
        unsigned count = (unsigned)__builtin_fabsl(y);
        Real base = a;
        result = 1;
        for (; count; count >>= 1) {
            if (count & 1) {
                result *= base;
            }
            base *= base;
        }
        if (y < 0) {
            result = 1 / result;
        }
    } else {
        result = exp2l(y * log2Of(a));
    }
    return x < 0 && oddInteger ? -result : result;
}

Real
hypotl(Real x, Real y) {
    if (__builtin_isinf(x) || __builtin_isinf(y)) {
        return __builtin_infl();
    }
    return sqrtl(x * x + y * y);
}

Real
cbrtl(Real x) {
    if (x == 0 || __builtin_isnan(x) || __builtin_isinf(x)) {
        return x;
    }
    Real a = __builtin_fabsl(x);
    Real r = expl(logl(a) / 3);
    r = r - (r * r * r - a) / (3 * r * r);
    r = r - (r * r * r - a) / (3 * r * r);
    return x < 0 ? -r : r;
}

Real
sinhl(Real x) {
    if (__builtin_fabsl(x) > 11356) {
        return x > 0 ? __builtin_infl() : -__builtin_infl();
    }
    Real m = expm1l(x);
    return 0.5L * (m + m / (m + 1));
}

Real
coshl(Real x) {
    if (__builtin_fabsl(x) > 11356) {
        return __builtin_infl();
    }
    Real e = expl(x);
    return 0.5L * (e + 1 / e);
}

Real
tanhl(Real x) {
    if (x > 25) {
        return 1;
    }
    if (x < -25) {
        return -1;
    }
    Real m = expm1l(2 * x);
    return m / (m + 2);
}

Real
asinhl(Real x) {
    Real a = __builtin_fabsl(x);
    Real r = log1pl(a + a * a / (1 + sqrtl(1 + a * a)));
    return x < 0 ? -r : r;
}

Real
acoshl(Real x) {
    if (x < 1) {
        return __builtin_nanl("");
    }
    return logl(x + sqrtl((x - 1) * (x + 1)));
}

Real
atanhl(Real x) {
    if (__builtin_fabsl(x) > 1) {
        return __builtin_nanl("");
    }
    return 0.5L * log1pl(2 * x / (1 - x));
}

Real
ldexpl(Real x, int exponent) { return scaleBy(x, (Real)exponent); }
Real
scalbnl(Real x, int exponent) { return scaleBy(x, (Real)exponent); }

Real
frexpl(Real x, int *exponent) {
    if (x == 0 || __builtin_isinf(x) || __builtin_isnan(x)) {
        *exponent = 0;
        return x;
    }
    Real significand;
    Real power;
    asm("fxtract" : "=t"(significand), "=u"(power) : "0"(x));
    *exponent = (int)power + 1;
    return significand / 2;
}

Real
modfl(Real x, Real *whole) {
    Real w = truncated(x);
    *whole = w;
    return __builtin_isinf(x) ? copysignl(0, x) : x - w;
}

Real
fmal(Real x, Real y, Real z) { return x * y + z; }
Real
fminl(Real x, Real y) { return __builtin_isnan(x) ? y : __builtin_isnan(y) ? x
                                                                           : (x < y ? x : y); }
Real
fmaxl(Real x, Real y) { return __builtin_isnan(x) ? y : __builtin_isnan(y) ? x
                                                                           : (x > y ? x : y); }
Real
fdiml(Real x, Real y) { return x > y ? x - y : 0; }
long
lroundl(Real x) { return (long)roundl(x); }
long long
llroundl(Real x) { return (long long)roundl(x); }
long
lrintl(Real x) { return (long)nearest(x); }
long long
llrintl(Real x) { return (long long)nearest(x); }

int
ilogbl(Real x) {
    int exponent;
    frexpl(x, &exponent);
    return exponent - 1;
}

Real
logbl(Real x) { return (Real)ilogbl(x); }

Real
erfl(Real x) {
    Real a = __builtin_fabsl(x);
    if (a > 6) {
        return x < 0 ? -1 : 1;
    }
    Real sum = a;
    Real term = a;
    Real square = a * a;
    if (a < 3) {
        for (int n = 1; n < 200; n++) {
            term *= -square / n;
            Real add = term / (2 * n + 1);
            sum += add;
            if (__builtin_fabsl(add) < 1e-20L * __builtin_fabsl(sum)) {
                break;
            }
        }
        Real r = 1.12837916709551257390L * sum;
        return x < 0 ? -r : r;
    }
    Real fraction = 0;
    for (int n = 60; n >= 1; n--) {
        fraction = n / 2.0L / (a + fraction);
    }
    Real r = 1 - expl(-square) / (1.77245385090551602730L * (a + fraction));
    return x < 0 ? -r : r;
}

Real
lgammal(Real x) {
    static const Real coefficients[9] = {0.99999999999980993L, 676.5203681218851L, -1259.1392167224028L,
                                         771.32342877765313L, -176.61502916214059L, 12.507343278686905L,
                                         -0.13857109526572012L, 9.9843695780195716e-6L, 1.5056327351493116e-7L};
    if (x < 0.5L) {
        return logl(PI / __builtin_fabsl(sinl(PI * x))) - lgammal(1 - x);
    }
    x -= 1;
    Real sum = coefficients[0];
    Real t = x + 7.5L;
    for (int i = 1; i < 9; i++) {
        sum += coefficients[i] / (x + i);
    }
    return 0.91893853320467274178L + (x + 0.5L) * logl(t) - t + logl(sum);
}

Real
tgammal(Real x) {
    if (x == truncated(x) && x <= 0) {
        return __builtin_nanl("");
    }
    if (x < 0.5L) {
        return PI / (sinl(PI * x) * tgammal(1 - x));
    }
    return expl(lgammal(x));
}

#define VITRAL_WRAP1(name) \
    double name(double x) { return (double)name##l(x); } \
    float name##f(float x) { return (float)name##l(x); }
#define VITRAL_WRAP2(name) \
    double name(double x, double y) { return (double)name##l(x, y); } \
    float name##f(float x, float y) { return (float)name##l(x, y); }

VITRAL_WRAP1(sin)
VITRAL_WRAP1(cos)
VITRAL_WRAP1(tan)
VITRAL_WRAP1(asin)
VITRAL_WRAP1(acos)
VITRAL_WRAP1(atan)
VITRAL_WRAP2(atan2)
VITRAL_WRAP1(sinh)
VITRAL_WRAP1(cosh)
VITRAL_WRAP1(tanh)
VITRAL_WRAP1(asinh)
VITRAL_WRAP1(acosh)
VITRAL_WRAP1(atanh)
VITRAL_WRAP1(exp)
VITRAL_WRAP1(exp2)
VITRAL_WRAP1(expm1)
VITRAL_WRAP1(log)
VITRAL_WRAP1(log2)
VITRAL_WRAP1(log10)
VITRAL_WRAP1(log1p)
VITRAL_WRAP2(pow)
VITRAL_WRAP1(cbrt)
VITRAL_WRAP2(hypot)
VITRAL_WRAP2(fmod)
VITRAL_WRAP2(remainder)
VITRAL_WRAP2(fdim)
VITRAL_WRAP1(erf)
VITRAL_WRAP1(tgamma)
VITRAL_WRAP1(lgamma)
VITRAL_WRAP1(logb)
VITRAL_WRAP1(rint)
VITRAL_WRAP1(nearbyint)

double
fmin(double x, double y) { return (double)fminl(x, y); }
float
fminf(float x, float y) { return (float)fminl(x, y); }
double
fmax(double x, double y) { return (double)fmaxl(x, y); }
float
fmaxf(float x, float y) { return (float)fmaxl(x, y); }
double
copysign(double x, double y) { return __builtin_copysign(x, y); }
float
copysignf(float x, float y) { return __builtin_copysignf(x, y); }
double
fabs(double x) { return __builtin_fabs(x); }
float
fabsf(float x) { return __builtin_fabsf(x); }
double
fma(double x, double y, double z) { return (double)fmal(x, y, z); }
float
fmaf(float x, float y, float z) { return (float)fmal(x, y, z); }
double
ldexp(double x, int e) { return (double)ldexpl(x, e); }
float
ldexpf(float x, int e) { return (float)ldexpl(x, e); }
double
scalbn(double x, int e) { return (double)scalbnl(x, e); }
float
scalbnf(float x, int e) { return (float)scalbnl(x, e); }
int
ilogb(double x) { return ilogbl(x); }
int
ilogbf(float x) { return ilogbl(x); }

double
frexp(double x, int *e) { return (double)frexpl(x, e); }
float
frexpf(float x, int *e) { return (float)frexpl(x, e); }

double
modf(double x, double *whole) {
    Real w;
    double fraction = (double)modfl(x, &w);
    *whole = (double)w;
    return fraction;
}

float
modff(float x, float *whole) {
    Real w;
    float fraction = (float)modfl(x, &w);
    *whole = (float)w;
    return fraction;
}

long
lround(double x) { return lroundl(x); }
long
lroundf(float x) { return lroundl(x); }
long long
llround(double x) { return llroundl(x); }
long long
llroundf(float x) { return llroundl(x); }
long
lrint(double x) { return lrintl(x); }
long
lrintf(float x) { return lrintl(x); }
long long
llrint(double x) { return llrintl(x); }
long long
llrintf(float x) { return llrintl(x); }

double
sqrt(double x) { return __builtin_sqrt(x); }
float
sqrtf(float x) { return __builtin_sqrtf(x); }

static double
floorX87(double x) { return (double)floorl(x); }
static double
ceilX87(double x) { return (double)ceill(x); }
static double
truncX87(double x) { return (double)truncl(x); }
static double
roundX87(double x) { return (double)roundl(x); }

__attribute__((target("sse4.1"))) static double
floorSse41(double x) {
    double r;
    asm("roundsd $9, %1, %0" : "=x"(r) : "x"(x));
    return r;
}

__attribute__((target("sse4.1"))) static double
ceilSse41(double x) {
    double r;
    asm("roundsd $10, %1, %0" : "=x"(r) : "x"(x));
    return r;
}

__attribute__((target("sse4.1"))) static double
truncSse41(double x) {
    double r;
    asm("roundsd $11, %1, %0" : "=x"(r) : "x"(x));
    return r;
}

static double (*floorImplementation)(double) = floorX87;
static double (*ceilImplementation)(double) = ceilX87;
static double (*truncImplementation)(double) = truncX87;
static double (*roundImplementation)(double) = roundX87;

void
MathDispatch::select() {
    if (Cpu::hasCpuid() && Cpu::hasSse41()) {
        floorImplementation = floorSse41;
        ceilImplementation = ceilSse41;
        truncImplementation = truncSse41;
    } else {
        floorImplementation = floorX87;
        ceilImplementation = ceilX87;
        truncImplementation = truncX87;
    }
}

double
floor(double x) { return floorImplementation(x); }
double
ceil(double x) { return ceilImplementation(x); }
double
trunc(double x) { return truncImplementation(x); }
double
round(double x) { return roundImplementation(x); }
float
floorf(float x) { return (float)floorImplementation(x); }
float
ceilf(float x) { return (float)ceilImplementation(x); }
float
truncf(float x) { return (float)truncImplementation(x); }
float
roundf(float x) { return (float)roundImplementation(x); }

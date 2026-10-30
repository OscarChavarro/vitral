/* MATHL's pre-387 trigonometric paths call Borland's real-mode INT 3Eh
 * shortcuts. Use only native 287 instructions in this freestanding CRT. */
#include <math.h>
#include <errno.h>
#include <float.h>

extern "C" double crt286TangentQuarter(double);
extern "C" double crt286AtanUnit(double);
extern "C" double crt286Log2(double);
extern "C" double crt286Exp2(double);

static void sineCosine(double angle, double* sine, double* cosine)
{
    const double halfPi = 1.57079632679489661923;
    const double twoPi = 6.28318530717958647693;
    /* MATHL fmod uses FPREM, which is available on the 287. Reduction first
     * keeps the integer conversion bounded, including negative angles. */
    double reduced = fmod(angle, twoPi);
    if (reduced != reduced) { *sine = *cosine = reduced; return; }
    int quadrant = (int)floor(reduced / halfPi + 0.5);
    double tangent = crt286TangentQuarter(reduced - quadrant * halfPi);
    double c = 1.0 / sqrt(1.0 + tangent * tangent);
    double s = tangent * c;
    quadrant %= 4;
    if (quadrant < 0) quadrant += 4;
    switch (quadrant) {
        case 0: *sine = s; *cosine = c; break;
        case 1: *sine = c; *cosine = -s; break;
        case 2: *sine = -s; *cosine = -c; break;
        default: *sine = -c; *cosine = s; break;
    }
}

extern "C" double sin(double angle)
{
    double s, c; sineCosine(angle, &s, &c); return s;
}
extern "C" double cos(double angle)
{
    double s, c; sineCosine(angle, &s, &c); return c;
}
extern "C" double tan(double angle)
{
    double s, c; sineCosine(angle, &s, &c); return s / c;
}

extern "C" double atan(double x)
{
    int negative = x < 0;
    if (negative) x = -x;
    double result = x > 1.0 ?
        1.57079632679489661923 - crt286AtanUnit(1.0 / x) :
        crt286AtanUnit(x);
    return negative ? -result : result;
}

extern "C" double atan2(double y, double x)
{
    const double pi = 3.14159265358979323846;
    if (x > 0.0) return atan(y / x);
    if (x < 0.0) return atan(y / x) + (y < 0.0 ? -pi : pi);
    if (y > 0.0) return pi / 2.0;
    if (y < 0.0) return -pi / 2.0;
    return y; // Signed zero; general IEEE exception handling is not provided.
}

static double exponential(double exponent)
{
    if (exponent > 1023.9999999999999) {
        errno = ERANGE;
        return HUGE_VAL;
    }
    if (exponent < -1074.0) { errno = ERANGE; return 0.0; }
    return crt286Exp2(exponent);
}

extern "C" double exp(double x)
{
    return exponential(x * 1.44269504088896340736);
}

extern "C" double pow(double base, double exponent)
{
    if (exponent == 0.0 || base == 1.0) return 1.0;
    if (base == 0.0) {
        if (exponent > 0.0) return 0.0;
        errno = ERANGE; return HUGE_VAL;
    }
    int negative = 0;
    if (base < 0.0) {
        if (floor(exponent) != exponent) {
            errno = EDOM;
            double zero = 0.0;
            return zero / zero;
        }
        negative = fmod(exponent, 2.0) != 0.0;
        base = -base;
    }
    double result = exponential(exponent * crt286Log2(base));
    return negative ? -result : result;
}

#ifndef __MATH__
#define __MATH__
#define M_E 2.7182818284590452354
#define M_LOG2E 1.4426950408889634074
#define M_LOG10E 0.43429448190325182765
#define M_LN2 0.69314718055994530942
#define M_LN10 2.30258509299404568402
#define M_PI 3.14159265358979323846
#define M_PI_2 1.57079632679489661923
#define M_PI_4 0.78539816339744830962
#define M_1_PI 0.31830988618379067154
#define M_2_PI 0.63661977236758134308
#define M_2_SQRTPI 1.12837916709551257390
#define M_SQRT2 1.41421356237309504880
#define M_SQRT1_2 0.70710678118654752440
#define HUGE_VAL (__builtin_huge_val())
#define HUGE_VALF (__builtin_huge_valf())
#define INFINITY (__builtin_inff())
#define NAN (__builtin_nanf(""))
#define FP_NAN 0
#define FP_INFINITE 1
#define FP_ZERO 2
#define FP_SUBNORMAL 3
#define FP_NORMAL 4
typedef double double_t;
typedef float float_t;
#define VITRAL_MATH_DECLARE(type, suffix) \
    type sin##suffix(type x); type cos##suffix(type x); type tan##suffix(type x); \
    type asin##suffix(type x); type acos##suffix(type x); type atan##suffix(type x); \
    type atan2##suffix(type y, type x); type sinh##suffix(type x); type cosh##suffix(type x); \
    type tanh##suffix(type x); type exp##suffix(type x); type exp2##suffix(type x); \
    type expm1##suffix(type x); type log##suffix(type x); type log2##suffix(type x); \
    type log10##suffix(type x); type log1p##suffix(type x); type pow##suffix(type x, type y); \
    type sqrt##suffix(type x); type cbrt##suffix(type x); type hypot##suffix(type x, type y); \
    type fabs##suffix(type x); type floor##suffix(type x); type ceil##suffix(type x); \
    type round##suffix(type x); type trunc##suffix(type x); type rint##suffix(type x); \
    type nearbyint##suffix(type x); type fmod##suffix(type x, type y); \
    type remainder##suffix(type x, type y); type copysign##suffix(type x, type y); \
    type fmin##suffix(type x, type y); type fmax##suffix(type x, type y); \
    type fdim##suffix(type x, type y); type ldexp##suffix(type x, int exponent); \
    type scalbn##suffix(type x, int exponent); type frexp##suffix(type x, int *exponent); \
    type modf##suffix(type x, type *whole); type fma##suffix(type x, type y, type z); \
    long lround##suffix(type x); long long llround##suffix(type x); long lrint##suffix(type x); \
    long long llrint##suffix(type x); type asinh##suffix(type x); type acosh##suffix(type x); \
    type atanh##suffix(type x); type erf##suffix(type x); type tgamma##suffix(type x); \
    type lgamma##suffix(type x); int ilogb##suffix(type x); type logb##suffix(type x);
VITRAL_MATH_DECLARE(double, )
VITRAL_MATH_DECLARE(float, f)
VITRAL_MATH_DECLARE(long double, l)
#undef VITRAL_MATH_DECLARE
inline int
isnan(float x) { return __builtin_isnan(x); }
inline int
isnan(double x) { return __builtin_isnan(x); }
inline int
isnan(long double x) { return __builtin_isnan(x); }
inline int
isinf(float x) { return __builtin_isinf(x); }
inline int
isinf(double x) { return __builtin_isinf(x); }
inline int
isinf(long double x) { return __builtin_isinf(x); }
inline int
isfinite(float x) { return __builtin_isfinite(x); }
inline int
isfinite(double x) { return __builtin_isfinite(x); }
inline int
isfinite(long double x) { return __builtin_isfinite(x); }
inline int
signbit(float x) { return __builtin_signbit(x); }
inline int
signbit(double x) { return __builtin_signbit(x); }
inline int
signbit(long double x) { return __builtin_signbit(x); }

#endif

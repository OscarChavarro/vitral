#ifndef __STD_ABS__
#define __STD_ABS__
#include <stdlib.h>
namespace std {
using ::abs;
using ::labs;
using ::llabs;
inline long
abs(long value) { return value < 0 ? -value : value; }
inline long long
abs(long long value) { return value < 0 ? -value : value; }
inline double
abs(double value) { return __builtin_fabs(value); }
inline float
abs(float value) { return __builtin_fabsf(value); }
inline long double
abs(long double value) { return __builtin_fabsl(value); }
}

#endif

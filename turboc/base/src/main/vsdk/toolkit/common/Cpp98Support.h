#ifndef __CPP98_SUPPORT__
#define __CPP98_SUPPORT__

#include <stdarg.h>
#include <string.h>
#include <string>

// C99 `va_copy` is not declared by C++98 `<stdarg.h>`
#ifndef va_copy
    #ifdef __va_copy
        #define va_copy(destination, source) __va_copy(destination, source)
    #else
        #define va_copy(destination, source) \
            memcpy(&(destination), &(source), sizeof(va_list))
    #endif
#endif

/**
Replacements for C++11 standard library functions (and C99 math functions
not exported by C++98 `<cmath>`), so the port builds with legacy C++98
compilers. Each method keeps the semantics of its standard counterpart.
*/
class Cpp98Support {
  public:
    // std::to_string
    static std::string toString(int value);
    static std::string toString(unsigned int value);
    static std::string toString(long value);
    static std::string toString(unsigned long value);
    static std::string toString(long long value);
    static std::string toString(unsigned long long value);
    static std::string toString(float value);
    static std::string toString(double value);
    static std::string toString(long double value);

    // std::fmax / std::fmin: when one argument is NaN, the other is returned
    static double fmax(double a, double b);
    static double fmin(double a, double b);

    // std::round family: halfway cases are rounded away from zero
    static double round(double value);
    static long lround(double value);
    static long long llround(double value);

    // std::hypot / std::ilogb, from the C library `<math.h>` (C99 / XSI)
    static double hypot(double x, double y);
    static int ilogb(double value);

    // std::isnan / std::isinf / std::isfinite / std::signbit
    static bool isNaN(double value);
    static bool isInfinite(double value);
    static bool isFinite(double value);
    static bool signBit(double value);

    // std::abs overload set: the C `abs` only takes `int`, so calling it on
    // floating point values would silently truncate them
    static int abs(int value);
    static long abs(long value);
    static long long abs(long long value);
    static float abs(float value);
    static double abs(double value);

  private:
    Cpp98Support();
};

#endif

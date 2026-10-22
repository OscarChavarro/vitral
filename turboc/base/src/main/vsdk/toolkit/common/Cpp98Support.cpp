#include <math.h>
#include <stdio.h>

#include "vsdk/toolkit/common/Cpp98Support.h"

String
Cpp98Support::toString(int value)
{
    char buffer[32];
    sprintf(buffer, "%d", value);
    return String(buffer);
}

String
Cpp98Support::toString(unsigned int value)
{
    char buffer[32];
    sprintf(buffer, "%u", value);
    return String(buffer);
}

String
Cpp98Support::toString(long value)
{
    char buffer[32];
    sprintf(buffer, "%ld", value);
    return String(buffer);
}

String
Cpp98Support::toString(unsigned long value)
{
    char buffer[32];
    sprintf(buffer, "%lu", value);
    return String(buffer);
}

String
Cpp98Support::toString(long long value)
{
    char buffer[32];
    sprintf(buffer, "%lld", value);
    return String(buffer);
}

String
Cpp98Support::toString(unsigned long long value)
{
    char buffer[32];
    sprintf(buffer, "%llu", value);
    return String(buffer);
}

String
Cpp98Support::toString(float value)
{
    return toString((double)value);
}

String
Cpp98Support::toString(double value)
{
    // "%f" of the largest double needs 309 digits plus sign and decimals
    char buffer[512];
    sprintf(buffer, "%f", value);
    return String(buffer);
}

String
Cpp98Support::toString(long double value)
{
    char buffer[5120];
    sprintf(buffer, "%Lf", value);
    return String(buffer);
}

double
Cpp98Support::fmax(double a, double b)
{
    if ( isNaN(a) ) {
        return b;
    }
    if ( isNaN(b) ) {
        return a;
    }
    return a > b ? a : b;
}

double
Cpp98Support::fmin(double a, double b)
{
    if ( isNaN(a) ) {
        return b;
    }
    if ( isNaN(b) ) {
        return a;
    }
    return a < b ? a : b;
}

double
Cpp98Support::round(double value)
{
    if ( !isFinite(value) ) {
        return value;
    }
    double integralPart = floor(::fabs(value));
    if ( ::fabs(value) - integralPart >= 0.5 ) {
        integralPart += 1.0;
    }
    return value < 0.0 ? -integralPart : integralPart;
}

long
Cpp98Support::lround(double value)
{
    return (long)round(value);
}

long long
Cpp98Support::llround(double value)
{
    return (long long)round(value);
}

double
Cpp98Support::hypot(double x, double y)
{
    return ::hypot(x, y);
}

int
Cpp98Support::ilogb(double value)
{
    return ::ilogb(value);
}

bool
Cpp98Support::isNaN(double value)
{
    return value != value;
}

bool
Cpp98Support::isInfinite(double value)
{
    return !isNaN(value) && isNaN(value - value);
}

bool
Cpp98Support::isFinite(double value)
{
    return !isNaN(value - value);
}

bool
Cpp98Support::signBit(double value)
{
    return copysign(1.0, value) < 0.0;
}

int
Cpp98Support::abs(int value)
{
    return value < 0 ? -value : value;
}

long
Cpp98Support::abs(long value)
{
    return value < 0 ? -value : value;
}

long long
Cpp98Support::abs(long long value)
{
    return value < 0 ? -value : value;
}

float
Cpp98Support::abs(float value)
{
    return (float)fabs(value);
}

double
Cpp98Support::abs(double value)
{
    return fabs(value);
}

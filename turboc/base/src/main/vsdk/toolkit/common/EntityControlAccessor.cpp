#include <limits>

#include "java/lang/Double.h"
#include "java/lang/Float.h"
#include "java/lang/Integer.h"
#include "java/lang/Long.h"
#include "java/lang/NumberFormatException.h"
#include "vsdk/toolkit/common/EntityControlAccessor.h"
#include <limits.h>

double
EntityControlValue<double>::parse(const String &text)
{
    return Double::parseDouble(text);
}

String
EntityControlValue<double>::toString(double value)
{
    return Double::toString(value);
}

float
EntityControlValue<float>::parse(const String &text)
{
    return Float::parseFloat(text);
}

String
EntityControlValue<float>::toString(float value)
{
    return Float::toString(value);
}

int
EntityControlValue<int>::parse(const String &text)
{
    return Integer::parseInt(text);
}

String
EntityControlValue<int>::toString(int value)
{
    return String::valueOf(value);
}

long long
EntityControlValue<long long>::parse(const String &text)
{
    return Long::parseLong(text);
}

String
EntityControlValue<long long>::toString(long long value)
{
    return String::valueOf(value);
}

long
EntityControlValue<long>::parse(const String &text)
{
    long long value = Long::parseLong(text);
    if ( value < LONG_MIN ||
         value > LONG_MAX ) {
        throw NumberFormatException(
            String("For input string: \"") + text + "\"");
    }
    return ((long)(value));
}

String
EntityControlValue<long>::toString(long value)
{
    return String::valueOf(value);
}

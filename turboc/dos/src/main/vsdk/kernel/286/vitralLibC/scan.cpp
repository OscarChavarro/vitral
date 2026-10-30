/* scanf subset for MIT scenes: strings, signed integers and decimal floats.
 * No dependency on Borland's DOS scanner, locale tables or FP startup. */
#include <stdarg.h>
#include <stddef.h>
#include <float.h>
#include <errno.h>

extern "C" long strtol(const char*, char**, int);
static int white(int c) { return c == ' ' || (c >= 9 && c <= 13); }
static double infinity()
{
    /* IEEE binary64 on the little-endian x86 targets of this runtime. */
    union { unsigned char bytes[8]; double value; } number;
    for (unsigned i = 0; i < 8; ++i) number.bytes[i] = 0;
    number.bytes[6] = 0xF0; number.bytes[7] = 0x7F;
    return number.value;
}

static int decimal(const char* text, const char** end, double* result)
{
    const char* p = text;
    double value = 0.0;
    int negative = 0, digits = 0, significant = 0, scale = 0;
    int exponent = 0, exponentNegative = 0;
    if (*p == '-' || *p == '+') negative = *p++ == '-';
    *result = negative ? -0.0 : 0.0;
    while (*p >= '0' && *p <= '9') {
        int d = *p++ - '0';
        digits = 1;
        if (!significant && !d) continue;
        if (significant < 17) { value = value * 10.0 + d; ++significant; }
        else ++scale;
    }
    if (*p == '.') {
        ++p;
        while (*p >= '0' && *p <= '9') {
            int d = *p++ - '0';
            digits = 1;
            if (!significant && !d) { --scale; continue; }
            if (significant < 17) { value = value * 10.0 + d; ++significant; --scale; }
        }
    }
    if (!digits) return 0;
    if (*p == 'e' || *p == 'E') {
        const char* marker = p++;
        if (*p == '-' || *p == '+') exponentNegative = *p++ == '-';
        if (*p < '0' || *p > '9') p = marker;
        else while (*p >= '0' && *p <= '9') {
            if (exponent < 1000) exponent = exponent * 10 + *p - '0';
            ++p;
        }
    }
    *end = p;
    if (value != 0.0) {
        scale += exponentNegative ? -exponent : exponent;
        if (scale > 4096 || scale < -4096) {
            if (scale > 0) *result = negative ? -infinity() : infinity();
            errno = ERANGE; return 0;
        }
        while (scale > 0) {
            if (value > DBL_MAX / 10.0) {
                *result = negative ? -infinity() : infinity();
                errno = ERANGE; return 0;
            }
            value *= 10.0; --scale;
        }
        while (scale < 0) { value *= 0.1; ++scale; }
        if (value == 0.0) { errno = ERANGE; return 0; }
    }
    *result = negative ? -value : value;
    return 1;
}

extern "C" double strtod(const char* text, char** end)
{
    const char* start = text;
    const char* stop = text;
    double result = 0.0;
    while (white(*text)) ++text;
    if (!decimal(text, &stop, &result) && stop == start) {
        if (end) *end = (char*)start;
        return 0.0;
    }
    if (end) *end = (char*)stop;
    return result;
}

static int scanText(const char* text, const char* format, va_list arguments)
{
    int assigned = 0;
    while (*format) {
        unsigned width = 0, n;
        int suppress = 0, wide = 0;
        char type, token[256];
        const char* stop;
        if (white(*format)) {
            while (white(*format)) ++format;
            while (white(*text)) ++text;
            continue;
        }
        if (*format != '%') {
            if (!*text) return assigned ? assigned : -1;
            if (*text != *format) break;
            ++text; ++format; continue;
        }
        ++format;
        if (*format == '%') {
            if (!*text) return assigned ? assigned : -1;
            if (*text != '%') break;
            ++text; ++format; continue;
        }
        if (*format == '*') { suppress = 1; ++format; }
        while (*format >= '0' && *format <= '9') {
            if (width < 1000) width = width * 10 + *format - '0';
            ++format;
        }
        if (*format == 'l') { wide = 1; ++format; }
        if (!*format) break;
        type = *format++;
        while (white(*text)) ++text;
        if (!*text) return assigned ? assigned : -1;
        if (type == 's') {
            char* output = suppress ? 0 : va_arg(arguments, char*);
            if (!width) width = 65535U;
            n = 0;
            while (*text && !white(*text) && n < width) {
                if (output) output[n] = *text;
                ++text; ++n;
            }
            if (output) { output[n] = 0; ++assigned; }
        }
        else if (type == 'd' || type == 'i' || type == 'f' || type == 'e' || type == 'g') {
            if (!width || width > 255) width = 255;
            for (n = 0; n < width && text[n] && !white(text[n]); ++n) token[n] = text[n];
            token[n] = 0;
            if (type == 'd' || type == 'i') {
                char* end;
                long value;
                errno = 0;
                value = strtol(token, &end, type == 'i' ? 0 : 10);
                if (end == token || errno == ERANGE) break;
                stop = end;
                if (!suppress) {
                    if (wide) *va_arg(arguments, long*) = value;
                    else *va_arg(arguments, int*) = (int)value;
                }
            }
            else {
                double value;
                if (!decimal(token, &stop, &value)) break;
                if (!suppress) {
                    if (wide) *va_arg(arguments, double*) = value;
                    else {
                        if (value > FLT_MAX || value < -FLT_MAX) { errno = ERANGE; break; }
                        *va_arg(arguments, float*) = (float)value;
                    }
                }
            }
            text += stop - token;
            if (!suppress) ++assigned;
        }
        else { errno = EINVAL; break; }
    }
    return assigned;
}

extern "C" int crt286ScanText(const char* text, const char* format, ...)
{
    int result;
    va_list arguments;
    va_start(arguments, format);
    result = scanText(text, format, arguments);
    va_end(arguments);
    return result;
}
extern "C" int sscanf(const char* text, const char* format, ...)
{
    int result;
    va_list arguments;
    va_start(arguments, format);
    result = scanText(text, format, arguments);
    va_end(arguments);
    return result;
}

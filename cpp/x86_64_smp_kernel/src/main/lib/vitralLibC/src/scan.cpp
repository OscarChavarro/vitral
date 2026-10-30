#include "format_engine.h"
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

static bool
inSet(const char *set, const char *end, char c) {
    bool negate = *set == '^';
    if (negate) {
        set++;
    }
    bool found = false;
    for (const char *p = set; p < end; p++) {
        if (p + 2 < end && p[1] == '-') {
            if (c >= p[0] && c <= p[2]) {
                found = true;
            }
            p += 2;
        } else if (*p == c) {
            found = true;
        }
    }
    return found != negate;
}

int
scanText(const char *text, const char *format, va_list arguments) {
    const char *start = text;
    int converted = 0;
    while (*format) {
        if (isspace((unsigned char)*format)) {
            while (isspace((unsigned char)*text)) {
                text++;
            }
            format++;
            continue;
        }
        if (*format != '%') {
            if (*text != *format) {
                return converted ? converted : (*text ? 0 : -1);
            }
            text++;
            format++;
            continue;
        }
        format++;
        if (*format == '%') {
            if (*text != '%') {
                return converted;
            }
            text++;
            format++;
            continue;
        }
        bool suppress = false;
        if (*format == '*') {
            suppress = true;
            format++;
        }
        int width = 0;
        while (*format >= '0' && *format <= '9') {
            width = width * 10 + (*format++ - '0');
        }
        int longs = 0;
        bool shortInt = false;
        bool longDouble = false;
        for (;; format++) {
            if (*format == 'l') {
                longs++;
            } else if (*format == 'h') {
                shortInt = true;
            } else if (*format == 'L') {
                longDouble = true;
            } else if (*format == 'z' || *format == 'j' || *format == 't') {
                longs = 1;
            } else {
                break;
            }
        }
        char conversion = *format;
        if (!conversion) {
            break;
        }
        format++;
        if (conversion == 'n') {
            if (!suppress) {
                *va_arg(arguments, int *) = (int)(text - start);
            }
            continue;
        }
        if (conversion != 'c' && conversion != '[') {
            while (isspace((unsigned char)*text)) {
                text++;
            }
        }
        if (!*text) {
            return converted ? converted : -1;
        }
        char buffer[512];
        size_t limit = width > 0 && width < (int)sizeof(buffer) - 1 ? (size_t)width : sizeof(buffer) - 1;
        switch (conversion) {
        case 'd':
        case 'i':
        case 'u':
        case 'x':
        case 'X':
        case 'o': {
            size_t n = 0;
            if ((text[n] == '+' || text[n] == '-') && n < limit) {
                buffer[n] = text[n], n++;
            }
            int base = conversion == 'x' || conversion == 'X' ? 16 : conversion == 'o' ? 8
                                                                 : conversion == 'i'   ? 0
                                                                                       : 10;
            while (n < limit && (isxdigit((unsigned char)text[n]) || (base == 0 && (text[n] == 'x' || text[n] == 'X')))) {
                if (base != 16 && base != 0 && !isdigit((unsigned char)text[n])) {
                    break;
                }
                if (base == 10 && !isdigit((unsigned char)text[n])) {
                    break;
                }
                buffer[n] = text[n];
                n++;
            }
            buffer[n] = 0;
            char *end;
            long long value = (conversion == 'u' || conversion == 'x' || conversion == 'X' || conversion == 'o')
                                  ? (long long)strtoull(buffer, &end, base)
                                  : strtoll(buffer, &end, base);
            if (end == buffer) {
                return converted;
            }
            text += end - buffer;
            if (!suppress) {
                if (longs >= 2) {
                    *va_arg(arguments, long long *) = value;
                } else if (longs == 1) {
                    *va_arg(arguments, long *) = (long)value;
                } else if (shortInt) {
                    *va_arg(arguments, short *) = (short)value;
                } else {
                    *va_arg(arguments, int *) = (int)value;
                }
                converted++;
            }
            break;
        }
        case 'f':
        case 'F':
        case 'e':
        case 'E':
        case 'g':
        case 'G': {
            size_t n = 0;
            while (n < limit && text[n] && (isalnum((unsigned char)text[n]) || text[n] == '.' || text[n] == '+' || text[n] == '-')) {
                if ((text[n] == '+' || text[n] == '-') && n > 0 && text[n - 1] != 'e' && text[n - 1] != 'E') {
                    break;
                }
                buffer[n] = text[n];
                n++;
            }
            buffer[n] = 0;
            char *end;
            double value = strtod(buffer, &end);
            if (end == buffer) {
                return converted;
            }
            text += end - buffer;
            if (!suppress) {
                if (longDouble) {
                    *va_arg(arguments, long double *) = value;
                } else if (longs) {
                    *va_arg(arguments, double *) = value;
                } else {
                    *va_arg(arguments, float *) = (float)value;
                }
                converted++;
            }
            break;
        }
        case 's': {
            char *out = suppress ? nullptr : va_arg(arguments, char *);
            size_t n = 0;
            while (text[n] && !isspace((unsigned char)text[n]) && (width <= 0 || (int)n < width)) {
                if (out) {
                    out[n] = text[n];
                }
                n++;
            }
            if (out) {
                out[n] = 0;
            }
            text += n;
            if (!suppress) {
                converted++;
            }
            break;
        }
        case 'c': {
            char *out = suppress ? nullptr : va_arg(arguments, char *);
            int n = width > 0 ? width : 1;
            for (int i = 0; i < n; i++) {
                if (!text[i]) {
                    return converted;
                }
                if (out) {
                    out[i] = text[i];
                }
            }
            text += n;
            if (!suppress) {
                converted++;
            }
            break;
        }
        case '[': {
            const char *set = format;
            if (*format == '^') {
                format++;
            }
            if (*format == ']') {
                format++;
            }
            while (*format && *format != ']') {
                format++;
            }
            const char *end = format;
            if (*format) {
                format++;
            }
            char *out = suppress ? nullptr : va_arg(arguments, char *);
            size_t n = 0;
            while (text[n] && inSet(set, end, text[n]) && (width <= 0 || (int)n < width)) {
                if (out) {
                    out[n] = text[n];
                }
                n++;
            }
            if (n == 0) {
                return converted;
            }
            if (out) {
                out[n] = 0;
            }
            text += n;
            if (!suppress) {
                converted++;
            }
            break;
        }
        default:
            return converted;
        }
    }
    return converted;
}

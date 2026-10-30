#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include "syscalls/Syscalls.h"

int
abs(int value) { return value < 0 ? -value : value; }
long
labs(long value) { return value < 0 ? -value : value; }
long long
llabs(long long value) { return value < 0 ? -value : value; }

div_t
div(int numerator, int denominator) { return {numerator / denominator, numerator % denominator}; }
ldiv_t
ldiv(long numerator, long denominator) { return {numerator / denominator, numerator % denominator}; }

static unsigned long long
parseUnsigned(const char *text, char **end, int base, bool &negative, bool &overflow) {
    const char *p = text;
    while (isspace((unsigned char)*p)) {
        p++;
    }
    negative = false;
    if (*p == '+' || *p == '-') {
        negative = *p++ == '-';
    }
    if ((base == 0 || base == 16) && p[0] == '0' && (p[1] == 'x' || p[1] == 'X') && isxdigit((unsigned char)p[2])) {
        p += 2;
        base = 16;
    } else if (base == 0) {
        base = p[0] == '0' ? 8 : 10;
    }
    const char *digits = p;
    unsigned long long value = 0;
    overflow = false;
    for (;; p++) {
        int digit;
        if (isdigit((unsigned char)*p)) {
            digit = *p - '0';
        } else if (isalpha((unsigned char)*p)) {
            digit = tolower((unsigned char)*p) - 'a' + 10;
        } else {
            break;
        }
        if (digit >= base) {
            break;
        }
        if (value > (ULLONG_MAX - digit) / base) {
            overflow = true;
        }
        value = value * base + digit;
    }
    if (end) {
        *end = (char *)(p == digits ? text : p);
    }
    return value;
}

unsigned long long
strtoull(const char *text, char **end, int base) {
    bool negative;
    bool overflow;
    unsigned long long value = parseUnsigned(text, end, base, negative, overflow);
    if (overflow) {
        errno = ERANGE;
        return ULLONG_MAX;
    }
    return negative ? 0 - value : value;
}

unsigned long
strtoul(const char *text, char **end, int base) { return (unsigned long)strtoull(text, end, base); }

long long
strtoll(const char *text, char **end, int base) {
    bool negative;
    bool overflow;
    unsigned long long value = parseUnsigned(text, end, base, negative, overflow);
    if (overflow || value > (negative ? (unsigned long long)LLONG_MAX + 1 : (unsigned long long)LLONG_MAX)) {
        errno = ERANGE;
        return negative ? LLONG_MIN : LLONG_MAX;
    }
    return negative ? (long long)(0 - value) : (long long)value;
}

long
strtol(const char *text, char **end, int base) { return (long)strtoll(text, end, base); }

int
atoi(const char *text) { return (int)strtol(text, nullptr, 10); }
long
atol(const char *text) { return strtol(text, nullptr, 10); }
long long
atoll(const char *text) { return strtoll(text, nullptr, 10); }

long double
strtold(const char *text, char **end) {
    const char *p = text;
    while (isspace((unsigned char)*p)) {
        p++;
    }
    bool negative = false;
    if (*p == '+' || *p == '-') {
        negative = *p++ == '-';
    }
    if (strncasecmp(p, "inf", 3) == 0) {
        p += strncasecmp(p, "infinity", 8) == 0 ? 8 : 3;
        if (end) {
            *end = (char *)p;
        }
        return negative ? -__builtin_infl() : __builtin_infl();
    }
    if (strncasecmp(p, "nan", 3) == 0) {
        if (end) {
            *end = (char *)(p + 3);
        }
        return __builtin_nanl("");
    }
    unsigned long long mantissa = 0;
    int exponent = 0;
    int digits = 0;
    bool any = false;
    for (; isdigit((unsigned char)*p); p++, any = true) {
        if (mantissa < (ULLONG_MAX - 9) / 10) {
            mantissa = mantissa * 10 + (unsigned)(*p - '0');
            if (mantissa) {
                digits++;
            }
        } else {
            exponent++;
        }
    }
    if (*p == '.') {
        p++;
        for (; isdigit((unsigned char)*p); p++, any = true) {
            if (mantissa < (ULLONG_MAX - 9) / 10) {
                mantissa = mantissa * 10 + (unsigned)(*p - '0');
                exponent--;
            }
        }
    }
    if (!any) {
        if (end) {
            *end = (char *)text;
        }
        return 0;
    }
    if (*p == 'e' || *p == 'E') {
        const char *q = p + 1;
        bool negativeExponent = false;
        if (*q == '+' || *q == '-') {
            negativeExponent = *q++ == '-';
        }
        if (isdigit((unsigned char)*q)) {
            int value = 0;
            for (; isdigit((unsigned char)*q); q++) {
                if (value < 10000) {
                    value = value * 10 + (*q - '0');
                }
            }
            exponent += negativeExponent ? -value : value;
            p = q;
        }
    }
    if (end) {
        *end = (char *)p;
    }
    long double result = (long double)mantissa;
    if (mantissa) {
        bool inverse = exponent < 0;
        unsigned count = (unsigned)(inverse ? -exponent : exponent);
        long double scale = 1;
        long double base = 10;
        for (unsigned bit = count; bit; bit >>= 1) {
            if (bit & 1) {
                scale *= base;
            }
            base *= base;
        }
        result = inverse ? result / scale : result * scale;
    }
    return negative ? -result : result;
}

double
strtod(const char *text, char **end) { return (double)strtold(text, end); }
float
strtof(const char *text, char **end) { return (float)strtold(text, end); }
double
atof(const char *text) { return strtod(text, nullptr); }

static unsigned long long randomState = 1;

void
srand(unsigned seed) { randomState = seed; }

int
rand() {
    randomState = randomState * 6364136223846793005ull + 1442695040888963407ull;
    return (int)(randomState >> 33);
}

static void
swapBytes(char *a, char *b, size_t size) {
    for (; size; size--, a++, b++) {
        char t = *a;
        *a = *b;
        *b = t;
    }
}

static void
siftDown(char *base, size_t size, size_t start, size_t count, int (*compare)(const void *, const void *)) {
    for (;;) {
        size_t child = 2 * start + 1;
        if (child >= count) {
            return;
        }
        if (child + 1 < count && compare(base + child * size, base + (child + 1) * size) < 0) {
            child++;
        }
        if (compare(base + start * size, base + child * size) >= 0) {
            return;
        }
        swapBytes(base + start * size, base + child * size, size);
        start = child;
    }
}

void
qsort(void *base, size_t count, size_t size, int (*compare)(const void *, const void *)) {
    if (count < 2) {
        return;
    }
    char *items = (char *)base;
    for (size_t i = count / 2; i-- > 0;) {
        siftDown(items, size, i, count, compare);
    }
    for (size_t end = count - 1; end > 0; end--) {
        swapBytes(items, items + end * size, size);
        siftDown(items, size, 0, end, compare);
    }
}

void *
bsearch(const void *key, const void *base, size_t count, size_t size,
        int (*compare)(const void *, const void *)) {
    const char *items = (const char *)base;
    size_t low = 0;
    size_t high = count;
    while (low < high) {
        size_t mid = low + (high - low) / 2;
        int order = compare(key, items + mid * size);
        if (order == 0) {
            return (void *)(items + mid * size);
        }
        if (order < 0) {
            high = mid;
        } else {
            low = mid + 1;
        }
    }
    return nullptr;
}

void
exit(int status) { Syscalls::exit(status); }
void
_Exit(int status) { Syscalls::exit(status); }

void
abort() {
    fputs("abort\n", stderr);
    Syscalls::exit(134);
}

int
atexit(void (*)()) { return 0; }
char *
getenv(const char *) { return nullptr; }
int
setenv(const char *, const char *, int) { return -1; }
int
unsetenv(const char *) { return -1; }
int
system(const char *) { return -1; }

#include <stddef.h>
#include <limits.h>
#include <errno.h>

extern "C" { char _ctype[257]; }
extern "C" void crt286InitializeCtype()
{
    unsigned i;
    _ctype[0] = 0; /* EOF */
    for (i = 0; i < 256; ++i) {
        unsigned bits = 0;
        if (i == ' ' || (i >= 9 && i <= 13)) bits |= 1;
        if (i >= '0' && i <= '9') bits |= 2 | 16;
        if (i >= 'A' && i <= 'Z') bits |= 4;
        if (i >= 'a' && i <= 'z') bits |= 8;
        if ((i >= 'a' && i <= 'f') || (i >= 'A' && i <= 'F')) bits |= 16;
        if (i < 32 || i == 127) bits |= 32;
        if (i > 32 && i < 127 && !(bits & (2 | 4 | 8))) bits |= 64;
        _ctype[i + 1] = (char)bits;
    }
}

extern "C" int strncmp(const char* a, const char* b, size_t count)
{
    while (count--) {
        if (*a != *b) return (int)(unsigned char)*a - (int)(unsigned char)*b;
        if (!*a) break;
        ++a; ++b;
    }
    return 0;
}
extern "C" char* strchr(const char* text, int value)
{
    do { if (*text == (char)value) return (char*)text; } while (*text++);
    return 0;
}
extern "C" char* strrchr(const char* text, int value)
{
    const char* found = 0;
    do { if (*text == (char)value) found = text; } while (*text++);
    return (char*)found;
}
extern "C" void* memchr(const void* data, int value, size_t count)
{
    const unsigned char* p = (const unsigned char*)data;
    while (count--) { if (*p == (unsigned char)value) return (void*)p; ++p; }
    return 0;
}
extern "C" char* strstr(const char* text, const char* needle)
{
    const char *a, *b;
    if (!*needle) return (char*)text;
    for (; *text; ++text) {
        a = text; b = needle;
        while (*a && *b && *a == *b) { ++a; ++b; }
        if (!*b) return (char*)text;
    }
    return 0;
}
extern "C" char* strtok(char* text, const char* delimiters)
{
    static char* cursor = 0;
    char* result;
    if (text) cursor = text;
    if (!cursor) return 0;
    while (*cursor && strchr(delimiters, *cursor)) ++cursor;
    if (!*cursor) { cursor = 0; return 0; }
    result = cursor;
    while (*cursor && !strchr(delimiters, *cursor)) ++cursor;
    if (*cursor) *cursor++ = 0;
    else cursor = 0;
    return result;
}
extern "C" int toupper(int c) { return c >= 'a' && c <= 'z' ? c - 'a' + 'A' : c; }
extern "C" int tolower(int c) { return c >= 'A' && c <= 'Z' ? c - 'A' + 'a' : c; }

static int digit(int c)
{
    if (c >= '0' && c <= '9') return c - '0';
    c = tolower(c);
    return c >= 'a' && c <= 'z' ? c - 'a' + 10 : -1;
}
extern "C" long strtol(const char* text, char** end, int base)
{
    const char* original = text;
    unsigned long value = 0, limit;
    int negative = 0, any = 0, overflow = 0, d;
    if (end) *end = (char*)original;
    if (base != 0 && (base < 2 || base > 36)) { errno = EINVAL; return 0; }
    while (*text == ' ' || (*text >= '\t' && *text <= '\r')) ++text;
    if (*text == '-' || *text == '+') negative = *text++ == '-';
    if ((base == 0 || base == 16) && text[0] == '0' &&
        (text[1] == 'x' || text[1] == 'X') && digit(text[2]) >= 0 && digit(text[2]) < 16) {
        base = 16; text += 2;
    }
    if (base == 0) base = *text == '0' ? 8 : 10;
    limit = (unsigned long)LONG_MAX + (negative ? 1UL : 0UL);
    while ((d = digit((unsigned char)*text)) >= 0 && d < base) {
        any = 1;
        if (value > (limit - (unsigned)d) / (unsigned)base) overflow = 1;
        else value = value * (unsigned)base + (unsigned)d;
        ++text;
    }
    if (!any) return 0;
    if (end) *end = (char*)text;
    if (overflow) { errno = ERANGE; return negative ? LONG_MIN : LONG_MAX; }
    if (negative && value == (unsigned long)LONG_MAX + 1UL) return LONG_MIN;
    return negative ? -(long)value : (long)value;
}
extern "C" long atol(const char* text) { return strtol(text, 0, 10); }

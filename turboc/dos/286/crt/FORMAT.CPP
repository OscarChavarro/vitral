#include "FORMAT.H"
#include <limits.h>
#include <float.h>
#include <errno.h>

static unsigned length(const char* s) { unsigned n = 0; while (s[n]) ++n; return n; }
static int emit(Crt286Output output, void* context, char c, int* count)
{
    if (*count == INT_MAX) { errno = ERANGE; return 0; }
    if (!output(c, context)) return 0;
    ++*count;
    return 1;
}
static unsigned integer(char* text, unsigned long value, unsigned base, int upper)
{
    char reversed[32];
    unsigned n = 0, i;
    const char* digits = upper ? "0123456789ABCDEF" : "0123456789abcdef";
    do { reversed[n++] = digits[value % base]; value /= base; } while (value);
    for (i = 0; i < n; ++i) text[i] = reversed[n - i - 1];
    text[n] = 0;
    return n;
}
static int fixed(char* text, long double value, int precision)
{
    long double rounding = .5L;
    int i, exponent = 0, digit;
    unsigned n = 0;
    if (value != value) { text[0]='n'; text[1]='a'; text[2]='n'; text[3]=0; return 3; }
    if (value > (long double)DBL_MAX) { errno = ERANGE; return -1; }
    for (i = 0; i < precision; ++i) rounding *= .1L;
    value += rounding;
    if (value >= 1) while (value >= 10) { value /= 10; ++exponent; }
    else if (value > 0) while (value < 1) { value *= 10; --exponent; }
    for (i = exponent > 0 ? exponent : 0; i >= -precision; --i) {
        if (i == -1) text[n++] = '.';
        if (i > exponent) text[n++] = '0';
        else {
            digit = (int)value;
            if (digit > 9) digit = 9;
            text[n++] = (char)('0' + digit);
            value = (value - digit) * 10;
        }
    }
    text[n] = 0;
    return (int)n;
}
int crt286VFormat(Crt286Output output, void* context, const char* format, va_list args)
{
    int count = 0;
    while (*format) {
        char text[384], sign = 0, type;
        const char* field = text;
        int left = 0, plus = 0, blank = 0, zero = 0, wide = 0;
        int width = 0, precision = -1, n = 0, padding, zeros = 0, numeric = 0;
        if (*format != '%') {
            if (!emit(output, context, *format++, &count)) return -1;
            continue;
        }
        ++format;
        for (;;) {
            if (*format == '-') left = 1;
            else if (*format == '+') plus = 1;
            else if (*format == ' ') blank = 1;
            else if (*format == '0') zero = 1;
            else break;
            ++format;
        }
        if (*format == '*') {
            width = va_arg(args, int); ++format;
            if (width < 0) { if (width == INT_MIN) return -1; left = 1; width = -width; }
        }
        else while (*format >= '0' && *format <= '9') {
            unsigned long next = (unsigned long)width * 10 + *format++ - '0';
            if (next > 4096) { errno = ERANGE; return -1; }
            width = (int)next;
        }
        if (width > 4096) { errno = ERANGE; return -1; }
        if (*format == '.') {
            ++format; precision = 0;
            if (*format == '*') { precision = va_arg(args, int); ++format; }
            else while (*format >= '0' && *format <= '9') {
                unsigned long next = (unsigned long)precision * 10 + *format++ - '0';
                if (next > 4096) { errno = ERANGE; return -1; }
                precision = (int)next;
            }
        }
        if (*format == 'l') { wide = 1; ++format; if (*format == 'l') ++format; }
        else if (*format == 'L') { wide = 2; ++format; }
        if (!*format) { errno = EINVAL; return -1; }
        type = *format++;
        if (type == 's') {
            field = va_arg(args, const char*);
            if (!field) field = "(null)";
            unsigned size = length(field);
            if (size > INT_MAX) { errno = ERANGE; return -1; }
            n = (int)size;
            if (precision >= 0 && n > precision) n = precision;
        }
        else if (type == 'c') { text[0] = (char)va_arg(args, int); n = 1; }
        else if (type == '%') { text[0] = '%'; n = 1; }
        else if (type == 'd' || type == 'i' || type == 'u' || type == 'x' || type == 'X' || type == 'p') {
            unsigned long value;
            numeric = 1;
            if (type == 'd' || type == 'i') {
                long v = wide ? va_arg(args, long) : (long)va_arg(args, int);
                if (v < 0) { sign = '-'; value = (unsigned long)(-(v + 1)) + 1UL; }
                else { value = (unsigned long)v; sign = plus ? '+' : blank ? ' ' : 0; }
            }
            else if (type == 'p') value = (unsigned long)va_arg(args, void*);
            else value = wide ? va_arg(args, unsigned long) : (unsigned long)va_arg(args, unsigned);
            n = (int)integer(text, value, type == 'x' || type == 'X' || type == 'p' ? 16 : 10, type == 'X');
            if (precision == 0 && value == 0) n = 0;
            if (precision > 4096) { errno = ERANGE; return -1; }
            if (precision > n) zeros = precision - n;
            if (precision >= 0) zero = 0;
        }
        else if (type == 'f') {
            long double v = wide == 2 ? va_arg(args, long double) : (long double)va_arg(args, double);
            numeric = 1;
            if (v < 0) { sign = '-'; v = -v; }
            else sign = plus ? '+' : blank ? ' ' : 0;
            if (precision < 0) precision = 6;
            if (precision > 18) { errno = ERANGE; return -1; }
            n = fixed(text, v, precision);
            if (n < 0) return -1;
        }
        else { errno = EINVAL; return -1; }
        padding = width - n - zeros - (sign != 0);
        if (!left && !(zero && numeric)) while (padding-- > 0)
            if (!emit(output, context, ' ', &count)) return -1;
        if (sign && !emit(output, context, sign, &count)) return -1;
        if (!left && zero && numeric) while (padding-- > 0)
            if (!emit(output, context, '0', &count)) return -1;
        while (zeros-- > 0) if (!emit(output, context, '0', &count)) return -1;
        for (int i = 0; i < n; ++i) if (!emit(output, context, field[i], &count)) return -1;
        if (left) while (padding-- > 0) if (!emit(output, context, ' ', &count)) return -1;
    }
    return count;
}

struct Buffer { char* text; size_t capacity; size_t used; };
static int bufferOutput(char c, void* context)
{
    Buffer* b = (Buffer*)context;
    if (b->capacity && b->used < b->capacity - 1) b->text[b->used] = c;
    ++b->used;
    return 1;
}
int crt286VBuffer(char* buffer, size_t capacity, const char* format, va_list args)
{
    Buffer b;
    int result;
    if (!buffer && capacity) { errno = EINVAL; return -1; }
    b.text = buffer; b.capacity = capacity; b.used = 0;
    result = crt286VFormat(bufferOutput, &b, format, args);
    if (capacity) buffer[b.used < capacity ? b.used : capacity - 1] = 0;
    return result;
}
extern "C" int crt286Format(char* buffer, size_t capacity, const char* format, ...)
{
    va_list args;
    va_start(args, format);
    int result = crt286VBuffer(buffer, capacity, format, args);
    va_end(args);
    return result;
}

#include "format_engine.h"
#include <string.h>
#include <stdint.h>

class BigNumber {
public:
    static constexpr unsigned WORDS = 96;

    BigNumber() : used(0) {}

    void set(uint64_t value) {
        used = 0;
        while (value) {
            word[used++] = (uint32_t)value;
            value >>= 32;
        }
    }

    void shiftLeft(unsigned bits) {
        if (!used) {
            return;
        }
        unsigned whole = bits / 32;
        unsigned part = bits % 32;
        if (used + whole + 1 > WORDS) {
            used = 0;
            return;
        }
        if (part) {
            uint32_t carry = 0;
            for (unsigned i = 0; i < used; i++) {
                uint32_t next = word[i] >> (32 - part);
                word[i] = (word[i] << part) | carry;
                carry = next;
            }
            if (carry) {
                word[used++] = carry;
            }
        }
        if (whole) {
            for (unsigned i = used; i-- > 0;) {
                word[i + whole] = word[i];
            }
            for (unsigned i = 0; i < whole; i++) {
                word[i] = 0;
            }
            used += whole;
        }
    }

    bool shiftRight(unsigned bits) {
        unsigned whole = bits / 32;
        unsigned part = bits % 32;
        bool sticky = false;
        if (whole >= used) {
            sticky = used != 0;
            used = 0;
            return sticky;
        }
        for (unsigned i = 0; i < whole; i++) {
            sticky |= word[i] != 0;
        }
        if (part) {
            sticky |= (word[whole] & ((1u << part) - 1)) != 0;
        }
        unsigned count = used - whole;
        for (unsigned i = 0; i < count; i++) {
            uint64_t low = word[i + whole] >> part;
            uint64_t high = i + 1 < count && part ? (uint64_t)word[i + whole + 1] << (32 - part) : 0;
            word[i] = (uint32_t)(low | high);
        }
        used = count;
        trim();
        return sticky;
    }

    void multiply(uint32_t factor) {
        uint64_t carry = 0;
        for (unsigned i = 0; i < used; i++) {
            uint64_t product = (uint64_t)word[i] * factor + carry;
            word[i] = (uint32_t)product;
            carry = product >> 32;
        }
        if (carry) {
            if (used == WORDS) {
                used = 0;
                return;
            }
            word[used++] = (uint32_t)carry;
        }
    }

    uint32_t divide(uint32_t divisor) {
        uint64_t remainder = 0;
        for (unsigned i = used; i-- > 0;) {
            uint64_t current = (remainder << 32) | word[i];
            word[i] = (uint32_t)(current / divisor);
            remainder = current % divisor;
        }
        trim();
        return (uint32_t)remainder;
    }

    void multiplyPowerOfTen(unsigned count) {
        for (; count >= 9; count -= 9) {
            multiply(1000000000u);
        }
        multiply(power(count));
    }

    bool dividePowerOfTen(unsigned count) {
        bool sticky = false;
        for (; count >= 9; count -= 9) {
            sticky |= divide(1000000000u) != 0;
        }
        sticky |= divide(power(count)) != 0;
        return sticky;
    }

    unsigned toDecimal(char *out) {
        char reversed[WORDS * 10 + 10];
        unsigned count = 0;
        while (used) {
            uint32_t chunk = divide(1000000000u);
            for (int i = 0; i < 9; i++) {
                reversed[count++] = (char)('0' + chunk % 10);
                chunk /= 10;
            }
        }
        while (count > 1 && reversed[count - 1] == '0') {
            count--;
        }
        if (count == 0) {
            reversed[count++] = '0';
        }
        for (unsigned i = 0; i < count; i++) {
            out[i] = reversed[count - 1 - i];
        }
        return count;
    }

private:
    uint32_t word[WORDS];
    unsigned used;

    static uint32_t power(unsigned count) {
        uint32_t value = 1;
        while (count--) {
            value *= 10;
        }
        return value;
    }
    void trim() {
        while (used && word[used - 1] == 0) {
            used--;
        }
    }
};

class Number {
public:
    static constexpr unsigned MAX_DIGITS = BigNumber::WORDS * 10 + 10;
    static constexpr int MAX_PRECISION = 300;

    uint64_t mantissa;
    int exponent;
    bool negative;
    bool infinite;
    bool notANumber;

    explicit Number(double value) : mantissa(0), exponent(0), negative(false), infinite(false), notANumber(false) {
        uint64_t bits;
        memcpy(&bits, &value, sizeof(bits));
        negative = bits >> 63;
        int field = (int)((bits >> 52) & 0x7ff);
        uint64_t fraction = bits & ((1ull << 52) - 1);
        if (field == 0x7ff) {
            infinite = fraction == 0;
            notANumber = fraction != 0;
        } else if (field == 0) {
            mantissa = fraction;
            exponent = -1074;
        } else {
            mantissa = fraction | (1ull << 52);
            exponent = field - 1075;
        }
    }

    bool zero() const { return mantissa == 0 && !infinite && !notANumber; }

    unsigned scaled(int power, char *digits, bool &sticky) const {
        BigNumber big;
        big.set(mantissa);
        if (exponent > 0) {
            big.shiftLeft((unsigned)exponent);
        }
        if (power > 0) {
            big.multiplyPowerOfTen((unsigned)power);
        }
        sticky = false;
        if (exponent < 0) {
            sticky |= big.shiftRight((unsigned)-exponent);
        }
        if (power < 0) {
            sticky |= big.dividePowerOfTen((unsigned)-power);
        }
        return big.toDecimal(digits);
    }

    int decimalExponentGuess() const {
        int top = 63 - __builtin_clzll(mantissa);
        int binary = top + exponent;
        int guess = (binary * 30103) / 100000;
        if (binary < 0 && (binary * 30103) % 100000 != 0) {
            guess--;
        }
        return guess;
    }
};

static unsigned
roundDigits(char *digits, unsigned count, bool sticky) {
    unsigned last = digits[count - 1] - '0';
    count--;
    bool up = last > 5 || (last == 5 && (sticky || (count > 0 && ((digits[count - 1] - '0') & 1))));
    if (count == 0) {
        digits[0] = up ? '1' : '0';
        return 1;
    }
    if (up) {
        unsigned i = count;
        while (i > 0) {
            if (digits[i - 1] == '9') {
                digits[i - 1] = '0';
                i--;
            } else {
                digits[i - 1]++;
                break;
            }
        }
        if (i == 0) {
            memmove(digits + 1, digits, count);
            digits[0] = '1';
            count++;
        }
    }
    return count;
}

static unsigned
significantDigits(const Number &number, unsigned total, char *digits, int &exponent) {
    if (number.zero()) {
        for (unsigned i = 0; i < total; i++) {
            digits[i] = '0';
        }
        exponent = 0;
        return total;
    }
    int guess = number.decimalExponentGuess();
    char raw[Number::MAX_DIGITS];
    unsigned count = 0;
    bool sticky = false;
    for (int attempt = 0; attempt < 4; attempt++) {
        count = number.scaled((int)total - guess, raw, sticky);
        if (count > total + 1) {
            guess++;
        } else if (count < total + 1) {
            guess--;
        } else {
            break;
        }
    }
    count = roundDigits(raw, count, sticky);
    if (count > total) {
        guess++;
        count = total;
    }
    memcpy(digits, raw, total);
    exponent = guess;
    return total;
}

struct Spec {
    bool left;
    bool plus;
    bool space;
    bool alternate;
    bool zero;
    int width;
    int precision;
};

static void
emitPadded(PrintSink &sink, const Spec &spec, const char *prefix, size_t prefixLength,
           const char *body, size_t bodyLength, size_t extraZeros, bool allowZeroPad) {
    size_t length = prefixLength + extraZeros + bodyLength;
    size_t pad = (size_t)spec.width > length ? (size_t)spec.width - length : 0;
    char fill[32];
    if (!spec.left && !(spec.zero && allowZeroPad)) {
        for (size_t left = pad; left;) {
            size_t chunk = left < sizeof(fill) ? left : sizeof(fill);
            memset(fill, ' ', chunk);
            sink.put(fill, chunk);
            left -= chunk;
        }
    }
    if (prefixLength) {
        sink.put(prefix, prefixLength);
    }
    size_t zeros = extraZeros + (!spec.left && spec.zero && allowZeroPad ? pad : 0);
    memset(fill, '0', sizeof(fill));
    while (zeros) {
        size_t chunk = zeros < sizeof(fill) ? zeros : sizeof(fill);
        sink.put(fill, chunk);
        zeros -= chunk;
    }
    sink.put(body, bodyLength);
    if (spec.left) {
        memset(fill, ' ', sizeof(fill));
        for (size_t left = pad; left;) {
            size_t chunk = left < sizeof(fill) ? left : sizeof(fill);
            sink.put(fill, chunk);
            left -= chunk;
        }
    }
}

static void
formatInteger(PrintSink &sink, const Spec &spec, uint64_t magnitude, bool negative, char conversion) {
    char digits[72];
    unsigned count = 0;
    unsigned base = conversion == 'o' ? 8 : (conversion == 'x' || conversion == 'X' || conversion == 'p') ? 16 : 10;
    const char *alphabet = conversion == 'X' ? "0123456789ABCDEF" : "0123456789abcdef";
    do {
        digits[count++] = alphabet[magnitude % base];
        magnitude /= base;
    } while (magnitude);
    char body[72];
    for (unsigned i = 0; i < count; i++) {
        body[i] = digits[count - 1 - i];
    }
    bool isZero = count == 1 && body[0] == '0';
    size_t zeros = 0;
    if (spec.precision >= 0) {
        if ((size_t)spec.precision > count) {
            zeros = (size_t)spec.precision - count;
        }
        if (spec.precision == 0 && isZero) {
            count = 0;
        }
    }
    char prefix[3];
    size_t prefixLength = 0;
    bool isSigned = conversion == 'd' || conversion == 'i';
    if (isSigned) {
        if (negative) {
            prefix[prefixLength++] = '-';
        } else if (spec.plus) {
            prefix[prefixLength++] = '+';
        } else if (spec.space) {
            prefix[prefixLength++] = ' ';
        }
    } else if (conversion == 'p' || (spec.alternate && (conversion == 'x' || conversion == 'X') && !isZero)) {
        prefix[prefixLength++] = '0';
        prefix[prefixLength++] = conversion == 'X' ? 'X' : 'x';
    } else if (spec.alternate && conversion == 'o' && zeros == 0 && (count == 0 || body[0] != '0')) {
        zeros = 1;
    }
    emitPadded(sink, spec, prefix, prefixLength, body, count, zeros, spec.precision < 0);
}

static void
formatFloat(PrintSink &sink, Spec spec, double value, char conversion) {
    Number number(value);
    char prefix[1];
    size_t prefixLength = 0;
    if (number.negative) {
        prefix[prefixLength++] = '-';
    } else if (spec.plus) {
        prefix[prefixLength++] = '+';
    } else if (spec.space) {
        prefix[prefixLength++] = ' ';
    }
    bool upper = conversion == 'F' || conversion == 'E' || conversion == 'G';
    if (number.infinite || number.notANumber) {
        const char *text = number.infinite ? (upper ? "INF" : "inf") : (upper ? "NAN" : "nan");
        emitPadded(sink, spec, number.notANumber ? "" : prefix, number.notANumber ? 0 : prefixLength, text, 3, 0, false);
        return;
    }
    int precision = spec.precision < 0 ? 6 : spec.precision;
    int extraZeros = 0;
    if (precision > Number::MAX_PRECISION) {
        extraZeros = precision - Number::MAX_PRECISION;
        precision = Number::MAX_PRECISION;
    }
    char body[Number::MAX_DIGITS + 64];
    unsigned length = 0;
    char lower = (char)(conversion | 0x20);
    bool trimZeros = false;
    char digits[Number::MAX_DIGITS];
    int exponent = 0;
    bool useExponent = lower == 'e';
    unsigned count = 0;
    if (lower == 'g') {
        int total = precision == 0 ? 1 : precision;
        count = significantDigits(number, (unsigned)total, digits, exponent);
        useExponent = exponent < -4 || exponent >= total;
        trimZeros = !spec.alternate;
        precision = total - 1;
    } else if (lower == 'e') {
        count = significantDigits(number, (unsigned)precision + 1, digits, exponent);
    }
    if (lower == 'f') {
        char raw[Number::MAX_DIGITS];
        bool sticky = false;
        unsigned n = 1;
        raw[0] = '0';
        if (!number.zero()) {
            n = number.scaled(precision + 1, raw, sticky);
        }
        n = roundDigits(raw, n, sticky);
        while ((int)n < precision + 1) {
            memmove(raw + 1, raw, n);
            raw[0] = '0';
            n++;
        }
        unsigned integerDigits = n - (unsigned)precision;
        memcpy(body, raw, integerDigits);
        length = integerDigits;
        if (precision > 0 || spec.alternate) {
            body[length++] = '.';
        }
        memcpy(body + length, raw + integerDigits, (size_t)precision);
        length += (unsigned)precision;
    } else if (useExponent) {
        body[length++] = digits[0];
        unsigned fraction = count - 1;
        if (trimZeros) {
            while (fraction > 0 && digits[fraction] == '0') {
                fraction--;
            }
        }
        if (fraction > 0 || spec.alternate) {
            body[length++] = '.';
        }
        for (unsigned i = 1; i <= fraction; i++) {
            body[length++] = digits[i];
        }
        body[length++] = upper ? 'E' : 'e';
        int e = exponent;
        body[length++] = e < 0 ? '-' : '+';
        if (e < 0) {
            e = -e;
        }
        char buffer[8];
        unsigned digitsInExponent = 0;
        do {
            buffer[digitsInExponent++] = (char)('0' + e % 10);
            e /= 10;
        } while (e);
        if (digitsInExponent < 2) {
            buffer[digitsInExponent++] = '0';
        }
        while (digitsInExponent) {
            body[length++] = buffer[--digitsInExponent];
        }
    } else {
        unsigned integerDigits;
        if (exponent >= 0) {
            integerDigits = (unsigned)exponent + 1;
            memcpy(body, digits, integerDigits);
            length = integerDigits;
        } else {
            body[length++] = '0';
            integerDigits = 0;
        }
        unsigned fractionStart = exponent >= 0 ? integerDigits : 0;
        char fraction[Number::MAX_DIGITS];
        unsigned fractionLength = 0;
        if (exponent < 0) {
            for (int i = 0; i < -exponent - 1; i++) {
                fraction[fractionLength++] = '0';
            }
        }
        for (unsigned i = fractionStart; i < count; i++) {
            fraction[fractionLength++] = digits[i];
        }
        if (trimZeros) {
            while (fractionLength > 0 && fraction[fractionLength - 1] == '0') {
                fractionLength--;
            }
        }
        if (fractionLength > 0 || spec.alternate) {
            body[length++] = '.';
        }
        memcpy(body + length, fraction, fractionLength);
        length += fractionLength;
    }
    if (lower == 'f') {
        for (int i = 0; i < extraZeros; i++) {
            body[length++] = '0';
        }
    }
    emitPadded(sink, spec, prefix, prefixLength, body, length, 0, true);
}

int
formatTo(PrintSink &sink, const char *format, va_list arguments) {
    struct CountingSink : PrintSink {
        PrintSink &inner;
        int total;
        explicit CountingSink(PrintSink &s) : inner(s), total(0) {}
        void put(const char *text, size_t count) override {
            inner.put(text, count);
            total += (int)count;
        }
    } out(sink);

    while (*format) {
        if (*format != '%') {
            const char *end = format;
            while (*end && *end != '%') {
                end++;
            }
            out.put(format, (size_t)(end - format));
            format = end;
            continue;
        }
        format++;
        Spec spec = {false, false, false, false, false, 0, -1};
        for (;; format++) {
            if (*format == '-') {
                spec.left = true;
            } else if (*format == '+') {
                spec.plus = true;
            } else if (*format == ' ') {
                spec.space = true;
            } else if (*format == '#') {
                spec.alternate = true;
            } else if (*format == '0') {
                spec.zero = true;
            } else {
                break;
            }
        }
        if (*format == '*') {
            spec.width = va_arg(arguments, int);
            if (spec.width < 0) {
                spec.left = true;
                spec.width = -spec.width;
            }
            format++;
        } else {
            while (*format >= '0' && *format <= '9') {
                spec.width = spec.width * 10 + (*format++ - '0');
            }
        }
        if (*format == '.') {
            format++;
            spec.precision = 0;
            if (*format == '*') {
                spec.precision = va_arg(arguments, int);
                format++;
            } else {
                while (*format >= '0' && *format <= '9') {
                    spec.precision = spec.precision * 10 + (*format++ - '0');
                }
            }
        }
        int longs = 0;
        bool shortInt = false;
        bool charInt = false;
        bool longDouble = false;
        bool sizeLike = false;
        for (;; format++) {
            if (*format == 'l') {
                longs++;
            } else if (*format == 'h') {
                if (shortInt) {
                    charInt = true;
                }
                shortInt = true;
            } else if (*format == 'L') {
                longDouble = true;
            } else if (*format == 'z' || *format == 'j' || *format == 't') {
                sizeLike = true;
            } else {
                break;
            }
        }
        char conversion = *format;
        if (!conversion) {
            break;
        }
        format++;
        switch (conversion) {
        case '%':
            out.put("%", 1);
            break;
        case 'c': {
            char c = (char)va_arg(arguments, int);
            emitPadded(out, spec, "", 0, &c, 1, 0, false);
            break;
        }
        case 's': {
            const char *text = va_arg(arguments, const char *);
            if (!text) {
                text = "(null)";
            }
            size_t length = 0;
            while (text[length] && (spec.precision < 0 || (int)length < spec.precision)) {
                length++;
            }
            emitPadded(out, spec, "", 0, text, length, 0, false);
            break;
        }
        case 'd':
        case 'i': {
            long long value;
            if (longs >= 2 || sizeLike) {
                value = va_arg(arguments, long long);
            } else if (longs == 1) {
                value = va_arg(arguments, long);
            } else {
                value = va_arg(arguments, int);
                if (charInt) {
                    value = (signed char)value;
                } else if (shortInt) {
                    value = (short)value;
                }
            }
            formatInteger(out, spec, value < 0 ? 0 - (uint64_t)value : (uint64_t)value, value < 0, conversion);
            break;
        }
        case 'u':
        case 'x':
        case 'X':
        case 'o': {
            unsigned long long value;
            if (longs >= 2 || sizeLike) {
                value = va_arg(arguments, unsigned long long);
            } else if (longs == 1) {
                value = va_arg(arguments, unsigned long);
            } else {
                value = va_arg(arguments, unsigned int);
                if (charInt) {
                    value = (unsigned char)value;
                } else if (shortInt) {
                    value = (unsigned short)value;
                }
            }
            formatInteger(out, spec, value, false, conversion);
            break;
        }
        case 'p': {
            void *value = va_arg(arguments, void *);
            if (!value) {
                emitPadded(out, spec, "", 0, "(nil)", 5, 0, false);
            } else {
                formatInteger(out, spec, (uint64_t)(uintptr_t)value, false, 'p');
            }
            break;
        }
        case 'f':
        case 'F':
        case 'e':
        case 'E':
        case 'g':
        case 'G': {
            double value = longDouble ? (double)va_arg(arguments, long double) : va_arg(arguments, double);
            formatFloat(out, spec, value, conversion);
            break;
        }
        case 'n':
            *va_arg(arguments, int *) = out.total;
            break;
        default:
            out.put("%", 1);
            out.put(&conversion, 1);
            break;
        }
    }
    return out.total;
}

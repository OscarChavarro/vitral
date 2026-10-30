#include <string>
#include <stdexcept>
#include <sstream>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

namespace std {
const string::size_type string::npos;

static string
formatted(const char *format, ...) __attribute__((format(printf, 1, 2)));

static string
formatted(const char *format, ...) {
    char buffer[64];
    va_list arguments;
    va_start(arguments, format);
    vsnprintf(buffer, sizeof buffer, format, arguments);
    va_end(arguments);
    return string(buffer);
}

string
to_string(int value) { return formatted("%d", value); }
string
to_string(long value) { return formatted("%ld", value); }
string
to_string(long long value) { return formatted("%lld", value); }
string
to_string(unsigned value) { return formatted("%u", value); }
string
to_string(unsigned long value) { return formatted("%lu", value); }
string
to_string(unsigned long long value) { return formatted("%llu", value); }
string
to_string(float value) { return formatted("%f", (double)value); }
string
to_string(double value) { return formatted("%f", value); }
string
to_string(long double value) { return formatted("%f", (double)value); }

template <typename T>
static T
parsed(T (*parse)(const char *, char **, int), const string &s, size_t *end, int base) {
    char *stop;
    T value = parse(s.c_str(), &stop, base);
    if (stop == s.c_str()) {
        throw invalid_argument("stoi");
    }
    if (end) {
        *end = (size_t)(stop - s.c_str());
    }
    return value;
}

int
stoi(const string &s, size_t *end, int base) { return (int)parsed(strtol, s, end, base); }
long
stol(const string &s, size_t *end, int base) { return parsed(strtol, s, end, base); }
long long
stoll(const string &s, size_t *end, int base) { return parsed(strtoll, s, end, base); }
unsigned long
stoul(const string &s, size_t *end, int base) { return parsed(strtoul, s, end, base); }

double
stod(const string &s, size_t *end) {
    char *stop;
    double value = strtod(s.c_str(), &stop);
    if (stop == s.c_str()) {
        throw invalid_argument("stod");
    }
    if (end) {
        *end = (size_t)(stop - s.c_str());
    }
    return value;
}

float
stof(const string &s, size_t *end) { return (float)stod(s, end); }

ostringstream &
ostringstream::operator<<(double value) { return *this << formatted("%g", value); }
ostringstream &
ostringstream::operator<<(const void *p) { return *this << formatted("%p", p); }
}

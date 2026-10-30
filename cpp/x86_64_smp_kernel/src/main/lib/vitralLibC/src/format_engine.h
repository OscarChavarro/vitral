#ifndef __FORMAT_ENGINE__
#define __FORMAT_ENGINE__
#include <stddef.h>
#include <stdarg.h>

class PrintSink {
public:
    virtual void put(const char *text, size_t count) = 0;
};

int
formatTo(PrintSink &sink, const char *format, va_list arguments);
int
scanText(const char *text, const char *format, va_list arguments);

#endif

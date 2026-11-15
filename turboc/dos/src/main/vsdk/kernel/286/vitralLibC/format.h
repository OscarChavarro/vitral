#ifndef __VITRAL_286_FORMAT__
#define __VITRAL_286_FORMAT__
#include <stddef.h>
#include <stdarg.h>
typedef int (*Crt286Output)(char, void*);
int crt286VFormat(Crt286Output output, void* context, const char* format, va_list args);
extern "C" int crt286Format(char* buffer, size_t capacity, const char* format, ...);
int crt286VBuffer(char* buffer, size_t capacity, const char* format, va_list args);
#endif

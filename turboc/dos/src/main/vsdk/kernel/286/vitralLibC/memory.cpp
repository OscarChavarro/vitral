#include <stddef.h>

extern "C" void* memcpy(void* destination, const void* source, size_t count)
{
    unsigned char* d = (unsigned char*)destination;
    const unsigned char* s = (const unsigned char*)source;
    while (count--) *d++ = *s++;
    return destination;
}

extern "C" void* memmove(void* destination, const void* source, size_t count)
{
    unsigned char* d = (unsigned char*)destination;
    const unsigned char* s = (const unsigned char*)source;
    if (d > s) {
        d += count; s += count;
        while (count--) *--d = *--s;
    }
    else while (count--) *d++ = *s++;
    return destination;
}

extern "C" void* memset(void* destination, int value, size_t count)
{
    unsigned char* d = (unsigned char*)destination;
    while (count--) *d++ = (unsigned char)value;
    return destination;
}

extern "C" int memcmp(const void* left, const void* right, size_t count)
{
    const unsigned char* a = (const unsigned char*)left;
    const unsigned char* b = (const unsigned char*)right;
    while (count--) { if (*a != *b) return *a < *b ? -1 : 1; ++a; ++b; }
    return 0;
}

extern "C" size_t strlen(const char* text)
{
    size_t count = 0;
    while (text[count]) ++count;
    return count;
}

extern "C" int strcmp(const char* left, const char* right)
{
    while (*left && *left == *right) { ++left; ++right; }
    return (int)(unsigned char)*left - (int)(unsigned char)*right;
}

extern "C" char* strcpy(char* destination, const char* source)
{
    char* result = destination;
    do { *destination++ = *source; } while (*source++);
    return result;
}

extern "C" char* strcat(char* destination, const char* source)
{
    char* result = destination;
    while (*destination) ++destination;
    do { *destination++ = *source; } while (*source++);
    return result;
}

extern "C" char* strncat(char* destination, const char* source, size_t count)
{
    char* result = destination;
    while (*destination) ++destination;
    while (count && *source) {
        *destination++ = *source++;
        --count;
    }
    *destination = 0;
    return result;
}

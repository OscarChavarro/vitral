#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"

void *
Memory::copy(void *dst, const void *src, size_t n) {
    void *d = dst;
    asm volatile("rep movsb" : "+D"(d), "+S"(src), "+c"(n) : : "memory");
    return dst;
}

void *
Memory::move(void *dst, const void *src, size_t n) {
    if (dst <= src || (const char *)dst >= (const char *)src + n) {
        return copy(dst, src, n);
    }
    char *d = (char *)dst + n;
    const char *s = (const char *)src + n;
    while (n--) {
        *--d = *--s;
    }
    return dst;
}

void *
Memory::set(void *dst, int byte, size_t n) {
    void *d = dst;
    asm volatile("rep stosb" : "+D"(d), "+c"(n) : "a"(byte) : "memory");
    return dst;
}

int
Memory::compare(const void *a, const void *b, size_t n) {
    const unsigned char *x = (const unsigned char *)a;
    const unsigned char *y = (const unsigned char *)b;
    for (; n; n--, x++, y++) {
        if (*x != *y) {
            return *x - *y;
        }
    }
    return 0;
}

size_t
String::length(const char *s) {
    size_t n = 0;
    while (s[n]) {
        n++;
    }
    return n;
}

bool
String::equals(const char *a, const char *b) {
    while (*a && *a == *b) {
        a++, b++;
    }
    return *a == *b;
}

void
String::copy(char *dst, const char *src, size_t size) {
    size_t i = 0;
    for (; i + 1 < size && src[i]; i++) {
        dst[i] = src[i];
    }
    dst[i] = 0;
}

void *
runtimeMemcpy(void *d, const void *s, size_t n) asm("memcpy");
void *
runtimeMemmove(void *d, const void *s, size_t n) asm("memmove");
void *
runtimeMemset(void *d, int c, size_t n) asm("memset");
int
runtimeMemcmp(const void *a, const void *b, size_t n) asm("memcmp");

void *
runtimeMemcpy(void *d, const void *s, size_t n) { return Memory::copy(d, s, n); }
void *
runtimeMemmove(void *d, const void *s, size_t n) { return Memory::move(d, s, n); }
void *
runtimeMemset(void *d, int c, size_t n) { return Memory::set(d, c, n); }
int
runtimeMemcmp(const void *a, const void *b, size_t n) { return Memory::compare(a, b, n); }

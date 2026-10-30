#ifndef __STDLIB__
#define __STDLIB__
#include <stddef.h>
#define EXIT_SUCCESS 0
#define EXIT_FAILURE 1
#define RAND_MAX 2147483647
void *
malloc(size_t size);
void *
calloc(size_t count, size_t size);
void *
realloc(void *pointer, size_t size);
void
free(void *pointer);
void *
aligned_alloc(size_t alignment, size_t size);
int
posix_memalign(void **out, size_t alignment, size_t size);
size_t
malloc_usable_size(void *pointer);
int
abs(int value);
long
labs(long value);
long long
llabs(long long value);
struct div_t {
    int quot;
    int rem;
};
struct ldiv_t {
    long quot;
    long rem;
};
div_t
div(int numerator, int denominator);
ldiv_t
ldiv(long numerator, long denominator);
int
atoi(const char *text);
long
atol(const char *text);
long long
atoll(const char *text);
double
atof(const char *text);
double
strtod(const char *text, char **end);
float
strtof(const char *text, char **end);
long double
strtold(const char *text, char **end);
long
strtol(const char *text, char **end, int base);
long long
strtoll(const char *text, char **end, int base);
unsigned long
strtoul(const char *text, char **end, int base);
unsigned long long
strtoull(const char *text, char **end, int base);
int
rand();
void
srand(unsigned seed);
void
qsort(void *base, size_t count, size_t size, int (*compare)(const void *, const void *));
void *
bsearch(const void *key, const void *base, size_t count, size_t size,
        int (*compare)(const void *, const void *));
[[noreturn]] void
exit(int status);
[[noreturn]] void
_Exit(int status);
[[noreturn]] void
abort();
int
atexit(void (*function)());
char *
getenv(const char *name);
int
setenv(const char *name, const char *value, int overwrite);
int
unsetenv(const char *name);
int
system(const char *command);

#endif

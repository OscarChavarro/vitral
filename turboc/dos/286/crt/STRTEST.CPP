/* Host-only test for the runtime's string/integer conversion primitives. */
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <errno.h>
#include <assert.h>

int main()
{
    char* end;
    char tokens[] = ",alpha,,beta,";
    char appended[16] = "abc";
    assert(strcat(appended, "de") == appended && !strcmp(appended, "abcde"));
    assert(strncat(appended, "XYZ", 0) == appended && !strcmp(appended, "abcde"));
    assert(strncat(appended, "XYZ", 2) == appended && !strcmp(appended, "abcdeXY"));
    assert(strncat(appended, "!", 5) == appended && !strcmp(appended, "abcdeXY!"));
    assert(strtol(" -17tail", &end, 10) == -17 && !strcmp(end, "tail"));
    assert(strtol("0x2f", &end, 0) == 47 && !*end);
    assert(strtol("077", &end, 0) == 63 && !*end);
    assert(strtol("+", &end, 10) == 0 && *end == '+');
    errno = 0;
    assert(strtol("99999999999999999999999999999", &end, 10) == LONG_MAX);
    assert(errno == ERANGE && !*end);
    assert(strncmp("abcd", "abce", 3) == 0);
    assert(strncmp("abcd", "abce", 4) < 0);
    assert(strchr("abc", 0) != 0 && strrchr("abca", 'a')[1] == 0);
    assert(!strcmp(strstr("abcdef", "cde"), "cdef"));
    assert(!strcmp(strtok(tokens, ","), "alpha"));
    assert(!strcmp(strtok(0, ","), "beta"));
    assert(strtok(0, ",") == 0);
    return 0;
}

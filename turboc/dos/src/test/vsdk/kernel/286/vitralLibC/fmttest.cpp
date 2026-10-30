#include "vitralLibC/format.h"
#include <assert.h>
#include <string.h>
#include <limits.h>
#include <errno.h>
#include <stdio.h>
int main()
{
    char b[512], small[5], expected[64];
    assert(crt286Format(b, sizeof(b), "%s %d %u %ld %08X %%", "RGB", -17, 42U, -123456L, 0xABU) == 29);
    assert(!strcmp(b, "RGB -17 42 -123456 000000AB %"));
    assert(crt286Format(small, sizeof(small), "%s", "abcdef") == 6);
    assert(!strcmp(small, "abcd"));
    assert(crt286Format(0, 0, "hello") == 5);
    assert(crt286Format(b, sizeof(b), "%+.2f %.*f %.0f", -123.456, 3, .125, 0.0) > 0);
    assert(!strcmp(b, "-123.46 0.125 0"));
    assert(crt286Format(b, sizeof(b), "[%5s][%-5s][%+05d]", "x", "y", 12) > 0);
    assert(!strcmp(b, "[    x][y    ][+0012]"));
    assert(crt286Format(b, sizeof(b), "%ld", LONG_MIN) > 0);
    sprintf(expected, "%ld", LONG_MIN);
    assert(!strcmp(b, expected));
    errno = 0;
    assert(crt286Format(b, sizeof(b), "%40960d", 1) == -1 && errno == ERANGE);
    errno = 0;
    assert(crt286Format(b, sizeof(b), "%q", 1) == -1 && errno == EINVAL);
    return 0;
}

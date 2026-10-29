#include <assert.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include <float.h>
extern "C" int crt286ScanText(const char*, const char*, ...);
int main()
{
    char command[256], small[4];
    int w = 0, h = 0;
    double x = 0, y = 0, z = 0, r = 0;
    assert(crt286ScanText("sphere -2.5 .125 1e2 3", "%255s %lf %lf %lf %lf",
        command, &x, &y, &z, &r) == 5);
    assert(!strcmp(command, "sphere") && x == -2.5 && y == .125 && z == 100 && r == 3);
    assert(crt286ScanText("viewport 320 240", "%*s %d %d", &w, &h) == 2);
    assert(w == 320 && h == 240);
    assert(crt286ScanText("color 1 2 3 plastic .5 -.25 0.125",
        "%*s %*f %*f %*f %*s %lf %lf %lf", &x, &y, &z) == 3);
    assert(x == .5 && y == -.25 && z == .125);
    assert(crt286ScanText("abcdef", "%3s", small) == 1 && !strcmp(small, "abc"));
    assert(crt286ScanText("", "%lf", &x) == -1);
    assert(crt286ScanText("abc", "%lf", &x) == 0);
    x = 42;
    errno = 0;
    assert(crt286ScanText("1e9999", "%lf", &x) == 0 && errno == ERANGE && x == 42);
    assert(crt286ScanText("1e-9999", "%lf", &x) == 0 && x == 42);
    char* end;
    assert(strtod("  -.25tail", &end) == -.25 && !strcmp(end, "tail"));
    errno = 0;
    assert(strtod("1e9999", &end) > DBL_MAX && !*end && errno == ERANGE);
    const char* invalid = "  nope";
    assert(strtod(invalid, &end) == 0 && end == invalid);
    return 0;
}

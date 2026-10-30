/* Host reference check for the C++ reduction/dispatch layer. The native
 * 287 helpers themselves must also be exercised in the DOS VM. */
#include <math.h>
#include <assert.h>
#include <stdio.h>

extern "C" double crt286TangentQuarter(double x) { return (double)tanl(x); }
extern "C" double crt286AtanUnit(double x) { return (double)atanl(x); }
extern "C" double crt286Log2(double x) { return (double)log2l(x); }
extern "C" double crt286Exp2(double x) { return (double)exp2l(x); }

int main()
{
    for (int i = -20000; i <= 20000; ++i) {
        double x = i * 0.001;
        assert(fabs(sin(x) - (double)sinl(x)) < 1e-13);
        assert(fabs(cos(x) - (double)cosl(x)) < 1e-13);
        if (fabs((double)cosl(x)) > 0.01)
            assert(fabs(tan(x) - (double)tanl(x)) < 1e-9);
    }
    for (int j = -1000; j <= 1000; ++j) {
        double x = j * 0.01;
        assert(fabs(atan(x) - (double)atanl(x)) < 2e-15);
        double expected = (double)expl(x);
        assert(fabs(exp(x) - expected) < expected * 5e-15);
        double base = (j + 1001) * 0.01;
        expected = (double)powl(base, 3.3);
        assert(fabs(pow(base, 3.3) - expected) < expected * 1e-14);
    }
    assert(pow(-2.0, 3.0) == -8.0);
    assert(pow(-2.0, 4.0) == 16.0);
    assert(pow(0.0, 0.0) == 1.0);
    for (int y = -10; y <= 10; ++y)
        for (int x = -10; x <= 10; ++x)
            assert(fabs(atan2((double)y, (double)x) - (double)atan2l(y, x)) < 1e-14);
    puts("287 math reduction/reference checks passed.");
    return 0;
}

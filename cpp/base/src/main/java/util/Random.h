#ifndef __JAVA_UTIL_RANDOM__
#define __JAVA_UTIL_RANDOM__

#include <cmath>

#include "java/lang/System.h"

namespace java {

/**
Emulation of `java.util.Random`: the same 48-bit linear congruential
generator, so a given seed produces the same sequence as in the Java port.
*/
class Random {
private:
    // Unsigned arithmetic wraps as Java long arithmetic does
    static const unsigned long long MULTIPLIER = 0x5DEECE66DULL;
    static const unsigned long long ADDEND = 0xBULL;
    static const unsigned long long MASK = (1ULL << 48) - 1;

    unsigned long long seed;
    double nextNextGaussian;
    bool haveNextNextGaussian;

    static unsigned long long initialScramble(long long value)
    {
        return ((unsigned long long)value ^ MULTIPLIER) & MASK;
    }

    int next(int bits)
    {
        seed = (seed * MULTIPLIER + ADDEND) & MASK;
        return (int)(long long)(seed >> (48 - bits));
    }

public:
    Random()
        : seed(initialScramble((long long)(
              (unsigned long long)java::System::nanoTime() ^
              (8682522807148012ULL * 181783497276652981ULL)))),
          nextNextGaussian(0.0), haveNextNextGaussian(false)
    {
    }

    explicit Random(long long seedValue) : seed(initialScramble(seedValue)),
        nextNextGaussian(0.0), haveNextNextGaussian(false)
    {
    }

    void setSeed(long long seedValue)
    {
        seed = initialScramble(seedValue);
        haveNextNextGaussian = false;
    }

    int nextInt()
    {
        return next(32);
    }

    /**
    @param bound upper bound (exclusive), must be positive
    @return uniformly distributed value in [0, bound)
    */
    int nextInt(int bound)
    {
        if ( bound <= 0 ) {
            return 0;
        }
        if ( (bound & -bound) == bound ) {
            return (int)((bound * (long long)next(31)) >> 31);
        }
        int bits;
        int value;
        do {
            bits = next(31);
            value = bits % bound;
        } while ( bits - value + (bound - 1) < 0 );
        return value;
    }

    /**
    @return uniformly distributed value in [0, 1)
    */
    double nextDouble()
    {
        return (double)(((long long)next(26) << 27) + next(27)) *
            (1.0 / (double)(1LL << 53));
    }

    bool nextBoolean()
    {
        return next(1) != 0;
    }

    /**
    Gaussian value of mean 0 and deviation 1, by the polar method, as
    `java.util.Random.nextGaussian`. Java uses `StrictMath.log` and
    `StrictMath.sqrt`: `sqrt` is exact in both; `std::log` is the C
    library's, which may differ from fdlibm in the last bit.
    */
    double nextGaussian()
    {
        if ( haveNextNextGaussian ) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        }
        double v1;
        double v2;
        double s;
        do {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while ( s >= 1 || s == 0 );
        double multiplier = std::sqrt(-2 * std::log(s) / s);
        nextNextGaussian = v2 * multiplier;
        haveNextNextGaussian = true;
        return v1 * multiplier;
    }
};

}

#endif

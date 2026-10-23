#ifndef __JAVA_UTIL_RANDOM__
#define __JAVA_UTIL_RANDOM__

#include <stddef.h>

#ifdef __TURBOC__
#include <sys/timeb.h>
#else
#include <sys/time.h>
#endif
#include "vsdk/toolkit/common/VSDK.h"


/**
Emulation of `java.util.Random`: the same 48-bit linear congruential
generator, so a given seed produces the same sequence as in the Java port.
*/
class Random {
private:
    // Unsigned arithmetic wraps as Java long arithmetic does
    static const unsigned long long MULTIPLIER;
    static const unsigned long long ADDEND;
    static const unsigned long long MASK;

    unsigned long long seed;

    static unsigned long long initialScramble(long long value)
    {
        return ((unsigned long long)value ^ MULTIPLIER) & MASK;
    }

    static unsigned long long currentTimeNanoseconds()
    {
#ifdef __TURBOC__
        // DOS has no gettimeofday: millisecond resolution
        struct timeb now;
        ftime(&now);
        return (unsigned long long)now.time * VSDK_ULL(1000000000) +
            (unsigned long long)now.millitm * VSDK_ULL(1000000);
#else
        struct timeval now;
        gettimeofday(&now, NULL);
        return (unsigned long long)now.tv_sec * VSDK_ULL(1000000000) +
            (unsigned long long)now.tv_usec * VSDK_ULL(1000);
#endif
    }

    int next(int bits)
    {
        seed = (seed * MULTIPLIER + ADDEND) & MASK;
        return (int)(long long)(seed >> (48 - bits));
    }

public:
    Random()
        : seed(initialScramble((long long)(
              currentTimeNanoseconds() ^
              (VSDK_ULL(8682522807148012) * VSDK_ULL(181783497276652981)))))
    {
    }

    explicit Random(long long seedValue) : seed(initialScramble(seedValue))
    {
    }

    void setSeed(long long seedValue)
    {
        seed = initialScramble(seedValue);
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
            (1.0 / (double)(VSDK_LL(1) << 53));
    }

    BOOLEAN nextBoolean()
    {
        return next(1) != 0;
    }
};


#endif

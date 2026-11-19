#ifndef __VITRAL_TEST__
#define __VITRAL_TEST__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/Entity.h"

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "vsdk/toolkit/common/VSDK.h"

/**
Minimal replacement of the GoogleTest macros used by the tests of the cpp
port, for the compilers of the turboc port (no GoogleTest, no exceptions on
Turbo C++ 3.0). Each test program is one source file with `TEST(Suite, Name)`
cases and a `VITRAL_TEST_MAIN()` line at its end; it runs every case, prints
one line per failed expectation and returns non-zero when any failed, as
`render/dithering/DitheringTest.cpp` does.

The case names are kept as strings; the functions that implement them are
named after the source line (Turbo C++ 3.0 distinguishes identifiers by their
first 32 characters only).

- `EXPECT_*` report the failure and continue; `ASSERT_*` report it and leave
  the case (`return`), so they are only used in `void` functions.
- `EXPECT_DOUBLE_EQ` accepts 4 units in the last place, as GoogleTest.
- `EXPECT_THROW` needs exceptions: without them (`VSDK_WITHOUT_EXCEPTIONS`,
  Turbo C++ 3.0) the statement is not run and the expectation is counted
  as skipped.
- `GTEST_SKIP()` leaves the case and counts it as skipped.
*/
class VitralTestCase {
  public:
    const char *suite;
    const char *name;
    void (*function)();
    VitralTestCase *next;

    VitralTestCase(const char *s, const char *n, void (*f)());

    static VitralTestCase *&first();
    static VitralTestCase *&last();
    static long &failures();
    static long &skipped();
    static BOOLEAN &currentFailed();
    static BOOLEAN &currentSkipped();
    static int runAll(int argc, char *argv[]);
};

inline VitralTestCase *&
VitralTestCase::first()
{
    static VitralTestCase *value = NULL;
    return value;
}

inline VitralTestCase *&
VitralTestCase::last()
{
    static VitralTestCase *value = NULL;
    return value;
}

inline long &
VitralTestCase::failures()
{
    static long value = 0;
    return value;
}

inline long &
VitralTestCase::skipped()
{
    static long value = 0;
    return value;
}

inline BOOLEAN &
VitralTestCase::currentFailed()
{
    static BOOLEAN value = FALSE;
    return value;
}

inline BOOLEAN &
VitralTestCase::currentSkipped()
{
    static BOOLEAN value = FALSE;
    return value;
}

inline
VitralTestCase::VitralTestCase(const char *s, const char *n, void (*f)()) :
    suite(s), name(n), function(f), next(NULL)
{
    if ( last() == NULL ) {
        first() = this;
    }
    else {
        last()->next = this;
    }
    last() = this;
}

/**
Runs the cases; with an argument, only those whose "Suite.Name" contains it.
*/
inline int
VitralTestCase::runAll(int argc, char *argv[])
{
    long passed = 0;
    long failed = 0;
    long skippedCases = 0;
    VitralTestCase *test;
    for ( test = first(); test != NULL; test = test->next ) {
        char fullName[256];
        sprintf(fullName, "%.120s.%.120s", test->suite, test->name);
        if ( argc > 1 && strstr(fullName, argv[1]) == NULL ) {
            continue;
        }
        currentFailed() = FALSE;
        currentSkipped() = FALSE;
        printf("[ RUN      ] %s\n", fullName);
        fflush(stdout);
        test->function();
        if ( currentFailed() ) {
            printf("[  FAILED  ] %s\n", fullName);
            failed++;
        }
        else if ( currentSkipped() ) {
            printf("[  SKIPPED ] %s\n", fullName);
            skippedCases++;
        }
        else {
            printf("[       OK ] %s\n", fullName);
            passed++;
        }
        fflush(stdout);
    }
    printf("%ld passed, %ld failed, %ld skipped (%ld skipped expectations)\n",
        passed, failed, skippedCases, skipped());
    return failed == 0 ? 0 : 1;
}

inline BOOLEAN
vitralTestCheck(BOOLEAN condition, const char *file, int line, const char *text)
{
    if ( !condition ) {
        printf("%s:%d: expectation failed: %s\n", file, line, text);
        VitralTestCase::currentFailed() = TRUE;
        VitralTestCase::failures()++;
    }
    return condition;
}

inline BOOLEAN
vitralTestDoubleEquals(double a, double b)
{
    if ( a == b ) {
        return TRUE;
    }
    if ( a != a || b != b ) {
        return FALSE;
    }
    // At most 4 units in the last place (as GoogleTest): the difference is
    // compared with the spacing of the binary64 numbers near the operands
    double magnitude = fabs(a) > fabs(b) ? fabs(a) : fabs(b);
    int exponent = 0;
    frexp(magnitude, &exponent);
    double ulp = ldexp(1.0, exponent - 53);
    double smallest = ldexp(1.0, -1074);
    if ( ulp < smallest ) {
        ulp = smallest;
    }
    return fabs(a - b) <= 4.0 * ulp;
}

inline BOOLEAN
vitralTestDoubleCheck(BOOLEAN condition, const char *file, int line,
    const char *text, double a, double b)
{
    if ( !condition ) {
        printf("%s:%d: expectation failed: %s (%.17g vs %.17g)\n", file, line,
            text, a, b);
        VitralTestCase::currentFailed() = TRUE;
        VitralTestCase::failures()++;
    }
    return condition;
}

#define VT_CONCAT2(a, b) a##b
#define VT_CONCAT(a, b) VT_CONCAT2(a, b)

#define TEST(suite, name) \
    static void VT_CONCAT(vitralTest, __LINE__)(); \
    static VitralTestCase VT_CONCAT(vitralCase, __LINE__)(#suite, #name, \
        &VT_CONCAT(vitralTest, __LINE__)); \
    static void VT_CONCAT(vitralTest, __LINE__)()

#define VT_CHECK(condition, text) \
    vitralTestCheck((condition) ? TRUE : FALSE, __FILE__, __LINE__, text)
#define VT_REQUIRE(condition, text) \
    if ( !vitralTestCheck((condition) ? TRUE : FALSE, __FILE__, __LINE__, text) ) return

#define EXPECT_TRUE(a) VT_CHECK((a), #a)
#define EXPECT_FALSE(a) VT_CHECK(!(a), "!(" #a ")")
#define EXPECT_EQ(a, b) VT_CHECK((a) == (b), #a " == " #b)
#define EXPECT_NE(a, b) VT_CHECK((a) != (b), #a " != " #b)
#define EXPECT_LT(a, b) VT_CHECK((a) < (b), #a " < " #b)
#define EXPECT_LE(a, b) VT_CHECK((a) <= (b), #a " <= " #b)
#define EXPECT_GT(a, b) VT_CHECK((a) > (b), #a " > " #b)
#define EXPECT_GE(a, b) VT_CHECK((a) >= (b), #a " >= " #b)
#define EXPECT_DOUBLE_EQ(a, b) \
    vitralTestDoubleCheck(vitralTestDoubleEquals((a), (b)), __FILE__, \
        __LINE__, #a " == " #b, (a), (b))
#define EXPECT_NEAR(a, b, tolerance) \
    vitralTestDoubleCheck(fabs((double)(a) - (double)(b)) <= (tolerance), \
        __FILE__, __LINE__, #a " near " #b, (a), (b))

#define ASSERT_TRUE(a) VT_REQUIRE((a), #a)
#define ASSERT_FALSE(a) VT_REQUIRE(!(a), "!(" #a ")")
#define ASSERT_EQ(a, b) VT_REQUIRE((a) == (b), #a " == " #b)
#define ASSERT_NE(a, b) VT_REQUIRE((a) != (b), #a " != " #b)
#define ASSERT_LT(a, b) VT_REQUIRE((a) < (b), #a " < " #b)
#define ASSERT_LE(a, b) VT_REQUIRE((a) <= (b), #a " <= " #b)
#define ASSERT_GT(a, b) VT_REQUIRE((a) > (b), #a " > " #b)
#define ASSERT_GE(a, b) VT_REQUIRE((a) >= (b), #a " >= " #b)
#define ASSERT_DOUBLE_EQ(a, b) \
    if ( !vitralTestDoubleCheck(vitralTestDoubleEquals((a), (b)), __FILE__, \
        __LINE__, #a " == " #b, (a), (b)) ) return
#define ASSERT_NEAR(a, b, tolerance) \
    if ( !vitralTestDoubleCheck(fabs((double)(a) - (double)(b)) <= (tolerance), \
        __FILE__, __LINE__, #a " near " #b, (a), (b)) ) return

#ifndef VSDK_WITHOUT_EXCEPTIONS
#define EXPECT_THROW(statement, type) \
    { \
        BOOLEAN vitralThrown = FALSE; \
        try { \
            statement; \
        } \
        catch ( const type & ) { \
            vitralThrown = TRUE; \
        } \
        VT_CHECK(vitralThrown, #statement " throws " #type); \
    }
#else
#define EXPECT_THROW(statement, type) VitralTestCase::skipped()++
#endif

#define GTEST_SKIP() \
    { \
        VitralTestCase::currentSkipped() = TRUE; \
        return; \
    }

/**
JUnit's `@Tag("slow")` of the Java tests (run there by `gradle :base:slowTest`
only): the case is skipped unless the environment variable
VITRAL_SLOW_TESTS is set (`make slowcheck`), so `make check` stays fast.
*/
inline BOOLEAN
vitralTestSlowEnabled()
{
    return getenv("VITRAL_SLOW_TESTS") != NULL;
}

#define VITRAL_SLOW_TEST() \
    if ( !vitralTestSlowEnabled() ) GTEST_SKIP()

/**
C++ port note: the dynamic_cast of the cpp tests (no RTTI on legacy
compilers), as `vitralTestCast(entity, T::CLASS, (T *)0)`: the entity as a
`T *` when it is an instance of `T`, else NULL.
*/
template <class T> T *
vitralTestCast(const Entity *entity, const Class &type, T *)
{
    return entity != NULL && entity->instanceOf(type) ? (T *)entity : (T *)NULL;
}

// The rounding-mode tests need <fenv.h> (C99), that Turbo C++ 3.0 and the
// libc5 of gcc 2.7.2 lack, as Tru64 UNIX 5.1 (__osf__): there, changing the
// rounding mode always fails and the tests that need it are skipped
#if defined(__GNUC__) && __GNUC__ >= 3 && !defined(__osf__)
#include <fenv.h>
#define VITRAL_TEST_HAS_FENV
#else
#define FE_TONEAREST 0
#define FE_DOWNWARD 0x400
#define FE_UPWARD 0x800
#define FE_TOWARDZERO 0xc00
inline int fegetround() { return FE_TONEAREST; }
inline int fesetround(int mode) { return mode == FE_TONEAREST ? 0 : 1; }
#endif

// The cpp build gives the repository root (CMake); the test programs of the
// turboc port run from this folder (see the Makefile)
#ifndef VITRAL_REPOSITORY_ROOT
#define VITRAL_REPOSITORY_ROOT "../../../.."
#endif

/**
As `vitralTestTempDir()` of GoogleTest: the folder for the temporary files of
the tests, the current one (DOS has no common temporary folder).
*/
inline String
vitralTestTempDir()
{
    return String("");
}

#define VITRAL_TEST_MAIN() \
    int main(int argc, char *argv[]) \
    { \
        return VitralTestCase::runAll(argc, argv); \
    }

#endif

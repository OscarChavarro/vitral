package vsdk.toolkit;

import static org.assertj.core.api.Assertions.assertThat;

/**
Assertions with the semantics of the GoogleTest macros used by the cpp tests
that the Java tests port. Only the macros whose meaning is not a plain AssertJ
assertion live here.
*/
public final class GTestAssertions
{
    private static final long MAXIMUM_ULPS = 4;

    private GTestAssertions()
    {
    }

    /** Biased integer of `value` so that adjacent doubles differ by one
    (GoogleTest `FloatingPoint::SignAndMagnitudeToBiased`). */
    private static long biased(double value)
    {
        final long bits = Double.doubleToRawLongBits(value);
        if ( (bits & Long.MIN_VALUE) != 0 ) {
            return ~bits + 1;
        }
        return Long.MIN_VALUE | bits;
    }

    /** True when `a` and `b` are within 4 ULPs (GoogleTest `AlmostEquals`). */
    public static boolean almostEquals(double a, double b)
    {
        if ( Double.isNaN(a) || Double.isNaN(b) ) {
            return false;
        }
        final long ba = biased(a);
        final long bb = biased(b);
        final long distance = Long.compareUnsigned(ba, bb) >= 0 ? ba - bb : bb - ba;
        return Long.compareUnsigned(distance, MAXIMUM_ULPS) <= 0;
    }

    /** `EXPECT_DOUBLE_EQ(expected, actual)`. */
    public static void expectDoubleEq(double expected, double actual)
    {
        assertThat(almostEquals(expected, actual))
            .as("expected %s (bits %016x) but was %s (bits %016x)",
                expected, Double.doubleToRawLongBits(expected),
                actual, Double.doubleToRawLongBits(actual))
            .isTrue();
    }

    /** `EXPECT_DOUBLE_EQ(expected, actual) << message`. */
    public static void expectDoubleEq(double expected, double actual, String message)
    {
        assertThat(almostEquals(expected, actual))
            .as("%s: expected %s but was %s", message, expected, actual)
            .isTrue();
    }

    /** `EXPECT_NEAR(expected, actual, tolerance)`. */
    public static void expectNear(double expected, double actual, double tolerance)
    {
        assertThat(Math.abs(expected - actual) <= tolerance)
            .as("expected %s within %s but was %s", expected, tolerance, actual)
            .isTrue();
    }

    /** `EXPECT_NEAR(expected, actual, tolerance) << message`. */
    public static void expectNear(double expected, double actual, double tolerance, String message)
    {
        assertThat(Math.abs(expected - actual) <= tolerance)
            .as("%s: expected %s within %s but was %s", message, expected, tolerance, actual)
            .isTrue();
    }

    private static boolean isIntegral(Object value)
    {
        return value instanceof Integer || value instanceof Long ||
            value instanceof Short || value instanceof Byte;
    }

    /** `operator==` of the cpp test: numbers compare by value whatever
    their boxed type, other values by `equals`. */
    private static boolean gtestEquals(Object a, Object b)
    {
        if ( a instanceof Number na && b instanceof Number nb ) {
            if ( isIntegral(a) && isIntegral(b) ) {
                return na.longValue() == nb.longValue();
            }
            return na.doubleValue() == nb.doubleValue();
        }
        if ( a == null || b == null ) {
            return a == b;
        }
        return a.equals(b);
    }

    /** `EXPECT_EQ(expected, actual)` and `ASSERT_EQ`. */
    public static void expectEq(Object expected, Object actual)
    {
        assertThat(gtestEquals(expected, actual))
            .as("expected <%s> but was <%s>", expected, actual).isTrue();
    }

    /** `EXPECT_EQ(expected, actual) << message`. */
    public static void expectEq(Object expected, Object actual, String message)
    {
        assertThat(gtestEquals(expected, actual))
            .as("%s: expected <%s> but was <%s>", message, expected, actual).isTrue();
    }

    /** `EXPECT_NE(a, b)`. */
    public static void expectNe(Object a, Object b)
    {
        assertThat(gtestEquals(a, b)).as("<%s> equals <%s>", a, b).isFalse();
    }

    /** `EXPECT_NE(a, b) << message`. */
    public static void expectNe(Object a, Object b, String message)
    {
        assertThat(gtestEquals(a, b)).as("%s: <%s> equals <%s>", message, a, b).isFalse();
    }

    public static void expectTrue(boolean value)
    {
        assertThat(value).isTrue();
    }

    public static void expectTrue(boolean value, String message)
    {
        assertThat(value).as(message).isTrue();
    }

    public static void expectFalse(boolean value)
    {
        assertThat(value).isFalse();
    }

    public static void expectFalse(boolean value, String message)
    {
        assertThat(value).as(message).isFalse();
    }

    public static void expectLt(double a, double b) { assertThat(a < b).as("%s < %s", a, b).isTrue(); }
    public static void expectLt(double a, double b, String message) { assertThat(a < b).as("%s: %s < %s", message, a, b).isTrue(); }
    public static void expectLe(double a, double b) { assertThat(a <= b).as("%s <= %s", a, b).isTrue(); }
    public static void expectLe(double a, double b, String message) { assertThat(a <= b).as("%s: %s <= %s", message, a, b).isTrue(); }
    public static void expectGt(double a, double b) { assertThat(a > b).as("%s > %s", a, b).isTrue(); }
    public static void expectGt(double a, double b, String message) { assertThat(a > b).as("%s: %s > %s", message, a, b).isTrue(); }
    public static void expectGe(double a, double b) { assertThat(a >= b).as("%s >= %s", a, b).isTrue(); }
    public static void expectGe(double a, double b, String message) { assertThat(a >= b).as("%s: %s >= %s", message, a, b).isTrue(); }

    /** `EXPECT_THROW(statement, Exception)`. */
    public static void expectThrow(Class<? extends Throwable> type, org.assertj.core.api.ThrowableAssert.ThrowingCallable statement)
    {
        org.assertj.core.api.Assertions.assertThatThrownBy(statement).isInstanceOf(type);
    }

    /** `EXPECT_THROW(statement, Exception) << message`. */
    public static void expectThrow(Class<? extends Throwable> type, org.assertj.core.api.ThrowableAssert.ThrowingCallable statement, String message)
    {
        org.assertj.core.api.Assertions.assertThatThrownBy(statement).as(message).isInstanceOf(type);
    }

    /** `EXPECT_ANY_THROW(statement)`. */
    public static void expectAnyThrow(org.assertj.core.api.ThrowableAssert.ThrowingCallable statement)
    {
        org.assertj.core.api.Assertions.assertThatThrownBy(statement);
    }

    /** `EXPECT_NO_THROW(statement)`. */
    public static void expectNoThrow(org.assertj.core.api.ThrowableAssert.ThrowingCallable statement)
    {
        org.assertj.core.api.Assertions.assertThatCode(statement).doesNotThrowAnyException();
    }

    /**
    The cpp tests' `VITRAL_REPOSITORY_ROOT`: the first directory above the
    working directory holding both `cpp` and `etc`.
    */
    public static String vitralRepositoryRoot()
    {
        java.io.File directory = new java.io.File(System.getProperty("user.dir")).getAbsoluteFile();
        while ( directory != null ) {
            if ( new java.io.File(directory, "cpp").isDirectory() && new java.io.File(directory, "etc").isDirectory() ) {
                return directory.getPath();
            }
            directory = directory.getParentFile();
        }
        throw new IllegalStateException("VITRAL repository root not found above " + System.getProperty("user.dir"));
    }

    /** `testing::TempDir()`: the temporary directory, ending in a separator. */
    public static String tempDir()
    {
        String directory = System.getProperty("java.io.tmpdir");
        return directory.endsWith("/") ? directory : directory + "/";
    }

    /** `fopen(path, "w")`, `fputs(text)`, `fclose`. */
    public static void writeFile(String path, String text)
    {
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(path),
                text.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        }
        catch ( java.io.IOException e ) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** `std::remove(path)`. */
    public static void removeFile(String path)
    {
        new java.io.File(path).delete();
    }
}

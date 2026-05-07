package vsdk.toolkit.common.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.linealAlgebra.exceptions.MatrixDimensionMismatchException;
import vsdk.toolkit.common.linealAlgebra.exceptions.MatrixIndexOutOfBoundsException;
import vsdk.toolkit.common.linealAlgebra.exceptions.MatrixNotSquareException;
import vsdk.toolkit.common.linealAlgebra.exceptions.MatrixSingularException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatrixNxMTest
{
    @Test
    void given_squareMatrix_when_computingDeterminantAndInverse_then_resultsAreConsistent()
    {
        MatrixNxM matrix = new MatrixNxM(2, 2)
            .withVal(0, 0, 4.0)
            .withVal(0, 1, 7.0)
            .withVal(1, 0, 2.0)
            .withVal(1, 1, 6.0);

        double determinant = matrix.determinant();
        MatrixNxM inverse = matrix.inverse();
        MatrixNxM identity = matrix.multiply(inverse);

        assertThat(determinant).isCloseTo(10.0, org.assertj.core.data.Offset.offset(1.0e-9));
        assertThat(identity.epsilonEquals(new MatrixNxM(2, 2), 1.0e-8)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("determinantCases")
    void given_knownMatrices_when_determinant_then_matchesExpected(
        MatrixNxM matrix, double expected)
    {
        assertThat(matrix.determinant())
            .isCloseTo(expected, org.assertj.core.data.Offset.offset(1.0e-9));
    }

    private static Stream<Arguments> determinantCases()
    {
        MatrixNxM m1 = new MatrixNxM(1, 1).withVal(0, 0, 3.5);
        MatrixNxM m2 = new MatrixNxM(2, 2)
            .withVal(0, 0, 1.0).withVal(0, 1, 2.0)
            .withVal(1, 0, 3.0).withVal(1, 1, 4.0);
        MatrixNxM m3 = new MatrixNxM(3, 3)
            .withVal(0, 0, 6.0).withVal(0, 1, 1.0).withVal(0, 2, 1.0)
            .withVal(1, 0, 4.0).withVal(1, 1, -2.0).withVal(1, 2, 5.0)
            .withVal(2, 0, 2.0).withVal(2, 1, 8.0).withVal(2, 2, 7.0);

        return Stream.of(
            Arguments.of(m1, 3.5),
            Arguments.of(m2, -2.0),
            Arguments.of(m3, -306.0)
        );
    }

    @ParameterizedTest
    @MethodSource("exceptionCases")
    void given_invalidOperations_when_executing_then_throwsTypedExceptions(
        Runnable operation, Class<? extends Throwable> expected)
    {
        assertThatThrownBy(operation::run).isInstanceOf(expected);
    }

    private static Stream<Arguments> exceptionCases()
    {
        MatrixNxM twoByThree = new MatrixNxM(2, 3);
        MatrixNxM twoByTwo = new MatrixNxM(2, 2);
        MatrixNxM oneByOne = new MatrixNxM(1, 1);
        MatrixNxM nonSquare = new MatrixNxM(2, 3);
        MatrixNxM singular = new MatrixNxM(2, 2)
            .withVal(0, 0, 1.0).withVal(0, 1, 2.0)
            .withVal(1, 0, 2.0).withVal(1, 1, 4.0);

        return Stream.of(
            Arguments.of((Runnable) () -> twoByThree.multiply(twoByTwo), MatrixDimensionMismatchException.class),
            Arguments.of((Runnable) () -> oneByOne.buildMinor(0, 0), MatrixDimensionMismatchException.class),
            Arguments.of((Runnable) () -> nonSquare.determinant(), MatrixNotSquareException.class),
            Arguments.of((Runnable) () -> singular.inverse(), MatrixSingularException.class),
            Arguments.of((Runnable) () -> twoByTwo.getVal(5, 0), MatrixIndexOutOfBoundsException.class),
            Arguments.of((Runnable) () -> new MatrixNxM(0, 2), MatrixDimensionMismatchException.class)
        );
    }

    @Test
    void given_closeMatrices_when_epsilonEquals_then_itDependsOnTolerance()
    {
        MatrixNxM a = new MatrixNxM(2, 2);
        MatrixNxM b = a.withVal(1, 0, 1.0e-7);
        MatrixNxM c = new MatrixNxM(3, 3);

        assertThat(a.epsilonEquals(b, 1.0e-6)).isTrue();
        assertThat(a.epsilonEquals(b, 1.0e-9)).isFalse();
        assertThat(a.epsilonEquals(c, 1.0e-6)).isFalse();
        assertThatThrownBy(() -> a.epsilonEquals(b, -1.0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

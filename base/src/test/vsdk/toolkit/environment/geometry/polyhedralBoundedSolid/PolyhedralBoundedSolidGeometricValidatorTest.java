package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;

import static org.assertj.core.api.Assertions.assertThat;

class PolyhedralBoundedSolidGeometricValidatorTest
{
    @ParameterizedTest
    @MethodSource("coplanaritySamples")
    void given_pointsSet_when_validateFacePointsAreCoplanar_then_matchesExpected(
        ArrayList<Vector3D> points, boolean expected)
    {
        // Arrange
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forPoints(points);

        // Action
        boolean result = PolyhedralBoundedSolidGeometricValidator
            .validateFacePointsAreCoplanar(points, numericContext);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("fixtureFaces")
    void given_fixtureFace_when_validateFaceIsPlanar_then_returnsTrue(
        _PolyhedralBoundedSolidFace face)
    {
        // Arrange
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forFace(face);

        // Action
        boolean result = PolyhedralBoundedSolidGeometricValidator
            .validateFaceIsPlanar(face, numericContext);

        // Assert
        assertThat(result).isTrue();
    }

    private static Stream<Arguments> coplanaritySamples()
    {
        ArrayList<Vector3D> coplanarSquare = new ArrayList<Vector3D>();
        coplanarSquare.add(new Vector3D(0.0, 0.0, 0.0));
        coplanarSquare.add(new Vector3D(1.0, 0.0, 0.0));
        coplanarSquare.add(new Vector3D(1.0, 1.0, 0.0));
        coplanarSquare.add(new Vector3D(0.0, 1.0, 0.0));

        ArrayList<Vector3D> nonCoplanar = new ArrayList<Vector3D>();
        nonCoplanar.add(new Vector3D(0.0, 0.0, 0.0));
        nonCoplanar.add(new Vector3D(1.0, 0.0, 0.0));
        nonCoplanar.add(new Vector3D(0.0, 1.0, 0.0));
        nonCoplanar.add(new Vector3D(0.0, 0.0, 1.0));

        ArrayList<Vector3D> almostCoplanar = new ArrayList<Vector3D>();
        almostCoplanar.add(new Vector3D(0.0, 0.0, 0.0));
        almostCoplanar.add(new Vector3D(2.0, 0.0, 0.0));
        almostCoplanar.add(new Vector3D(2.0, 2.0, 1.0e-12));
        almostCoplanar.add(new Vector3D(0.0, 2.0, 0.0));

        return Stream.of(
            Arguments.of(coplanarSquare, true),
            Arguments.of(nonCoplanar, false),
            Arguments.of(almostCoplanar, true)
        );
    }

    private static Stream<Arguments> fixtureFaces()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);

        Stream.Builder<Arguments> builder = Stream.builder();
        int i;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            builder.add(Arguments.of(solid.polygonsList.get(i)));
        }
        return builder.build();
    }
}

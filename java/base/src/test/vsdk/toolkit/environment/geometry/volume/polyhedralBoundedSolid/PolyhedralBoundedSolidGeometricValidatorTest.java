package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;

import static org.assertj.core.api.Assertions.assertThat;

/**
Exercises face-planarity predicates used by the B-Rep validation layer.

<p>Traceability: [MANT1988] Ch. 13.1, face equations and plane
consistency for polyhedral faces.</p>
 */
class PolyhedralBoundedSolidGeometricValidatorTest
{
    @ParameterizedTest
    @MethodSource("coplanaritySamples")
    void given_pointsSet_when_validateFacePointsAreCoplanar_then_matchesExpected(
        ArrayList<Vector3Dd> points, boolean expected)
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
        ArrayList<Vector3Dd> coplanarSquare = new ArrayList<Vector3Dd>();
        coplanarSquare.add(new Vector3Dd(0.0, 0.0, 0.0));
        coplanarSquare.add(new Vector3Dd(1.0, 0.0, 0.0));
        coplanarSquare.add(new Vector3Dd(1.0, 1.0, 0.0));
        coplanarSquare.add(new Vector3Dd(0.0, 1.0, 0.0));

        ArrayList<Vector3Dd> nonCoplanar = new ArrayList<Vector3Dd>();
        nonCoplanar.add(new Vector3Dd(0.0, 0.0, 0.0));
        nonCoplanar.add(new Vector3Dd(1.0, 0.0, 0.0));
        nonCoplanar.add(new Vector3Dd(0.0, 1.0, 0.0));
        nonCoplanar.add(new Vector3Dd(0.0, 0.0, 1.0));

        ArrayList<Vector3Dd> almostCoplanar = new ArrayList<Vector3Dd>();
        almostCoplanar.add(new Vector3Dd(0.0, 0.0, 0.0));
        almostCoplanar.add(new Vector3Dd(2.0, 0.0, 0.0));
        almostCoplanar.add(new Vector3Dd(2.0, 2.0, 1.0e-12));
        almostCoplanar.add(new Vector3Dd(0.0, 2.0, 0.0));

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
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            builder.add(Arguments.of(solid.getPolygonsList().get(i)));
        }
        return builder.build();
    }
}

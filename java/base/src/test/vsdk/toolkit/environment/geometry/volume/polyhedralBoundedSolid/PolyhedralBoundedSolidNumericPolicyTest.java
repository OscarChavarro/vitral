package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
Validates the scale-aware tolerance policy used by B-Rep predicates.

<p>Traceability: [MANT1988] Ch. 13.1-13.2, where face equations,
containment, and intersection predicates depend on robust numerical
comparisons even though the book presents them in exact arithmetic.</p>
 */
class PolyhedralBoundedSolidNumericPolicyTest
{
    @Test
    void given_notFiniteScale_when_fromScale_then_usesMinimumScaleContext()
    {
        // Arrange
        double scale = Double.NaN;

        // Action
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context =
            PolyhedralBoundedSolidNumericPolicy.fromScale(scale);

        // Assert
        assertThat(context.modelScale()).isEqualTo(1.0);
        assertThat(context.epsilon())
            .isCloseTo(PolyhedralBoundedSolidNumericPolicy.BREP_EPSILON,
                within(1.0e-18));
        assertThat(context.bigEpsilon())
            .isCloseTo(PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON,
                within(1.0e-18));
    }

    @Test
    void given_largeScale_when_fromScale_then_scalesEpsilonsWithModelSize()
    {
        // Arrange
        double scale = 10.0;

        // Action
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context =
            PolyhedralBoundedSolidNumericPolicy.fromScale(scale);

        // Assert
        assertThat(context.modelScale()).isEqualTo(scale);
        assertThat(context.epsilon())
            .isCloseTo(PolyhedralBoundedSolidNumericPolicy.BREP_EPSILON * scale,
                within(1.0e-18));
        assertThat(context.bigEpsilon())
            .isCloseTo(PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON * scale,
                within(1.0e-18));
    }

    @Test
    void given_twoPoints_when_forPoints_then_usesBoundingDiagonalAsScale()
    {
        // Arrange
        ArrayList<Vector3D> points = new ArrayList<Vector3D>();
        points.add(new Vector3D(0.0, 0.0, 0.0));
        points.add(new Vector3D(3.0, 4.0, 0.0));

        // Action
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context =
            PolyhedralBoundedSolidNumericPolicy.forPoints(points);

        // Assert
        assertThat(context.modelScale()).isCloseTo(5.0, within(1.0e-12));
    }

    @Test
    void given_boxSolid_when_forSolid_then_usesSolidScaleAboveMinimum()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 4.0, 4.0,
                0.0, 0.0, 0.0);

        // Action
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);

        // Assert
        assertThat(context.modelScale()).isCloseTo(6.0, within(1.0e-12));
        assertThat(context.unitIntervalTolerance()).isGreaterThan(0.0);
    }
}

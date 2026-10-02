package vsdk.toolkit.environment.geometry.volume;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;

import static org.assertj.core.api.Assertions.assertThat;

/**
Exercises the classification of points against boxes (centered at the
origin) and cones (axis along Z, base at z = 0), as used by the voxelization
of solids.
 */
class BoxConeContainmentTest
{
    private static final double TOLERANCE = 0.01;

    @Test
    void given_box_when_pointsClassified_then_insideLimitAndOutsideAreDistinguished()
    {
        // Arrange
        Box box = new Box(2, 4, 6);

        // Act & Assert
        assertThat(box.doContainmentTest(new Vector3Dd(0.5, -1.5, 2.5), TOLERANCE))
            .isEqualTo(Geometry.INSIDE);
        assertThat(box.doContainmentTest(new Vector3Dd(1.0, 0, 0), TOLERANCE))
            .isEqualTo(Geometry.LIMIT);
        assertThat(box.doContainmentTest(new Vector3Dd(0, -2.005, 0), TOLERANCE))
            .isEqualTo(Geometry.LIMIT);
        assertThat(box.doContainmentTest(new Vector3Dd(0, 0, 3.1), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
        assertThat(box.doContainmentTest(new Vector3Dd(1.5, 0, 0), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
    }

    @Test
    void given_cone_when_pointsClassified_then_sideAndCapsAreRespected()
    {
        // Arrange: radius 1 at z = 0, apex at z = 2
        Cone cone = new Cone(1, 0, 2);

        // Act & Assert
        assertThat(cone.doContainmentTest(new Vector3Dd(0, 0, 1), TOLERANCE))
            .isEqualTo(Geometry.INSIDE);
        // At z = 1 the radius is 0.5
        assertThat(cone.doContainmentTest(new Vector3Dd(0.45, 0, 1), TOLERANCE))
            .isEqualTo(Geometry.INSIDE);
        assertThat(cone.doContainmentTest(new Vector3Dd(0.6, 0, 1), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
        assertThat(cone.doContainmentTest(new Vector3Dd(0, 0.5, 1), TOLERANCE))
            .isEqualTo(Geometry.LIMIT);
        assertThat(cone.doContainmentTest(new Vector3Dd(0.2, 0, 0), TOLERANCE))
            .isEqualTo(Geometry.LIMIT);
        assertThat(cone.doContainmentTest(new Vector3Dd(0, 0, -0.5), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
        assertThat(cone.doContainmentTest(new Vector3Dd(0, 0, 2.5), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
    }

    @Test
    void given_cylinder_when_pointsClassified_then_itBehavesAsAStraightTube()
    {
        // Arrange
        Cone cylinder = new Cone(1, 1, 2);

        // Act & Assert
        assertThat(cylinder.doContainmentTest(new Vector3Dd(0.9, 0, 1.9), TOLERANCE))
            .isEqualTo(Geometry.INSIDE);
        assertThat(cylinder.doContainmentTest(new Vector3Dd(0, 1.0, 1), TOLERANCE))
            .isEqualTo(Geometry.LIMIT);
        assertThat(cylinder.doContainmentTest(new Vector3Dd(0.8, 0.8, 1), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
    }
}

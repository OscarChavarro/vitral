package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static org.assertj.core.api.Assertions.assertThat;

class PolyhedralBoundedSolidQuantitativeInvisibilityTest
{
    @Test
    void given_rayEnteringBoxThroughEdge_when_measuringQuantitativeInvisibility_then_countsSinglePiercing()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0,
                0.0, 0.0, 0.0);

        int qi = solid.computeQuantitativeInvisibility(
            new Vector3D(-3.0, -2.0, 0.0),
            new Vector3D(-0.5, -0.75, 0.0));

        assertThat(qi).isEqualTo(1);
    }

    @Test
    void given_rayEnteringBoxThroughVertex_when_measuringQuantitativeInvisibility_then_countsSinglePiercing()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0,
                0.0, 0.0, 0.0);

        int qi = solid.computeQuantitativeInvisibility(
            new Vector3D(-3.0, -2.0, -2.0),
            new Vector3D(-0.5, -0.75, -0.75));

        assertThat(qi).isEqualTo(1);
    }

    @Test
    void given_raySlidingAlongBoundaryWithoutEnteringVolume_when_measuringQuantitativeInvisibility_then_itDoesNotCountTangentialContact()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0,
                0.0, 0.0, 0.0);

        int qi = solid.computeQuantitativeInvisibility(
            new Vector3D(-3.0, -2.0, -1.0),
            new Vector3D(3.0, 1.0, -1.0));

        assertThat(qi).isEqualTo(0);
    }
}

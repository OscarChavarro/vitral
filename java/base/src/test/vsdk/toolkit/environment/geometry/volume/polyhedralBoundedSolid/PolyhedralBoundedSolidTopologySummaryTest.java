package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

import static org.assertj.core.api.Assertions.assertThat;

class PolyhedralBoundedSolidTopologySummaryTest
{
    @Test
    void given_box_when_summarized_then_itHasOneSphericalShell()
    {
        PolyhedralBoundedSolid box =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                1.0, 1.0, 1.0, 0.0, 0.0, 0.0);

        PolyhedralBoundedSolidTopologySummary summary =
            PolyhedralBoundedSolidTopologySummary.from(box);

        assertThat(summary.getShellCount()).isEqualTo(1);
        assertThat(summary.getAdjustedEulerCharacteristic()).isEqualTo(2);
        assertThat(summary.isEveryFaceReachedExactlyOnce()).isTrue();
        assertThat(summary.getInvalidEdgeAdjacencyCount()).isZero();
        assertThat(summary.getShells().get(0)
            .isClosedOrientableEulerCompatible()).isTrue();
    }

    @Test
    void given_twoDisjointBoxes_when_summarized_then_eachShellHasChiTwo()
    {
        PolyhedralBoundedSolid[] pair =
            PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
        pair[0].merge(pair[1]);

        PolyhedralBoundedSolidTopologySummary summary =
            PolyhedralBoundedSolidTopologySummary.from(pair[0]);

        assertThat(summary.getShellCount()).isEqualTo(2);
        assertThat(summary.getAdjustedEulerCharacteristic()).isEqualTo(4);
        assertThat(summary.getShells())
            .extracting(
                PolyhedralBoundedSolidTopologySummary.Shell::
                    getAdjustedEulerCharacteristic)
            .containsExactly(2, 2);
    }

    @Test
    void given_throughTube_when_summarized_then_innerLoopsAdjustChiToZero()
    {
        PolyhedralBoundedSolid outer = createCylinder(2.0, 2.0, 0.0);
        PolyhedralBoundedSolid cutter = createCylinder(1.0, 2.2, -0.1);

        PolyhedralBoundedSolid tube = PolyhedralBoundedSolidModeler.setOp(
            outer, cutter, PolyhedralBoundedSolidModeler.SUBTRACT,
            false, true, false);
        PolyhedralBoundedSolidTopologySummary summary =
            PolyhedralBoundedSolidTopologySummary.from(tube);

        boolean hasFaceWithInnerLoop = false;
        for ( int i = 0; i < tube.getPolygonsList().size(); i++ ) {
            if ( tube.getPolygonsList().get(i).boundariesList.size() > 1 ) {
                hasFaceWithInnerLoop = true;
                break;
            }
        }
        assertThat(hasFaceWithInnerLoop).isTrue();
        assertThat(summary.getShellCount()).isEqualTo(1);
        assertThat(summary.getAdjustedEulerCharacteristic()).isZero();
        assertThat(summary.getShells().get(0)
            .getAdjustedEulerCharacteristic()).isZero();
        assertThat(summary.hasUniversalContradiction()).isFalse();
    }

    @Test
    void given_duplicateFaceReference_when_summarized_then_reachabilityFails()
    {
        PolyhedralBoundedSolid box =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        box.getPolygonsList().add(box.getPolygonsList().get(0));

        PolyhedralBoundedSolidTopologySummary summary =
            PolyhedralBoundedSolidTopologySummary.from(box);

        assertThat(summary.isEveryFaceReachedExactlyOnce()).isFalse();
        assertThat(summary.hasUniversalContradiction()).isTrue();
    }

    private static PolyhedralBoundedSolid createCylinder(
        double radius, double height, double z)
    {
        PolyhedralBoundedSolid solid = PolyhedralBoundedSolidModeler
            .createCircularLamina(0.0, 0.0, radius, 0.0, 24);
        Matrix4x4d sweep = new Matrix4x4d();
        sweep = sweep.translation(0.0, 0.0, height);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), sweep);
        if ( z != 0.0 ) {
            Matrix4x4d translation = new Matrix4x4d();
            translation = translation.translation(0.0, 0.0, z);
            PolyhedralBoundedSolidModeler.applyTransformation(
                solid, translation);
        }
        return solid;
    }
}

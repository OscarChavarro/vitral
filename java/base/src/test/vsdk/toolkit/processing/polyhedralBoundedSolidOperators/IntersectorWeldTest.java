package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;

/**
Acceptance tests for §4.2 of the stage-2 hardening plan: post-Generate weld
of coincident intersection vertices.

<p>These tests verify that after setOpGenerate runs and the weld pass fires,
the result solid contains no spatially coincident vertices.  The weld pass
is critical for preventing the Classify and Connect phases from seeing
duplicate nodes that share the same position.</p>

<p>Traceability: §4.2 (weldIntersectionVertices + pruneStaleVertexFaceEntries),
plan-csg-boolean-fix-stage2.md, 2026-05-14.</p>
*/
class IntersectorWeldTest
{
    /**
    Two overlapping boxes produce an intersection ring.  After the boolean
    operation completes, the result must have no two vertices at the same
    position (within bigEpsilon).
    */
    @Test
    void given_overlappingBoxes_when_union_then_resultHasNoCoincidentVertices()
    {
        PolyhedralBoundedSolid solidA =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        PolyhedralBoundedSolid solidB =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, PolyhedralBoundedSolidModeler.UNION, false);

        assertThat(result).isNotNull();

        StringBuilder msg = new StringBuilder();
        boolean noCoincident = PolyhedralBoundedSolidGeometricValidator
            .validateNoCoincidentVertices(result,
                PolyhedralBoundedSolidNumericPolicy.forSolid(result), msg);
        assertThat(noCoincident)
            .as("result must have no coincident vertices after weld: " + msg)
            .isTrue();
    }

    /**
    Subtraction of an overlapping box should also leave no coincident vertices.
    */
    @Test
    void given_overlappingBoxes_when_subtract_then_resultHasNoCoincidentVertices()
    {
        PolyhedralBoundedSolid solidA =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        PolyhedralBoundedSolid solidB =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, PolyhedralBoundedSolidModeler.SUBTRACT, false);

        assertThat(result).isNotNull();

        StringBuilder msg = new StringBuilder();
        boolean noCoincident = PolyhedralBoundedSolidGeometricValidator
            .validateNoCoincidentVertices(result,
                PolyhedralBoundedSolidNumericPolicy.forSolid(result), msg);
        assertThat(noCoincident)
            .as("result must have no coincident vertices after weld: " + msg)
            .isTrue();
    }
}

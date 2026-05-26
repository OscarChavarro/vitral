package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;

/**
Acceptance tests for §4.3 of the stage-2 hardening plan: deterministic
parametric ordering of sonea/soneb null-edge sets.

<p>The comparator for {@code _PolyhedralBoundedSolidSetOperatorNullEdge} uses
exact double comparison (no epsilon band) so that {@code Collections.sort}
produces a consistent total order.  These tests verify that running the same
boolean operation on identical solid pairs returns structurally identical
results, regardless of any traversal-order variation at the JVM level.</p>

<p>Traceability: §4.3 (exact compareTo in NullEdge), plan stage-2 2026-05-14.</p>
*/
class IntersectorParametricOrderingTest
{
    /**
    Two independent union operations on the same geometry must produce results
    with identical face, edge and vertex counts.  A non-deterministic sort
    would sometimes pair null-edges from different intersection chains,
    producing different face counts across runs.
    */
    @Test
    void given_overlappingBoxes_when_unionTwice_then_sameStructure()
    {
        PolyhedralBoundedSolid a1 =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        PolyhedralBoundedSolid b1 =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        PolyhedralBoundedSolid a2 =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        PolyhedralBoundedSolid b2 =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        PolyhedralBoundedSolid result1 = PolyhedralBoundedSolidModeler.setOp(
            a1, b1, PolyhedralBoundedSolidModeler.UNION, false);
        PolyhedralBoundedSolid result2 = PolyhedralBoundedSolidModeler.setOp(
            a2, b2, PolyhedralBoundedSolidModeler.UNION, false);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();

        assertThat(result1.getPolygonsList().size())
            .as("face count must be deterministic across runs")
            .isEqualTo(result2.getPolygonsList().size());
        assertThat(result1.getEdgesList().size())
            .as("edge count must be deterministic across runs")
            .isEqualTo(result2.getEdgesList().size());
        assertThat(result1.getVerticesList().size())
            .as("vertex count must be deterministic across runs")
            .isEqualTo(result2.getVerticesList().size());
    }

    /**
    Same determinism check for subtraction, which has a different connect path
    and is more sensitive to null-edge ordering in the chain traversal.
    */
    @Test
    void given_overlappingBoxes_when_subtractTwice_then_sameStructure()
    {
        PolyhedralBoundedSolid a1 =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        PolyhedralBoundedSolid b1 =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        PolyhedralBoundedSolid a2 =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        PolyhedralBoundedSolid b2 =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        PolyhedralBoundedSolid result1 = PolyhedralBoundedSolidModeler.setOp(
            a1, b1, PolyhedralBoundedSolidModeler.SUBTRACT, false);
        PolyhedralBoundedSolid result2 = PolyhedralBoundedSolidModeler.setOp(
            a2, b2, PolyhedralBoundedSolidModeler.SUBTRACT, false);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();

        assertThat(result1.getPolygonsList().size())
            .as("face count must be deterministic across runs")
            .isEqualTo(result2.getPolygonsList().size());
        assertThat(result1.getEdgesList().size())
            .as("edge count must be deterministic across runs")
            .isEqualTo(result2.getEdgesList().size());
        assertThat(result1.getVerticesList().size())
            .as("vertex count must be deterministic across runs")
            .isEqualTo(result2.getVerticesList().size());
    }
}

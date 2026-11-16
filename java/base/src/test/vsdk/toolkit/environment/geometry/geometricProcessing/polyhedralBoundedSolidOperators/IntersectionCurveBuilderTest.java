package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static org.assertj.core.api.Assertions.assertThat;

/**
Audits {@code _PolyhedralBoundedSolidSetIntersectionCurveBuilder} (§5 of
doc/mythosPlan.md): the reconstruction of intersection curves from the
classifier's paired null-edge lists, captured by the connect stage in
{@code _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport}.

<p>Traceability: [MANT1988] §15.7 — the connect stage implicitly requires
null edges ordered along each intersection curve; these tests make the curve
structure observable and pin the structural expectations measured on
2026-06-10 (mythosPlan Phase 1 baseline) for reference fixtures, the
Kurlander star canonical case, and representative moon cases — including the
moons whose A-B subtraction currently produces an EMPTY result. The key
measured fact: <b>the curves of the failing moons are cleanly closed</b>,
so a curve-traversal order exists and the EMPTY failures are an ordering
problem in connect, not a missing-geometry problem in generate.</p>
*/
public class IntersectionCurveBuilderTest
{
    private static _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report
    runAndReport(PolyhedralBoundedSolid a, PolyhedralBoundedSolid b, int op)
    {
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport = null;
        PolyhedralBoundedSolidModeler.setOp(a, b, op, false);
        return _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport;
    }

    private static _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report
    reportForCorpus(CsgSampleCorpus sample, int op)
    {
        PolyhedralBoundedSolid[] pair = CsgSampleCorpusFixtures
            .createPair(sample);
        return runAndReport(pair[0], pair[1], op);
    }

    private static _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report
    reportForKurlanderMotif(int motifIndex)
    {
        PolyhedralBoundedSolid[] pair = CsgKurlanderBowlFixture
            .createBowlAndFirstStarOperands(motifIndex);
        return runAndReport(pair[0], pair[1],
            PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    @Test
    public void given_touchingOnlyPair_when_subtracting_then_noCurveReportIsCaptured()
    {
        _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report report;

        // STACKED_BLOCKS is resolved by the touching-only preflight before
        // the connect stage runs, so no curve report is captured for it.
        report = reportForCorpus(CsgSampleCorpus.STACKED_BLOCKS,
            PolyhedralBoundedSolidModeler.SUBTRACT);
        assertThat(report).isNull();
    }

    @Test
    public void given_mant1988_15_1_when_subtracting_then_vertexContactsAreIsolatedNodes()
    {
        _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report report;

        // [MANT1988] Fig. 15.1: the pyramid touches the block at vertex
        // contacts. The two grazing contacts produce null edges with no
        // curve neighbor — the builder isolates them instead of forcing
        // them into a curve (these are the same contacts that the connect
        // stage historically reported as looseA=4 while still producing a
        // correct result; see plan-csg-boolean-fix-stage2 §7.1).
        report = reportForCorpus(CsgSampleCorpus.MANT1988_15_1,
            PolyhedralBoundedSolidModeler.SUBTRACT);
        assertThat(report).isNotNull();
        assertThat(report.nodeCount).isEqualTo(10);
        assertThat(report.cycles).hasSize(2);
        assertThat(report.cycles.get(0).length).isEqualTo(4);
        assertThat(report.cycles.get(1).length).isEqualTo(4);
        assertThat(report.isolatedNodes).hasSize(2);
        assertThat(report.openChains).isEmpty();
        assertThat(report.pinchNodes).isEmpty();
        assertThat(report.isCleanlyClosed()).isFalse();
    }

    @Test
    public void given_hollowBrick_when_intersecting_then_twoCleanCyclesAreRecovered()
    {
        _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report report;

        // The hollow-brick intersection is the canonical multi-curve case
        // that motivated ring grouping: two separate intersection loops.
        report = reportForCorpus(CsgSampleCorpus.HOLLOW_BRICK,
            PolyhedralBoundedSolidModeler.INTERSECTION);
        assertThat(report).isNotNull();
        assertThat(report.nodeCount).isEqualTo(8);
        assertThat(report.cycles).hasSize(2);
        assertThat(report.isCleanlyClosed()).isTrue();
    }

    @Test
    public void given_moonBlockCylinders_when_subtracting_then_singleCleanCycleIsRecovered()
    {
        _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report report;

        report = reportForCorpus(CsgSampleCorpus.MOON_BLOCK,
            PolyhedralBoundedSolidModeler.SUBTRACT);
        assertThat(report).isNotNull();
        assertThat(report.nodeCount).isEqualTo(34);
        assertThat(report.cycles).hasSize(1);
        assertThat(report.cycles.get(0).length).isEqualTo(34);
        assertThat(report.isCleanlyClosed()).isTrue();
    }

    @Test
    public void given_kurlanderStarMotifs_when_subtracting_then_curvesAreCleanlyClosed()
    {
        int[] starMotifs = { 0, 5 };
        int i;

        for ( i = 0; i < starMotifs.length; i++ ) {
            _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report report;

            report = reportForKurlanderMotif(starMotifs[i]);
            String label = CsgKurlanderBowlFixture
                .describeSingleMotif(starMotifs[i]);
            assertThat(report).as(label).isNotNull();
            assertThat(report.nodeCount).as(label).isEqualTo(24);
            assertThat(report.cycles).as(label).hasSize(2);
            assertThat(report.cycles.get(0).length).as(label).isEqualTo(12);
            assertThat(report.cycles.get(1).length).as(label).isEqualTo(12);
            assertThat(report.isCleanlyClosed()).as(label).isTrue();
        }
    }

    @Test
    public void given_alreadyValidEmissionOrder_when_computingTraversalOrder_then_permutationIsIdentity()
    {
        java.util.ArrayList<int[]> cycles = new java.util.ArrayList<int[]>();

        // Two contiguous cycles, each already traversed from its first
        // emitted node toward its second: the canonical star-motif shape.
        cycles.add(new int[] { 0, 1, 2, 3 });
        cycles.add(new int[] { 4, 5, 6 });
        int[] permutation = _PolyhedralBoundedSolidSetIntersectionCurveBuilder
            .computeTraversalOrder(cycles, 7);
        assertThat(permutation).containsExactly(0, 1, 2, 3, 4, 5, 6);
    }

    @Test
    public void given_rotatedCycles_when_computingTraversalOrder_then_orderIsRotatedNotReversed()
    {
        java.util.ArrayList<int[]> cycles;
        int[] permutation;

        // Cycle stored as 2 -> 1 -> 0 -> 3 -> (2). Rotated to start at 0,
        // stored direction preserved: 0, 3, 2, 1.
        cycles = new java.util.ArrayList<int[]>();
        cycles.add(new int[] { 2, 1, 0, 3 });
        permutation = _PolyhedralBoundedSolidSetIntersectionCurveBuilder
            .computeTraversalOrder(cycles, 4);
        assertThat(permutation).containsExactly(0, 3, 2, 1);

        // Cycle stored as 1 -> 3 -> 0 -> 2 -> (1): rotated to 0, stored
        // direction: 0, 2, 1, 3.
        cycles = new java.util.ArrayList<int[]>();
        cycles.add(new int[] { 1, 3, 0, 2 });
        permutation = _PolyhedralBoundedSolidSetIntersectionCurveBuilder
            .computeTraversalOrder(cycles, 4);
        assertThat(permutation).containsExactly(0, 2, 1, 3);

        // Cycles are emitted by ascending minimum member: the cycle holding
        // node 0 goes first even when given last.
        cycles = new java.util.ArrayList<int[]>();
        cycles.add(new int[] { 5, 4, 6 });
        cycles.add(new int[] { 3, 2, 0, 1 });
        permutation = _PolyhedralBoundedSolidSetIntersectionCurveBuilder
            .computeTraversalOrder(cycles, 7);
        assertThat(permutation).containsExactly(0, 1, 3, 2, 4, 6, 5);
    }

    @Test
    public void given_incompleteCycleCover_when_computingTraversalOrder_then_orderIsRejected()
    {
        java.util.ArrayList<int[]> cycles;

        // Missing node 3 of 4: not a complete cover, no order is computed.
        cycles = new java.util.ArrayList<int[]>();
        cycles.add(new int[] { 0, 1, 2 });
        assertThat(_PolyhedralBoundedSolidSetIntersectionCurveBuilder
            .computeTraversalOrder(cycles, 4)).isNull();

        // Duplicate node: rejected.
        cycles = new java.util.ArrayList<int[]>();
        cycles.add(new int[] { 0, 1, 2, 1 });
        assertThat(_PolyhedralBoundedSolidSetIntersectionCurveBuilder
            .computeTraversalOrder(cycles, 4)).isNull();
    }

    @Test
    public void given_kurlanderMoonMotifs_when_subtracting_then_curvesAreCleanlyClosed()
    {
        // Includes moons whose A-B currently produces EMPTY (21, 23, 24):
        // their intersection curves DO close — the defect is downstream,
        // in the connect-stage processing order (mythosPlan §4 R1).
        int[] moonMotifs = { 20, 21, 23, 24 };
        int i;

        for ( i = 0; i < moonMotifs.length; i++ ) {
            _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report report;

            report = reportForKurlanderMotif(moonMotifs[i]);
            String label = CsgKurlanderBowlFixture
                .describeSingleMotif(moonMotifs[i]);
            assertThat(report).as(label).isNotNull();
            assertThat(report.cycles).as(label).hasSize(2);
            assertThat(report.openChains).as(label).isEmpty();
            assertThat(report.isolatedNodes).as(label).isEmpty();
            assertThat(report.pinchNodes).as(label).isEmpty();
            assertThat(report.isCleanlyClosed()).as(label).isTrue();
        }
    }
}

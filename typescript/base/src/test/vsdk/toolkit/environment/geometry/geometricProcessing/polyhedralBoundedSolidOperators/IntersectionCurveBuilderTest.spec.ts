import { describe, expect, it } from "vitest";

import { ArrayList } from "java/util/ArrayList.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgKurlanderBowlFixture } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.js";
import { _PolyhedralBoundedSolidSetIntersectionCurveBuilder } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersectionCurveBuilder.js";
import { _PolyhedralBoundedSolidSetNullEdgesConnector } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";

type Report = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report;

/**
Harness accommodation: JUnit imposes no per-test time limit, while vitest
defaults to 5 s. The Kurlander bowl fixtures build spheres, cylinders and
motifs through dozens of boolean operations, so the motif cases get an
explicit budget instead of the default.
*/
const KURLANDER_TIMEOUT_MS = 600_000;

/**
Audits `_PolyhedralBoundedSolidSetIntersectionCurveBuilder` (§5 of
doc/mythosPlan.md): the reconstruction of intersection curves from the
classifier's paired null-edge lists, captured by the connect stage in
`_PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport`.

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
describe("IntersectionCurveBuilderTest", () => {
    function runAndReport(a: PolyhedralBoundedSolid, b: PolyhedralBoundedSolid, op: number): Report | null {
        _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport = null;
        PolyhedralBoundedSolidModeler.setOp(a, b, op, false);
        return _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport;
    }

    function reportForCorpus(sample: CsgSampleCorpus, op: number): Report | null {
        const pair = CsgSampleCorpusFixtures.createPair(sample);
        return runAndReport(pair[0]!, pair[1]!, op);
    }

    function reportForKurlanderMotif(motifIndex: number): Report | null {
        const pair = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motifIndex);
        return runAndReport(pair[0]!, pair[1]!, PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    it("given_touchingOnlyPair_when_subtracting_then_noCurveReportIsCaptured", () => {
        // STACKED_BLOCKS is resolved by the touching-only preflight before
        // the connect stage runs, so no curve report is captured for it.
        const report = reportForCorpus(CsgSampleCorpus.STACKED_BLOCKS, PolyhedralBoundedSolidModeler.SUBTRACT);
        expect(report).toBeNull();
    });

    it("given_mant1988_15_1_when_subtracting_then_vertexContactsAreIsolatedNodes", () => {
        // [MANT1988] Fig. 15.1: the pyramid touches the block at vertex
        // contacts. The two grazing contacts produce null edges with no
        // curve neighbor — the builder isolates them instead of forcing
        // them into a curve (these are the same contacts that the connect
        // stage historically reported as looseA=4 while still producing a
        // correct result; see plan-csg-boolean-fix-stage2 §7.1).
        const report = reportForCorpus(CsgSampleCorpus.MANT1988_15_1, PolyhedralBoundedSolidModeler.SUBTRACT);
        expect(report).not.toBeNull();
        expect(report!.nodeCount).toBe(10);
        expect(report!.cycles.size()).toBe(2);
        expect(report!.cycles.get(0).length).toBe(4);
        expect(report!.cycles.get(1).length).toBe(4);
        expect(report!.isolatedNodes.size()).toBe(2);
        expect(report!.openChains.isEmpty()).toBe(true);
        expect(report!.pinchNodes.isEmpty()).toBe(true);
        expect(report!.isCleanlyClosed()).toBe(false);
    });

    it("given_hollowBrick_when_intersecting_then_twoCleanCyclesAreRecovered", () => {
        // The hollow-brick intersection is the canonical multi-curve case
        // that motivated ring grouping: two separate intersection loops.
        const report = reportForCorpus(CsgSampleCorpus.HOLLOW_BRICK, PolyhedralBoundedSolidModeler.INTERSECTION);
        expect(report).not.toBeNull();
        expect(report!.nodeCount).toBe(8);
        expect(report!.cycles.size()).toBe(2);
        expect(report!.isCleanlyClosed()).toBe(true);
    });

    it("given_moonBlockCylinders_when_subtracting_then_singleCleanCycleIsRecovered", () => {
        const report = reportForCorpus(CsgSampleCorpus.MOON_BLOCK, PolyhedralBoundedSolidModeler.SUBTRACT);
        expect(report).not.toBeNull();
        expect(report!.nodeCount).toBe(34);
        expect(report!.cycles.size()).toBe(1);
        expect(report!.cycles.get(0).length).toBe(34);
        expect(report!.isCleanlyClosed()).toBe(true);
    });

    it(
        "given_kurlanderStarMotifs_when_subtracting_then_curvesAreCleanlyClosed",
        () => {
            const starMotifs = [0, 5];
            let i: number;

            for (i = 0; i < starMotifs.length; i++) {
                const report = reportForKurlanderMotif(starMotifs[i]!);
                const label = CsgKurlanderBowlFixture.describeSingleMotif(starMotifs[i]!);
                expect(report, label).not.toBeNull();
                expect(report!.nodeCount, label).toBe(24);
                expect(report!.cycles.size(), label).toBe(2);
                expect(report!.cycles.get(0).length, label).toBe(12);
                expect(report!.cycles.get(1).length, label).toBe(12);
                expect(report!.isCleanlyClosed(), label).toBe(true);
            }
        },
        KURLANDER_TIMEOUT_MS,
    );

    it("given_alreadyValidEmissionOrder_when_computingTraversalOrder_then_permutationIsIdentity", () => {
        const cycles = new ArrayList<number[]>();

        // Two contiguous cycles, each already traversed from its first
        // emitted node toward its second: the canonical star-motif shape.
        cycles.add([0, 1, 2, 3]);
        cycles.add([4, 5, 6]);
        const permutation = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.computeTraversalOrder(cycles, 7);
        expect(permutation).toEqual([0, 1, 2, 3, 4, 5, 6]);
    });

    it("given_rotatedCycles_when_computingTraversalOrder_then_orderIsRotatedNotReversed", () => {
        let cycles: ArrayList<number[]>;
        let permutation: number[] | null;

        // Cycle stored as 2 -> 1 -> 0 -> 3 -> (2). Rotated to start at 0,
        // stored direction preserved: 0, 3, 2, 1.
        cycles = new ArrayList<number[]>();
        cycles.add([2, 1, 0, 3]);
        permutation = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.computeTraversalOrder(cycles, 4);
        expect(permutation).toEqual([0, 3, 2, 1]);

        // Cycle stored as 1 -> 3 -> 0 -> 2 -> (1): rotated to 0, stored
        // direction: 0, 2, 1, 3.
        cycles = new ArrayList<number[]>();
        cycles.add([1, 3, 0, 2]);
        permutation = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.computeTraversalOrder(cycles, 4);
        expect(permutation).toEqual([0, 2, 1, 3]);

        // Cycles are emitted by ascending minimum member: the cycle holding
        // node 0 goes first even when given last.
        cycles = new ArrayList<number[]>();
        cycles.add([5, 4, 6]);
        cycles.add([3, 2, 0, 1]);
        permutation = _PolyhedralBoundedSolidSetIntersectionCurveBuilder.computeTraversalOrder(cycles, 7);
        expect(permutation).toEqual([0, 1, 3, 2, 4, 6, 5]);
    });

    it("given_incompleteCycleCover_when_computingTraversalOrder_then_orderIsRejected", () => {
        let cycles: ArrayList<number[]>;

        // Missing node 3 of 4: not a complete cover, no order is computed.
        cycles = new ArrayList<number[]>();
        cycles.add([0, 1, 2]);
        expect(_PolyhedralBoundedSolidSetIntersectionCurveBuilder.computeTraversalOrder(cycles, 4)).toBeNull();

        // Duplicate node: rejected.
        cycles = new ArrayList<number[]>();
        cycles.add([0, 1, 2, 1]);
        expect(_PolyhedralBoundedSolidSetIntersectionCurveBuilder.computeTraversalOrder(cycles, 4)).toBeNull();
    });

    it(
        "given_kurlanderMoonMotifs_when_subtracting_then_curvesAreCleanlyClosed",
        () => {
            // Includes moons whose A-B currently produces EMPTY (21, 23, 24):
            // their intersection curves DO close — the defect is downstream,
            // in the connect-stage processing order (mythosPlan §4 R1).
            const moonMotifs = [20, 21, 23, 24];
            let i: number;

            for (i = 0; i < moonMotifs.length; i++) {
                const report = reportForKurlanderMotif(moonMotifs[i]!);
                const label = CsgKurlanderBowlFixture.describeSingleMotif(moonMotifs[i]!);
                expect(report, label).not.toBeNull();
                expect(report!.cycles.size(), label).toBe(2);
                expect(report!.openChains.isEmpty(), label).toBe(true);
                expect(report!.isolatedNodes.isEmpty(), label).toBe(true);
                expect(report!.pinchNodes.isEmpty(), label).toBe(true);
                expect(report!.isCleanlyClosed(), label).toBe(true);
            }
        },
        KURLANDER_TIMEOUT_MS,
    );
});

import { describe, expect, it } from "vitest";

import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { StringBuilder } from "java/lang/StringBuilder.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidGeometricValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgKurlanderBowlFixture } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.js";
import { _PolyhedralBoundedSolidSetIntersector } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersector.js";
import { TopologicalSummary } from "./BooleansFromReferenceObjectPairsTestSupport.js";
import { yieldToEventLoop } from "./_HarnessEventLoopYield.js";

/**
Harness accommodation: JUnit has no per-test time budget; every case rebuilds
the Kurlander bowl and runs four boolean operations on it.
*/
const KURLANDER_TIMEOUT_MS = 1_800_000;

/**
40x4 diagnostic of the Kurlander sweep: for the 40 motifs x 4 operations
(A-B, B-A, A∩B, A+B), classify each result as OK / EMPTY / INVALID /
BLACK_FACES / EXCEPTION and document the `TopologicalSummary` when the
result is valid.

<p>See `doc/plan-csg-boolean-fix-stage3.md` §6 for the full workflow.</p>

<p>The permanent regression test for motif 0 is in
`given_kurlanderBowlAndMotif0_when_allFourOps_then_topologyMatchesBaseline`.
The diagnostic test is skipped (Java `@Disabled`) so it does not run as part
of the regular suite; enable it explicitly by un-skipping it.</p>
*/
describe("KurlanderMotif4OperationMatrixTest", () => {
    /** Java's private `OpStatus` enum. */
    enum OpStatus {
        OK,
        EMPTY,
        INVALID,
        BLACK_FACES,
        EXCEPTION,
    }

    const OP_NAMES = ["A-B", "B-A", "AiB", "A+B"];

    /**
    Execution control for the parameterized test: one boolean per motif (index 0-39).
    `true` = the motif is known to be 4-OK and the assertion runs.
    `false` = the motif still fails in some operation; it is skipped.

    Current 4-OK motifs (plan-csg-boolean-fix-stage3 §14.2):
      ✅ shellCount=2: 0, 2, 10, 15, 21
      ⚠️ shellCount=1: 1, 5, 7, 12, 14, 23
    */
    const ENABLED = [
        //   0      1      2      3      4      5      6      7      8      9
        true,
        true,
        true,
        false,
        false,
        true,
        false,
        true,
        false,
        false,
        //  10     11     12     13     14     15     16     17     18     19
        true,
        false,
        true,
        false,
        true,
        true,
        false,
        false,
        false,
        false,
        //  20     21     22     23     24     25     26     27     28     29
        false,
        true,
        false,
        true,
        false,
        false,
        false,
        false,
        false,
        false,
        //  30     31     32     33     34     35     36     37     38     39
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
    ];

    /**
    Execute the operation indicated by `opIdx`:
    0 = A-B (SUBTRACT), 1 = B-A (reverse SUBTRACT),
    2 = A∩B (INTERSECTION), 3 = A+B (UNION).
    */
    function executeOp(
        bowl: PolyhedralBoundedSolid,
        motif: PolyhedralBoundedSolid,
        opIdx: number,
    ): PolyhedralBoundedSolid {
        switch (opIdx) {
            case 0:
                return PolyhedralBoundedSolidModeler.setOp(bowl, motif, PolyhedralBoundedSolidModeler.SUBTRACT, false);
            case 1:
                return PolyhedralBoundedSolidModeler.setOp(motif, bowl, PolyhedralBoundedSolidModeler.SUBTRACT, false);
            case 2:
                return PolyhedralBoundedSolidModeler.setOp(
                    bowl,
                    motif,
                    PolyhedralBoundedSolidModeler.INTERSECTION,
                    false,
                );
            case 3:
                return PolyhedralBoundedSolidModeler.setOp(bowl, motif, PolyhedralBoundedSolidModeler.UNION, false);
            default:
                throw new IllegalArgumentException("opIdx must be 0-3, got: " + opIdx);
        }
    }

    function classify(result: PolyhedralBoundedSolid | null): OpStatus {
        if (result === null || result.getPolygonsList().size() === 0) {
            return OpStatus.EMPTY;
        }

        let topOK: boolean;
        try {
            topOK = PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        } catch {
            return OpStatus.INVALID;
        }
        if (!topOK) {
            return OpStatus.INVALID;
        }

        const msg = new StringBuilder();
        let orientOK: boolean;
        try {
            orientOK = PolyhedralBoundedSolidGeometricValidator.validateConsistentFaceOrientations(result, msg);
        } catch {
            orientOK = true;
        }

        return orientOK ? OpStatus.OK : OpStatus.BLACK_FACES;
    }

    function allMotifIndices(): number[] {
        const indices: number[] = [];
        for (let i = 0; i < CsgKurlanderBowlFixture.getSingleMotifCount(); i++) {
            indices.push(i);
        }
        return indices;
    }

    let ops: PolyhedralBoundedSolid[];
    let result: PolyhedralBoundedSolid;
    let summary: TopologicalSummary;

    it(
        "given_kurlanderBowlAndMotif0_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif0AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.shellCount, "B-A must produce 2 shells").toBe(2);
            expect(summary.equals(expectedMotif0BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif0AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif0ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif1_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(1);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif1AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(1);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif1BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(1);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif1AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(1);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif1ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif2_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(2);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif2AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(2);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.shellCount, "B-A must produce 2 shells").toBe(2);
            expect(summary.equals(expectedMotif2BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(2);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif2AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(2);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif2ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif5_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(5);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif5AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(5);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif5BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(5);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif5AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(5);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif5ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif7_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(7);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif7AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(7);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif7BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(7);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif7AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(7);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif7ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif10_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(10);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif10AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(10);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.shellCount, "B-A must produce 2 shells").toBe(2);
            expect(summary.equals(expectedMotif10BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(10);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif10AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(10);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif10ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif12_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(12);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif12AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(12);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif12BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(12);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif12AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(12);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif12ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif14_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(14);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif14AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(14);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif14BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(14);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif14AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(14);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif14ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif15_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(15);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif15AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(15);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.shellCount, "B-A must produce 2 shells").toBe(2);
            expect(summary.equals(expectedMotif15BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(15);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif15AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(15);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif15ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif21_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(21);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif21AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(21);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.shellCount, "B-A must produce 2 shells").toBe(2);
            expect(summary.equals(expectedMotif21BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(21);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif21AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(21);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif21ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndMotif23_when_allFourOps_then_topologyMatchesBaseline",
        async () => {
            await yieldToEventLoop();
            // A-B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
            result = executeOp(ops[0]!, ops[1]!, 0);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif23AB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // B-A
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
            result = executeOp(ops[0]!, ops[1]!, 1);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif23BA()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A∩B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
            result = executeOp(ops[0]!, ops[1]!, 2);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif23AiB()), `${summary}`).toBe(true);

            await yieldToEventLoop();
            // A+B
            ops = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(23);
            result = executeOp(ops[0]!, ops[1]!, 3);
            summary = TopologicalSummary.from(result);
            expect(summary.equals(expectedMotif23ApB()), `${summary}`).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );
    function expectedMotif0AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            203,
            418,
            216,
            204,
            1,
            1,
            [203],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 9, 9, 9, 9, 16, 16,
            ],
            [-1000000, -993462, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif0BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            30,
            72,
            46,
            30,
            0,
            4,
            [15, 15],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10],
            [-190211, -1150000, 738197, 190211, -600000, 1100000],
        );
    }

    function expectedMotif0AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            18,
            42,
            26,
            18,
            0,
            2,
            [18],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 7, 7, 7, 7],
            [-190211, -1000000, 738197, 190211, -874541, 1100000],
        );
    }

    function expectedMotif0ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            215,
            448,
            236,
            216,
            1,
            3,
            [215],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 9, 9,
                9, 9, 10, 10, 16, 16,
            ],
            [-1000000, -1150000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif1AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            203,
            418,
            216,
            204,
            1,
            1,
            [203],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif1BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            30,
            72,
            46,
            30,
            0,
            4,
            [15, 15],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 10, 10],
            [-615818, -1135252, 488197, -53878, -481537, 850000],
        );
    }

    function expectedMotif1AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            18,
            42,
            26,
            18,
            0,
            2,
            [18],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6],
            [-521999, -908753, 488197, -151400, -652765, 850000],
        );
    }

    function expectedMotif1ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            215,
            448,
            236,
            216,
            1,
            3,
            [215],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7,
                8, 8, 10, 10, 16, 16,
            ],
            [-1000000, -1135252, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif2AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            203,
            418,
            216,
            204,
            1,
            1,
            [203],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif2BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            30,
            72,
            46,
            30,
            0,
            4,
            [15, 15],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 10, 10],
            [-947673, -947673, 1238197, -289764, -289764, 1600000],
        );
    }

    function expectedMotif2AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            18,
            42,
            26,
            18,
            0,
            2,
            [18],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6],
            [-740195, -740195, 1238197, -412942, -412942, 1600000],
        );
    }

    function expectedMotif2ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            215,
            448,
            236,
            216,
            1,
            3,
            [215],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7,
                8, 8, 10, 10, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif5AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            203,
            418,
            216,
            204,
            1,
            1,
            [203],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 9, 9, 9, 9, 16, 16,
            ],
            [-993462, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif5BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            30,
            72,
            46,
            30,
            0,
            4,
            [15, 15],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10],
            [-1150000, -190211, 738197, -600000, 190211, 1100000],
        );
    }

    function expectedMotif5AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            18,
            42,
            26,
            18,
            0,
            2,
            [18],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 7, 7, 7, 7],
            [-1000000, -190211, 738197, -874541, 190211, 1100000],
        );
    }

    function expectedMotif5ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            215,
            448,
            236,
            216,
            1,
            3,
            [215],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 9, 9,
                9, 9, 10, 10, 16, 16,
            ],
            [-1150000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif7AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            203,
            418,
            216,
            204,
            1,
            1,
            [203],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif7BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            30,
            72,
            46,
            30,
            0,
            4,
            [15, 15],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 10, 10],
            [-947673, 289764, 1238197, -289764, 947673, 1600000],
        );
    }

    function expectedMotif7AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            18,
            42,
            26,
            18,
            0,
            2,
            [18],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6],
            [-740195, 412942, 1238197, -412942, 740195, 1600000],
        );
    }

    function expectedMotif7ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            215,
            448,
            236,
            216,
            1,
            3,
            [215],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7,
                8, 8, 10, 10, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif10AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            203,
            418,
            216,
            204,
            1,
            1,
            [203],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 9, 9, 9, 9, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 993462, 1650000],
        );
    }

    function expectedMotif10BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            30,
            72,
            46,
            30,
            0,
            4,
            [15, 15],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10],
            [-190211, 600000, 738197, 190211, 1150000, 1100000],
        );
    }

    function expectedMotif10AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            18,
            42,
            26,
            18,
            0,
            2,
            [18],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 7, 7, 7, 7],
            [-190211, 874541, 738197, 190211, 1000000, 1100000],
        );
    }

    function expectedMotif10ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            215,
            448,
            236,
            216,
            1,
            3,
            [215],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 9, 9,
                9, 9, 10, 10, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1150000, 1650000],
        );
    }

    function expectedMotif12AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            203,
            418,
            216,
            204,
            1,
            1,
            [203],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 8, 8, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif12BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            30,
            72,
            46,
            30,
            0,
            4,
            [15, 15],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 10, 10],
            [289764, 289764, 1238197, 947673, 947673, 1600000],
        );
    }

    function expectedMotif12AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            18,
            42,
            26,
            18,
            0,
            2,
            [18],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6],
            [412942, 412942, 1238197, 740195, 740195, 1600000],
        );
    }

    function expectedMotif12ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            215,
            448,
            236,
            216,
            1,
            3,
            [215],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7,
                8, 8, 10, 10, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif14AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            207,
            428,
            222,
            208,
            1,
            1,
            [207],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
                3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif14BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            30,
            74,
            48,
            30,
            0,
            4,
            [15, 15],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10],
            [481537, 53878, 988197, 1135252, 615818, 1350000],
        );
    }

    function expectedMotif14AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            18,
            44,
            28,
            18,
            0,
            2,
            [18],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 7, 7, 7],
            [731016, 157215, 988197, 946509, 527814, 1350000],
        );
    }

    function expectedMotif14ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            219,
            458,
            242,
            220,
            1,
            3,
            [219],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
                3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 7, 7,
                7, 7, 8, 8, 8, 8, 10, 10, 16, 16,
            ],
            [-1000000, -1000000, 0, 1135252, 1000000, 1650000],
        );
    }

    function expectedMotif15AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            203,
            418,
            216,
            204,
            1,
            1,
            [203],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 9, 9, 9, 9, 16, 16,
            ],
            [-1000000, -1000000, 0, 993462, 1000000, 1650000],
        );
    }

    function expectedMotif15BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            30,
            72,
            46,
            30,
            0,
            4,
            [15, 15],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 7, 7, 7, 7, 10, 10],
            [600000, -190211, 738197, 1150000, 190211, 1100000],
        );
    }

    function expectedMotif15AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            18,
            42,
            26,
            18,
            0,
            2,
            [18],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 7, 7, 7, 7],
            [874541, -190211, 738197, 1000000, 190211, 1100000],
        );
    }

    function expectedMotif15ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            215,
            448,
            236,
            216,
            1,
            3,
            [215],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 9, 9,
                9, 9, 10, 10, 16, 16,
            ],
            [-1000000, -1000000, 0, 1150000, 1000000, 1650000],
        );
    }

    function expectedMotif21AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            229,
            498,
            270,
            230,
            1,
            1,
            [229],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5,
                5, 5, 5, 6, 6, 6, 6, 6, 7, 8, 11, 11, 11, 11, 11, 12, 13, 14, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif21BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            85,
            243,
            162,
            85,
            0,
            4,
            [37, 48],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            ],
            [
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
                5, 5, 6, 6, 7, 7, 7, 7, 12, 14, 16, 18, 24, 30, 32,
            ],
            [-150000, -1040000, 1250822, 150000, -490000, 1549178],
        );
    }

    function expectedMotif21AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            40,
            114,
            76,
            40,
            0,
            2,
            [40],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
            ],
            [
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7,
                7, 12, 14, 16, 18,
            ],
            [-150000, -950108, 1250822, 55000, -748789, 1549178],
        );
    }

    function expectedMotif21ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            274,
            627,
            356,
            275,
            1,
            3,
            [274],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5,
                5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 7, 8, 11, 11, 11, 11, 11, 12, 13, 14, 16, 16, 24, 30, 32,
            ],
            [-1000000, -1040000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif23AB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            229,
            498,
            270,
            230,
            1,
            1,
            [229],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 6, 6, 6, 6, 8, 9, 9, 10, 10, 11, 11, 11, 11, 16, 16, 16, 16,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    function expectedMotif23BA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            85,
            243,
            162,
            85,
            0,
            4,
            [37, 48],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            ],
            [
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5,
                6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 22, 22, 24, 30, 32,
            ],
            [-841457, -774282, 750822, -240416, -240416, 1049178],
        );
    }

    function expectedMotif23AiB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            40,
            114,
            76,
            40,
            0,
            2,
            [40],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
            ],
            [
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 7, 7, 7,
                8, 8, 8, 22, 22,
            ],
            [-780539, -732803, 750822, -591536, -528915, 1049178],
        );
    }

    function expectedMotif23ApB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            274,
            627,
            356,
            275,
            1,
            3,
            [274],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
            ],
            [
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 9, 9, 10, 10, 11, 11, 11, 11, 16, 16, 16, 16, 24, 30, 32,
            ],
            [-1000000, -1000000, 0, 1000000, 1000000, 1650000],
        );
    }

    // Java: @Disabled("40x4 diagnostic - run manually")
    it.skip("diagnose_allMotifsAllOps_printTopologicalSummaryMatrix", () => {
        const total = CsgKurlanderBowlFixture.getSingleMotifCount();
        const statusCounts: number[][] = [];
        for (let i = 0; i < OP_NAMES.length; i++) {
            statusCounts.push([0, 0, 0, 0, 0]);
        }

        for (let motif = 0; motif < total; motif++) {
            const desc = CsgKurlanderBowlFixture.describeSingleMotif(motif);

            for (let opIdx = 0; opIdx < OP_NAMES.length; opIdx++) {
                let operands: PolyhedralBoundedSolid[];
                let opResult: PolyhedralBoundedSolid;
                let status: OpStatus;
                let detail = "";

                try {
                    operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
                } catch (t) {
                    status = OpStatus.EXCEPTION;
                    detail = " build-operands: " + (t as Error).constructor.name + ": " + (t as Error).message;
                    statusCounts[opIdx]![status]++;
                    console.log(`[MATRIX] motif=${motif} ${desc} op=${OP_NAMES[opIdx]} status=EXCEPTION${detail}`);
                    continue;
                }

                try {
                    opResult = executeOp(operands[0]!, operands[1]!, opIdx);
                } catch (t) {
                    status = OpStatus.EXCEPTION;
                    detail = " " + (t as Error).constructor.name + ": " + (t as Error).message;
                    statusCounts[opIdx]![status]++;
                    console.log(`[MATRIX] motif=${motif} ${desc} op=${OP_NAMES[opIdx]} status=EXCEPTION${detail}`);
                    continue;
                }

                status = classify(opResult);
                statusCounts[opIdx]![status]++;

                if (status === OpStatus.OK) {
                    const opSummary = TopologicalSummary.from(opResult);
                    if (opIdx === 1) {
                        detail = " shellCount=" + opSummary.shellCount + " => " + opSummary.toLiteral();
                    } else {
                        detail = " => " + opSummary.toLiteral();
                    }
                }

                console.log(
                    `[MATRIX] motif=${motif} ${desc} op=${OP_NAMES[opIdx]} status=${OpStatus[status]}${detail}`,
                );
            }
        }

        console.log("\n=== 40×4 MATRIX SUMMARY ===");
        console.log("op     " + [0, 1, 2, 3, 4].map((s) => OpStatus[s]).join(" "));
        for (let opIdx = 0; opIdx < OP_NAMES.length; opIdx++) {
            console.log(OP_NAMES[opIdx] + "   " + statusCounts[opIdx]!.join(" "));
        }
    });

    /**
    For each motif enabled in `ENABLED`, run the 4 boolean operations
    and verify that all of them produce `OpStatus.OK`.
    For each operation, print the trace of vertices created during the intersection.
    Motifs with `ENABLED[motif] === false` are skipped.
    */
    it.each(allMotifIndices())(
        "motif[%i]",
        async (motif: number) => {
            await yieldToEventLoop();
            if (!ENABLED[motif]) {
                // Java: assumeTrue(ENABLED[motif], "motif N disabled in ENABLED - it is not 4-OK yet")
                return;
            }

            const motifDesc = CsgKurlanderBowlFixture.describeSingleMotif(motif);
            console.log(`\n[PARAM] motif=${motif} ${motifDesc}`);

            for (let opIdx = 0; opIdx < OP_NAMES.length; opIdx++) {
                await yieldToEventLoop();
                const motifOps = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
                const motifResult = executeOp(motifOps[0]!, motifOps[1]!, opIdx);

                console.log(`[PARAM]   op=${OP_NAMES[opIdx]} — intersection vertices:`);
                for (let i = 0; i < _PolyhedralBoundedSolidSetIntersector.intersectionTrace.size(); i++) {
                    console.log("[PARAM]     " + _PolyhedralBoundedSolidSetIntersector.intersectionTrace.get(i));
                }
                if (_PolyhedralBoundedSolidSetIntersector.intersectionTrace.isEmpty()) {
                    console.log("[PARAM]     (none)");
                }

                const status = classify(motifResult);
                expect(status, `motif ${motif} op ${OP_NAMES[opIdx]}`).toBe(OpStatus.OK);
            }
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "zzTempPrintMoonBaselines",
        async () => {
            const motifs = [21, 23];
            for (const motif of motifs) {
                for (let opIdx = 0; opIdx < 4; opIdx++) {
                    await yieldToEventLoop();
                    const baselineOps = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
                    const baselineResult = executeOp(baselineOps[0]!, baselineOps[1]!, opIdx);
                    const baselineSummary = TopologicalSummary.from(baselineResult);
                    console.log("MOTIF" + motif + "_OP" + opIdx + "=" + baselineSummary.toLiteral());
                }
            }
        },
        KURLANDER_TIMEOUT_MS,
    );
});

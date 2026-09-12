import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { PolyhedralBoundedSolidGeometricValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgKurlanderBowlFixture } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.js";
import { yieldToEventLoop } from "./_HarnessEventLoopYield.js";

/**
Harness accommodation: JUnit has no per-test time budget; each motif rebuilds
the Kurlander bowl through dozens of boolean operations.
*/
const KURLANDER_TIMEOUT_MS = 600_000;

/**
Protected invariant (see `KurlanderBowlFixPlan.md` §4-ter): every Kurlander
star motif (indices 0..19), subtracted from the bowl, must produce a valid,
non-empty, correctly-oriented solid.

<p>This locks in the result of Step 2 (commit `56be7fb6`): the connect
phase now preserves the classifier emission order for all-singleton null-edge
sets, so all 20 stars close cleanly (`looseA == 0`). Any future change to
the boolean pipeline must keep this green.</p>

<p>The moon motifs (indices 20..39) are <b>not</b> covered here on purpose; they
are the open investigation of §4-bis. Their status is tracked by the
`ENABLED[]`/`assumeTrue` mechanism in `KurlanderMotif4OperationMatrixTest`.</p>
*/
describe("KurlanderBowlStarInvariantTest", () => {
    const STAR_MOTIFS = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19];

    it.each(STAR_MOTIFS)(
        "STAR motif %i",
        async (motif: number) => {
            // Harness accommodation: vitest's worker RPC times out after a fixed
            // 60 s; the star sequence would otherwise block the worker's event
            // loop across the whole file. Java has no such constraint.
            await yieldToEventLoop();
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
            const result = PolyhedralBoundedSolidModeler.setOp(
                operands[0]!,
                operands[1]!,
                PolyhedralBoundedSolidModeler.SUBTRACT,
                false,
            );

            expect(result, `star ${motif}: A-B must not be null`).not.toBeNull();
            expect(
                result.getPolygonsList().size(),
                `star ${motif}: A-B must be non-empty (object must not disappear)`,
            ).toBeGreaterThan(0);
            expect(
                PolyhedralBoundedSolidValidationEngine.validateIntermediate(result),
                `star ${motif}: A-B must pass validateIntermediate`,
            ).toBe(true);

            const orientationMessage = new StringBuilder();
            const orientationOk = PolyhedralBoundedSolidGeometricValidator.validateConsistentFaceOrientations(
                result,
                orientationMessage,
            );
            expect(
                orientationOk,
                `star ${motif}: A-B must have no inverted (black) faces. ${orientationMessage.toString()}`,
            ).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );
});

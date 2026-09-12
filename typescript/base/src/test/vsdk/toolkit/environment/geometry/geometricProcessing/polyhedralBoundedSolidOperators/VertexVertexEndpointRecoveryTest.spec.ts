import { describe, expect, it } from "vitest";

import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";
import {
    SeparateEdgeSequenceResult,
    _PolyhedralBoundedSolidSetOperator,
} from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.js";

/**
Acceptance tests for the V/V endpoint recovery loop in
`_PolyhedralBoundedSolidSetOperator.separateEdgeSequence`, after the
§5.3 refactor in plan-csg-boolean-fix-stage2 replaced the legacy
"recoveryGuard > 16" magic ceiling with strict cycle detection over the
visited (from, to) pair history.

<p>The loop's progress invariant is: every iteration produces an unseen
`(from, to)` configuration. Because the half-edge population of
both loops is finite, the visited set cannot grow forever — the loop
either reaches the desired pairing (vertices coincide) or detects a
revisited configuration and reports
`SeparateEdgeSequenceResult.FAILED_CYCLE_DETECTED`.</p>

<p>Mapping to Mäntylä: the recovery handles the cases A-E that program
[MANT1988].15.12 (extended in wMANT2008) needs to canonicalize before
LMEV is safe to execute (figure 15.13 of the book).</p>

<p>Runtime boundary: the Java test reaches the package-private method and
the nested enum through reflection; TypeScript has no access modifiers at
run time, so both are used directly.</p>
 */
describe("VertexVertexEndpointRecoveryTest", () => {
    it("given_separateEdgeSequence_when_invokedWithNullFrom_then_returnsFailedNullInput", () => {
        const result = _PolyhedralBoundedSolidSetOperator.separateEdgeSequence(
            null,
            null,
            0,
            null as never,
            null as never,
        );

        expect(SeparateEdgeSequenceResult[result]).toBe("FAILED_NULL_INPUT");
    });

    it("given_separateEdgeSequence_when_apiInspected_then_resultEnumExposesAllFailureModes", () => {
        // Regression guard: the §5.3 refactor must keep all five named
        // outcomes (OK + four failure modes) as part of the public diagnostic
        // surface so the caller can react to each cause specifically.
        const names = new Set<string>();
        for (const key of Object.keys(SeparateEdgeSequenceResult)) {
            if (Number.isNaN(Number(key))) {
                names.add(key);
            }
        }
        expect([...names].sort()).toEqual(
            [
                "OK",
                "FAILED_NULL_INPUT",
                "FAILED_DIFFERENT_SOLIDS",
                "FAILED_CYCLE_DETECTED",
                "FAILED_NO_PAIRING_REACHED",
            ].sort(),
        );
    });

    it("given_classicReferencePair_when_runningSetOpThatExercisesRecovery_then_brepRemainsValid", () => {
        // The MANT1988 §15.1 pair stresses the V/V endpoint pairing path
        // (multiple coincident vertices in the coplanar interface). Driving
        // it through INTERSECTION must complete without triggering the new
        // cycle-detection abort and produce a valid B-rep.
        const pair = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();

        const result = PolyhedralBoundedSolidModeler.setOp(
            pair[0]!,
            pair[1]!,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
        );

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(result.getPolygonsList().size()).toBeGreaterThan(0);
    });

    it("given_classicReferencePair_when_runningSubtractThatExercisesRecovery_then_brepRemainsValid", () => {
        const pair = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();

        const result = PolyhedralBoundedSolidModeler.setOp(
            pair[0]!,
            pair[1]!,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
        );

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(result.getPolygonsList().size()).toBeGreaterThan(0);
    });
});

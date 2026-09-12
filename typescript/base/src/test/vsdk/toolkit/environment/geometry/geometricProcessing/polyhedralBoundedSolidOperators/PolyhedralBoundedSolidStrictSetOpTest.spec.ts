import { describe, expect, it } from "vitest";

import { IllegalStateException } from "java/lang/IllegalStateException.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidTestFixtures } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.js";

describe("PolyhedralBoundedSolidStrictSetOpTest", () => {
    function assertEquivalentShape(first: PolyhedralBoundedSolid, second: PolyhedralBoundedSolid): void {
        expect(first.getPolygonsList().size()).toBe(second.getPolygonsList().size());
        expect(first.getEdgesList().size()).toBe(second.getEdgesList().size());
        expect(first.getVerticesList().size()).toBe(second.getVerticesList().size());
        expect(Array.from(first.getMinMax())).toEqual(Array.from(second.getMinMax()));
    }

    it("given_defaultOverloadAndExplicitTrue_when_run_then_resultsAreEquivalent", () => {
        PolyhedralBoundedSolidValidationEngine.resetStrictValidationInvocationCount();
        const oldOperands = PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
        const explicitTrueOperands = PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();

        const oldResult = PolyhedralBoundedSolidModeler.setOp(
            oldOperands[0]!,
            oldOperands[1]!,
            PolyhedralBoundedSolidModeler.UNION,
            false,
            true,
        );
        const explicitTrueResult = PolyhedralBoundedSolidModeler.setOp(
            explicitTrueOperands[0]!,
            explicitTrueOperands[1]!,
            PolyhedralBoundedSolidModeler.UNION,
            false,
            true,
            true,
        );

        assertEquivalentShape(oldResult, explicitTrueResult);
        expect(PolyhedralBoundedSolidValidationEngine.getStrictValidationInvocationCount()).toBe(2n);
    });

    it("given_validDisjointPreflight_when_strictEnabled_then_resultReturns", () => {
        PolyhedralBoundedSolidValidationEngine.resetStrictValidationInvocationCount();
        const operands = PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();

        const result = PolyhedralBoundedSolidModeler.setOp(
            operands[0]!,
            operands[1]!,
            PolyhedralBoundedSolidModeler.UNION,
            false,
            true,
            true,
        );

        expect(result).not.toBeNull();
        expect(result.getPolygonsList().size()).toBe(12);
        expect(PolyhedralBoundedSolidValidationEngine.getStrictValidationInvocationCount()).toBe(1n);
    });

    it("given_currentPseudomanifoldResult_when_strictEnabled_then_itFailsAtSetOpGateway", () => {
        const operands = PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(0);

        let thrown: unknown = null;
        try {
            PolyhedralBoundedSolidModeler.setOp(
                operands[0]!,
                operands[1]!,
                PolyhedralBoundedSolidModeler.UNION,
                false,
                true,
                true,
            );
        } catch (e) {
            thrown = e;
        }
        expect(thrown).toBeInstanceOf(IllegalStateException);
        const message = String((thrown as Error).message);
        expect(message).toContain("Strict boolean result validation failed");
        expect(message).toContain("op=UNION");
        expect(message).toContain("path=");
        expect(message).toContain("operandA={faces=");
        expect(message).toContain("result={faces=");
        expect(message).toContain("TopologySummary{");
        expect(message).toContain("adjustedEuler=");
    });
});

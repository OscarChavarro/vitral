import { describe, expect, it } from "vitest";

import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidTestFixtures } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.js";

/**
Policy tests for no-intersection, touching-only, and containment boolean
cases.

<p>Traceability: [MANT1988] Ch. 15.1 set-operation statement and special
cases where boundaries do not properly intersect before the normal pipeline.</p>
 */
describe("PolyhedralBoundedSolidSetOperatorTest", () => {
    function disjointSetOperationSamples(): [number, number, number, number][] {
        const operands = PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
        const solidA = operands[0]!;
        const solidB = operands[1]!;

        const facesA = solidA.getPolygonsList().size();
        const edgesA = solidA.getEdgesList().size();
        const verticesA = solidA.getVerticesList().size();
        const facesB = solidB.getPolygonsList().size();
        const edgesB = solidB.getEdgesList().size();
        const verticesB = solidB.getVerticesList().size();

        return [
            [PolyhedralBoundedSolidModeler.UNION, facesA + facesB, edgesA + edgesB, verticesA + verticesB],
            [PolyhedralBoundedSolidModeler.INTERSECTION, 0, 0, 0],
            [PolyhedralBoundedSolidModeler.SUBTRACT, facesA, edgesA, verticesA],
        ];
    }

    function touchingSetOperationSamples(): [number][] {
        return [
            [PolyhedralBoundedSolidModeler.UNION],
            [PolyhedralBoundedSolidModeler.INTERSECTION],
            [PolyhedralBoundedSolidModeler.SUBTRACT],
        ];
    }

    function containmentSetOperationSamples(): [number, number, number, number][] {
        const operands = PolyhedralBoundedSolidTestFixtures.createContainmentBoxPair();
        const inner = operands[0]!;
        const outer = operands[1]!;

        const facesInner = inner.getPolygonsList().size();
        const edgesInner = inner.getEdgesList().size();
        const verticesInner = inner.getVerticesList().size();
        const facesOuter = outer.getPolygonsList().size();
        const edgesOuter = outer.getEdgesList().size();
        const verticesOuter = outer.getVerticesList().size();

        return [
            [PolyhedralBoundedSolidModeler.UNION, facesOuter, edgesOuter, verticesOuter],
            [PolyhedralBoundedSolidModeler.INTERSECTION, facesInner, edgesInner, verticesInner],
            [PolyhedralBoundedSolidModeler.SUBTRACT, 0, 0, 0],
        ];
    }

    function commutativeOperations(): [number][] {
        return [[PolyhedralBoundedSolidModeler.UNION], [PolyhedralBoundedSolidModeler.INTERSECTION]];
    }

    function mant1988StrictOperationSamples(): [number, number][] {
        return [
            [-1, PolyhedralBoundedSolidModeler.UNION],
            [-1, PolyhedralBoundedSolidModeler.SUBTRACT],
            [0, PolyhedralBoundedSolidModeler.INTERSECTION],
            [1, PolyhedralBoundedSolidModeler.UNION],
            [1, PolyhedralBoundedSolidModeler.INTERSECTION],
            [1, PolyhedralBoundedSolidModeler.SUBTRACT],
        ];
    }

    function mant1988IntermediateOnlySamples(): [number, number][] {
        return [
            [-1, PolyhedralBoundedSolidModeler.INTERSECTION],
            [0, PolyhedralBoundedSolidModeler.UNION],
            [0, PolyhedralBoundedSolidModeler.SUBTRACT],
        ];
    }

    function appe1967CornerUnionSamples(): [PolyhedralBoundedSolid, PolyhedralBoundedSolid, number, number, number][] {
        return [
            [
                PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 0.2, 0.2, 0.5, 0.1, 0.1),
                PolyhedralBoundedSolidTestFixtures.createBoxSolid(0.2, 1.0, 0.2, 0.1, 0.5, 0.1),
                8,
                18,
                12,
            ],
        ];
    }

    it.each(disjointSetOperationSamples())(
        "given_disjointSolids_when_setOperation_then_matchesNoIntersectionPolicy: op=%s",
        (op: number, expectedFaceCount: number, expectedEdgeCount: number, expectedVertexCount: number) => {
            // Arrange
            const operands = PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
            const solidA = operands[0]!;
            const solidB = operands[1]!;

            // Action
            const result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false);

            // Assert
            expect(result).not.toBeNull();
            expect(result.getPolygonsList().size()).toBe(expectedFaceCount);
            expect(result.getEdgesList().size()).toBe(expectedEdgeCount);
            expect(result.getVerticesList().size()).toBe(expectedVertexCount);
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        },
    );

    it.each(touchingSetOperationSamples())(
        "given_touchingOnlySolids_when_setOperation_then_matchesTouchingPolicy: op=%s",
        (op: number) => {
            // Arrange
            const operands = PolyhedralBoundedSolidTestFixtures.createTouchingBoxPair();
            const solidA = operands[0]!;
            const solidB = operands[1]!;

            // Action
            const result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false, true, false);

            // Assert
            expect(result).not.toBeNull();
            if (op === PolyhedralBoundedSolidModeler.INTERSECTION) {
                expect(result.getPolygonsList().size()).toBe(0);
                expect(result.getEdgesList().size()).toBe(0);
                expect(result.getVerticesList().size()).toBe(0);
            } else if (op === PolyhedralBoundedSolidModeler.UNION) {
                expect(result.getPolygonsList().size()).toBeGreaterThanOrEqual(1);
                expect(result.getEdgesList().size()).toBeGreaterThanOrEqual(1);
                expect(result.getVerticesList().size()).toBeGreaterThanOrEqual(1);
            } else {
                expect(result.getPolygonsList().size()).toBeGreaterThanOrEqual(0);
                expect(result.getEdgesList().size()).toBeGreaterThanOrEqual(0);
                expect(result.getVerticesList().size()).toBeGreaterThanOrEqual(0);
            }
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        },
    );

    it.each(containmentSetOperationSamples())(
        "given_containmentWithoutBoundaryIntersection_when_setOperation_then_matchesContainmentPolicy: op=%s",
        (op: number, expectedFaceCount: number, expectedEdgeCount: number, expectedVertexCount: number) => {
            // Arrange
            const operands = PolyhedralBoundedSolidTestFixtures.createContainmentBoxPair();
            const inner = operands[0]!;
            const outer = operands[1]!;

            // Action
            const result = PolyhedralBoundedSolidModeler.setOp(inner, outer, op, false);

            // Assert
            expect(result).not.toBeNull();
            expect(result.getPolygonsList().size()).toBe(expectedFaceCount);
            expect(result.getEdgesList().size()).toBe(expectedEdgeCount);
            expect(result.getVerticesList().size()).toBe(expectedVertexCount);
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        },
    );

    it.each(commutativeOperations())(
        "given_sameOperandsSwapped_when_unionOrIntersection_then_resultsKeepEquivalentSizes: op=%s",
        (op: number) => {
            // Arrange
            const disjointPairA = PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
            const disjointPairB = PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();

            // Action
            const resultAB = PolyhedralBoundedSolidModeler.setOp(disjointPairA[0]!, disjointPairA[1]!, op, false);
            const resultBA = PolyhedralBoundedSolidModeler.setOp(disjointPairB[1]!, disjointPairB[0]!, op, false);

            // Assert
            expect(resultAB.getPolygonsList().size()).toBe(resultBA.getPolygonsList().size());
            expect(resultAB.getEdgesList().size()).toBe(resultBA.getEdgesList().size());
            expect(resultAB.getVerticesList().size()).toBe(resultBA.getVerticesList().size());
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(resultAB)).toBe(true);
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(resultBA)).toBe(true);
        },
    );

    it.each(appe1967CornerUnionSamples())(
        "given_coplanarOverlappingBars_when_union_then_itProducesTheExpectedLPrism",
        (
            solidA: PolyhedralBoundedSolid,
            solidB: PolyhedralBoundedSolid,
            expectedFaceCount: number,
            expectedEdgeCount: number,
            expectedVertexCount: number,
        ) => {
            // Arrange

            // Action
            const result = PolyhedralBoundedSolidModeler.setOp(
                solidA,
                solidB,
                PolyhedralBoundedSolidModeler.UNION,
                false,
            );

            // Assert
            expect(result).not.toBeNull();
            expect(result.getPolygonsList().size()).toBe(expectedFaceCount);
            expect(result.getEdgesList().size()).toBe(expectedEdgeCount);
            expect(result.getVerticesList().size()).toBe(expectedVertexCount);
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        },
    );

    it.each(mant1988StrictOperationSamples())(
        "given_mant1988Section15_2Scenarios_when_strictOperations_then_resultIsReturnedAndStrictValid: situation=%s op=%s",
        (situation: number, op: number) => {
            // Arrange
            const operands = PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(situation);
            const solidA = operands[0]!;
            const solidB = operands[1]!;

            // Action
            const result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false);

            // Assert
            expect(result).not.toBeNull();
            expect(result.getPolygonsList().size()).toBeGreaterThanOrEqual(0);
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
            expect(PolyhedralBoundedSolidValidationEngine.validateStrict(result)).toBe(true);
        },
    );

    it.each(mant1988IntermediateOnlySamples())(
        "given_mant1988Section15_2Scenarios_when_pseudomanifoldOperations_then_resultRemainsIntermediateButNotStrict: situation=%s op=%s",
        (situation: number, op: number) => {
            // Arrange
            const operands = PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(situation);
            const solidA = operands[0]!;
            const solidB = operands[1]!;

            // Action
            const result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false, true, false);

            // Assert
            expect(result).not.toBeNull();
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
            expect(PolyhedralBoundedSolidValidationEngine.validateStrict(result)).toBe(false);
        },
    );

    it("given_mant1988Section15_2HoledIntersection_when_finalMaximizeFacesEnabled_then_resultRemainsIntermediateValidButNotStrict", () => {
        // Arrange
        const operands = PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);
        const solidA = operands[0]!;
        const solidB = operands[1]!;

        // Action
        const result = PolyhedralBoundedSolidModeler.setOp(
            solidA,
            solidB,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
            true,
            false,
        );

        // Assert
        expect(result).not.toBeNull();
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(result)).toBe(false);
    });

    it("given_mant1988Section15_2HoledUnion_when_currentKernelRuns_then_resultBecomesStrictAndKeepsBlockWithTriangularWings", () => {
        // Arrange
        const operands = PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);

        // Action
        const result = PolyhedralBoundedSolidModeler.setOp(
            operands[0]!,
            operands[1]!,
            PolyhedralBoundedSolidModeler.UNION,
            false,
        );

        // Assert
        expect(result).not.toBeNull();
        expect(result.getPolygonsList().size()).toBe(14);
        expect(result.getEdgesList().size()).toBe(30);
        expect(result.getVerticesList().size()).toBe(20);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(result)).toBe(true);
        expect(result.findFace(1)).not.toBeNull();
        expect(result.findFace(1)!.boundariesList.size()).toBe(2);
        expect(Array.from(result.getMinMax())).toEqual([0.0, 0.0, 0.0, 0.775, 1.0, 0.6]);
    });

    it("given_mant1988Section15_1DifferenceBA_when_currentKernelRuns_then_resultClosesDoubleLoop", () => {
        // Arrange
        const operands = PolyhedralBoundedSolidTestFixtures.createMant1988_15_1Pair();

        // Action
        const result = PolyhedralBoundedSolidModeler.setOp(
            operands[1]!,
            operands[0]!,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
        );

        // Assert
        expect(result).not.toBeNull();
        expect(result.getPolygonsList().size()).toBe(8);
        expect(result.getEdgesList().size()).toBe(18);
        expect(result.getVerticesList().size()).toBe(12);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(result)).toBe(true);
        expect(Array.from(result.getMinMax())).toEqual([1.0 / 3.0, 0.0, 0.25, 1.0, 1.0, 1.0]);
    });

    it("given_mant1988Section15_1DifferenceBA_when_classicConnectRuns_then_resultClosesDoubleLoop", () => {
        // Originally this test toggled the "flexibleEndpointChains" path
        // (and four sibling flags). After §6.1-A of plan-csg-boolean-fix-stage2
        // the flexible path was removed: the classic Connect (the only one
        // left) must still produce the same B-A subtract result topology
        // documented by Mantyla §15.1 figure.
        const operands = PolyhedralBoundedSolidTestFixtures.createMant1988_15_1Pair();

        const result = PolyhedralBoundedSolidModeler.setOp(
            operands[1]!,
            operands[0]!,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
        );

        expect(result).not.toBeNull();
        expect(result.getPolygonsList().size()).toBe(8);
        expect(result.getEdgesList().size()).toBe(18);
        expect(result.getVerticesList().size()).toBe(12);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(result)).toBe(true);
        expect(Array.from(result.getMinMax())).toEqual([1.0 / 3.0, 0.0, 0.25, 1.0, 1.0, 1.0]);
    });

    it("given_mant1988Section15_2HoledIntersection_when_togglingFinalMaximizeFaces_then_resultTopologyIsPreserved", () => {
        // Arrange
        const operandsWithMax = PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);
        const operandsWithoutMax = PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);

        // Action
        const withMax = PolyhedralBoundedSolidModeler.setOp(
            operandsWithMax[0]!,
            operandsWithMax[1]!,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
            true,
            false,
        );
        const withoutMax = PolyhedralBoundedSolidModeler.setOp(
            operandsWithoutMax[0]!,
            operandsWithoutMax[1]!,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
            false,
            false,
        );

        // Assert
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(withMax)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(withoutMax)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(withMax)).toBe(false);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(withoutMax)).toBe(false);
        expect(withMax.getPolygonsList().size()).toBe(withoutMax.getPolygonsList().size());
        expect(withMax.getEdgesList().size()).toBe(withoutMax.getEdgesList().size());
        expect(withMax.getVerticesList().size()).toBe(withoutMax.getVerticesList().size());
    });
});

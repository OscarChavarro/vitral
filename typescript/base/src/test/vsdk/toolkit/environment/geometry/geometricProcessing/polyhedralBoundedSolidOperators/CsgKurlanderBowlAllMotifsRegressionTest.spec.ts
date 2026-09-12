import { describe, expect, it } from "vitest";

import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgKurlanderBowlFixture } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.js";

/**
Harness accommodation: JUnit has no per-test time budget; each case rebuilds
the Kurlander bowl through dozens of boolean operations.
*/
const KURLANDER_TIMEOUT_MS = 600_000;

describe("CsgKurlanderBowlAllMotifsRegressionTest", () => {
    const GEOMETRY_TOLERANCE = 1.0e-9;
    const TOP_MOUTH_TOLERANCE = 1.0e-7;
    const FIRST_MOON_MOTIF_INDEX = 20;
    const THIRD_MOON_MOTIF_INDEX = 22;

    function assertVectorClose(actual: Vector3Dd, expected: Vector3Dd): void {
        expect(Math.abs(actual.x() - expected.x())).toBeLessThanOrEqual(GEOMETRY_TOLERANCE);
        expect(Math.abs(actual.y() - expected.y())).toBeLessThanOrEqual(GEOMETRY_TOLERANCE);
        expect(Math.abs(actual.z() - expected.z())).toBeLessThanOrEqual(GEOMETRY_TOLERANCE);
    }

    function loopStaysOnZ(loop: _PolyhedralBoundedSolidLoop, z: number): boolean {
        let halfEdge = loop.boundaryStartHalfEdge!;
        const start = halfEdge;

        do {
            if (Math.abs(halfEdge.startingVertex.position.z() - z) > TOP_MOUTH_TOLERANCE) {
                return false;
            }
            halfEdge = halfEdge.next()!;
        } while (halfEdge !== start);
        return true;
    }

    function isTopFace(face: _PolyhedralBoundedSolidFace | null, topZ: number): boolean {
        let i: number;

        if (face === null || face.boundariesList.size() < 1) {
            return false;
        }
        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop = face.boundariesList.get(i);

            if (loop === null || loop.boundaryStartHalfEdge === null || !loopStaysOnZ(loop, topZ)) {
                return false;
            }
        }
        return true;
    }

    function findExtremeRadius(face: _PolyhedralBoundedSolidFace, minimum: boolean): number {
        let radius = minimum ? Number.POSITIVE_INFINITY : Number.NEGATIVE_INFINITY;
        let i: number;

        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop = face.boundariesList.get(i)!;
            let halfEdge = loop.boundaryStartHalfEdge!;
            const start = halfEdge;

            do {
                const currentRadius = Math.hypot(
                    halfEdge.startingVertex.position.x(),
                    halfEdge.startingVertex.position.y(),
                );

                radius = minimum ? Math.min(radius, currentRadius) : Math.max(radius, currentRadius);
                halfEdge = halfEdge.next()!;
            } while (halfEdge !== start);
        }
        return radius;
    }

    function findMinimumRadius(face: _PolyhedralBoundedSolidFace): number {
        return findExtremeRadius(face, true);
    }

    function findMaximumRadius(face: _PolyhedralBoundedSolidFace): number {
        return findExtremeRadius(face, false);
    }

    function countHalfEdges(face: _PolyhedralBoundedSolidFace): number {
        let count = 0;
        let i: number;

        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop = face.boundariesList.get(i);

            if (loop !== null) {
                count += loop.halfEdgesList.size();
            }
        }
        return count;
    }

    function isTopMouthCapFace(face: _PolyhedralBoundedSolidFace, topZ: number, innerMouthRadius: number): boolean {
        return (
            isTopFace(face, topZ) &&
            face.boundariesList.size() === 1 &&
            countHalfEdges(face) >= 3 &&
            findMaximumRadius(face) <= innerMouthRadius + TOP_MOUTH_TOLERANCE
        );
    }

    function countTopMouthCapFaces(result: PolyhedralBoundedSolid, topZ: number, innerMouthRadius: number): number {
        let count = 0;
        let i: number;

        for (i = 0; i < result.getPolygonsList().size(); i++) {
            const face = result.getPolygonsList().get(i)!;

            if (isTopMouthCapFace(face, topZ, innerMouthRadius)) {
                count++;
            }
        }
        return count;
    }

    function findReferenceTopInnerMouthRadius(referenceBowl: PolyhedralBoundedSolid, topZ: number): number {
        let innerRadius = Number.POSITIVE_INFINITY;
        let i: number;

        for (i = 0; i < referenceBowl.getPolygonsList().size(); i++) {
            const face = referenceBowl.getPolygonsList().get(i)!;

            if (isTopFace(face, topZ) && face.boundariesList.size() > 1) {
                innerRadius = Math.min(innerRadius, findMinimumRadius(face));
            }
        }
        if (!Number.isFinite(innerRadius)) {
            throw new Error("Expected reference bowl to expose an open top annulus");
        }
        return innerRadius;
    }

    function hasSameBounds(actual: Float64Array | number[] | null, expected: Float64Array | number[] | null): boolean {
        let i: number;

        if (actual === null || expected === null || actual.length !== expected.length) {
            return false;
        }
        for (i = 0; i < actual.length; i++) {
            if (Math.abs(actual[i]! - expected[i]!) > GEOMETRY_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    function hasSameShapeData(
        result: PolyhedralBoundedSolid,
        originalFaces: number,
        originalEdges: number,
        originalVertices: number,
        originalBounds: Float64Array | number[],
    ): boolean {
        return (
            result.getPolygonsList().size() === originalFaces &&
            result.getEdgesList().size() === originalEdges &&
            result.getVerticesList().size() === originalVertices &&
            hasSameBounds(result.getMinMax(), originalBounds)
        );
    }

    it(
        "given_kurlanderBowlAndFirstMoon_when_subtractingMoonFromBowl_then_resultStaysNonEmptyAndIntermediateValid",
        () => {
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(FIRST_MOON_MOTIF_INDEX);
            const topZ = operands[0]!.getMinMax()[5]!;
            const innerMouthRadius = findReferenceTopInnerMouthRadius(operands[0]!, topZ);
            const result = PolyhedralBoundedSolidModeler.setOp(
                operands[0]!,
                operands[1]!,
                PolyhedralBoundedSolidModeler.SUBTRACT,
                false,
            );

            expect(result).not.toBeNull();
            expect(result.getPolygonsList().size()).toBeGreaterThan(0);
            expect(result.getEdgesList().size()).toBeGreaterThan(0);
            expect(result.getVerticesList().size()).toBeGreaterThan(0);
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
            expect(
                countTopMouthCapFaces(result, topZ, innerMouthRadius),
                "Subtracting the moon must keep the bowl mouth open; no planar cap should be created over the inner top rim",
            ).toBe(0);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndThirdMoon_when_subtractingMoonFromBowl_then_resultStaysNonEmptyAndIntermediateValid",
        () => {
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(THIRD_MOON_MOTIF_INDEX);
            const originalFaces = operands[0]!.getPolygonsList().size();
            const originalEdges = operands[0]!.getEdgesList().size();
            const originalVertices = operands[0]!.getVerticesList().size();
            const originalBounds = Array.from(operands[0]!.getMinMax());
            const result = PolyhedralBoundedSolidModeler.setOp(
                operands[0]!,
                operands[1]!,
                PolyhedralBoundedSolidModeler.SUBTRACT,
                false,
            );

            expect(result).not.toBeNull();
            expect(result.getPolygonsList().size()).toBeGreaterThan(0);
            expect(result.getEdgesList().size()).toBeGreaterThan(0);
            expect(result.getVerticesList().size()).toBeGreaterThan(0);
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
            expect(
                hasSameShapeData(result, originalFaces, originalEdges, originalVertices, originalBounds),
                "Subtracting the third moon must actually modify the bowl; returning the unmodified operand A hides the failed cut",
            ).toBe(false);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderShellAndFirstMoon_when_subtractingMoonFromShell_then_resultStaysValid",
        () => {
            const operands = CsgKurlanderBowlFixture.createShellAndFirstMoonOperands();
            const result = PolyhedralBoundedSolidModeler.setOp(
                operands[0]!,
                operands[1]!,
                PolyhedralBoundedSolidModeler.SUBTRACT,
                false,
                false,
            );

            expect(result).not.toBeNull();
            expect(result.getPolygonsList().size()).toBeGreaterThan(0);
            expect(result.getEdgesList().size()).toBeGreaterThan(0);
            expect(result.getVerticesList().size()).toBeGreaterThan(0);
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it("given_kurlanderFirstStarPlacement_whenCreated_thenTopTipStaysUprightAgainstZ", () => {
        const placement = CsgKurlanderBowlFixture.createStarPlacementTransformation(9.0, -90.0);
        const origin = placement.multiply(new Vector3Dd());
        const extrusionAxis = placement.multiply(new Vector3Dd(0.0, 0.0, 0.55)).subtract(origin);
        const topTip = placement.multiply(new Vector3Dd(0.0, -0.2, 0.0)).subtract(origin);

        assertVectorClose(origin, new Vector3Dd(0.0, -0.6, 0.9));
        assertVectorClose(extrusionAxis, new Vector3Dd(0.0, -0.55, 0.0));
        assertVectorClose(topTip, new Vector3Dd(0.0, 0.0, 0.2));
    });

    it(
        "given_kurlanderFirstMoonOperand_whenCreated_thenMoonIsRolledAndInsetIntoBowl",
        () => {
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(FIRST_MOON_MOTIF_INDEX);
            const moon = operands[1]!;
            const minMax = moon.getMinMax();
            const placement = CsgKurlanderBowlFixture.createMoonPlacementTransformation(4.0, -90.0);
            const origin = placement.multiply(new Vector3Dd());
            const cylinderAxis = placement.multiply(new Vector3Dd(0.0, 0.0, 0.5)).subtract(origin);
            const crescentOffset = placement.multiply(new Vector3Dd(0.11, 0.0, 0.06)).subtract(origin);

            expect(moon.getVerticesList().size()).toBeGreaterThan(0);
            expect(Math.abs(minMax[1]! - -1.04)).toBeLessThanOrEqual(GEOMETRY_TOLERANCE);
            expect(Math.abs(minMax[4]! - -0.49)).toBeLessThanOrEqual(GEOMETRY_TOLERANCE);
            assertVectorClose(origin, new Vector3Dd(0.0, -0.49, 0.4));
            assertVectorClose(cylinderAxis, new Vector3Dd(0.0, -0.5, 0.0));
            assertVectorClose(crescentOffset, new Vector3Dd(0.11, -0.06, 0.0));
        },
        KURLANDER_TIMEOUT_MS,
    );
});

import { describe, expect, it } from "vitest";

import { Math as JavaMath } from "java/lang/Math.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { ToleranceContext } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import type { _PolyhedralBoundedSolidVertex } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgKurlanderBowlFixture } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.js";
import { _PolyhedralBoundedSolidSetFinisher } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetFinisher.js";
import { _PolyhedralBoundedSolidSetNullEdgesConnector } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.js";

/**
Harness accommodation: JUnit has no per-test time budget; each case rebuilds
the Kurlander bowl through dozens of boolean operations.
*/
const KURLANDER_TIMEOUT_MS = 600_000;

describe("CsgKurlanderBowlFirstStarRegressionTest", () => {
    const STAR_VERTEX_COUNT = 10;

    function loopAreaMagnitude(loop: _PolyhedralBoundedSolidLoop): number {
        let he = loop.boundaryStartHalfEdge!;
        const start = he;
        let normalAccumulator = new Vector3Dd();

        do {
            const p = he.startingVertex.position;
            const q = he.next()!.startingVertex.position;

            normalAccumulator = normalAccumulator.add(
                new Vector3Dd(
                    (p.y() - q.y()) * (p.z() + q.z()),
                    (p.z() - q.z()) * (p.x() + q.x()),
                    (p.x() - q.x()) * (p.y() + q.y()),
                ),
            );
            he = he.next()!;
        } while (he !== start);
        return normalAccumulator.length();
    }

    function hasPreviousMatchingPosition(
        loop: _PolyhedralBoundedSolidLoop,
        currentIndex: number,
        current: _PolyhedralBoundedSolidHalfEdge,
        numericContext: ToleranceContext,
    ): boolean {
        let i: number;

        for (i = 0; i < currentIndex; i++) {
            const previous = loop.halfEdgesList.get(i)!;
            if (
                PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    previous.startingVertex.position,
                    current.startingVertex.position,
                    numericContext,
                )
            ) {
                return true;
            }
        }
        return false;
    }

    function countUniqueLoopPositions(loop: _PolyhedralBoundedSolidLoop, numericContext: ToleranceContext): number {
        let uniqueCount = 0;
        let i: number;

        for (i = 0; i < loop.halfEdgesList.size(); i++) {
            const he = loop.halfEdgesList.get(i)!;
            if (!hasPreviousMatchingPosition(loop, i, he, numericContext)) {
                uniqueCount++;
            }
        }
        return uniqueCount;
    }

    function isZeroAreaClosedStarDoubleWalkLoop(
        loop: _PolyhedralBoundedSolidLoop | null,
        numericContext: ToleranceContext,
    ): boolean {
        if (
            loop === null ||
            loop.boundaryStartHalfEdge === null ||
            loop.halfEdgesList.size() < (STAR_VERTEX_COUNT - 1) * 2
        ) {
            return false;
        }
        if (loopAreaMagnitude(loop) > numericContext.bigEpsilon()) {
            return false;
        }
        return countUniqueLoopPositions(loop, numericContext) <= STAR_VERTEX_COUNT;
    }

    function countZeroAreaClosedStarDoubleWalkLoops(
        solid: PolyhedralBoundedSolid,
        numericContext: ToleranceContext,
    ): number {
        let count = 0;
        let i: number;
        let j: number;

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            for (j = 0; j < face.boundariesList.size(); j++) {
                const loop = face.boundariesList.get(j);
                if (isZeroAreaClosedStarDoubleWalkLoop(loop, numericContext)) {
                    count++;
                }
            }
        }
        return count;
    }

    function loopWalkReturnsToStart(loop: _PolyhedralBoundedSolidLoop): boolean {
        let halfEdge: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
        let i: number;

        for (i = 0; i < loop.halfEdgesList.size(); i++) {
            if (halfEdge === null) {
                return false;
            }
            halfEdge = halfEdge.next();
        }
        return halfEdge === loop.boundaryStartHalfEdge;
    }

    function hasCoincidentPartnerVertex(
        solid: PolyhedralBoundedSolid,
        vertex: _PolyhedralBoundedSolidVertex,
        numericContext: ToleranceContext,
    ): boolean {
        let i: number;

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const candidate = solid.getVerticesList().get(i)!;
            if (
                candidate !== vertex &&
                PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    candidate.position,
                    vertex.position,
                    numericContext,
                )
            ) {
                return true;
            }
        }
        return false;
    }

    function isClosedDoubleBoundaryContour(
        solid: PolyhedralBoundedSolid,
        loop: _PolyhedralBoundedSolidLoop | null,
        numericContext: ToleranceContext,
    ): boolean {
        let i: number;

        if (loop === null || loop.boundaryStartHalfEdge === null || loop.halfEdgesList.size() < STAR_VERTEX_COUNT) {
            return false;
        }

        for (i = 0; i < loop.halfEdgesList.size(); i++) {
            const halfEdge = loop.halfEdgesList.get(i);
            if (
                halfEdge === null ||
                halfEdge.next() === null ||
                halfEdge.parentEdge === null ||
                !hasCoincidentPartnerVertex(solid, halfEdge.startingVertex, numericContext)
            ) {
                return false;
            }
        }
        return loopWalkReturnsToStart(loop);
    }

    function countClosedDoubleBoundaryContours(
        solid: PolyhedralBoundedSolid,
        numericContext: ToleranceContext,
    ): number {
        let count = 0;
        let i: number;
        let j: number;

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            for (j = 0; j < face.boundariesList.size(); j++) {
                const loop = face.boundariesList.get(j);
                if (isClosedDoubleBoundaryContour(solid, loop, numericContext)) {
                    count++;
                }
            }
        }
        return count;
    }

    // `countClosedDoubleBoundaryContours` (and the helpers it owns) has no
    // caller in the Java original either; it is kept verbatim, and this
    // reference exists only so the unused-symbol lint does not delete it.
    void countClosedDoubleBoundaryContours;

    function createStarPoints(): Vector3Dd[] {
        const points = new Array<Vector3Dd>(STAR_VERTEX_COUNT);
        const outerRadius = 2.0;
        const innerRadius = 0.77;
        const start = JavaMath.toRadians(-90.0);
        let i: number;

        for (i = 0; i < points.length; i++) {
            const angle = start + (i * Math.PI) / 5.0;
            const radius = i % 2 === 0 ? outerRadius : innerRadius;
            points[i] = new Vector3Dd(radius * Math.cos(angle), radius * Math.sin(angle), 0.0);
        }
        return points;
    }

    function createClosedDoubleWalkStarLamina(): PolyhedralBoundedSolid {
        const solid = new PolyhedralBoundedSolid();
        const starPoints = createStarPoints();
        let vertexId = 1;
        let i: number;

        PolyhedralBoundedSolidEulerOperators.mvfs(solid, starPoints[0]!, 1, 1);
        for (i = 1; i < starPoints.length; i++) {
            vertexId++;
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, vertexId - 1, vertexId, starPoints[i]!);
        }
        for (i = starPoints.length - 2; i >= 1; i--) {
            vertexId++;
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, vertexId - 1, vertexId, starPoints[i]!);
        }
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, vertexId, 1, 2);
        return solid;
    }

    it("given_kurlanderSingleMotifSelector_whenIndexing_thenStarsComeBeforeMoonsAndWrapsCircularly", () => {
        const starCount = CsgKurlanderBowlFixture.getSingleMotifStarCount();
        const moonCount = CsgKurlanderBowlFixture.getSingleMotifMoonCount();
        const motifCount = CsgKurlanderBowlFixture.getSingleMotifCount();

        expect(starCount).toBe(20);
        expect(moonCount).toBe(20);
        expect(motifCount).toBe(40);
        expect(CsgKurlanderBowlFixture.normalizeSingleMotifIndex(0)).toBe(0);
        expect(CsgKurlanderBowlFixture.normalizeSingleMotifIndex(motifCount)).toBe(0);
        expect(CsgKurlanderBowlFixture.normalizeSingleMotifIndex(-1)).toBe(motifCount - 1);
        expect(CsgKurlanderBowlFixture.describeSingleMotif(0).startsWith("STAR 1/20")).toBe(true);
        expect(CsgKurlanderBowlFixture.describeSingleMotif(starCount).startsWith("MOON 1/20")).toBe(true);
    });

    it(
        "given_kurlanderBowlAndFirstStar_when_subtractingStarFromBowl_then_resultStaysNonEmptyAndIntermediateValid",
        () => {
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
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
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndFirstStar_when_subtractingStarFromBowl_then_connectStageClosesAllStarEdges",
        () => {
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands();
            const result = PolyhedralBoundedSolidModeler.setOp(
                operands[0]!,
                operands[1]!,
                PolyhedralBoundedSolidModeler.SUBTRACT,
                false,
            );

            expect(result).not.toBeNull();
            expect(_PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseACount()).toBe(0);
            expect(_PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseBCount()).toBe(0);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndFirstStar_when_subtractingStarFromBowl_then_noZeroAreaLoopWalksTwiceAroundClosedStar",
        () => {
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands();
            const result = PolyhedralBoundedSolidModeler.setOp(
                operands[0]!,
                operands[1]!,
                PolyhedralBoundedSolidModeler.SUBTRACT,
                false,
            );
            const numericContext = PolyhedralBoundedSolidNumericPolicy.forSolid(result);

            expect(countZeroAreaClosedStarDoubleWalkLoops(result, numericContext)).toBe(0);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_kurlanderBowlAndFifthStar_when_subtractingStarFromBowl_then_resultIsValidAndPairIndexMatchingSucceeds",
        () => {
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(4);
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
            // §9.1: pairIndex matching must work for the curved-surface case
            expect(
                _PolyhedralBoundedSolidSetFinisher.getLastLegacyFallbackCount(),
                "§9.1: sanitizePairedFaces must not fall back to legacy ordering for star 5",
            ).toBe(0);
            // §9.2: triangulateNonPlanarFaces fires for curved surfaces (expected)
            // — the bowl is tessellated; adjacent faces have different normals.
            // The counter merely records how many lmef splits were needed; it is
            // expected to be > 0 for curved-surface operands. Assert result is valid.
            expect(
                PolyhedralBoundedSolidValidationEngine.validateIntermediate(result),
                "result of bowl minus star 5 must pass intermediate validation",
            ).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it("given_closedStarLoopWalkedForwardAndBackward_whenScanningLoops_then_zeroAreaDoubleWalkIsDetected", () => {
        const solid = createClosedDoubleWalkStarLamina();
        const numericContext = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);

        expect(countZeroAreaClosedStarDoubleWalkLoops(solid, numericContext)).toBeGreaterThan(0);
    });
});

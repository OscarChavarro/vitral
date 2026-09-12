import { beforeEach, describe, expect, it } from "vitest";

import { Math as JavaMath } from "java/lang/Math.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { ToleranceContext } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";
import { _PolyhedralBoundedSolidOperator } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSetOperator } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.js";
import { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.js";
import { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex.js";
import { PolyhedralBoundedSolidTestFixtures } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";

/**
Probes coplanar and touching predicates used by boolean classification.

<p>Traceability: [MANT1988] Ch. 13.2 containment/intersection predicates
and Ch. 15.6 boundary classification for vertex/face and vertex/vertex
neighborhoods.</p>

<p>Runtime boundary: Java resolves the private statics
(`sectoroverlap`, `classifyCoplanarSectorRelation`,
`isTouchingOnlyPreflightCase`, `setNumericContext`) with reflection and
`setAccessible(true)`; TypeScript erases `private`, so they are reached
through structural casts of the class objects.</p>
 */
interface SetOperatorStatics {
    sectoroverlap: (
        na: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
        nb: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
    ) => boolean;
    classifyCoplanarSectorRelation: (
        sectorInfo: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace,
        referenceFace: _PolyhedralBoundedSolidFace,
    ) => number;
    isTouchingOnlyPreflightCase: (a: PolyhedralBoundedSolid, b: PolyhedralBoundedSolid) => boolean;
}

interface OperatorStatics {
    setNumericContext: (context: ToleranceContext | null) => void;
}

describe("PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest", () => {
    let setOperatorStatics: SetOperatorStatics;
    let operatorStatics: OperatorStatics;

    beforeEach(() => {
        setOperatorStatics = _PolyhedralBoundedSolidSetOperator as unknown as SetOperatorStatics;
        operatorStatics = _PolyhedralBoundedSolidOperator as unknown as OperatorStatics;
    });

    function invokeSectorOverlap(
        a: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
        b: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
    ): boolean {
        return setOperatorStatics.sectoroverlap(a, b);
    }

    function directionOnXY(degrees: number): Vector3Dd {
        const radians = JavaMath.toRadians(degrees);
        return new Vector3Dd(Math.cos(radians), Math.sin(radians), 0.0);
    }

    function createSector(
        he: _PolyhedralBoundedSolidHalfEdge,
        startDegrees: number,
        endDegrees: number,
    ): _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex {
        const sector = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex();
        sector.he = he;
        sector.ref1 = directionOnXY(startDegrees);
        sector.ref2 = directionOnXY(endDegrees);
        sector.ref12 = sector.ref1.crossProduct(sector.ref2);
        sector.wide = false;
        return sector;
    }

    function createTranslatedBox(
        sx: number,
        sy: number,
        sz: number,
        tx: number,
        ty: number,
        tz: number,
    ): PolyhedralBoundedSolid {
        const box = new Box(new Vector3Dd(sx, sy, sz));
        const solid = box.exportToPolyhedralBoundedSolid();
        let translation = new Matrix4x4d();
        translation = translation.translation(tx, ty, tz);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, translation);
        return solid;
    }

    function createHollowBrickOperands(): PolyhedralBoundedSolid[] {
        const operands = new Array<PolyhedralBoundedSolid>(2);
        const a = createTranslatedBox(1.0, 0.2, 0.2, 0.5, 0.1, 0.1);
        const b = createTranslatedBox(1.0, 0.2, 0.2, 0.5, 0.9, 0.1);
        const c = createTranslatedBox(0.2, 1.0, 0.2, 0.1, 0.5, 0.1);
        const d = createTranslatedBox(0.2, 1.0, 0.2, 0.9, 0.5, 0.1);

        operands[0] = PolyhedralBoundedSolidModeler.setOp(b, c, PolyhedralBoundedSolidModeler.UNION, false);
        operands[1] = PolyhedralBoundedSolidModeler.setOp(a, d, PolyhedralBoundedSolidModeler.UNION, false);
        return operands;
    }

    function findFaceWithNormal(
        solid: PolyhedralBoundedSolid,
        normalHint: Vector3Dd,
    ): _PolyhedralBoundedSolidFace | null {
        let i: number;
        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            const plane = face.getContainingPlane();
            if (plane !== null && plane.getNormal().dotProduct(normalHint) > 0.9) {
                return face;
            }
        }
        return null;
    }

    function findFaceOnPlane(
        solid: PolyhedralBoundedSolid,
        pointOnPlane: Vector3Dd,
        normalHint: Vector3Dd,
    ): _PolyhedralBoundedSolidFace | null {
        let i: number;
        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            const plane = face.getContainingPlane();
            if (plane === null) {
                continue;
            }
            if (
                Math.abs(plane.pointDistance(pointOnPlane)) <= 1.0e-6 &&
                plane.getNormal().dotProduct(normalHint) > 0.9
            ) {
                return face;
            }
        }
        return null;
    }

    it("given_coplanarSectors_when_angularIntervalsOverlap_then_sectoroverlapReturnsTrue", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const he = findFaceWithNormal(solid, new Vector3Dd(0.0, 0.0, 1.0))!.boundariesList.get(
            0,
        )!.boundaryStartHalfEdge!;

        operatorStatics.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolid(solid));

        // Action
        const relation = invokeSectorOverlap(createSector(he, 0.0, 60.0), createSector(he, 30.0, 90.0));

        // Assert
        expect(relation).toBe(true);
    });

    // Java: @Disabled("Legacy sectoroverlap intentionally treats boundary-ray
    // contact as overlap; fixing requires null-edge strut logic that knows the
    // touching case from the coplanar V/V path — deferred to future rework")
    it.skip("given_coplanarNeighborSectors_when_theyOnlyShareBoundaryRay_then_sectoroverlapReturnsFalse", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const he = findFaceWithNormal(solid, new Vector3Dd(0.0, 0.0, 1.0))!.boundariesList.get(
            0,
        )!.boundaryStartHalfEdge!;

        operatorStatics.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolid(solid));

        // Action
        const relation = invokeSectorOverlap(createSector(he, 0.0, 60.0), createSector(he, 60.0, 120.0));

        // Assert
        expect(relation).toBe(false);
    });

    // Java: @Disabled("Legacy sectoroverlap is deliberately permissive
    // (epsilon-tolerant); fixing the B-left-of-A disjoint case without breaking
    // touching-sector classification requires a restructured coplanar V/V path")
    it.skip("given_coplanarDisjointSectorsOnSameAngularSide_when_intervalsDoNotIntersect_then_sectoroverlapReturnsFalse", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const he = findFaceWithNormal(solid, new Vector3Dd(0.0, 0.0, 1.0))!.boundariesList.get(
            0,
        )!.boundaryStartHalfEdge!;

        operatorStatics.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolid(solid));

        // Action
        const relation = invokeSectorOverlap(createSector(he, -60.0, 0.0), createSector(he, -120.0, -80.0));

        // Assert
        expect(relation).toBe(false);
    });

    it("given_coincidentCoplanarFaces_when_classifyingLocalSectorAgainstReferenceFace_then_relationIsOverlap", () => {
        // Arrange
        const pair = PolyhedralBoundedSolidTestFixtures.createTouchingBoxPair();
        const solidA = pair[0]!;
        const solidB = pair[1]!;
        const interfaceX = solidA.getMinMax()[3]!;
        const interfacePoint = new Vector3Dd(interfaceX, 0.0, 0.0);

        const faceA = findFaceOnPlane(solidA, interfacePoint, new Vector3Dd(1.0, 0.0, 0.0))!;
        const faceB = findFaceOnPlane(solidB, interfacePoint, new Vector3Dd(-1.0, 0.0, 0.0))!;
        const sectorInfo = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();

        operatorStatics.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB));

        // Action
        sectorInfo.sector = faceA.boundariesList.get(0)!.boundaryStartHalfEdge;
        const relation = setOperatorStatics.classifyCoplanarSectorRelation(sectorInfo, faceB);

        // Assert
        expect(relation).toBe(_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_OVERLAP);
    });

    it("given_touchingBoxes_when_runningTouchingOnlyPreflight_then_itRecognizesNoVolumetricIntersection", () => {
        // Arrange
        const pair = PolyhedralBoundedSolidTestFixtures.createTouchingBoxPair();

        operatorStatics.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0]!, pair[1]!));

        // Action
        const touchingOnly = setOperatorStatics.isTouchingOnlyPreflightCase(pair[0]!, pair[1]!);

        // Assert
        expect(touchingOnly).toBe(true);
    });

    it("given_stackedBlocksWithPartialFaceOverlap_when_runningTouchingOnlyPreflight_then_itDoesNotDowngradeToTouching", () => {
        // Arrange
        const pair = CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.STACKED_BLOCKS);

        operatorStatics.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0]!, pair[1]!));

        // Action
        const touchingOnly = setOperatorStatics.isTouchingOnlyPreflightCase(pair[0]!, pair[1]!);

        // Assert
        expect(touchingOnly).toBe(false);
    });

    it("given_stackedBlocksWithPartialFaceOverlap_when_intersecting_then_itReturnsContactLamina", () => {
        // Arrange
        const pair = CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.STACKED_BLOCKS);

        // Action
        const result = PolyhedralBoundedSolidModeler.setOp(
            pair[0]!,
            pair[1]!,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
        );
        const minmax = result.getMinMax();

        // Assert
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(result.getPolygonsList().size()).toBe(2);
        expect(result.getEdgesList().size()).toBe(4);
        expect(result.getVerticesList().size()).toBe(4);
        expect(Array.from(minmax)).toEqual([0.25, 0.25, 0.3, 0.75, 0.75, 0.3]);
    });

    it("given_knownIntersectingMant1986Case_when_runningTouchingOnlyPreflight_then_itDoesNotDowngradeToTouching", () => {
        // Arrange
        const pair = SimpleTestGeometryLibrary.createTestObjectPairMANT1986_2();

        operatorStatics.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0]!, pair[1]!));

        // Action
        const touchingOnly = setOperatorStatics.isTouchingOnlyPreflightCase(pair[0]!, pair[1]!);

        // Assert
        expect(touchingOnly).toBe(false);
    });

    it("given_hollowBrickLOperands_when_runningTouchingOnlyPreflight_then_itDoesNotDowngradeCornerOverlapsToTouching", () => {
        // Arrange
        const pair = createHollowBrickOperands();

        operatorStatics.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolids(pair[0]!, pair[1]!));

        // Action
        const touchingOnly = setOperatorStatics.isTouchingOnlyPreflightCase(pair[0]!, pair[1]!);

        // Assert
        expect(touchingOnly).toBe(false);
    });
});

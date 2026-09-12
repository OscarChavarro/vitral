//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Boolean as JavaBoolean } from "../../../../../../../../java/lang/Boolean.js";
import { platformPrint } from "../../../../../../../../java/lang/_PlatformConsole.js";
import { VSDK } from "../../../../../../common/VSDK.js";
import { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../../../../Geometry.js";
import type { InfinitePlane } from "../../../../surface/InfinitePlane.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type {
    _PolyhedralBoundedSolidFace,
    PointInsideResult,
} from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSetOperator } from "../_PolyhedralBoundedSolidSetOperator.js";
import { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace } from "../classification/_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.js";
import { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector } from "../classification/_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.js";
import type { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex } from "../classification/_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex.js";

/**
Structured trace of a single {@code sectoroverlap} invocation captured
when the static collector is active (see §7.3.1 of
plan-csg-boolean-fix-stage2). Mutable POJO by design so the test can
introspect every field without reflection.
*/
class SectoroverlapTraceEntry {
    public callIndex = 0;
    public faceA = 0;
    public faceB = 0;
    public vertexAFrom = 0;
    public vertexATo = 0;
    public vertexBFrom = 0;
    public vertexBTo = 0;
    public a1 = 0;
    public a2 = 0;
    public b1 = 0;
    public b2 = 0;
    public diffA2B1 = 0;
    public diffB2A1 = 0;
    public boundaryRayContact = false;
    public decision = false;
}

class CoplanarAngleBasis {
    public normal: Vector3Dd | null = null;
    public u: Vector3Dd | null = null;
    public v: Vector3Dd | null = null;
}

class CoplanarAngularInterval {
    public start = 0;
    public end = 0;
    public interior = 0;
}

/**
Geometric predicates and coplanar-angle algebra used by the set-operations
classifiers from sections [MANT1988].14.5, [MANT1988].15.6.1, and
[MANT1988].15.6.2.
*/
export class _PolyhedralBoundedSolidSetGeometricPredicateProcessor extends _PolyhedralBoundedSolidOperator {
    private static readonly TWO_PI = 2.0 * Math.PI;
    private static readonly TRACE_COPLANAR_TANGENTIAL_PROPERTY = "vsdk.setop.traceCoplanarTangential";

    private static sectoroverlapTrace: SectoroverlapTraceEntry[] | null = null;
    private static sectoroverlapCallCounter = 0;

    public static enableSectoroverlapTrace(): void {
        _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlapTrace = [];
        _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlapCallCounter = 0;
    }

    public static disableSectoroverlapTrace(): void {
        _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlapTrace = null;
    }

    public static getSectoroverlapTrace(): SectoroverlapTraceEntry[] | null {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlapTrace;
    }

    private static recordSectoroverlapCall(
        na: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex | null,
        nb: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex | null,
        a1: number,
        a2: number,
        b1: number,
        b2: number,
        decision: boolean,
    ): void {
        const trace = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlapTrace;
        if (trace === null) {
            return;
        }
        const entry = new SectoroverlapTraceEntry();
        entry.callIndex = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlapCallCounter++;
        entry.faceA =
            na !== null && na.he !== null && na.he.parentLoop !== null && na.he.parentLoop.parentFace !== null
                ? na.he.parentLoop.parentFace.id
                : -1;
        entry.faceB =
            nb !== null && nb.he !== null && nb.he.parentLoop !== null && nb.he.parentLoop.parentFace !== null
                ? nb.he.parentLoop.parentFace.id
                : -1;
        entry.vertexAFrom =
            na !== null && na.he !== null && na.he.startingVertex !== null ? na.he.startingVertex.id : -1;
        entry.vertexATo = -1;
        if (na !== null && na.he !== null && na.he.next() !== null && na.he.next()!.startingVertex !== null) {
            entry.vertexATo = na.he.next()!.startingVertex.id;
        }
        entry.vertexBFrom =
            nb !== null && nb.he !== null && nb.he.startingVertex !== null ? nb.he.startingVertex.id : -1;
        entry.vertexBTo = -1;
        if (nb !== null && nb.he !== null && nb.he.next() !== null && nb.he.next()!.startingVertex !== null) {
            entry.vertexBTo = nb.he.next()!.startingVertex.id;
        }
        entry.a1 = a1;
        entry.a2 = a2;
        entry.b1 = b1;
        entry.b2 = b2;
        entry.diffA2B1 = a2 - b1;
        entry.diffB2A1 = b2 - a1;
        entry.boundaryRayContact = Math.abs(entry.diffA2B1) < 1.0e-12 || Math.abs(entry.diffB2A1) < 1.0e-12;
        entry.decision = decision;
        trace.push(entry);
    }

    private static readonly COPLANAR_OP_UNION = 0;
    private static readonly COPLANAR_OP_INTERSECTION = 1;
    private static readonly COPLANAR_OP_DIFFERENCE = 2;

    private static readonly COPLANAR_SIDE_A_VS_B = 0;
    private static readonly COPLANAR_SIDE_B_VS_A = 1;

    private static readonly COPLANAR_ORIENTATION_SAME = 0;
    private static readonly COPLANAR_ORIENTATION_OPPOSITE = 1;

    private static isCoplanarTangentialTraceEnabled(): boolean {
        return JavaBoolean.getBoolean(
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.TRACE_COPLANAR_TANGENTIAL_PROPERTY,
        );
    }

    private static traceCoplanarTangential(message: string): void {
        if (!_PolyhedralBoundedSolidSetGeometricPredicateProcessor.isCoplanarTangentialTraceEnabled()) {
            return;
        }
        console.log("[SetOpCoplanarTrace] " + message);
    }

    /*
    Coplanar overlap decision table for vertex/face classifier.
    */
    private static readonly COPLANAR_VERTEX_FACE_CLASS_TABLE: readonly (readonly (readonly number[])[])[] = [
        [
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
            ],
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
            ],
        ],
        [
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
            ],
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
            ],
        ],
        [
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
            ],
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
            ],
        ],
    ];

    /*
    Coplanar overlap decision table for vertex/vertex classifier.
    */
    private static readonly COPLANAR_VERTEX_VERTEX_CLASS_TABLE: readonly (readonly (readonly number[])[])[] = [
        [
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
            ],
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
            ],
        ],
        [
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
            ],
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
            ],
        ],
        [
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
            ],
            [
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
            ],
        ],
    ];

    public static compareToZero(value: number): number {
        return PolyhedralBoundedSolidNumericPolicy.compareToZero(value, _PolyhedralBoundedSolidOperator.numericContext);
    }

    public static pointInFace(face: _PolyhedralBoundedSolidFace, point: Vector3Dd): number {
        return face.testPointInside(point, _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon());
    }

    public static pointInFaceDetailed(face: _PolyhedralBoundedSolidFace, point: Vector3Dd): PointInsideResult {
        return face.testPointInsideDetailed(point, _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon());
    }

    public static colinearVectors(a: Vector3Dd, b: Vector3Dd): boolean {
        return PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
            a,
            b,
            _PolyhedralBoundedSolidOperator.numericContext,
        );
    }

    public static colinearVectorsWithDirection(a: Vector3Dd, b: Vector3Dd): boolean {
        if (PolyhedralBoundedSolidNumericPolicy.vectorsColinear(a, b, _PolyhedralBoundedSolidOperator.numericContext)) {
            if (a.dotProduct(b) >= 0) return true;
        }
        return false;
    }

    /**
    Following program [MANT1988].15.9. According to the sector intersection
    test from section [MANT1988].15.6.2, the variables are interpreted as in
    figure [MANT1988].15.8 and equation [MANT1988].15.5.
    */
    public static sctrwitthin(dir: Vector3Dd, ref1: Vector3Dd, ref2: Vector3Dd, ref12: Vector3Dd): boolean {
        const c1 = dir.crossProduct(ref1);
        if (
            PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                dir,
                ref1,
                _PolyhedralBoundedSolidOperator.numericContext,
            )
        ) {
            return ref1.dotProduct(dir) > 0.0;
        }
        const c2 = ref2.crossProduct(dir);
        if (
            PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                ref2,
                dir,
                _PolyhedralBoundedSolidOperator.numericContext,
            )
        ) {
            return ref2.dotProduct(dir) > 0.0;
        }
        const t1 = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.compareToZero(c1.dotProduct(ref12));
        const t2 = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.compareToZero(c2.dotProduct(ref12));
        return t1 < 0.0 && t2 < 0.0;
    }

    /**
    Strict version of the sector-within test from section [MANT1988].15.6.2
    that excludes the boundary-line cases implicit in figure [MANT1988].15.8
    and equation [MANT1988].15.5.
    */
    public static sctrwitthinProper(dir: Vector3Dd, ref1: Vector3Dd, ref2: Vector3Dd, ref12: Vector3Dd): boolean {
        if (
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.colinearVectors(dir, ref1) ||
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.colinearVectors(dir, ref2)
        ) {
            return false;
        }

        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sctrwitthin(dir, ref1, ref2, ref12);
    }

    /**
    Checks overlap of two coplanar sectors for the vertex/vertex classifier.
    Following section [MANT1988].15.6.2, where the operation is required but
    left implicit after program [MANT1988].15.9.
    */
    public static sectoroverlap(
        na: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
        nb: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
        withDebug: boolean,
    ): boolean {
        let a1: number;
        let a2: number;
        let b1: number;
        let b2: number;

        const n = na.he!.parentLoop.parentFace.getContainingPlane()!.getNormal();
        const u = new Vector3Dd(na.ref1!).normalized();
        const v = n.crossProduct(u).normalized();

        const a = new Vector3Dd(na.ref2!).normalized();
        const b = new Vector3Dd(nb.ref1!).normalized();
        const c = new Vector3Dd(nb.ref2!).normalized();

        a1 = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.angleFromVectors(u, v, u);
        a2 = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.angleFromVectors(u, v, a);
        b1 = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.angleFromVectors(u, v, b);
        b2 = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.angleFromVectors(u, v, c);

        if (a1 > a2) {
            const t = a1;
            a1 = a2;
            a2 = t;
        }
        if (b1 > b2) {
            const t = b1;
            b1 = b2;
            b2 = t;
        }

        // Closed-set (epsilon-tolerant) interval-overlap check.
        // Intentionally returns true for touching sectors (a2 ≈ b1) because the
        // setop pipeline requires a null-edge strut even at coplanar boundary-ray
        // contact — a strict open-set check breaks MANT1988 §15.1 reference geometry.
        // The symmetric disjoint case (B entirely left of A) is not yet fixed here;
        // that requires a deeper restructuring of the coplanar V/V path.
        const decision = a2 + VSDK.EPSILON > b1 - VSDK.EPSILON;
        _PolyhedralBoundedSolidSetGeometricPredicateProcessor.recordSectoroverlapCall(na, nb, a1, a2, b1, b2, decision);
        if (withDebug) {
            platformPrint(decision ? " <TRUE>" : " <FALSE>");
        }
        return decision;
    }

    private static angleFromVectors(u: Vector3Dd, v: Vector3Dd, a: Vector3Dd): number {
        let x: number;
        let angle: number;

        x = a.dotProduct(u);
        const y = a.dotProduct(v);
        if (x > 1.0) {
            x = 1.0;
        } else if (x < -1.0) {
            x = -1.0;
        }

        angle = Math.acos(x);
        if (y < 0) {
            angle *= -1;
        }
        return angle;
    }

    /**
    Resolves the class to propagate for coplanar sector pairs when applying the
    8-way boundary-classification logic of section [MANT1988].15.3 and the
    vertex/vertex classifier of section [MANT1988].15.6.2.
    */
    public static resolveCoplanarVertexVertexClass(op: number, sameOrientation: boolean, sideA: boolean): number {
        const sideIndex = sideA ? 0 : 1;
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.COPLANAR_VERTEX_VERTEX_CLASS_TABLE[
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.coplanarOpIndex(op)
        ]![_PolyhedralBoundedSolidSetGeometricPredicateProcessor.coplanarOrientationIndex(sameOrientation)]![
            sideIndex
        ]!;
    }

    /**
    Applies the coplanar reclassification rules for the vertex/face classifier,
    starting from sections [MANT1988].14.5.1 and [MANT1988].14.5.2 and biased
    toward set operations as proposed in [MANT1988].15.6.1 and problem
    [MANT1988].15.4.
    */
    public static applyCoplanarRulesToVertexFaceNeighborhood(
        nbr: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace[],
        referenceFace: _PolyhedralBoundedSolidFace,
        referencePlane: InfinitePlane | null,
        BvsA: number,
        op: number,
        useMirrorFace: boolean,
    ): void {
        let current: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace;
        let he: _PolyhedralBoundedSolidHalfEdge | null;
        let localFace: _PolyhedralBoundedSolidFace | null;
        let c: Vector3Dd;
        let d: number;
        let i: number;
        const nnbr = nbr.length;
        let relation: number;
        let resolvedClass: number;
        let sameOrientation: boolean;

        for (i = 0; i < nnbr; i++) {
            current = nbr[i]!;
            he = current.sector;
            if (he === null || he.parentLoop === null || he.parentLoop.parentFace === null) {
                continue;
            }

            if (useMirrorFace) {
                const mirror = he.mirrorHalfEdge();
                if (mirror === null || mirror.parentLoop === null || mirror.parentLoop.parentFace === null) {
                    continue;
                }
                localFace = mirror.parentLoop.parentFace;
            } else {
                localFace = he.parentLoop.parentFace;
            }
            if (localFace === null || localFace.getContainingPlane() === null || referencePlane === null) {
                continue;
            }

            c = localFace.getContainingPlane()!.getNormal().crossProduct(referencePlane.getNormal());
            d = c.dotProduct(c);
            if (_PolyhedralBoundedSolidSetGeometricPredicateProcessor.compareToZero(d) !== 0) {
                continue;
            }

            relation = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.classifyCoplanarSectorRelation(
                current,
                referenceFace,
            );
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.registerCoplanarRelation(nbr, i, relation);
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.registerCoplanarRelation(
                nbr,
                (i + 1) % nnbr,
                relation,
            );
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.traceCoplanarTangential(
                "vertexFace coplanar relation op=" +
                    op +
                    " side=" +
                    BvsA +
                    " face=" +
                    referenceFace.id +
                    " localFace=" +
                    localFace.id +
                    " sectorIndex=" +
                    i +
                    " relation=" +
                    relation,
            );

            if (relation === _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_OVERLAP) {
                d = localFace.getContainingPlane()!.getNormal().dotProduct(referencePlane.getNormal());
                sameOrientation = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.compareToZero(d) === 1;
                resolvedClass = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.resolveCoplanarSectorClass(
                    op,
                    BvsA,
                    sameOrientation,
                );
                _PolyhedralBoundedSolidSetGeometricPredicateProcessor.traceCoplanarTangential(
                    "  resolved vertexFace coplanar overlap sameOrientation=" +
                        sameOrientation +
                        " class=" +
                        resolvedClass,
                );
                nbr[i]!.cl = resolvedClass;
                nbr[(i + 1) % nnbr]!.cl = resolvedClass;
            }
        }
    }

    private static normalizedDirection(direction: Vector3Dd | null): Vector3Dd | null {
        let out: Vector3Dd;

        if (direction === null) {
            return null;
        }

        out = new Vector3Dd(direction);
        if (out.length() <= _PolyhedralBoundedSolidOperator.numericContext.unitVectorTolerance()) {
            return null;
        }
        out = out.normalized();
        return out;
    }

    private static buildCoplanarAngleBasis(
        planeNormal: Vector3Dd | null,
        preferredDirection: Vector3Dd | null,
        fallbackDirection: Vector3Dd | null,
    ): CoplanarAngleBasis | null {
        let u: Vector3Dd | null;
        let v: Vector3Dd | null;
        const tolerance = _PolyhedralBoundedSolidOperator.numericContext.unitVectorTolerance();

        const normal = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(planeNormal);
        if (normal === null) {
            return null;
        }

        u = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(preferredDirection);
        if (u === null || normal.crossProduct(u).length() <= tolerance) {
            u = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(fallbackDirection);
        }
        if (u === null || normal.crossProduct(u).length() <= tolerance) {
            if (Math.abs(normal.x()) < 0.9) {
                u = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(
                    new Vector3Dd(1.0, 0.0, 0.0).subtract(normal.multiply(normal.x())),
                );
            } else {
                u = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(
                    new Vector3Dd(0.0, 1.0, 0.0).subtract(normal.multiply(normal.y())),
                );
            }
        }
        if (u === null) {
            return null;
        }

        v = normal.crossProduct(u);
        v = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(v);
        if (v === null) {
            return null;
        }

        const basis = new CoplanarAngleBasis();
        basis.normal = normal;
        basis.u = u;
        basis.v = v;
        return basis;
    }

    private static angleOnBasis(basis: CoplanarAngleBasis | null, direction: Vector3Dd | null): number {
        const d = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(direction);
        if (basis === null || d === null) {
            return 0.0;
        }
        return Math.atan2(d.dotProduct(basis.v!), d.dotProduct(basis.u!));
    }

    private static unwrapAngleNear(angle: number, reference: number): number {
        while (angle - reference <= -Math.PI) {
            angle += _PolyhedralBoundedSolidSetGeometricPredicateProcessor.TWO_PI;
        }
        while (angle - reference > Math.PI) {
            angle -= _PolyhedralBoundedSolidSetGeometricPredicateProcessor.TWO_PI;
        }
        return angle;
    }

    private static sectorContainsDirectionInclusive(
        dir: Vector3Dd,
        ref1: Vector3Dd,
        ref2: Vector3Dd,
        ref12: Vector3Dd,
    ): boolean {
        if (
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.colinearVectorsWithDirection(dir, ref1) ||
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.colinearVectorsWithDirection(dir, ref2)
        ) {
            return true;
        }
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sctrwitthin(dir, ref1, ref2, ref12);
    }

    private static acceptSectorInteriorProbe(
        candidate: Vector3Dd | null,
        ref1: Vector3Dd,
        ref2: Vector3Dd,
        ref12: Vector3Dd,
    ): Vector3Dd | null {
        const normalized = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(candidate);
        if (normalized === null) {
            return null;
        }
        if (_PolyhedralBoundedSolidSetGeometricPredicateProcessor.sctrwitthinProper(normalized, ref1, ref2, ref12)) {
            return normalized;
        }
        if (
            !_PolyhedralBoundedSolidSetGeometricPredicateProcessor.colinearVectors(normalized, ref1) &&
            !_PolyhedralBoundedSolidSetGeometricPredicateProcessor.colinearVectors(normalized, ref2) &&
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectorContainsDirectionInclusive(
                normalized,
                ref1,
                ref2,
                ref12,
            )
        ) {
            return normalized;
        }
        return null;
    }

    private static selectSectorInteriorProbe(
        ref1: Vector3Dd,
        ref2: Vector3Dd,
        ref12: Vector3Dd,
        fallbackProbe: Vector3Dd | null,
    ): Vector3Dd | null {
        let probe: Vector3Dd | null;

        const bisector = ref1.add(ref2);
        probe = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.acceptSectorInteriorProbe(
            bisector,
            ref1,
            ref2,
            ref12,
        );
        if (probe !== null) {
            return probe;
        }

        probe = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.acceptSectorInteriorProbe(
            bisector.multiply(-1.0),
            ref1,
            ref2,
            ref12,
        );
        if (probe !== null) {
            return probe;
        }

        probe = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.acceptSectorInteriorProbe(
            ref12.crossProduct(ref1),
            ref1,
            ref2,
            ref12,
        );
        if (probe !== null) {
            return probe;
        }

        probe = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.acceptSectorInteriorProbe(
            ref2.crossProduct(ref12),
            ref1,
            ref2,
            ref12,
        );
        if (probe !== null) {
            return probe;
        }

        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.acceptSectorInteriorProbe(
            fallbackProbe,
            ref1,
            ref2,
            ref12,
        );
    }

    private static buildCoplanarAngularInterval(
        basis: CoplanarAngleBasis | null,
        boundary1: Vector3Dd | null,
        boundary2: Vector3Dd | null,
        interiorProbe: Vector3Dd | null,
    ): CoplanarAngularInterval | null {
        let t: number;

        if (
            basis === null ||
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(boundary1) === null ||
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(boundary2) === null
        ) {
            return null;
        }

        const probe = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.normalizedDirection(interiorProbe);
        if (probe === null) {
            return null;
        }

        const interval = new CoplanarAngularInterval();
        interval.interior = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.angleOnBasis(basis, probe);
        interval.start = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.unwrapAngleNear(
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.angleOnBasis(basis, boundary1),
            interval.interior,
        );
        interval.end = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.unwrapAngleNear(
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.angleOnBasis(basis, boundary2),
            interval.interior,
        );

        if (interval.start > interval.end) {
            t = interval.start;
            interval.start = interval.end;
            interval.end = t;
        }

        return interval;
    }

    private static alignCoplanarInterval(
        source: CoplanarAngularInterval | null,
        referenceInterior: number,
    ): CoplanarAngularInterval | null {
        if (source === null) {
            return null;
        }

        const newInterior = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.unwrapAngleNear(
            source.interior,
            referenceInterior,
        );
        const delta = newInterior - source.interior;
        const aligned = new CoplanarAngularInterval();
        aligned.start = source.start + delta;
        aligned.end = source.end + delta;
        aligned.interior = newInterior;
        return aligned;
    }

    private static classifyCoplanarIntervalRelation(
        a: CoplanarAngularInterval | null,
        b: CoplanarAngularInterval | null,
    ): number {
        if (a === null || b === null) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_DISJOINT;
        }

        const alignedB = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.alignCoplanarInterval(b, a.interior)!;
        const overlap = Math.min(a.end, alignedB.end) - Math.max(a.start, alignedB.start);

        if (overlap > _PolyhedralBoundedSolidOperator.numericContext.angleTolerance()) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_OVERLAP;
        }
        if (overlap >= -_PolyhedralBoundedSolidOperator.numericContext.angleTolerance()) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_TOUCHING;
        }
        return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_DISJOINT;
    }

    private static buildIntervalForHalfEdgeSector(
        basis: CoplanarAngleBasis | null,
        he: _PolyhedralBoundedSolidHalfEdge | null,
    ): CoplanarAngularInterval | null {
        let probe: Vector3Dd | null;

        if (
            he === null ||
            he.startingVertex === null ||
            he.previous() === null ||
            he.next() === null ||
            he.previous()!.startingVertex === null ||
            he.next()!.startingVertex === null
        ) {
            return null;
        }

        const ref1 = he.previous()!.startingVertex.position.subtract(he.startingVertex.position);
        const ref2 = he.next()!.startingVertex.position.subtract(he.startingVertex.position);
        probe = _PolyhedralBoundedSolidSetOperator.inside(he);
        if (probe === null || probe.length() <= _PolyhedralBoundedSolidOperator.numericContext.unitVectorTolerance()) {
            probe = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.selectSectorInteriorProbe(
                ref1,
                ref2,
                ref1.crossProduct(ref2),
                null,
            );
        }
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.buildCoplanarAngularInterval(
            basis,
            ref1,
            ref2,
            probe,
        );
    }

    private static buildIntervalForVertexSector(
        basis: CoplanarAngleBasis | null,
        sector: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex | null,
    ): CoplanarAngularInterval | null {
        let fallbackProbe: Vector3Dd | null;

        if (sector === null) {
            return null;
        }

        fallbackProbe = null;
        if (sector.he !== null) {
            fallbackProbe = _PolyhedralBoundedSolidSetOperator.inside(sector.he);
        }
        const probe = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.selectSectorInteriorProbe(
            sector.ref1!,
            sector.ref2!,
            sector.ref12!,
            fallbackProbe,
        );
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.buildCoplanarAngularInterval(
            basis,
            sector.ref1,
            sector.ref2,
            probe,
        );
    }

    private static buildIntervalForCoplanarEdge(
        basis: CoplanarAngleBasis | null,
        edge: _PolyhedralBoundedSolidHalfEdge | null,
        faceNormal: Vector3Dd,
    ): CoplanarAngularInterval | null {
        if (
            edge === null ||
            edge.startingVertex === null ||
            edge.next() === null ||
            edge.next()!.startingVertex === null
        ) {
            return null;
        }

        const edgeDirection = edge.next()!.startingVertex.position.subtract(edge.startingVertex.position);
        const inward = faceNormal.crossProduct(edgeDirection);
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.buildCoplanarAngularInterval(
            basis,
            edgeDirection,
            edgeDirection.multiply(-1.0),
            inward,
        );
    }

    private static classifySectorAgainstReferenceVertex(
        currentInterval: CoplanarAngularInterval | null,
        basis: CoplanarAngleBasis | null,
        referenceFace: _PolyhedralBoundedSolidFace | null,
        referenceVertex: _PolyhedralBoundedSolidVertex | null,
    ): number {
        let i: number;
        let j: number;
        let bestRelation: number;
        let referenceInterval: CoplanarAngularInterval | null;

        if (currentInterval === null || referenceFace === null || referenceVertex === null) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_DISJOINT;
        }

        bestRelation = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_DISJOINT;

        for (i = 0; i < referenceFace.boundariesList.size(); i++) {
            let he: _PolyhedralBoundedSolidHalfEdge;

            const heStart = referenceFace.boundariesList.get(i)!.boundaryStartHalfEdge;
            if (heStart === null) {
                continue;
            }

            he = heStart;
            do {
                if (he.startingVertex === referenceVertex) {
                    referenceInterval =
                        _PolyhedralBoundedSolidSetGeometricPredicateProcessor.buildIntervalForHalfEdgeSector(basis, he);
                    j = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.classifyCoplanarIntervalRelation(
                        currentInterval,
                        referenceInterval,
                    );
                    if (j > bestRelation) {
                        bestRelation = j;
                    }
                    if (
                        bestRelation === _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_OVERLAP
                    ) {
                        return bestRelation;
                    }
                }
                he = he.next()!;
            } while (he !== heStart);
        }

        return bestRelation;
    }

    private static registerCoplanarRelation(
        nbr: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace[],
        index: number,
        relation: number,
    ): void {
        const n = nbr[index]!;
        if (relation > n.coplanarRelation) {
            n.coplanarRelation = relation;
        }
    }

    private static coplanarOpIndex(op: number): number {
        if (op === _PolyhedralBoundedSolidOperator.UNION) {
            return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.COPLANAR_OP_UNION;
        }
        if (op === _PolyhedralBoundedSolidOperator.SUBTRACT) {
            return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.COPLANAR_OP_DIFFERENCE;
        }
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.COPLANAR_OP_INTERSECTION;
    }

    private static coplanarSideIndex(BvsA: number): number {
        return BvsA === 0
            ? _PolyhedralBoundedSolidSetGeometricPredicateProcessor.COPLANAR_SIDE_A_VS_B
            : _PolyhedralBoundedSolidSetGeometricPredicateProcessor.COPLANAR_SIDE_B_VS_A;
    }

    private static coplanarOrientationIndex(sameOrientation: boolean): number {
        return sameOrientation
            ? _PolyhedralBoundedSolidSetGeometricPredicateProcessor.COPLANAR_ORIENTATION_SAME
            : _PolyhedralBoundedSolidSetGeometricPredicateProcessor.COPLANAR_ORIENTATION_OPPOSITE;
    }

    public static classifyCoplanarSectorRelation(
        sectorInfo: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace | null,
        referenceFace: _PolyhedralBoundedSolidFace | null,
    ): number {
        let status: number;

        if (sectorInfo === null || referenceFace === null) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_DISJOINT;
        }

        const he = sectorInfo.sector;
        if (he === null || he.startingVertex === null) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_DISJOINT;
        }

        const start = he.startingVertex.position;
        const containment = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.pointInFaceDetailed(
            referenceFace,
            start,
        );
        status = containment.status();

        if (status === Geometry.INSIDE) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_OVERLAP;
        }
        if (status !== Geometry.LIMIT) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_DISJOINT;
        }

        const basis = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.buildCoplanarAngleBasis(
            he.parentLoop.parentFace.getContainingPlane()!.getNormal(),
            he.next()!.startingVertex.position.subtract(start),
            he.previous()!.startingVertex.position.subtract(start),
        );
        const currentInterval = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.buildIntervalForHalfEdgeSector(
            basis,
            he,
        );
        if (currentInterval === null) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_TOUCHING;
        }

        const intersectedHalfedge = containment.intersectedHalfedge();
        if (intersectedHalfedge !== null) {
            return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.classifyCoplanarIntervalRelation(
                currentInterval,
                _PolyhedralBoundedSolidSetGeometricPredicateProcessor.buildIntervalForCoplanarEdge(
                    basis,
                    intersectedHalfedge,
                    referenceFace.getContainingPlane()!.getNormal(),
                ),
            );
        }
        const intersectedVertex = containment.intersectedVertex();
        if (intersectedVertex !== null) {
            status = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.classifySectorAgainstReferenceVertex(
                currentInterval,
                basis,
                referenceFace,
                intersectedVertex,
            );
            if (status !== _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_DISJOINT) {
                return status;
            }
        }
        return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.COPLANAR_TOUCHING;
    }

    private static resolveCoplanarSectorClass(op: number, BvsA: number, sameOrientation: boolean): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.COPLANAR_VERTEX_FACE_CLASS_TABLE[
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.coplanarOpIndex(op)
        ]![_PolyhedralBoundedSolidSetGeometricPredicateProcessor.coplanarSideIndex(BvsA)]![
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.coplanarOrientationIndex(sameOrientation)
        ]!;
    }
}

type _SectoroverlapTraceEntry = SectoroverlapTraceEntry;
type _CoplanarAngleBasis = CoplanarAngleBasis;
type _CoplanarAngularInterval = CoplanarAngularInterval;

export namespace _PolyhedralBoundedSolidSetGeometricPredicateProcessor {
    export type SectoroverlapTraceEntry = _SectoroverlapTraceEntry;
    /** Java private nested class; exported only as a type for inventory traceability. */
    export type CoplanarAngleBasis = _CoplanarAngleBasis;
    /** Java private nested class; exported only as a type for inventory traceability. */
    export type CoplanarAngularInterval = _CoplanarAngularInterval;
}

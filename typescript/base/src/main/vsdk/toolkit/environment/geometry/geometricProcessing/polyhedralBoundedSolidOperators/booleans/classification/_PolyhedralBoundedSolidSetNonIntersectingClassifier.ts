//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Integer } from "../../../../../../../../java/lang/Integer.js";
import { ArrayList } from "../../../../../../../../java/util/ArrayList.js";
import { Collections } from "../../../../../../../../java/util/Collections.js";
import { Double as JavaDouble } from "../../../../../../../../java/lang/Double.js";
import { Vector2Dd } from "../../../../../../common/linealAlgebra/Vector2Dd.js";
import { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../../../../Geometry.js";
import { Ray } from "../../../../element/Ray.js";
import type { InfinitePlane } from "../../../../surface/InfinitePlane.js";
import { PolyhedralBoundedSolid } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidTopologyEditing } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.js";
import type { _PolyhedralBoundedSolidEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidFace } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSetGeometricPredicateProcessor } from "../intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.js";

/**
Per-`setOp` memo for the expensive, order-independent preflight
predicates. Within a single Boolean operation the operand solids are not
mutated until the Mäntylä generate stage runs (after every preflight has
either fired or declined), so each predicate is a pure function of
`(a, b)` and may be computed once and reused across
`runPartialCoplanarFaceAreaCase`, `runTouchingOnlyPreflightCase`,
`runContainmentOnlyPreflightCase` and `runSetOpNoIntersectionCase`.

<p>The cache only memoizes the heavy primitives (two
`classifySolidAgainstSolid` scans, the interior-overlap grid test and
the two edge/face intersection scans). It does not change any decision: a
request that hits the cache returns exactly the value the uncached path
would have recomputed.</p>
*/
class _PreflightCache {
    private static readonly UNSET = Integer.MIN_VALUE;

    private readonly a: PolyhedralBoundedSolid;
    private readonly b: PolyhedralBoundedSolid;
    private aInBValue = _PreflightCache.UNSET;
    private bInAValue = _PreflightCache.UNSET;
    private interiorOverlap = -1;
    private edgeFaceAB = -1;
    private edgeFaceBA = -1;

    public constructor(a: PolyhedralBoundedSolid, b: PolyhedralBoundedSolid) {
        this.a = a;
        this.b = b;
    }

    public aInB(): number {
        if (this.aInBValue === _PreflightCache.UNSET) {
            this.aInBValue = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifySolidAgainstSolid(
                this.a,
                this.b,
            );
        }
        return this.aInBValue;
    }

    public bInA(): number {
        if (this.bInAValue === _PreflightCache.UNSET) {
            this.bInAValue = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifySolidAgainstSolid(
                this.b,
                this.a,
            );
        }
        return this.bInAValue;
    }

    public hasInteriorOverlap(): boolean {
        if (this.interiorOverlap < 0) {
            this.interiorOverlap = _PolyhedralBoundedSolidSetNonIntersectingClassifier.hasConfirmedInteriorOverlap(
                this.a,
                this.b,
            )
                ? 1
                : 0;
        }
        return this.interiorOverlap === 1;
    }

    public hasEdgeFaceIntersectionAB(): boolean {
        if (this.edgeFaceAB < 0) {
            this.edgeFaceAB = _PolyhedralBoundedSolidSetNonIntersectingClassifier.hasProperEdgeFaceIntersection(
                this.a,
                this.b,
            )
                ? 1
                : 0;
        }
        return this.edgeFaceAB === 1;
    }

    public hasEdgeFaceIntersectionBA(): boolean {
        if (this.edgeFaceBA < 0) {
            this.edgeFaceBA = _PolyhedralBoundedSolidSetNonIntersectingClassifier.hasProperEdgeFaceIntersection(
                this.b,
                this.a,
            )
                ? 1
                : 0;
        }
        return this.edgeFaceBA === 1;
    }
}

/**
Encapsulates preflight classification and no-intersection resolution for
Boolean set operations. This keeps containment/touching policy separate from
the main intersection/splitting pipeline.
*/
export class _PolyhedralBoundedSolidSetNonIntersectingClassifier extends _PolyhedralBoundedSolidOperator {
    private static readonly NO_INT_RELATION_DISJOINT = 0;
    private static readonly NO_INT_RELATION_TOUCHING = 1;
    private static readonly NO_INT_RELATION_A_IN_B = 2;
    private static readonly NO_INT_RELATION_B_IN_A = 3;

    /**
    Builds a fresh per-`setOp` preflight memo for the operand pair. The
    caller is responsible for using it only while `inSolidA`/`inSolidB`
    remain unmutated (i.e. across the preflight block, before the generate
    stage).
    */
    public static newPreflightCache(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): _PreflightCache {
        return new _PreflightCache(inSolidA, inSolidB);
    }

    /**
    §7.3.1.D preflight — detects the case where one solid is strictly
    contained in the other (A⊂B or B⊂A) without any real edge/face
    intersection and without partial coplanar overlap. When this holds,
    the regular intersect/classify/connect pipeline produces ∅ (no
    intersections found → empty boundary), even though set-theoretically
    the result must be one of the operands per Mäntylä Ch. 15.1.

    <p>Together with `runTouchingOnlyPreflightCase`, this routes
    every "no real intersection" geometry to `runSetOpNoIntersectionCase`,
    which has the table-15.1 dispatch for all four cases (disjoint,
    touching, A⊂B, B⊂A).</p>
    */
    public static runContainmentOnlyPreflightCase(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        cache?: _PreflightCache,
    ): boolean {
        if (cache === undefined) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runContainmentOnlyPreflightCase(
                inSolidA,
                inSolidB,
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.newPreflightCache(inSolidA, inSolidB),
            );
        }

        _PolyhedralBoundedSolidOperator.setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB),
        );

        const relation = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyNoIntersectionRelation(
            cache.aInB(),
            cache.bInA(),
        );

        // Restricted to strict containment only. An earlier attempt to
        // extend this preflight to "tangent containment" (one solid
        // sitting inside the other with all its boundary vertices on
        // the other's surface) regressed legitimate cases — those need
        // the regular pipeline because their result expects the inner
        // surface preserved as a hole/cut, not a plain merge. The two
        // absorption-step-2 fixtures (MANT1988_15_2_LIMIT,
        // MANT1988_6_13) that motivate this preflight are tracked in
        // plan §7.3.1.D-cont as remaining drift.
        if (
            relation !== _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_A_IN_B &&
            relation !== _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_B_IN_A
        ) {
            return false;
        }

        if (cache.hasEdgeFaceIntersectionAB()) {
            return false;
        }
        if (cache.hasEdgeFaceIntersectionBA()) {
            return false;
        }

        return true;
    }

    public static runTouchingOnlyPreflightCase(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        cache?: _PreflightCache,
    ): boolean {
        if (cache === undefined) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runTouchingOnlyPreflightCase(
                inSolidA,
                inSolidB,
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.newPreflightCache(inSolidA, inSolidB),
            );
        }

        _PolyhedralBoundedSolidOperator.setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB),
        );

        const relation = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyNoIntersectionRelation(
            cache.aInB(),
            cache.bInA(),
        );

        if (relation !== _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_TOUCHING) {
            return false;
        }

        if (cache.hasInteriorOverlap()) {
            return false;
        }

        if (cache.hasEdgeFaceIntersectionAB()) {
            return false;
        }
        if (cache.hasEdgeFaceIntersectionBA()) {
            return false;
        }

        if (_PolyhedralBoundedSolidSetNonIntersectingClassifier.hasPartialCoplanarFaceAreaOverlap(inSolidA, inSolidB)) {
            return false;
        }

        return true;
    }

    public static runSetOpNoIntersectionCase(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        outRes: PolyhedralBoundedSolid,
        op: number,
        cache?: _PreflightCache,
    ): PolyhedralBoundedSolid {
        if (cache === undefined) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runSetOpNoIntersectionCase(
                inSolidA,
                inSolidB,
                outRes,
                op,
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.newPreflightCache(inSolidA, inSolidB),
            );
        }

        _PolyhedralBoundedSolidOperator.setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB),
        );

        const relation = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyNoIntersectionRelation(
            cache.aInB(),
            cache.bInA(),
        );

        if (op === _PolyhedralBoundedSolidOperator.INTERSECTION) {
            if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_A_IN_B) {
                outRes.merge(inSolidA);
            } else if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_B_IN_A) {
                outRes.merge(inSolidB);
            }
            return outRes;
        }

        if (op === _PolyhedralBoundedSolidOperator.UNION) {
            if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_A_IN_B) {
                outRes.merge(inSolidB);
            } else if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_B_IN_A) {
                outRes.merge(inSolidA);
            } else {
                outRes.merge(inSolidA);
                outRes.merge(inSolidB);
            }
            return outRes;
        }

        if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_A_IN_B) {
            return outRes;
        }
        if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_B_IN_A) {
            outRes.merge(inSolidA);
            inSolidB.revert();
            outRes.merge(inSolidB);
            PolyhedralBoundedSolidTopologyEditing.compactIds(outRes);
            return outRes;
        }
        outRes.merge(inSolidA);
        return outRes;
    }

    public static runPartialCoplanarFaceAreaCase(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        outRes: PolyhedralBoundedSolid,
        op: number,
        cache?: _PreflightCache,
    ): PolyhedralBoundedSolid | null {
        if (cache === undefined) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.runPartialCoplanarFaceAreaCase(
                inSolidA,
                inSolidB,
                outRes,
                op,
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.newPreflightCache(inSolidA, inSolidB),
            );
        }

        let i: number;

        if (op === _PolyhedralBoundedSolidOperator.UNION) {
            return null;
        }

        _PolyhedralBoundedSolidOperator.setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB),
        );

        if (cache.hasInteriorOverlap() || cache.hasEdgeFaceIntersectionAB() || cache.hasEdgeFaceIntersectionBA()) {
            return null;
        }

        const contactPolygons =
            _PolyhedralBoundedSolidSetNonIntersectingClassifier.partialCoplanarFaceAreaOverlapPolygons(
                inSolidA,
                inSolidB,
            );
        if (contactPolygons.isEmpty()) {
            return null;
        }

        if (op === _PolyhedralBoundedSolidOperator.SUBTRACT) {
            outRes.merge(inSolidA);
            return outRes;
        }

        for (i = 0; i < contactPolygons.size(); i++) {
            const lamina = _PolyhedralBoundedSolidSetNonIntersectingClassifier.createLaminaFromPolygon(
                contactPolygons.get(i),
            );
            if (lamina.getPolygonsList().size() > 0) {
                outRes.merge(lamina);
            }
        }

        return outRes;
    }

    private static compareToZero(value: number): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.compareToZero(value);
    }

    private static pointInFace(face: _PolyhedralBoundedSolidFace, point: Vector3Dd): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.pointInFace(face, point);
    }

    /**
    Precomputes the containing plane of every face of `solid`, indexed by
    face position. Point classification tests many points against the same
    unmutated solid, so computing each plane once (instead of once per point per
    face) removes the dominant Newell/tolerance-context recompute from the hot
    path. The returned array is only valid while `solid` is not mutated.
    */
    private static precomputeFacePlanes(solid: PolyhedralBoundedSolid | null): (InfinitePlane | null)[] {
        let n: number;
        let i: number;

        if (solid === null) {
            return [];
        }
        n = solid.getPolygonsList().size();
        const planes = new Array<InfinitePlane | null>(n);
        for (i = 0; i < n; i++) {
            planes[i] = solid.getPolygonsList().get(i)!.getContainingPlane();
        }
        return planes;
    }

    private static classifyPointAgainstSolid(
        solid: PolyhedralBoundedSolid | null,
        facePlanesOrPoint: (InfinitePlane | null)[] | Vector3Dd,
        maybePoint?: Vector3Dd,
    ): number {
        if (maybePoint === undefined) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                solid,
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.precomputeFacePlanes(solid),
                facePlanesOrPoint as Vector3Dd,
            );
        }

        const facePlanes = facePlanesOrPoint as (InfinitePlane | null)[];
        const point = maybePoint;
        let i: number;
        let j: number;
        let face: _PolyhedralBoundedSolidFace;
        const eps = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();
        let insideVotes = 0;
        let outsideVotes = 0;

        if (solid === null || solid.getPolygonsList().size() < 1) {
            return Geometry.OUTSIDE;
        }

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            face = solid.getPolygonsList().get(i)!;
            const facePlane = facePlanes[i]!;
            if (facePlane === null || facePlane === undefined) {
                continue;
            }
            if (Math.abs(facePlane.pointDistance(point)) <= eps) {
                if (face.testPointInside(point, eps, facePlane) !== Geometry.OUTSIDE) {
                    return Geometry.LIMIT;
                }
            }
        }

        const dirs = [
            new Vector3Dd(1.0, 0.371, 0.137),
            new Vector3Dd(0.193, 1.0, 0.417),
            new Vector3Dd(0.217, 0.173, 1.0),
        ];

        for (j = 0; j < dirs.length; j++) {
            let hits = 0;
            let ambiguous = false;
            const distances = new ArrayList<number>();
            const ray = new Ray(point, dirs[j]!);

            for (i = 0; i < solid.getPolygonsList().size(); i++) {
                face = solid.getPolygonsList().get(i)!;
                const facePlane = facePlanes[i]!;
                if (facePlane === null || facePlane === undefined) {
                    ambiguous = true;
                    break;
                }
                const rayHit = new Ray(ray);
                const hit = facePlane.doIntersectionFirstHit(rayHit);
                if (hit === null) {
                    continue;
                }
                if (hit.getT() <= eps) {
                    continue;
                }

                const pi = hit.getOrigin().add(hit.getDirection().multiply(hit.getT()));
                const status = face.testPointInside(pi, eps, facePlane);
                if (status === Geometry.LIMIT) {
                    ambiguous = true;
                    break;
                }
                if (status === Geometry.INSIDE) {
                    let duplicated = false;
                    let k: number;
                    for (k = 0; k < distances.size(); k++) {
                        if (Math.abs(distances.get(k) - hit.getT()) <= eps) {
                            duplicated = true;
                            break;
                        }
                    }
                    if (!duplicated) {
                        distances.add(hit.getT());
                        hits++;
                    }
                }
            }

            if (!ambiguous) {
                if (hits % 2 === 1) {
                    insideVotes++;
                } else {
                    outsideVotes++;
                }
            }
        }

        if (insideVotes > outsideVotes) {
            return Geometry.INSIDE;
        }
        if (outsideVotes > insideVotes) {
            return Geometry.OUTSIDE;
        }
        return Geometry.LIMIT;
    }

    private static overlappingBounds(solidA: PolyhedralBoundedSolid, solidB: PolyhedralBoundedSolid): number[] {
        const a = solidA.getMinMax();
        const b = solidB.getMinMax();

        return [
            Math.max(a[0]!, b[0]!),
            Math.max(a[1]!, b[1]!),
            Math.max(a[2]!, b[2]!),
            Math.min(a[3]!, b[3]!),
            Math.min(a[4]!, b[4]!),
            Math.min(a[5]!, b[5]!),
        ];
    }

    private static hasPositiveOverlapVolume(bounds: number[]): boolean {
        const eps = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();

        return bounds[3]! - bounds[0]! > eps && bounds[4]! - bounds[1]! > eps && bounds[5]! - bounds[2]! > eps;
    }

    /**
    Cheap, constant-size existence probe for a point interior to both solids.
    Tests the 27 quarter/center/three-quarter combinations of the overlap AABB
    `bounds` (a strict superset of the per-axis center samples the full
    grid uses). Returns `true` only on a genuine INSIDE/INSIDE witness, so
    a positive result is always exact; a negative result is inconclusive and the
    caller must still run the exhaustive grid.
    */
    private static hasInteriorOverlapWitnessInAabb(
        bounds: number[],
        solidA: PolyhedralBoundedSolid,
        solidB: PolyhedralBoundedSolid,
        planesA: (InfinitePlane | null)[],
        planesB: (InfinitePlane | null)[],
    ): boolean {
        const sx = _PolyhedralBoundedSolidSetNonIntersectingClassifier.axisProbeCoordinates(bounds[0]!, bounds[3]!);
        const sy = _PolyhedralBoundedSolidSetNonIntersectingClassifier.axisProbeCoordinates(bounds[1]!, bounds[4]!);
        const sz = _PolyhedralBoundedSolidSetNonIntersectingClassifier.axisProbeCoordinates(bounds[2]!, bounds[5]!);
        let i: number;
        let j: number;
        let k: number;

        for (i = 0; i < sx.length; i++) {
            for (j = 0; j < sy.length; j++) {
                for (k = 0; k < sz.length; k++) {
                    const sample = new Vector3Dd(sx[i]!, sy[j]!, sz[k]!);
                    if (
                        _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                            solidA,
                            planesA,
                            sample,
                        ) === Geometry.INSIDE &&
                        _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                            solidB,
                            planesB,
                            sample,
                        ) === Geometry.INSIDE
                    ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static axisProbeCoordinates(min: number, max: number): number[] {
        const span = max - min;

        return [min + span / 4.0, min + span / 2.0, max - span / 4.0];
    }

    private static vertexCoordinate(vertex: _PolyhedralBoundedSolidVertex, axis: number): number {
        if (axis === 0) {
            return vertex.position.x();
        }
        if (axis === 1) {
            return vertex.position.y();
        }
        return vertex.position.z();
    }

    private static appendInteriorVertexCoordinates(
        coords: ArrayList<number>,
        solid: PolyhedralBoundedSolid | null,
        axis: number,
        min: number,
        max: number,
        eps: number,
    ): void {
        let i: number;

        if (solid === null) {
            return;
        }

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const c = _PolyhedralBoundedSolidSetNonIntersectingClassifier.vertexCoordinate(
                solid.getVerticesList().get(i)!,
                axis,
            );
            if (c > min + eps && c < max - eps) {
                coords.add(c);
            }
        }
    }

    private static appendUniqueInteriorSample(
        samples: ArrayList<number>,
        value: number,
        min: number,
        max: number,
        eps: number,
    ): void {
        let i: number;

        if (value <= min + eps || value >= max - eps) {
            return;
        }

        for (i = 0; i < samples.size(); i++) {
            if (Math.abs(samples.get(i) - value) <= eps) {
                return;
            }
        }
        samples.add(value);
    }

    private static sampleCoordinates(
        min: number,
        max: number,
        solidA: PolyhedralBoundedSolid,
        solidB: PolyhedralBoundedSolid,
        axis: number,
    ): number[] {
        const eps = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();
        const center = (min + max) / 2.0;
        const quarter = min + (max - min) / 4.0;
        const threeQuarters = max - (max - min) / 4.0;
        let i: number;

        const coords = new ArrayList<number>();
        coords.add(min);
        coords.add(max);
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendInteriorVertexCoordinates(
            coords,
            solidA,
            axis,
            min,
            max,
            eps,
        );
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendInteriorVertexCoordinates(
            coords,
            solidB,
            axis,
            min,
            max,
            eps,
        );
        Collections.sort(coords);

        const uniqueCoords = new ArrayList<number>();
        for (i = 0; i < coords.size(); i++) {
            const c = coords.get(i);
            if (uniqueCoords.isEmpty() || Math.abs(uniqueCoords.get(uniqueCoords.size() - 1) - c) > eps) {
                uniqueCoords.add(c);
            }
        }

        const samples = new ArrayList<number>();
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendUniqueInteriorSample(samples, quarter, min, max, eps);
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendUniqueInteriorSample(samples, center, min, max, eps);
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendUniqueInteriorSample(
            samples,
            threeQuarters,
            min,
            max,
            eps,
        );

        for (i = 0; i < uniqueCoords.size() - 1; i++) {
            const left = uniqueCoords.get(i);
            const right = uniqueCoords.get(i + 1);
            if (right - left > eps) {
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendUniqueInteriorSample(
                    samples,
                    (left + right) / 2.0,
                    min,
                    max,
                    eps,
                );
            }
        }

        Collections.sort(samples);
        if (samples.isEmpty()) {
            return [center];
        }

        const result = new Array<number>(samples.size());
        for (i = 0; i < samples.size(); i++) {
            result[i] = samples.get(i);
        }
        return result;
    }

    public static hasConfirmedInteriorOverlap(
        solidA: PolyhedralBoundedSolid | null,
        solidB: PolyhedralBoundedSolid | null,
    ): boolean {
        let bounds: number[];
        let xs: number[];
        let ys: number[];
        let zs: number[];
        let i: number;
        let j: number;
        let k: number;

        if (solidA === null || solidB === null) {
            return false;
        }

        bounds = _PolyhedralBoundedSolidSetNonIntersectingClassifier.overlappingBounds(solidA, solidB);
        if (!_PolyhedralBoundedSolidSetNonIntersectingClassifier.hasPositiveOverlapVolume(bounds)) {
            return false;
        }

        // Precompute each solid's face planes once; every probe/grid point is
        // classified against the same unmutated solids, so this removes the
        // per-point Newell/tolerance-context recompute (P2 scoped cache).
        const planesA = _PolyhedralBoundedSolidSetNonIntersectingClassifier.precomputeFacePlanes(solidA);
        const planesB = _PolyhedralBoundedSolidSetNonIntersectingClassifier.precomputeFacePlanes(solidB);

        // P1.2 — cheap early-positive pass. hasConfirmedInteriorOverlap is an
        // existence test ("is there a point interior to both solids"); any
        // witness gives the same answer. Probe a constant-size set of likely
        // interior candidates (the 27 quarter/center/three-quarter combinations
        // of the overlap AABB) before paying for the full vertex-derived grid,
        // which can reach 10^4-10^6 points for the bowl. This only returns true
        // on a genuine INSIDE/INSIDE witness; the exhaustive grid below remains
        // the exact fallback for the negative case.
        if (
            _PolyhedralBoundedSolidSetNonIntersectingClassifier.hasInteriorOverlapWitnessInAabb(
                bounds,
                solidA,
                solidB,
                planesA,
                planesB,
            )
        ) {
            return true;
        }

        xs = _PolyhedralBoundedSolidSetNonIntersectingClassifier.sampleCoordinates(
            bounds[0]!,
            bounds[3]!,
            solidA,
            solidB,
            0,
        );
        ys = _PolyhedralBoundedSolidSetNonIntersectingClassifier.sampleCoordinates(
            bounds[1]!,
            bounds[4]!,
            solidA,
            solidB,
            1,
        );
        zs = _PolyhedralBoundedSolidSetNonIntersectingClassifier.sampleCoordinates(
            bounds[2]!,
            bounds[5]!,
            solidA,
            solidB,
            2,
        );

        for (i = 0; i < xs.length; i++) {
            for (j = 0; j < ys.length; j++) {
                for (k = 0; k < zs.length; k++) {
                    const sample = new Vector3Dd(xs[i]!, ys[j]!, zs[k]!);
                    if (
                        _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                            solidA,
                            planesA,
                            sample,
                        ) === Geometry.INSIDE &&
                        _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                            solidB,
                            planesB,
                            sample,
                        ) === Geometry.INSIDE
                    ) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static classifySolidAgainstSolid(
        solidA: PolyhedralBoundedSolid | null,
        solidB: PolyhedralBoundedSolid | null,
    ): number {
        let i: number;
        let sawLimit = false;
        let sawOutside = false;

        if (solidA === null || solidA.getVerticesList().size() < 1) {
            return Geometry.OUTSIDE;
        }

        const planesB = _PolyhedralBoundedSolidSetNonIntersectingClassifier.precomputeFacePlanes(solidB);
        for (i = 0; i < solidA.getVerticesList().size(); i++) {
            const v = solidA.getVerticesList().get(i)!;
            const status = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                solidB,
                planesB,
                v.position,
            );
            if (status === Geometry.INSIDE) {
                return Geometry.INSIDE;
            }
            if (status === Geometry.LIMIT) {
                sawLimit = true;
            } else {
                sawOutside = true;
            }
        }

        if (sawLimit) {
            return Geometry.LIMIT;
        }
        if (sawOutside) {
            return Geometry.OUTSIDE;
        }
        return Geometry.OUTSIDE;
    }

    private static classifyNoIntersectionRelation(aInB: number, bInA: number): number {
        if (aInB === Geometry.INSIDE) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_A_IN_B;
        }
        if (bInA === Geometry.INSIDE) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_B_IN_A;
        }
        if (aInB === Geometry.LIMIT || bInA === Geometry.LIMIT) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_TOUCHING;
        }
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_DISJOINT;
    }

    public static hasProperEdgeFaceIntersection(
        current: PolyhedralBoundedSolid | null,
        other: PolyhedralBoundedSolid | null,
    ): boolean {
        let i: number;
        let j: number;
        let edge: _PolyhedralBoundedSolidEdge;
        let face: _PolyhedralBoundedSolidFace;
        let v1: _PolyhedralBoundedSolidVertex;
        let v2: _PolyhedralBoundedSolidVertex;
        let d1: number;
        let d2: number;
        let d3: number;
        let t: number;
        let s1: number;
        let s2: number;
        let p: Vector3Dd;

        if (current === null || other === null) {
            return false;
        }

        for (i = 0; i < current.getEdgesList().size(); i++) {
            edge = current.getEdgesList().get(i)!;
            if (edge === null || edge.rightHalf === null || edge.leftHalf === null) {
                continue;
            }
            v1 = edge.rightHalf.startingVertex;
            v2 = edge.leftHalf.startingVertex;
            if (v1 === null || v2 === null) {
                continue;
            }

            for (j = 0; j < other.getPolygonsList().size(); j++) {
                face = other.getPolygonsList().get(j)!;
                if (face === null) {
                    continue;
                }
                const facePlane = face.getContainingPlane();
                if (facePlane === null) {
                    continue;
                }

                d1 = facePlane.pointDistance(v1.position);
                d2 = facePlane.pointDistance(v2.position);
                s1 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.compareToZero(d1);
                s2 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.compareToZero(d2);

                if (!((s1 === -1 && s2 === 1) || (s1 === 1 && s2 === -1))) {
                    continue;
                }

                t = d1 / (d1 - d2);
                p = v1.position.add(v2.position.subtract(v1.position).multiply(t));
                d3 = facePlane.pointDistance(p);
                if (_PolyhedralBoundedSolidSetNonIntersectingClassifier.compareToZero(d3) !== 0) {
                    continue;
                }

                if (_PolyhedralBoundedSolidSetNonIntersectingClassifier.pointInFace(face, p) === Geometry.INSIDE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static hasPartialCoplanarFaceAreaOverlap(
        solidA: PolyhedralBoundedSolid | null,
        solidB: PolyhedralBoundedSolid | null,
    ): boolean {
        let i: number;
        let j: number;

        if (solidA === null || solidB === null) {
            return false;
        }

        for (i = 0; i < solidA.getPolygonsList().size(); i++) {
            const faceA = solidA.getPolygonsList().get(i)!;
            for (j = 0; j < solidB.getPolygonsList().size(); j++) {
                const faceB = solidB.getPolygonsList().get(j)!;
                if (
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.coplanarFaces(faceA, faceB) &&
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.partialCoplanarFaceAreaOverlap(faceA, faceB)
                ) {
                    return true;
                }
            }
        }

        return false;
    }

    private static partialCoplanarFaceAreaOverlapPolygons(
        solidA: PolyhedralBoundedSolid | null,
        solidB: PolyhedralBoundedSolid | null,
    ): ArrayList<ArrayList<Vector3Dd>> {
        let i: number;
        let j: number;

        const polygons = new ArrayList<ArrayList<Vector3Dd>>();
        if (solidA === null || solidB === null) {
            return polygons;
        }

        for (i = 0; i < solidA.getPolygonsList().size(); i++) {
            const faceA = solidA.getPolygonsList().get(i)!;
            for (j = 0; j < solidB.getPolygonsList().size(); j++) {
                const faceB = solidB.getPolygonsList().get(j)!;
                if (
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.coplanarFaces(faceA, faceB) &&
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.partialCoplanarFaceAreaOverlap(faceA, faceB)
                ) {
                    const polygon = _PolyhedralBoundedSolidSetNonIntersectingClassifier.coplanarFaceIntersectionPolygon(
                        faceA,
                        faceB,
                    );
                    if (polygon.size() >= 3) {
                        polygons.add(polygon);
                    }
                }
            }
        }

        return polygons;
    }

    private static coplanarFaces(
        faceA: _PolyhedralBoundedSolidFace | null,
        faceB: _PolyhedralBoundedSolidFace | null,
    ): boolean {
        if (faceA === null || faceB === null) {
            return false;
        }
        const planeA = faceA.getContainingPlane();
        const planeB = faceB.getContainingPlane();
        if (planeA === null || planeB === null) {
            return false;
        }
        if (
            !PolyhedralBoundedSolidNumericPolicy.unitVectorsParallel(
                planeA.getNormal(),
                planeB.getNormal(),
                _PolyhedralBoundedSolidOperator.numericContext,
            )
        ) {
            return false;
        }
        if (faceB.boundariesList.size() < 1 || faceB.boundariesList.get(0)!.boundaryStartHalfEdge === null) {
            return false;
        }

        return (
            Math.abs(
                planeA.pointDistance(faceB.boundariesList.get(0)!.boundaryStartHalfEdge!.startingVertex.position),
            ) <= _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()
        );
    }

    private static partialCoplanarFaceAreaOverlap(
        faceA: _PolyhedralBoundedSolidFace,
        faceB: _PolyhedralBoundedSolidFace,
    ): boolean {
        if (
            _PolyhedralBoundedSolidSetNonIntersectingClassifier.faceHasInteriorVertexOrEdgeMidpoint(faceA, faceB) ||
            _PolyhedralBoundedSolidSetNonIntersectingClassifier.faceHasInteriorVertexOrEdgeMidpoint(faceB, faceA)
        ) {
            return true;
        }

        return _PolyhedralBoundedSolidSetNonIntersectingClassifier.faceBoundariesCrossProperly(faceA, faceB);
    }

    private static coplanarFaceIntersectionPolygon(
        faceA: _PolyhedralBoundedSolidFace,
        faceB: _PolyhedralBoundedSolidFace,
    ): ArrayList<Vector3Dd> {
        const points = new ArrayList<Vector3Dd>();
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendFaceVerticesInsideOther(points, faceA, faceB);
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendFaceVerticesInsideOther(points, faceB, faceA);
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendBoundaryIntersections(points, faceA, faceB);
        const planeNormalA = faceA.getContainingPlane()!.getNormal();
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.sortCoplanarPolygon(points, planeNormalA);

        if (
            _PolyhedralBoundedSolidSetNonIntersectingClassifier.coplanarPolygonAreaMagnitude(points, planeNormalA) <=
            _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon() *
                _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()
        ) {
            points.clear();
        }

        return points;
    }

    private static appendFaceVerticesInsideOther(
        points: ArrayList<Vector3Dd>,
        source: _PolyhedralBoundedSolidFace,
        target: _PolyhedralBoundedSolidFace,
    ): void {
        let i: number;

        for (i = 0; i < source.boundariesList.size(); i++) {
            const loop = source.boundariesList.get(i);
            let start: _PolyhedralBoundedSolidHalfEdge;
            let he: _PolyhedralBoundedSolidHalfEdge | null;

            if (loop === null || loop.boundaryStartHalfEdge === null) {
                continue;
            }
            start = loop.boundaryStartHalfEdge;
            he = start;
            do {
                if (
                    target.testPointInside(
                        he!.startingVertex.position,
                        _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon(),
                    ) !== Geometry.OUTSIDE
                ) {
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendUniquePoint(
                        points,
                        he!.startingVertex.position,
                    );
                }
                he = he!.next();
            } while (he !== null && he !== start);
        }
    }

    private static appendBoundaryIntersections(
        points: ArrayList<Vector3Dd>,
        faceA: _PolyhedralBoundedSolidFace,
        faceB: _PolyhedralBoundedSolidFace,
    ): void {
        let i: number;
        let j: number;
        const dominantCoordinate = _PolyhedralBoundedSolidSetNonIntersectingClassifier.dominantCoordinateForFace(faceA);

        for (i = 0; i < faceA.boundariesList.size(); i++) {
            const loopA = faceA.boundariesList.get(i);
            if (loopA === null) {
                continue;
            }
            for (j = 0; j < faceB.boundariesList.size(); j++) {
                const loopB = faceB.boundariesList.get(j);
                if (loopB !== null) {
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendLoopIntersections(
                        points,
                        loopA,
                        loopB,
                        dominantCoordinate,
                    );
                }
            }
        }
    }

    private static appendLoopIntersections(
        points: ArrayList<Vector3Dd>,
        loopA: _PolyhedralBoundedSolidLoop,
        loopB: _PolyhedralBoundedSolidLoop,
        dominantCoordinate: number,
    ): void {
        let i: number;
        let j: number;

        for (i = 0; i < loopA.halfEdgesList.size(); i++) {
            const heA = loopA.halfEdgesList.get(i);
            if (heA === null || heA.next() === null) {
                continue;
            }
            for (j = 0; j < loopB.halfEdgesList.size(); j++) {
                const heB = loopB.halfEdgesList.get(j);
                if (heB === null || heB.next() === null) {
                    continue;
                }
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendSegmentIntersection(
                    points,
                    heA,
                    heB,
                    dominantCoordinate,
                );
            }
        }
    }

    private static appendSegmentIntersection(
        points: ArrayList<Vector3Dd>,
        heA: _PolyhedralBoundedSolidHalfEdge,
        heB: _PolyhedralBoundedSolidHalfEdge,
        dominantCoordinate: number,
    ): void {
        const a1 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.projectPointTo2D(
            heA.startingVertex.position,
            dominantCoordinate,
        );
        const a2 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.projectPointTo2D(
            heA.next()!.startingVertex.position,
            dominantCoordinate,
        );
        const b1 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.projectPointTo2D(
            heB.startingVertex.position,
            dominantCoordinate,
        );
        const b2 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.projectPointTo2D(
            heB.next()!.startingVertex.position,
            dominantCoordinate,
        );
        let den: number;
        let t: number;
        let da: Vector2Dd;
        let db: Vector2Dd;
        let ba: Vector2Dd;

        if (!_PolyhedralBoundedSolidSetNonIntersectingClassifier.segmentsCrossProperly2D(a1, a2, b1, b2)) {
            return;
        }

        da = new Vector2Dd(a2.x - a1.x, a2.y - a1.y);
        db = new Vector2Dd(b2.x - b1.x, b2.y - b1.y);
        ba = new Vector2Dd(b1.x - a1.x, b1.y - a1.y);
        den = _PolyhedralBoundedSolidSetNonIntersectingClassifier.cross2D(da, db);
        if (Math.abs(den) <= _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()) {
            return;
        }

        t = _PolyhedralBoundedSolidSetNonIntersectingClassifier.cross2D(ba, db) / den;
        _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendUniquePoint(
            points,
            heA.startingVertex.position.add(
                heA.next()!.startingVertex.position.subtract(heA.startingVertex.position).multiply(t),
            ),
        );
    }

    private static cross2D(a: Vector2Dd, b: Vector2Dd): number {
        return a.x * b.y - a.y * b.x;
    }

    private static appendUniquePoint(points: ArrayList<Vector3Dd>, point: Vector3Dd): void {
        let i: number;

        for (i = 0; i < points.size(); i++) {
            if (
                PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    points.get(i),
                    point,
                    _PolyhedralBoundedSolidOperator.numericContext,
                )
            ) {
                return;
            }
        }
        points.add(new Vector3Dd(point));
    }

    private static sortCoplanarPolygon(points: ArrayList<Vector3Dd>, normal: Vector3Dd): void {
        let center: Vector3Dd;
        let u: Vector3Dd;
        let v: Vector3Dd;
        let n: Vector3Dd;
        let i: number;

        if (points.size() < 3) {
            return;
        }

        center = new Vector3Dd();
        for (i = 0; i < points.size(); i++) {
            center = center.add(points.get(i));
        }
        center = center.multiply(1.0 / points.size());

        n = new Vector3Dd(normal).normalized();
        u = points.get(0).subtract(center);
        if (u.length() <= _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()) {
            return;
        }
        u = u.normalized();
        v = n.crossProduct(u).normalized();

        const sortCenter = center;
        const sortU = u;
        const sortV = v;
        Collections.sort(points, {
            compare: (p1: Vector3Dd, p2: Vector3Dd): number => {
                const d1 = p1.subtract(sortCenter);
                const d2 = p2.subtract(sortCenter);
                const a1 = Math.atan2(d1.dotProduct(sortV), d1.dotProduct(sortU));
                const a2 = Math.atan2(d2.dotProduct(sortV), d2.dotProduct(sortU));
                return JavaDouble.compare(a1, a2);
            },
        });
    }

    private static coplanarPolygonAreaMagnitude(points: ArrayList<Vector3Dd>, normal: Vector3Dd): number {
        let accumulator: Vector3Dd;
        let i: number;

        if (points.size() < 3) {
            return 0.0;
        }

        accumulator = new Vector3Dd();
        for (i = 0; i < points.size(); i++) {
            const p = points.get(i);
            const q = points.get((i + 1) % points.size());
            accumulator = accumulator.add(p.crossProduct(q));
        }

        return Math.abs(accumulator.dotProduct(normal.normalized())) * 0.5;
    }

    private static createLaminaFromPolygon(points: ArrayList<Vector3Dd>): PolyhedralBoundedSolid {
        let solid: PolyhedralBoundedSolid;
        let i: number;

        solid = new PolyhedralBoundedSolid();
        if (points.size() < 3) {
            return solid;
        }

        PolyhedralBoundedSolidEulerOperators.mvfs(solid, points.get(0), 1, 1);
        for (i = 1; i < points.size(); i++) {
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1, points.get(i));
        }
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, points.size(), 1, 2);
        return solid;
    }

    private static faceHasInteriorVertexOrEdgeMidpoint(
        source: _PolyhedralBoundedSolidFace,
        target: _PolyhedralBoundedSolidFace,
    ): boolean {
        let i: number;

        for (i = 0; i < source.boundariesList.size(); i++) {
            const loop = source.boundariesList.get(i);
            let start: _PolyhedralBoundedSolidHalfEdge;
            let he: _PolyhedralBoundedSolidHalfEdge | null;

            if (loop === null || loop.boundaryStartHalfEdge === null) {
                continue;
            }
            start = loop.boundaryStartHalfEdge;
            he = start;
            do {
                if (
                    target.testPointInside(
                        he!.startingVertex.position,
                        _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon(),
                    ) === Geometry.INSIDE
                ) {
                    return true;
                }
                if (he!.next() !== null) {
                    const midpoint = he!.startingVertex.position.add(
                        he!.next()!.startingVertex.position.subtract(he!.startingVertex.position).multiply(0.5),
                    );
                    if (
                        target.testPointInside(
                            midpoint,
                            _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon(),
                        ) === Geometry.INSIDE
                    ) {
                        return true;
                    }
                }
                he = he!.next();
            } while (he !== null && he !== start);
        }

        return false;
    }

    private static faceBoundariesCrossProperly(
        faceA: _PolyhedralBoundedSolidFace,
        faceB: _PolyhedralBoundedSolidFace,
    ): boolean {
        let i: number;
        let j: number;
        const dominantCoordinate = _PolyhedralBoundedSolidSetNonIntersectingClassifier.dominantCoordinateForFace(faceA);

        for (i = 0; i < faceA.boundariesList.size(); i++) {
            const loopA = faceA.boundariesList.get(i);
            if (loopA === null) {
                continue;
            }
            for (j = 0; j < faceB.boundariesList.size(); j++) {
                const loopB = faceB.boundariesList.get(j);
                if (
                    loopB !== null &&
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.loopsCrossProperly(
                        loopA,
                        loopB,
                        dominantCoordinate,
                    )
                ) {
                    return true;
                }
            }
        }

        return false;
    }

    private static loopsCrossProperly(
        loopA: _PolyhedralBoundedSolidLoop,
        loopB: _PolyhedralBoundedSolidLoop,
        dominantCoordinate: number,
    ): boolean {
        let i: number;
        let j: number;

        for (i = 0; i < loopA.halfEdgesList.size(); i++) {
            const heA = loopA.halfEdgesList.get(i);
            if (heA === null || heA.next() === null) {
                continue;
            }
            const a1 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.projectPointTo2D(
                heA.startingVertex.position,
                dominantCoordinate,
            );
            const a2 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.projectPointTo2D(
                heA.next()!.startingVertex.position,
                dominantCoordinate,
            );

            for (j = 0; j < loopB.halfEdgesList.size(); j++) {
                const heB = loopB.halfEdgesList.get(j);
                if (heB === null || heB.next() === null) {
                    continue;
                }
                const b1 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.projectPointTo2D(
                    heB.startingVertex.position,
                    dominantCoordinate,
                );
                const b2 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.projectPointTo2D(
                    heB.next()!.startingVertex.position,
                    dominantCoordinate,
                );
                if (_PolyhedralBoundedSolidSetNonIntersectingClassifier.segmentsCrossProperly2D(a1, a2, b1, b2)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static dominantCoordinateForFace(face: _PolyhedralBoundedSolidFace): number {
        const n = face.getContainingPlane()!.getNormal();

        if (Math.abs(n.x()) >= Math.abs(n.y()) && Math.abs(n.x()) >= Math.abs(n.z())) {
            return 1;
        }
        if (Math.abs(n.y()) >= Math.abs(n.x()) && Math.abs(n.y()) >= Math.abs(n.z())) {
            return 2;
        }
        return 3;
    }

    private static projectPointTo2D(inPoint: Vector3Dd, dominantCoordinate: number): Vector2Dd {
        if (dominantCoordinate === 1) {
            return new Vector2Dd(inPoint.y(), inPoint.z());
        }
        if (dominantCoordinate === 2) {
            return new Vector2Dd(inPoint.x(), inPoint.z());
        }
        return new Vector2Dd(inPoint.x(), inPoint.y());
    }

    private static orientation2D(a: Vector2Dd, b: Vector2Dd, c: Vector2Dd): number {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static segmentsCrossProperly2D(a1: Vector2Dd, a2: Vector2Dd, b1: Vector2Dd, b2: Vector2Dd): boolean {
        const o1 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.orientation2D(a1, a2, b1);
        const o2 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.orientation2D(a1, a2, b2);
        const o3 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.orientation2D(b1, b2, a1);
        const o4 = _PolyhedralBoundedSolidSetNonIntersectingClassifier.orientation2D(b1, b2, a2);
        let tolerance = PolyhedralBoundedSolidNumericPolicy.orientationTolerance2D(
            a1,
            a2,
            b1,
            _PolyhedralBoundedSolidOperator.numericContext,
        );

        tolerance = Math.max(
            tolerance,
            PolyhedralBoundedSolidNumericPolicy.orientationTolerance2D(
                a1,
                a2,
                b2,
                _PolyhedralBoundedSolidOperator.numericContext,
            ),
        );
        tolerance = Math.max(
            tolerance,
            PolyhedralBoundedSolidNumericPolicy.orientationTolerance2D(
                b1,
                b2,
                a1,
                _PolyhedralBoundedSolidOperator.numericContext,
            ),
        );
        tolerance = Math.max(
            tolerance,
            PolyhedralBoundedSolidNumericPolicy.orientationTolerance2D(
                b1,
                b2,
                a2,
                _PolyhedralBoundedSolidOperator.numericContext,
            ),
        );

        return (
            ((o1 > tolerance && o2 < -tolerance) || (o1 < -tolerance && o2 > tolerance)) &&
            ((o3 > tolerance && o4 < -tolerance) || (o3 < -tolerance && o4 > tolerance))
        );
    }
}

type __PreflightCache = _PreflightCache;

export namespace _PolyhedralBoundedSolidSetNonIntersectingClassifier {
    export type _PreflightCache = __PreflightCache;
}

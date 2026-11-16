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
import { PolyhedralBoundedSolidFaceGeometry } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.js";
import { PolyhedralBoundedSolidGeometricValidator } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidTopologyEditing } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.js";
import type { _PolyhedralBoundedSolidEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidFace } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { NonManifoldBooleanResultException } from "../../NonManifoldBooleanResultException.js";
import { _PolyhedralBoundedSolidInteriorOverlap } from "./_PolyhedralBoundedSolidInteriorOverlap.js";
import { _PolyhedralBoundedSolidSetGeometricPredicateProcessor } from "../intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.js";

/**
Per-`setOp` memo for the expensive, order-independent preflight
predicates. Within a single Boolean operation the operand solids are not
mutated until the Mäntylä generate stage runs (after every preflight has
either fired or declined), so each predicate is a pure function of
`(a, b)` and may be computed once and reused across
`runTouchingOnlyPreflightCase`,
`runContainmentOnlyPreflightCase` and `runSetOpNoIntersectionCase`.

<p>The cache only memoizes the heavy primitives (two
`classifySolidAgainstSolid` scans, the interior-overlap test and
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

        if (cache.hasEdgeFaceIntersectionAB()) {
            return false;
        }
        if (cache.hasEdgeFaceIntersectionBA()) {
            return false;
        }

        // Solids that share part of a face and nothing else are a contact
        // too (STACKED_BLOCKS): the interiors do not overlap. A union along
        // such a face is glued by the pipeline; the caller keeps it out.
        if (cache.hasInteriorOverlap()) {
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

        let relation = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyNoIntersectionRelation(
            cache.aInB(),
            cache.bInA(),
        );

        // Tangent containment (B-02 b): one solid inside the other with
        // faces on the other's boundary. The vertex test cannot see it
        // (every vertex is "on"), so it reports touching. Only the answers
        // that need no cavity use it; B \ A-style cavities need the
        // pipeline.
        if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_TOUCHING) {
            if (_PolyhedralBoundedSolidSetNonIntersectingClassifier.isTangentlyContained(inSolidA, inSolidB)) {
                relation = _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_A_IN_B;
            } else if (
                op !== _PolyhedralBoundedSolidOperator.SUBTRACT &&
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.isTangentlyContained(inSolidB, inSolidA)
            ) {
                relation = _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_B_IN_A;
            }
        }

        if (op === _PolyhedralBoundedSolidOperator.INTERSECTION) {
            if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_A_IN_B) {
                outRes.merge(inSolidA);
            } else if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_B_IN_A) {
                outRes.merge(inSolidB);
            } else if (!cache.hasInteriorOverlap()) {
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.appendContactLaminae(inSolidA, inSolidB, outRes);
            }
            return outRes;
        }

        if (op === _PolyhedralBoundedSolidOperator.UNION) {
            if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_A_IN_B) {
                outRes.merge(inSolidB);
            } else if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_B_IN_A) {
                outRes.merge(inSolidA);
            } else if (relation === _PolyhedralBoundedSolidSetNonIntersectingClassifier.NO_INT_RELATION_TOUCHING) {
                // Two shells sharing only an edge or a vertex: a
                // non-manifold result, rejected by design (B-04). Face
                // contact never gets here: the pipeline glues it.
                throw new NonManifoldBooleanResultException(
                    "UNION of solids touching only along edges or at " +
                        "vertices is non-manifold (two shells sharing a " +
                        "boundary point); rejected",
                );
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

    /**
    Lamina policy for the intersection of solids that share part of a face
    and nothing else (STACKED_BLOCKS): the shared part is returned as
    laminae, zero volume, valid and easy to display, not as the regularised
    empty set. A contact whose area is below the collapsed face threshold
    gives no lamina: a collapsed lamina is not a valid result (see
    _GeometricCollapsedLaminaStrategy).

    <p>This is part of the "no intersections found" resolution of the
    pipeline ([MANT1988] 15.4). Until plan-stage11 Phase E a preflight
    answered these pairs before generate and classify ran.</p>
    */
    private static appendContactLaminae(
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
        outRes: PolyhedralBoundedSolid,
    ): void {
        const contactPolygons =
            _PolyhedralBoundedSolidSetNonIntersectingClassifier.partialCoplanarFaceAreaOverlapPolygons(
                inSolidA,
                inSolidB,
            );
        let i: number;

        for (i = 0; i < contactPolygons.size(); i++) {
            if (
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.polygonArea(contactPolygons.get(i)) <
                PolyhedralBoundedSolidGeometricValidator.collapsedFaceAreaThreshold(
                    _PolyhedralBoundedSolidOperator.numericContext,
                )
            ) {
                continue;
            }
            const lamina = _PolyhedralBoundedSolidSetNonIntersectingClassifier.createLaminaFromPolygon(
                contactPolygons.get(i),
            );
            if (lamina.getPolygonsList().size() > 0) {
                outRes.merge(lamina);
            }
        }
    }

    private static compareToZero(value: number): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.compareToZero(value);
    }

    private static pointInFace(face: _PolyhedralBoundedSolidFace, point: Vector3Dd): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.pointInFace(face, point);
    }

    /**
    Precomputes the containing plane and the bounding box of every face of
    `solid`, indexed by face position. Point classification tests many
    points against the same unmutated solid, so computing each plane once
    (instead of once per point per face) removes the dominant
    Newell/tolerance-context recompute from the hot path, and the box skips
    the point-in-face test for points far from the face (Stage G). The
    returned array is only valid while `solid` is not mutated.
    */
    private static precomputeFacePlanes(solid: PolyhedralBoundedSolid | null): PolyhedralBoundedSolidFaceGeometry[] {
        if (solid === null) {
            return [];
        }
        return PolyhedralBoundedSolidFaceGeometry.of(solid);
    }

    /** @internal Java package-private (the three-argument overload). */
    public static classifyPointAgainstSolid(
        solid: PolyhedralBoundedSolid | null,
        facePlanesOrPoint: PolyhedralBoundedSolidFaceGeometry[] | Vector3Dd,
        maybePoint?: Vector3Dd,
    ): number {
        if (maybePoint === undefined) {
            return _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                solid,
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.precomputeFacePlanes(solid),
                facePlanesOrPoint as Vector3Dd,
            );
        }

        const facePlanes = facePlanesOrPoint as PolyhedralBoundedSolidFaceGeometry[];
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
            const facePlane = facePlanes[i]!.getPlane();
            if (facePlane === null) {
                continue;
            }
            if (Math.abs(facePlane.pointDistance(point)) <= eps && !facePlanes[i]!.projectionExcludes(point, eps)) {
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
                const facePlane = facePlanes[i]!.getPlane();
                if (facePlane === null) {
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
                if (facePlanes[i]!.projectionExcludes(pi, eps)) {
                    continue;
                }
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

    /**
    Witness test used to tell a right empty result from a lost solid
    (Fable5.2Analysis.md Stage F step 4): true when some point is interior
    to both solids, so their intersection cannot be empty. The operands
    must be intact (not consumed by a set operation).
    @internal Java package-private.
    */
    public static interiorsOverlap(solidA: PolyhedralBoundedSolid, solidB: PolyhedralBoundedSolid): boolean {
        _PolyhedralBoundedSolidOperator.setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB),
        );
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier.hasConfirmedInteriorOverlap(solidA, solidB);
    }

    /**
    Witness test for A \ B: true when a vertex or a face interior witness
    of `solidA` lies strictly outside `solidB`. A boundary point of A
    outside B has interior points of A around it that are outside B, so
    A \ B cannot be empty. (The converse does not hold, so a false answer
    proves nothing.)
    @internal Java package-private.
    */
    public static hasBoundaryPointOutside(solidA: PolyhedralBoundedSolid, solidB: PolyhedralBoundedSolid): boolean {
        let i: number;

        _PolyhedralBoundedSolidOperator.setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB),
        );
        const planesB = _PolyhedralBoundedSolidSetNonIntersectingClassifier.precomputeFacePlanes(solidB);
        for (i = 0; i < solidA.getVerticesList().size(); i++) {
            if (
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                    solidB,
                    planesB,
                    solidA.getVerticesList().get(i)!.position,
                ) === Geometry.OUTSIDE
            ) {
                return true;
            }
        }
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier.someFaceWitnessIsOutside(solidA, solidB, planesB);
    }

    /**
    True when some point is strictly inside both solids. Decided from the
    boundaries by `_PolyhedralBoundedSolidInteriorOverlap`, without
    sampling.
    @internal Java private; public here because `_PreflightCache` is a
    module-level class.
    */
    public static hasConfirmedInteriorOverlap(solidA: PolyhedralBoundedSolid, solidB: PolyhedralBoundedSolid): boolean {
        return _PolyhedralBoundedSolidInteriorOverlap.interiorsOverlap(solidA, solidB);
    }

    /**
    Classifies solid A against solid B for the "no proper intersection"
    preflights (Fable5.2Analysis.md Stage D step 1, defect B-03).
    - INSIDE: A is contained in B. No vertex of A is outside B, at least
      one is strictly inside, and a witness point interior to every face of
      A is not outside B. The witness test separates tangent containment
      from a solid whose edges lie in a face of B while part of it sticks
      out (B-03): there every vertex is in or on B, yet a face is not.
    - LIMIT: A touches B, or straddles its boundary (vertices on both
      sides, or a face witness outside). Callers must not take LIMIT as
      containment; the touching preflight additionally requires that the
      interiors do not overlap.
    - OUTSIDE: every vertex of A is outside B.
    Before this rule the first vertex found inside made A "contained", even
    with other vertices outside.
    @internal Java private; public here because `_PreflightCache` is a
    module-level class.
    */
    public static classifySolidAgainstSolid(
        solidA: PolyhedralBoundedSolid | null,
        solidB: PolyhedralBoundedSolid | null,
    ): number {
        let i: number;
        let inside = 0;
        let limit = 0;
        let outside = 0;

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
                inside++;
            } else if (status === Geometry.LIMIT) {
                limit++;
            } else {
                outside++;
            }
        }

        if (outside > 0) {
            return inside > 0 || limit > 0 ? Geometry.LIMIT : Geometry.OUTSIDE;
        }
        if (inside === 0) {
            return Geometry.LIMIT;
        }
        if (_PolyhedralBoundedSolidSetNonIntersectingClassifier.someFaceWitnessIsOutside(solidA, solidB!, planesB)) {
            return Geometry.LIMIT;
        }
        return Geometry.INSIDE;
    }

    /**
    True when no vertex and no face witness of `inner` is outside
    `outer`, and at least one face witness is strictly inside it:
    containment with boundary contact.
    */
    private static isTangentlyContained(inner: PolyhedralBoundedSolid, outer: PolyhedralBoundedSolid): boolean {
        let i: number;
        let someInside = false;
        const planes = _PolyhedralBoundedSolidSetNonIntersectingClassifier.precomputeFacePlanes(outer);

        for (i = 0; i < inner.getVerticesList().size(); i++) {
            if (
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                    outer,
                    planes,
                    inner.getVerticesList().get(i)!.position,
                ) === Geometry.OUTSIDE
            ) {
                return false;
            }
        }
        for (i = 0; i < inner.getPolygonsList().size(); i++) {
            const witness = _PolyhedralBoundedSolidSetNonIntersectingClassifier.faceInteriorWitness(
                inner.getPolygonsList().get(i)!,
            );
            if (witness === null) {
                continue;
            }
            const status = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                outer,
                planes,
                witness,
            );
            if (status === Geometry.OUTSIDE) {
                return false;
            }
            if (status === Geometry.INSIDE) {
                someInside = true;
            }
        }
        return someInside;
    }

    /**
    True when some vertex of `inner`, or a point inside one of its faces,
    lies on the boundary of `outer`. Meant for a solid already known to be
    contained in `outer`.
    @internal Java package-private.
    */
    public static touchesBoundaryFromInside(inner: PolyhedralBoundedSolid, outer: PolyhedralBoundedSolid): boolean {
        let i: number;

        _PolyhedralBoundedSolidOperator.setNumericContext(PolyhedralBoundedSolidNumericPolicy.forSolids(inner, outer));
        const planes = _PolyhedralBoundedSolidSetNonIntersectingClassifier.precomputeFacePlanes(outer);
        for (i = 0; i < inner.getVerticesList().size(); i++) {
            if (
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                    outer,
                    planes,
                    inner.getVerticesList().get(i)!.position,
                ) === Geometry.LIMIT
            ) {
                return true;
            }
        }
        for (i = 0; i < inner.getPolygonsList().size(); i++) {
            const witness = _PolyhedralBoundedSolidSetNonIntersectingClassifier.faceInteriorWitness(
                inner.getPolygonsList().get(i)!,
            );
            if (
                witness !== null &&
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                    outer,
                    planes,
                    witness,
                ) === Geometry.LIMIT
            ) {
                return true;
            }
        }
        return false;
    }

    private static someFaceWitnessIsOutside(
        solidA: PolyhedralBoundedSolid,
        solidB: PolyhedralBoundedSolid,
        planesB: PolyhedralBoundedSolidFaceGeometry[],
    ): boolean {
        let i: number;

        for (i = 0; i < solidA.getPolygonsList().size(); i++) {
            const witness = _PolyhedralBoundedSolidSetNonIntersectingClassifier.faceInteriorWitness(
                solidA.getPolygonsList().get(i)!,
            );
            if (
                witness !== null &&
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                    solidB,
                    planesB,
                    witness,
                ) === Geometry.OUTSIDE
            ) {
                return true;
            }
        }
        return false;
    }

    /**
    A point strictly inside the face: the centroid of the first triangle of
    consecutive outer-loop vertices whose centroid the face classifies as
    INSIDE (holes and concave corners are rejected by that test).
    @return the witness, or null when no such triangle exists
    @internal Java package-private.
    */
    public static faceInteriorWitness(face: _PolyhedralBoundedSolidFace | null): Vector3Dd | null {
        let loop: _PolyhedralBoundedSolidLoop | null;
        let he: _PolyhedralBoundedSolidHalfEdge;
        let plane: InfinitePlane | null;
        const eps = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();

        if (face === null || face.boundariesList.size() < 1) {
            return null;
        }
        plane = face.getContainingPlane();
        loop = face.boundariesList.get(0);
        if (plane === null || loop === null || loop.boundaryStartHalfEdge === null) {
            return null;
        }
        he = loop.boundaryStartHalfEdge;
        do {
            const n1 = he.next();
            if (n1 === null || n1.next() === null) {
                return null;
            }
            const c = he.startingVertex.position
                .add(n1.startingVertex.position)
                .add(n1.next()!.startingVertex.position)
                .multiply(1.0 / 3.0);
            if (face.testPointInside(c, eps, plane) === Geometry.INSIDE) {
                return c;
            }
            he = n1;
        } while (he !== loop.boundaryStartHalfEdge);
        return null;
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
        let otherFaces: PolyhedralBoundedSolidFaceGeometry[];

        if (current === null || other === null) {
            return false;
        }

        otherFaces = _PolyhedralBoundedSolidSetNonIntersectingClassifier.precomputeFacePlanes(other);
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
                const facePlane = otherFaces[j]!.getPlane();
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

    /**
    True when a face of A and a face of B lie on the same plane with
    opposite orientation and share a region of positive area: the solids
    touch along a face (Fable5.2Analysis.md B-04). Two cubes sharing a whole
    face are detected through the interior witness of one face, since none
    of their vertices or edge midpoints is inside the other face.
    @internal Java package-private.
    */
    public static hasOppositeCoplanarFaceAreaContact(
        solidA: PolyhedralBoundedSolid,
        solidB: PolyhedralBoundedSolid,
    ): boolean {
        let i: number;
        let j: number;

        _PolyhedralBoundedSolidOperator.setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB),
        );
        for (i = 0; i < solidA.getPolygonsList().size(); i++) {
            const faceA = solidA.getPolygonsList().get(i)!;
            for (j = 0; j < solidB.getPolygonsList().size(); j++) {
                const faceB = solidB.getPolygonsList().get(j)!;
                if (
                    !_PolyhedralBoundedSolidSetNonIntersectingClassifier.coplanarFaces(faceA, faceB) ||
                    faceA.getContainingPlane()!.getNormal().dotProduct(faceB.getContainingPlane()!.getNormal()) >= 0.0
                ) {
                    continue;
                }
                if (
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.partialCoplanarFaceAreaOverlap(faceA, faceB) ||
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.witnessInsideFace(faceA, faceB) ||
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier.witnessInsideFace(faceB, faceA)
                ) {
                    return true;
                }
            }
        }
        return false;
    }

    private static witnessInsideFace(
        source: _PolyhedralBoundedSolidFace,
        target: _PolyhedralBoundedSolidFace,
    ): boolean {
        const witness = _PolyhedralBoundedSolidSetNonIntersectingClassifier.faceInteriorWitness(source);

        return (
            witness !== null &&
            target.testPointInside(witness, _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon()) ===
                Geometry.INSIDE
        );
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

    private static polygonArea(points: ArrayList<Vector3Dd>): number {
        let nx = 0.0;
        let ny = 0.0;
        let nz = 0.0;
        let i: number;

        for (i = 0; i < points.size(); i++) {
            const p = points.get(i);
            const q = points.get((i + 1) % points.size());
            nx += (p.y() - q.y()) * (p.z() + q.z());
            ny += (p.z() - q.z()) * (p.x() + q.x());
            nz += (p.x() - q.x()) * (p.y() + q.y());
        }
        return 0.5 * Math.sqrt(nx * nx + ny * ny + nz * nz);
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

//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { ArrayList } from "../../../../../../../../java/util/ArrayList.js";
import { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../../../../Geometry.js";
import type { InfinitePlane } from "../../../../surface/InfinitePlane.js";
import type { PolyhedralBoundedSolid } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidFaceGeometry } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.js";
import type { _PolyhedralBoundedSolidFace } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSetNonIntersectingClassifier } from "./_PolyhedralBoundedSolidSetNonIntersectingClassifier.js";

/** A segment in the plane of the face being cut, in plane coordinates. */
class Cut {
    public readonly startX: number;
    public readonly startY: number;
    public readonly endX: number;
    public readonly endY: number;

    public constructor(startX: number, startY: number, endX: number, endY: number) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }
}

/**
Decides whether the interiors of two solids overlap: whether some point is
strictly inside both. The "no proper intersection" preflights of the set
operator ([MANT1988] 15.4, problem 15.1) and the "this result cannot be
empty" guard of `setOp` rest on this decision.

<p>The decision follows from the boundaries, with no sampling of space
(plan-stage11 Phase D). If the interiors overlap in a region R, the
boundary of R is made of pieces of the two boundaries, so one of these
holds, and each of them is enough:</p>
<ul>
<li>part of a face of one solid is strictly inside the other;</li>
<li>a face of one lies on a face of the other with the material of both on
    the same side (then the points just under the shared part are inside
    both).</li>
</ul>

<p>Each face F of one solid is therefore cut by everything of the other
solid Y that meets its plane: the segments where the faces of Y cross that
plane and the edges of Y that lie in it. Together with the edges of F
these segments divide F into cells, and inside one cell the answer to "is
this point inside Y" does not change. Every cell has one of those segments
on its border, so a point beside the middle of every piece of every
segment, on both sides, visits every cell. When such a point is inside Y
or on its boundary, the point just under it, inside the solid of F, is
tried: if it is inside both solids it is the witness. The answer is true
only with a witness in hand.</p>

<p>The points stand at 4 tolerances from the segments and from the face,
so an overlap thinner than about 5 tolerances is not seen: at that size
the two boundaries are the same surface for the kernel, and the pair is
a contact.</p>

<p>It replaced a test by sampling: 27 probes in the common bounding box,
then a grid built from the vertex coordinates of both solids. That test
missed overlaps that fell between the samples (two prisms with coplanar
ends overlapping in a corner, InteriorOverlapPredicateTest) and its cost
grew with the cube of the number of vertices.</p>
*/
export class _PolyhedralBoundedSolidInteriorOverlap extends _PolyhedralBoundedSolidOperator {
    /** Distance of the probe points from the cutting segments, in tolerances. */
    private static readonly CLEARANCE_IN_TOLERANCES = 4.0;

    private constructor() {
        super();
    }

    /**
    True when some point is strictly inside both solids. The numeric
    context must be the one of the pair (the caller sets it).
    @internal Java package-private.
    */
    public static interiorsOverlap(
        solidA: PolyhedralBoundedSolid | null,
        solidB: PolyhedralBoundedSolid | null,
    ): boolean {
        if (
            solidA === null ||
            solidB === null ||
            solidA.getPolygonsList().size() < 1 ||
            solidB.getPolygonsList().size() < 1
        ) {
            return false;
        }
        const eps = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();
        const a = solidA.getMinMax();
        const b = solidB.getMinMax();
        let axis: number;

        for (axis = 0; axis < 3; axis++) {
            if (Math.min(a[axis + 3]!, b[axis + 3]!) - Math.max(a[axis]!, b[axis]!) <= eps) {
                return false;
            }
        }

        const facesA = PolyhedralBoundedSolidFaceGeometry.of(solidA);
        const facesB = PolyhedralBoundedSolidFaceGeometry.of(solidB);

        return (
            _PolyhedralBoundedSolidInteriorOverlap.someFaceCellIsInside(solidA, facesA, solidB, facesB) ||
            _PolyhedralBoundedSolidInteriorOverlap.someFaceCellIsInside(solidB, facesB, solidA, facesA)
        );
    }

    private static someFaceCellIsInside(
        solid: PolyhedralBoundedSolid,
        faces: PolyhedralBoundedSolidFaceGeometry[],
        other: PolyhedralBoundedSolid,
        otherFaces: PolyhedralBoundedSolidFaceGeometry[],
    ): boolean {
        const eps = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();
        let i: number;
        let j: number;

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            const plane = faces[i]!.getPlane();
            if (plane === null) {
                continue;
            }
            const normal = _PolyhedralBoundedSolidInteriorOverlap.unit(plane.getNormal());
            const origin = _PolyhedralBoundedSolidInteriorOverlap.firstVertex(face);
            if (normal === null || origin === null) {
                continue;
            }
            const u = _PolyhedralBoundedSolidInteriorOverlap.inPlaneAxis(normal)!;
            const v = normal.crossProduct(u);
            const cuts = new ArrayList<Cut>();

            _PolyhedralBoundedSolidInteriorOverlap.addEdges(face, origin, u, v, cuts);
            for (j = 0; j < other.getPolygonsList().size(); j++) {
                if (otherFaces[j]!.getPlane() === null || faces[i]!.boxSeparatedFrom(otherFaces[j]!, eps)) {
                    continue;
                }
                _PolyhedralBoundedSolidInteriorOverlap.addCutsOfFace(
                    plane,
                    normal,
                    origin,
                    u,
                    v,
                    other.getPolygonsList().get(j)!,
                    otherFaces[j]!.getPlane()!,
                    cuts,
                );
            }
            if (
                _PolyhedralBoundedSolidInteriorOverlap.someCellIsInside(
                    face,
                    plane,
                    normal,
                    origin,
                    u,
                    v,
                    cuts,
                    solid,
                    faces,
                    other,
                    otherFaces,
                )
            ) {
                return true;
            }
        }
        return false;
    }

    /** The edges of every loop of `face`, as cuts. */
    private static addEdges(
        face: _PolyhedralBoundedSolidFace,
        origin: Vector3Dd,
        u: Vector3Dd,
        v: Vector3Dd,
        cuts: ArrayList<Cut>,
    ): void {
        let i: number;

        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop = face.boundariesList.get(i)!;
            const start = loop.boundaryStartHalfEdge;
            let he = start;
            if (he === null) {
                continue;
            }
            do {
                const next: _PolyhedralBoundedSolidHalfEdge | null = he!.next();
                if (next === null) {
                    break;
                }
                _PolyhedralBoundedSolidInteriorOverlap.addCut(
                    he!.startingVertex.position,
                    next.startingVertex.position,
                    origin,
                    u,
                    v,
                    cuts,
                );
                he = next;
            } while (he !== start);
        }
    }

    /**
    What `otherFace` leaves on the plane of the face being cut: its edges
    that lie in the plane, and the stretches of the line where the two
    planes meet that are inside `otherFace`.
    */
    private static addCutsOfFace(
        plane: InfinitePlane,
        normal: Vector3Dd,
        origin: Vector3Dd,
        u: Vector3Dd,
        v: Vector3Dd,
        otherFace: _PolyhedralBoundedSolidFace,
        otherPlane: InfinitePlane,
        cuts: ArrayList<Cut>,
    ): void {
        const eps = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();
        const crossings = new ArrayList<Vector3Dd>();
        let i: number;
        let j: number;

        for (i = 0; i < otherFace.boundariesList.size(); i++) {
            const loop = otherFace.boundariesList.get(i)!;
            const start = loop.boundaryStartHalfEdge;
            let he = start;
            if (he === null) {
                continue;
            }
            do {
                const next: _PolyhedralBoundedSolidHalfEdge | null = he!.next();
                if (next === null) {
                    break;
                }
                const p = he!.startingVertex.position;
                const q = next.startingVertex.position;
                const dp = plane.pointDistance(p);
                const dq = plane.pointDistance(q);
                const pOn = Math.abs(dp) <= eps;
                const qOn = Math.abs(dq) <= eps;
                if (pOn && qOn) {
                    _PolyhedralBoundedSolidInteriorOverlap.addCut(p, q, origin, u, v, cuts);
                }
                if (pOn) {
                    crossings.add(p);
                } else if (!qOn && dp < 0.0 !== dq < 0.0) {
                    crossings.add(p.add(q.subtract(p).multiply(dp / (dp - dq))));
                }
                he = next;
            } while (he !== start);
        }
        if (crossings.size() < 2) {
            return;
        }
        const direction = normal.crossProduct(otherPlane.getNormal());
        if (
            direction.length() <=
            _PolyhedralBoundedSolidOperator.numericContext.unitVectorTolerance() * otherPlane.getNormal().length()
        ) {
            // Coplanar faces: the edges in the plane are all there is.
            return;
        }
        // Sort the crossings along the line where the planes meet.
        for (i = 1; i < crossings.size(); i++) {
            const moving = crossings.get(i);
            const key = moving.dotProduct(direction);
            for (j = i - 1; j >= 0 && crossings.get(j).dotProduct(direction) > key; j--) {
                crossings.set(j + 1, crossings.get(j));
            }
            crossings.set(j + 1, moving);
        }
        for (i = 0; i + 1 < crossings.size(); i++) {
            const p = crossings.get(i);
            const q = crossings.get(i + 1);
            if (q.subtract(p).length() <= eps) {
                continue;
            }
            const middle = p.add(q).multiply(0.5);
            if (otherFace.testPointInside(middle, eps, otherPlane) !== Geometry.OUTSIDE) {
                _PolyhedralBoundedSolidInteriorOverlap.addCut(p, q, origin, u, v, cuts);
            }
        }
    }

    private static addCut(
        p: Vector3Dd,
        q: Vector3Dd,
        origin: Vector3Dd,
        u: Vector3Dd,
        v: Vector3Dd,
        cuts: ArrayList<Cut>,
    ): void {
        const a = p.subtract(origin);
        const b = q.subtract(origin);

        cuts.add(new Cut(a.dotProduct(u), a.dotProduct(v), b.dotProduct(u), b.dotProduct(v)));
    }

    /**
    Visits every cell the cuts leave on the face, through a point on each
    side of the middle of every piece of every cut.
    */
    private static someCellIsInside(
        face: _PolyhedralBoundedSolidFace,
        plane: InfinitePlane,
        normal: Vector3Dd,
        origin: Vector3Dd,
        u: Vector3Dd,
        v: Vector3Dd,
        cuts: ArrayList<Cut>,
        solid: PolyhedralBoundedSolid,
        faces: PolyhedralBoundedSolidFaceGeometry[],
        other: PolyhedralBoundedSolid,
        otherFaces: PolyhedralBoundedSolidFaceGeometry[],
    ): boolean {
        const eps = _PolyhedralBoundedSolidOperator.numericContext.bigEpsilon();
        const clearance = _PolyhedralBoundedSolidInteriorOverlap.CLEARANCE_IN_TOLERANCES * eps;
        let i: number;
        let k: number;
        let side: number;

        for (i = 0; i < cuts.size(); i++) {
            const cut = cuts.get(i);
            const rx = cut.endX - cut.startX;
            const ry = cut.endY - cut.startY;
            const length = Math.sqrt(rx * rx + ry * ry);
            if (length <= eps) {
                continue;
            }
            const stops = _PolyhedralBoundedSolidInteriorOverlap.stopsAlong(cut, i, cuts, eps);
            for (k = 0; k + 1 < stops.length; k++) {
                if ((stops[k + 1]! - stops[k]!) * length <= eps) {
                    continue;
                }
                const t = (stops[k]! + stops[k + 1]!) / 2.0;
                for (side = -1; side <= 1; side += 2) {
                    const x = cut.startX + t * rx - (side * clearance * ry) / length;
                    const y = cut.startY + t * ry + (side * clearance * rx) / length;
                    const point = origin.add(u.multiply(x)).add(v.multiply(y));
                    if (face.testPointInside(point, eps, plane) !== Geometry.INSIDE) {
                        continue;
                    }
                    if (
                        _PolyhedralBoundedSolidInteriorOverlap.isWitness(
                            point,
                            normal,
                            clearance,
                            solid,
                            faces,
                            other,
                            otherFaces,
                        )
                    ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
    `point` is in the interior of a face of `solid`, and inside `other` or
    on its boundary. It proves the overlap when a point just off the face,
    at the clearance distance, is inside both solids. Asking for that
    point, and not only for `point` to be inside `other`, keeps the answer
    out of the tolerance band: a face that enters the other solid by one
    tolerance is a contact, not an overlap.
    */
    private static isWitness(
        point: Vector3Dd,
        normal: Vector3Dd,
        clearance: number,
        solid: PolyhedralBoundedSolid,
        faces: PolyhedralBoundedSolidFaceGeometry[],
        other: PolyhedralBoundedSolid,
        otherFaces: PolyhedralBoundedSolidFaceGeometry[],
    ): boolean {
        const status = _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
            other,
            otherFaces,
            point,
        );
        let side: number;

        if (status === Geometry.OUTSIDE) {
            return false;
        }
        for (side = -1; side <= 1; side += 2) {
            const under = point.add(normal.multiply(side * clearance));
            if (
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(solid, faces, under) ===
                    Geometry.INSIDE &&
                _PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid(
                    other,
                    otherFaces,
                    under,
                ) === Geometry.INSIDE
            ) {
                return true;
            }
        }
        return false;
    }

    /**
    Parameters in [0, 1] where `cut` is met by the other cuts, sorted, with
    0 and 1 at the ends: crossings, T-junctions and the ends of the cuts
    that run along it.
    */
    private static stopsAlong(cut: Cut, index: number, cuts: ArrayList<Cut>, eps: number): number[] {
        const rx = cut.endX - cut.startX;
        const ry = cut.endY - cut.startY;
        const length2 = rx * rx + ry * ry;
        const length = Math.sqrt(length2);
        const stops = new ArrayList<number>();
        let i: number;
        let j: number;

        stops.add(0.0);
        stops.add(1.0);
        for (i = 0; i < cuts.size(); i++) {
            if (i === index) {
                continue;
            }
            const o = cuts.get(i);
            const wx = o.endX - o.startX;
            const wy = o.endY - o.startY;
            const otherLength = Math.sqrt(wx * wx + wy * wy);
            const cross = rx * wy - ry * wx;
            // Ends of the other cut that lie on this one.
            _PolyhedralBoundedSolidInteriorOverlap.addStopAt(
                o.startX,
                o.startY,
                cut,
                rx,
                ry,
                length,
                length2,
                eps,
                stops,
            );
            _PolyhedralBoundedSolidInteriorOverlap.addStopAt(o.endX, o.endY, cut, rx, ry, length, length2, eps, stops);
            if (
                otherLength <= eps ||
                Math.abs(cross) <=
                    _PolyhedralBoundedSolidOperator.numericContext.unitVectorTolerance() * length * otherLength
            ) {
                continue;
            }
            const qx = o.startX - cut.startX;
            const qy = o.startY - cut.startY;
            const t = (qx * wy - qy * wx) / cross;
            const s = (qx * ry - qy * rx) / cross;
            if (t > 0.0 && t < 1.0 && s * otherLength >= -eps && (s - 1.0) * otherLength <= eps) {
                stops.add(t);
            }
        }
        const sorted = new Array<number>(stops.size());
        for (i = 0; i < sorted.length; i++) {
            sorted[i] = stops.get(i);
        }
        for (i = 1; i < sorted.length; i++) {
            const moving = sorted[i]!;
            for (j = i - 1; j >= 0 && sorted[j]! > moving; j--) {
                sorted[j + 1] = sorted[j]!;
            }
            sorted[j + 1] = moving;
        }
        return sorted;
    }

    private static addStopAt(
        x: number,
        y: number,
        cut: Cut,
        rx: number,
        ry: number,
        length: number,
        length2: number,
        eps: number,
        stops: ArrayList<number>,
    ): void {
        const qx = x - cut.startX;
        const qy = y - cut.startY;
        const t = (qx * rx + qy * ry) / length2;

        if (t > 0.0 && t < 1.0 && Math.abs(qx * ry - qy * rx) / length <= eps) {
            stops.add(t);
        }
    }

    private static firstVertex(face: _PolyhedralBoundedSolidFace): Vector3Dd | null {
        if (face.boundariesList.size() < 1 || face.boundariesList.get(0)!.boundaryStartHalfEdge === null) {
            return null;
        }
        return face.boundariesList.get(0)!.boundaryStartHalfEdge!.startingVertex.position;
    }

    private static unit(vector: Vector3Dd): Vector3Dd | null {
        const length = vector.length();

        return length > 0.0 ? vector.multiply(1.0 / length) : null;
    }

    /** A unit vector perpendicular to the unit vector `normal`. */
    private static inPlaneAxis(normal: Vector3Dd): Vector3Dd | null {
        const ax = Math.abs(normal.x());
        const ay = Math.abs(normal.y());
        const az = Math.abs(normal.z());
        const helper =
            ax <= ay && ax <= az ? new Vector3Dd(1, 0, 0) : ay <= az ? new Vector3Dd(0, 1, 0) : new Vector3Dd(0, 0, 1);

        return _PolyhedralBoundedSolidInteriorOverlap.unit(normal.crossProduct(helper));
    }
}

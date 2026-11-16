//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { ArrayList } from "../../../../../../java/util/ArrayList.js";
import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import type { InfinitePlane } from "../../surface/InfinitePlane.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "./_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSectorClassification } from "./_PolyhedralBoundedSolidSectorClassification.js";

/**
Neighbourhood of a vertex classified against a plane through it, following
section [MANT1988].14.5.2 and program [MANT1988].14.4. Problem
[MANT1988].15.4 observes that the vertex/face classifier of the set
operations is the same procedure as the splitter's; both build their
neighbourhood here (Fable5.2Analysis.md pending item 11) and keep their own
reclassification rules: [MANT1988].14.5 for the splitter, table
[MANT1988].15.3 (which depends on the operation) for the set operations.
*/
export class _PolyhedralBoundedSolidVertexNeighborhood extends _PolyhedralBoundedSolidOperator {
    private constructor() {
        super();
    }

    /**
    Classifies the neighbourhood of `vtx` against `plane`. The loop turns
    with mirror(he).next(), so the sector of `he` (its face, between
    he.previous() and he) lies between the entry of the previous half-edge
    and the entry of `he`: an entry for the far end of each edge, preceded
    by an entry for the bisector of the sector when the sector is reflex.
    Convex and flat sectors are bounded by their two edge entries; a
    reflex one is split by its bisector into two sectors narrower than a
    half-turn, as the book requires. Edge entries that lie on the plane are
    tagged INPLANE_EDGE.
    @param factory creates the entry type of the caller
    */
    public static classify<T extends _PolyhedralBoundedSolidSectorClassification>(
        vtx: _PolyhedralBoundedSolidVertex,
        plane: InfinitePlane,
        factory: () => T,
    ): ArrayList<T> {
        const neighbors = new ArrayList<T>();
        let he = vtx.emanatingHalfEdge!;
        let c: T;

        do {
            if (_PolyhedralBoundedSolidVertexNeighborhood.isReflexSector(he)) {
                const bisect = _PolyhedralBoundedSolidVertexNeighborhood.inwardReflexBisector(he);
                c = factory();
                c.sector = he;
                c.classification = _PolyhedralBoundedSolidVertexNeighborhood.compareToZero(plane.pointDistance(bisect));
                c.isWide = true;
                c.position = new Vector3Dd(bisect);
                c.situation = _PolyhedralBoundedSolidSectorClassification.CROSSING_EDGE;
                neighbors.add(c);
            }
            const far = he.next()!.startingVertex.position;
            c = factory();
            c.sector = he;
            c.classification = _PolyhedralBoundedSolidVertexNeighborhood.compareToZero(plane.pointDistance(far));
            c.isWide = false;
            c.position = new Vector3Dd(far);
            c.situation =
                c.classification === _PolyhedralBoundedSolidSectorClassification.ON
                    ? _PolyhedralBoundedSolidSectorClassification.INPLANE_EDGE
                    : _PolyhedralBoundedSolidSectorClassification.UNDEFINED;
            neighbors.add(c);
            he = he.mirrorHalfEdge()!.next()!;
        } while (he !== vtx.emanatingHalfEdge);

        return neighbors;
    }

    /**
    True when the face sector at he.startingVertex, between he.previous()
    and he, is reflex: (next - v) x (prev - v) points against the face
    normal (loops run counterclockwise about the normal). A flat sector (a
    vertex inserted inside an edge) is not: its two edge entries already
    bound it, and a bisector sample there lies on the plane whenever the
    face crosses it, which the edge reclassification would then turn into a
    spurious entry. The test compares the sine of the angle, so it does not
    depend on the size of the model.
    */
    public static isReflexSector(he: _PolyhedralBoundedSolidHalfEdge): boolean {
        const v = he.startingVertex.position;
        const a = he.next()!.startingVertex.position.subtract(v);
        const b = he.previous()!.startingVertex.position.subtract(v);
        const n = he.parentLoop.parentFace.getContainingPlane()!.getNormal();
        const axb = a.crossProduct(b);

        if (
            axb.length() <=
            _PolyhedralBoundedSolidOperator.numericContext.unitVectorTolerance() * a.length() * b.length()
        ) {
            return false;
        }
        return axb.dotProduct(n) < 0.0;
    }

    /**
    A point on the bisector of the reflex face corner at
    `he.startingVertex`, inside the face, at half the length of the shorter
    of its two edges: only its side of a plane through the vertex is used,
    and at that distance the comparison with the tolerance is relative to
    the local size of the model. For a reflex corner the sum of the two
    unit edge directions points out of the face, so it is negated.
    */
    public static inwardReflexBisector(he: _PolyhedralBoundedSolidHalfEdge): Vector3Dd {
        const v = he.startingVertex.position;
        const toNext = he.next()!.startingVertex.position.subtract(v);
        const toPrevious = he.previous()!.startingVertex.position.subtract(v);
        const reach = 0.5 * Math.min(toNext.length(), toPrevious.length());
        const a = _PolyhedralBoundedSolidVertexNeighborhood.unit(toNext);
        const b = _PolyhedralBoundedSolidVertexNeighborhood.unit(toPrevious);
        let sum = a.add(b);

        if (sum.length() <= _PolyhedralBoundedSolidOperator.numericContext.unitVectorTolerance()) {
            // Straight angle: any inward normal works.
            sum = he.parentLoop.parentFace.getContainingPlane()!.getNormal().crossProduct(a).multiply(-1.0);
        }
        return v.subtract(_PolyhedralBoundedSolidVertexNeighborhood.unit(sum).multiply(reach));
    }

    private static unit(v: Vector3Dd): Vector3Dd {
        const length = v.length();
        return length > 0.0 ? v.multiply(1.0 / length) : v;
    }

    private static compareToZero(value: number): number {
        return PolyhedralBoundedSolidNumericPolicy.compareToZero(value, _PolyhedralBoundedSolidOperator.numericContext);
    }
}

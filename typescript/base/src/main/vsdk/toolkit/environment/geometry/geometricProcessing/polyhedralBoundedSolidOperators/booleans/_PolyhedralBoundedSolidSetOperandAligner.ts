//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import type { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidFaceGeometry } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidFace } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidOperator } from "../_PolyhedralBoundedSolidOperator.js";

/**
The one decision of the set operations on whether two faces coincide.

<p>A face of B that is parallel to a face of A, next to it, and closer
than two tolerances is, for the kernel, the same plane. Left as it is, each
stage decides that on its own: generate with the distance of each vertex,
the classifiers with the planes, the contact decision with interior
points. Inside that band they disagree: one finds a crossing where another
finds a contact, the intersection curve does not close, and the operation
is rejected (51 of the 432 operations of the near-degenerate gate,
plan-stage11).</p>

<p>The decision is taken here, once, before any stage runs: B is
translated, as a rigid body, by the least displacement that puts those
faces on the planes of their partners. After it every stage sees exact
coincidence, which it handles. A translation keeps every face of B planar
and every angle; moving single vertices would not. The displacement is at
most two tolerances per direction, the size the kernel already takes for
zero.</p>

<p>When the pairs ask for displacements that cannot all hold (two faces of
B close to two parallel faces of A at different distances), nothing is
moved and the operands go on as they are.</p>

<p>This works on the copies of the operands that the operation owns. Only
B is moved, so A u B and B u A can differ by that displacement.</p>
*/
export class _PolyhedralBoundedSolidSetOperandAligner extends _PolyhedralBoundedSolidOperator {
    /** Width of the band, in tolerances, on each side of a plane. */
    private static readonly BAND_IN_TOLERANCES = 2.0;

    /** Distances below this fraction of the tolerance are already zero. */
    private static readonly ALREADY_ALIGNED = 1.0e-6;

    private constructor() {
        super();
    }

    /**
    Moves `solidB` so that its faces inside the band of a parallel face of
    `solidA` lie on that face's plane.
    @return the displacement applied, or null when nothing was moved
    */
    public static align(solidA: PolyhedralBoundedSolid, solidB: PolyhedralBoundedSolid): Vector3Dd | null {
        const context = PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB);
        const tolerance = context.bigEpsilon();
        const band = _PolyhedralBoundedSolidSetOperandAligner.BAND_IN_TOLERANCES * tolerance;
        const facesA = PolyhedralBoundedSolidFaceGeometry.of(solidA);
        const facesB = PolyhedralBoundedSolidFaceGeometry.of(solidB);
        const basis: Vector3Dd[] = [];
        let displacement = new Vector3Dd(0, 0, 0);
        let found = false;
        let i: number;
        let j: number;
        let k: number;

        for (i = 0; i < facesA.length; i++) {
            const planeA = facesA[i]!.getPlane();
            const pointA = _PolyhedralBoundedSolidSetOperandAligner.firstVertex(solidA.getPolygonsList().get(i)!);
            if (planeA === null || pointA === null) {
                continue;
            }
            const normalA = _PolyhedralBoundedSolidSetOperandAligner.unit(planeA.getNormal());
            for (j = 0; j < facesB.length; j++) {
                const planeB = facesB[j]!.getPlane();
                const pointB = _PolyhedralBoundedSolidSetOperandAligner.firstVertex(solidB.getPolygonsList().get(j)!);
                if (planeB === null || pointB === null || facesA[i]!.boxSeparatedFrom(facesB[j]!, band)) {
                    continue;
                }
                const normalB = _PolyhedralBoundedSolidSetOperandAligner.unit(planeB.getNormal());
                if (normalA.crossProduct(normalB).length() > context.unitVectorTolerance()) {
                    continue;
                }
                const distance = pointB.subtract(pointA).dotProduct(normalA);
                if (
                    Math.abs(distance) > band ||
                    Math.abs(distance) <= _PolyhedralBoundedSolidSetOperandAligner.ALREADY_ALIGNED * tolerance
                ) {
                    continue;
                }
                // Wanted: normalA . displacement = -distance. Each new
                // direction is taken perpendicular to the ones already
                // used, so that the earlier pairs stay aligned.
                const residual = -distance - normalA.dotProduct(displacement);
                let free = normalA;
                for (k = 0; k < basis.length; k++) {
                    free = free.subtract(basis[k]!.multiply(free.dotProduct(basis[k]!)));
                }
                if (free.length() <= context.unitVectorTolerance()) {
                    if (Math.abs(residual) > _PolyhedralBoundedSolidSetOperandAligner.ALREADY_ALIGNED * tolerance) {
                        // Two pairs along one direction at different
                        // distances: no translation serves both.
                        return null;
                    }
                    continue;
                }
                displacement = displacement.add(free.multiply(residual / normalA.dotProduct(free)));
                basis.push(_PolyhedralBoundedSolidSetOperandAligner.unit(free));
                found = true;
            }
        }
        if (!found) {
            return null;
        }
        for (i = 0; i < solidB.getVerticesList().size(); i++) {
            const vertex = solidB.getVerticesList().get(i)!;
            vertex.position = vertex.position.add(displacement);
        }
        return displacement;
    }

    private static firstVertex(face: _PolyhedralBoundedSolidFace): Vector3Dd | null {
        if (face.boundariesList.size() < 1 || face.boundariesList.get(0)!.boundaryStartHalfEdge === null) {
            return null;
        }
        return face.boundariesList.get(0)!.boundaryStartHalfEdge!.startingVertex.position;
    }

    private static unit(vector: Vector3Dd): Vector3Dd {
        const length = vector.length();

        return length > 0.0 ? vector.multiply(1.0 / length) : vector;
    }
}

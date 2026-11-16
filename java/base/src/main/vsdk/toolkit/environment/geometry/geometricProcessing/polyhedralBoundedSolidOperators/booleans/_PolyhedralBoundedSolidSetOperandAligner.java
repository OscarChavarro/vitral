//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidFaceGeometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

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
final class _PolyhedralBoundedSolidSetOperandAligner
    extends _PolyhedralBoundedSolidOperator
{
    /** Width of the band, in tolerances, on each side of a plane. */
    private static final double BAND_IN_TOLERANCES = 2.0;

    /** Distances below this fraction of the tolerance are already zero. */
    private static final double ALREADY_ALIGNED = 1.0e-6;

    private _PolyhedralBoundedSolidSetOperandAligner()
    {
    }

    /**
    Moves `solidB` so that its faces inside the band of a parallel face of
    `solidA` lie on that face's plane.
    @return the displacement applied, or null when nothing was moved
    */
    static Vector3Dd align(PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context =
            PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB);
        double tolerance = context.bigEpsilon();
        double band = BAND_IN_TOLERANCES * tolerance;
        PolyhedralBoundedSolidFaceGeometry[] facesA =
            PolyhedralBoundedSolidFaceGeometry.of(solidA);
        PolyhedralBoundedSolidFaceGeometry[] facesB =
            PolyhedralBoundedSolidFaceGeometry.of(solidB);
        ArrayList<Vector3Dd> basis = new ArrayList<Vector3Dd>();
        Vector3Dd displacement = new Vector3Dd(0, 0, 0);
        boolean found = false;
        int i;
        int j;
        int k;

        for ( i = 0; i < facesA.length; i++ ) {
            InfinitePlane planeA = facesA[i].getPlane();
            Vector3Dd pointA = firstVertex(solidA.getPolygonsList().get(i));
            if ( planeA == null || pointA == null ) {
                continue;
            }
            Vector3Dd normalA = unit(planeA.getNormal());
            for ( j = 0; j < facesB.length; j++ ) {
                InfinitePlane planeB = facesB[j].getPlane();
                Vector3Dd pointB =
                    firstVertex(solidB.getPolygonsList().get(j));
                if ( planeB == null || pointB == null ||
                     facesA[i].boxSeparatedFrom(facesB[j], band) ) {
                    continue;
                }
                Vector3Dd normalB = unit(planeB.getNormal());
                if ( normalA.crossProduct(normalB).length() >
                     context.unitVectorTolerance() ) {
                    continue;
                }
                double distance =
                    pointB.subtract(pointA).dotProduct(normalA);
                if ( Math.abs(distance) > band ||
                     Math.abs(distance) <= ALREADY_ALIGNED * tolerance ) {
                    continue;
                }
                // Wanted: normalA . displacement = -distance. Each new
                // direction is taken perpendicular to the ones already
                // used, so that the earlier pairs stay aligned.
                double residual = -distance -
                    normalA.dotProduct(displacement);
                Vector3Dd free = normalA;
                for ( k = 0; k < basis.size(); k++ ) {
                    free = free.subtract(basis.get(k).multiply(
                        free.dotProduct(basis.get(k))));
                }
                if ( free.length() <= context.unitVectorTolerance() ) {
                    if ( Math.abs(residual) > ALREADY_ALIGNED * tolerance ) {
                        // Two pairs along one direction at different
                        // distances: no translation serves both.
                        return null;
                    }
                    continue;
                }
                displacement = displacement.add(free.multiply(
                    residual / normalA.dotProduct(free)));
                basis.add(unit(free));
                found = true;
            }
        }
        if ( !found ) {
            return null;
        }
        for ( i = 0; i < solidB.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex =
                solidB.getVerticesList().get(i);
            vertex.position = vertex.position.add(displacement);
        }
        return displacement;
    }

    private static Vector3Dd firstVertex(_PolyhedralBoundedSolidFace face)
    {
        if ( face.boundariesList.size() < 1 ||
             face.boundariesList.get(0).boundaryStartHalfEdge == null ) {
            return null;
        }
        return face.boundariesList.get(0).boundaryStartHalfEdge
            .startingVertex.position;
    }

    private static Vector3Dd unit(Vector3Dd vector)
    {
        double length = vector.length();

        return length > 0.0 ? vector.multiply(1.0 / length) : vector;
    }
}

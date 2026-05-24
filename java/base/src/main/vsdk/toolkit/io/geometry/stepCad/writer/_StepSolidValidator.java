package vsdk.toolkit.io.geometry.stepCad.writer;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Pre-export validator. Refuses to export a solid that has not passed
`validateIntermediate`, that does not meet the minimum tetrahedral
topology, or whose signed volume is below the kernel tolerance.

The volume is computed by summing tetrahedra spanned from the
coordinate origin over a fan triangulation of every face outer loop;
its sign reflects global face orientation, only the magnitude is
checked for non-degeneracy.

This is an internal collaborator of `StepWriter`.
*/
public class _StepSolidValidator {

    private _StepSolidValidator()
    {
    }

    public static void validate(PolyhedralBoundedSolid solid)
    {
        if ( !PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid) ) {
            throw new IllegalStateException(
                "Solid did not pass validateIntermediate; refusing to "
                + "export a potentially non-manifold model.");
        }
        if ( solid.getVerticesList().size() < 4 ||
             solid.getEdgesList().size() < 6 ||
             solid.getPolygonsList().size() < 4 ) {
            throw new IllegalStateException(
                "Solid has too few topological entities to form a closed "
                + "volume (need at least a tetrahedron).");
        }
        double volume = computeSignedVolume(solid);
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        if ( Math.abs(volume) <= numericContext.bigEpsilon() ) {
            throw new IllegalStateException(
                "Solid has near-zero volume (|V| = " + Math.abs(volume)
                + "); refusing to export a degenerate model.");
        }
    }

    private static double computeSignedVolume(PolyhedralBoundedSolid solid)
    {
        double volume = 0.0;
        int i;
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            if ( face.boundariesList.size() < 1 ) {
                continue;
            }
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(0);
            if ( loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            Vector3Dd anchor = start.startingVertex.position;
            _PolyhedralBoundedSolidHalfEdge he = start.next();
            while ( he.next() != start ) {
                Vector3Dd b = he.startingVertex.position;
                Vector3Dd c = he.next().startingVertex.position;
                volume += anchor.dotProduct(b.crossProduct(c)) / 6.0;
                he = he.next();
            }
        }
        return volume;
    }
}

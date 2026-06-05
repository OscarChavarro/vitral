package vsdk.toolkit.io.geometry.stl;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

final class _StlSolidValidator
{
    private _StlSolidValidator()
    {
    }

    static void validate(PolyhedralBoundedSolid solid)
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);

        validateNoDegenerateEdges(solid, numericContext);

        StringBuilder msg = new StringBuilder();
        if ( !PolyhedralBoundedSolidGeometricValidator
            .validateAllFacesPlanarityAndPlanes(solid, numericContext, msg) ) {
            throw new IllegalStateException(
                "STL export rejected: non-planar face geometry detected:\n"
                + msg);
        }

        msg.setLength(0);
        if ( !PolyhedralBoundedSolidGeometricValidator
            .validateLoopsStrict(solid, numericContext, msg) ) {
            throw new IllegalStateException(
                "STL export rejected: invalid face loop geometry detected:\n"
                + msg);
        }

        msg.setLength(0);
        if ( !PolyhedralBoundedSolidGeometricValidator
            .validateNoCoincidentVertices(solid, numericContext, msg) ) {
            throw new IllegalStateException(
                "STL export rejected: degenerate vertices detected:\n" + msg);
        }

        validateFacesHaveTriangulableLoops(solid, numericContext);

        if ( !PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid) ) {
            throw new IllegalStateException(
                "STL export rejected: solid failed validateIntermediate; "
                + "export requires a manifold planar intermediate solid");
        }
    }

    private static void validateNoDegenerateEdges(
        PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int i;
        for ( i = 0; i < solid.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(i);
            if ( edge == null || edge.rightHalf == null || edge.leftHalf == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge rightHe = edge.rightHalf;
            _PolyhedralBoundedSolidHalfEdge leftHe = edge.leftHalf;
            double length = Vector3Dd.distance(
                rightHe.startingVertex.position, leftHe.startingVertex.position);
            if ( length <= numericContext.bigEpsilon() ) {
                throw new IllegalStateException(
                    "STL export rejected: edge " + edge.id
                    + " is degenerate between vertices "
                    + rightHe.startingVertex.id + " and "
                    + leftHe.startingVertex.id + " (length " + length + ")");
            }
        }
    }

    private static void validateFacesHaveTriangulableLoops(
        PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int i;
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            validateFaceLoops(face, numericContext);
        }
    }

    private static void validateFaceLoops(
        _PolyhedralBoundedSolidFace face,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        if ( face.getContainingPlane() == null ) {
            throw new IllegalStateException(
                "STL export rejected: face " + face.id + " has no containing plane");
        }
        int i;
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i
                    + " has no traversable boundary");
            }
            ArrayList<Vector3Dd> points = collectLoopPoints(face, loop, i);
            if ( points.size() < 3 ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i
                    + " has fewer than 3 vertices");
            }
            double areaTolerance = PolyhedralBoundedSolidNumericPolicy
                .areaTolerance2D(PolyhedralBoundedSolidNumericPolicy.forFace(face));
            double projectedArea = projectedLoopAreaMagnitude(points, face);
            if ( projectedArea <= areaTolerance ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i
                    + " has near-zero area"
                    + buildLoopDiagnostics(face, loop, i, projectedArea,
                        areaTolerance));
            }
        }
    }

    private static ArrayList<Vector3Dd> collectLoopPoints(
        _PolyhedralBoundedSolidFace face,
        _PolyhedralBoundedSolidLoop loop,
        int loopIndex)
    {
        ArrayList<Vector3Dd> points = new ArrayList<>();
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge current = start;
        do {
            if ( current == null || current.startingVertex == null ||
                 current.startingVertex.position == null ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + loopIndex
                    + " is not traversable");
            }
            points.add(current.startingVertex.position);
            current = current.next();
            if ( current == null ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + loopIndex
                    + " is not closed");
            }
        } while ( current != start );
        return points;
    }

    private static double projectedLoopAreaMagnitude(ArrayList<Vector3Dd> points,
                                                     _PolyhedralBoundedSolidFace face)
    {
        Vector3Dd anchor = points.get(0);
        Vector3Dd normal = face.getContainingPlane().getNormal().normalized();
        Vector3Dd axis = chooseReferenceAxis(normal);
        Vector3Dd u = axis.crossProduct(normal).normalized();
        Vector3Dd v = normal.crossProduct(u).normalized();

        double areaTwice = 0.0;
        int i;
        for ( i = 0; i < points.size(); i++ ) {
            Vector3Dd current = points.get(i).subtract(anchor);
            Vector3Dd next = points.get((i + 1) % points.size()).subtract(anchor);
            double x0 = current.dotProduct(u);
            double y0 = current.dotProduct(v);
            double x1 = next.dotProduct(u);
            double y1 = next.dotProduct(v);
            areaTwice += x0 * y1 - x1 * y0;
        }
        return Math.abs(areaTwice * 0.5);
    }

    private static Vector3Dd chooseReferenceAxis(Vector3Dd normal)
    {
        double ax = Math.abs(normal.x());
        double ay = Math.abs(normal.y());
        double az = Math.abs(normal.z());
        if ( ax <= ay && ax <= az ) {
            return new Vector3Dd(1.0, 0.0, 0.0);
        }
        if ( ay <= az ) {
            return new Vector3Dd(0.0, 1.0, 0.0);
        }
        return new Vector3Dd(0.0, 0.0, 1.0);
    }

    private static String buildLoopDiagnostics(
        _PolyhedralBoundedSolidFace face,
        _PolyhedralBoundedSolidLoop loop,
        int loopIndex,
        double projectedArea,
        double areaTolerance)
    {
        StringBuilder msg = new StringBuilder();
        msg.append("\n  projectedArea=").append(projectedArea);
        msg.append(" areaTolerance=").append(areaTolerance);
        msg.append(" faceScale=")
            .append(PolyhedralBoundedSolidNumericPolicy.forFace(face).modelScale());
        msg.append(" solidScale=")
            .append(PolyhedralBoundedSolidNumericPolicy.forSolid(face.parentSolid)
                .modelScale());
        if ( face.getContainingPlane() != null ) {
            Vector3Dd normal = face.getContainingPlane().getNormal();
            msg.append(" normal=(")
                .append(normal.x()).append(", ")
                .append(normal.y()).append(", ")
                .append(normal.z()).append(")");
        }
        msg.append("\n  loop ").append(loopIndex).append(" vertices:");

        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge current = start;
        do {
            msg.append("\n    v").append(current.startingVertex.id).append("=(")
                .append(current.startingVertex.position.x()).append(", ")
                .append(current.startingVertex.position.y()).append(", ")
                .append(current.startingVertex.position.z()).append(")");
            current = current.next();
        } while ( current != null && current != start );
        return msg.toString();
    }
}

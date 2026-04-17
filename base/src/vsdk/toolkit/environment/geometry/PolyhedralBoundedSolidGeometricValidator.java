package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector2D;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

public class PolyhedralBoundedSolidGeometricValidator
{
    private PolyhedralBoundedSolidGeometricValidator()
    {
    }

    public static boolean validateFacePointsAreCoplanar(ArrayList<Vector3D> points)
    {
        if ( points.size() < 3 ) {
            return false;
        }

        Vector3D p0, p1, p2;
        p0 = points.get(0);
        boolean test = false;

        int i;
        for ( i = 1; i < points.size(); i++ ) {
            p1 = points.get(i);
            if ( VSDK.vectorDistance(p0, p1) > 10 * VSDK.EPSILON ) {
                test = true;
                break;
            }
        }
        if ( !test ) {
            return false;
        }

        Vector3D a, b, n;
        double aDotB;
        InfinitePlane facePlane = null;
        int j, k;

        for ( i = 0; i < points.size(); i++ ) {
            for ( j = 0; j < points.size(); j++ ) {
                for ( k = 0; k < points.size(); k++ ) {
                    if ( i == j || i == k || j == k ) {
                        continue;
                    }
                    p0 = points.get(i);
                    p1 = points.get(j);
                    p2 = points.get(k);
                    if ( VSDK.vectorDistance(p0, p2) > 10 * VSDK.EPSILON &&
                         VSDK.vectorDistance(p1, p2) > 10 * VSDK.EPSILON ) {
                        a = p2.substract(p0);
                        b = p1.substract(p0);
                        a.normalize();
                        b.normalize();
                        aDotB = Math.abs(a.dotProduct(b));
                        if ( aDotB < 1 - 2*VSDK.EPSILON ) {
                            n = a.crossProduct(b);
                            n.normalize();
                            facePlane = new InfinitePlane(n, p0);
                        }
                        break;
                    }
                }
            }
        }

        if ( facePlane == null ) {
            return false;
        }

        for ( i = 1; i < points.size(); i++ ) {
            p0 = points.get(i);
            if ( facePlane.doContainmentTest(p0, VSDK.EPSILON) != Geometry.LIMIT ) {
                return false;
            }
        }

        return true;
    }

    public static ArrayList<Vector3D> extractPointsFromFace(_PolyhedralBoundedSolidFace face)
    {
        boolean test = true;
        ArrayList<Vector3D> points;
        points = new ArrayList<Vector3D>();
        int j;

        for ( j = 0; j < face.boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge he, heStart;

            loop = face.boundariesList.get(j);
            he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                test = false;
                break;
            }
            heStart = he;
            do {
                he = he.next();
                if ( he == null ) {
                    test = false;
                    break;
                }
                points.add(he.startingVertex.position);
            } while( he != heStart );
        }

        if ( !test ) {
            return null;
        }
        return points;
    }

    public static boolean validateFaceIsPlanar(_PolyhedralBoundedSolidFace face)
    {
        ArrayList<Vector3D> points = extractPointsFromFace(face);
        return (points != null) && validateFacePointsAreCoplanar(points);
    }

    public static boolean validateAllFacesPlanarityAndPlanes(
        PolyhedralBoundedSolid solid, StringBuilder msg)
    {
        int i;
        boolean test = true;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( validateFaceIsPlanar(face) ) {
                face.calculatePlane();
                if ( face.containingPlane == null ) {
                    msg.append("  - Face [").append(face.id)
                       .append("] was not able to compute containing plane\n");
                    test = false;
                }
            }
            else {
                msg.append("  - Face [").append(face.id).append("] is not coplanar\n");
                test = false;
            }
        }
        return test;
    }

    private static int dominantCoordinateForFace(_PolyhedralBoundedSolidFace face)
    {
        Vector3D n = face.containingPlane.getNormal();
        if ( Math.abs(n.x) >= Math.abs(n.y) && Math.abs(n.x) >= Math.abs(n.z) ) {
            return 1;
        }
        if ( Math.abs(n.y) >= Math.abs(n.x) && Math.abs(n.y) >= Math.abs(n.z) ) {
            return 2;
        }
        return 3;
    }

    private static Vector2D projectPointTo2D(Vector3D in, int dominantCoordinate)
    {
        Vector2D out = new Vector2D();
        if ( dominantCoordinate == 1 ) {
            out.x = in.y;
            out.y = in.z;
        }
        else if ( dominantCoordinate == 2 ) {
            out.x = in.x;
            out.y = in.z;
        }
        else {
            out.x = in.x;
            out.y = in.y;
        }
        return out;
    }

    private static double orientation2D(Vector2D a, Vector2D b, Vector2D c)
    {
        return (b.x-a.x)*(c.y-a.y) - (b.y-a.y)*(c.x-a.x);
    }

    private static boolean pointOnSegment2D(Vector2D p, Vector2D a, Vector2D b, double tolerance)
    {
        if ( Math.abs(orientation2D(a, b, p)) > tolerance ) {
            return false;
        }
        double minX = Math.min(a.x, b.x) - tolerance;
        double maxX = Math.max(a.x, b.x) + tolerance;
        double minY = Math.min(a.y, b.y) - tolerance;
        double maxY = Math.max(a.y, b.y) + tolerance;
        return p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY;
    }

    private static boolean segmentsIntersect2D(Vector2D a1, Vector2D a2,
                                               Vector2D b1, Vector2D b2,
                                               double tolerance)
    {
        double o1 = orientation2D(a1, a2, b1);
        double o2 = orientation2D(a1, a2, b2);
        double o3 = orientation2D(b1, b2, a1);
        double o4 = orientation2D(b1, b2, a2);

        boolean proper = (o1 > tolerance && o2 < -tolerance ||
                          o1 < -tolerance && o2 > tolerance) &&
                         (o3 > tolerance && o4 < -tolerance ||
                          o3 < -tolerance && o4 > tolerance);
        if ( proper ) {
            return true;
        }

        return pointOnSegment2D(b1, a1, a2, tolerance) ||
               pointOnSegment2D(b2, a1, a2, tolerance) ||
               pointOnSegment2D(a1, b1, b2, tolerance) ||
               pointOnSegment2D(a2, b1, b2, tolerance);
    }

    private static boolean loopHasSelfIntersection(_PolyhedralBoundedSolidFace face,
                                                   _PolyhedralBoundedSolidLoop loop,
                                                   StringBuilder msg)
    {
        int n = loop.halfEdgesList.size();
        if ( n < 3 ) {
            msg.append("  - Face [").append(face.id)
               .append("] has a loop with fewer than 3 edges.\n");
            return true;
        }

        int dominantCoordinate = dominantCoordinateForFace(face);
        int i, j;
        for ( i = 0; i < n; i++ ) {
            _PolyhedralBoundedSolidHalfEdge heA = loop.halfEdgesList.get(i);
            _PolyhedralBoundedSolidHalfEdge heANext = heA.next();
            if ( heANext == null ) {
                msg.append("  - Face [").append(face.id)
                   .append("] has a non-closed loop during strict validation.\n");
                return true;
            }
            Vector2D a1 = projectPointTo2D(heA.startingVertex.position, dominantCoordinate);
            Vector2D a2 = projectPointTo2D(heANext.startingVertex.position, dominantCoordinate);

            for ( j = i+1; j < n; j++ ) {
                if ( j == (i+1)%n || i == (j+1)%n ) {
                    continue;
                }
                _PolyhedralBoundedSolidHalfEdge heB = loop.halfEdgesList.get(j);
                _PolyhedralBoundedSolidHalfEdge heBNext = heB.next();
                if ( heBNext == null ) {
                    msg.append("  - Face [").append(face.id)
                       .append("] has a non-closed loop during strict validation.\n");
                    return true;
                }
                Vector2D b1 = projectPointTo2D(heB.startingVertex.position, dominantCoordinate);
                Vector2D b2 = projectPointTo2D(heBNext.startingVertex.position, dominantCoordinate);

                if ( segmentsIntersect2D(a1, a2, b1, b2, 10*VSDK.EPSILON) ) {
                    msg.append("  - Face [").append(face.id)
                       .append("] has a self-intersecting loop.\n");
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean loopsIntersect(_PolyhedralBoundedSolidFace face,
                                          _PolyhedralBoundedSolidLoop loopA,
                                          _PolyhedralBoundedSolidLoop loopB,
                                          StringBuilder msg)
    {
        int dominantCoordinate = dominantCoordinateForFace(face);
        int i, j;
        for ( i = 0; i < loopA.halfEdgesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge heA = loopA.halfEdgesList.get(i);
            _PolyhedralBoundedSolidHalfEdge heANext = heA.next();
            if ( heANext == null ) {
                msg.append("  - Face [").append(face.id)
                   .append("] has a non-closed loop during strict validation.\n");
                return true;
            }
            Vector2D a1 = projectPointTo2D(heA.startingVertex.position, dominantCoordinate);
            Vector2D a2 = projectPointTo2D(heANext.startingVertex.position, dominantCoordinate);

            for ( j = 0; j < loopB.halfEdgesList.size(); j++ ) {
                _PolyhedralBoundedSolidHalfEdge heB = loopB.halfEdgesList.get(j);
                _PolyhedralBoundedSolidHalfEdge heBNext = heB.next();
                if ( heBNext == null ) {
                    msg.append("  - Face [").append(face.id)
                       .append("] has a non-closed loop during strict validation.\n");
                    return true;
                }
                Vector2D b1 = projectPointTo2D(heB.startingVertex.position, dominantCoordinate);
                Vector2D b2 = projectPointTo2D(heBNext.startingVertex.position, dominantCoordinate);
                if ( segmentsIntersect2D(a1, a2, b1, b2, 10*VSDK.EPSILON) ) {
                    msg.append("  - Face [").append(face.id)
                       .append("] has intersecting loops.\n");
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean validateLoopsStrict(PolyhedralBoundedSolid solid, StringBuilder msg)
    {
        int i, j, k;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( face.containingPlane == null ) {
                msg.append("  - Face [").append(face.id)
                   .append("] has no containing plane for strict checks.\n");
                return false;
            }

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                if ( loopHasSelfIntersection(face, loop, msg) ) {
                    return false;
                }
            }
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                for ( k = j+1; k < face.boundariesList.size(); k++ ) {
                    if ( loopsIntersect(face, face.boundariesList.get(j),
                                        face.boundariesList.get(k), msg) ) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean facesAreCoplanar(_PolyhedralBoundedSolidFace faceA,
                                            _PolyhedralBoundedSolidFace faceB)
    {
        if ( faceA.containingPlane == null || faceB.containingPlane == null ) {
            return false;
        }

        Vector3D nA = faceA.containingPlane.getNormal().multiply(1.0);
        Vector3D nB = faceB.containingPlane.getNormal().multiply(1.0);
        nA.normalize();
        nB.normalize();
        if ( Math.abs(Math.abs(nA.dotProduct(nB)) - 1.0) > 100*VSDK.EPSILON ) {
            return false;
        }

        for ( int i = 0; i < faceA.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = faceA.boundariesList.get(i);
            if ( loop.halfEdgesList.size() > 0 ) {
                Vector3D p = loop.halfEdgesList.get(0).startingVertex.position;
                return Math.abs(faceB.containingPlane.pointDistance(p)) <= 10*VSDK.EPSILON;
            }
        }
        return false;
    }

    private static boolean segmentSharesEndpoint(_PolyhedralBoundedSolidHalfEdge a,
                                                 _PolyhedralBoundedSolidHalfEdge b)
    {
        _PolyhedralBoundedSolidHalfEdge an = a.next();
        _PolyhedralBoundedSolidHalfEdge bn = b.next();
        if ( an == null || bn == null ) {
            return false;
        }
        _PolyhedralBoundedSolidVertex a0 = a.startingVertex;
        _PolyhedralBoundedSolidVertex a1 = an.startingVertex;
        _PolyhedralBoundedSolidVertex b0 = b.startingVertex;
        _PolyhedralBoundedSolidVertex b1 = bn.startingVertex;
        return a0 == b0 || a0 == b1 || a1 == b0 || a1 == b1;
    }

    private static boolean vertexStrictlyInsideFace(_PolyhedralBoundedSolidVertex v,
                                                    _PolyhedralBoundedSolidFace face)
    {
        if ( face.containingPlane.doContainmentTest(v.position, 10*VSDK.EPSILON) != Geometry.LIMIT ) {
            return false;
        }
        return face.testPointInside(v.position, 10*VSDK.EPSILON) == Geometry.INSIDE;
    }

    private static boolean edgePiercesFaceInterior(_PolyhedralBoundedSolidHalfEdge he,
                                                   _PolyhedralBoundedSolidFace face)
    {
        _PolyhedralBoundedSolidHalfEdge next = he.next();
        if ( next == null || face.containingPlane == null ) {
            return false;
        }

        Vector3D p0 = he.startingVertex.position;
        Vector3D p1 = next.startingVertex.position;
        double d0 = face.containingPlane.pointDistance(p0);
        double d1 = face.containingPlane.pointDistance(p1);

        if ( Math.abs(d0) <= 10*VSDK.EPSILON && Math.abs(d1) <= 10*VSDK.EPSILON ) {
            return false;
        }
        if ( d0*d1 > 0 ) {
            return false;
        }

        double denom = d0 - d1;
        if ( Math.abs(denom) <= VSDK.EPSILON ) {
            return false;
        }
        double t = d0 / denom;
        if ( t <= 10*VSDK.EPSILON || t >= 1.0 - 10*VSDK.EPSILON ) {
            return false;
        }

        Vector3D p = p0.add(p1.substract(p0).multiply(t));
        return face.testPointInside(p, 10*VSDK.EPSILON) == Geometry.INSIDE;
    }

    private static boolean facesHaveImproperIntersection(_PolyhedralBoundedSolidFace faceA,
                                                         _PolyhedralBoundedSolidFace faceB,
                                                         StringBuilder msg)
    {
        int i, j, k;
        _PolyhedralBoundedSolidHalfEdge he;

        for ( i = 0; i < faceA.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = faceA.boundariesList.get(i);
            for ( j = 0; j < loop.halfEdgesList.size(); j++ ) {
                he = loop.halfEdgesList.get(j);
                if ( vertexStrictlyInsideFace(he.startingVertex, faceB) ) {
                    msg.append("  - Faces [").append(faceA.id).append("] and [")
                       .append(faceB.id).append("] intersect improperly.\n");
                    return true;
                }
                if ( edgePiercesFaceInterior(he, faceB) ) {
                    msg.append("  - Faces [").append(faceA.id).append("] and [")
                       .append(faceB.id).append("] intersect improperly.\n");
                    return true;
                }
            }
        }

        for ( i = 0; i < faceB.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = faceB.boundariesList.get(i);
            for ( j = 0; j < loop.halfEdgesList.size(); j++ ) {
                he = loop.halfEdgesList.get(j);
                if ( vertexStrictlyInsideFace(he.startingVertex, faceA) ) {
                    msg.append("  - Faces [").append(faceA.id).append("] and [")
                       .append(faceB.id).append("] intersect improperly.\n");
                    return true;
                }
                if ( edgePiercesFaceInterior(he, faceA) ) {
                    msg.append("  - Faces [").append(faceA.id).append("] and [")
                       .append(faceB.id).append("] intersect improperly.\n");
                    return true;
                }
            }
        }

        if ( facesAreCoplanar(faceA, faceB) ) {
            int dominantCoordinate = dominantCoordinateForFace(faceA);
            for ( i = 0; i < faceA.boundariesList.size(); i++ ) {
                _PolyhedralBoundedSolidLoop loopA = faceA.boundariesList.get(i);
                for ( j = 0; j < loopA.halfEdgesList.size(); j++ ) {
                    _PolyhedralBoundedSolidHalfEdge heA = loopA.halfEdgesList.get(j);
                    _PolyhedralBoundedSolidHalfEdge heANext = heA.next();
                    if ( heANext == null ) {
                        continue;
                    }
                    Vector2D a1 = projectPointTo2D(heA.startingVertex.position, dominantCoordinate);
                    Vector2D a2 = projectPointTo2D(heANext.startingVertex.position, dominantCoordinate);

                    for ( k = 0; k < faceB.boundariesList.size(); k++ ) {
                        _PolyhedralBoundedSolidLoop loopB = faceB.boundariesList.get(k);
                        for ( int m = 0; m < loopB.halfEdgesList.size(); m++ ) {
                            _PolyhedralBoundedSolidHalfEdge heB = loopB.halfEdgesList.get(m);
                            _PolyhedralBoundedSolidHalfEdge heBNext = heB.next();
                            if ( heBNext == null ) {
                                continue;
                            }
                            if ( heA.parentEdge == heB.parentEdge ) {
                                continue;
                            }
                            Vector2D b1 = projectPointTo2D(heB.startingVertex.position, dominantCoordinate);
                            Vector2D b2 = projectPointTo2D(heBNext.startingVertex.position, dominantCoordinate);
                            if ( segmentsIntersect2D(a1, a2, b1, b2, 10*VSDK.EPSILON) &&
                                 !segmentSharesEndpoint(heA, heB) ) {
                                msg.append("  - Faces [").append(faceA.id).append("] and [")
                                   .append(faceB.id).append("] intersect improperly.\n");
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public static boolean validateFaceIntersectionsStrict(
        PolyhedralBoundedSolid solid, StringBuilder msg)
    {
        int i, j;
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace faceA = solid.polygonsList.get(i);
            for ( j = i+1; j < solid.polygonsList.size(); j++ ) {
                _PolyhedralBoundedSolidFace faceB = solid.polygonsList.get(j);
                if ( facesHaveImproperIntersection(faceA, faceB, msg) ) {
                    return false;
                }
            }
        }
        return true;
    }
}

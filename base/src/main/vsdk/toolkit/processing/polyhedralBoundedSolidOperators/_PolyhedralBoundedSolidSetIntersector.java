//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Helper for set operation big phase 0: generation of vertex/face and
vertex/vertex intersections, following the initial detection phase from
program [MANT1988].15.2.
*/
final class _PolyhedralBoundedSolidSetIntersector
    extends _PolyhedralBoundedSolidOperator
{
    private static final class BoundaryHit
    {
        private final _PolyhedralBoundedSolidHalfEdge halfEdge;
        private final _PolyhedralBoundedSolidVertex vertex;
        private final Vector3D point;

        private BoundaryHit(_PolyhedralBoundedSolidHalfEdge halfEdge,
                            _PolyhedralBoundedSolidVertex vertex,
                            Vector3D point)
        {
            this.halfEdge = halfEdge;
            this.vertex = vertex;
            this.point = point;
        }
    }

    static final class GenerationResult
    {
        private final ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv;
        private final ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva;
        private final ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb;

        private GenerationResult(
            ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv,
            ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva,
            ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb)
        {
            this.sonvv = sonvv;
            this.sonva = sonva;
            this.sonvb = sonvb;
        }

        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv()
        {
            return sonvv;
        }

        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva()
        {
            return sonva;
        }

        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb()
        {
            return sonvb;
        }
    }

    private static int compareToZero(double value)
    {
        return PolyhedralBoundedSolidNumericPolicy.compareToZero(value,
            numericContext);
    }

    private static _PolyhedralBoundedSolidFace.PointInsideResult
    pointInFaceDetailed(_PolyhedralBoundedSolidFace face, Vector3D point)
    {
        return face.testPointInsideDetailed(point, numericContext.bigEpsilon());
    }

    private static BoundaryHit findNearbyBoundaryHit(
        _PolyhedralBoundedSolidFace face,
        Vector3D point)
    {
        double tolerance;
        int i;

        if ( face == null || point == null ||
             face.boundariesList == null ) {
            return null;
        }

        tolerance = Math.max(numericContext.bigEpsilon() * 10.0, 1.0e-7);
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge start;
            _PolyhedralBoundedSolidHalfEdge current;
            int guard;

            if ( face.boundariesList.get(i) == null ||
                 face.boundariesList.get(i).boundaryStartHalfEdge == null ) {
                continue;
            }
            start = face.boundariesList.get(i).boundaryStartHalfEdge;
            current = start;
            guard = 0;
            do {
                BoundaryHit hit = findNearbyBoundaryHit(current, point,
                    tolerance);

                if ( hit != null ) {
                    return hit;
                }
                current = current.next();
                guard++;
            } while ( current != start &&
                      guard <= face.boundariesList.get(i).halfEdgesList.size() + 1 );
        }
        return null;
    }

    private static BoundaryHit findNearbyBoundaryHit(
        _PolyhedralBoundedSolidHalfEdge halfEdge,
        Vector3D point,
        double tolerance)
    {
        Vector3D a;
        Vector3D b;
        Vector3D ab;
        Vector3D closest;
        double lengthSquared;
        double t;

        if ( halfEdge == null ||
             halfEdge.startingVertex == null ||
             halfEdge.next() == null ||
             halfEdge.next().startingVertex == null ) {
            return null;
        }

        a = halfEdge.startingVertex.position;
        b = halfEdge.next().startingVertex.position;
        if ( a == null || b == null ) {
            return null;
        }
        if ( point.subtract(a).length() <= tolerance ) {
            return new BoundaryHit(null, halfEdge.startingVertex, a);
        }
        if ( point.subtract(b).length() <= tolerance ) {
            return new BoundaryHit(null, halfEdge.next().startingVertex, b);
        }

        ab = b.subtract(a);
        lengthSquared = ab.dotProduct(ab);
        if ( lengthSquared <= tolerance * tolerance ) {
            return null;
        }
        t = point.subtract(a).dotProduct(ab) / lengthSquared;
        if ( t <= 0.0 || t >= 1.0 ) {
            return null;
        }
        closest = a.add(ab.multiply(t));
        if ( point.subtract(closest).length() > tolerance ) {
            return null;
        }
        return new BoundaryHit(halfEdge, null, closest);
    }

    private static int nextVertexId(PolyhedralBoundedSolid current,
                                    PolyhedralBoundedSolid other)
    {
        int currentMax;
        int otherMax;

        currentMax = current.getMaxVertexId();
        otherMax = other.getMaxVertexId();
        if ( otherMax > currentMax ) {
            currentMax = otherMax;
        }
        return currentMax + 1;
    }

    /**
    Inserts a vertex/face coincidence into the set corresponding to the
    `sonva`/`sonvb` variables of program [MANT1988].15.1.
    */
    private static void addsovf(_PolyhedralBoundedSolidHalfEdge he,
                                _PolyhedralBoundedSolidFace f, int BvsA,
                                ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva,
                                ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb)
    {
        _PolyhedralBoundedSolidSetOperatorVertexFace elem;
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonv;
        int i;

        if ( BvsA == 0 ) {
            sonv = sonva;
        }
        else {
            sonv = sonvb;
        }

        for ( i = 0; i < sonv.size(); i++ ) {
            elem = sonv.get(i);
            if ( elem.v == he.startingVertex && elem.f == f ) {
                return;
            }
        }

        elem = new _PolyhedralBoundedSolidSetOperatorVertexFace();
        elem.v = he.startingVertex;
        elem.f = f;
        sonv.add(elem);
    }

    /**
    Inserts a vertex/vertex coincidence into the set corresponding to the
    `sonvv` variable of program [MANT1988].15.1.
    */
    private static void addsovv(_PolyhedralBoundedSolidVertex a,
                                _PolyhedralBoundedSolidVertex b, int BvsA,
                                ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv)
    {
        _PolyhedralBoundedSolidSetOperatorVertexVertex elem;
        int i;

        for ( i = 0; i < sonvv.size(); i++ ) {
            elem = sonvv.get(i);
            if ( BvsA == 0 && elem.va == a && elem.vb == b ||
                 BvsA != 0 && elem.va == b && elem.vb == a ) {
                return;
            }
        }

        elem = new _PolyhedralBoundedSolidSetOperatorVertexVertex();
        if ( BvsA == 0 ) {
            elem.va = a;
            elem.vb = b;
        }
        else {
            elem.va = b;
            elem.vb = a;
        }
        sonvv.add(elem);
    }

    /**
    Handles the degenerate branch of the edge/face test from section
    [MANT1988].15.3 when one endpoint already lies on the reference face, using
    the point-on-edge and point-on-vertex bookkeeping suggested by problem
    [MANT1988].13.3.
    */
    private static void doVertexOnFace(
        _PolyhedralBoundedSolidVertex v,
        _PolyhedralBoundedSolidFace f,
        int BvsA,
        PolyhedralBoundedSolid edgeSolid,
        PolyhedralBoundedSolid faceSolid,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb)
    {
        int cont;
        double d;
        _PolyhedralBoundedSolidFace.PointInsideResult containment;
        _PolyhedralBoundedSolidHalfEdge intersectedHalfedge;
        _PolyhedralBoundedSolidVertex intersectedVertex;
        BoundaryHit nearbyBoundaryHit;

        d = f.getContainingPlane().pointDistance(v.position);
        if ( compareToZero(d) == 0 ) {
            containment = pointInFaceDetailed(f, v.position);
            cont = containment.status();
            intersectedHalfedge = containment.intersectedHalfedge();
            intersectedVertex = containment.intersectedVertex();
            if ( cont == Geometry.INSIDE ) {
                nearbyBoundaryHit = findNearbyBoundaryHit(f, v.position);
                if ( nearbyBoundaryHit != null ) {
                    cont = Geometry.LIMIT;
                    intersectedHalfedge = nearbyBoundaryHit.halfEdge;
                    intersectedVertex = nearbyBoundaryHit.vertex;
                }
            }
            if ( cont == Geometry.INSIDE ) {
                addsovf(v.emanatingHalfEdge, f, BvsA, sonva, sonvb);
            }
            else if ( cont == Geometry.LIMIT &&
                      intersectedHalfedge != null ) {
                PolyhedralBoundedSolidEulerOperators.lmev(faceSolid, 
                    intersectedHalfedge,
                    intersectedHalfedge.mirrorHalfEdge().next(),
                    nextVertexId(edgeSolid, faceSolid), v.position);
                addsovv(v, intersectedHalfedge.startingVertex, BvsA,
                    sonvv);
            }
            else if ( cont == Geometry.LIMIT &&
                      intersectedVertex != null ) {
                addsovv(v, intersectedVertex, BvsA, sonvv);
            }
        }
    }

    /**
    Performs one edge/face intersection test for the big-phase-0 generator.
    This is the edge/face crossing analysis required by section [MANT1988].15.3
    as part of the initial detector of program [MANT1988].15.2.
    */
    private static _PolyhedralBoundedSolidEdge doSetOpGenerate(
        _PolyhedralBoundedSolidEdge e,
        _PolyhedralBoundedSolidFace f,
        int BvsA,
        PolyhedralBoundedSolid edgeSolid,
        PolyhedralBoundedSolid faceSolid,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb)
    {
        _PolyhedralBoundedSolidVertex v1;
        _PolyhedralBoundedSolidVertex v2;
        double d1;
        double d2;
        double d3;
        double t;
        Vector3D p;
        int s1;
        int s2;
        int cont;
        _PolyhedralBoundedSolidFace.PointInsideResult containment;
        _PolyhedralBoundedSolidHalfEdge intersectedHalfedge;
        _PolyhedralBoundedSolidVertex intersectedVertex;
        BoundaryHit nearbyBoundaryHit;

        v1 = e.rightHalf.startingVertex;
        v2 = e.leftHalf.startingVertex;
        d1 = f.getContainingPlane().pointDistance(v1.position);
        d2 = f.getContainingPlane().pointDistance(v2.position);
        s1 = compareToZero(d1);
        s2 = compareToZero(d2);

        if ( (s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1) ) {
            t = d1 / (d1 - d2);
            p = v1.position.add(
                    (v2.position.subtract(
                         v1.position)).multiply(t));

            d3 = f.getContainingPlane().pointDistance(p);
            if ( compareToZero(d3) == 0 ) {
                containment = pointInFaceDetailed(f, p);
                cont = containment.status();
                nearbyBoundaryHit = null;
                if ( cont == Geometry.INSIDE ) {
                    nearbyBoundaryHit = findNearbyBoundaryHit(f, p);
                    if ( nearbyBoundaryHit != null ) {
                        cont = Geometry.LIMIT;
                        p = nearbyBoundaryHit.point;
                    }
                }

                if ( cont != Geometry.OUTSIDE ) {
                    PolyhedralBoundedSolidEulerOperators.lmev(edgeSolid,
                        e.rightHalf, e.leftHalf.next(),
                        nextVertexId(edgeSolid, faceSolid), p);

                    if ( cont == Geometry.INSIDE ) {
                        addsovf(e.rightHalf, f, BvsA, sonva, sonvb);
                    }
                    else if ( cont == Geometry.LIMIT &&
                              (nearbyBoundaryHit != null &&
                               nearbyBoundaryHit.halfEdge != null ||
                               containment.intersectedHalfedge() != null) ) {
                        intersectedHalfedge = nearbyBoundaryHit != null &&
                            nearbyBoundaryHit.halfEdge != null ?
                            nearbyBoundaryHit.halfEdge :
                            containment.intersectedHalfedge();
                        PolyhedralBoundedSolidEulerOperators.lmev(faceSolid, intersectedHalfedge,
                            intersectedHalfedge.mirrorHalfEdge().next(),
                                     nextVertexId(edgeSolid, faceSolid), p);
                        addsovv(e.rightHalf.startingVertex,
                            intersectedHalfedge.startingVertex, BvsA,
                            sonvv);
                    }
                    else if ( cont == Geometry.LIMIT &&
                              (nearbyBoundaryHit != null &&
                               nearbyBoundaryHit.vertex != null ||
                               containment.intersectedVertex() != null) ) {
                        intersectedVertex = nearbyBoundaryHit != null &&
                            nearbyBoundaryHit.vertex != null ?
                            nearbyBoundaryHit.vertex :
                            containment.intersectedVertex();
                        addsovv(e.rightHalf.startingVertex,
                            intersectedVertex, BvsA, sonvv);
                    }
                    return e.rightHalf.previous().parentEdge;
                }
            }
        }
        else {
            if ( s1 == 0 ) {
                doVertexOnFace(v1, f, BvsA, edgeSolid, faceSolid, sonvv, sonva,
                    sonvb);
            }
            if ( s2 == 0 ) {
                doVertexOnFace(v2, f, BvsA, edgeSolid, faceSolid, sonvv, sonva,
                    sonvb);
            }
        }

        return null;
    }

    private static void processEdge(_PolyhedralBoundedSolidEdge e,
                                    PolyhedralBoundedSolid edgeSolid,
                                    PolyhedralBoundedSolid faceSolid,
                                    int BvsA,
                                    ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv,
                                    ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva,
                                    ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb)
    {
        _PolyhedralBoundedSolidFace f;
        _PolyhedralBoundedSolidEdge generatedEdge;
        int i;

        for ( i = 0; i < faceSolid.getPolygonsList().size(); i++ ) {
            f = faceSolid.getPolygonsList().get(i);
            generatedEdge = doSetOpGenerate(e, f, BvsA, edgeSolid,
                faceSolid, sonvv, sonva, sonvb);
            if ( generatedEdge != null ) {
                processEdge(generatedEdge, edgeSolid, faceSolid, BvsA,
                    sonvv, sonva, sonvb);
            }
        }
    }

    /**
    Initial vertex intersection detector for the set operations algorithm
    (big phase 0).
    Following program [MANT1988].15.2.
    */
    static GenerationResult setOpGenerate(PolyhedralBoundedSolid inSolidA,
                                          PolyhedralBoundedSolid inSolidB)
    {
        _PolyhedralBoundedSolidEdge e;
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv;
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva;
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb;
        int i;

        sonvv = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>();
        sonva = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>();
        sonvb = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>();

        for ( i = 0; i < inSolidA.getEdgesList().size(); i++ ) {
            e = inSolidA.getEdgesList().get(i);
            processEdge(e, inSolidA, inSolidB, 0, sonvv, sonva, sonvb);
        }
        for ( i = 0; i < inSolidB.getEdgesList().size(); i++ ) {
            e = inSolidB.getEdgesList().get(i);
            processEdge(e, inSolidB, inSolidA, 1, sonvv, sonva, sonvb);
        }

        return new GenerationResult(sonvv, sonva, sonvb);
    }
}

//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayDeque;
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

    private static int pointInFace(_PolyhedralBoundedSolidFace face, Vector3D point)
    {
        return face.testPointInside(point, numericContext.bigEpsilon());
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
        PolyhedralBoundedSolid current,
        PolyhedralBoundedSolid other,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb)
    {
        int cont;
        double d;

        d = f.containingPlane.pointDistance(v.position);
        if ( compareToZero(d) == 0 ) {
            cont = pointInFace(f, v.position);
            if ( cont == Geometry.INSIDE ) {
                addsovf(v.emanatingHalfEdge, f, BvsA, sonva, sonvb);
            }
            else if ( cont == Geometry.LIMIT &&
                      f.lastIntersectedHalfedge != null ) {
                current.lmev(
                    f.lastIntersectedHalfedge,
                    f.lastIntersectedHalfedge.mirrorHalfEdge().next(),
                    nextVertexId(current, other), v.position);
                addsovv(v, f.lastIntersectedHalfedge.startingVertex, BvsA,
                    sonvv);
            }
            else if ( cont == Geometry.LIMIT &&
                      f.lastIntersectedVertex != null ) {
                addsovv(v, f.lastIntersectedVertex, BvsA, sonvv);
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
        PolyhedralBoundedSolid current,
        PolyhedralBoundedSolid other,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva,
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb)
    {
        _PolyhedralBoundedSolidVertex v1, v2;
        double d1, d2, d3, t;
        Vector3D p;
        int s1, s2, cont;

        v1 = e.rightHalf.startingVertex;
        v2 = e.leftHalf.startingVertex;
        d1 = f.containingPlane.pointDistance(v1.position);
        d2 = f.containingPlane.pointDistance(v2.position);
        s1 = compareToZero(d1);
        s2 = compareToZero(d2);

        if ( (s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1) ) {
            t = d1 / (d1 - d2);
            p = v1.position.add(
                    (v2.position.substract(
                         v1.position)).multiply(t));

            d3 = f.containingPlane.pointDistance(p);
            if ( compareToZero(d3) == 0 ) {
                cont = pointInFace(f, p);

                if ( cont != Geometry.OUTSIDE ) {
                    current.lmev(e.rightHalf, e.leftHalf.next(),
                                       nextVertexId(current, other), p);

                    if ( cont == Geometry.INSIDE ) {
                        addsovf(e.rightHalf, f, BvsA, sonva, sonvb);
                    }
                    else if ( cont == Geometry.LIMIT &&
                              f.lastIntersectedHalfedge != null ) {
                        current.lmev(f.lastIntersectedHalfedge,
                            f.lastIntersectedHalfedge.mirrorHalfEdge().next(),
                                     nextVertexId(current, other), p);
                        addsovv(e.rightHalf.startingVertex,
                            f.lastIntersectedHalfedge.startingVertex, BvsA,
                            sonvv);
                    }
                    else if ( cont == Geometry.LIMIT &&
                              f.lastIntersectedVertex != null ) {
                        addsovv(e.rightHalf.startingVertex,
                            f.lastIntersectedVertex, BvsA, sonvv);
                    }
                    return e.rightHalf.previous().parentEdge;
                }
            }
        }
        else {
            if ( s1 == 0 ) {
                doVertexOnFace(v1, f, BvsA, current, other, sonvv, sonva,
                    sonvb);
            }
            if ( s2 == 0 ) {
                doVertexOnFace(v2, f, BvsA, current, other, sonvv, sonva,
                    sonvb);
            }
        }

        return null;
    }

    private static void processEdge(_PolyhedralBoundedSolidEdge e,
                                    PolyhedralBoundedSolid s,
                                    int BvsA,
                                    PolyhedralBoundedSolid other,
                                    ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv,
                                    ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva,
                                    ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb)
    {
        _PolyhedralBoundedSolidFace f;
        _PolyhedralBoundedSolidEdge generatedEdge;
        int i;
        ArrayDeque<_PolyhedralBoundedSolidEdge> pendingEdges;

        pendingEdges = new ArrayDeque<_PolyhedralBoundedSolidEdge>();
        pendingEdges.addFirst(e);

        while ( !pendingEdges.isEmpty() ) {
            e = pendingEdges.removeFirst();
            for ( i = 0; i < s.polygonsList.size(); i++ ) {
                f = s.polygonsList.get(i);
                generatedEdge = doSetOpGenerate(e, f, BvsA, s, other, sonvv,
                    sonva, sonvb);
                if ( generatedEdge != null ) {
                    pendingEdges.addFirst(generatedEdge);
                }
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

        for ( i = 0; i < inSolidA.edgesList.size(); i++ ) {
            e = inSolidA.edgesList.get(i);
            processEdge(e, inSolidB, 0, inSolidA, sonvv, sonva, sonvb);
        }
        for ( i = 0; i < inSolidB.edgesList.size(); i++ ) {
            e = inSolidB.edgesList.get(i);
            processEdge(e, inSolidA, 1, inSolidB, sonvv, sonva, sonvb);
        }

        return new GenerationResult(sonvv, sonva, sonvb);
    }
}

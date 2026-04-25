//= References:                                                             =
//= [GLAS1989] Glassner, Andrew. "An introduction to ray tracing",          =
//=     Academic Press, 1989.                                               =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes;
import java.io.Serial;

import java.util.ArrayList;

import vsdk.toolkit.common.CircularDoubleLinkedList;
import vsdk.toolkit.common.FundamentalEntity;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ArrayListOfDoubles;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.processing.ComputationalGeometry;

/**
As noted in [MANT1988].10.2.1, class `_PolyhedralBoundedSolidFace` represents
one planar face of the polyhedron represented by the half-edge data
structure in a `PolyhedralBoundedSolid`. A face is defined as a planar
polygon whose interior is connected, considering that could be convex or
concave, with or without holes (but without "islands", in which case there
are more than one polygon), and based in this, a polygon can have more than
one polygonal boundary.

Note that in current implementation, the first loop in the list of boundaries
is the outer boundary, and the others are "rings" or hole loops.

Note that in the sake of simplify and eficiency current programming 
implementation of this class exhibit public access attributes. It is important
to note that those attributes will only be accessed directly from related 
classes in the same package
(vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes) and
from methods in the `PolyhedralBoundedSolid` class, and that they should
not be used from outer classes.
*/
public class _PolyhedralBoundedSolidFace extends FundamentalEntity {
    @Serial private static final long serialVersionUID = 20061118L;

    /// Defined as presented in [MANT1988].10.2.1
    public int id;

    /// Defined as presented in [MANT1988].10.2.1
    public PolyhedralBoundedSolid parentSolid;

    /// Each face should have at least one loop, corresponding to the
    /// external boundary. Each subsequent loop will be interpreted as a ring.
    /// Defined as presented in [MANT1988].10.2.1
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidLoop> boundariesList;

    //=================================================================

    public static final class PointInsideResult {
        private final int status;
        private final _PolyhedralBoundedSolidHalfEdge intersectedHalfedge;
        private final _PolyhedralBoundedSolidVertex intersectedVertex;

        private PointInsideResult(
            int status,
            _PolyhedralBoundedSolidHalfEdge intersectedHalfedge,
            _PolyhedralBoundedSolidVertex intersectedVertex)
        {
            this.status = status;
            this.intersectedHalfedge = intersectedHalfedge;
            this.intersectedVertex = intersectedVertex;
        }

        public int status()
        {
            return status;
        }

        public _PolyhedralBoundedSolidHalfEdge intersectedHalfedge()
        {
            return intersectedHalfedge;
        }

        public _PolyhedralBoundedSolidVertex intersectedVertex()
        {
            return intersectedVertex;
        }
    }

    public _PolyhedralBoundedSolidFace(PolyhedralBoundedSolid parent, int id)
    {
        init(parent, id);
    }

    private void init(PolyhedralBoundedSolid parent, int id)
    {
        this.id = id;
        parentSolid = parent;
        parentSolid.getPolygonsList().add(this);
        boundariesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidLoop>();
    }

    private static double boundaryLoopAreaMagnitude(
        _PolyhedralBoundedSolidLoop loop)
    {
        _PolyhedralBoundedSolidHalfEdge start;
        _PolyhedralBoundedSolidHalfEdge he;
        Vector3D normalAccumulator;

        if ( loop == null || loop.boundaryStartHalfEdge == null ) {
            return 0.0;
        }

        start = loop.boundaryStartHalfEdge;
        he = start;
        normalAccumulator = new Vector3D();
        do {
            Vector3D p = he.startingVertex.position;
            Vector3D q = he.next().startingVertex.position;

            normalAccumulator = normalAccumulator.add(new Vector3D(
                (p.y() - q.y()) * (p.z() + q.z()),
                (p.z() - q.z()) * (p.x() + q.x()),
                (p.x() - q.x()) * (p.y() + q.y())));
            he = he.next();
        } while ( he != start );

        return normalAccumulator.length();
    }

    private _PolyhedralBoundedSolidLoop selectLoopForPlaneCalculation()
    {
        _PolyhedralBoundedSolidLoop selectedLoop;
        int i;
        double maxAreaMagnitude;

        if ( boundariesList.size() < 1 ) {
            return null;
        }

        selectedLoop = boundariesList.get(0);
        maxAreaMagnitude = boundaryLoopAreaMagnitude(selectedLoop);

        for ( i = 1; i < boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop candidate = boundariesList.get(i);
            double candidateAreaMagnitude =
                boundaryLoopAreaMagnitude(candidate);
            if ( candidateAreaMagnitude > maxAreaMagnitude ) {
                maxAreaMagnitude = candidateAreaMagnitude;
                selectedLoop = candidate;
            }
        }

        return selectedLoop;
    }

    /**
    Find the halfedge from vertex `vn1` to vertex `vn2`. 
    Returns null if halfedge not found, or current founded halfedge otherwise.
    Build based over function `fhe` in program [MANT1988].11.9.
    @param vn1
    @param vn2
    @return requested half edge
    */
    public _PolyhedralBoundedSolidHalfEdge findHalfEdge(int vn1, int vn2)
    {
        _PolyhedralBoundedSolidLoop loop;
        _PolyhedralBoundedSolidHalfEdge he;
        int i;

        for ( i = 0; i < boundariesList.size(); i++ ) {
            loop = boundariesList.get(i);
            he = loop.halfEdgeVertices(vn1, vn2);
            if ( he != null ) {
                return he;
            }
        }
        return null;
    }

    /**
    Find the first halfedge originating from vertex `vn1`.
    Returns null if halfedge not found, or current founded halfedge otherwise.
    @param vn1
    @return requested halfedge
    */
    public _PolyhedralBoundedSolidHalfEdge findHalfEdge(int vn1)
    {
        _PolyhedralBoundedSolidLoop loop;
        _PolyhedralBoundedSolidHalfEdge he;
        int i;

        for ( i = 0; i < boundariesList.size(); i++ ) {
            loop = boundariesList.get(i);
            he = loop.firstHalfEdgeAtVertex(vn1);
            if ( he != null ) {
                return he;
            }
        }
        return null;
    }

    /**
    Compatibility hook for callers that used to refresh a cached containing
    plane. Faces no longer store that plane; use getContainingPlane() to
    compute it for the current topology.
    @return true when a containing plane cannot be calculated.
    */
    public boolean
    calculatePlane()
    {
        return getContainingPlane() == null;
    }

    public InfinitePlane getContainingPlane()
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forFace(this);

        // Ignore only numerically degenerate edges. A fixed 0.1 threshold
        // rejects valid small polygons such as finely tessellated cylinders.
        return calculatePlaneByCorner(numericContext.bigEpsilon());
    }

    /**
    Current implementation takes in to account only the first loop.
    @return a plane containing the face, or null when it cannot be calculated.
    */
    private InfinitePlane calculatePlaneByCorner (double tolerance) {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forFace(this);
        double nonColinearDotTolerance = numericContext.coplanarDotTolerance();
        _PolyhedralBoundedSolidLoop loop;
        _PolyhedralBoundedSolidHalfEdge he;
        _PolyhedralBoundedSolidHalfEdge heStart;
        _PolyhedralBoundedSolidHalfEdge heInferior;
        Vector3D p0 = new Vector3D ();
        Vector3D p1;
        Vector3D a = new Vector3D ();
        Vector3D b = new Vector3D ();
        Vector3D n1;
        Vector3D temp = new Vector3D ();
        boolean readyVecA;
        boolean readyVecB;
        double dotP;
        //domPlane: 1=xy, 2=xz, 3=yz
        byte domPlane;
        Vector3D vPrev = new Vector3D ();
        Vector3D vNext = new Vector3D ();

        if ( boundariesList.size () < 1 ) {
            return null;
        }
        loop = selectLoopForPlaneCalculation();
        if ( loop == null ) {
            return null;
        }
        he = loop.boundaryStartHalfEdge;
        if ( he == null ) {
            // Loop without starting halfedge
            return null;
        }
        heStart = he;

        // Calculate temporal normal (the sense may not be the correct), to find
        // the dominant plane.
        // The superior point is calculated too.
        readyVecA = false;
        readyVecB = false;

        do {
            //Obtain any two non collinear vectors
            p0 = he.startingVertex.position;
            p1 = he.next ().startingVertex.position;
            temp = p1.subtract(p0);
            if ( !readyVecA ) {
                if ( temp.length () > tolerance ) {
                    a = new Vector3D(temp);
                    a = a.normalized();
                    readyVecA = true;
                }
            } else if ( !readyVecB ) {
                if ( temp.length () > tolerance ) {
                    temp = temp.normalized();
                    dotP = Math.abs (temp.dotProduct (a));
                    if ( dotP < 1 - nonColinearDotTolerance ) {
                        b = new Vector3D(temp);
                        readyVecB = true;
                    }
                }
            }
            he = he.next ();
        } while ( he != heStart && !readyVecB );
        if ( (a.length () == 0) || (b.length () == 0) ) {
            // Any vector is zero.
            return null;
        }
        n1 = a.crossProduct (b); //Temporal normal.
        // Special case: triangle
        if ( loop.halfEdgesList.size () == 3 ) {
            n1 = n1.normalized();
            return new InfinitePlane (n1, p0);
        }
        //Test for dominant plane.
        //domPlane: 1=xy, 2=xz, 3=yz
        if ( Math.abs(n1.z()) > Math.abs(n1.x()) ) {
            if ( Math.abs(n1.z()) > Math.abs(n1.y()) ) {
                domPlane = 1;
            } else {
                domPlane = 2;
            }
        } else if ( Math.abs(n1.x()) > Math.abs(n1.y()) ) {
            domPlane = 3;
        } else {
            domPlane = 2;
        }
        //Find inferior point of face, given the dominant plane.
        he = loop.boundaryStartHalfEdge;
        heInferior = he;
        he = he.next ();
        while ( he != heStart ) {
            p0 = he.startingVertex.position;
            switch ( domPlane ) {
                case 1: //xy plane
                    if ( p0.y() < heInferior.startingVertex.position.y() ) {
                        heInferior = he;
                    }
                    break;
                case 2: //xz plane
                    if ( p0.z() < heInferior.startingVertex.position.z() ) {
                        heInferior = he;
                    }
                    break;
                case 3: //yz plane
                    if ( p0.z() < heInferior.startingVertex.position.z() ) {
                        heInferior = he;
                    }
                    break;
            }
            he = he.next ();
        }
        // Find next and previous vectors from inferior point(previously found) 
        // to calculate the plane.
        he = heInferior;
        p0 = heInferior.startingVertex.position;
        do {
            he = he.next ();
            p1 = he.startingVertex.position;
            vNext = p1.subtract(p0);
            if ( vNext.length () > tolerance ) {
                vNext = vNext.normalized();
                break;
            }
        } while ( he != heInferior );
        he = heInferior;
        do {
            // The previous vector should not be collinear with the first one found.
            he = he.previous ();
            p1 = he.startingVertex.position;
            vPrev = p1.subtract(p0);
            if ( vPrev.length () > tolerance ) {
                vPrev = vPrev.normalized();
                dotP = Math.abs (vPrev.dotProduct (vNext));
                if ( dotP < 1 - nonColinearDotTolerance ) {
                    break;
                }
            }
        } while ( he != heInferior );
        n1 = vNext.crossProduct (vPrev);
        return new InfinitePlane (n1, p0);
    }

    /**
    @coord: 1 means drop x, 2 means drop y and 3 means drop z
    */
    private Vector3D dropCoordinate(Vector3D in, int coord)
    {
        switch ( coord ) {
          case 1:
            // Drop X
            return new Vector3D(in.y(), in.z(), 0);
          case 2:
            // Drop Y
            return new Vector3D(in.x(), in.z(), 0);
          case 3: default:
            // Drop Z
            return new Vector3D(in.x(), in.y(), 0);
        }
    }

    /**
    Given a point p in the containing plane of this face, the method returns:
    @param p
    @param tolerance
    @return Geometry.OUTSIDE if point is outside polygon, Geometry.LIMIT if
    its in the polygon border, Geometry.INSIDE if point is inside border.
    PRE:
    - Polygon is planar
    - Point p is in the containing plane
    The structure of this algorithm follows the one outlined in
    [GLAS1989].2.3.2. with a little variation in the handlig of `sh`
    which allows this code to manage internal loops.

    Functionally, this is equivalent to procedure `contfv` proposed at
    problem [MANT1988].13.3.
    */
    public int
    testPointInside(Vector3D p, double tolerance)
    {
        return testPointInsideDetailed(p, tolerance).status();
    }

    public PointInsideResult
    testPointInsideDetailed(Vector3D p, double tolerance)
    {
        int nc; // Number of crossings
        int sh; // Sign holder for vertex crossings
        int nsh; // Next sign holder for vertex crossings

        //-----------------------------------------------------------------
        //- 1. For all vertices in face, project them in to dominant
        //- coordinate's plane
        ArrayListOfDoubles polygon2Du = new ArrayListOfDoubles(100);
        ArrayListOfDoubles polygon2Dv = new ArrayListOfDoubles(100);
        ArrayList<_PolyhedralBoundedSolidHalfEdge> polygon2Dh;
        ArrayList<_PolyhedralBoundedSolidVertex> polygon2Dvv;
        double u;
        double v;
        Vector3D projectedPoint = new Vector3D();
        int dominantCoordinate;
        int i;
        Vector3D n;

        polygon2Dh = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        polygon2Dvv = new ArrayList<_PolyhedralBoundedSolidVertex>();
        n = getContainingPlane().getNormal();

        if ( Math.abs(n.x()) >= Math.abs(n.y()) &&
             Math.abs(n.x()) >= Math.abs(n.z()) ) {
            dominantCoordinate = 1;
        }
        else if ( Math.abs(n.y()) >= Math.abs(n.x()) &&
                  Math.abs(n.y()) >= Math.abs(n.z()) ) {
            dominantCoordinate = 2;
        }
        else {
            dominantCoordinate = 3;
        }

        _PolyhedralBoundedSolidHalfEdge he;

        for ( i = 0; i < boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge heStart;
            _PolyhedralBoundedSolidHalfEdge heOld;

            loop = boundariesList.get(i);
            he = loop.boundaryStartHalfEdge;            
            if ( he == null ) {
                // Loop without starting halfedge
                return new PointInsideResult(Geometry.OUTSIDE, null, null);
            }
            heStart = he;
            do {
                if ( VSDK.vectorDistance(p, he.startingVertex.position) 
                     < 2*tolerance ) {
                    return new PointInsideResult(Geometry.LIMIT, null,
                        he.startingVertex);
                }

                projectedPoint = dropCoordinate(he.startingVertex.position,
                    dominantCoordinate);
                polygon2Du.add(projectedPoint.x());
                polygon2Dv.add(projectedPoint.y());
                polygon2Dh.add(he);
                heOld = he;
                polygon2Dvv.add(he.startingVertex);
                he = he.next();
                if ( he == null ) {
                    // Loop is not closed!
                    return new PointInsideResult(Geometry.OUTSIDE, null, null);
                }
                projectedPoint = dropCoordinate(he.startingVertex.position,
                    dominantCoordinate);
                polygon2Du.add(projectedPoint.x());
                polygon2Dv.add(projectedPoint.y());
                polygon2Dvv.add(he.startingVertex);

                if ( VSDK.vectorDistance(p, he.startingVertex.position) 
                     < 2*tolerance ) {
                    return new PointInsideResult(Geometry.LIMIT, null,
                        he.startingVertex);
                }

                if ( ComputationalGeometry.lineSegmentContainmentTest(
                         heOld.startingVertex.position,
                         he.startingVertex.position, p, tolerance
                     ) == Geometry.LIMIT ) {
                    return new PointInsideResult(Geometry.LIMIT, heOld, null);
                }
            } while( he != heStart );
        }

        projectedPoint = dropCoordinate(p, dominantCoordinate);
        u = projectedPoint.x();
        v = projectedPoint.y();

        //-----------------------------------------------------------------
        //- 2. Translate the 2D polygon such that the intersection point is
        //- in the origin
        for ( i = 0; i < polygon2Du.size(); i++ ) {
            double val;
            val = polygon2Du.get(i) - u;
            polygon2Du.set(i, val);
            val = polygon2Dv.get(i) - v;
            polygon2Dv.set(i, val);
        }
        nc = 0;

        //-----------------------------------------------------------------
        //- 3. Iterate edges
        double ua;
        double va;
        double ub;
        double vb;
        _PolyhedralBoundedSolidVertex vva;
        _PolyhedralBoundedSolidVertex vvb;

        for ( i = 0; i < polygon2Du.size() - 1; i += 2 ) {
            // This iteration tests the line segment (ua, va) - (ub, vb)
            ua = polygon2Du.get(i);
            va = polygon2Dv.get(i);
            ub = polygon2Du.get(i+1);
            vb = polygon2Dv.get(i+1);
            vva = polygon2Dvv.get(i);
            vvb = polygon2Dvv.get(i+1);

            // Note that testing line is (y = 0), so "segment crossed" can be
            // detected as a sign change in the v dimension.

            // First, calculate the va and vb signs in sh and nsh respectively
            if ( va < 0 ) {
                sh = -1;
            }
            else {
                sh = 1;
            }
            if ( vb < 0 ) {
                nsh = -1;
            }
            else {
                nsh = 1;
            }

            // If a sign change in the v dimension occurs, then report cross...
            if ( sh != nsh ) {
                // But taking into account the special case crossing occurring
                // over a vertex
                if ( ua >= 0 && ub >= 0 ) {
                    nc++;
                }
                else if ( ua >= 0 || ub >= 0 ) {
                    if ( ua - va*(ub-ua)/(vb - va) > 0 ) {
                        nc++;
                    }
                }
            }

        }

        if ( (nc % 2) == 1 ) {
            return new PointInsideResult(Geometry.INSIDE, null, null);
        }

        return new PointInsideResult(Geometry.OUTSIDE, null, null);
    }

    /**
    Current implementation only takes into account the containing plane.
    @param c
    @return 1 if this face is visible from camera c, -1 if is not visible and
    0 if is tangent to it.
    \todo : generalize to plane. This is returning "1" in cases where should
    return "-1".
    */
    public int isVisibleFrom(Camera c)
    {
        Vector3D iv = new Vector3D(1, 0, 0);
        Vector3D viewingVector;
        viewingVector = c.getRotation().multiply(iv);
        Vector3D n = getContainingPlane().getNormal();
        Vector3D cp;
        Vector3D t;
        n = n.normalized();
        double dot;
        int i;
        Vector3D p;

        if ( c.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            viewingVector = viewingVector.normalized();
            dot = n.dotProduct(viewingVector);
            if ( dot > VSDK.EPSILON ) {
                return -1;
            }
            else if ( dot > VSDK.EPSILON ) {
                return 1;
            }
            else return 0;
        }
        else {
            cp = c.getPosition();
            _PolyhedralBoundedSolidLoop l;
            for ( i = 0; i < boundariesList.size(); i++ ) {
                //System.out.println("  - Testing boundary " + i + " of " + boundariesList.size());
                l = boundariesList.get(i);
                _PolyhedralBoundedSolidHalfEdge he;
                _PolyhedralBoundedSolidHalfEdge heStart;

                he = l.boundaryStartHalfEdge;
                heStart = he;
                do {
                    // Logic
                    he = he.next();
                    if ( he == null ) {
                        // Loop is not closed!
                        break;
                    }

                    // Calculate containing plane equation for current edge
                    p = he.startingVertex.position;
                    //System.out.println("    . Testing point " + p);
                    t = p.subtract(cp);
                    t = t.multiply(-1);
                    t = t.normalized();
                    //System.out.println("     -> Viewing point " + t);
                    if ( t.dotProduct(n) > 0.0 ) {
                        return 1;
                        //System.out.println("  * Face in");
                    }
                } while( he != heStart );
            }
            //System.out.println("  * Face out");
            return -1;
        }
    }

    public void revert()
    {
        int i;

        for ( i = 0; i < boundariesList.size(); i++ ) {
            boundariesList.get(i).revert();
        }
    }

    @Override
    public String toString()
    {
        String msg;

        msg = "Face id [" + id + "], " + boundariesList.size() + " loops.";

        return msg;
    }
}

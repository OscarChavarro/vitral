//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 18 2006 - Oscar Chavarro: Original base version              =
//= - January 3 2007 - Oscar Chavarro: First phase implementation           =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [GLAS1989] Glassner, Andrew. "An introduction to ray tracing",          =
//=     Academic Press, 1989.                                               =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

package vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes;

import vsdk.toolkit.common.CircularDoubleLinkedList;
import vsdk.toolkit.common.FundamentalEntity;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ArrayListOfDoubles;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.Camera;

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
(vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes) and
from methods in the `PolyhedralBoundedSolid` class, and that they should
not be used from outer classes.
*/
public class _PolyhedralBoundedSolidFace extends FundamentalEntity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    /// Defined as presented in [MANT1988].10.2.1
    public int id;

    /// Defined as presented in [MANT1988].10.2.1
    public PolyhedralBoundedSolid parentSolid;

    /// Each face should have at least one loop, corresponding to the
    /// external boundary. Each subsequent loop will be interpreted as a ring.
    /// Defined as presented in [MANT1988].10.2.1
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidLoop> boundariesList;
    /// Defined as presented in [MANT1988].10.2.1
    public InfinitePlane containingPlane;

    //=================================================================

    public _PolyhedralBoundedSolidFace(PolyhedralBoundedSolid parent, int id)
    {
        this.id = id;
        parentSolid = parent;
        parentSolid.polygonsList.add(this);
        boundariesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidLoop>();
    }

    /**
    Find the halfedge from vertex `vn1` to vertex `vn2`. 
    Returns null if halfedge not found, or current founded halfedge otherwise.
    Build based over function `fhe` in program [MANT1988].11.9.
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
    PRE: current face points must be all co-planar. Previous solid validation
    should be made!
    POST: current face contains a plane containing the face.

    Current implementation takes in to account only the first loop.
    */
    public void
    calculatePlane()
    {
        if ( boundariesList.size() < 1 ) {
            return;
        }

        _PolyhedralBoundedSolidLoop loop;
        _PolyhedralBoundedSolidHalfEdge he, heStart;

        loop = boundariesList.get(0);

        he = loop.boundaryStartHalfEdge;
        if ( he == null ) {
            // Loop without starting halfedge
            return;
        }
        heStart = he;


        // This is only considering the first three vertices, and not taking
        // in to account the possible case of to close vertices. Should be
        // replaced to consider the full vertices set.
        Vector3D p0 = null, p1 = null;
        Vector3D p2 = null, a, b;
        Vector3D n;

        p0 = he.startingVertex.position;
        p1 = he.next().startingVertex.position;
        p2 = he.next().next().startingVertex.position;
        a = p1.substract(p0);    a.normalize();
        b = p2.substract(p0);    b.normalize();
        n = a.crossProduct(b);   n.normalize();
        containingPlane = new InfinitePlane(n, p0);

        /*
        do {
            he = he.next();
            if ( he == null ) {
                // Loop is not closed!
                break;
            }
            // ?
        } while( he != heStart );
        */

    }

    /**
    @coord: 1 means drop x, 2 means drop y and 3 means drop z
    */
    private void dropCoordinate(Vector3D in, Vector3D out, int coord)
    {
        out.z = 0;

        switch ( coord ) {
          case 1:
            // Drop X
            out.x = in.y;
            out.y = in.z;
            break;
          case 2:
            // Drop Y
            out.x = in.x;
            out.y = in.z;
            break;
          case 3: default:
            // Drop Z
            out.x = in.x;
            out.y = in.y;
            break;
        }
    }

    /**
    Given a point p in the containing plane of this face, the method returns:
    @return 1 if point is outside polygon, 0 if its in the polygon border,
    -1 if point is inside border.
    PRE:
    - Polygon is planar
    - Point p is in the containing plane
    The structure of this algorithm follows the one outlined in
    [GLAS1989].2.3.2. with a little variation in the handlig of `sh`
    which allows this code to manage internal loops.
    */
    public int
    testPointInside(Vector3D p)
    {
        Vector3D n = containingPlane.getNormal();
        int nc; // Number of crossings
        int sh; // Sign holder for vertex crossings
        int nsh; // Next sign holder for vertex crossings

        //-----------------------------------------------------------------
        //- 1. For all vertices in face, project them in to dominant
        //- coordinate's plane
        ArrayListOfDoubles polygon2Du = new ArrayListOfDoubles(100);
        ArrayListOfDoubles polygon2Dv = new ArrayListOfDoubles(100);
        double u, v;
        Vector3D projectedPoint = new Vector3D();
        int dominantCoordinate = 3;
        int i;

        if ( Math.abs(n.x) >= Math.abs(n.y) &&
             Math.abs(n.x) >= Math.abs(n.z) ) {
            dominantCoordinate = 1;
        }
        else if ( Math.abs(n.y) >= Math.abs(n.x) &&
                  Math.abs(n.y) >= Math.abs(n.z) ) {
            dominantCoordinate = 2;
        }
        else {
            dominantCoordinate = 3;
        }

        for ( i = 0; i < boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge he, heStart;

            loop = boundariesList.get(i);
            he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                // Loop without starting halfedge
                return 1;
            }
            heStart = he;
            do {
                dropCoordinate(he.startingVertex.position, projectedPoint,
                               dominantCoordinate);
                polygon2Du.append(projectedPoint.x);
                polygon2Dv.append(projectedPoint.y);
                he = he.next();
                if ( he == null ) {
                    // Loop is not closed!
                    return 1;
                }
                dropCoordinate(he.startingVertex.position, projectedPoint,
                               dominantCoordinate);
                polygon2Du.append(projectedPoint.x);
                polygon2Dv.append(projectedPoint.y);
            } while( he != heStart );
        }
        dropCoordinate(p, projectedPoint, dominantCoordinate);
        u = projectedPoint.x;
        v = projectedPoint.y;

        //-----------------------------------------------------------------
        //- 2. Translate the 2D polygon such that the intersection point is
        //- in the origin
        for ( i = 0; i < polygon2Du.size; i++ ) {
            polygon2Du.array[i] -= u;
            polygon2Dv.array[i] -= v;
        }
        nc = 0;

        //-----------------------------------------------------------------
        //- 3. Iterate edges
        double ua, va, ub, vb;

        for ( i = 0; i < polygon2Du.size - 1; i += 2 ) {
            // This iteration tests the line segment (ua, va) - (ub, vb)
            ua = polygon2Du.array[i];
            va = polygon2Dv.array[i];
            ub = polygon2Du.array[i+1];
            vb = polygon2Dv.array[i+1];

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
            return -1;
        }

        return 1;
    }

    /**
    Current implementation only takes into account the containing plane.
    @return 1 if this face is visible from camera c, -1 if is not visible and
    0 if is tangent to it.
    @todo: generalize to plane. This is returning "1" in cases where should
    return "-1".
    */
    public int isVisibleFrom(Camera c)
    {
        Vector3D iv = new Vector3D(1, 0, 0);
        Vector3D viewingVector;
        viewingVector = c.getRotation().multiply(iv);
        Vector3D n = containingPlane.getNormal();
        Vector3D cp, t;
        n.normalize();
        double dot;
        int i;
        Vector3D p;

        if ( c.getProjectionMode() == c.PROJECTION_MODE_ORTHOGONAL ) {
            viewingVector.normalize();
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
                _PolyhedralBoundedSolidHalfEdge he, heStart;

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
                    t = p.substract(cp);
                    t = t.multiply(-1);
                    t.normalize();
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

    public String toString()
    {
        String msg;

        msg = "Face id [" + id + "], " + boundariesList.size() + " loops.";

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

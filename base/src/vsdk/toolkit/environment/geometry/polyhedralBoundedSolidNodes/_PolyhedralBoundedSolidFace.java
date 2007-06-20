//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 18 2006 - Oscar Chavarro: Original base version              =
//= - January 3 2007 - Oscar Chavarro: First phase implementation           =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

package vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes;

import vsdk.toolkit.common.FundamentalEntity;
import vsdk.toolkit.common.CircularDoubleLinkedList;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.InfinitePlane;

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

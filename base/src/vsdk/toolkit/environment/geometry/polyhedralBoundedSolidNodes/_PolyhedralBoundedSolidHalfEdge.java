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
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;

/**
As noted in [MANT1988].10.2.1, a `_PolyhedralBoundedSolidHalfEdge` describes
one line segment inside a `_PolyhedralBoundedSolidLoop`. It has only a
reference to a vertex in a `PolyhedralBoundedSolid`.

Note that in the sake of simplify and eficiency current programming 
implementation this class exhibit public access attributes. It is important
to note that those attributes will only be accessed directly from related 
classes in the same package
(vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes) and
from methods in the `PolyhedralBoundedSolid` class, and that they should
not be used from outer classes.
*/
public class _PolyhedralBoundedSolidHalfEdge extends FundamentalEntity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    public static final int LEFT_SIDE = 1;
    public static final int RIGHT_SIDE = 2;
    public static final int NO_SIDE = 3;

    /// Defined as presented in [MANT1988].10.2.1
    public _PolyhedralBoundedSolidLoop parentLoop;

    /// Defined as presented in [MANT1988].10.2.2. Note that as commented in
    /// [MANT1988].10.2.2, this reference can be `null` in the special case
    /// of empty loops.
    public _PolyhedralBoundedSolidEdge parentEdge;

    /// Defined as presented in [MANT1988].10.2.1
    public _PolyhedralBoundedSolidVertex startingVertex;

    //
    public int id;
    private static int currentId = 1;

    //=================================================================
    public _PolyhedralBoundedSolidHalfEdge(_PolyhedralBoundedSolidVertex v,
        _PolyhedralBoundedSolidLoop parentLoop,
        PolyhedralBoundedSolid parentSolid)
    {
        startingVertex = v;
        this.parentLoop = parentLoop;
        parentEdge = null;

        id = currentId;
        currentId++;
    }

    /**
    Locates the previous halfedge in current list
    */
    public _PolyhedralBoundedSolidHalfEdge previous()
    {
        parentLoop.halfEdgesList.locateWindowAtElem(this);
        parentLoop.halfEdgesList.previous();
        return parentLoop.halfEdgesList.getWindow();
    }

    /**
    Locates the next halfedge in current list
    */
    public _PolyhedralBoundedSolidHalfEdge next()
    {
        parentLoop.halfEdgesList.locateWindowAtElem(this);
        parentLoop.halfEdgesList.next();
        return parentLoop.halfEdgesList.getWindow();
    }

    private int
    determineSideness()
    {
        if ( parentEdge == null ) {
            return NO_SIDE;
        }
        if ( this == parentEdge.leftHalf ) {
            return LEFT_SIDE;
        }
        else if ( this == parentEdge.rightHalf ) {
            return RIGHT_SIDE;
        }
        return NO_SIDE;
    }

    /**
    Given current half edge, this method returns complementary half edge
    with respect to parent edge. Note that this code corresponds to macro
    `mate(he)`, defined in program 10.2, from [MANT1988].10.3.
    */
    public _PolyhedralBoundedSolidHalfEdge
    mirrorHalfEdge()
    {
        if ( parentEdge == null ) return null;

        if ( this == parentEdge.rightHalf ) {
            return parentEdge.leftHalf;
        }
        return parentEdge.rightHalf;
    }

    public String toString()
    {
        String msg;
        msg = "HalfEdge id " + id + ". Parent face " + parentLoop.parentFace.id + ". ";
        if ( parentEdge == null ) {
            msg = msg + "without parent edge. ";
          }
          else {
            msg = msg + "with parent edge ";
            if ( this == parentEdge.leftHalf ) {
                msg = msg + "(left side)";
              }
              else if ( this == parentEdge.rightHalf ) {
                msg = msg + "(right side)";
              }
              else {
                msg = msg + "(INCONSISTENT!)";
              }
            ;
            msg = msg + ". ";
        }
        msg = msg + "Starting at vertex " + startingVertex.id + ". ";
        msg = msg + "Next halfedge: " + next().id + ".";
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

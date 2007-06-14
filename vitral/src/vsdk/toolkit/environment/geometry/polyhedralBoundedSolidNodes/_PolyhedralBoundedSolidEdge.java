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

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;

/**
As noted in [MANT1988].10.2.2, a `_PolyhedralBoundedSolidEdge` makes a
face-to-face relationship representing the identification of the line
segments between faces.

Note that in the sake of simplify and eficiency current programming 
implementation this class exhibit public access attributes. It is important
to note that those attributes will only be accessed directly from related 
classes in the same package
(vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes) and
from methods in the `PolyhedralBoundedSolid` class, and that they should
not be used from outer classes.
*/
public class _PolyhedralBoundedSolidEdge extends Entity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    /// Reference to `right` half edge, as defined in [MANT1988].10.2.2.
    /// Note that half edge in this side is considered positively oriented.
    public _PolyhedralBoundedSolidHalfEdge rightHalf;

    /// Reference to `right` half edge, as defined in [MANT1988].10.2.2.
    /// Note that half edge in this side is considered negatively oriented.
    public _PolyhedralBoundedSolidHalfEdge leftHalf;

    //=================================================================
    public _PolyhedralBoundedSolidEdge(PolyhedralBoundedSolid parentSolid)
    {
        parentSolid.edgesList.add(this);
    rightHalf = null;
    leftHalf = null;
    }

    public String toString()
    {
        String msg;
        msg = "Edge, half1: ";
        if ( leftHalf == null ) {
        msg = msg + "null. ";
    }
    else {
            msg = msg + "vertex " + leftHalf.startingVertex.id;
    }

        msg = msg + " / half2: ";

        if ( rightHalf == null ) {
        msg = msg + "null. ";
    }
    else {
            msg = msg + "vertex " + rightHalf.startingVertex.id;
    }

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

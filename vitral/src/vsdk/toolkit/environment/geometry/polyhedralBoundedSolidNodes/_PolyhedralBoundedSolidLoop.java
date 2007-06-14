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

/**
As noted in [MANT1988].10.2.1, a `_PolyhedralBoundedSolidLoop` describes
one connected boundary inside a `_PolyhedralBoundedSolidFace`.

Note that in the sake of simplify and eficiency current programming 
implementation of this class exhibit public access attributes. It is important
to note that those attributes will only be accessed directly from related 
classes in the same package
(vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes) and
from methods in the `PolyhedralBoundedSolid` class, and that they should
not be used from outer classes.
*/
public class _PolyhedralBoundedSolidLoop extends Entity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    /// Defined as presented in [MANT1988].10.2.1
    public _PolyhedralBoundedSolidFace parentFace;

    /// As noted in [MANT1988].10.2.3, consider that there is a special
    /// case for empty loops. Note that this case doesn't affect this
    /// reference.
    public _PolyhedralBoundedSolidHalfEdge boundaryStartHalfEdge;

    // To erase later
    private static int count = 1;
    public int id;
    //

    //=================================================================
    public _PolyhedralBoundedSolidLoop(_PolyhedralBoundedSolidFace parent)
    {
        parentFace = parent;
        parentFace.boundariesList.add(this);

        // To erase later
        id = count;
    count++;
        //
    }

    public String toString()
    {
        String msg;

        msg = "Loop id [" + id + "], parent face " + parentFace.id;

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

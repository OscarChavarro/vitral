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
public class _PolyhedralBoundedSolidFace extends Entity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    /// Defined as presented in [MANT1988].10.2.1
    public PolyhedralBoundedSolid parentSolid;

    /// Each face should have at least one loop, corresponding to the
    /// external boundary. Each subsequent loop will be interpreted as a ring.
    /// Defined as presented in [MANT1988].10.2.1
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidLoop> boundariesList;
    /// Defined as presented in [MANT1988].10.2.1
    public InfinitePlane containingPlane;

    // To erase later
    private static int count = 1;
    public int id;
    //

    //=================================================================

    public _PolyhedralBoundedSolidFace(PolyhedralBoundedSolid parent)
    {
        parentSolid = parent;
        parentSolid.polygonsList.add(this);
        boundariesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidLoop>();

        // To erase later
        id = count;
    count++;
        //
    }

    public String toString()
    {
        String msg;

        msg = "Face id [" + id + "], " + boundariesList.size() + " loops: ";

    for ( int i = 0; i < boundariesList.size(); i++ ) {
        msg = msg + boundariesList.get(i).id;
        if ( i < boundariesList.size()-1 ) msg = msg + ", ";
    }
        msg = msg + ".";

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

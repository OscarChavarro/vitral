//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 18 2006 - Oscar Chavarro: Original base version              =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;

/**
This class encapsulates a polyhedral boundary representation for 2-manifold
solids, as presented in [MANT1988]. As noted in [MANT1988].10.2.1:
The `PolyhedralBoundedSolid` class gives access to faces, edges and vertices
of the model through agregations to ArrayLists.
*/
public class PolyhedralBoundedSolid extends Solid {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    //= Main boundary representation solid data structure =============
    private ArrayList<_PolyhedralBoundedSolidFace> polygonsList;
    private ArrayList<_PolyhedralBoundedSolidLoop> boundariesList;
    private ArrayList<_PolyhedralBoundedSolidHalfEdge> halfEdgesList;
    private ArrayList<_PolyhedralBoundedSolidEdge> edgesList;

    //=================================================================

    public boolean
    doIntersection(Ray inout_rayo) {
        VSDK.reportMessage(this, VSDK.WARNING, "doIntersection",
            "Method not implemented");
    return false;
    }

    public void
    doExtraInformation(Ray inRay, double inT, 
                                  GeometryIntersectionInformation outData) {
        VSDK.reportMessage(this, VSDK.WARNING, "doExtraInformation",
            "Method not implemented");
    }

    public double[] getMinMax()
    {
        double minmax[] = new double[6];
        for ( int i = 0; i < 3; i++ ) {
        minmax[i] = -1.0;
    }
        for ( int i = 3; i < 6; i++ ) {
        minmax[i] = 1.0;
    }

        VSDK.reportMessage(this, VSDK.WARNING, "getMinMax",
            "Method not implemented");

        return minmax;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

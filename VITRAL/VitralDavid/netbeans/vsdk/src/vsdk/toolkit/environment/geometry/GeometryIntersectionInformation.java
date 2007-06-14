//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 9 2006 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.Vector3D;

public class GeometryIntersectionInformation {
    public Vector3D p;
    public Vector3D n;

    public GeometryIntersectionInformation() 
    {
        p = new Vector3D();
        n = new Vector3D();
    }

    public GeometryIntersectionInformation(GeometryIntersectionInformation b) 
    {
        p = new Vector3D(b.p);
        n = new Vector3D(b.n);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;

// An object must implement a Geometry interface in order to
// be ray traced. Using this interface it is straight forward
// to add new objects
public abstract class Geometry {
    public abstract boolean doIntersection(Ray r);
    public abstract void doExtraInformation(Ray inRay, double intT, 
                                      GeometryIntersectionInformation outData);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

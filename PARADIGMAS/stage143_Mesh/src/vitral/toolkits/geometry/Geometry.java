//===========================================================================

package vitral.toolkits.geometry;

import vitral.toolkits.common.Vector3D;
import vitral.toolkits.environment.Ray;
import vitral.toolkits.environment.Material;

// An object must implement a Geometry interface in order to
// be ray traced. Using this interface it is straight forward
// to add new objects
public abstract class Geometry {
    public Material material = null;

    public abstract boolean interseccion(Ray r);
    public abstract void informacion_extra(Ray ray, double t, 
                                           Vector3D p, Vector3D n);
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

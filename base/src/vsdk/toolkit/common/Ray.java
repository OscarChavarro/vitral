//===========================================================================

package vsdk.toolkit.common;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.VSDK;

public class Ray  extends FundamentalEntity
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    public Vector3D origin;
    public Vector3D direction;
    public double t;

    public Ray(Vector3D origin, Vector3D direction)
    {
        this.origin = new Vector3D(origin.x, origin.y, origin.z);
        this.direction = new Vector3D(direction.x, direction.y, direction.z);
        this.direction.normalize();
    }

    public Ray(Ray b)
    {
        origin = new Vector3D(b.origin);
        direction = new Vector3D(b.direction);
        t = b.t;
    }

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    */
    public String toString()
    {
        return "Ray Origin: " + origin + "; Direction: " + direction + " T: " + VSDK.formatDouble(t);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

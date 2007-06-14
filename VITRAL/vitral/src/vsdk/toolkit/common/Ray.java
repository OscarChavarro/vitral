//===========================================================================

package vsdk.toolkit.common;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.VSDK;

public class Ray  extends Entity
{
    public Vector3D origin;
    public Vector3D direction;
    public double t;

    public Ray(Vector3D eye, Vector3D dir)
    {
        origin = new Vector3D(eye.x, eye.y, eye.z);
        direction = new Vector3D(dir.x, dir.y, dir.z);
        direction.normalize();
    }

    public Ray(Ray b)
    {
        origin = new Vector3D(b.origin);
        direction = new Vector3D(b.direction);
        t = b.t;
    }

    public String toString()
    {
        return "Ray Origin: " + origin + "; Direction: " + direction + " T: " + VSDK.formatDouble(t);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

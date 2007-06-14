//===========================================================================

package vitral.toolkits.common;

import vitral.toolkits.common.Vector3D;

public class Ray {
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
        return "Ray Origin: " + origin + "; Direction: " + direction + " T: " + t;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

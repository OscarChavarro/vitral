//===========================================================================

package vitral.toolkits.environment;

import vitral.toolkits.common.Vector3D;

public class Ray {
    public Vector3D origin;
    public Vector3D direction;
    public double t;

    public Ray(Vector3D eye, Vector3D dir) {
        origin = new Vector3D(eye.x, eye.y, eye.z);
        direction = new Vector3D(dir.x, dir.y, dir.z);
        direction.normalize();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

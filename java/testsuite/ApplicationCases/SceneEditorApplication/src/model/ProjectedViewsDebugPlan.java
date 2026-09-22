//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =

package model;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;

/**
Placement of the 13 boxes (3 axis aligned faces and 10 diagonal ones) used to
present the projected views of a body for debugging, and of the cameras that
take those views. Views are numbered from 1 to `VIEW_COUNT`.
*/
public class ProjectedViewsDebugPlan
{
    public static final int VIEW_COUNT = 13;

    /**
    Position, scale and orientation of the box presenting one projected view.
    */
    public static class ViewPlacement
    {
        private final Vector3Dd position;
        private final Vector3Dd scale;
        private final Matrix4x4d rotation;

        ViewPlacement(Vector3Dd position, Vector3Dd scale, Matrix4x4d rotation)
        {
            this.position = position;
            this.scale = scale;
            this.rotation = rotation;
        }

        public Vector3Dd getPosition()
        {
            return position;
        }

        public Vector3Dd getScale()
        {
            return scale;
        }

        public Matrix4x4d getRotation()
        {
            return rotation;
        }
    }

    private ProjectedViewsDebugPlan()
    {
    }

    private static Matrix4x4d rotation(double degrees, double x, double y, double z)
    {
        return new Matrix4x4d().axisRotation(Math.toRadians(degrees), new Vector3Dd(x, y, z));
    }

    private static Matrix4x4d composed(Matrix4x4d first, Matrix4x4d second)
    {
        return second.multiply(first);
    }

    private static Vector3Dd diagonalPosition(double x, double y, double z)
    {
        return new Vector3Dd(x, y, z).normalized().multiply(1.5);
    }

    /**
    @param view number of the view, from 1 to `VIEW_COUNT`
    @return the placement of the box for that view, or null if the number is
    out of range
    */
    public static ViewPlacement getPlacement(int view)
    {
        Vector3Dd unit = new Vector3Dd(1, 1, 1);
        Vector3Dd half = new Vector3Dd(0.5, 0.5, 0.5);

        switch ( view ) {
          case 1:
            return new ViewPlacement(new Vector3Dd(0, -2, 0), unit,
                rotation(90, 1, 0, 0));
          case 2:
            return new ViewPlacement(new Vector3Dd(-2, 0, 0), unit,
                composed(rotation(90, 0, 0, -1), rotation(90, 0, -1, 0)));
          case 3:
            return new ViewPlacement(new Vector3Dd(0, 0, -2), unit,
                rotation(180, 0, 1, 0));
          case 4:
            return new ViewPlacement(diagonalPosition(-1, -1, 1), half,
                composed(rotation(45, 0, 0, -1), rotation(35, 1, -1, 0)));
          case 5:
            return new ViewPlacement(diagonalPosition(1, -1, 1), half,
                composed(rotation(45, 0, 0, 1), rotation(35, 1, 1, 0)));
          case 6:
            return new ViewPlacement(diagonalPosition(1, 1, 1), half,
                composed(rotation(135, 0, 0, 1), rotation(35, -1, 1, 0)));
          case 7:
            return new ViewPlacement(diagonalPosition(-1, 1, 1), half,
                composed(rotation(135, 0, 0, -1), rotation(35, -1, -1, 0)));
          case 8:
            return new ViewPlacement(diagonalPosition(0, 1, -1), half,
                composed(rotation(180, 0, 0, 1), rotation(135, -1, 0, 0)));
          case 9:
            return new ViewPlacement(diagonalPosition(-1, 0, -1), half,
                composed(rotation(90, 0, 0, -1), rotation(135, 0, -1, 0)));
          case 10:
            return new ViewPlacement(diagonalPosition(0, -1, -1), half,
                rotation(135, 1, 0, 0));
          case 11:
            return new ViewPlacement(diagonalPosition(1, 0, -1), half,
                composed(rotation(90, 0, 0, 1), rotation(135, 0, 1, 0)));
          case 12:
            return new ViewPlacement(diagonalPosition(1, -1, 0), half,
                composed(rotation(90, 1, 0, 0), rotation(45, 0, 0, 1)));
          case 13:
            return new ViewPlacement(diagonalPosition(1, 1, 0), half,
                composed(rotation(90, 1, 0, 0), rotation(135, 0, 0, 1)));
          default:
            return null;
        }
    }

    /**
    Creates the camera that takes one projected view of a body normalized
    inside the unit cube, as decribed in [FUNK2003].5, and figure
    [FUNK2003].8 (orthogonal projection, 10 units away from the origin).
    Views are:
      - 1   Front side view (from -Y axis)
      - 2   Lateral side view (from -X axis)
      - 3   Top side view (from -Z axis)
      - 4   Corner view from -X -Y Z direction
      - 5   Corner view from  X -Y Z direction
      - 6   Corner view from  X  Y Z direction
      - 7   Corner view from -X  Y Z direction
      - 8   Tilt view edge +Y on plane -Z
      - 9   Tilt view edge -X on plane -Z
      - 10  Tilt view edge -Y on plane -Z
      - 11  Tilt view edge +X on plane -Z
      - 12  Tilt view edge +X on plane -Y
      - 13  Tilt view edge +X on plane Y
    @param view number of the view, from 1 to `VIEW_COUNT`
    @return the camera for that view (at the origin, if the number is out of
    range)
    */
    public static Camera createCamera(int view)
    {
        Camera camera = new Camera();
        camera.setFov(90);
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.setNearPlaneDistance(2);
        camera.setFarPlaneDistance(20);

        Vector3Dd position = new Vector3Dd(0, 0, 0);
        Matrix4x4d R = new Matrix4x4d();
        double cornerAngle;

        Vector3Dd down = new Vector3Dd(0, 0, -1);
        Vector3Dd cornerReference = new Vector3Dd(10, 10, -10);
        cornerReference = cornerReference.normalized();
        cornerAngle = (Math.PI/2-(Math.acos(down.dotProduct(cornerReference))));
        switch( view ) {
          case 1:
            position = new Vector3Dd(0, -10, 0);
            R = R.eulerAnglesRotation(Math.toRadians(90), 0, 0);
            break;
          case 2:
            position = new Vector3Dd(-10, 0, 0);
            break;
          case 3:
            position = new Vector3Dd(0, 0, -10);
            R = R.eulerAnglesRotation(Math.toRadians(-90), Math.toRadians(90), 0);
            break;
          case 4:
            position = new Vector3Dd(-10, -10, 10);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(45), -cornerAngle, 0);
            break;
          case 5:
            position = new Vector3Dd(10, -10, 10);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(135), -cornerAngle, 0);
            break;
          case 6:
            position = new Vector3Dd(10, 10, 10);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(-135), -cornerAngle, 0);
            break;
          case 7:
            position = new Vector3Dd(-10, 10, 10);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(-45), -cornerAngle, 0);
            break;
          case 8:
            position = new Vector3Dd(0, 10, -10);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(-90), Math.toRadians(45), 0);
            break;
          case 9:
            position = new Vector3Dd(-10, 0, -10);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(0), Math.toRadians(45), 0);
            break;
          case 10:
            position = new Vector3Dd(0, -10, -10);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(45), 0);
            break;
          case 11:
            position = new Vector3Dd(10, 0, -10);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(180), Math.toRadians(45), 0);
            break;
          case 12:
            position = new Vector3Dd(10, -10, 0);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(135), 0, 0);
            break;
          case 13:
            position = new Vector3Dd(10, 10, 0);
            position = position.normalized();
            position = position.multiply(10);
            R = R.eulerAnglesRotation(Math.toRadians(180+45), 0, 0);
            break;
          default:
            break;
        }
        camera.setPosition(position);
        camera.setRotation(R);
        return camera;
    }
}

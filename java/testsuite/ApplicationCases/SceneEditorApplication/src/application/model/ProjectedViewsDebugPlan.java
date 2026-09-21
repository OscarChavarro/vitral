package application.model;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Placement of the 13 boxes (3 axis aligned faces and 10 diagonal ones) used to
present the projected views of a body for debugging. Views are numbered from
1 to `VIEW_COUNT`.
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
}

package vsdk.toolkit.processing;

// Vitral classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.surface.ParametricCurve;

/**
Utility class with static geometry algorithms, mostly for creating and
modifying geometric entities.

It complements:
- `ComputationalGeometry` (geometric queries)
- `SimpleTestGeometryLibrary` (sample geometry generation)

This class does not render anything. It only manipulates geometric data
structures and delegates visualization concerns to renderers.
*/
public class CurveModeler extends ProcessingElement
{
    /**
    Creates a 3D line from point (x1, y1, z1) to point (x2, y2, z2).
    */
    public static ParametricCurve
    createLine(double x1, double y1, double z1,
               double x2, double y2, double z2)
    {
        ParametricCurve lineModel;
        Vector3D[] pointParameters;

        lineModel = new ParametricCurve();
        pointParameters = new Vector3D[1];
        pointParameters[0] = new Vector3D(x1, y1, z1);
        lineModel.addPoint(pointParameters, ParametricCurve.CORNER);

        pointParameters = new Vector3D[1];
        pointParameters[0] = new Vector3D(x2, y2, z2);
        lineModel.addPoint(pointParameters, ParametricCurve.CORNER);

        return lineModel;
    }
}

// Vitral classes
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import { ParametricCurve } from "../environment/geometry/curve/ParametricCurve.js";
import { ProcessingElement } from "./ProcessingElement.js";

/**
Utility class with static geometry algorithms, mostly for creating and
modifying geometric entities.

It complements:
- `ComputationalGeometry` (geometric queries)
- `SimpleTestGeometryLibrary` (sample geometry generation)

This class does not render anything. It only manipulates geometric data
structures and delegates visualization concerns to renderers.
*/
export class CurveModeler extends ProcessingElement {
    /**
    Creates a 3D line from point (x1, y1, z1) to point (x2, y2, z2).
    */
    public static createLine(x1: number, y1: number, z1: number, x2: number, y2: number, z2: number): ParametricCurve {
        let lineModel: ParametricCurve;
        let pointParameters: Vector3Dd[];

        lineModel = new ParametricCurve();
        pointParameters = new Array<Vector3Dd>(1);
        pointParameters[0] = new Vector3Dd(x1, y1, z1);
        lineModel.addPoint(pointParameters, ParametricCurve.CORNER);

        pointParameters = new Array<Vector3Dd>(1);
        pointParameters[0] = new Vector3Dd(x2, y2, z2);
        lineModel.addPoint(pointParameters, ParametricCurve.CORNER);

        return lineModel;
    }
}

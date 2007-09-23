//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 23 2007 - Oscar Chavarro: Original base version             =
//===========================================================================

package vsdk.toolkit.processing;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.geometry.ParametricCurve;

/**
This is a utility class containing a lot of geometry operations (mostly
geometry generating and modifying operations). This is a companion class
for the `ComputationalGeometry` class, which holds geometrical querys.

This class is comprised of static methods, depending only of VSDK's Entity's.
From a design point of view, it can be view as an "strategy" design pattern
in the sense it encapsulates algorithms. It also could be viewd as a "factory"
or "abstract factory", as it is a class for creating objects, following
the data hierarchy of interface "geometry".

From a practical point of use, note this is similar to Java's `Graphics`
class in Java2D, as this is the class where user can create graphics primitives
using mid-level constructs, but note that this class is not responsible of
any rendering, rasterizing or visualization; it only uses data structures.
*/
public class GeometricModeler extends ProcessingElement
{

    /**
    Creates a 3D line from point (x1, y1, z1) to point (x2, y2, z2).
    */
    public static ParametricCurve
    createLine(double x1, double y1, double z1,
               double x2, double y2, double z2)
    {
        ParametricCurve lineModel;
        Vector3D pointParameters[];

        lineModel = new ParametricCurve();
        pointParameters = new Vector3D[1];
        pointParameters[0] = new Vector3D(x1, y1, z1);
        lineModel.addPoint(pointParameters, lineModel.CORNER);

        pointParameters = new Vector3D[1];
        pointParameters[0] = new Vector3D(x2, y2, z2);
        lineModel.addPoint(pointParameters, lineModel.CORNER);

        return lineModel;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

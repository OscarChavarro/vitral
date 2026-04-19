package vsdk.toolkit.processing;

// Java classes
import java.util.ArrayList;

// Vitral classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.ParametricCurve;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

/**
Utility class with static geometry algorithms, mostly for creating and
modifying geometric entities.

It complements:
- `ComputationalGeometry` (geometric queries)
- `SimpleTestGeometryLibrary` (sample geometry generation)

This class does not render anything. It only manipulates geometric data
structures and delegates visualization concerns to renderers.
*/
public class GeometricModeler extends ProcessingElement
{
    public static final int UNION = PolyhedralBoundedSolidModeler.UNION;
    public static final int INTERSECTION =
        PolyhedralBoundedSolidModeler.INTERSECTION;
    public static final int SUBTRACT = PolyhedralBoundedSolidModeler.SUBTRACT;
    public static final int DIFFERENCE = SUBTRACT;

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

    /**
    Implements the construction style from [MANT1988] section 12.2 / program
    12.1.

    Builds an arc on plane `z = h`, centered at (`cx`, `cy`), radius `rad`.
    The start vertex already exists in `faceId` and is identified by
    `vertexId`; the method appends `n` new edge steps from `phi1` to `phi2`
    (degrees, counterclockwise, 0 degrees on +X).
    */
    public static void addArc(PolyhedralBoundedSolid solid,
        int faceId, int vertexId,
        double cx, double cy, double rad, double h, double phi1, double phi2,
        int n)
    {
        PolyhedralBoundedSolidModeler.addArc(solid, faceId, vertexId,
            cx, cy, rad, h, phi1, phi2, n);
    }

    /**
    Implements [MANT1988] section 12.2 / program 12.2.

    Creates a planar circular lamina as a single face by:
    1) creating an initial vertex (`mvfs`)
    2) adding arc vertices (`addArc`)
    3) closing the loop (`smef`)
    */
    public static PolyhedralBoundedSolid createCircularLamina(
        double cx, double cy, double rad, double h, int n)
    {
        return PolyhedralBoundedSolidModeler.createCircularLamina(
            cx, cy, rad, h, n);
    }

    /**
    Generalized translational sweep of a face, inspired by [MANT1988] 12.3.1.

    Instead of pure translation, transform `T` can include translation,
    rotation, and scale in the face-local context.

    PRE: `face` is closed and planar.
    */
    public static void translationalSweepExtrudeFace(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face,
        Matrix4x4 T)
    {
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFace(solid,
            face, T);
    }

    /**
    Variant of `translationalSweepExtrudeFace` with an extra planar check pass.

    After sweep generation, each created side face is tested for planarity.
    If a face is non-planar, it is split once (`lmef`) to triangulate it.
    */
    public static void translationalSweepExtrudeFacePlanar(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidFace face,
        Matrix4x4 T)
    {
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, face, T);
    }

    /**
    Rotational sweep of an open wire profile around the X axis, following the
    construction style of [MANT1988] 12.2 and 12.5.

    PRE:
    - `solid` is a wire-like profile (single face, open loop)
    - profile lies on `z = 0`
    - `nfaces >= 3`
    */
    public static void rotationalSweepExtrudeWireAroundXAxis(
        PolyhedralBoundedSolid solid, int nfaces)
    {
        PolyhedralBoundedSolidModeler.rotationalSweepExtrudeWireAroundXAxis(
            solid, nfaces);
    }

    public static PolyhedralBoundedSolid createBrepFromParametricCurve(ParametricCurve curve)
    {
        return PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(
            curve);
    }

    /**
    Convenience wrapper over `PolyhedralBoundedSolidSplitter.split`.

    Splits `inSolid` by `inSplittingPlane` and appends resulting pieces to
    `outSolidsAbove` and `outSolidsBelow`.
    */
    public static void split(
                      PolyhedralBoundedSolid inSolid,
                      InfinitePlane inSplittingPlane,
                      ArrayList<PolyhedralBoundedSolid> outSolidsAbove,
                      ArrayList<PolyhedralBoundedSolid> outSolidsBelow)
    {
        PolyhedralBoundedSolidModeler.split(inSolid, inSplittingPlane,
            outSolidsAbove, outSolidsBelow);
    }

    /**
    Convenience wrapper over `PolyhedralBoundedSolidSetOperator.setOp`.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        return PolyhedralBoundedSolidModeler.setOp(inSolidA, inSolidB, op);
    }

    /**
    Convenience wrapper over `PolyhedralBoundedSolidSetOperator.setOp`
    including debug mode.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op, boolean withDebug)
    {
        return PolyhedralBoundedSolidModeler.setOp(inSolidA, inSolidB, op,
            withDebug);
    }

}

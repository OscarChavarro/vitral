//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import models.DebuggerModel;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import models.CsgSampleNames;
import models.CsgOperationNames;
import models.GeneralModelsBuilder;

public class PolyhedralBoundedSolidModelingTools
{
    public static PolyhedralBoundedSolid buildSolid(DebuggerModel model)
    {
        return GeneralModelsBuilder.buildSolid(model);
    }

    public static PolyhedralBoundedSolid createBox(Vector3D boxSize)
    {
        return GeneralModelsBuilder.createBox(boxSize);
    }

    public static PolyhedralBoundedSolid createSphere(double r)
    {
        return GeneralModelsBuilder.createSphere(r);
    }

    public static PolyhedralBoundedSolid createCone(double r1, double r2, double h)
    {
        return GeneralModelsBuilder.createCone(r1, r2, h);
    }

    public static PolyhedralBoundedSolid createCylinder(double r, double h)
    {
        return GeneralModelsBuilder.createCylinder(r, h);
    }

    public static PolyhedralBoundedSolid createCsgLampShell(
        int subdivisionCircunference, int subdivisionHeight)
    {
        return GeneralModelsBuilder.createCsgLampShell(
            subdivisionCircunference, subdivisionHeight);
    }

    public static PolyhedralBoundedSolid createArrow(double p1, double p2, double p3, double p4)
    {
        return GeneralModelsBuilder.createArrow(p1, p2, p3, p4);
    }

    public static void extrudeBox(PolyhedralBoundedSolid solid)
    {
        GeneralModelsBuilder.extrudeBox(solid);
    }

    public static PolyhedralBoundedSolid createHoledBox()
    {
        return GeneralModelsBuilder.createHoledBox();
    }

    public static PolyhedralBoundedSolid createHollowBox()
    {
        return GeneralModelsBuilder.createHollowBox();
    }

    public static PolyhedralBoundedSolid createLaminaWithTwoShells()
    {
        return GeneralModelsBuilder.createLaminaWithTwoShells();
    }

    public static PolyhedralBoundedSolid createLaminaWithHole()
    {
        return GeneralModelsBuilder.createLaminaWithHole();
    }

    public static PolyhedralBoundedSolid createFontBlock(String fontFile, String msg)
    {
        return GeneralModelsBuilder.createFontBlock(fontFile, msg);
    }

    public static PolyhedralBoundedSolid createGluedCilinders()
    {
        return GeneralModelsBuilder.createGluedCilinders();
    }

    public static PolyhedralBoundedSolid eulerOperatorsTest()
    {
        return GeneralModelsBuilder.eulerOperatorsTest();
    }

    public static PolyhedralBoundedSolid rotationalSweepTest()
    {
        return GeneralModelsBuilder.rotationalSweepTest();
    }

    public static PolyhedralBoundedSolid splitTest(int part)
    {
        return GeneralModelsBuilder.splitTest(part);
    }

    public static PolyhedralBoundedSolid csgTest(int part,
        CsgOperationNames op,
        CsgSampleNames sample,
        boolean withDebug)
    {
        return GeneralModelsBuilder.csgTest(part, op, sample, withDebug);
    }

    public static PolyhedralBoundedSolid featuredObject()
    {
        return GeneralModelsBuilder.featuredObject();
    }
}

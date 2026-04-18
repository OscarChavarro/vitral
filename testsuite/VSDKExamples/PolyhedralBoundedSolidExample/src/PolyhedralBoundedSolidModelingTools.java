//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;

public class PolyhedralBoundedSolidModelingTools
{
    public static PolyhedralBoundedSolid buildSolid(DebuggerModel model)
    {
        return BoundedSolidTestSelector.buildSolid(model);
    }

    public static PolyhedralBoundedSolid createBox(Vector3D boxSize)
    {
        return BoundedSolidTestSelector.createBox(boxSize);
    }

    public static PolyhedralBoundedSolid createSphere(double r)
    {
        return BoundedSolidTestSelector.createSphere(r);
    }

    public static PolyhedralBoundedSolid createCone(double r1, double r2, double h)
    {
        return BoundedSolidTestSelector.createCone(r1, r2, h);
    }

    public static PolyhedralBoundedSolid createCylinder(double r, double h)
    {
        return BoundedSolidTestSelector.createCylinder(r, h);
    }

    public static PolyhedralBoundedSolid createCsgLampShell(
        int subdivisionCircunference, int subdivisionHeight)
    {
        return BoundedSolidTestSelector.createCsgLampShell(
            subdivisionCircunference, subdivisionHeight);
    }

    public static PolyhedralBoundedSolid createArrow(double p1, double p2, double p3, double p4)
    {
        return BoundedSolidTestSelector.createArrow(p1, p2, p3, p4);
    }

    public static void extrudeBox(PolyhedralBoundedSolid solid)
    {
        BoundedSolidTestSelector.extrudeBox(solid);
    }

    public static PolyhedralBoundedSolid createHoledBox()
    {
        return BoundedSolidTestSelector.createHoledBox();
    }

    public static PolyhedralBoundedSolid createHollowBox()
    {
        return BoundedSolidTestSelector.createHollowBox();
    }

    public static PolyhedralBoundedSolid createLaminaWithTwoShells()
    {
        return BoundedSolidTestSelector.createLaminaWithTwoShells();
    }

    public static PolyhedralBoundedSolid createLaminaWithHole()
    {
        return BoundedSolidTestSelector.createLaminaWithHole();
    }

    public static PolyhedralBoundedSolid createFontBlock(String fontFile, String msg)
    {
        return BoundedSolidTestSelector.createFontBlock(fontFile, msg);
    }

    public static PolyhedralBoundedSolid createGluedCilinders()
    {
        return BoundedSolidTestSelector.createGluedCilinders();
    }

    public static PolyhedralBoundedSolid eulerOperatorsTest()
    {
        return BoundedSolidTestSelector.eulerOperatorsTest();
    }

    public static PolyhedralBoundedSolid rotationalSweepTest()
    {
        return BoundedSolidTestSelector.rotationalSweepTest();
    }

    public static PolyhedralBoundedSolid splitTest(int part)
    {
        return BoundedSolidTestSelector.splitTest(part);
    }

    public static PolyhedralBoundedSolid csgTest(int part,
        CsgOperationNames op,
        CsgSampleNames sample,
        boolean withDebug)
    {
        return BoundedSolidTestSelector.csgTest(part, op, sample, withDebug);
    }

    public static PolyhedralBoundedSolid featuredObject()
    {
        return BoundedSolidTestSelector.featuredObject();
    }
}

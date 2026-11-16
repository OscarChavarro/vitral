//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
#include "PolyhedralBoundedSolidModelingTools.h"

#include "models/GeneralModelsBuilder.h"

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::buildSolid(
    DebuggerModel& model)
{
    return GeneralModelsBuilder::buildSolid(model);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createBox(
    const Vector3Dd& boxSize)
{
    return GeneralModelsBuilder::createBox(boxSize);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createSphere(double r)
{
    return GeneralModelsBuilder::createSphere(r);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createCone(
    double r1, double r2, double h)
{
    return GeneralModelsBuilder::createCone(r1, r2, h);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createCylinder(
    double r, double h)
{
    return GeneralModelsBuilder::createCylinder(r, h);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createCsgLampShell(
    int subdivisionCircunference, int subdivisionHeight)
{
    return GeneralModelsBuilder::createCsgLampShell(
        subdivisionCircunference, subdivisionHeight);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createArrow(
    double p1, double p2, double p3, double p4)
{
    return GeneralModelsBuilder::createArrow(p1, p2, p3, p4);
}

void PolyhedralBoundedSolidModelingTools::extrudeBox(PolyhedralBoundedSolid* solid)
{
    GeneralModelsBuilder::extrudeBox(solid);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createHoledBox()
{
    return GeneralModelsBuilder::createHoledBox();
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createHollowBox()
{
    return GeneralModelsBuilder::createHollowBox();
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createLaminaWithTwoShells()
{
    return GeneralModelsBuilder::createLaminaWithTwoShells();
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createLaminaWithHole()
{
    return GeneralModelsBuilder::createLaminaWithHole();
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createFontBlock(
    const java::String& fontFile, const java::String& msg)
{
    return GeneralModelsBuilder::createFontBlock(fontFile, msg);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::createGluedCilinders()
{
    return GeneralModelsBuilder::createGluedCilinders();
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::eulerOperatorsTest()
{
    return GeneralModelsBuilder::eulerOperatorsTest();
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::rotationalSweepTest()
{
    return GeneralModelsBuilder::rotationalSweepTest();
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::splitTest(int part)
{
    return GeneralModelsBuilder::splitTest(part);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::csgTest(int part,
    CsgOperationNames op,
    CsgSampleNames sample,
    bool withDebug)
{
    return GeneralModelsBuilder::csgTest(part, op, sample, withDebug);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModelingTools::featuredObject()
{
    return GeneralModelsBuilder::featuredObject();
}

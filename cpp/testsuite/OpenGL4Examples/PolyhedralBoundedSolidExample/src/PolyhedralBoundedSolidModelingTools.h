//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
#ifndef __POLYHEDRAL_BOUNDED_SOLID_MODELING_TOOLS__
#define __POLYHEDRAL_BOUNDED_SOLID_MODELING_TOOLS__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "models/CsgOperationNames.h"
#include "models/CsgSampleNames.h"

class DebuggerModel;
class PolyhedralBoundedSolid;

/**
C++ port of Java's `PolyhedralBoundedSolidModelingTools`: a facade over
`GeneralModelsBuilder`. The returned solids are owned by the caller.
*/
class PolyhedralBoundedSolidModelingTools {
public:
    static PolyhedralBoundedSolid* buildSolid(DebuggerModel& model);
    static PolyhedralBoundedSolid* createBox(const Vector3Dd& boxSize);
    static PolyhedralBoundedSolid* createSphere(double r);
    static PolyhedralBoundedSolid* createCone(double r1, double r2, double h);
    static PolyhedralBoundedSolid* createCylinder(double r, double h);
    static PolyhedralBoundedSolid* createCsgLampShell(
        int subdivisionCircunference, int subdivisionHeight);
    static PolyhedralBoundedSolid* createArrow(double p1, double p2, double p3,
        double p4);
    static void extrudeBox(PolyhedralBoundedSolid* solid);
    static PolyhedralBoundedSolid* createHoledBox();
    static PolyhedralBoundedSolid* createHollowBox();
    static PolyhedralBoundedSolid* createLaminaWithTwoShells();
    static PolyhedralBoundedSolid* createLaminaWithHole();
    static PolyhedralBoundedSolid* createFontBlock(const java::String& fontFile,
        const java::String& msg);
    static PolyhedralBoundedSolid* createGluedCilinders();
    static PolyhedralBoundedSolid* eulerOperatorsTest();
    static PolyhedralBoundedSolid* rotationalSweepTest();
    static PolyhedralBoundedSolid* splitTest(int part);
    static PolyhedralBoundedSolid* csgTest(int part,
        CsgOperationNames op,
        CsgSampleNames sample,
        bool withDebug);
    static PolyhedralBoundedSolid* featuredObject();

private:
    PolyhedralBoundedSolidModelingTools() {}
};

#endif

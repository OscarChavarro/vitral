//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
#ifndef __POLYHEDRAL_EXAMPLE_GENERAL_MODELS_BUILDER__
#define __POLYHEDRAL_EXAMPLE_GENERAL_MODELS_BUILDER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "models/CsgOperationNames.h"
#include "models/CsgSampleNames.h"

class DebuggerModel;
class PolyhedralBoundedSolid;

/**
C++ port of Java's `models.GeneralModelsBuilder`.

C++ port notes:
- Every returned solid is new and owned by the caller; the intermediate
  solids (operands of set operations, split pieces) are deleted.
- The Java `PolyhedralBoundedSolid[]` operand pairs are lists of two
  solids, as in the C++ `SimpleTestGeometryLibrary`.
*/
class GeneralModelsBuilder {
public:
    static PolyhedralBoundedSolid* buildSolid(DebuggerModel& model);

    static PolyhedralBoundedSolid* createBox(const Vector3Dd& boxSize);
    static PolyhedralBoundedSolid* createSphere(double r);
    static PolyhedralBoundedSolid* createSphere(double r,
        int subdivisionCircunference, int subdivisionHeight);
    static PolyhedralBoundedSolid* createCone(double r1, double r2, double h);
    static PolyhedralBoundedSolid* createCone(double r1, double r2,
        double h, int subdivisionCircunference, int subdivisionHeight);
    static PolyhedralBoundedSolid* createCylinder(double r, double h);
    static PolyhedralBoundedSolid* createCylinder(double r, double h,
        int subdivisionCircunference, int subdivisionHeight);
    static PolyhedralBoundedSolid* createCsgLampShell(
        int subdivisionCircunference, int subdivisionHeight);
    static PolyhedralBoundedSolid* createArrow(double p1, double p2,
        double p3, double p4);
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
    static PolyhedralBoundedSolid* csgTest(int part,
        CsgOperationNames op,
        CsgSampleNames sample,
        bool withDebug,
        int kurlanderSingleMotifIndex);
    static PolyhedralBoundedSolid* featuredObject();

private:
    GeneralModelsBuilder() {}

    static void printKurlanderAllMotifsProgressHint(CsgSampleNames sample,
        int part);
    static void printProgressMessage(const java::String& message);
    static PolyhedralBoundedSolid* importFromFile(const java::String& filename);
    static PolyhedralBoundedSolid* importFromStepFile(const java::String& filename);
    static java::ArrayList<PolyhedralBoundedSolid*> buildCsgMoonBlock();
    static void glue(PolyhedralBoundedSolid* solid1,
        PolyhedralBoundedSolid* solid2, int faceid1, int faceid2);
    static void rotationalSweepVersion1(PolyhedralBoundedSolid* solid,
        int nfaces);
    static void rotationalSweepVersion2(PolyhedralBoundedSolid* solid,
        int nfaces);
    static PolyhedralBoundedSolid* createTestTorus(int nsides, int nrad);
    static java::ArrayList<PolyhedralBoundedSolid*> buildCsgTest2();
    static java::ArrayList<PolyhedralBoundedSolid*> buildCsgTest4();
    static java::ArrayList<PolyhedralBoundedSolid*> buildCsgTest5();
    static java::ArrayList<PolyhedralBoundedSolid*> createCsgOperands(
        CsgSampleNames sample);
    static java::ArrayList<PolyhedralBoundedSolid*> createCsgOperands(
        CsgSampleNames sample,
        int kurlanderSingleMotifIndex);
    static java::ArrayList<PolyhedralBoundedSolid*> createCsgHudPreviewOperands(
        CsgSampleNames sample);
    static java::ArrayList<PolyhedralBoundedSolid*> createCsgHudPreviewOperands(
        CsgSampleNames sample,
        int kurlanderSingleMotifIndex);
};

#endif

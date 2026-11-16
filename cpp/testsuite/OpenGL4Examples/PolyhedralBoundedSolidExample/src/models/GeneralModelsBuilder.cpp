//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
#include <cmath>

#include "java/util/ArrayList.txx"
#include "models/GeneralModelsBuilder.h"

#include "java/io/File.h"
#include "java/lang/Exception.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/Math.h"
#include "java/lang/System.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerConstructions.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#ifdef VITRAL_WITH_FREETYPE
#include "vsdk/toolkit/io/geometry/Freetype2FontReader.h"
#endif
#include "vsdk/toolkit/io/geometry/stepCad/reader/StepReader.h"
#include "models/DebuggerModel.h"

typedef PolyhedralBoundedSolidEulerOperators EulerOps;

/// Java: `getClass().getName()` of the reporting class
static const char* const CLASS_NAME = "models.GeneralModelsBuilder";

/// C++ port: deletes the solids that are no longer needed
static void deleteSolids(java::ArrayList<PolyhedralBoundedSolid*>& solids)
{
    long int i;
    for ( i = 0; i < solids.size(); i++ ) {
        delete solids[i];
        solids[i] = nullptr;
    }
}

PolyhedralBoundedSolid* GeneralModelsBuilder::buildSolid(DebuggerModel& model)
{
    PolyhedralBoundedSolid* mySolid;
    java::ArrayList<PolyhedralBoundedSolid*> csgPreviewOperands;
    Matrix4x4d translationMatrix;
    Matrix4x4d rotationMatrix;
    Matrix4x4d scaleMatrix;
    Matrix4x4d transformationMatrix;
    model.clampSubdivisions();
    model.setCsgPreviewOperandA(nullptr);
    model.setCsgPreviewOperandB(nullptr);

    switch ( model.getSolidModelName() ) {
      case SolidModelNames::MVFS_SMEV_SAMPLE:
        mySolid = new PolyhedralBoundedSolid();
        EulerOps::mvfs(mySolid, Vector3Dd(0.1, 0.1, 0.1), 1, 1);
        EulerOps::smev(mySolid, 1, 1, 4, Vector3Dd(0.1, 1, 0.1));
        EulerOps::smev(mySolid, 1, 4, 3, Vector3Dd(1, 1, 0.1));
        break;
      case SolidModelNames::BOX:
        mySolid = createBox(Vector3Dd(0.9, 0.9, 0.9));
        break;
      case SolidModelNames::ARC_SAMPLE:
        mySolid = new PolyhedralBoundedSolid();
        EulerOps::mvfs(mySolid, Vector3Dd(1, 0.5, 0.1), 1, 1);
        PolyhedralBoundedSolidEulerConstructions::addArcToExistingFace(
            mySolid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 9);
        break;
      case SolidModelNames::CIRCULAR_LAMINA:
        mySolid = PolyhedralBoundedSolidEulerConstructions::createCircularLamina(
            0.5, 0.5, 0.5, 0.1, 12
        );
        break;
      case SolidModelNames::TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC:
        mySolid = new PolyhedralBoundedSolid();
        EulerOps::mvfs(mySolid, Vector3Dd(1, 0.5, 0.1), 1, 1);
        PolyhedralBoundedSolidEulerConstructions::addArcToExistingFace(
            mySolid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 18);

        translationMatrix = Matrix4x4d();
        translationMatrix = translationMatrix.translation(0.0, 0.0, 0.5);
        rotationMatrix = Matrix4x4d();
        rotationMatrix = rotationMatrix.axisRotation(java::Math::toRadians(5), 0, 1, 0);
        scaleMatrix = Matrix4x4d();
        scaleMatrix = scaleMatrix.scale(0.5, 0.5, 0.5);
        transformationMatrix = translationMatrix.multiply(rotationMatrix.multiply(scaleMatrix));

        PolyhedralBoundedSolidEulerConstructions::translationalSweepExtrudeFacePlanar(
            mySolid, mySolid->findFace(1), transformationMatrix);

        break;
      case SolidModelNames::TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR:
        mySolid = PolyhedralBoundedSolidEulerConstructions::createCircularLamina(
            0.5, 0.5, 0.5, 0.1, 24
        );

        translationMatrix = Matrix4x4d();
        translationMatrix = translationMatrix.translation(0.0, 0.0, 0.5);
        rotationMatrix = Matrix4x4d();
        rotationMatrix = rotationMatrix.axisRotation(java::Math::toRadians(5), 0, 1, 0);
        scaleMatrix = Matrix4x4d();
        scaleMatrix = scaleMatrix.scale(0.5, 0.5, 0.5);
        transformationMatrix = translationMatrix.multiply(rotationMatrix.multiply(scaleMatrix));
        PolyhedralBoundedSolidEulerConstructions::translationalSweepExtrudeFacePlanar(
            mySolid, mySolid->findFace(1), transformationMatrix);
        break;

      case SolidModelNames::SPHERE:
        mySolid = createSphere(0.5, model.getSubdivisionCircumference(),
            model.getSubdivisionHeight());
        break;
      case SolidModelNames::CONE:
        mySolid = createCone(0.5, 0.0, 1.0,
            model.getSubdivisionCircumference(), model.getSubdivisionHeight());
        break;
      case SolidModelNames::CYLINDER:
        mySolid = createCylinder(0.5, 1.0,
            model.getSubdivisionCircumference(), model.getSubdivisionHeight());
        break;
      case SolidModelNames::CSG_MOON_BLOCK:
        csgPreviewOperands = createCsgHudPreviewOperands(
            CsgSampleNames::MOON_BLOCK);
        model.setCsgPreviewOperandA(csgPreviewOperands[0]);
        model.setCsgPreviewOperandB(csgPreviewOperands[1]);
        mySolid = csgTest(1, CsgOperationNames::DIFFERENCE_A_MINUS_B,
            CsgSampleNames::MOON_BLOCK, model.isDebugCsg());
        model.setDebugCsg(false);
        break;
      case SolidModelNames::CSG_LAMP_SHELL:
        mySolid = createCsgLampShell(model.getSubdivisionCircumference(),
            model.getSubdivisionHeight());
        break;
      case SolidModelNames::ARROW:
        mySolid = createArrow(0.7, 0.3, 0.05, 0.1);
        break;
      case SolidModelNames::LAMINA_WITH_TWO_SHELLS:
        mySolid = createLaminaWithTwoShells();
        break;
      case SolidModelNames::LAMINA_WITH_HOLE:
        mySolid = createLaminaWithHole();
        break;
      case SolidModelNames::FONT_BLOCK:
        mySolid = createFontBlock("../../../../etc/fonts/cyrvetic.ttf", "A");

        translationMatrix = Matrix4x4d();
        translationMatrix = translationMatrix.translation(0.0, 0.0, 0.1);

        PolyhedralBoundedSolidEulerConstructions::translationalSweepExtrudeFacePlanar(
            mySolid, mySolid->findFace(1), translationMatrix);

        break;
      case SolidModelNames::GLUED_CYLINDERS:
        mySolid = createGluedCilinders();
        break;
      case SolidModelNames::EULER_OPERATORS_TEST:
        mySolid = eulerOperatorsTest();
        break;
      case SolidModelNames::ROTATIONAL_SWEEP:
        mySolid = rotationalSweepTest();
        break;
      case SolidModelNames::SPLIT_TEST_PART_1:
        mySolid = splitTest(1);
        break;
      case SolidModelNames::SPLIT_TEST_PART_2:
        mySolid = splitTest(2);
        break;
      case SolidModelNames::SPLIT_TEST_PART_3:
        mySolid = splitTest(3);
        break;
      case SolidModelNames::CSG_DIRECT:
        printKurlanderAllMotifsProgressHint(model.getCsgSample(), 1);
        csgPreviewOperands = createCsgHudPreviewOperands(
            model.getCsgSample(),
            model.getKurlanderBowlSingleMotifIndex());
        model.setCsgPreviewOperandA(csgPreviewOperands[0]);
        model.setCsgPreviewOperandB(csgPreviewOperands[1]);
        mySolid = csgTest(1, model.getCsgOperation(),
            model.getCsgSample(), model.isDebugCsg(),
            model.getKurlanderBowlSingleMotifIndex());
        model.setDebugCsg(false);
        break;
      case SolidModelNames::CSG_OPERAND1_PARTIAL:
        printKurlanderAllMotifsProgressHint(model.getCsgSample(), 2);
        csgPreviewOperands = createCsgHudPreviewOperands(
            model.getCsgSample(),
            model.getKurlanderBowlSingleMotifIndex());
        model.setCsgPreviewOperandA(csgPreviewOperands[0]);
        model.setCsgPreviewOperandB(csgPreviewOperands[1]);
        mySolid = csgTest(2, model.getCsgOperation(),
            model.getCsgSample(), model.isDebugCsg(),
            model.getKurlanderBowlSingleMotifIndex());
        model.setDebugCsg(false);
        break;
      case SolidModelNames::CSG_OPERAND2_PARTIAL:
        printKurlanderAllMotifsProgressHint(model.getCsgSample(), 3);
        csgPreviewOperands = createCsgHudPreviewOperands(
            model.getCsgSample(),
            model.getKurlanderBowlSingleMotifIndex());
        model.setCsgPreviewOperandA(csgPreviewOperands[0]);
        model.setCsgPreviewOperandB(csgPreviewOperands[1]);
        mySolid = csgTest(3, model.getCsgOperation(),
            model.getCsgSample(), model.isDebugCsg(),
            model.getKurlanderBowlSingleMotifIndex());
        model.setDebugCsg(false);
        break;
      case SolidModelNames::FEATURED_OBJECT:
        mySolid = featuredObject();
        break;
      case SolidModelNames::IMPORT_OR_FEATURED_OBJECT:
        {
            java::File fd("/tmp/solid.bin");
            if ( fd.exists() ) {
                mySolid = importFromFile("/tmp/solid.bin");
            }
            else {
                mySolid = featuredObject();
            }
        }
        break;
      case SolidModelNames::STEP_IMPORT:
        mySolid = importFromStepFile("../../../../etc/solids/kurlanderBowl.step");
        break;
      case SolidModelNames::HOLLOW_BOX:
        mySolid = createHollowBox();
        break;
      case SolidModelNames::HOLED_BOX:
      default:
        mySolid = createHoledBox();
        break;
    }

    return mySolid;
}

void GeneralModelsBuilder::printKurlanderAllMotifsProgressHint(
    CsgSampleNames /*sample*/,
    int part)
{
    if ( part != 1 ) {
        printProgressMessage(
            java::String("KURLANDER_BOWL_ALL_MOTIFS selected as operand preview; ") +
            "full motif processing only runs in CSG_DIRECT.");
        return;
    }
    printProgressMessage(
        "KURLANDER_BOWL_ALL_MOTIFS selected; full motif processing will start.");
}

void GeneralModelsBuilder::printProgressMessage(const java::String& message)
{
    java::System::out.println(message.c_str());
    java::System::out.flush();
    java::System::err.println(message.c_str());
    java::System::err.flush();
}

/**
C++ port note: Java reads the solid with Java serialization
(`ObjectInputStream`), a format with no C++ reader, so this always takes
the Java error path (an `IOException`): a warning, and a null solid.
*/
PolyhedralBoundedSolid* GeneralModelsBuilder::importFromFile(
    const java::String& filename)
{
    PolyhedralBoundedSolid* mysolid = nullptr;

    java::Exception e(java::String("Java object serialization is not "
        "available in C++: ") + filename);
    Logger::reportMessageWithException(
        CLASS_NAME,
        Logger::WARNING,
        "importFromFile",
        java::String("Error reading solid from file ") + filename, &e);

    return mysolid;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::importFromStepFile(
    const java::String& filename)
{
    try {
        java::File file(filename);
        PolyhedralBoundedSolid* solid = StepReader::readSolid(&file);
        if ( solid == nullptr ) {
            Logger::reportMessage(
                CLASS_NAME,
                Logger::WARNING,
                "importFromStepFile",
                java::String("StepReader returned null for ") + filename);
            return createHoledBox();
        }
        return solid;
    }
    catch ( const java::Exception& e ) {
        Logger::reportMessageWithException(
            CLASS_NAME,
            Logger::WARNING,
            "importFromStepFile",
            java::String("Could not read STEP file ") + filename
            + " — falling back to holed box.", &e);
        return createHoledBox();
    }
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createBox(const Vector3Dd& boxSize)
{
    PolyhedralBoundedSolid* solid;

    Matrix4x4d R;
    R = R.translation(0.55, 0.55, 0.55);

    Box b(boxSize);
    solid = b.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler::applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createSphere(double r)
{
    return createSphere(r, 16, 8);
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createSphere(double r,
    int subdivisionCircunference, int subdivisionHeight)
{
    PolyhedralBoundedSolid* solid;

    Matrix4x4d R;
    R = R.translation(0.55, 0.55, 0.55);

    Sphere s(r);
    solid = s.exportToPolyhedralBoundedSolid(subdivisionCircunference,
        subdivisionHeight);
    PolyhedralBoundedSolidModeler::applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createCone(double r1, double r2,
    double h)
{
    return createCone(r1, r2, h, 36, 1);
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createCone(double r1, double r2,
    double h, int subdivisionCircunference, int subdivisionHeight)
{
    PolyhedralBoundedSolid* solid;

    Matrix4x4d R;
    R = R.translation(0.55, 0.55, 0.05);

    Cone c(r1, r2, h);
    solid = c.exportToPolyhedralBoundedSolid(subdivisionCircunference,
        subdivisionHeight);
    PolyhedralBoundedSolidModeler::applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createCylinder(double r, double h)
{
    return createCylinder(r, h, 36, 1);
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createCylinder(double r, double h,
    int subdivisionCircunference, int subdivisionHeight)
{
    PolyhedralBoundedSolid* solid;

    Matrix4x4d R;
    R = R.translation(0.55, 0.55, 0.05);

    Cone c(r, r, h);
    solid = c.exportToPolyhedralBoundedSolid(subdivisionCircunference,
        subdivisionHeight);
    PolyhedralBoundedSolidModeler::applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

java::ArrayList<PolyhedralBoundedSolid*> GeneralModelsBuilder::buildCsgMoonBlock()
{
    PolyhedralBoundedSolid* cylinderA = createCylinder(0.5, 1.0);
    PolyhedralBoundedSolid* cylinderB = createCylinder(0.5, 2);

    Matrix4x4d T;
    T = T.translation(0.275, 0.0, -0.5);
    PolyhedralBoundedSolidModeler::applyTransformation(cylinderB, T);

    PolyhedralBoundedSolidValidationEngine::validateIntermediate(cylinderA);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(cylinderB);

    java::ArrayList<PolyhedralBoundedSolid*> operands;
    operands.add(cylinderA);
    operands.add(cylinderB);
    return operands;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createCsgLampShell(
    int subdivisionCircunference, int subdivisionHeight)
{
    double outerRadius = 0.5;
    double innerRadius = 0.45;

    PolyhedralBoundedSolid* outerSphere = createSphere(outerRadius,
        subdivisionCircunference, subdivisionHeight);
    PolyhedralBoundedSolid* innerSphere = createSphere(innerRadius,
        subdivisionCircunference, subdivisionHeight);

    PolyhedralBoundedSolidStatistics::reset();
    PolyhedralBoundedSolid* sphericalShell;
    try {
        sphericalShell = PolyhedralBoundedSolidModeler::setOp(
            outerSphere, innerSphere, PolyhedralBoundedSolidModeler::SUBTRACT,
            false);
    }
    catch ( ... ) {
        delete outerSphere;
        delete innerSphere;
        throw;
    }
    delete outerSphere;
    delete innerSphere;

    // Cube fully contains shell in X/Y, starts below it in Z and stops at
    // ~80% of shell height to mimic the unperforated lamp bowl profile.
    Box clipCubeGeometry(Vector3Dd(1.4, 1.4, 1.05));
    PolyhedralBoundedSolid* clipCube = clipCubeGeometry
        .exportToPolyhedralBoundedSolid();
    Matrix4x4d cubeMove;
    cubeMove = cubeMove.translation(0.55, 0.55, 0.325);
    PolyhedralBoundedSolidModeler::applyTransformation(clipCube, cubeMove);

    PolyhedralBoundedSolidStatistics::reset();
    PolyhedralBoundedSolid* result;
    try {
        result = PolyhedralBoundedSolidModeler::setOp(
            sphericalShell, clipCube,
            PolyhedralBoundedSolidModeler::INTERSECTION, false);
    }
    catch ( ... ) {
        delete sphericalShell;
        delete clipCube;
        throw;
    }
    delete sphericalShell;
    delete clipCube;
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(result);
    return result;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createArrow(double p1, double p2,
    double p3, double p4)
{
    PolyhedralBoundedSolid* solid;

    Matrix4x4d R;
    R = R.translation(0.55, 0.55, 0.05);

    Arrow a(p1, p2, p3, p4);
    solid = a.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler::applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

/**
PRE:
Works on the output of `createBox` method, for a box from <0.1, 0.1, 0.1>
to <1, 1, 1>
*/
void GeneralModelsBuilder::extrudeBox(PolyhedralBoundedSolid* solid)
{
    //- Cube modification to holed box --------------------------------
    EulerOps::smev(solid, 6, 5, 9, Vector3Dd(0.3, 0.3, 1));
    EulerOps::kemr(solid, 6, 6, 5, 9, 9, 5);
    EulerOps::smev(solid, 6, 9, 10, Vector3Dd(0.8, 0.3, 1));
    EulerOps::smev(solid, 6, 10, 11, Vector3Dd(0.8, 0.8, 1));
    EulerOps::smev(solid, 6, 11, 12, Vector3Dd(0.3, 0.8, 1));
    EulerOps::mef(solid, 6, 6, 9, 10, 12, 11, 7);

    //- Box extrusion -------------------------------------------------
    EulerOps::smev(solid, 7, 9, 13, Vector3Dd(0.3, 0.3, 0.1));
    EulerOps::smev(solid, 7, 10, 14, Vector3Dd(0.8, 0.3, 0.1));
    EulerOps::mef(solid, 7, 7, 13, 9, 14, 10, 8);
    EulerOps::smev(solid, 7, 11, 15, Vector3Dd(0.8, 0.8, 0.1));
    EulerOps::mef(solid, 7, 7, 14, 10, 15, 11, 9);
    EulerOps::smev(solid, 7, 12, 16, Vector3Dd(0.3, 0.8, 0.1));
    EulerOps::mef(solid, 7, 7, 15, 11, 16, 12, 10);
    EulerOps::mef(solid, 7, 7, 13, 14, 16, 12, 11);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
}

/**
This method implements the example presented in section [MANT1988].9.3,
and figure [MANT1988].9.11.
*/
PolyhedralBoundedSolid* GeneralModelsBuilder::createHoledBox()
{
    PolyhedralBoundedSolid* solid;

    solid = GeneralModelsBuilder::createBox(Vector3Dd(0.9, 0.9, 0.9));
    GeneralModelsBuilder::extrudeBox(solid);
    EulerOps::kfmrh(solid, 2, 11);
    //R = R.translation(-0.55, -0.55, -0.55);
    //PolyhedralBoundedSolidModeler.applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);

    return solid;
}

/**
This method is a test for the solution to problem [MANT1988].15.1 on case of one solid
fully inside another.
*/
PolyhedralBoundedSolid* GeneralModelsBuilder::createHollowBox()
{
    PolyhedralBoundedSolid* solidA;
    PolyhedralBoundedSolid* solidB;
    PolyhedralBoundedSolid* result;

    // Outer box.
    solidA = GeneralModelsBuilder::createBox(Vector3Dd(0.9, 0.9, 0.9));

    // Inner box at 80% size, centered at the same position as solidA.
    solidB = GeneralModelsBuilder::createBox(Vector3Dd(0.72, 0.72, 0.72));

    // Hollow box = outer box minus inner box.
    try {
        result = PolyhedralBoundedSolidModeler::setOp(solidA, solidB,
            PolyhedralBoundedSolidModeler::SUBTRACT);
    }
    catch ( ... ) {
        delete solidA;
        delete solidB;
        throw;
    }
    delete solidA;
    delete solidB;

    return result;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::createLaminaWithTwoShells()
{
    //- Basic lamina --------------------------------------------------
    Matrix4x4d R;
    PolyhedralBoundedSolid* solid;

    R = R.translation(0.55, 0.55, 0.55);
    solid = new PolyhedralBoundedSolid();
    EulerOps::mvfs(solid, Vector3Dd(-0.5, -0.5, 0), 1, 1);
    EulerOps::smev(solid, 1, 1, 4, Vector3Dd(-0.5, 0.0, 0));
    EulerOps::smev(solid, 1, 4, 3, Vector3Dd(0.5, 0.0, 0));
    EulerOps::smev(solid, 1, 3, 2, Vector3Dd(0.5, -0.5, 0));
    EulerOps::mef(solid, 1, 1, 1, 4, 2, 3, 2);

    //- Hole ----------------------------------------------------------
    EulerOps::smev(solid, 1, 1, 5, Vector3Dd(-0.3, 0.1, 0));
    EulerOps::kemr(solid, 1, 1, 1, 5, 5, 1);
    EulerOps::smev(solid, 1, 5, 6, Vector3Dd(0.0, 0.4, 0));
    EulerOps::smev(solid, 1, 6, 7, Vector3Dd(0.3, 0.1, 0));
    EulerOps::mef(solid, 1, /* face1 */
              1, /* face2 */
              5, /* v1 */
              6, /* v2 */
              7, /* v3 */
              6, /* v4 */
              3  /* newfaceid */);

    EulerOps::kfmrh(solid, 2, 3);

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidModeler::applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;

}

PolyhedralBoundedSolid* GeneralModelsBuilder::createLaminaWithHole()
{
    //- Basic lamina --------------------------------------------------
    Matrix4x4d R;
    PolyhedralBoundedSolid* solid;

    R = R.translation(0.55, 0.55, 0.55);
    solid = new PolyhedralBoundedSolid();
    EulerOps::mvfs(solid, Vector3Dd(-0.5, -0.5, 0), 1, 1);
    EulerOps::smev(solid, 1, 1, 4, Vector3Dd(-0.5, 0.5, 0));
    EulerOps::smev(solid, 1, 4, 3, Vector3Dd(0.5, 0.5, 0));
    EulerOps::smev(solid, 1, 3, 2, Vector3Dd(0.5, -0.5, 0));
    EulerOps::mef(solid, 1, 1, 1, 4, 2, 3, 2);

    //- Hole ----------------------------------------------------------
    EulerOps::smev(solid, 1, 1, 5, Vector3Dd(-0.3, -0.3, 0));
    EulerOps::kemr(solid, 1, 1, 1, 5, 5, 1);
    EulerOps::smev(solid, 1, 5, 6, Vector3Dd(0.0, 0.3, 0));
    EulerOps::smev(solid, 1, 6, 7, Vector3Dd(0.3, -0.3, 0));
    EulerOps::mef(solid, 1, /* face1 */
              1, /* face2 */
              5, /* v1 */
              6, /* v2 */
              7, /* v3 */
              6, /* v4 */
              3  /* newfaceid */);

    EulerOps::kfmrh(solid, 2, 3);

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidModeler::applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

/**
C++ port notes: the text is UTF-8, and `Freetype2FontReader` is the C++
counterpart of Java's `AwtFontReader`.
*/
PolyhedralBoundedSolid* GeneralModelsBuilder::createFontBlock(
    const java::String& fontFile, const java::String& msg)
{
    if ( msg.trim().isEmpty() ) {
        throw java::IllegalArgumentException("Font block text cannot be blank");
    }

    // Java: `new String(Character.toChars(msg.codePointAt(0)))`, the first
    // code point (one UTF-8 sequence)
    int length = 1;
    unsigned char lead = (unsigned char)msg.charAt(0);
    if ( (lead & 0xE0) == 0xC0 ) {
        length = 2;
    }
    else if ( (lead & 0xF0) == 0xE0 ) {
        length = 3;
    }
    else if ( (lead & 0xF8) == 0xF0 ) {
        length = 4;
    }
    if ( length > msg.length() ) {
        length = msg.length();
    }
    java::String character = msg.substring(0, length);

#ifdef VITRAL_WITH_FREETYPE
    Freetype2FontReader fontReader;
    ParametricCurve* curve = fontReader.extractGlyph(fontFile, character);
#else
    ParametricCurve* curve = nullptr;
#endif
    if ( curve == nullptr ) {
        throw java::IllegalStateException(
            java::String("Unable to extract glyph '") + character +
            "' from font " + fontFile);
    }
    // Java: `curve.types.size()`, one type per control point
    if ( curve->getPointSize() < 2 ) {
        delete curve;
        throw java::IllegalStateException(
            java::String("Glyph '") + character + "' from font " + fontFile
            + " did not produce a usable parametric curve");
    }
    curve->setApproximationSteps(8);

    PolyhedralBoundedSolid* solid;
    try {
        solid = PolyhedralBoundedSolidModeler::createBrepFromParametricCurve(curve);
    }
    catch ( ... ) {
        delete curve;
        throw;
    }
    delete curve;
    return solid;
}

/**
After algorithm decribed on section [MANT1988].12.4, and program
[MANT1988].12.7.
C++ port note: `solid2` is drained by `merge`, and deleted.
*/
void GeneralModelsBuilder::glue(PolyhedralBoundedSolid* solid1,
    PolyhedralBoundedSolid* solid2, int faceid1, int faceid2)
{
    solid1->merge(solid2);
    delete solid2;
    EulerOps::kfmrh(solid1, faceid1, faceid2);
    PolyhedralBoundedSolidTopologyEditing::loopGlue(solid1, faceid1);
}

/**
This method builds a test solid for evaluating the gluing algorithm in
a controlled way, as proposed on the example from section [MANT1988].12.4.
It is similar to the solid shown on figure [MANT1988].12.2.
*/
PolyhedralBoundedSolid* GeneralModelsBuilder::createGluedCilinders()
{
    //- Create cilynder 1 ---------------------------------------------
    PolyhedralBoundedSolid* solid1;

    Matrix4x4d T;
    T = T.translation(0, 0, 0.4);

    solid1 = PolyhedralBoundedSolidEulerConstructions::createCircularLamina(
        0.0, 0.0, 0.5, 0.0, 6
    );
    PolyhedralBoundedSolidEulerConstructions::translationalSweepExtrudeFacePlanar(
        solid1, solid1->findFace(1), T);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid1);

    //-----------------------------------------------------------------
    double ang = (2*M_PI) / 6;
    Matrix4x4d R;
    Vector3Dd a(0.5, 0, 0);
    Vector3Dd b(0.5*std::cos(ang), 0.5*std::sin(ang), 0);
    Vector3Dd c = a.add(b);
    c = c.multiply(0.5);
    R = R.translation(-c.x(), c.y(), c.z());
    PolyhedralBoundedSolidModeler::applyTransformation(solid1, R);

    //- Create cilynder 2 ---------------------------------------------
    PolyhedralBoundedSolid* solid2;

    solid2 = PolyhedralBoundedSolidEulerConstructions::createCircularLamina(
        0.0, 0.0, 0.5, 0.0, 6
    );
    PolyhedralBoundedSolidEulerConstructions::translationalSweepExtrudeFacePlanar(
        solid2, solid2->findFace(1), T);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid2);

    //-----------------------------------------------------------------
    R = R.translation(c.x(), -c.y(), c.z());
    PolyhedralBoundedSolidModeler::applyTransformation(solid2, R);

    //-----------------------------------------------------------------
    glue(solid1, solid2, 8, 13);

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid1);
    PolyhedralBoundedSolidTopologyEditing::maximizeFaces(solid1);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid1);

    return solid1;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::eulerOperatorsTest()
{
    PolyhedralBoundedSolid* solid;

    //-----------------------------------------------------------------
    solid = new PolyhedralBoundedSolid();
    EulerOps::mvfs(solid, Vector3Dd(0.0, 0.0, 0.0), 1, 1);
    EulerOps::smev(solid, 1, 1, 2, Vector3Dd(1.0, 0.0, 0.0));
    EulerOps::smev(solid, 1, 2, 3, Vector3Dd(1.0, 1.0, 0.0));

    //-----------------------------------------------------------------
    //PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    java::System::out.println(solid->toString().c_str());
    return solid;
}

/**
Current method implements a simple and restricted rotational sweep (lathe)
algorithm for wires (solids with one face, and one open loop) in the z=0
plane, to be rotated about the x axis, as described in section
[MANT1988].12.3.2, and presented in program [MANT1988].12.5.
This version of the rotational sweep has some limitations and
characteristics:
  - It is the simpler form of rotational sweep, and serves as the base to
    develop complex/generalized versions of the algorithm.
  - The rotation axis is fixed to be the x-axis
  - The profile path must be open (a "wire" solid with just one face with
    one loop, which is open, with a single, connected and nonforking string
    of edges)
  - All edges must lie on the half plane [y>0,z=0], and must not touch the
    x axis.
*/
void GeneralModelsBuilder::rotationalSweepVersion1(
    PolyhedralBoundedSolid* solid, int nfaces)
{
    _PolyhedralBoundedSolidHalfEdge* first;
    _PolyhedralBoundedSolidHalfEdge* cfirst;
    _PolyhedralBoundedSolidHalfEdge* last;
    _PolyhedralBoundedSolidHalfEdge* scan = nullptr;
    Vector3Dd v;
    Matrix4x4d M;

    first = solid->getPolygonsList().get(0)->boundariesList.get(0)->boundaryStartHalfEdge;
    while ( first->parentEdge != first->next()->parentEdge ) {
        first = first->next();
    }
    last = first->next();
    while ( last->parentEdge != last->next()->parentEdge ) {
        last = last->next();
    }
    cfirst = first;
    M = Matrix4x4d();
    M = M.axisRotation( (2*M_PI) / ((double)nfaces), 1, 0, 0);

    int i;
    for ( i = 0; i < nfaces-1; i++ ) {
        v = M.multiply(cfirst->next()->startingVertex->position);
        EulerOps::lmev(solid, cfirst->next(), cfirst->next(), solid->getMaxVertexId()+1, v);
        scan = cfirst->next();
        while ( scan != last->next() ) {
            v = M.multiply(scan->previous()->startingVertex->position);
            EulerOps::lmev(solid, scan->previous(), scan->previous(), solid->getMaxVertexId()+1, v);
            EulerOps::lmef(solid, scan->previous()->previous(), scan->next(), solid->getMaxFaceId()+1);
            scan = (scan->next()->next())->mirrorHalfEdge();
        }
        last = scan;
        cfirst = (cfirst->next()->next())->mirrorHalfEdge();
    }
    EulerOps::lmef(solid, cfirst->next(), first->mirrorHalfEdge(), solid->getMaxFaceId()+1);
    while ( cfirst != scan ) {
        EulerOps::lmef(solid, cfirst, cfirst->next()->next()->next(), solid->getMaxFaceId()+1);
        cfirst = (cfirst->previous())->mirrorHalfEdge()->previous();
    }
}

/**
Current method implements a variant of simple and restricted rotational
sweep (lathe) algorithm for wires and laminas in the z=0 plane, to be
rotated about the x axis, as described in section [MANT1988].12.5, and
presented in programs [MANT1988].12.5 and [MANT1988].12.11.
*/
void GeneralModelsBuilder::rotationalSweepVersion2(
    PolyhedralBoundedSolid* solid, int nfaces)
{
    //-----------------------------------------------------------------
    _PolyhedralBoundedSolidHalfEdge* first;
    _PolyhedralBoundedSolidHalfEdge* cfirst;
    _PolyhedralBoundedSolidHalfEdge* last;
    _PolyhedralBoundedSolidHalfEdge* scan = nullptr;
    _PolyhedralBoundedSolidHalfEdge* h;
    _PolyhedralBoundedSolidFace* tailf = nullptr;
    _PolyhedralBoundedSolidFace* headf = nullptr;
    bool closedFigure = false;
    Vector3Dd v;
    Matrix4x4d M;

    //-----------------------------------------------------------------
    if ( solid->getPolygonsList().size() > 1 ) {
        // Assume it's a lamina
        closedFigure = true;
        h = solid->getPolygonsList().get(0)->boundariesList.get(0)->boundaryStartHalfEdge;
        EulerOps::lmev(solid, h, (h->mirrorHalfEdge())->next(),
                   solid->getMaxVertexId()+1, h->startingVertex->position);

        EulerOps::lkef(solid, h->previous(), (h->previous())->mirrorHalfEdge());
        headf = solid->getPolygonsList().get(0);

    }

    //-----------------------------------------------------------------
    first = solid->getPolygonsList().get(0)->boundariesList.get(0)->boundaryStartHalfEdge;
    while ( first->parentEdge != first->next()->parentEdge ) {
        first = first->next();
    }
    last = first->next();
    while ( last->parentEdge != last->next()->parentEdge ) {
        last = last->next();
    }
    cfirst = first;
    M = Matrix4x4d();
    M = M.axisRotation( (2*M_PI) / ((double)nfaces), 1, 0, 0);

    int i;
    for ( i = 0; i < nfaces-1; i++ ) {
        v = M.multiply(cfirst->next()->startingVertex->position);
        EulerOps::lmev(solid, cfirst->next(), cfirst->next(), solid->getMaxVertexId()+1, v);
        scan = cfirst->next();
        while ( scan != last->next() ) {
            v = M.multiply(scan->previous()->startingVertex->position);
            EulerOps::lmev(solid, scan->previous(), scan->previous(), solid->getMaxVertexId()+1, v);
            EulerOps::lmef(solid, scan->previous()->previous(), scan->next(), solid->getMaxFaceId()+1);
            scan = (scan->next()->next())->mirrorHalfEdge();
        }
        last = scan;
        cfirst = (cfirst->next()->next())->mirrorHalfEdge();
    }
    tailf = EulerOps::lmef(solid, cfirst->next(), first->mirrorHalfEdge(), solid->getMaxFaceId()+1);
    while ( cfirst != scan ) {
        EulerOps::lmef(solid, cfirst, cfirst->next()->next()->next(), solid->getMaxFaceId()+1);
        cfirst = (cfirst->previous())->mirrorHalfEdge()->previous();
    }

    //-----------------------------------------------------------------
    if ( closedFigure ) {
        EulerOps::lkfmrh(solid, headf, tailf);
        PolyhedralBoundedSolidTopologyEditing::loopGlue(solid, headf->id);
    }

    //-----------------------------------------------------------------
}

/**
This method builds a test solid for evaluating the second version of the
rotational sweep algorithm in a controlled way, as proposed on the example
from section [MANT1988].12.5.
The created solid is is similar to the solid shown on figure
[MANT1988].12.5. (in particular when seting 4 sides);
*/
PolyhedralBoundedSolid* GeneralModelsBuilder::createTestTorus(int nsides,
    int nrad)
{
    PolyhedralBoundedSolid* solid;
    Vector3Dd center(0.5, 0.5, 0);

    solid = PolyhedralBoundedSolidEulerConstructions::createCircularLamina(
        center.x(), center.y(), 0.2, 0.0, nsides);

    // For seting 4 sided case to be equal to figure [MANT1988].12.5.
    // an aditional rotation must be applied to the lamina prior to the
    // rotational sweep.
    Matrix4x4d T1;
    Matrix4x4d T2;
    Matrix4x4d R;
    Matrix4x4d M;
    T1 = T1.translation(center.multiply(-1));
    T2 = T2.translation(center);
    R = R.axisRotation(M_PI/((double)nsides), 0, 0, 1);
    M = T2.multiply(R.multiply(T1));
    PolyhedralBoundedSolidModeler::applyTransformation(solid, M);

    rotationalSweepVersion2(solid, nrad);
    return solid;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::rotationalSweepTest()
{
    PolyhedralBoundedSolid* solid;

    //-----------------------------------------------------------------
    solid = createTestTorus(4, 16);
    //-----------------------------------------------------------------
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);

    return solid;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::splitTest(int part)
{
    //- Basic lamina --------------------------------------------------
    PolyhedralBoundedSolid* solid =
        SimpleTestGeometryLibrary::createTestObjectMANT1986_1();

    //-----------------------------------------------------------------
    java::ArrayList<PolyhedralBoundedSolid*> solidsAbove;
    java::ArrayList<PolyhedralBoundedSolid*> solidsBelow;

    InfinitePlane sp(Vector3Dd(0, 0, 1) /*n*/,
                     Vector3Dd(0, 0, 0.30) /*p*/);

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);

    if ( part == 1 ) {
        return solid;
    }

    try {
        PolyhedralBoundedSolidModeler::split(solid, sp, solidsAbove, solidsBelow);
    }
    catch ( ... ) {
        delete solid;
        deleteSolids(solidsAbove);
        deleteSolids(solidsBelow);
        throw;
    }
    delete solid;

    //-----------------------------------------------------------------
    if ( part == 3 ) {
        solid = solidsBelow.get(0);
        solidsBelow[0] = nullptr;
    }
    else {
        solid = solidsAbove.get(0);
        solidsAbove[0] = nullptr;
    }
    deleteSolids(solidsAbove);
    deleteSolids(solidsBelow);

    PolyhedralBoundedSolidTopologyEditing::maximizeFaces(solid);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);

    return solid;
}

/**
This method builds a test sample pair of solids for evaluating
the set operations algorithm in a controlled way.
This set correspond to a simple cases for CSG operations test: two
blocks without intersecting vertex pairs (only edge/face
intersections are present). The resulting gluing face can be a variation
of the method `createTestObjectsPairMANT1986_2`, if blocks are translated
so their parallel faces don't touch; or can be one simple test case for
the complex sector intersection.
*/
java::ArrayList<PolyhedralBoundedSolid*> GeneralModelsBuilder::buildCsgTest2()
{
    java::ArrayList<PolyhedralBoundedSolid*> operands;
    PolyhedralBoundedSolid* a;
    PolyhedralBoundedSolid* b;

    //-----------------------------------------------------------------
    Matrix4x4d R;
    R = R.translation(0.5, 0.5, 0.15);

    Box box(Vector3Dd(1, 0.5, 0.3));
    a = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler::applyTransformation(a, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(a);

    //-----------------------------------------------------------------
    R = Matrix4x4d();
    R = R.translation(0.5, 0.5, 0.15+0.3);

    Box box2(Vector3Dd(0.5, 1, 0.3));
    b = box2.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler::applyTransformation(b, R);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(b);

    //-----------------------------------------------------------------
    operands.add(a);
    operands.add(b);

    return operands;
}

/// C++ port: a box solid of the given size, translated (Java: `new Box`,
/// `exportToPolyhedralBoundedSolid`, `applyTransformation`,
/// `validateIntermediate`)
static PolyhedralBoundedSolid* translatedBox(double tx, double ty, double tz,
    double sx, double sy, double sz)
{
    Matrix4x4d T;
    T = T.translation(tx, ty, tz);
    Box box(Vector3Dd(sx, sy, sz));
    PolyhedralBoundedSolid* solid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler::applyTransformation(solid, T);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

/// C++ port: `setOp` deleting its two operands
static PolyhedralBoundedSolid* setOpConsuming(PolyhedralBoundedSolid* a,
    PolyhedralBoundedSolid* b, int op)
{
    PolyhedralBoundedSolid* result;
    try {
        result = PolyhedralBoundedSolidModeler::setOp(a, b, op);
    }
    catch ( ... ) {
        delete a;
        delete b;
        throw;
    }
    delete a;
    delete b;
    return result;
}

/**
Makes a hollowed brick from two L-shaped boxes. Note that on UNION
operation this object leads to an interesting topological problem for
PolyhedralBoundedSolid.maximizeFaces operation.
*/
java::ArrayList<PolyhedralBoundedSolid*> GeneralModelsBuilder::buildCsgTest4()
{
    java::ArrayList<PolyhedralBoundedSolid*> operands;

    PolyhedralBoundedSolid* a;
    PolyhedralBoundedSolid* b;
    PolyhedralBoundedSolid* c;
    PolyhedralBoundedSolid* d;
    PolyhedralBoundedSolid* x;
    PolyhedralBoundedSolid* y;

    //-----------------------------------------------------------------
    a = translatedBox(0.5, 0.1, 0.1, 1, 0.2, 0.2);
    //-----------------------------------------------------------------
    b = translatedBox(0.5, 0.9, 0.1, 1, 0.2, 0.2);
    //-----------------------------------------------------------------
    c = translatedBox(0.1, 0.5, 0.1, 0.2, 1, 0.2);
    //-----------------------------------------------------------------
    d = translatedBox(0.9, 0.5, 0.1, 0.2, 1, 0.2);

    //-----------------------------------------------------------------
    try {
        x = setOpConsuming(b, c, PolyhedralBoundedSolidModeler::UNION);
    }
    catch ( ... ) {
        delete a;
        delete d;
        throw;
    }
    try {
        y = setOpConsuming(a, d, PolyhedralBoundedSolidModeler::UNION);
    }
    catch ( ... ) {
        delete x;
        throw;
    }

    operands.add(x);
    operands.add(y);
    return operands;
}

java::ArrayList<PolyhedralBoundedSolid*> GeneralModelsBuilder::buildCsgTest5()
{
    java::ArrayList<PolyhedralBoundedSolid*> operands;

    PolyhedralBoundedSolid* a;
    PolyhedralBoundedSolid* b;
    PolyhedralBoundedSolid* c;
    PolyhedralBoundedSolid* d;
    PolyhedralBoundedSolid* e;
    PolyhedralBoundedSolid* f;
    PolyhedralBoundedSolid* g;
    PolyhedralBoundedSolid* h;
    PolyhedralBoundedSolid* ac;

    //-----------------------------------------------------------------
    a = translatedBox(0.5, 0.1, 0.1, 1, 0.2, 0.2);
    //-----------------------------------------------------------------
    b = translatedBox(0.5, 0.9, 0.1, 1, 0.2, 0.2);
    //-----------------------------------------------------------------
    c = translatedBox(0.1, 0.5, 0.1, 0.2, 1, 0.2);
    //-----------------------------------------------------------------
    d = translatedBox(0.9, 0.5, 0.1, 0.2, 1, 0.2);
    //-----------------------------------------------------------------
    e = translatedBox(0.1, 0.5, 0.1, 0.2, 1, 0.2);
    //-----------------------------------------------------------------
    f = translatedBox(0.1, 0.5, 0.9, 0.2, 1, 0.2);
    //-----------------------------------------------------------------
    g = translatedBox(0.1, 0.1, 0.5, 0.2, 0.2, 1);
    //-----------------------------------------------------------------
    h = translatedBox(0.1, 0.9, 0.5, 0.2, 0.2, 1);

    //-----------------------------------------------------------------
    // C++ port: b, d, e, f and h are built (as in Java) but unused
    delete b;
    delete d;
    delete e;
    delete f;
    delete h;
    try {
        ac = setOpConsuming(a, c, PolyhedralBoundedSolidModeler::UNION);
    }
    catch ( ... ) {
        delete g;
        throw;
    }

    operands.add(ac);
    operands.add(g);
    return operands;
}

PolyhedralBoundedSolid* GeneralModelsBuilder::csgTest(int part,
    CsgOperationNames op,
    CsgSampleNames sample,
    bool withDebug)
{
    return csgTest(part, op, sample, withDebug, 0);
}

PolyhedralBoundedSolid* GeneralModelsBuilder::csgTest(int part,
    CsgOperationNames op,
    CsgSampleNames sample,
    bool withDebug,
    int kurlanderSingleMotifIndex)
{
    PolyhedralBoundedSolid* res = nullptr;
    java::ArrayList<PolyhedralBoundedSolid*> operands;

    java::System::out.printf("Creating C.S.G. test object with parts %d, "
        "operation %s, and sample pair %s\n", part,
        CsgOperationNamesUtil::getLabel(op), CsgSampleNamesUtil::getLabel(sample));

    operands = createCsgOperands(sample, kurlanderSingleMotifIndex);
    PolyhedralBoundedSolidStatistics::reset();

    //-----------------------------------------------------------------
    try {
        if ( op == CsgOperationNames::UNION ) {
            res = PolyhedralBoundedSolidModeler::setOp(operands[0], operands[1],
                PolyhedralBoundedSolidModeler::UNION, withDebug);
        }
        else if ( op == CsgOperationNames::INTERSECTION ) {
            res = PolyhedralBoundedSolidModeler::setOp(operands[0], operands[1],
                PolyhedralBoundedSolidModeler::INTERSECTION, withDebug);
        }
        else if ( op == CsgOperationNames::DIFFERENCE_A_MINUS_B ) {
            res = PolyhedralBoundedSolidModeler::setOp(operands[0], operands[1],
                PolyhedralBoundedSolidModeler::SUBTRACT, withDebug);
        }
        else {
            res = PolyhedralBoundedSolidModeler::setOp(operands[1], operands[0],
                PolyhedralBoundedSolidModeler::SUBTRACT, withDebug);
        }
    }
    catch ( ... ) {
        deleteSolids(operands);
        throw;
    }

    //-----------------------------------------------------------------
    PolyhedralBoundedSolid* selected;
    if ( part == 2 ) {
        selected = operands[0];
        operands[0] = nullptr;
        delete res;
    }
    else if ( part == 3 ) {
        selected = operands[1];
        operands[1] = nullptr;
        delete res;
    }
    else {
        selected = res;
    }
    deleteSolids(operands);
    return selected;
}

java::ArrayList<PolyhedralBoundedSolid*> GeneralModelsBuilder::createCsgOperands(
    CsgSampleNames sample)
{
    return createCsgOperands(sample, 0);
}

java::ArrayList<PolyhedralBoundedSolid*> GeneralModelsBuilder::createCsgOperands(
    CsgSampleNames sample,
    int kurlanderSingleMotifIndex)
{
    java::ArrayList<PolyhedralBoundedSolid*> operands;

    switch ( sample ) {
        case CsgSampleNames::MANT1986_2:
            operands =
                SimpleTestGeometryLibrary::createTestObjectPairMANT1986_2();
            break;
        case CsgSampleNames::STACKED_BLOCKS:
            operands = buildCsgTest2();
            break;
        case CsgSampleNames::MANT1988_3:
            operands =
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_3();
            break;
        case CsgSampleNames::HOLLOW_BRICK:
            operands = buildCsgTest4();
            break;
        case CsgSampleNames::CROSS_PAIR:
            operands = buildCsgTest5();
            break;
        case CsgSampleNames::MOON_BLOCK:
            operands = buildCsgMoonBlock();
            break;
        case CsgSampleNames::MANT1988_15_2_HOLED:
            operands =
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_2(-1);
            break;
        case CsgSampleNames::MANT1988_15_2_LIMIT_DIFFERENCE:
            operands =
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_2(0);
            break;
        case CsgSampleNames::MANT1988_15_2_OPEN_DIFFERENCE:
            operands =
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_2(1);
            break;
        case CsgSampleNames::MANT1988_6_13:
            operands =
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_6_13();
            break;
        case CsgSampleNames::MANT1988_15_1:
            operands =
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_1();
            break;
        case CsgSampleNames::KURLANDER_BOWL_SINGLE_MOTIF:
        default:
            operands =
                CsgKurlanderBowlFixture::createBowlAndFirstStarOperands(
                    kurlanderSingleMotifIndex);
            break;
    }

    return operands;
}

java::ArrayList<PolyhedralBoundedSolid*>
GeneralModelsBuilder::createCsgHudPreviewOperands(CsgSampleNames sample)
{
    return createCsgHudPreviewOperands(sample, 0);
}

java::ArrayList<PolyhedralBoundedSolid*>
GeneralModelsBuilder::createCsgHudPreviewOperands(
    CsgSampleNames sample,
    int kurlanderSingleMotifIndex)
{
    switch ( sample ) {
        case CsgSampleNames::MANT1986_2:
            return
                SimpleTestGeometryLibrary::createTestObjectPairMANT1986_2();
        case CsgSampleNames::MANT1988_3:
            return
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_3();
        case CsgSampleNames::MANT1988_15_2_HOLED:
            return
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_2(-1);
        case CsgSampleNames::MANT1988_15_2_LIMIT_DIFFERENCE:
            return
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_2(0);
        case CsgSampleNames::MANT1988_15_2_OPEN_DIFFERENCE:
            return
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_2(1);
        case CsgSampleNames::MANT1988_6_13:
            return
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_6_13();
        case CsgSampleNames::MANT1988_15_1:
            return
                SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_1();
        case CsgSampleNames::KURLANDER_BOWL_SINGLE_MOTIF:
            return CsgKurlanderBowlFixture::createBowlAndFirstStarOperands(
                kurlanderSingleMotifIndex);
        default:
            return createCsgOperands(sample, kurlanderSingleMotifIndex);
    }
}

PolyhedralBoundedSolid* GeneralModelsBuilder::featuredObject()
{
    return SimpleTestGeometryLibrary::createTestObjectAPPE1967_3();
}

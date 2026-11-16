#include "CsgSampleCorpusFixtures.h"

#include "PolyhedralBoundedSolidTestFixtures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

ArrayList<PolyhedralBoundedSolid*> CsgSampleCorpusFixtures::createPair(
    CsgSampleCorpus sample)
{
    switch ( sample ) {
        case HOLLOW_BRICK:
            return createHollowBrickPair();
        case MANT1986_2:
            return PolyhedralBoundedSolidTestFixtures::createMant1986_2Pair();
        case STACKED_BLOCKS:
            return createStackedBlocksPair();
        case CROSS_PAIR:
            return createCrossPair();
        case MOON_BLOCK:
            return createMoonBlockPair();
        case MANT1988_6_13:
            return PolyhedralBoundedSolidTestFixtures::createMant1988_6_13Pair();
        case MANT1988_3:
            return PolyhedralBoundedSolidTestFixtures::createMant1988_3Pair();
        case MANT1988_15_2_HOLED:
            return PolyhedralBoundedSolidTestFixtures::createMant1988_15_2Pair(-1);
        case MANT1988_15_1:
        default:
            break;
    }
    return PolyhedralBoundedSolidTestFixtures::createMant1988_15_1Pair();
}

ArrayList<PolyhedralBoundedSolid*>
CsgSampleCorpusFixtures::createMoonBlockPair()
{
    ArrayList<PolyhedralBoundedSolid*> operands;
    PolyhedralBoundedSolid* cylinderA = createTranslatedCylinder(0.5, 1.0);
    PolyhedralBoundedSolid* cylinderB = createTranslatedCylinder(0.5, 2.0);
    Matrix4x4d translation;

    translation = translation.translation(0.275, 0.0, -0.5);
    PolyhedralBoundedSolidModeler::applyTransformation(cylinderB, translation);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(cylinderA);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(cylinderB);

    operands.add(cylinderA);
    operands.add(cylinderB);
    return operands;
}

PolyhedralBoundedSolid* CsgSampleCorpusFixtures::createTranslatedCylinder(
    double radius, double height)
{
    Cone cone(radius, radius, height);
    PolyhedralBoundedSolid* solid = cone.exportToPolyhedralBoundedSolid();
    Matrix4x4d translation;

    translation = translation.translation(0.55, 0.55, 0.05);
    PolyhedralBoundedSolidModeler::applyTransformation(solid, translation);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

ArrayList<PolyhedralBoundedSolid*>
CsgSampleCorpusFixtures::createStackedBlocksPair()
{
    ArrayList<PolyhedralBoundedSolid*> operands;

    operands.add(createTranslatedBox(1.0, 0.5, 0.3, 0.5, 0.5, 0.15));
    operands.add(createTranslatedBox(0.5, 1.0, 0.3, 0.5, 0.5, 0.45));
    return operands;
}

ArrayList<PolyhedralBoundedSolid*>
CsgSampleCorpusFixtures::createHollowBrickPair()
{
    ArrayList<PolyhedralBoundedSolid*> operands;
    PolyhedralBoundedSolid* a = createTranslatedBox(1.0, 0.2, 0.2,
        0.5, 0.1, 0.1);
    PolyhedralBoundedSolid* b = createTranslatedBox(1.0, 0.2, 0.2,
        0.5, 0.9, 0.1);
    PolyhedralBoundedSolid* c = createTranslatedBox(0.2, 1.0, 0.2,
        0.1, 0.5, 0.1);
    PolyhedralBoundedSolid* d = createTranslatedBox(0.2, 1.0, 0.2,
        0.9, 0.5, 0.1);

    operands.add(PolyhedralBoundedSolidModeler::setOp(
        b, c, PolyhedralBoundedSolidModeler::UNION, FALSE));
    operands.add(PolyhedralBoundedSolidModeler::setOp(
        a, d, PolyhedralBoundedSolidModeler::UNION, FALSE));
    delete a;
    delete b;
    delete c;
    delete d;
    return operands;
}

ArrayList<PolyhedralBoundedSolid*> CsgSampleCorpusFixtures::createCrossPair()
{
    ArrayList<PolyhedralBoundedSolid*> operands;
    PolyhedralBoundedSolid* a = createTranslatedBox(1.0, 0.2, 0.2,
        0.5, 0.1, 0.1);
    PolyhedralBoundedSolid* c = createTranslatedBox(0.2, 1.0, 0.2,
        0.1, 0.5, 0.1);
    PolyhedralBoundedSolid* g = createTranslatedBox(0.2, 0.2, 1.0,
        0.1, 0.1, 0.5);

    operands.add(PolyhedralBoundedSolidModeler::setOp(
        a, c, PolyhedralBoundedSolidModeler::UNION, FALSE));
    operands.add(g);
    delete a;
    delete c;
    return operands;
}

PolyhedralBoundedSolid* CsgSampleCorpusFixtures::createTranslatedBox(
    double sx, double sy, double sz, double tx, double ty, double tz)
{
    Box box(Vector3Dd(sx, sy, sz));
    PolyhedralBoundedSolid* solid = box.exportToPolyhedralBoundedSolid();
    Matrix4x4d translation;

    translation = translation.translation(tx, ty, tz);
    PolyhedralBoundedSolidModeler::applyTransformation(solid, translation);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    return solid;
}

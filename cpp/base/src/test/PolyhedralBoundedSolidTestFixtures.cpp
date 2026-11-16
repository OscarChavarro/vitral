#include "PolyhedralBoundedSolidTestFixtures.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

typedef java::ArrayList<PolyhedralBoundedSolid*> SolidList;

PolyhedralBoundedSolid* PolyhedralBoundedSolidTestFixtures::createBoxSolid(
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

static SolidList pairOf(PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b)
{
    SolidList pair;
    pair.add(a);
    pair.add(b);
    return pair;
}

SolidList PolyhedralBoundedSolidTestFixtures::createDisjointBoxPair()
{
    PolyhedralBoundedSolid* solidA = createBoxSolid(1.0, 1.0, 1.0,
        0.0, 0.0, 0.0);
    PolyhedralBoundedSolid* solidB = createBoxSolid(1.0, 1.0, 1.0,
        4.0, 0.0, 0.0);
    return pairOf(solidA, solidB);
}

SolidList PolyhedralBoundedSolidTestFixtures::createTouchingBoxPair()
{
    PolyhedralBoundedSolid* solidA = createBoxSolid(1.0, 1.0, 1.0,
        0.0, 0.0, 0.0);
    PolyhedralBoundedSolid* solidB = createBoxSolid(1.0, 1.0, 1.0,
        1.0, 0.0, 0.0);
    return pairOf(solidA, solidB);
}

SolidList PolyhedralBoundedSolidTestFixtures::createContainmentBoxPair()
{
    PolyhedralBoundedSolid* inner = createBoxSolid(1.0, 1.0, 1.0,
        0.0, 0.0, 0.0);
    PolyhedralBoundedSolid* outer = createBoxSolid(4.0, 4.0, 4.0,
        0.0, 0.0, 0.0);
    return pairOf(inner, outer);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidTestFixtures::createMant1986_1Solid()
{
    return SimpleTestGeometryLibrary::createTestObjectMANT1986_1();
}

SolidList PolyhedralBoundedSolidTestFixtures::createMant1986_2Pair()
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1986_2();
}

SolidList PolyhedralBoundedSolidTestFixtures::createMant1988_3Pair()
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_3();
}

SolidList PolyhedralBoundedSolidTestFixtures::createMant1988_6_13Pair()
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_6_13();
}

SolidList PolyhedralBoundedSolidTestFixtures::createMant1988_15_1Pair()
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_1();
}

SolidList PolyhedralBoundedSolidTestFixtures::createMant1988_15_2LimitPair()
{
    return createMant1988_15_2Pair(0);
}

SolidList PolyhedralBoundedSolidTestFixtures::createMant1988_15_2Pair(
    int situation)
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_2(
        situation);
}

#include "PolyhedralBoundedSolidTestFixtures.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

ArrayList<PolyhedralBoundedSolid*> PolyhedralBoundedSolidTestFixtures::pairOf(
    PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b)
{
    ArrayList<PolyhedralBoundedSolid*> pair;

    pair.add(a);
    pair.add(b);
    return pair;
}

void PolyhedralBoundedSolidTestFixtures::dispose(
    ArrayList<PolyhedralBoundedSolid*>& pair)
{
    long i;

    for ( i = 0; i < pair.size(); i++ ) {
        delete pair[i];
        pair[i] = NULL;
    }
}

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

ArrayList<PolyhedralBoundedSolid*>
PolyhedralBoundedSolidTestFixtures::createDisjointBoxPair()
{
    PolyhedralBoundedSolid* solidA = createBoxSolid(1.0, 1.0, 1.0,
        0.0, 0.0, 0.0);
    PolyhedralBoundedSolid* solidB = createBoxSolid(1.0, 1.0, 1.0,
        4.0, 0.0, 0.0);
    return pairOf(solidA, solidB);
}

ArrayList<PolyhedralBoundedSolid*>
PolyhedralBoundedSolidTestFixtures::createTouchingBoxPair()
{
    PolyhedralBoundedSolid* solidA = createBoxSolid(1.0, 1.0, 1.0,
        0.0, 0.0, 0.0);
    PolyhedralBoundedSolid* solidB = createBoxSolid(1.0, 1.0, 1.0,
        1.0, 0.0, 0.0);
    return pairOf(solidA, solidB);
}

ArrayList<PolyhedralBoundedSolid*>
PolyhedralBoundedSolidTestFixtures::createContainmentBoxPair()
{
    PolyhedralBoundedSolid* inner = createBoxSolid(1.0, 1.0, 1.0,
        0.0, 0.0, 0.0);
    PolyhedralBoundedSolid* outer = createBoxSolid(4.0, 4.0, 4.0,
        0.0, 0.0, 0.0);
    return pairOf(inner, outer);
}

PolyhedralBoundedSolid*
PolyhedralBoundedSolidTestFixtures::createMant1986_1Solid()
{
    return SimpleTestGeometryLibrary::createTestObjectMANT1986_1();
}

ArrayList<PolyhedralBoundedSolid*>
PolyhedralBoundedSolidTestFixtures::createMant1986_2Pair()
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1986_2();
}

ArrayList<PolyhedralBoundedSolid*>
PolyhedralBoundedSolidTestFixtures::createMant1988_3Pair()
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_3();
}

ArrayList<PolyhedralBoundedSolid*>
PolyhedralBoundedSolidTestFixtures::createMant1988_6_13Pair()
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_6_13();
}

ArrayList<PolyhedralBoundedSolid*>
PolyhedralBoundedSolidTestFixtures::createMant1988_15_1Pair()
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_1();
}

ArrayList<PolyhedralBoundedSolid*>
PolyhedralBoundedSolidTestFixtures::createMant1988_15_2LimitPair()
{
    return createMant1988_15_2Pair(0);
}

ArrayList<PolyhedralBoundedSolid*>
PolyhedralBoundedSolidTestFixtures::createMant1988_15_2Pair(int situation)
{
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_2(
        situation);
}

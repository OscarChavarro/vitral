#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
Lamina policy (decision of 2026-11-16): a lamina, two faces back to back
with zero volume, is a valid result. The intersection of two solids that
share part of a face is returned as such a lamina, not as the regularised
empty set, because it is topologically sound and easy to display. A
collapsed lamina, whose area is below EPSILON scaled by the square of the
model scale, is not valid. Tolerances follow the size of the model, so a
small lamina is valid at its own scale; collapsed means a sliver.
C++ counterpart of Java's `LaminaPolicyTest`.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidEulerOperators Euler;

/**
A right triangle lamina with legs `length` and `height` (area
length height / 2) in the plane z = 0. With length 1 the model scale is
about 1.
*/
PolyhedralBoundedSolid* triangleLamina(double length, double height)
{
    PolyhedralBoundedSolid* solid = new PolyhedralBoundedSolid();
    Euler::mvfs(solid, Vector3Dd(0, 0, 0), 1, 1);
    Euler::smev(solid, 1, 1, 2, Vector3Dd(length, 0, 0));
    Euler::smev(solid, 1, 2, 3, Vector3Dd(0, height, 0));
    Euler::smef(solid, 1, 3, 1, 2);
    return solid;
}

}

TEST(LaminaPolicyTest, BlocksSharingPartOfAFaceIntersectingValidLamina) {
    PolyhedralBoundedSolid* a = Oracle::OrientedBox(2, 2, 1,
        Oracle::translation(0, 0, 0)).toSolid();
    PolyhedralBoundedSolid* b = Oracle::OrientedBox(1, 1, 1,
        Oracle::translation(0.5, 0.5, 1.0)).toSolid();
    PolyhedralBoundedSolid* result = Modeler::setOp(a, b,
        Modeler::INTERSECTION);
    PolyhedralBoundedSolidTopologySummary summary =
        PolyhedralBoundedSolidTopologySummary::from(result);
    EXPECT_FALSE(Modeler::isEmptyResult(result));
    EXPECT_EQ(2, result->getPolygonsList().size());
    EXPECT_EQ(1, summary.getShellCount());
    EXPECT_NEAR(0.0, Oracle::solidVolume(result), 1.0e-12);
    java::String message;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        result, &message));
    delete result;
    delete a;
    delete b;
}

TEST(LaminaPolicyTest, LaminaWithRealAreaValidatedStrictlyValid) {
    PolyhedralBoundedSolid* lamina = triangleLamina(1.0, 1.0e-2);
    java::String message;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        lamina, &message));
    delete lamina;
}

TEST(LaminaPolicyTest, CollapsedLaminaValidatedStrictlyRejected) {
    PolyhedralBoundedSolid* lamina = triangleLamina(1.0, 1.0e-7);
    java::String report;
    EXPECT_FALSE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        lamina, &report));
    // A sliver this thin has no measurable plane either; either report
    // rejects it.
    EXPECT_TRUE(report.contains("is collapsed") ||
        report.contains("was not able to compute containing plane"))
        << report.c_str();
    delete lamina;
}

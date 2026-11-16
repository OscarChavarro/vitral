// C++ port of Java's LaminaPolicyTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidValidationEngine Validation;

/**
A right triangle lamina with legs `length` and `height` (area
length height / 2) in the plane z = 0. With length 1 the model scale is
about 1.
*/
static PolyhedralBoundedSolid*
triangleLamina(double length, double height)
{
    PolyhedralBoundedSolid* solid = new PolyhedralBoundedSolid();

    PolyhedralBoundedSolidEulerOperators::mvfs(solid,
        Vector3Dd(0, 0, 0), 1, 1);
    PolyhedralBoundedSolidEulerOperators::smev(solid, 1, 1, 2,
        Vector3Dd(length, 0, 0));
    PolyhedralBoundedSolidEulerOperators::smev(solid, 1, 2, 3,
        Vector3Dd(0, height, 0));
    PolyhedralBoundedSolidEulerOperators::smef(solid, 1, 3, 1, 2);
    return solid;
}

TEST(LaminaPolicy, given_blocksSharingPartOfAFace_when_intersecting_then_validLamina)
{
    PolyhedralBoundedSolid* a = OrientedBox(2, 2, 1,
        Oracle::translation(0, 0, 0)).toSolid();
    PolyhedralBoundedSolid* b = OrientedBox(1, 1, 1,
        Oracle::translation(0.5, 0.5, 1.0)).toSolid();

    PolyhedralBoundedSolid* result = Modeler::setOp(a, b,
        Modeler::INTERSECTION);
    PolyhedralBoundedSolidTopologySummary summary =
        PolyhedralBoundedSolidTopologySummary::from(result);

    EXPECT_FALSE(Modeler::isEmptyResult(result));
    EXPECT_EQ(2, result->getPolygonsList().size());
    EXPECT_EQ(1, summary.getShellCount());
    EXPECT_NEAR(0.0, Oracle::solidVolume(result), 1.0e-12);
    String message;
    EXPECT_TRUE(Validation::validateStrict(result, &message));
    delete a;
    delete b;
    delete result;
}

TEST(LaminaPolicy, given_laminaWithRealArea_when_validatedStrictly_then_valid)
{
    PolyhedralBoundedSolid* lamina = triangleLamina(1.0, 1.0e-2);
    String message;

    EXPECT_TRUE(Validation::validateStrict(lamina, &message));
    delete lamina;
}

TEST(LaminaPolicy, given_collapsedLamina_when_validatedStrictly_then_rejected)
{
    PolyhedralBoundedSolid* lamina = triangleLamina(1.0, 1.0e-7);
    String report;

    EXPECT_FALSE(Validation::validateStrict(lamina, &report));
    // A sliver this thin has no measurable plane either; either report
    // rejects it.
    EXPECT_TRUE(report.indexOf(String("is collapsed")) >= 0 ||
        report.indexOf(String("was not able to compute containing plane")) >=
            0);
    delete lamina;
}

VITRAL_TEST_MAIN()

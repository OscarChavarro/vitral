// C++ port of Java's CsgKurlanderBowlFullModelTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

/**
The whole Kurlander bowl: a spherical shell, minus 20 moons and 20 stars cut
through its wall, clipped by a cylinder (82 booleans). Each motif makes a
hole, so the result is one closed shell of genus 40.

This replaces the reference test of `BooleansFromReferenceObjectPairsTest`
that was disabled while the bowl failed. Its builder was a stale copy of
`CsgKurlanderBowlFixture` that composed the motif placement in the
wrong order, so the motifs missed the bowl. It takes about two minutes, so
it runs in `gradle :base:slowTest`.

C++ port note: Java's @Tag("slow") is VITRAL_SLOW_TEST() (runs only with
VITRAL_SLOW_TESTS set, see VitralTest.h).

Traceability: [MANT1988] Ch. 15 (set operations), Sec. 13.3 (volume).
*/
TEST(CsgKurlanderBowlFullModel, given_kurlanderBowl_when_allMotifsAreSubtracted_then_oneValidShellOfGenus40)
{
    VITRAL_SLOW_TEST();
    PolyhedralBoundedSolid* plain = CsgKurlanderBowlFixture::createBowl();
    PolyhedralBoundedSolid* bowl = CsgKurlanderBowlFixture::create();
    PolyhedralBoundedSolidTopologySummary summary =
        PolyhedralBoundedSolidTopologySummary::from(bowl);
    double volume = PolyhedralBoundedSolidVolumeOracle::solidVolume(bowl);
    String message;

    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        bowl, &message));
    EXPECT_EQ(1, summary.getShellCount());
    // V - E + F - (L - F) = 2 - 2 genus, one hole per motif.
    EXPECT_EQ(-78, summary.getAdjustedEulerCharacteristic());
    EXPECT_EQ(1133, summary.getFaceCount());
    EXPECT_EQ(3396, summary.getEdgeCount());
    EXPECT_EQ(2186, summary.getVertexCount());
    EXPECT_NEAR(0.38391604531097, volume, 1.0e-9);
    EXPECT_LT(volume, PolyhedralBoundedSolidVolumeOracle::solidVolume(plain));
    delete bowl;
    delete plain;
}

VITRAL_TEST_MAIN()

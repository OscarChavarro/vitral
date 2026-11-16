#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
The whole Kurlander bowl: a spherical shell, minus 20 moons and 20 stars cut
through its wall, clipped by a cylinder (82 booleans). Each motif makes a
hole, so the result is one closed shell of genus 40.

This replaces the reference test of BooleansFromReferenceObjectPairsTest that
was disabled while the bowl failed.

Slow (Java `@Tag("slow")`): built into `vitral_base_slow_tests`.

Traceability: [MANT1988] Ch. 15 (set operations), Sec. 13.3 (volume).
C++ counterpart of Java's `CsgKurlanderBowlFullModelTest`.
*/
TEST(CsgKurlanderBowlFullModelTest, KurlanderBowlAllMotifsAreSubtractedOneValidShellOfGenus40) {
    PolyhedralBoundedSolid* plain = CsgKurlanderBowlFixture::createBowl();
    PolyhedralBoundedSolid* bowl = CsgKurlanderBowlFixture::create();
    PolyhedralBoundedSolidTopologySummary summary =
        PolyhedralBoundedSolidTopologySummary::from(bowl);
    double volume = PolyhedralBoundedSolidVolumeOracle::solidVolume(bowl);
    java::String message;

    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        bowl, &message)) << message.c_str();
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

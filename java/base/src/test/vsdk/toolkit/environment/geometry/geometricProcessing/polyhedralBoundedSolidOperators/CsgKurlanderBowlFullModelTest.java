package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
The whole Kurlander bowl: a spherical shell, minus 20 moons and 20 stars cut
through its wall, clipped by a cylinder (82 booleans). Each motif makes a
hole, so the result is one closed shell of genus 40.

<p>This replaces the reference test of {@code BooleansFromReferenceObjectPairsTest}
that was disabled while the bowl failed. Its builder was a stale copy of
{@link CsgKurlanderBowlFixture} that composed the motif placement in the
wrong order, so the motifs missed the bowl. It takes about two minutes, so
it runs in {@code gradle :base:slowTest}.</p>

<p>Traceability: [MANT1988] Ch. 15 (set operations), Sec. 13.3 (volume).</p>
 */
@Tag("slow")
class CsgKurlanderBowlFullModelTest
{
    @Test
    void given_kurlanderBowl_when_allMotifsAreSubtracted_then_oneValidShellOfGenus40()
    {
        PolyhedralBoundedSolid plain = CsgKurlanderBowlFixture.createBowl();
        PolyhedralBoundedSolid bowl = CsgKurlanderBowlFixture.create();
        PolyhedralBoundedSolidTopologySummary summary =
            PolyhedralBoundedSolidTopologySummary.from(bowl);
        double volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(bowl);

        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            bowl, new StringBuilder())).isTrue();
        assertThat(summary.getShellCount()).isEqualTo(1);
        // V - E + F - (L - F) = 2 - 2 genus, one hole per motif.
        assertThat(summary.getAdjustedEulerCharacteristic()).isEqualTo(-78);
        assertThat(summary.getFaceCount()).isEqualTo(1133);
        assertThat(summary.getEdgeCount()).isEqualTo(3396);
        assertThat(summary.getVertexCount()).isEqualTo(2186);
        assertThat(volume).isCloseTo(0.38391604531097, offset(1.0e-9));
        assertThat(volume).isLessThan(
            PolyhedralBoundedSolidVolumeOracle.solidVolume(plain));
    }
}

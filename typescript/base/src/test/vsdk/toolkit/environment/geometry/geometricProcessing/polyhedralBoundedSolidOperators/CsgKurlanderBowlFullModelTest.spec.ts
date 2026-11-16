import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { CsgKurlanderBowlFixture } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/**
The whole Kurlander bowl: a spherical shell, minus 20 moons and 20 stars cut
through its wall, clipped by a cylinder (82 booleans). Each motif makes a
hole, so the result is one closed shell of genus 40.

<p>This replaces the reference test of `BooleansFromReferenceObjectPairsTest`
that was disabled while the bowl failed. Its builder was a stale copy of
`CsgKurlanderBowlFixture` that composed the motif placement in the
wrong order, so the motifs missed the bowl. It takes about two minutes, so
it runs in `gradle :base:slowTest`.</p>

<p>Traceability: [MANT1988] Ch. 15 (set operations), Sec. 13.3 (volume).</p>
 */
describe("CsgKurlanderBowlFullModelTest", () => {
    // Java: @Tag("slow") — excluded from the regular `gradle test` run and
    // run by `gradle :base:slowTest`; skipped here for the same reason.
    it.skip("given_kurlanderBowl_when_allMotifsAreSubtracted_then_oneValidShellOfGenus40", () => {
        const plain = CsgKurlanderBowlFixture.createBowl();
        const bowl = CsgKurlanderBowlFixture.create();
        const summary = PolyhedralBoundedSolidTopologySummary.from(bowl);
        const volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(bowl);

        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(bowl, new StringBuilder())).toBe(true);
        expect(summary.getShellCount()).toBe(1);
        // V - E + F - (L - F) = 2 - 2 genus, one hole per motif.
        expect(summary.getAdjustedEulerCharacteristic()).toBe(-78);
        expect(summary.getFaceCount()).toBe(1133);
        expect(summary.getEdgeCount()).toBe(3396);
        expect(summary.getVertexCount()).toBe(2186);
        expect(Math.abs(volume - 0.38391604531097)).toBeLessThanOrEqual(1.0e-9);
        expect(volume).toBeLessThan(PolyhedralBoundedSolidVolumeOracle.solidVolume(plain));
    });
});

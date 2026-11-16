import { describe, expect, it } from "vitest";

import { Random } from "java/util/Random.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { InfinitePlane } from "vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { BooleanCompositeOracleFuzz, javaMapToString } from "./BooleanCompositeOracleFuzz.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";
import { PrismFixtures } from "./PrismFixtures.js";
import { SplitterOracleFuzzTestSupport } from "./SplitterOracleFuzzTestSupport.js";

/**
Seeded oracle fuzz for the splitter with planes in general position
(plan-stage11 Phase B). `SplitterOracleFuzzTest` cuts the reference
corpus with planes through its vertices; this one cuts solids the corpus
does not have, with planes through random interior points:
<ul>
<li>star prisms (N reflex edges crossed at once);</li>
<li>regular prisms of many sides;</li>
<li>L, U, stepped and frame shapes (the frame has a through hole, genus
    1), built with unions and differences of boxes and rotated.</li>
</ul>
The check is the one of `SplitterOracleFuzzTest`: the pieces conserve
the volume, each lies on its side, each is strict-valid and none is empty.
 */
describe("SplitterGeneralPlaneOracleFuzzTest", () => {
    /** First measurement (2026-11-16): 240 of 240. */
    const MINIMUM_CORRECT = 240;

    /** A value in the middle 80 % of [lo, hi]. */
    function between(random: Random, lo: number, hi: number): number {
        return lo + (0.1 + 0.8 * random.nextDouble()) * (hi - lo);
    }

    it("given_nonConvexSolidsAndGeneralPlanes_when_split_then_correctCountDoesNotRegress", () => {
        const random = new Random(77);
        const failures: string[] = [];
        const byReason = new Map<string, number>();
        let total = 0;
        let correct = 0;

        for (let s = 0; s < 30; s++) {
            let name: string;
            let base: PolyhedralBoundedSolid;
            const motion = PolyhedralBoundedSolidVolumeOracle.translation(
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
            ).multiply(
                PolyhedralBoundedSolidVolumeOracle.rotation(
                    3.0 * random.nextDouble(),
                    random.nextGaussian(),
                    random.nextGaussian(),
                    random.nextGaussian(),
                ),
            );
            if (s % 3 === 0) {
                const points = 4 + random.nextInt(4);
                name = "STAR" + points;
                base = PrismFixtures.prism(
                    PrismFixtures.starPolygon(points, 1.0, 0.4 + 0.1 * random.nextDouble()),
                    1.0,
                );
                PolyhedralBoundedSolidModeler.applyTransformation(base, motion);
            } else if (s % 3 === 1) {
                const sides = 5 + random.nextInt(28);
                name = "PRISM" + sides;
                base = PrismFixtures.prism(PrismFixtures.regularPolygon(sides, 1.0), 1.0);
                PolyhedralBoundedSolidModeler.applyTransformation(base, motion);
            } else {
                const shape = BooleanCompositeOracleFuzz.randomShape(random, 1.0).moved(motion);
                name = shape.name;
                base = shape.toSolid();
            }
            const minMax = base.getMinMax();
            for (let k = 0; k < 8; k++) {
                const n = new Vector3Dd(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
                const p = new Vector3Dd(
                    between(random, minMax[0]!, minMax[3]!),
                    between(random, minMax[1]!, minMax[4]!),
                    between(random, minMax[2]!, minMax[5]!),
                );
                if (n.length() < 1.0e-3) {
                    continue;
                }
                total++;
                const reason = SplitterOracleFuzzTestSupport.check(
                    base,
                    new InfinitePlane(n.multiply(1.0 / n.length()), p),
                );
                if (reason === null) {
                    correct++;
                } else {
                    byReason.set(reason, (byReason.get(reason) ?? 0) + 1);
                    failures.push(name + " s=" + s + " k=" + k + " " + reason);
                }
            }
        }
        // Java TreeMap: printed in key order.
        const sorted = new Map(Array.from(byReason.entries()).sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0)));
        console.log("general-plane split fuzz: " + correct + " / " + total + " " + javaMapToString(sorted));
        failures.forEach((f) => console.log("  " + f));
        expect(correct, "correct split results must not regress").toBeGreaterThanOrEqual(MINIMUM_CORRECT);
    });
});

import { describe, expect, it } from "vitest";

import { Random } from "java/util/Random.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { InfinitePlane } from "vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import { javaMapToString } from "./BooleanCompositeOracleFuzz.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { SplitterOracleFuzzTestSupport } from "./SplitterOracleFuzzTestSupport.js";

/**
Seeded oracle fuzz for the splitter (Fable5.2Analysis.md Stage E). Every
solid of the reference corpus is split by planes through its own vertices:
random normals, axis normals and diagonal normals. The L-shaped hollow brick
also gets 40 planes through its two reflex vertices, where a face is
crossed twice at the same vertex.

<p>A case is correct when the pieces conserve the volume, each piece is on
the side its list names, each is strict-valid, and no piece is empty while
the plane really cuts the solid. The count of correct cases is a ratchet:
raise it when a fix lands, never lower it.</p>

<p>When the section is pinched at a reflex vertex (two section regions
meeting at the vertex), the splitter splits the self-touching section loop
into two faces; regions that meet only at the vertex come out as separate
solids.</p>

<p>Traceability: [MANT1988] Ch. 14, Sections 14.4-14.8.</p>
 */
describe("SplitterOracleFuzzTest", () => {
    /**
    Before Stage E: 37 / 256. Stage E: 252 / 256. Pinched sections
    resolved (2026-11-16): 256 / 256.
    */
    const MINIMUM_CORRECT = 256;

    it("given_corpusSolidsAndPlanesThroughVertices_when_split_then_correctCountDoesNotRegress", () => {
        const random = new Random(7);
        const failures: string[] = [];
        const byReason = new Map<string, number>();
        const reflexPlanes: Vector3Dd[][] = [];
        const reflexVertices = [new Vector3Dd(0.2, 0.8, 0), new Vector3Dd(0.2, 0.8, 0.2)];
        let total = 0;
        let correct = 0;
        let k: number;

        for (k = 0; k < 40; k++) {
            reflexPlanes.push([
                new Vector3Dd(random.nextGaussian(), random.nextGaussian(), 0.5 * random.nextGaussian()),
                reflexVertices[k % 2]!,
            ]);
        }
        for (const sample of CsgSampleCorpus.values()) {
            const pair = CsgSampleCorpusFixtures.createPair(sample);
            for (let which = 0; which < 2; which++) {
                const name = sample + (which === 0 ? "/A" : "/B");
                const base = pair[which]!;
                const lShape = name === "HOLLOW_BRICK/A";
                const vertexCount = base.getVerticesList().size();
                for (k = 0; k < (lShape ? 52 : 12); k++) {
                    let p = base.getVerticesList().get(random.nextInt(vertexCount))!.position;
                    let n: Vector3Dd;
                    if (k >= 12) {
                        n = reflexPlanes[k - 12]![0]!;
                        p = reflexPlanes[k - 12]![1]!;
                    } else if (k % 3 === 0) {
                        n = new Vector3Dd(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
                    } else if (k % 3 === 1) {
                        const axis = random.nextInt(3);
                        n = new Vector3Dd(axis === 0 ? 1 : 0, axis === 1 ? 1 : 0, axis === 2 ? 1 : 0);
                    } else {
                        n = new Vector3Dd(random.nextInt(3) - 1, random.nextInt(3) - 1, 1);
                    }
                    if (n.length() < 1.0e-6) {
                        continue;
                    }
                    total++;
                    const reason = SplitterOracleFuzzTestSupport.check(base, new InfinitePlane(n.normalized(), p));
                    if (reason === null) {
                        correct++;
                    } else {
                        byReason.set(reason, (byReason.get(reason) ?? 0) + 1);
                        failures.push(name + " k=" + k + " " + reason + " n=" + n + " p=" + p);
                    }
                }
            }
        }
        // Java TreeMap: printed in key order.
        const sorted = new Map(Array.from(byReason.entries()).sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0)));
        console.log("split fuzz: " + correct + " / " + total + " " + javaMapToString(sorted));
        failures.forEach((f) => console.log("  " + f));
        expect(correct, "correct split results must not regress").toBeGreaterThanOrEqual(MINIMUM_CORRECT);
    });
});

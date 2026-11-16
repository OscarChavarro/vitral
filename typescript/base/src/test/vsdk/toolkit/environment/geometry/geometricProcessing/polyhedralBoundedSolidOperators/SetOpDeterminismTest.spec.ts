import { describe, expect, it } from "vitest";

import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidOperationException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.js";
import { BooleanOracleFuzz } from "./BooleanOracleFuzz.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";
import { PrismFixtures } from "./PrismFixtures.js";

/**
Determinism (plan-stage11 Phase B): the same operation on the same operands
gives the same solid, whatever ran before in the JVM. "Same" means the same
topology summary and the same vertex positions in the same order, bit for
bit; a typed failure must repeat as the same failure.

<p>The kernel keeps global counters (ids) and per-operation state; a result
that depends on them shows up here as a difference between two runs.</p>
 */
describe("SetOpDeterminismTest", () => {
    const bitsView = new DataView(new ArrayBuffer(8));

    /** Java `Double.doubleToLongBits` (NaN canonicalized), as a decimal string. */
    function doubleToLongBits(value: number): string {
        bitsView.setFloat64(0, Number.isNaN(value) ? Number.NaN : value);
        return bitsView.getBigInt64(0).toString();
    }

    function run(a: PolyhedralBoundedSolid, b: PolyhedralBoundedSolid, op: number): string {
        let result: PolyhedralBoundedSolid;
        try {
            result =
                op === BooleanOracleFuzz.B_MINUS_A
                    ? PolyhedralBoundedSolidModeler.setOp(b, a, PolyhedralBoundedSolidModeler.SUBTRACT)
                    : PolyhedralBoundedSolidModeler.setOp(a, b, op);
        } catch (e) {
            if (e instanceof PolyhedralBoundedSolidOperationException) {
                return "THROWS " + e.constructor.name;
            }
            throw e;
        }
        let text = PolyhedralBoundedSolidTopologySummary.from(result).toString();
        for (let i = 0; i < result.getVerticesList().size(); i++) {
            const p = result.getVerticesList().get(i)!.position;
            text += "\n" + doubleToLongBits(p.x()) + " " + doubleToLongBits(p.y()) + " " + doubleToLongBits(p.z());
        }
        for (let i = 0; i < result.getPolygonsList().size(); i++) {
            text += "\n" + result.getPolygonsList().get(i)!.boundariesList.size();
        }
        return text;
    }

    function fingerprint(sample: CsgSampleCorpus, op: number): string {
        const pair = CsgSampleCorpusFixtures.createPair(sample);
        return run(pair[0]!, pair[1]!, op);
    }

    function prismFingerprint(op: number): string {
        const a = PrismFixtures.prism(PrismFixtures.starPolygon(5, 1.0, 0.45), 1.0);
        const b = PrismFixtures.prism(PrismFixtures.regularPolygon(9, 0.8), 1.2);
        PolyhedralBoundedSolidModeler.applyTransformation(
            b,
            PolyhedralBoundedSolidVolumeOracle.translation(0.2, 0.1, 0.3).multiply(
                PolyhedralBoundedSolidVolumeOracle.rotation(0.6, 1, 2, 3),
            ),
        );
        return run(a, b, op);
    }

    it("given_referencePairs_when_eachOperationRunsTwice_then_resultsAreIdentical", () => {
        const differences: string[] = [];
        const values = CsgSampleCorpus.values();
        for (let ordinal = 0; ordinal < values.length; ordinal++) {
            const sample = values[ordinal]!;
            for (const op of BooleanOracleFuzz.OPERATIONS) {
                const first = fingerprint(sample, op);
                // Unrelated work in between: other operations move the
                // global id counters and leave their own state behind.
                fingerprint(values[(ordinal + 1) % values.length]!, PolyhedralBoundedSolidModeler.UNION);
                const second = fingerprint(sample, op);
                if (first !== second) {
                    differences.push(sample + " " + BooleanOracleFuzz.opName(op));
                }
            }
        }
        expect(differences, "operations whose result changed between two runs").toEqual([]);
    });

    it("given_starPrisms_when_eachOperationRunsTwice_then_resultsAreIdentical", () => {
        const differences: string[] = [];
        for (const op of BooleanOracleFuzz.OPERATIONS) {
            const first = prismFingerprint(op);
            const second = prismFingerprint(op);
            if (first !== second) {
                differences.push(BooleanOracleFuzz.opName(op));
            }
        }
        expect(differences).toEqual([]);
    });
});

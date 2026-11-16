import { describe, expect, it } from "vitest";

import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { NonManifoldBooleanResultException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.js";
import { _PolyhedralBoundedSolidSetOperator } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.js";
import { BooleanOracleFuzz, Family, Outcome } from "./BooleanOracleFuzz.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

const rotation = PolyhedralBoundedSolidVolumeOracle.rotation;
const translation = PolyhedralBoundedSolidVolumeOracle.translation;

/**
Post-connect invariant (Fable5.2Analysis.md Stage C step 1): when connect
leaves no loose ends, every null face handed to finish has exactly two loops,
and no other face carries two coincident loops. The second rule is what
defect B-01 broke: a section ring strictly inside a face must stay in that
face as a hole while the disk becomes a separate face; before the fix the
hole was moved into the disk, leaving the carrier without hole and an
annular zero-area face.

<p>Traceability: [MANT1988] Sec. 14.7 (join, laringmv), Sec. 15.6.1 (null
edges inserted as rings of the pierced face, Fig. 15.6), Sec. 15.7
(Program 15.14) and Sec. 15.8 (Program 15.15 expects two loops per null
face).</p>
 */
describe("SetOpConnectRingInvariantTest", () => {
    function corpusCases(): [CsgSampleCorpus, number][] {
        const rows: [CsgSampleCorpus, number][] = [];
        for (const sample of CsgSampleCorpus.values()) {
            for (const op of BooleanOracleFuzz.OPERATIONS) {
                rows.push([sample, op]);
            }
        }
        return rows;
    }

    function sectionRingCases(): [string, OrientedBox[], number][] {
        const peg = [
            new OrientedBox(2, 2, 2, translation(0, 0, 0)),
            new OrientedBox(0.5, 0.5, 1.5, translation(0.1, 0.2, 1.0)),
        ];
        const pegThrough = [
            new OrientedBox(2, 2, 2, translation(0, 0, 0)),
            new OrientedBox(0.5, 0.5, 3.0, translation(0.1, 0.2, 0.05).multiply(rotation(0.3, 0, 0, 1))),
        ];
        const corner = [
            new OrientedBox(4, 4, 1, translation(0.05, 0.03, 1.2)),
            new OrientedBox(
                1,
                1,
                1,
                rotation(0.37, 0, 0, 1).multiply(rotation(Math.acos(1.0 / Math.sqrt(3.0)), 1, -1, 0)),
            ),
        ];
        const rows: [string, OrientedBox[], number][] = [];
        for (const op of BooleanOracleFuzz.OPERATIONS) {
            rows.push(["peg-ending-inside-block", peg, op]);
            rows.push(["peg-through-block", pegThrough, op]);
            rows.push(["cube-corner-through-slab-face", corner, op]);
        }
        return rows;
    }

    function assertViolationsEmpty(label: string): void {
        const violations = _PolyhedralBoundedSolidSetOperator.getLastConnectInvariantViolations();
        if (violations !== null) {
            expect(violations.toArray(), label).toEqual([]);
        }
    }

    it.each(corpusCases())(
        "given_referencePair_when_setOpReachesConnect_then_ringInvariantHolds: %s op=%i",
        (sample: CsgSampleCorpus, op: number) => {
            const p = CsgSampleCorpusFixtures.createPair(sample);
            const bMinusA = op === BooleanOracleFuzz.B_MINUS_A;
            try {
                PolyhedralBoundedSolidModeler.setOp(
                    bMinusA ? p[1]! : p[0]!,
                    bMinusA ? p[0]! : p[1]!,
                    bMinusA ? PolyhedralBoundedSolidModeler.SUBTRACT : op,
                    false,
                    true,
                    false,
                );
            } catch (e) {
                // Rejected after finish (CROSS_PAIR A-B: two bars touching
                // along an edge); connect ran and its invariant still applies.
                if (!(e instanceof NonManifoldBooleanResultException)) {
                    throw e;
                }
            }
            assertViolationsEmpty(sample + " " + BooleanOracleFuzz.opName(op));
        },
    );

    it.each(sectionRingCases())(
        "given_sectionRingInsideAFace_when_setOp_then_ringInvariantHolds: %s op=%i",
        (name: string, pair: OrientedBox[], op: number) => {
            const result = BooleanOracleFuzz.run(Family.B_ROTATED_ABOUT_RANDOM_AXIS, 0, pair, op);
            expect(
                _PolyhedralBoundedSolidSetOperator.getLastConnectInvariantViolations(),
                name + " reached connect",
            ).not.toBeNull();
            assertViolationsEmpty(name + " " + BooleanOracleFuzz.opName(op));
            expect(result.outcome, result.toString()).toBe(Outcome.OK);
        },
    );

    it("given_nonCoplanarFuzzFamilies_when_setOp_then_ringInvariantHolds", () => {
        const report: string[] = [];
        for (const family of Family.values()) {
            if (family === Family.INTEGER_GRID || family === Family.COPLANAR_TOP_BOTTOM_ROTATED_45) {
                // Coplanar contacts: Stage D (defects B-02, B-05).
                continue;
            }
            const pairs = BooleanOracleFuzz.pairs(family);
            let i: number;
            for (i = 0; i < pairs.length; i++) {
                for (const op of BooleanOracleFuzz.OPERATIONS) {
                    BooleanOracleFuzz.run(family, i, pairs[i]!, op);
                    const v = _PolyhedralBoundedSolidSetOperator.getLastConnectInvariantViolations();
                    if (v !== null && !v.isEmpty()) {
                        report.push(family + "#" + i + " " + BooleanOracleFuzz.opName(op) + " " + v.toString());
                    }
                }
            }
        }
        expect(report).toEqual([]);
    });
});

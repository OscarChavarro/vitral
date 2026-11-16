import { describe, expect, it } from "vitest";

import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { _PolyhedralBoundedSolidSetOperandAligner } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperandAligner.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/**
The one decision on whether two faces coincide
(`_PolyhedralBoundedSolidSetOperandAligner`): a face of B parallel to
a face of A, next to it and closer than two tolerances is put on its plane
by translating B, before any stage of the set operation runs.

<p>The cases: inside the band B moves by exactly the gap; outside the band
and at exact contact nothing moves; two gaps along two directions are both
closed; two different gaps along one direction cannot be, and nothing
moves; the caller's operands are never touched by `setOp`.</p>
 */
describe("SetOperandAlignerTest", () => {
    const UNIT = [0, 0, 0, 1, 1, 1];

    function box(corners: number[]): PolyhedralBoundedSolid {
        return new OrientedBox(
            corners[3]! - corners[0]!,
            corners[4]! - corners[1]!,
            corners[5]! - corners[2]!,
            PolyhedralBoundedSolidVolumeOracle.translation(
                (corners[0]! + corners[3]!) / 2.0,
                (corners[1]! + corners[4]!) / 2.0,
                (corners[2]! + corners[5]!) / 2.0,
            ),
        ).toSolid();
    }

    function tolerance(): number {
        return PolyhedralBoundedSolidNumericPolicy.forSolids(box(UNIT), box([0.4, 0.3, 1, 1.4, 1.3, 1.6])).bigEpsilon();
    }

    function expectCloseTo(actual: number, expected: number, offset: number, label?: string): void {
        expect(Math.abs(actual - expected), label).toBeLessThanOrEqual(offset);
    }

    it("given_faceInsideTheBand_when_aligned_then_bMovesByTheGap", () => {
        const t = tolerance();
        for (const gap of [-1.5, -0.3, 0.4, 1.7]) {
            const a = box(UNIT);
            const b = box([0.4, 0.3, 1 + gap * t, 1.4, 1.3, 1.6 + gap * t]);

            const moved = _PolyhedralBoundedSolidSetOperandAligner.align(a, b);

            expect(moved, "gap of " + gap + " tolerances").not.toBeNull();
            expectCloseTo(moved!.x(), 0.0, 1.0e-15);
            expectCloseTo(moved!.y(), 0.0, 1.0e-15);
            expectCloseTo(moved!.z(), -gap * t, 1.0e-9 * t);
            expectCloseTo(b.getMinMax()[2]!, 1.0, 1.0e-15);
        }
    });

    it("given_faceOutsideTheBandOrInContact_when_aligned_then_nothingMoves", () => {
        const t = tolerance();
        for (const gap of [-10, -3, 0, 3, 10]) {
            const b = box([0.4, 0.3, 1 + gap * t, 1.4, 1.3, 1.6 + gap * t]);
            const before = b.getMinMax()[2];

            expect(
                _PolyhedralBoundedSolidSetOperandAligner.align(box(UNIT), b),
                "gap of " + gap + " tolerances",
            ).toBeNull();
            expect(b.getMinMax()[2]).toBe(before);
        }
    });

    /** B sits in a corner of an L of two boxes: two gaps, two directions. */
    it("given_gapsAlongTwoDirections_when_aligned_then_bothAreClosed", () => {
        const t = tolerance();
        const a = box(UNIT);
        const b = box([1 + 0.7 * t, 0.2, 0.5 - 1.2 * t, 1.8 + 0.7 * t, 0.9, 1 - 1.2 * t]);

        const moved = _PolyhedralBoundedSolidSetOperandAligner.align(a, b);

        expect(moved).not.toBeNull();
        expectCloseTo(moved!.x(), -0.7 * t, 1.0e-9 * t);
        expectCloseTo(moved!.z(), 1.2 * t, 1.0e-9 * t);
        expectCloseTo(moved!.y(), 0.0, 1.0e-15);
    });

    /**
    B stands between the top of A and the bottom of a second piece of A,
    a tolerance short of each on different amounts: no translation closes
    both gaps, so B stays where it is.
    */
    it("given_twoGapsAlongOneDirection_when_aligned_then_nothingMoves", () => {
        const t = tolerance();
        const a = PolyhedralBoundedSolidModeler.setOp(
            box(UNIT),
            box([0, 0, 2, 1, 1, 3]),
            PolyhedralBoundedSolidModeler.UNION,
        );
        const b = box([0.2, 0.2, 1 + 0.5 * t, 0.8, 0.8, 2 - 1.5 * t]);
        const before = b.getMinMax()[2];

        expect(_PolyhedralBoundedSolidSetOperandAligner.align(a, b)).toBeNull();
        expect(b.getMinMax()[2]).toBe(before);
    });

    it("given_operandsInsideTheBand_when_setOp_then_answeredAndOperandsUntouched", () => {
        const t = tolerance();
        const a = box(UNIT);
        const b = box([0.4, 0.3, 1 - 0.5 * t, 1.4, 1.3, 1.6 - 0.5 * t]);
        const bottomOfB = b.getMinMax()[2];

        const union = PolyhedralBoundedSolidModeler.setOp(a, b, PolyhedralBoundedSolidModeler.UNION);

        expectCloseTo(PolyhedralBoundedSolidVolumeOracle.solidVolume(union), 1.0 + 0.6, 2.0 * t);
        expect(b.getMinMax()[2]).toBe(bottomOfB);
    });

    /**
    B inside A with one face on a face of A: A - B is A with a pocket open
    to the outside (11 faces), not A with a cavity. The containment
    decision used to answer it with a cavity, which strict validation
    rejected.
    */
    it("given_boxInsideTouchingAFace_when_subtracted_then_resultIsAPocket", () => {
        const result = PolyhedralBoundedSolidModeler.setOp(
            box(UNIT),
            box([0.2, 0.3, 0.6, 0.7, 0.8, 1]),
            PolyhedralBoundedSolidModeler.SUBTRACT,
        );

        expectCloseTo(PolyhedralBoundedSolidVolumeOracle.solidVolume(result), 1.0 - 0.5 * 0.5 * 0.4, 1.0e-12);
        expect(result.getPolygonsList().size()).toBe(11);
        expect(PolyhedralBoundedSolidModeler.getLastSetOpResultPath()).toBe("normal-pipeline");
    });
});

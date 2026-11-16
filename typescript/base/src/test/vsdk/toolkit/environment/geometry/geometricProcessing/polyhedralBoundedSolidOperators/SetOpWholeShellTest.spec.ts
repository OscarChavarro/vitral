import { describe, expect, it } from "vitest";

import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { BooleanOracleFuzz } from "./BooleanOracleFuzz.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/**
Operands with several shells, one of which the other operand does not cross
(plan-stage11 Phase F). The finish stage moves to the result the parts that
are connected to a cut; a shell without any cut was left behind without an
error. The composite oracle found it in a chain: (A n B) u C lost a piece
of A n B that C did not touch.

<p>A is two separate unit cubes, P at the origin and Q five units away. B
crosses P only (or lies inside it). The expected volumes follow from the
boxes; the rotated run repeats them away from the axes.</p>
 */
describe("SetOpWholeShellTest", () => {
    const BOX = 1.0;

    function box(x0: number, y0: number, z0: number, x1: number, y1: number, z1: number): OrientedBox {
        return new OrientedBox(
            x1 - x0,
            y1 - y0,
            z1 - z0,
            PolyhedralBoundedSolidVolumeOracle.translation((x0 + x1) / 2.0, (y0 + y1) / 2.0, (z0 + z1) / 2.0),
        );
    }

    function place(original: OrientedBox, rotated: boolean): OrientedBox {
        if (!rotated) {
            return original;
        }
        return new OrientedBox(
            original.size[0]!,
            original.size[1]!,
            original.size[2]!,
            PolyhedralBoundedSolidVolumeOracle.translation(0.3, -0.2, 0.5)
                .multiply(PolyhedralBoundedSolidVolumeOracle.rotation(0.83, 1, 2, -1.5))
                .multiply(original.transform),
        );
    }

    function twoCubes(rotated: boolean): PolyhedralBoundedSolid {
        const both = PolyhedralBoundedSolidModeler.setOp(
            place(box(0, 0, 0, 1, 1, 1), rotated).toSolid(),
            place(box(5, 0, 0, 6, 1, 1), rotated).toSolid(),
            PolyhedralBoundedSolidModeler.UNION,
        );
        expect(PolyhedralBoundedSolidTopologySummary.from(both).getShellCount()).toBe(2);
        return both;
    }

    function assertVolume(rotated: boolean, op: number, swapped: boolean, expected: number, shells: number): void {
        const a = twoCubes(rotated);
        const b = place(box(0.2, 0.2, 0.7, 0.8, 0.8, 1.3), rotated).toSolid();
        const result = swapped
            ? PolyhedralBoundedSolidModeler.setOp(b, a, op)
            : PolyhedralBoundedSolidModeler.setOp(a, b, op);
        const label =
            (rotated ? "rotated " : "") + BooleanOracleFuzz.opName(swapped ? BooleanOracleFuzz.B_MINUS_A : op);

        expect(
            Math.abs(PolyhedralBoundedSolidVolumeOracle.solidVolume(result) - expected),
            label + " volume",
        ).toBeLessThanOrEqual(1.0e-9);
        expect(PolyhedralBoundedSolidTopologySummary.from(result).getShellCount(), label + " shells").toBe(shells);
    }

    /** B sticks out of P: half of it (0.108 of 0.216) is inside. */
    function check(rotated: boolean): void {
        const inside = 0.6 * 0.6 * 0.3;
        const b = 0.6 * 0.6 * 0.6;

        assertVolume(rotated, PolyhedralBoundedSolidModeler.UNION, false, 2 * BOX + b - inside, 2);
        assertVolume(rotated, PolyhedralBoundedSolidModeler.INTERSECTION, false, inside, 1);
        assertVolume(rotated, PolyhedralBoundedSolidModeler.SUBTRACT, false, 2 * BOX - inside, 2);
        assertVolume(rotated, PolyhedralBoundedSolidModeler.SUBTRACT, true, b - inside, 1);
    }

    it("given_twoPieceSolidCrossedInOnePiece_when_setOp_then_theOtherPieceIsKept", () => {
        check(false);
        check(true);
    });

    /**
    The void left by a box removed from inside P is a shell of its own. A
    tool that crosses only the outside of P must not lose it.
    */
    it("given_solidWithACavity_when_toolCrossesTheOuterShellOnly_then_theCavityIsKept", () => {
        const hollow = PolyhedralBoundedSolidModeler.setOp(
            box(0, 0, 0, 2, 2, 2).toSolid(),
            box(0.5, 0.5, 0.5, 1.5, 1.5, 1.5).toSolid(),
            PolyhedralBoundedSolidModeler.SUBTRACT,
        );
        expect(PolyhedralBoundedSolidTopologySummary.from(hollow).getShellCount()).toBe(2);
        const tool = box(1.7, 0.8, 0.8, 2.3, 1.2, 1.2).toSolid();

        const cut = PolyhedralBoundedSolidModeler.setOp(hollow, tool, PolyhedralBoundedSolidModeler.SUBTRACT);

        expect(
            Math.abs(PolyhedralBoundedSolidVolumeOracle.solidVolume(cut) - (8.0 - 1.0 - 0.3 * 0.4 * 0.4)),
        ).toBeLessThanOrEqual(1.0e-9);
        expect(PolyhedralBoundedSolidTopologySummary.from(cut).getShellCount()).toBe(2);
    });
});

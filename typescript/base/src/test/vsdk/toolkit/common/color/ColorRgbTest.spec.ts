// Covers the operations added to cpp/base/src/main/vsdk/toolkit/common/color/ColorRgb.h
// (the C++ port has no unit test for them). Every operation returns a new
// color and leaves its operands untouched.
import { describe, expect, it } from "vitest";

import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { VSDK } from "vsdk/toolkit/common/VSDK.js";

describe("ColorRgbTest", () => {
    function expectColor(c: ColorRgb, r: number, g: number, b: number): void {
        expect(c.getR()).toBe(r);
        expect(c.getG()).toBe(g);
        expect(c.getB()).toBe(b);
    }

    const a = new ColorRgb(0.5, -0.25, 2.0);
    const b = new ColorRgb(0.25, 0.5, -1.0);

    it("given_twoColors_when_combinedComponentWise_then_returnsNewColors", () => {
        expectColor(a.add(b), 0.75, 0.25, 1.0);
        expectColor(a.subtract(b), 0.25, -0.75, 3.0);
        expectColor(a.addScaled(2.0, b), 1.0, 0.75, 0.0);
        expectColor(a.multiply(2.0), 1.0, -0.5, 4.0);
        expectColor(a.multiply(b), 0.125, -0.125, -2.0);
        expectColor(a.minimum(b), 0.25, -0.25, -1.0);
        expectColor(a.maximum(b), 0.5, 0.5, 2.0);
        expectColor(a.absolute(), 0.5, 0.25, 2.0);
        // Operands are untouched
        expectColor(a, 0.5, -0.25, 2.0);
        expectColor(b, 0.25, 0.5, -1.0);
    });

    it("given_zeroDivisors_when_divided_then_thoseComponentsAreUnchanged", () => {
        expectColor(a.divide(new ColorRgb(2.0, 0.0, 4.0)), 0.25, -0.25, 0.5);
        expectColor(a.scaleInverse(4.0), 0.125, -0.0625, 0.5);
        expectColor(a.scaleInverse(0.0), 0.5, -0.25, 2.0);
        // scaleInverse multiplies by the reciprocal: 0.3 * (1 / 7) differs
        // from 0.3 / 7 in the last bit
        expect(new ColorRgb(0.3, 0, 0).scaleInverse(7.0).getR()).toBe((1.0 / 7.0) * 0.3);
        expect(new ColorRgb(0.3, 0, 0).scaleInverse(7.0).getR()).not.toBe(0.3 / 7.0);
    });

    it("given_color_when_reduced_then_returnsScalars", () => {
        expect(a.sumAbsComponents()).toBe(2.75);
        expect(a.maximumComponent()).toBe(2.0);
        expect(b.maximumComponent()).toBe(0.5);
        expect(new ColorRgb(3, 2, 1).maximumComponent()).toBe(3);
        expect(a.average()).toBe(2.25 / 3.0);
        expect(a.distance(b)).toBe(Math.sqrt(0.0625 + 0.5625 + 9.0));
        expect(ColorRgb.distance(a, b)).toBe(a.distance(b));
    });

    it("given_nearlyBlackColor_when_tested_then_isBlackOnlyInsideEpsilon", () => {
        expect(new ColorRgb().isBlack()).toBe(true);
        expect(new ColorRgb(VSDK.EPSILON / 2, -VSDK.EPSILON / 2, 0).isBlack()).toBe(true);
        expect(new ColorRgb(VSDK.EPSILON, 0, 0).isBlack()).toBe(false);
        expect(new ColorRgb(0, 0, -VSDK.EPSILON).isBlack()).toBe(false);
    });
});

import { describe, expect, it } from "vitest";
import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { SimpleBackground } from "vsdk/toolkit/environment/background/SimpleBackground.js";

describe("SimpleBackground", () => {
    it("starts black and returns independent colour values", () => {
        const background = new SimpleBackground();
        const first = background.colorInDireccion(new Vector3Dd(0, 0, 1));
        const second = background.colorInDireccion(new Vector3Dd(1, 2, 3));

        expect(first).toEqual(new ColorRgb(0, 0, 0));
        expect(second).toEqual(new ColorRgb(0, 0, 0));
        expect(first).not.toBe(second);
    });

    it("uses the configured colour regardless of direction", () => {
        const background = new SimpleBackground();
        background.setColor(0.125, 0.5, 0.875);

        expect(background.colorInDireccion(new Vector3Dd(-4, 3, 2))).toEqual(new ColorRgb(0.125, 0.5, 0.875));
    });
});

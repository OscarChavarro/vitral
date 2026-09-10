import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
describe("Matrix4x4d", () =>
    it("translates, scales and computes determinant", () => {
        const m = new Matrix4x4d().translation(5, -2, 1.5);
        expect(m.multiply(new Vector3Dd(1, 2, 3)).epsilonEquals(new Vector3Dd(6, 0, 4.5), 1e-9)).toBe(true);
        expect(new Matrix4x4d().scale(2, 3, 4).determinant()).toBeCloseTo(24);
    }));

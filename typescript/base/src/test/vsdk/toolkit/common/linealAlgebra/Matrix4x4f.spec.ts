import { describe, expect, it } from "vitest";
import { Matrix4x4f } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4f.js";
import { Vector3Df } from "vsdk/toolkit/common/linealAlgebra/Vector3Df.js";
describe("Matrix4x4f", () =>
    it("translates vectors", () => {
        const m = new Matrix4x4f().translation(5, -2, 1.5);
        expect(m.multiply(new Vector3Df(1, 2, 3)).epsilonEquals(new Vector3Df(6, 0, 4.5), 1e-5)).toBe(true);
    }));

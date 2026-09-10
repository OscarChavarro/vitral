import { describe, expect, it } from "vitest";
import { Vector4Dd } from "vsdk/toolkit/common/linealAlgebra/Vector4Dd.js";
describe("Vector4Dd", () =>
    it("divides homogeneous coordinates", () =>
        expect(new Vector4Dd(4, 6, 8, 2).dividedByW().epsilonEquals(new Vector4Dd(2, 3, 4, 1), 1e-12)).toBe(true)));

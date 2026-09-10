import { describe, expect, it } from "vitest";
import { Vector4Df } from "vsdk/toolkit/common/linealAlgebra/Vector4Df.js";
describe("Vector4Df", () =>
    it("divides homogeneous coordinates", () =>
        expect(new Vector4Df(4, 6, 8, 2).dividedByW().epsilonEquals(new Vector4Df(2, 3, 4, 1), 1e-6)).toBe(true)));

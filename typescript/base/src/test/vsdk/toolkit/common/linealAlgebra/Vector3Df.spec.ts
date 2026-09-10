import { describe, expect, it } from "vitest";
import { Vector3Df } from "vsdk/toolkit/common/linealAlgebra/Vector3Df.js";
describe("Vector3Df", () =>
    it("computes cross product", () => {
        expect(
            new Vector3Df(1, 0, 0).crossProduct(new Vector3Df(0, 1, 0)).epsilonEquals(new Vector3Df(0, 0, 1), 1e-6),
        ).toBe(true);
    }));

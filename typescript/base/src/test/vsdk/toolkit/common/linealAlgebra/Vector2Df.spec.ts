import { describe, expect, it } from "vitest";
import { Vector2Df } from "vsdk/toolkit/common/linealAlgebra/Vector2Df.js";
describe("Vector2Df", () =>
    it("adds vectors", () => {
        const a = new Vector2Df(1.5, -2),
            b = new Vector2Df(-0.5, 3);
        expect(a.add(b).epsilonEquals(new Vector2Df(1, 1), 1e-6)).toBe(true);
    }));

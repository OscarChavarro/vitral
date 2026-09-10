import { describe, expect, it } from "vitest";
import { Vector2Dd } from "vsdk/toolkit/common/linealAlgebra/Vector2Dd.js";
describe("Vector2Dd", () =>
    it("adds and rejects invalid epsilon", () => {
        const a = new Vector2Dd(1.5, -2),
            b = new Vector2Dd(-0.5, 3);
        expect(a.add(b).epsilonEquals(new Vector2Dd(1, 1), 1e-9)).toBe(true);
        expect(() => a.epsilonEquals(b, -1)).toThrow();
    }));

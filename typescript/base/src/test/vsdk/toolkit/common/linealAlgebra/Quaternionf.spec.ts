import { describe, expect, it } from "vitest";
import { Quaternionf } from "vsdk/toolkit/common/linealAlgebra/Quaternionf.js";
import { Vector3Df } from "vsdk/toolkit/common/linealAlgebra/Vector3Df.js";
describe("Quaternionf", () =>
    it("rotates vectors", () => {
        const q = new Quaternionf(new Vector3Df(0, 0, Math.fround(Math.sin(Math.PI / 4))), Math.cos(Math.PI / 4));
        expect(q.rotate(new Vector3Df(1, 0, 0)).epsilonEquals(new Vector3Df(0, 1, 0), 1e-5)).toBe(true);
    }));

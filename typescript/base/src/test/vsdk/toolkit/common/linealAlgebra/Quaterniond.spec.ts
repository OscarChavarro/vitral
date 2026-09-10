import { describe, expect, it } from "vitest";
import { Quaterniond } from "vsdk/toolkit/common/linealAlgebra/Quaterniond.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
describe("Quaterniond", () =>
    it("rotates vectors", () => {
        const q = new Quaterniond(new Vector3Dd(0, 0, Math.sin(Math.PI / 4)), Math.cos(Math.PI / 4));
        expect(q.rotate(new Vector3Dd(1, 0, 0)).epsilonEquals(new Vector3Dd(0, 1, 0), 1e-8)).toBe(true);
    }));

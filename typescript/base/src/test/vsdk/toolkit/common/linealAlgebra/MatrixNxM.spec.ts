import { describe, expect, it } from "vitest";
import { MatrixNxM } from "vsdk/toolkit/common/linealAlgebra/MatrixNxM.js";
describe("MatrixNxM", () =>
    it("computes determinant and inverse", () => {
        const m = new MatrixNxM(2, 2).withVal(0, 0, 4).withVal(0, 1, 7).withVal(1, 0, 2).withVal(1, 1, 6);
        expect(m.determinant()).toBeCloseTo(10);
        expect(m.multiply(m.inverse()).epsilonEquals(new MatrixNxM(2, 2), 1e-8)).toBe(true);
    }));

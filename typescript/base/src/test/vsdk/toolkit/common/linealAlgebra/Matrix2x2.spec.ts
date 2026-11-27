import { describe, expect, it } from "vitest";
import { Matrix2x2 } from "vsdk/toolkit/common/linealAlgebra/Matrix2x2.js";
import { Vector2Dd } from "vsdk/toolkit/common/linealAlgebra/Vector2Dd.js";
import { Matrix2x2Printer } from "vsdk/toolkit/io/wrapper/Matrix2x2Printer.js";
describe("Matrix2x2", () => {
    it("identity and accessors", () => {
        const identity = new Matrix2x2();
        expect(identity.get(0, 0)).toBe(1.0);
        expect(identity.get(0, 1)).toBe(0.0);
        expect(identity.get(1, 0)).toBe(0.0);
        expect(identity.get(1, 1)).toBe(1.0);
        expect(identity.getTranslation(0)).toBe(0.0);
        const m = new Matrix2x2(0.5, 0.25, -1.0, 2.0, 3.0, -4.0);
        expect(m.get(1, 0)).toBe(-1.0);
        expect(m.getTranslation(1)).toBe(-4.0);
    });
    it("transform point", () => {
        const m = new Matrix2x2(0.5, 0.0, 0.0, 0.5, 0.5, 0.0);
        const p = m.transformPoint(new Vector2Dd(1.0, 1.0));
        expect(p.x).toBe(1.0);
        expect(p.y).toBe(0.5);
        const g = new Matrix2x2(0.3, 0.7, 1.1, 1.3, 0.2, 0.9);
        const q = g.transformPoint(new Vector2Dd(0.6, 0.1));
        expect(q.x).toBe(0.3 * 0.6 + 0.7 * 0.1 + 0.2);
        expect(q.y).toBe(1.1 * 0.6 + 1.3 * 0.1 + 0.9);
    });
    it("multiply composes", () => {
        const a = new Matrix2x2(0.5, 0.0, 0.0, 0.5, 0.5, 0.0);
        const b = new Matrix2x2(0.5, 0.0, 0.0, 0.5, 0.0, 0.5);
        const p = new Vector2Dd(0.3, 0.6);
        const composed = a.multiply(b).transformPoint(p);
        const stepwise = a.transformPoint(b.transformPoint(p));
        expect(composed.x).toBe(stepwise.x);
        expect(composed.y).toBe(stepwise.y);
        expect(new Matrix2x2().multiply(a).equals(a)).toBe(true);
    });
});
describe("Matrix2x2Printer", () => {
    it("null stream is ignored", () => {
        Matrix2x2Printer.print(new Matrix2x2(), null);
    });
});

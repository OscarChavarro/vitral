import { describe, expect, it } from "vitest";
import { Matrix2x2 } from "vsdk/toolkit/common/linealAlgebra/Matrix2x2.js";
import { Vector2Df } from "vsdk/toolkit/common/linealAlgebra/Vector2Df.js";
import { Matrix2x2Printer } from "vsdk/toolkit/io/wrapper/Matrix2x2Printer.js";
const f = Math.fround;
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
        const p = m.transformPoint(new Vector2Df(1.0, 1.0));
        expect(p.x()).toBe(1.0);
        expect(p.y()).toBe(0.5);
        const g = new Matrix2x2(0.3, 0.7, 1.1, 1.3, 0.2, 0.9);
        const q = g.transformPoint(new Vector2Df(0.6, 0.1));
        expect(q.x()).toBe(f(f(f(f(0.3) * f(0.6)) + f(f(0.7) * f(0.1))) + f(0.2)));
        expect(q.y()).toBe(f(f(f(f(1.1) * f(0.6)) + f(f(1.3) * f(0.1))) + f(0.9)));
    });
    it("multiply composes", () => {
        const a = new Matrix2x2(0.5, 0.0, 0.0, 0.5, 0.5, 0.0);
        const b = new Matrix2x2(0.5, 0.0, 0.0, 0.5, 0.0, 0.5);
        const p = new Vector2Df(0.3, 0.6);
        const composed = a.multiply(b).transformPoint(p);
        const stepwise = a.transformPoint(b.transformPoint(p));
        expect(composed.x()).toBe(stepwise.x());
        expect(composed.y()).toBe(stepwise.y());
        expect(new Matrix2x2().multiply(a).equals(a)).toBe(true);
    });
});
describe("Matrix2x2Printer", () => {
    it("null stream is ignored", () => {
        Matrix2x2Printer.print(new Matrix2x2(), null);
    });
});

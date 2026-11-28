import { describe, expect, it } from "vitest";
import { Double } from "java/lang/Double.js";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";

describe("Double.parseDouble", () => {
    it("accepts Java literals and rounds hexadecimal significands once", () => {
        expect(Double.parseDouble(" -0x1.8p1f ")).toBe(-3);
        expect(Double.parseDouble("0x.8p1")).toBe(1);
        expect(Double.parseDouble("0x1.00000000000008p0")).toBe(1);
        expect(Double.parseDouble("0x1.00000000000008001p0")).toBe(1 + Number.EPSILON);
        expect(Double.parseDouble("0x1p-1074")).toBe(Number.MIN_VALUE);
        expect(Double.parseDouble("-0x1p-1075")).toBe(-0);
        expect(Double.parseDouble("0x1.fffffffffffffp1023")).toBe(Number.MAX_VALUE);
        expect(Double.parseDouble("0x1.fffffffffffff8p1023")).toBe(Infinity);
        expect(Double.parseDouble("+NaN")).toBeNaN();
        expect(Double.parseDouble("-1.2e3D")).toBe(-1200);
        for (const text of ["f", "0x12", "0b11", "0o7", "1_000", "\u00a01"]) {
            expect(() => Double.parseDouble(text)).toThrow(IllegalArgumentException);
        }
    });
});

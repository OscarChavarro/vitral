import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Matrix4x4f } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4f.js";
import { Vector3Df } from "vsdk/toolkit/common/linealAlgebra/Vector3Df.js";
import { Vector4Df } from "vsdk/toolkit/common/linealAlgebra/Vector4Df.js";
import { expectNear } from "../../../../GTestAssertions.js";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { IndexOutOfBoundsException } from "java/lang/IndexOutOfBoundsException.js";
const f = Math.fround;
describe("Matrix4x4f", () => {
    it("translates vectors", () => {
        const m = new Matrix4x4f().translation(5, -2, 1.5);
        expect(m.multiply(new Vector3Df(1, 2, 3)).epsilonEquals(new Vector3Df(6, 0, 4.5), 1e-5)).toBe(true);
    });
    it("stores floats exactly", () => {
        // 0.1f is not a double-rounded value: it must come back bit for bit
        const values = [
            [0.1, 0.2, 0.3, 0.4],
            [1.0 / 3.0, 2.0, 3.0, 4.0],
            [5.0, 6.0, 7.0, 8.0],
            [9.0, 10.0, 11.0, 12.0],
        ].map((row) => row.map(f));
        const m = new Matrix4x4f(values);
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                expect(m.get(i, j)).toBe(values[i]![j]!);
            }
        }
        expect(m.withVal(2, 1, 0.7).get(2, 1)).toBe(f(0.7));
    });
    it("products compute in float from the first term", () => {
        const a = [
            [0.1, 0.2, 0.3, 0.4],
            [0.5, 0.6, 0.7, 0.8],
            [0.9, 1.1, 1.2, 1.3],
            [1.4, 1.5, 1.6, 1.7],
        ].map((row) => row.map(f));
        const b = [
            [1.7, 0.3, 0.11, 2.0],
            [0.13, 1.9, 0.4, 0.5],
            [0.6, 0.23, 1.3, 0.7],
            [0.8, 0.9, 0.31, 1.0],
        ].map((row) => row.map(f));
        const p = new Matrix4x4f(a).multiply(new Matrix4x4f(b));
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                const expected = f(
                    f(f(f(a[i]![0]! * b[0]![j]!) + f(a[i]![1]! * b[1]![j]!)) + f(a[i]![2]! * b[2]![j]!)) +
                        f(a[i]![3]! * b[3]![j]!),
                );
                expect(p.get(i, j)).toBe(expected);
            }
        }
        const v = new Matrix4x4f(a).multiply(new Vector4Df(0.3, -1.1, 2.5, 1.0));
        expect(v.x()).toBe(
            f(f(f(f(a[0]![0]! * f(0.3)) + f(a[0]![1]! * f(-1.1))) + f(a[0]![2]! * f(2.5))) + f(a[0]![3]! * f(1.0))),
        );
        expect(v.w()).toBe(
            f(f(f(f(a[3]![0]! * f(0.3)) + f(a[3]![1]! * f(-1.1))) + f(a[3]![2]! * f(2.5))) + f(a[3]![3]! * f(1.0))),
        );
    });
    it("rotation, projection and inverse", () => {
        const r = new Matrix4x4f().axisRotation(f(Math.PI / 2.0), 0, 0, 1);
        expect(r.multiply(new Vector3Df(1, 0, 0)).epsilonEquals(new Vector3Df(0, 1, 0), 1e-6)).toBe(true);
        expect(r.multiply(r.transpose()).epsilonEquals(new Matrix4x4f(), 1e-6)).toBe(true);

        const o = new Matrix4x4f().orthogonalProjection(-2, 2, -1, 1, 1, 10);
        const od = new Matrix4x4d().orthogonalProjection(-2, 2, -1, 1, 1, 10);
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                expectNear(o.get(i, j), od.get(i, j), 1e-6);
            }
        }

        const m = new Matrix4x4f().axisRotation(0.3, 1, 2, 3).multiply(new Matrix4x4f().translation(1, -2, 0.5));
        expect(m.multiply(m.invert()).epsilonEquals(new Matrix4x4f(), 1e-5)).toBe(true);
        expectNear(m.determinant(), 1.0, f(1e-5));
        expect(
            m
                .withoutTranslation()
                .extractTranslation()
                .equals(new Vector3Df(0, 0, 0)),
        ).toBe(true);
    });
    it("quaternion round trip", () => {
        const r = new Matrix4x4f().axisRotation(0.7, 0.0, 1.0, 0.0);
        expect(r.importFromQuaternion(r.exportToQuaternion()).epsilonEquals(r, 1e-5)).toBe(true);
    });

    it("uses Java float-array equality and hashing", () => {
        const one = new Matrix4x4f();
        const nan = one.withVal(0, 1, NaN);
        expect(nan.equals(one.withVal(0, 1, NaN))).toBe(true);
        expect(nan.hashCode()).toBe(one.withVal(0, 1, NaN).hashCode());
        expect(one.equals(one.withVal(0, 1, -0))).toBe(false);
        expect(() => one.epsilonEquals(one, -1)).toThrow(IllegalArgumentException);
        expect(() => one.get(4, 0)).toThrow(IndexOutOfBoundsException);
    });
});

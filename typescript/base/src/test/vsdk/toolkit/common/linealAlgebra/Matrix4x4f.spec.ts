// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/Matrix4x4fTest.java
import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Matrix4x4f } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4f.js";
import { Vector3Df } from "vsdk/toolkit/common/linealAlgebra/Vector3Df.js";
import { Vector4Df } from "vsdk/toolkit/common/linealAlgebra/Vector4Df.js";
import { expectNear } from "../../../../GTestAssertions.js";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";

const f = Math.fround;
const EPS = f(1.0e-5);

describe("Matrix4x4fTest", () => {
    const translationCases = (): [Matrix4x4f, Vector3Df, Vector3Df][] => [
        [new Matrix4x4f().translation(5.0, -2.0, 1.5), new Vector3Df(1.0, 2.0, 3.0), new Vector3Df(6.0, 0.0, 4.5)],
        [
            new Matrix4x4f().translation(0.0, 0.0, 0.0),
            new Vector3Df(f(1.0e-5), f(-1.0e-5), 0.0),
            new Vector3Df(f(1.0e-5), f(-1.0e-5), 0.0),
        ],
    ];

    it.each(translationCases())(
        "given_translationMatrix_when_multiplyingPoint_then_coordinatesAreTranslated %#",
        (translation, point, expected) => {
            const moved = translation.multiply(point);
            expect(moved.epsilonEquals(expected, f(1.0e-4))).toBe(true);
        },
    );

    const axisRotationCases = (): [number, Vector3Df, Vector3Df, Vector3Df][] => [
        [f(Math.PI / 2.0), new Vector3Df(0.0, 0.0, 1.0), new Vector3Df(1.0, 0.0, 0.0), new Vector3Df(0.0, 1.0, 0.0)],
        [f(Math.PI / 3.0), new Vector3Df(0.0, 0.0, 0.0), new Vector3Df(2.0, -1.0, 0.5), new Vector3Df(2.0, -1.0, 0.5)],
    ];

    it.each(axisRotationCases())(
        "given_axisRotation_when_axisIsDegenerateOrValid_then_itBehavesAsExpected %#",
        (angle, axis, input, expected) => {
            const rotation = new Matrix4x4f().axisRotation(angle, axis);
            const result = rotation.multiply(input);
            expect(result.epsilonEquals(expected, f(1.0e-4))).toBe(true);
        },
    );

    it("given_rotationMatrix_when_convertingToQuaternionAndBack_then_rotationStaysEquivalent", () => {
        const rotation = new Matrix4x4f().axisRotation(f(Math.PI / 3.0), 0.0, 0.0, 1.0);

        const q = rotation.exportToQuaternion().normalized();
        const rebuilt = new Matrix4x4f().importFromQuaternion(q);

        const ref = new Vector3Df(1.0, 0.0, 0.0);
        const r1 = rotation.multiply(ref);
        const r2 = rebuilt.multiply(ref);

        expect(r2.epsilonEquals(r1, f(1.0e-4))).toBe(true);
    });

    it("given_affineMatrix_when_inverting_then_productIsIdentity", () => {
        const matrix = new Matrix4x4f().scale(2.0, 3.0, 4.0).multiply(new Matrix4x4f().translation(5.0, -1.0, 2.0));

        const inverse = matrix.invert();
        const identity = matrix.multiply(inverse);

        expect(identity.epsilonEquals(new Matrix4x4f(), f(1.0e-4))).toBe(true);
    });

    it("given_matrix_when_withoutTranslation_then_translationIsRemovedOnly", () => {
        const matrix = new Matrix4x4f().translation(3.0, -2.0, 5.0).multiply(new Matrix4x4f().scale(2.0, 2.0, 2.0));

        const noTranslation = matrix.withoutTranslation();

        expect(noTranslation.extractTranslation().epsilonEquals(new Vector3Df(0.0, 0.0, 0.0), EPS)).toBe(true);
    });

    it("given_closeMatrices_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Matrix4x4f();
        const b = a.withVal(0, 1, f(1.0e-4));

        expect(a.epsilonEquals(b, f(1.0e-3))).toBe(true);
        expect(a.epsilonEquals(b, f(1.0e-6))).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });

    it("given_identityMatrix_when_multiplied_then_itActsAsNeutralElement", () => {
        const identity = Matrix4x4f.identityMatrix();
        const matrix = new Matrix4x4f().translation(2.0, -3.0, 1.0).multiply(new Matrix4x4f().scale(1.5, 2.0, 0.5));

        expect(identity.multiply(matrix).epsilonEquals(matrix, EPS)).toBe(true);
        expect(matrix.multiply(identity).epsilonEquals(matrix, EPS)).toBe(true);
    });

    it("given_identityAndScale_when_checkingDeterminant_then_valuesAreExpected", () => {
        const identity = Matrix4x4f.identityMatrix();
        const scale = new Matrix4x4f().scale(2.0, 3.0, 4.0);

        expect(Math.abs(identity.determinant() - 1.0)).toBeLessThanOrEqual(EPS);
        expect(Math.abs(scale.determinant() - 24.0)).toBeLessThanOrEqual(f(1.0e-3));
    });

    it("given_anyMatrix_when_transposedTwice_then_originalIsRecovered", () => {
        const matrix = new Matrix4x4f().withVal(0, 1, 2.0).withVal(1, 2, -3.0).withVal(2, 3, 4.0).withVal(3, 0, -1.0);

        const rebuilt = matrix.transpose().transpose();

        expect(rebuilt.epsilonEquals(matrix, EPS)).toBe(true);
    });

    // Ports of the Matrix4x4fTest cases of `cpp/base/src/test/matrix4x4_tests.cpp`;
    // each test method is named `<cpp suite>_<cpp test>`.

    it("Matrix4x4fTest_StoresFloatsExactly", () => {
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
    it("Matrix4x4fTest_ProductsComputeInFloatFromTheFirstTerm", () => {
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
    it("Matrix4x4fTest_RotationProjectionAndInverse", () => {
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
    it("Matrix4x4fTest_QuaternionRoundTrip", () => {
        const r = new Matrix4x4f().axisRotation(0.7, 0.0, 1.0, 0.0);
        expect(r.importFromQuaternion(r.exportToQuaternion()).epsilonEquals(r, 1e-5)).toBe(true);
    });
});

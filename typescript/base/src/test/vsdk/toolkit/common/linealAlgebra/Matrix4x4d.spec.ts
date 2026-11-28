// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/Matrix4x4Test.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

const EPS = 1.0e-9;

describe("Matrix4x4Test", () => {
    const translationCases = (): [Matrix4x4d, Vector3Dd, Vector3Dd][] => [
        [new Matrix4x4d().translation(5.0, -2.0, 1.5), new Vector3Dd(1.0, 2.0, 3.0), new Vector3Dd(6.0, 0.0, 4.5)],
        [
            new Matrix4x4d().translation(0.0, 0.0, 0.0),
            new Vector3Dd(1.0e-12, -1.0e-12, 0.0),
            new Vector3Dd(1.0e-12, -1.0e-12, 0.0),
        ],
    ];

    it.each(translationCases())(
        "given_translationMatrix_when_multiplyingPoint_then_coordinatesAreTranslated %#",
        (translation, point, expected) => {
            const moved = translation.multiply(point);
            expect(moved.epsilonEquals(expected, 1.0e-9)).toBe(true);
        },
    );

    const axisRotationCases = (): [number, Vector3Dd, Vector3Dd, Vector3Dd][] => [
        [Math.PI / 2.0, new Vector3Dd(0.0, 0.0, 1.0), new Vector3Dd(1.0, 0.0, 0.0), new Vector3Dd(0.0, 1.0, 0.0)],
        [Math.PI / 3.0, new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(2.0, -1.0, 0.5), new Vector3Dd(2.0, -1.0, 0.5)],
    ];

    it.each(axisRotationCases())(
        "given_axisRotation_when_axisIsDegenerateOrValid_then_itBehavesAsExpected %#",
        (angle, axis, input, expected) => {
            const rotation = new Matrix4x4d().axisRotation(angle, axis);
            const result = rotation.multiply(input);
            expect(result.epsilonEquals(expected, 1.0e-8)).toBe(true);
        },
    );

    it("given_rotationMatrix_when_convertingToQuaternionAndBack_then_rotationStaysEquivalent", () => {
        const rotation = new Matrix4x4d().axisRotation(Math.PI / 3.0, 0.0, 0.0, 1.0);

        const q = rotation.exportToQuaternion().normalized();
        const rebuilt = new Matrix4x4d().importFromQuaternion(q);

        const ref = new Vector3Dd(1.0, 0.0, 0.0);
        const r1 = rotation.multiply(ref);
        const r2 = rebuilt.multiply(ref);

        expect(r2.epsilonEquals(r1, 1.0e-6)).toBe(true);
    });

    it("given_affineMatrix_when_inverting_then_productIsIdentity", () => {
        const matrix = new Matrix4x4d().scale(2.0, 3.0, 4.0).multiply(new Matrix4x4d().translation(5.0, -1.0, 2.0));

        const inverse = matrix.invert();
        const identity = matrix.multiply(inverse);

        expect(identity.epsilonEquals(new Matrix4x4d(), 1.0e-6)).toBe(true);
    });

    it("given_matrix_when_withoutTranslation_then_translationIsRemovedOnly", () => {
        const matrix = new Matrix4x4d().translation(3.0, -2.0, 5.0).multiply(new Matrix4x4d().scale(2.0, 2.0, 2.0));

        const noTranslation = matrix.withoutTranslation();

        expect(noTranslation.extractTranslation().epsilonEquals(new Vector3Dd(0.0, 0.0, 0.0), EPS)).toBe(true);
    });

    it("given_closeMatrices_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Matrix4x4d();
        const b = a.withVal(0, 1, 1.0e-7);

        expect(a.epsilonEquals(b, 1.0e-6)).toBe(true);
        expect(a.epsilonEquals(b, 1.0e-9)).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });

    it("given_identityMatrix_when_multiplied_then_itActsAsNeutralElement", () => {
        const identity = Matrix4x4d.identityMatrix();
        const matrix = new Matrix4x4d().translation(2.0, -3.0, 1.0).multiply(new Matrix4x4d().scale(1.5, 2.0, 0.5));

        expect(identity.multiply(matrix).epsilonEquals(matrix, EPS)).toBe(true);
        expect(matrix.multiply(identity).epsilonEquals(matrix, EPS)).toBe(true);
    });

    it("given_identityAndScale_when_checkingDeterminant_then_valuesAreExpected", () => {
        const identity = Matrix4x4d.identityMatrix();
        const scale = new Matrix4x4d().scale(2.0, 3.0, 4.0);

        expect(Math.abs(identity.determinant() - 1.0)).toBeLessThanOrEqual(EPS);
        expect(Math.abs(scale.determinant() - 24.0)).toBeLessThanOrEqual(1.0e-9);
    });

    it("given_anyMatrix_when_transposedTwice_then_originalIsRecovered", () => {
        const matrix = new Matrix4x4d().withVal(0, 1, 2.0).withVal(1, 2, -3.0).withVal(2, 3, 4.0).withVal(3, 0, -1.0);

        const rebuilt = matrix.transpose().transpose();

        expect(rebuilt.epsilonEquals(matrix, EPS)).toBe(true);
    });
});

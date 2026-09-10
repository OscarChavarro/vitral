import { VSDK } from "../../common/VSDK.js";
import { MatrixNxM } from "../../common/linealAlgebra/MatrixNxM.js";
import { MatrixSingularException } from "../../common/linealAlgebra/exceptions/MatrixSingularException.js";
import type { DeterminantStrategy } from "./DeterminantStrategy.js";
import type { InverseStrategy } from "./InverseStrategy.js";
import { MatrixAlgorithmsSupport } from "./MatrixAlgorithmsSupport.js";

export class GaussCpuStrategy implements DeterminantStrategy, InverseStrategy {
    public id(): string {
        return "gauss-cpu";
    }
    public determinant(matrix: MatrixNxM): number {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        const a = MatrixAlgorithmsSupport.toArray(matrix),
            n = a.length;
        let sign = 1;
        for (let k = 0; k < n; k++) {
            let pivot = k,
                max = Math.abs(a[k]![k]!);
            for (let i = k + 1; i < n; i++)
                if (Math.abs(a[i]![k]!) > max) {
                    max = Math.abs(a[i]![k]!);
                    pivot = i;
                }
            if (Math.abs(max) < VSDK.EPSILON) return 0;
            if (pivot !== k) {
                [a[pivot], a[k]] = [a[k]!, a[pivot]!];
                sign = -sign;
            }
            for (let i = k + 1; i < n; i++) {
                const factor = a[i]![k]! / a[k]![k]!;
                for (let j = k + 1; j < n; j++) a[i]![j]! -= factor * a[k]![j]!;
                a[i]![k] = 0;
            }
        }
        let determinant = sign;
        for (let i = 0; i < n; i++) determinant *= a[i]![i]!;
        return determinant;
    }
    public inverse(matrix: MatrixNxM): MatrixNxM {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        const a = MatrixAlgorithmsSupport.toArray(matrix),
            n = a.length;
        const inverse = Array.from({ length: n }, (_, i) => Array.from({ length: n }, (_, j) => (i === j ? 1 : 0)));
        for (let column = 0; column < n; column++) {
            let pivot = column,
                max = Math.abs(a[column]![column]!);
            for (let i = column + 1; i < n; i++)
                if (Math.abs(a[i]![column]!) > max) {
                    max = Math.abs(a[i]![column]!);
                    pivot = i;
                }
            if (Math.abs(max) < VSDK.EPSILON)
                throw new MatrixSingularException("Matrix is singular during Gauss-Jordan elimination");
            if (pivot !== column) {
                [a[pivot], a[column]] = [a[column]!, a[pivot]!];
                [inverse[pivot], inverse[column]] = [inverse[column]!, inverse[pivot]!];
            }
            const pivotValue = a[column]![column]!;
            for (let j = 0; j < n; j++) {
                a[column]![j]! /= pivotValue;
                inverse[column]![j]! /= pivotValue;
            }
            for (let i = 0; i < n; i++)
                if (i !== column) {
                    const factor = a[i]![column]!;
                    for (let j = 0; j < n; j++) {
                        a[i]![j]! -= factor * a[column]![j]!;
                        inverse[i]![j]! -= factor * inverse[column]![j]!;
                    }
                }
        }
        return MatrixAlgorithmsSupport.fromArray(inverse);
    }
}

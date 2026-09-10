import { VSDK } from "../../common/VSDK.js";
import { MatrixNxM } from "../../common/linealAlgebra/MatrixNxM.js";
import { MatrixSingularException } from "../../common/linealAlgebra/exceptions/MatrixSingularException.js";
import type { DeterminantStrategy } from "./DeterminantStrategy.js";
import type { InverseStrategy } from "./InverseStrategy.js";
import { MatrixAlgorithmsSupport } from "./MatrixAlgorithmsSupport.js";

export class LuDecomposition {
    public constructor(
        public readonly lu: number[][],
        public readonly piv: number[],
        public readonly pivotSign: number,
    ) {}
}
export class LuCpuStrategy implements DeterminantStrategy, InverseStrategy {
    public id(): string {
        return "lu-cpu";
    }
    public determinant(matrix: MatrixNxM): number {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        const decomposition = this.decompose(MatrixAlgorithmsSupport.toArray(matrix));
        let determinant = decomposition.pivotSign;
        for (let i = 0; i < decomposition.lu.length; i++) determinant *= decomposition.lu[i]![i]!;
        return determinant;
    }
    public inverse(matrix: MatrixNxM): MatrixNxM {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        const decomposition = this.decompose(MatrixAlgorithmsSupport.toArray(matrix)),
            n = decomposition.lu.length;
        const inverse = Array.from({ length: n }, () => new Array<number>(n).fill(0));
        for (let column = 0; column < n; column++) {
            const e = new Array<number>(n).fill(0);
            e[column] = 1;
            const x = this.solve(decomposition, e);
            for (let row = 0; row < n; row++) inverse[row]![column] = x[row]!;
        }
        return MatrixAlgorithmsSupport.fromArray(inverse);
    }
    private decompose(source: number[][]): LuDecomposition {
        const n = source.length,
            lu = source.map((row) => row.slice()),
            piv = Array.from({ length: n }, (_, i) => i);
        let pivotSign = 1;
        for (let k = 0; k < n; k++) {
            let p = k,
                max = Math.abs(lu[k]![k]!);
            for (let i = k + 1; i < n; i++)
                if (Math.abs(lu[i]![k]!) > max) {
                    max = Math.abs(lu[i]![k]!);
                    p = i;
                }
            if (Math.abs(max) < VSDK.EPSILON)
                throw new MatrixSingularException("Matrix is singular during LU decomposition");
            if (p !== k) {
                [lu[p], lu[k]] = [lu[k]!, lu[p]!];
                [piv[p], piv[k]] = [piv[k]!, piv[p]!];
                pivotSign = -pivotSign;
            }
            for (let i = k + 1; i < n; i++) {
                lu[i]![k]! /= lu[k]![k]!;
                for (let j = k + 1; j < n; j++) lu[i]![j]! -= lu[i]![k]! * lu[k]![j]!;
            }
        }
        return new LuDecomposition(lu, piv, pivotSign);
    }
    private solve(decomposition: LuDecomposition, b: number[]): number[] {
        const n = decomposition.lu.length,
            x = Array.from({ length: n }, (_, i) => b[decomposition.piv[i]!]!);
        for (let i = 0; i < n; i++) for (let j = 0; j < i; j++) x[i]! -= decomposition.lu[i]![j]! * x[j]!;
        for (let i = n - 1; i >= 0; i--) {
            for (let j = i + 1; j < n; j++) x[i]! -= decomposition.lu[i]![j]! * x[j]!;
            x[i]! /= decomposition.lu[i]![i]!;
        }
        return x;
    }
}

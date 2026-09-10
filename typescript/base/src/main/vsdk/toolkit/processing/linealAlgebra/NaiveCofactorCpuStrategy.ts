import { VSDK } from "../../common/VSDK.js";
import { MatrixNxM } from "../../common/linealAlgebra/MatrixNxM.js";
import { MatrixSingularException } from "../../common/linealAlgebra/exceptions/MatrixSingularException.js";
import type { DeterminantStrategy } from "./DeterminantStrategy.js";
import type { InverseStrategy } from "./InverseStrategy.js";
import { MatrixAlgorithmsSupport } from "./MatrixAlgorithmsSupport.js";

export class NaiveCofactorCpuStrategy implements DeterminantStrategy, InverseStrategy {
    public id(): string {
        return "naive-cofactor-cpu";
    }
    public determinant(matrix: MatrixNxM): number {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        return this.determinantArray(MatrixAlgorithmsSupport.toArray(matrix));
    }
    public inverse(matrix: MatrixNxM): MatrixNxM {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        const source = MatrixAlgorithmsSupport.toArray(matrix),
            determinant = this.determinantArray(source),
            n = source.length;
        if (Math.abs(determinant) < VSDK.EPSILON)
            throw new MatrixSingularException("Trying to invert a matrix with zero determinant");
        const cofactors = Array.from({ length: n }, () => new Array<number>(n));
        for (let row = 0; row < n; row++)
            for (let column = 0; column < n; column++)
                cofactors[row]![column] =
                    ((row + column) % 2 === 0 ? 1 : -1) * this.determinantArray(this.minor(source, row, column));
        const inverse = Array.from({ length: n }, () => new Array<number>(n));
        for (let row = 0; row < n; row++)
            for (let column = 0; column < n; column++) inverse[row]![column] = cofactors[column]![row]! / determinant;
        return MatrixAlgorithmsSupport.fromArray(inverse);
    }
    private determinantArray(matrix: number[][]): number {
        const n = matrix.length;
        if (n === 1) return matrix[0]![0]!;
        if (n === 2) return matrix[0]![0]! * matrix[1]![1]! - matrix[0]![1]! * matrix[1]![0]!;
        let accum = 0;
        for (let column = 0; column < n; column++)
            accum +=
                (column % 2 === 0 ? 1 : -1) *
                matrix[0]![column]! *
                this.determinantArray(this.minor(matrix, 0, column));
        return accum;
    }
    private minor(matrix: number[][], rowToSkip: number, columnToSkip: number): number[][] {
        return matrix
            .filter((_, row) => row !== rowToSkip)
            .map((row) => row.filter((_, column) => column !== columnToSkip));
    }
}

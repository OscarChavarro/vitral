import { MatrixNxM } from "../../common/linealAlgebra/MatrixNxM.js";
import { MatrixDimensionMismatchException } from "../../common/linealAlgebra/exceptions/MatrixDimensionMismatchException.js";
import { MatrixNotSquareException } from "../../common/linealAlgebra/exceptions/MatrixNotSquareException.js";

export class MatrixAlgorithmsSupport {
    private constructor() {}
    public static requireNonNull(matrix: MatrixNxM | null): asserts matrix is MatrixNxM {
        if (matrix === null) throw new RangeError("matrix cannot be null");
    }
    public static requireSquare(matrix: MatrixNxM | null): asserts matrix is MatrixNxM {
        this.requireNonNull(matrix);
        if (matrix.getNumRows() !== matrix.getNumColumns()) throw new MatrixNotSquareException("Matrix must be square");
    }
    public static toArray(matrix: MatrixNxM): number[][] {
        const values = Array.from({ length: matrix.getNumRows() }, () => new Array<number>(matrix.getNumColumns()));
        for (let i = 0; i < matrix.getNumRows(); i++)
            for (let j = 0; j < matrix.getNumColumns(); j++) values[i]![j] = matrix.getVal(i, j);
        return values;
    }
    public static fromArray(values: number[][] | null): MatrixNxM {
        if (values === null || values.length === 0 || values[0] === undefined || values[0].length === 0)
            throw new MatrixDimensionMismatchException("values cannot be null or empty");
        const columns = values[0].length;
        let result = new MatrixNxM(values.length, columns);
        for (let i = 0; i < values.length; i++) {
            if (values[i] === undefined || values[i]!.length !== columns)
                throw new MatrixDimensionMismatchException("all rows must have the same length");
            for (let j = 0; j < columns; j++) result = result.withVal(i, j, values[i]![j]!);
        }
        return result;
    }
}

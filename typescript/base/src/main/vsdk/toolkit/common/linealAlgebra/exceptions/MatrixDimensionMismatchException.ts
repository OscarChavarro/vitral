import { IllegalArgumentException } from "../../../../../java/lang/IllegalArgumentException.js";

export class MatrixDimensionMismatchException extends IllegalArgumentException {
    public override name = "MatrixDimensionMismatchException";
}

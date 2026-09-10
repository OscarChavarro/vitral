import type { MatrixNxM } from "../../common/linealAlgebra/MatrixNxM.js";
export interface DeterminantStrategy {
    id(): string;
    determinant(matrix: MatrixNxM): number;
}

import type { MatrixNxM } from "../../common/linealAlgebra/MatrixNxM.js";
export interface InverseStrategy {
    id(): string;
    inverse(matrix: MatrixNxM): MatrixNxM;
}

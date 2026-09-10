import type { DeterminantStrategy } from "./DeterminantStrategy.js";
import type { InverseStrategy } from "./InverseStrategy.js";
export interface LinearAlgebraBackend {
    id(): string;
    determinantStrategy(): DeterminantStrategy;
    inverseStrategy(): InverseStrategy;
}

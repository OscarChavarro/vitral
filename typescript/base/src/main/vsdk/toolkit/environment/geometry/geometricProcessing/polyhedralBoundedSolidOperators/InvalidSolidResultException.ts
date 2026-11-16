import { PolyhedralBoundedSolidOperationException } from "./PolyhedralBoundedSolidOperationException.js";

/**
The operation completed, but its result fails strict validation
(`PolyhedralBoundedSolidValidationEngine.validateStrict`): self-intersecting
faces, broken shells or a wrong Euler characteristic. The message carries
the operation, the result path and the validator report.
 */
export class InvalidSolidResultException extends PolyhedralBoundedSolidOperationException {
    public constructor(message: string) {
        super(message);
        this.name = "InvalidSolidResultException";
    }
}

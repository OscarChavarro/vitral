import type { Throwable } from "../../../../../../java/lang/Throwable.js";
import { PolyhedralBoundedSolidOperationException } from "./PolyhedralBoundedSolidOperationException.js";

/**
An operand of a boolean operation is not a valid boundary model
(`PolyhedralBoundedSolidValidationEngine.validateBooleanInputs` fails), even
after coincident vertices were welded. The operation does not start: the
pipeline of [MANT1988] Ch. 15 assumes valid operands. The message carries
the validator report.
 */
export class InvalidOperandException extends PolyhedralBoundedSolidOperationException {
    public constructor(message: string, cause?: Throwable) {
        super(message, cause);
        this.name = "InvalidOperandException";
    }
}

import type { Throwable } from "../../../../../../java/lang/Throwable.js";
import { PolyhedralBoundedSolidOperationException } from "./PolyhedralBoundedSolidOperationException.js";

/**
The operation could not produce a result: an Euler operator reported a
fatal inconsistency (the cause is the original
`vsdk.toolkit.common.VSDKFatalException`), the pipeline returned no solid,
or it returned an empty solid where the operands prove that the result is
not empty (for instance a union of non-empty solids, or an intersection of
solids whose interiors overlap). An empty result that is right, such as
A \ A, is returned normally; see
{@link PolyhedralBoundedSolidModeler#setOp}.
 */
export class SolidOperationFailedException extends PolyhedralBoundedSolidOperationException {
    public constructor(message: string, cause?: Throwable) {
        super(message, cause);
        this.name = "SolidOperationFailedException";
    }
}

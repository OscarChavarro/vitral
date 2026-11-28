import { Throwable } from "../../../../java/lang/Throwable.js";
import { VSDKException } from "../VSDKException.js";

export class AlgebraicExpressionException extends VSDKException {
    public constructor();
    public constructor(message: string);
    public constructor(cause: Throwable);
    public constructor(message: string, cause: Throwable);
    public constructor(messageOrCause?: string | Throwable, cause?: Throwable) {
        if (typeof messageOrCause === "string" && cause !== undefined) {
            super(messageOrCause, cause);
        } else if (typeof messageOrCause === "string") {
            super(messageOrCause);
        } else if (messageOrCause !== undefined) {
            super(messageOrCause);
        } else {
            super();
        }
        this.name = "AlgebraicExpressionException";
    }
}

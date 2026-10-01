import { Exception } from "../../../../java/lang/Exception.js";

export class ExceptionWidgetParseError extends Exception {
    public static readonly serialVersionUID = 20140314;

    public constructor() {
        super("Parse error reading GUI data");
        this.name = "ExceptionWidgetParseError";
    }

    public override toString(): string {
        return "Parse error reading GUI data";
    }
}

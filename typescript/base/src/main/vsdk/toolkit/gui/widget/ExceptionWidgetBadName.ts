import { Exception } from "../../../../java/lang/Exception.js";

export class ExceptionWidgetBadName extends Exception {
    public static readonly serialVersionUID = 20140314;

    public constructor() {
        super("Bad name");
        this.name = "ExceptionWidgetBadName";
    }

    public override toString(): string {
        return "Bad name";
    }
}

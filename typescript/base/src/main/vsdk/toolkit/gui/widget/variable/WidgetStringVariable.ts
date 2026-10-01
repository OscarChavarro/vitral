import { WidgetVariable } from "./WidgetVariable.js";

export class WidgetStringVariable extends WidgetVariable {
    public override getType(): string {
        return "String";
    }

    public override getValidRange(): string {
        return this.validRange;
    }

    public override setValidRange(vr: string): void {
        this.validRange = vr;
    }
}

import { WidgetVariable } from "./WidgetVariable.js";

export class WidgetIntegerVariable extends WidgetVariable {
    public constructor() {
        super();
        this.validRange = "(-INF, INF)";
    }

    public override getType(): string {
        return "Integer";
    }

    public override getValidRange(): string {
        return this.validRange;
    }

    public override setValidRange(vr: string): void {
        this.validRange = vr;
    }
}

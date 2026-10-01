import { WidgetVariable } from "./WidgetVariable.js";

export class WidgetColorRgbVariable extends WidgetVariable {
    public constructor() {
        super();
        this.validRange = "<[0.0, 1.0], [0.0, 1.0], [0.0, 1.0]>";
    }

    public override getType(): string {
        return "ColorRgb";
    }

    public override getValidRange(): string {
        return this.validRange;
    }

    public override setValidRange(vr: string): void {
        this.validRange = vr;
    }
}

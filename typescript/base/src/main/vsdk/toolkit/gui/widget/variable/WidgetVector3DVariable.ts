import { WidgetVariable } from "./WidgetVariable.js";

export class WidgetVector3DVariable extends WidgetVariable {
    public constructor() {
        super();
        this.validRange = "<(-INF, INF), (-INF, INF), (-INF, INF)>";
    }

    public override getType(): string {
        return "Vector3Dd";
    }

    public override getValidRange(): string {
        return this.validRange;
    }

    public override setValidRange(vr: string): void {
        this.validRange = vr;
    }
}

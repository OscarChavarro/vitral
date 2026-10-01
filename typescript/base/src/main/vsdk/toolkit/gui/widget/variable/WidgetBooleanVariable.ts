import { UnsupportedOperationException } from "../../../../../java/lang/UnsupportedOperationException.js";
import type { Image } from "../../../media/Image.js";
import { WidgetVariable } from "./WidgetVariable.js";

export class WidgetBooleanVariable extends WidgetVariable {
    private imageForTrueState: Image | null;
    private imageForFalseState: Image | null;

    public constructor() {
        super();
        this.validRange = "false, true";
        this.imageForTrueState = null;
        this.imageForFalseState = null;
    }

    public override getType(): string {
        return "Boolean";
    }

    public override getValidRange(): string {
        return this.validRange;
    }
    //New Oscar August 13/2014
    public getImageForTrueState(): Image | null {
        return this.imageForTrueState;
    }

    public getImageForFalseState(): Image | null {
        return this.imageForFalseState;
    }

    public override setValidRange(_vr: string): void {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //New Oscar August 13/2014
    public setImageForTrueState(imageForTrueState: Image | null): void {
        this.imageForTrueState = imageForTrueState;
    }

    public setImageForFalseState(imageForFalseState: Image | null): void {
        this.imageForFalseState = imageForFalseState;
    }
}

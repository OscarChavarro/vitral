import { PresentationElement } from "../PresentationElement.js";

/**
A visual variable contains information needed to display it on visual
analytics widgets.
*/
export class VisualDoubleVariable extends PresentationElement {
    private name: string | null;
    private description: string | null;
    private currentValue: number;

    public constructor() {
        super();
        this.name = null;
        this.description = null;
        this.currentValue = 0;
    }

    /**
    @return the name
    */
    public getName(): string | null {
        return this.name;
    }

    /**
    @param name the name to set
    */
    public setName(name: string | null): void {
        this.name = name;
    }

    /**
    @return the description
    */
    public getDescription(): string | null {
        return this.description;
    }

    /**
    @param description the description to set
    */
    public setDescription(description: string | null): void {
        this.description = description;
    }

    /**
    @return the currentValue
    */
    public getCurrentValue(): number {
        return this.currentValue;
    }

    /**
    @param currentValue the currentValue to set
    */
    public setCurrentValue(currentValue: number): void {
        this.currentValue = currentValue;
    }
}

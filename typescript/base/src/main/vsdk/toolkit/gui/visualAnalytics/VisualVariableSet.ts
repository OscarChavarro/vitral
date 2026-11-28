import { ArrayList } from "../../../../java/util/ArrayList.js";
import { PresentationElement } from "../PresentationElement.js";
import type { VisualDoubleVariable } from "./VisualDoubleVariable.js";

/**
A visual variable set is a container for visual variables used on visual
analytics widgets.
*/
export class VisualVariableSet extends PresentationElement {
    private readonly doubles: ArrayList<VisualDoubleVariable>;

    public constructor() {
        super();
        this.doubles = new ArrayList<VisualDoubleVariable>();
    }

    /**
    @return the doubles
    */
    public getDoubles(): ArrayList<VisualDoubleVariable> {
        return this.doubles;
    }
}

import { WidgetElement } from "../WidgetElement.js";

/**
A WidgetVariable is a value stored at computer memory which has a type, and is
assigned to a name and that is inside a valid values range. For example, the
radius of an sphere has a valid value range expressed as an interval: "[0,
INF]". A current value for that variable could be the number "5.0", and its
name could be "r". This variable is of type "WidgetDoubleVariable".

This class is the superclass of several other classes, each one representing
an specific variable type.

This class and its subclasses plays a client role in a reflection design
pattern.

This class plays a role of leaf on an n-ary tree in the composite design
pattern.
*/
export abstract class WidgetVariable extends WidgetElement {
    /// Variable names follows a convention of scope operator. Example:
    /// "position" is a global name, "camera.position" is the same variable
    /// under the "camera" scope. "scene.camera.position" could be a full
    /// hierarchy name for a variable inside the system.

    protected name: string;
    protected validRange: string;
    protected initialvalue: string;

    public constructor() {
        super();
        this.name = "";
        this.validRange = "";
        this.initialvalue = "";
    }

    public getName(): string {
        return this.name;
    }

    public setName(name: string): void {
        this.name = name;
    }

    public getInitialvalue(): string {
        return this.initialvalue;
    }

    public setInitialvalue(initialvalue: string): void {
        this.initialvalue = initialvalue;
    }

    public override toString(): string {
        let msg = "";
        msg =
            msg +
            "VARIABLE:\n" +
            "     TYPE: " + this.getType() + "\n" +
            "     NAME: " + this.getName() + "\n" +
            "     INITIAL_VALUE: " + this.getInitialvalue() + "\n" +
            "     VALID_RANGE: " + this.getValidRange() + "\n";
        return msg;
    }

    /**
    Each variable has a type. Examples: "Integer", "Double", "String",
    "Vector3Dd".
    */
    public abstract getType(): string;

    /**
    Gets the current String specifying valid value range. The returned String
    contains an specification expressed in Vitral GUI value ranges language.
    */
    public abstract getValidRange(): string;

    /**
    Sets the current String specifying valid value range. The returned String
    contains an specification expressed in Vitral GUI value ranges language.
    */
    public abstract setValidRange(vr: string): void;
}

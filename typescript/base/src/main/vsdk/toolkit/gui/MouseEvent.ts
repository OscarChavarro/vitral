import { PresentationElement } from "./PresentationElement.js";

/**
Mouse event, as delivered to the interaction techniques of vitral. Buttons and
modifier masks keep the values of Java's AWT (`getButton`, `getModifiersEx`),
which is the convention every port of the toolkit follows.
*/
export class MouseEvent extends PresentationElement {
    //public static final int MOUSE_FIRST = 500;
    //public static final int MOUSE_LAST = 507;
    //public static final int MOUSE_CLICKED = 500;
    //public static final int MOUSE_PRESSED = 501;
    //public static final int MOUSE_RELEASED = 502;
    //public static final int MOUSE_MOVED = 503;
    //public static final int MOUSE_ENTERED = 504;
    //public static final int MOUSE_EXITED = 505;
    //public static final int MOUSE_DRAGGED = 506;
    //public static final int MOUSE_WHEEL = 507;
    //public static final int NOBUTTON = 0;

    public static readonly BUTTON1 = 1;
    public static readonly BUTTON2 = 2;
    public static readonly BUTTON3 = 3;
    public static readonly CTRL_DOWN_MASK = 128;
    public static readonly BUTTON1_DOWN_MASK = 1024;
    public static readonly BUTTON2_DOWN_MASK = 2048;
    public static readonly BUTTON3_DOWN_MASK = 4096;

    private x = 0;
    private y = 0;
    private button = 0;
    private modifiers = 0;
    private clicks = 0; // used for wheel mouse movements

    public getClicks(): number {
        return this.clicks;
    }

    public getX(): number {
        return this.x;
    }

    public getY(): number {
        return this.y;
    }

    public getButton(): number {
        return this.button;
    }

    public getModifiers(): number {
        return this.modifiers;
    }

    public setClicks(value: number): void {
        this.clicks = value;
    }

    public setX(value: number): void {
        this.x = value;
    }

    public setY(value: number): void {
        this.y = value;
    }

    public setButton(value: number): void {
        this.button = value;
    }

    public setModifiers(value: number): void {
        this.modifiers = value;
    }
}

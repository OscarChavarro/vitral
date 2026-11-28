// Java basic classes
import { ArrayList } from "../../../../java/util/ArrayList.js";

// VSDK classes
import type { HudIcon } from "../HudIcon.js";

export class InformationDialog {
    private title: string;
    private contents: string;

    /// Changes in the range [0, 1.0]. 1.0 means to fully display dialog,
    /// other values are used in popup and popdown animation process.
    private animationParameter: number;
    private animationDelta: number;
    private dialogState: number;

    private readonly dialogIcons: ArrayList<HudIcon>;

    public static readonly OPENING = 1;
    public static readonly NORMAL = 2;
    public static readonly CLOSING = 3;
    public static readonly CLOSED = 4;

    public constructor() {
        this.title = "Empty dialog";
        this.contents = "Empty contents";
        this.animationDelta = 0.1;
        this.animationParameter = 0.0;
        this.dialogState = InformationDialog.OPENING;
        this.dialogIcons = new ArrayList<HudIcon>();
    }

    /**
    @return the title
    */
    public getTitle(): string {
        return this.title;
    }

    /**
    @param title the title to set
    */
    public setTitle(title: string): void {
        this.title = title;
    }

    /**
    @return the contents
    */
    public getContents(): string {
        return this.contents;
    }

    /**
    @param contents the contents to set
    */
    public setContents(contents: string): void {
        this.contents = contents;
    }

    /**
    @return the animationParameter
    */
    public getAnimationParameter(): number {
        return this.animationParameter;
    }

    /**
    @param animationParameter the animationParameter to set
    */
    public setAnimationParameter(animationParameter: number): void {
        this.animationParameter = animationParameter;
        if (this.animationParameter < 0.0) {
            this.animationParameter = 0.0;
            this.dialogState = InformationDialog.CLOSED;
        }
        if (this.animationParameter > 1.0) {
            this.animationParameter = 1.0;
            this.dialogState = InformationDialog.NORMAL;
        }
    }

    /**
    @return the animationDelta
    */
    public getAnimationDelta(): number {
        return this.animationDelta;
    }

    /**
    @param animationDelta the animationDelta to set
    */
    public setAnimationDelta(animationDelta: number): void {
        this.animationDelta = animationDelta;
    }

    /**
    @return the dialogState
    */
    public getDialogState(): number {
        return this.dialogState;
    }

    /**
    @param dialogState the dialogState to set
    */
    public setDialogState(dialogState: number): void {
        this.dialogState = dialogState;
    }

    /**
    @return the dialogIcons
    */
    public getDialogIcons(): ArrayList<HudIcon> {
        return this.dialogIcons;
    }
}

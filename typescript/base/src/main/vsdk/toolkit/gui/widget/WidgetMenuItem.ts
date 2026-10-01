import type { Widget } from "./Widget.js";
import { WidgetMenuElement } from "./WidgetMenuElement.js";

export class WidgetMenuItem extends WidgetMenuElement {
    private name: string | null;
    private commandName: string | null;
    private isSeparatorFlag: boolean;
    private mnemonic: string;
    private accelerator: string | null;

    public isSeparator(): boolean {
        return this.isSeparatorFlag;
    }

    public constructor(c: Widget | null) {
        super();
        this.context = c;
        this.name = null;
        this.commandName = null;
        this.isSeparatorFlag = false;
        this.mnemonic = "\0";
        this.accelerator = null;
    }

    public setName(n: string | null): void {
        this.name = this.processSimplifiedName(n);
        this.mnemonic = this.processMnemonic(n);
        this.accelerator = this.processAccelerator(n);
    }

    public getName(): string {
        if (this.name === null) return "No Name";
        return this.name;
    }

    public getCommandName(): string {
        if (this.commandName === null) return "IDC_NO_COMMAND";
        return this.commandName;
    }

    public setCommandName(a: string | null): void {
        this.commandName = a;
    }

    public addModifier(m: string): void {
        if (m === "CHECKED") {
            // Java records nothing for this modifier
        } else if (m === "GRAYED") {
            // Java records nothing for this modifier
        } else if (m === "UNCHEKED") {
            // Java records nothing for this modifier
        } else if (m === "SEPARATOR") {
            this.isSeparatorFlag = true;
        } else {
            this.setCommandName(m);
        }
    }

    public override toStringAtLevel(level: number): string {
        let leadingSpace = "";
        let j: number;

        for (j = 0; j < level; j++) {
            leadingSpace = leadingSpace + "  ";
        }

        let msg = leadingSpace + " - MenuItem ";

        if (this.isSeparatorFlag) {
            msg = msg + "--- SEPARATOR ---";
        } else {
            msg = msg + '"' + this.name + '"';
        }

        msg = msg + "\n";

        return msg;
    }

    public override toString(): string {
        return this.toStringAtLevel(0);
    }

    public getMnemonic(): string {
        return this.mnemonic;
    }

}

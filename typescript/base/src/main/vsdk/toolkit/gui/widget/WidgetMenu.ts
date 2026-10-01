import type { Widget } from "./Widget.js";
import { WidgetMenuElement } from "./WidgetMenuElement.js";

export class WidgetMenu extends WidgetMenuElement {
    private readonly children: WidgetMenuElement[];
    private name: string | null;
    private mnemonic: string;
    private accelerator: string | null;

    public constructor(c: Widget | null) {
        super();
        this.context = c;
        this.children = [];
        this.name = null;
        this.mnemonic = "\0";
        this.accelerator = null;
    }

    public getChildren(): WidgetMenuElement[] {
        return this.children;
    }

    public setName(n: string | null): void {
        this.name = this.processSimplifiedName(n);
        this.mnemonic = this.processMnemonic(n);
        this.accelerator = this.processAccelerator(n);
    }

    public addChild(i: WidgetMenuElement): void {
        this.children.push(i);
    }

    public override toStringAtLevel(level: number): string {
        let leadingSpace = "";
        let j: number;

        for (j = 0; j < level; j++) {
            leadingSpace = leadingSpace + "  ";
        }

        let msg = leadingSpace + 'Menu "' + this.name + '"\n';

        let i: number;

        for (i = 0; i < this.children.length; i++) {
            msg = msg + this.children[i]!.toStringAtLevel(level + 1);
        }

        return msg;
    }

    public override toString(): string {
        return this.toStringAtLevel(0);
    }

    public getName(): string | null {
        return this.name;
    }

    public getMnemonic(): string {
        return this.mnemonic;
    }

}

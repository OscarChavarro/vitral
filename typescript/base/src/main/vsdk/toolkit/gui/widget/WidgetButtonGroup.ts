import type { Widget } from "./Widget.js";
import type { WidgetCommand } from "./WidgetCommand.js";
import { WidgetElement } from "./WidgetElement.js";

export class WidgetButtonGroup extends WidgetElement {
    private readonly commands: WidgetCommand[];
    private name: string | null = null;

    private showText = false;
    private showIcons = false;
    private showTitle = false;
    private direction = 0;

    public static readonly HORIZONTAL = 1;
    public static readonly VERTICAL = 2;

    public constructor(parent: Widget) {
        super();
        this.commands = [];
        this.context = parent;
    }

    public setShowText(f: boolean): void {
        this.showText = f;
    }

    public setShowIcons(f: boolean): void {
        this.showIcons = f;
    }

    public setTitle(f: boolean): void {
        this.showTitle = f;
    }

    public setDirection(d: number): void {
        this.direction = d;
    }

    public getDirection(): number {
        return this.direction;
    }

    public isShowTextSet(): boolean {
        return this.showText;
    }

    public isShowIconsSet(): boolean {
        return this.showIcons;
    }

    public isShowTitleSet(): boolean {
        return this.showTitle;
    }

    public getCommands(): WidgetCommand[] {
        return this.commands;
    }

    public setName(n: string | null): void {
        this.name = n;
    }

    public getName(): string | null {
        return this.name;
    }

    public addCommandByName(commandName: string): void {
        const command: WidgetCommand | null = this.context === null ? null : this.context.getCommandByName(commandName);

        if (command !== null) {
            this.commands.push(command);
        }
    }
}

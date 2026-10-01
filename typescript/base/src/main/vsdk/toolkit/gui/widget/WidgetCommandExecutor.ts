import { PresentationElement } from "../PresentationElement.js";

export abstract class WidgetCommandExecutor extends PresentationElement {
    protected commandCache: Map<number, string>;

    public constructor() {
        super();
        this.commandCache = new Map<number, string>();
    }

    public addIdToCommandCache(id: number, command: string): void {
        this.commandCache.set(id, command);
    }

    public getCommandFromId(id: number): string | null {
        const command: string | undefined = this.commandCache.get(id);
        return command === undefined ? null : command;
    }

    public abstract executeMenuCommand(inIdCommand: string): boolean;
}

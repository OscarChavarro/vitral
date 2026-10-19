import { WidgetDialog } from "../../widget/WidgetDialog.js";

/** Per-opening values and lifetime for a renderer-independent dialog definition. */
export class DialogSession {
    private readonly drafts = new Map<string, string>();
    private open = true;
    private busy = false;
    private generation = 0;

    public constructor(private readonly definition: WidgetDialog) {}

    public getDefinition(): WidgetDialog { return this.definition; }
    public setDraft(binding: string, value: string): void { this.drafts.set(binding, value); }
    public getDraft(binding: string): string | undefined { return this.drafts.get(binding); }
    public isOpen(): boolean { return this.open; }
    public isBusy(): boolean { return this.busy; }
    public beginAction(): number {
        if (!this.open || this.busy) return -1;
        this.busy = true;
        return ++this.generation;
    }
    public completeAction(request: number): boolean {
        if (!this.open || !this.busy || request !== this.generation) return false;
        this.busy = false;
        return true;
    }
    public close(): void { this.open = false; this.busy = false; ++this.generation; }
}

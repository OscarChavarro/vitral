/**
Counterpart of `java.awt.event.ActionEvent` for the DOM GUIs of the toolkit:
what a button or a menu item reports when the user activates it.
*/
export class HtmlActionEvent {
    private readonly source: object;
    private readonly id: number;
    private readonly actionCommand: string;

    /**
    @param source element (or object) that originated the event
    @param id identifier of the kind of event
    @param actionCommand command string of the event
    */
    public constructor(source: object, id: number, actionCommand: string) {
        this.source = source;
        this.id = id;
        this.actionCommand = actionCommand;
    }

    /**
    @return the element (or object) that originated the event
    */
    public getSource(): object {
        return this.source;
    }

    /**
    @return identifier of the kind of event
    */
    public getID(): number {
        return this.id;
    }

    /**
    @return command string of the event
    */
    public getActionCommand(): string {
        return this.actionCommand;
    }
}

/**
Counterpart of `java.awt.event.ActionListener`.
*/
export interface HtmlActionListener {
    actionPerformed(event: HtmlActionEvent): void;
}

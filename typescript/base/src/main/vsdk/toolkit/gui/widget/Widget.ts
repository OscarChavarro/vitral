import { PresentationElement } from "../PresentationElement.js";
import type { WidgetVariable } from "./variable/WidgetVariable.js";
import type { WidgetButtonGroup } from "./WidgetButtonGroup.js";
import type { WidgetCommand } from "./WidgetCommand.js";
import type { WidgetDialog } from "./WidgetDialog.js";
import type { WidgetMenu } from "./WidgetMenu.js";

/**
In order to understand this class, the following concepts must be taken into
account: - WidgetVariable - WidgetCommand - Reflection (introspection) design
pattern - Menubars, buttons bars, and other are based upon WidgetCommands -
Dialogs are based upon WidgetVariables and WidgetCommands - Dialogs and menus are
hierarchical
*/
export class Widget extends PresentationElement {
    // Basic / fundamental / atomic elements

    private readonly commandList: WidgetCommand[];
    private readonly variableList: WidgetVariable[];
    private readonly messagesTable: Map<string, string>;
    // Composite elements
    private menubar: WidgetMenu | null;
    private readonly popupMenuList: WidgetMenu[];
    private readonly buttonGroupList: WidgetButtonGroup[];
    private dialogList: WidgetDialog[];

    public getDialogList(): WidgetDialog[] {
        return this.dialogList;
    }

    public setDialogList(dialogList: WidgetDialog[]): void {
        this.dialogList = dialogList;
    }

    public constructor() {
        super();
        this.menubar = null;
        this.popupMenuList = [];
        this.commandList = [];
        this.buttonGroupList = [];
        this.messagesTable = new Map<string, string>();
        /*
         * TODO variableList should be hashMap
         */
        this.variableList = [];
        this.dialogList = [];
    }

    public addMessage(id: string, message: string): void {
        this.messagesTable.set(id, message);
    }

    public getMessage(id: string): string {
        const msg: string | undefined = this.messagesTable.get(id);

        if (msg === undefined) {
            return id;
        }
        return msg;
    }

    public setMenubar(m: WidgetMenu | null): void {
        this.menubar = m;
    }

    public getMenubar(): WidgetMenu | null {
        return this.menubar;
    }

    public getCommandByName(name: string): WidgetCommand | null {
        let command: WidgetCommand | null = null;
        let candidate: WidgetCommand;
        let i: number;

        for (i = 0; i < this.commandList.length; i++) {
            candidate = this.commandList[i]!;
            if (candidate.getId() === name) {
                command = candidate;
                break;
            }
        }
        return command;
    }

    public getButtonGroup(name: string | null): WidgetButtonGroup | null {
        if (name === null) {
            return null;
        }

        let group: WidgetButtonGroup | null = null;
        let candidate: WidgetButtonGroup;
        let i: number;

        for (i = 0; i < this.buttonGroupList.length; i++) {
            candidate = this.buttonGroupList[i]!;
            if (candidate.getName() === name) {
                group = candidate;
                break;
            }
        }
        return group;
    }

    public getPopup(name: string | null): WidgetMenu | null {
        let menu: WidgetMenu | null = null;
        let candidate: WidgetMenu;

        let i: number;
        for (i = 0; i < this.popupMenuList.length; i++) {
            candidate = this.popupMenuList[i]!;
            if (candidate.getName() === name) {
                menu = candidate;
                break;
            }
        }
        return menu;
    }

    public addPopupMenu(p: WidgetMenu): void {
        this.popupMenuList.push(p);
    }

    public addCommand(c: WidgetCommand): void {
        this.commandList.push(c);
    }

    public addButtonGroup(b: WidgetButtonGroup): void {
        this.buttonGroupList.push(b);
    }

    public addDialog(dialog: WidgetDialog): void {
        this.dialogList.push(dialog);
    }

    public addVariable(variable: WidgetVariable): void {
        this.variableList.push(variable);
    }

    public getVariableByName(name: string): WidgetVariable | null {
        let i: number;
        for (i = 0; i < this.variableList.length; i++) {
            if (this.variableList[i]!.getName() === name) {
                return this.variableList[i]!;
            }
        }
        return null;
    }

    public override toString(): string {
        let msg = "= Widget report =========================================================\n";
        msg = msg + "Widget cache structure contains " + this.popupMenuList.length + " popup submenu structures registered\n";
        msg = msg + "Widget cache structure contains " + this.commandList.length + " commands registered\n";

        let i: number;
        let command: WidgetCommand;
        for (i = 0; i < this.commandList.length; i++) {
            command = this.commandList[i]!;
            msg = msg + command.toString();
        }

        if (this.menubar === null) {
            msg = msg + "There is NO menubar!";
        } else {
            msg = msg + 'There is a menubar active, called "' + this.menubar.getName() + '"\n';
            msg = msg + "Dumping menubar tree structure...\n";
            msg = msg + this.menubar.toString();
        }

        /**
         * TODO print lists of WidgetDialogs and variables pending
         */

        //-----------------------------------------------------------------------------------
        let dialog: WidgetDialog;
        for (i = 0; i < this.dialogList.length; i++) {
            dialog = this.dialogList[i]!;
            msg = msg + dialog.toString();
        }

        let variable: WidgetVariable;
        for (i = 0; i < this.variableList.length; i++) {
            variable = this.variableList[i]!;
            msg = msg + variable.toString();
        }
        //-----------------------------------------------------------------------------------
        msg = msg + "===========================================================================\n";

        return msg;
    }
}

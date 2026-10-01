import { VSDK } from "../../common/VSDK.js";
import { Logger } from "../../common/logging/Logger.js";
import { WidgetElement } from "./WidgetElement.js";

/**
This class plays a role of internal node on an n-ary tree in the composite
design pattern.
*/
export class WidgetDialog extends WidgetElement {
    public static readonly ORIENTATION_HORIZONTAL = 0x01;
    public static readonly ORIENTATION_VERTICAL = 0x02;
    private id: string | null = null;
    private name: string | null = null;
    private orientation: number;
    private widgetElementList: WidgetElement[];
    /// In the importing from file process, a dialog could be "incomplete",
    /// due to having a variable reference to a variable that is not loaded
    /// yet.  In such situation, a two pass processing is performed:
    /// On first pass does not create neither associate any variable, but store
    /// its incomplete references (names) on this ArrayList. On second pass
    /// the factory traverse this list in order to add current variables
    /// from context. Check WidgetPersistence.importAquynzaWidgetDialog method.
    private pendingVariableNames: string[];
    private pendingCommandNames: string[];
    private pendingDialogNames: string[];
    private pendingDialogRefNames: string[];
    private collapsable: boolean;

    public constructor() {
        super();
        this.widgetElementList = [];
        this.pendingVariableNames = [];
        this.pendingCommandNames = [];
        this.pendingDialogNames = [];
        this.pendingDialogRefNames = [];
        this.orientation = WidgetDialog.ORIENTATION_VERTICAL;
        this.collapsable = false;
    }

    public isCollapsable(): boolean {
        return this.collapsable;
    }

    public setCollapsable(collapsable: boolean): void {
        this.collapsable = collapsable;
    }

    public getPendingCommandNames(): string[] {
        return this.pendingCommandNames;
    }

    public setPendingCommandNames(pendingCommandNames: string[]): void {
        this.pendingCommandNames = pendingCommandNames;
    }

    public getWidgetElementList(): WidgetElement[] {
        return this.widgetElementList;
    }

    public setWidgetElementList(widgetElementList: WidgetElement[]): void {
        this.widgetElementList = widgetElementList;
    }

    public getId(): string | null {
        return this.id;
    }

    public setId(id: string | null): void {
        this.id = id;
    }

    public getName(): string | null {
        return this.name;
    }

    public setName(name: string | null): void {
        this.name = name;
    }

    public getOrientation(): number {
        return this.orientation;
    }

    public setOrientation(orientation: number): void {
        this.orientation = orientation;
    }

    public getPendingVariableNames(): string[] {
        return this.pendingVariableNames;
    }

    public setPendingVariableNames(pendingVariableNames: string[]): void {
        this.pendingVariableNames = pendingVariableNames;
    }

    public getPendingDialogNames(): string[] {
        return this.pendingDialogNames;
    }

    public setPendingDialogNames(pendingDialogNames: string[]): void {
        this.pendingDialogNames = pendingDialogNames;
    }

    public getChildren(): WidgetElement[] {
        return this.widgetElementList;
    }

    public setChildren(widgetElementList: WidgetElement[]): void {
        this.widgetElementList = widgetElementList;
    }

    public getPendingDialogRefNames(): string[] {
        return this.pendingDialogRefNames;
    }

    public setPendingDialogRefNames(pendingDialogRefNames: string[]): void {
        this.pendingDialogRefNames = pendingDialogRefNames;
    }

    /**
     * Given a variableName, this method asks the context for a WidgetVariable
     * pointer in order to reference the given variable. If that variable
     * doesn't exist, an exception is thrown.
     */
    public associateVariable(variableName: string): void {
        Logger.reportMessage(this, VSDK.FATAL_ERROR, "associateVariable", "Variable " + variableName + " not found!");
    }

    public override toString(): string {
        let msg = "";

        msg = msg + "    DIALOG: " + this.getId() + "\n";
        for (let j = 0; j < this.widgetElementList.length; j++) {
            msg = msg + "    " + String(this.widgetElementList[j]) + "\n";
        }
        for (let j = 0; j < this.pendingCommandNames.length; j++) {
            msg = msg + "    Commandname: " + this.pendingCommandNames[j] + "\n";
        }

        for (let j = 0; j < this.pendingDialogRefNames.length; j++) {
            msg = msg + "    DialogRefNames: " + this.pendingDialogRefNames[j] + "\n";
        }

        for (let j = 0; j < this.pendingVariableNames.length; j++) {
            msg = msg + "    VariableNames: " + this.pendingVariableNames[j] + "\n";
        }
        return msg;
    }
}

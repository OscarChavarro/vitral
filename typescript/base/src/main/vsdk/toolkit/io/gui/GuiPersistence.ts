import { ExceptionWidgetBadName } from "../../gui/widget/ExceptionWidgetBadName.js";
import { Widget } from "../../gui/widget/Widget.js";
import { WidgetButtonGroup } from "../../gui/widget/WidgetButtonGroup.js";
import { WidgetCommand } from "../../gui/widget/WidgetCommand.js";
import { WidgetDialog } from "../../gui/widget/WidgetDialog.js";
import { WidgetMenu } from "../../gui/widget/WidgetMenu.js";
import type { WidgetMenuElement } from "../../gui/widget/WidgetMenuElement.js";
import { WidgetMenuItem } from "../../gui/widget/WidgetMenuItem.js";
import { WidgetBooleanVariable } from "../../gui/widget/variable/WidgetBooleanVariable.js";
import { WidgetColorRgbVariable } from "../../gui/widget/variable/WidgetColorRgbVariable.js";
import { WidgetDoubleVariable } from "../../gui/widget/variable/WidgetDoubleVariable.js";
import { WidgetIntegerVariable } from "../../gui/widget/variable/WidgetIntegerVariable.js";
import { WidgetStringVariable } from "../../gui/widget/variable/WidgetStringVariable.js";
import type { WidgetVariable } from "../../gui/widget/variable/WidgetVariable.js";
import { WidgetVector3DVariable } from "../../gui/widget/variable/WidgetVector3DVariable.js";
import type { RGBAImageUncompressed } from "../../media/RGBAImageUncompressed.js";
import type { RGBImageUncompressed } from "../../media/RGBImageUncompressed.js";
import { PersistenceElement } from "../PersistenceElement.js";

/**
Reads the images the commands of a GUI definition name (their icons and
transparency masks). Java opens them as `java.io.File`s through
`ImagePersistence`; every runtime supplies its own reader here (i.e. over
`fetch` in a browser, see `WebGuiPersistence` in `@vitral/webgl`), so this class
stays platform neutral.
*/
export interface GuiPersistenceImageReader {
    /**
    @param path path of the image, as Java builds it: the global data path, a
    slash and the file name of the GUI definition
    @return the image, with an alpha channel
    */
    importRGBA(path: string): Promise<RGBAImageUncompressed>;

    /**
    @param path path of the image, built as for `importRGBA`
    @return the image, without alpha channel
    */
    importRGB(path: string): Promise<RGBImageUncompressed>;
}

type JsonNode = unknown;

function isObject(node: JsonNode): node is Record<string, JsonNode> {
    return typeof node === "object" && node !== null && !Array.isArray(node);
}

/**
Jackson's `JsonNode.path(fieldName)`: the field of an object, or a missing
node (here `undefined`) for anything else.
*/
function path(node: JsonNode, fieldName: string): JsonNode {
    return isObject(node) ? node[fieldName] : undefined;
}

/** Jackson's `JsonNode.asText()` for the scalar nodes a GUI definition holds. */
function asText(node: JsonNode): string {
    if (typeof node === "string") {
        return node;
    }
    if (typeof node === "number" || typeof node === "boolean") {
        return String(node);
    }
    return "";
}

export class GuiPersistence extends PersistenceElement {
    /**
    Imports the Vitral widget GUI model from the modern JSON representation.

    The public method name is preserved because older applications already call
    this entry point; only the on-disk format changed from Aquynza .gui text to
    JSON.

    Java reads the definition from an `InputStream` and its images from files;
    here the definition arrives as its JSON text, and the images through the
    reader of the runtime. Since that reader is asynchronous, so is this method.

    @param source text of the JSON GUI definition
    @param globalDataPath path the image file names of the definition are
    relative to
    @param imageReader reads the images of the commands
    @return the GUI definition
    */
    public static async importAquynzaGui(
        source: string,
        globalDataPath: string,
        imageReader: GuiPersistenceImageReader,
    ): Promise<Widget> {
        const root: JsonNode = JSON.parse(source) as JsonNode;
        const context = new Widget();

        await GuiPersistence.importCommands(path(root, "commands"), context, globalDataPath, imageReader);
        GuiPersistence.importVariables(path(root, "variables"), context);
        GuiPersistence.importMenubar(path(root, "menubar"), context);
        GuiPersistence.importPopups(path(root, "popups"), context);
        GuiPersistence.importButtonGroups(path(root, "buttonGroups"), context);
        GuiPersistence.importDialogs(path(root, "dialogs"), context);
        GuiPersistence.importMessages(path(root, "messages"), context);

        return context;
    }

    private static async importCommands(
        commandNodes: JsonNode,
        context: Widget,
        globalDataPath: string,
        imageReader: GuiPersistenceImageReader,
    ): Promise<void> {
        if (!Array.isArray(commandNodes)) {
            return;
        }

        for (const commandNode of commandNodes as JsonNode[]) {
            const command = new WidgetCommand();
            command.setId(GuiPersistence.requiredText(commandNode, "id"));
            command.setName(GuiPersistence.text(commandNode, "name"));
            command.setBrief(GuiPersistence.text(commandNode, "brief"));

            const help: JsonNode = isObject(commandNode) ? commandNode["help"] : undefined;
            if (help !== undefined && help !== null) {
                if (Array.isArray(help)) {
                    for (const line of help as JsonNode[]) {
                        command.appendToHelp(asText(line));
                    }
                } else {
                    command.setHelp(asText(help));
                }
            }

            await GuiPersistence.loadCommandImage(command, commandNode, "icon", globalDataPath, true, false, imageReader);
            await GuiPersistence.loadCommandImage(
                command, commandNode, "secondaryIcon", globalDataPath, false, false, imageReader);
            await GuiPersistence.loadCommandImage(
                command, commandNode, "iconTransparency", globalDataPath, true, true, imageReader);
            await GuiPersistence.loadCommandImage(
                command, commandNode, "secondaryIconTransparency", globalDataPath, false, true, imageReader);
            command.applyTransparency();
            command.applySecondTransparency();

            context.addCommand(command);
        }
    }

    private static async loadCommandImage(
        command: WidgetCommand,
        commandNode: JsonNode,
        fieldName: string,
        globalDataPath: string,
        primary: boolean,
        transparency: boolean,
        imageReader: GuiPersistenceImageReader,
    ): Promise<void> {
        const filename: string | null = GuiPersistence.text(commandNode, fieldName);
        if (filename === null) {
            return;
        }

        try {
            const file: string = globalDataPath + "/" + filename;
            if (transparency) {
                const mask: RGBImageUncompressed = await imageReader.importRGB(file);
                if (primary) {
                    command.setIconTransparency(mask);
                } else {
                    command.setSecondaryIconTransparency(mask);
                }
            } else {
                const image: RGBAImageUncompressed = await imageReader.importRGBA(file);
                if (primary) {
                    command.setIcon(image);
                } else {
                    command.setSecondaryIcon(image);
                }
            }
        } catch (e) {
            console.error('Warning: could not read the image file "' + filename + '".');
            console.error(String(e));
        }
    }

    private static importVariables(variableNodes: JsonNode, context: Widget): void {
        if (!Array.isArray(variableNodes)) {
            return;
        }

        for (const variableNode of variableNodes as JsonNode[]) {
            const variable: WidgetVariable | null = GuiPersistence.buildVariable(variableNode);
            if (variable !== null) {
                context.addVariable(variable);
            }
        }
    }

    private static buildVariable(variableNode: JsonNode): WidgetVariable | null {
        const id: string = GuiPersistence.requiredText(variableNode, "id");
        const type: string | null = GuiPersistence.text(variableNode, "type");
        const range: string | null = GuiPersistence.text(variableNode, "range");
        const initialValue: string | null = GuiPersistence.text(variableNode, "initialValue");
        const lowerType: string | null = type === null ? null : type.toLowerCase();

        let variable: WidgetVariable | null = null;
        if (lowerType === "double") {
            variable = new WidgetDoubleVariable();
        } else if (lowerType === "vector3d" || lowerType === "vector3dd") {
            variable = new WidgetVector3DVariable();
        } else if (lowerType === "colorrgb") {
            variable = new WidgetColorRgbVariable();
        } else if (lowerType === "integer") {
            variable = new WidgetIntegerVariable();
        } else if (lowerType === "boolean") {
            variable = new WidgetBooleanVariable();
        } else if (lowerType === "string") {
            variable = new WidgetStringVariable();
        }

        if (variable === null) {
            return null;
        }

        variable.setName(id);
        if (range !== null) {
            variable.setValidRange(range);
        }
        if (initialValue !== null) {
            variable.setInitialvalue(initialValue);
        }
        return variable;
    }

    private static importMenubar(menubarNode: JsonNode, context: Widget): void {
        if (menubarNode === undefined || menubarNode === null) {
            return;
        }
        context.setMenubar(GuiPersistence.buildMenu(menubarNode, context, true));
    }

    private static importPopups(popupNodes: JsonNode, context: Widget): void {
        if (!Array.isArray(popupNodes)) {
            return;
        }

        for (const popupNode of popupNodes as JsonNode[]) {
            context.addPopupMenu(GuiPersistence.buildMenu(popupNode, context, true));
        }
    }

    private static buildMenu(menuNode: JsonNode, context: Widget, registerChildPopups: boolean): WidgetMenu {
        const menu = new WidgetMenu(context);
        menu.setName(GuiPersistence.requiredText(menuNode, "name"));

        const children: JsonNode = path(menuNode, "children");
        if (Array.isArray(children)) {
            for (const child of children as JsonNode[]) {
                const type: string | null = GuiPersistence.text(child, "type");
                if (type === "menu") {
                    const childMenu: WidgetMenu = GuiPersistence.buildMenu(child, context, registerChildPopups);
                    if (registerChildPopups) {
                        context.addPopupMenu(childMenu);
                    }
                    menu.addChild(childMenu);
                } else if (type === "item") {
                    menu.addChild(GuiPersistence.buildMenuItem(child, context));
                }
            }
        }

        return menu;
    }

    private static buildMenuItem(itemNode: JsonNode, context: Widget): WidgetMenuElement {
        const item = new WidgetMenuItem(context);
        const name: string | null = GuiPersistence.text(itemNode, "name");
        if (name !== null) {
            item.setName(name);
        }

        const modifiers: JsonNode = path(itemNode, "modifiers");
        if (Array.isArray(modifiers)) {
            for (const modifier of modifiers as JsonNode[]) {
                item.addModifier(asText(modifier));
            }
        }

        return item;
    }

    private static importButtonGroups(buttonGroupNodes: JsonNode, context: Widget): void {
        if (!Array.isArray(buttonGroupNodes)) {
            return;
        }

        for (const buttonGroupNode of buttonGroupNodes as JsonNode[]) {
            const group = new WidgetButtonGroup(context);
            group.setName(GuiPersistence.requiredText(buttonGroupNode, "name"));
            group.setShowIcons(GuiPersistence.booleanValue(buttonGroupNode, "showIcons", false));
            group.setShowText(GuiPersistence.booleanValue(buttonGroupNode, "showText", false));
            group.setTitle(GuiPersistence.booleanValue(buttonGroupNode, "showTitle", false));

            const direction: string | null = GuiPersistence.text(buttonGroupNode, "direction");
            if (direction === "horizontal") {
                group.setDirection(WidgetButtonGroup.HORIZONTAL);
            } else {
                group.setDirection(WidgetButtonGroup.VERTICAL);
            }

            const commands: JsonNode = path(buttonGroupNode, "commands");
            if (Array.isArray(commands)) {
                for (const command of commands as JsonNode[]) {
                    group.addCommandByName(asText(command));
                }
            }

            context.addButtonGroup(group);
        }
    }

    private static importDialogs(dialogNodes: JsonNode, context: Widget): void {
        if (!Array.isArray(dialogNodes)) {
            return;
        }

        for (const dialogNode of dialogNodes as JsonNode[]) {
            context.addDialog(GuiPersistence.buildDialog(dialogNode, context));
        }
    }

    private static buildDialog(dialogNode: JsonNode, context: Widget): WidgetDialog {
        const dialog = new WidgetDialog();
        dialog.setId(GuiPersistence.requiredText(dialogNode, "id"));
        dialog.setName(GuiPersistence.text(dialogNode, "name"));
        dialog.setCollapsable(GuiPersistence.booleanValue(dialogNode, "collapsable", false));

        const orientation: string | null = GuiPersistence.text(dialogNode, "orientation");
        if (orientation === "horizontal") {
            dialog.setOrientation(WidgetDialog.ORIENTATION_HORIZONTAL);
        } else if (orientation === "vertical") {
            dialog.setOrientation(WidgetDialog.ORIENTATION_VERTICAL);
        }

        GuiPersistence.addTextArray(path(dialogNode, "variables"), dialog.getPendingVariableNames());
        GuiPersistence.addTextArray(path(dialogNode, "commands"), dialog.getPendingCommandNames());
        GuiPersistence.addTextArray(path(dialogNode, "dialogRefs"), dialog.getPendingDialogRefNames());

        const children: JsonNode = path(dialogNode, "dialogs");
        if (Array.isArray(children)) {
            for (const child of children as JsonNode[]) {
                dialog.getChildren().push(GuiPersistence.buildDialog(child, context));
            }
        }

        return dialog;
    }

    private static addTextArray(source: JsonNode, destination: string[]): void {
        if (!Array.isArray(source)) {
            return;
        }
        for (const value of source as JsonNode[]) {
            destination.push(asText(value));
        }
    }

    private static importMessages(messagesNode: JsonNode, context: Widget): void {
        if (!isObject(messagesNode)) {
            return;
        }

        for (const [key, value] of Object.entries(messagesNode)) {
            context.addMessage(key, asText(value));
        }
    }

    private static text(node: JsonNode, fieldName: string): string | null {
        const value: JsonNode = isObject(node) ? node[fieldName] : undefined;
        if (value === undefined || value === null) {
            return null;
        }
        return asText(value);
    }

    private static requiredText(node: JsonNode, fieldName: string): string {
        const value: string | null = GuiPersistence.text(node, fieldName);
        if (value === null || value.length === 0) {
            throw new ExceptionWidgetBadName();
        }
        return value;
    }

    private static booleanValue(node: JsonNode, fieldName: string, defaultValue: boolean): boolean {
        const value: JsonNode = isObject(node) ? node[fieldName] : undefined;
        if (value === undefined || value === null) {
            return defaultValue;
        }
        if (typeof value === "boolean") {
            return value;
        }
        const text: string = asText(value).toLowerCase();
        return text === "true" || text === "on";
    }
}

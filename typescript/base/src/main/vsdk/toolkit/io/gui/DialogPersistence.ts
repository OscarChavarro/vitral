import { Widget } from "../../gui/widget/Widget.js";
import { WidgetDialog, WidgetDialogElement } from "../../gui/widget/WidgetDialog.js";

type JsonObject = Record<string, unknown>;

/** Reads the common declarative dialog schema used by each GUI port. */
export class DialogPersistence {
    public static importDialogs(source: string, widget: Widget): void {
        const root: unknown = JSON.parse(source);
        if (!DialogPersistence.isObject(root) || root.schemaVersion !== 1 || !Array.isArray(root.dialogs)) {
            throw new Error("dialogs.json: unsupported schemaVersion or dialogs");
        }
        const ids = new Set<string>();
        root.dialogs.forEach((node, index) => {
            if (!DialogPersistence.isObject(node)) throw new Error(`dialogs[${index}]: expected object`);
            const id = DialogPersistence.required(node, "id", `dialogs[${index}]`);
            if (ids.has(id) || widget.getDialogById(id) !== null) throw new Error(`dialogs[${index}]: duplicate dialog id ${id}`);
            ids.add(id);
            const dialog = new WidgetDialog();
            dialog.setId(id);
            dialog.setTitleKey(DialogPersistence.required(node, "titleKey", `dialogs[${index}]`));
            dialog.setModal(node.modal === true);
            if (!Array.isArray(node.content)) throw new Error(`dialogs[${index}].content: expected array`);
            const elementIds = new Set<string>();
            node.content.forEach((element, elementIndex) => dialog.addElement(
                DialogPersistence.element(element, `dialogs[${index}].content[${elementIndex}]`, elementIds),
            ));
            widget.addDialog(dialog);
        });
    }

    private static element(value: unknown, path: string, ids: Set<string>): WidgetDialogElement {
        if (!DialogPersistence.isObject(value)) throw new Error(`${path}: expected object`);
        const kind = DialogPersistence.required(value, "kind", path);
        const allowed = new Set(["column", "row", "textInput", "button", "tree", "action", "fileChooser", "fileFilter"]);
        if (!allowed.has(kind)) throw new Error(`${path}: unsupported kind ${kind}`);
        const id = DialogPersistence.optional(value, "id");
        if (id !== null && (ids.has(id) || !ids.add(id))) throw new Error(`${path}: duplicate element id ${id}`);
        const binding = DialogPersistence.optional(value, "binding");
        const action = DialogPersistence.optional(value, "action");
        if (["textInput", "tree", "fileFilter"].includes(kind) && binding === null) throw new Error(`${path}: missing binding`);
        if (["button", "action"].includes(kind) && action === null) throw new Error(`${path}: missing action`);
        const children = Array.isArray(value.children) ? value.children.map((child, index) => DialogPersistence.element(child, `${path}.children[${index}]`, ids)) : [];
        return new WidgetDialogElement(kind, id, DialogPersistence.optional(value, "labelKey"), binding, action, children);
    }

    private static required(value: JsonObject, field: string, path: string): string {
        const result = DialogPersistence.optional(value, field);
        if (result === null) throw new Error(`${path}: missing ${field}`);
        return result;
    }
    private static optional(value: JsonObject, field: string): string | null {
        const result = value[field];
        return typeof result === "string" && result.trim() !== "" ? result : null;
    }
    private static isObject(value: unknown): value is JsonObject {
        return value !== null && typeof value === "object" && !Array.isArray(value);
    }
}

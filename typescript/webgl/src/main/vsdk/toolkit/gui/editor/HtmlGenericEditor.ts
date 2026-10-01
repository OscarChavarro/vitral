import { GenericEditor, type ControlSpecification } from "@vitral/base";

/**
Port of `vsdk.toolkit.gui.editor.AwtGenericEditor`.

DOM presentation of a `GenericEditor`: a title, one labeled text field per
control specification and a message line. All the logic (reading,
validating and writing values) lives in `GenericEditor`; this class only
creates elements and forwards what the user types. As a Swing `JTextField`
fires its action on enter, a field is applied when the user presses enter.
*/
export class HtmlGenericEditor extends GenericEditor {
    private static readonly INVALID_BACKGROUND: string = "rgb(255, 200, 200)";

    private readonly container: HTMLElement;
    private readonly fields: Map<HTMLInputElement, ControlSpecification>;
    private rows: HTMLDivElement | null;
    private messageLabel: HTMLDivElement | null;

    /**
    @param container element that will hold the editor; its previous
    contents are removed on each `build`
    */
    public constructor(container: HTMLElement) {
        super();
        this.container = container;
        this.fields = new Map<HTMLInputElement, ControlSpecification>();
        this.rows = null;
        this.messageLabel = null;
    }

    protected override beginBuild(title: string): void {
        this.container.replaceChildren();
        this.fields.clear();

        const wrapper: HTMLDivElement = document.createElement("div");
        wrapper.className = "vitral-generic-editor";
        this.rows = document.createElement("div");
        this.rows.className = "vitral-generic-editor-rows";
        wrapper.appendChild(this.rows);
        this.container.appendChild(wrapper);

        const titleLabel: HTMLDivElement = document.createElement("div");
        titleLabel.className = "vitral-generic-editor-title";
        titleLabel.textContent = title.toUpperCase() + " EDITOR";
        this.rows.appendChild(titleLabel);
        this.messageLabel = document.createElement("div");
        this.messageLabel.className = "vitral-generic-editor-message";
        this.messageLabel.textContent = " ";
    }

    protected override addControl(specification: ControlSpecification, value: string): void {
        let label: string = specification.getLabel();
        if (specification.getIntervalText().length !== 0) {
            label += " " + specification.getIntervalText();
        }

        const field: HTMLInputElement = document.createElement("input");
        field.type = "text";
        field.className = "vitral-text-field";
        field.value = value;
        field.size = 10;
        field.addEventListener("keydown", (event: KeyboardEvent): void => {
            event.stopPropagation();
            if (event.key === "Enter") {
                this.actionPerformed(field);
            }
        });
        this.fields.set(field, specification);

        const row: HTMLDivElement = document.createElement("div");
        row.className = "vitral-generic-editor-row";
        const labelElement: HTMLLabelElement = document.createElement("label");
        labelElement.textContent = label + ": ";
        row.appendChild(labelElement);
        row.appendChild(field);
        this.rows!.appendChild(row);
    }

    protected override endBuild(): void {
        if (this.rows !== null && this.messageLabel !== null) {
            this.rows.appendChild(this.messageLabel);
        }
    }

    protected override showValidationMessage(message: string | null): void {
        if (this.messageLabel !== null) {
            this.messageLabel.textContent = message === null ? " " : message;
        }
    }

    protected override setControlValue(specification: ControlSpecification, value: string): void {
        for (const [field, fieldSpecification] of this.fields) {
            if (fieldSpecification === specification) {
                field.value = value;
                field.style.background = "";
            }
        }
    }

    protected override clearControls(message: string): void {
        this.container.replaceChildren();
        this.fields.clear();
        this.rows = null;
        this.messageLabel = null;
        const label: HTMLDivElement = document.createElement("div");
        label.className = "vitral-generic-editor-title";
        label.textContent = message;
        this.container.appendChild(label);
    }

    private actionPerformed(field: HTMLInputElement): void {
        const specification: ControlSpecification | undefined = this.fields.get(field);
        if (specification === undefined) {
            return;
        }

        // On success the entity emits UPDATED and `setControlValue`
        // refreshes the field
        if (!this.updateValue(specification, field.value)) {
            field.style.background = HtmlGenericEditor.INVALID_BACKGROUND;
        }
    }
}

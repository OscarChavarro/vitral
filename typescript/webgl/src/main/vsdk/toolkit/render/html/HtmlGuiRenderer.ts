import {
    WidgetBooleanVariable,
    WidgetButtonGroup,
    WidgetColorRgbVariable,
    WidgetCommand,
    WidgetDialog,
    WidgetDoubleVariable,
    WidgetIntegerVariable,
    WidgetMenu,
    WidgetMenuItem,
    WidgetStringVariable,
    WidgetVector3DVariable,
    type RGBAImageUncompressed,
    type Widget,
    type WidgetMenuElement,
    type WidgetVariable,
} from "@vitral/base";
import { HtmlActionEvent, type HtmlActionListener } from "./HtmlActionListener.js";
import { HtmlCollapsablePanel } from "./HtmlCollapsablePanel.js";
import { HtmlImageRenderer } from "./HtmlImageRenderer.js";
import { HtmlMenuBar, HtmlPopupMenu } from "./HtmlMenus.js";

/**
A menu with its title (Swing's `JMenu`), as built by `buildPopupMenu`.
*/
export class HtmlMenu {
    public readonly text: string;
    public mnemonic: string;
    public readonly popup: HtmlPopupMenu;

    public constructor(text: string, popup: HtmlPopupMenu) {
        this.text = text;
        this.mnemonic = "\0";
        this.popup = popup;
    }

    /**
    Swing's `AbstractButton.setMnemonic`.
    */
    public setMnemonic(mnemonic: string): void {
        this.mnemonic = mnemonic;
    }
}

/**
Placement of a component in a grid (the part of Swing's `GridBagConstraints`
the configurations below use).
*/
export class HtmlGridConstraints {
    public gridx: number = 0;
    public gridy: number = 0;
    public gridwidth: number = 1;
    public gridheight: number = 1;
    public center: boolean = false;
}

/**
Port of `vsdk.toolkit.render.swing.SwingGuiRenderer`.

Builds the DOM presentation of the GUI definitions of a `Widget` (menu bars,
popup menus, button groups, variables and dialogs), as the Swing renderer
builds Swing components from them. The presentation is styled by the
`vitral-*` classes of `styles/vitral-html-gui.scss` of this package.

Runtime boundaries:
  - Swing's `JMenuBar`, `JMenu` and `JMenuItem` are the `HtmlMenuBar`,
    `HtmlMenu` and `HtmlPopupMenu` of `HtmlMenus`.
  - A button reports its command to the executor with an `HtmlActionEvent`
    whose source is the `<button>`, named (`button.name`) after the id of its
    command, as Swing names the `JButton`; a menu item reports its command
    name, as `SwingEventListener` does.
  - Icons are exported to images with `HtmlImageRenderer`, as Swing exports
    them with `AwtRGBAImageUncompressedRenderer`.
  - Swing's layouts are CSS layouts: `FlowLayout` and `BoxLayout` are flex
    boxes, `GridLayout` and `GridBagLayout` are CSS grids.
*/
export class HtmlGuiRenderer {
    private constructor() {}

    /**
    Swing keeps a mnemonic only for a letter or a digit
    (`convertMnemonic2Swing`).
    @return the mnemonic in upper case, or `"\0"` if it is not a letter or a
    digit
    */
    private static convertMnemonic2Swing(input: string): string {
        const uc: string = input.toUpperCase();

        if (uc.length === 1 && /[0-9A-Z]/.test(uc)) {
            return uc;
        }
        return "\0";
    }

    public static buildPopupMenu(context: Widget, name: string | null, executor: HtmlActionListener): HtmlMenu {
        const widgetPopup: HtmlMenu = new HtmlMenu(name ?? "", new HtmlPopupMenu());

        const menu: WidgetMenu | null = context.getPopup(name);

        if (menu === null) {
            widgetPopup.popup.addItem("Popup menu not found on GUI", "\0", (): void => {});
        }
        else {
            const children: WidgetMenuElement[] = menu.getChildren();

            for (let i: number = 0; i < children.length; i++) {
                const element: WidgetMenuElement = children[i]!;
                if (element instanceof WidgetMenu) {
                    const widgetSubmenu: HtmlMenu = HtmlGuiRenderer.buildPopupMenu(context, element.getName(), executor);
                    widgetPopup.popup.addSubmenu(widgetSubmenu.text, widgetSubmenu.mnemonic, widgetSubmenu.popup);
                }
                else if (element instanceof WidgetMenuItem) {
                    if (element.isSeparator()) {
                        widgetPopup.popup.addSeparator();
                    }
                    else {
                        const commandName: string = element.getCommandName();
                        const mnemonic: string = HtmlGuiRenderer.convertMnemonic2Swing(element.getMnemonic());
                        widgetPopup.popup.addItem(element.getName(), mnemonic, (): void => {
                            // SwingEventListener: the event carries the command name
                            executor.actionPerformed(new HtmlActionEvent(widgetPopup, 1, commandName));
                        });
                    }
                }
            }
        }
        return widgetPopup;
    }

    public static buildButtonGroup(context: Widget, name: string, executor: HtmlActionListener): HTMLDivElement {
        const frame: HTMLDivElement = document.createElement("div");
        frame.className = "vitral-button-group";
        const group: WidgetButtonGroup | null = context.getButtonGroup(name);

        if (group === null) {
            frame.classList.add("vitral-button-group-error");
            const l: HTMLSpanElement = document.createElement("span");
            l.textContent = "No ButtonGroup \"" + name + "\" found in GUI";
            frame.appendChild(l);
            return frame;
        }

        if (group.getDirection() === WidgetButtonGroup.HORIZONTAL) {
            frame.classList.add("vitral-button-group-horizontal");
        }
        else {
            frame.classList.add("vitral-button-group-vertical");
        }

        const list: WidgetCommand[] = group.getCommands();

        for (let i: number = 0; i < list.length; i++) {
            const element: WidgetCommand = list[i]!;
            const b: HTMLButtonElement = document.createElement("button");
            b.type = "button";
            b.className = "vitral-button";

            // Button goes with images ... if any inside command
            const img: RGBAImageUncompressed | null = element.getIcon();

            if (img === null || !group.isShowIconsSet()) {
                b.textContent = element.getName() ?? "";
            }
            else {
                b.classList.add("vitral-button-icon");
                const primaryIcon: string = HtmlImageRenderer.exportToDataUrl(img);
                const icon: HTMLImageElement = document.createElement("img");
                icon.src = primaryIcon;
                icon.alt = element.getName() ?? "";
                icon.draggable = false;
                b.appendChild(icon);
                const secondary: RGBAImageUncompressed | null = element.getSecondaryIcon();
                if (secondary !== null) {
                    const secondaryIcon: string = HtmlImageRenderer.exportToDataUrl(secondary);
                    b.addEventListener("click", (): void => {
                        icon.src = icon.src === primaryIcon ? secondaryIcon : primaryIcon;
                    });
                }
            }

            b.name = element.getId() ?? "";

            if (group.isShowTextSet()) {
                const text: HTMLSpanElement = document.createElement("span");
                text.className = "vitral-button-text";
                text.textContent = element.getName() ?? "";
                if (img === null || !group.isShowIconsSet()) {
                    b.textContent = "";
                }
                b.appendChild(text);
            }

            const brief: string | null = element.getBriefDescription();
            if (brief !== null) {
                b.title = brief;
            }
            b.addEventListener("click", (): void => {
                executor.actionPerformed(new HtmlActionEvent(b, 1, b.textContent ?? ""));
            });
            frame.appendChild(b);
        }

        return frame;
    }

    /**
    This method construct the DOM menu structure for the menu contained in
    data context which has the specified name. If null is given as name, the
    context's menubar is used. In this way, different frame windows could
    have different menubars.

    The builded menu is supposed to be used as a menubar on top of the page
    area of an application.

    \todo : permit the selection of a different name menu
    @param context GUI definition
    @param _name name of the menu (unused, as in Java)
    @param executor receives the commands of the items
    @return the menu bar
    */
    public static buildMenubar(context: Widget | null, _name: string | null, executor: HtmlActionListener): HtmlMenuBar {
        let menubar: WidgetMenu | null = null;
        let errorMenu: string | null = null;

        if (context !== null) {
            menubar = context.getMenubar();
        }
        else {
            errorMenu = "No Widget specified!";
        }
        if (menubar === null) {
            errorMenu = "No menubar in GUI!";
        }

        const widgetMenubar: HtmlMenuBar = new HtmlMenuBar();

        if (menubar === null || errorMenu !== null || context === null) {
            const widgetPopup: HtmlPopupMenu = new HtmlPopupMenu();
            widgetPopup.addItem("Exit", "\0", (): void => {
                executor.actionPerformed(new HtmlActionEvent(widgetPopup, 1, "IDC_FILE_QUIT"));
            });
            widgetMenubar.addMenu(errorMenu ?? "", "\0", widgetPopup);
        }
        else {
            const children: WidgetMenuElement[] = menubar.getChildren();

            for (let i: number = 0; i < children.length; i++) {
                const element: WidgetMenuElement = children[i]!;
                if (element instanceof WidgetMenu) {
                    const widgetPopup: HtmlMenu = HtmlGuiRenderer.buildPopupMenu(context, element.getName(), executor);
                    const mnemonic: string = HtmlGuiRenderer.convertMnemonic2Swing(element.getMnemonic());
                    if (mnemonic !== "\0") {
                        widgetPopup.setMnemonic(mnemonic);
                    }
                    widgetMenubar.addMenu(widgetPopup.text, widgetPopup.mnemonic, widgetPopup.popup);
                }
            }
        }
        return widgetMenubar;
    }

    ///codeOscar
    // Code Oz
    public static buildBooleanVariable(v: WidgetBooleanVariable, executor: HtmlActionListener): HTMLDivElement {
        const p: HTMLDivElement = HtmlGuiRenderer.createPanel();

        const optionA: HTMLButtonElement = document.createElement("button");
        optionA.type = "button";
        optionA.className = "vitral-button";
        optionA.textContent = "./etc/1.jpg";

        const label: HTMLLabelElement = document.createElement("label");
        const cb: HTMLInputElement = document.createElement("input");
        cb.type = "checkbox";
        cb.addEventListener("change", (): void => {
            executor.actionPerformed(new HtmlActionEvent(cb, 1, v.getName() ?? ""));
        });
        label.appendChild(cb);
        label.appendChild(document.createTextNode(v.getName() ?? ""));
        p.appendChild(label);
        // As in Java, the button is added (and so moved) after the check box
        p.appendChild(optionA);

        return p;
    }

    private static buildNumberWidget(subname: string, executor: HtmlActionListener): HTMLDivElement {
        const p: HTMLDivElement = HtmlGuiRenderer.createPanel();
        const l: HTMLLabelElement = document.createElement("label");
        const t: HTMLInputElement = document.createElement("input");
        t.type = "text";
        t.value = "0.0";
        t.addEventListener("keydown", (event: KeyboardEvent): void => {
            // A JTextField fires its action when the user presses enter
            if (event.key === "Enter") {
                executor.actionPerformed(new HtmlActionEvent(t, 1, t.value));
            }
        });
        l.textContent = subname;
        p.appendChild(l);
        p.appendChild(t);
        return p;
    }

    public static buildVector3DVariable(v: WidgetVector3DVariable, executor: HtmlActionListener): HTMLDivElement {
        const p: HTMLDivElement = HtmlGuiRenderer.createPanel();
        const lv: HTMLSpanElement = document.createElement("span");
        lv.textContent = v.getName() ?? "";
        p.appendChild(lv);
        p.appendChild(HtmlGuiRenderer.buildNumberWidget("X:", executor));
        p.appendChild(HtmlGuiRenderer.buildNumberWidget("Y:", executor));
        p.appendChild(HtmlGuiRenderer.buildNumberWidget("Z:", executor));
        return p;
    }

    public static buildColorRgbVariable(_v: WidgetColorRgbVariable, executor: HtmlActionListener): HTMLDivElement {
        const p: HTMLDivElement = HtmlGuiRenderer.createPanel();
        p.appendChild(HtmlGuiRenderer.buildNumberWidget("R:", executor));
        p.appendChild(HtmlGuiRenderer.buildNumberWidget("G:", executor));
        p.appendChild(HtmlGuiRenderer.buildNumberWidget("B:", executor));
        return p;
    }

    public static buildDoubleVariable(v: WidgetDoubleVariable, executor: HtmlActionListener): HTMLDivElement {
        const p: HTMLDivElement = HtmlGuiRenderer.createPanel();
        p.appendChild(HtmlGuiRenderer.buildNumberWidget(v.getName() + ":", executor));
        return p;
    }

    public static buildIntegerVariable(v: WidgetIntegerVariable, executor: HtmlActionListener): HTMLDivElement {
        const p: HTMLDivElement = HtmlGuiRenderer.createPanel();
        p.appendChild(HtmlGuiRenderer.buildNumberWidget(v.getName() ?? "", executor));
        return p;
    }

    public static buildStringVariable(v: WidgetStringVariable, executor: HtmlActionListener): HTMLDivElement {
        const p: HTMLDivElement = HtmlGuiRenderer.createPanel();
        p.appendChild(HtmlGuiRenderer.buildNumberWidget(v.getName() ?? "", executor));
        return p;
    }

    public static buildCommandButton(c: WidgetCommand, executor: HtmlActionListener): HTMLButtonElement {
        const b: HTMLButtonElement = document.createElement("button");
        b.type = "button";
        b.className = "vitral-button";
        b.textContent = c.getName() ?? "";
        b.title = "Test Message: " + c.getName();
        b.addEventListener("click", (): void => {
            executor.actionPerformed(new HtmlActionEvent(b, 1, b.textContent ?? ""));
        });
        return b;
    }

    public static buildVariable(v: WidgetVariable | null, executor: HtmlActionListener): HTMLDivElement {
        let containingPanelWidget: HTMLDivElement = HtmlGuiRenderer.createPanel();
        if (v === null) {
            const l: HTMLSpanElement = document.createElement("span");
            l.textContent = "NULL Variable ";
            containingPanelWidget.appendChild(l);
            return containingPanelWidget;
        }

        if (v instanceof WidgetBooleanVariable) {
            containingPanelWidget = HtmlGuiRenderer.buildBooleanVariable(v, executor);
        }
        else if (v instanceof WidgetVector3DVariable) {
            containingPanelWidget = HtmlGuiRenderer.buildVector3DVariable(v, executor);
        }
        else if (v instanceof WidgetColorRgbVariable) {
            containingPanelWidget = HtmlGuiRenderer.buildColorRgbVariable(v, executor);
        }
        else if (v instanceof WidgetDoubleVariable) {
            containingPanelWidget = HtmlGuiRenderer.buildDoubleVariable(v, executor);
        }
        else if (v instanceof WidgetIntegerVariable) {
            containingPanelWidget = HtmlGuiRenderer.buildIntegerVariable(v, executor);
        }
        else if (v instanceof WidgetStringVariable) {
            containingPanelWidget = HtmlGuiRenderer.buildStringVariable(v, executor);
        }
        else {
            const l: HTMLSpanElement = document.createElement("span");
            l.textContent = "Variable of type " + v.constructor.name + " not supported yet";
            containingPanelWidget.appendChild(l);
        }
        return containingPanelWidget;
    }

    private static place(panel: HTMLElement, component: HTMLElement, cons: HtmlGridConstraints): void {
        component.style.gridColumn = (cons.gridx + 1) + " / span " + cons.gridwidth;
        component.style.gridRow = (cons.gridy + 1) + " / span " + cons.gridheight;
        if (cons.center) {
            component.style.justifySelf = "center";
            component.style.alignSelf = "center";
        }
        panel.appendChild(component);
    }

    private static placeAt(panel: HTMLElement, component: HTMLElement, cons: HtmlGridConstraints,
                           gridx: number, gridy: number, center: boolean = false): void {
        cons.gridx = gridx;
        cons.gridy = gridy;
        cons.gridwidth = 1;
        cons.gridheight = 1;
        if (center) {
            cons.center = true;
        }
        HtmlGuiRenderer.place(panel, component, cons);
    }

    public static buildGuiCommandConfiguration(panel: HTMLElement, aux: string, cons: HtmlGridConstraints,
                                               c: WidgetCommand, executor: HtmlActionListener): void {
        const name: string | null = c.getName();
        const button = (): HTMLButtonElement => HtmlGuiRenderer.buildCommandButton(c, executor);
        panel.classList.add("vitral-grid-panel");

        if (aux === "Camera Rotations") {
            if (name === "UP") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 1, 0, true);
            }
            else if (name === "DOWN") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 1, 1);
            }
            else if (name === "LEFT") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 0, 1);
            }
            else if (name === "RIGHT") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 2, 1);
            }
        }
        else if (aux === "Camera Position Movements" || aux === "Terrain Position" ||
                 aux === "Terrain Visualization") {
            if (name === "UP") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 1, 0, true);
            }
            else if (name === "DOWN") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 1, 1);
            }
            else if (name === "LEFT") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 0, 1);
            }
            else if (name === "RIGHT") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 2, 1);
            }
            else if (name === "BACK") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 0, 0);
            }
            else if (name === "FORWARD") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 2, 0);
            }
        }
        else if (aux === "Lines Visualization") {
            if (name === "LEFT") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 0, 1);
            }
            else if (name === "RIGHT") {
                HtmlGuiRenderer.placeAt(panel, button(), cons, 2, 1);
            }
        }
    }

    public static buildGuiVariableConfiguration(panel: HTMLElement, aux: string, cons: HtmlGridConstraints,
                                                gui: Widget, executor: HtmlActionListener): void {
        const variable = (): HTMLDivElement => HtmlGuiRenderer.buildVariable(gui.getVariableByName(aux), executor);
        panel.classList.add("vitral-grid-panel");

        if (aux === "MODE_1") {
            HtmlGuiRenderer.placeAt(panel, variable(), cons, 0, 0);
        }
        else if (aux === "MODE_2") {
            HtmlGuiRenderer.placeAt(panel, variable(), cons, 10, 11);
        }
        else if (aux === "MODE_3") {
            HtmlGuiRenderer.placeAt(panel, variable(), cons, 0, 2);
        }
        else if (aux === "color") {
            HtmlGuiRenderer.placeAt(panel, variable(), cons, 0, 2);
        }
        else if (aux === "TYPE_LINES") {
            HtmlGuiRenderer.placeAt(panel, variable(), cons, 0, 0);
        }
        else if (aux === "TOGGLE") {
            HtmlGuiRenderer.placeAt(panel, variable(), cons, 2, 0);
        }
    }

    public static buildDialog(d: WidgetDialog | null, gui: Widget, executor: HtmlActionListener): HTMLDivElement {
        const panel: HTMLDivElement = HtmlGuiRenderer.createPanel();

        if (d === null) {
            const l: HTMLSpanElement = document.createElement("span");
            l.textContent = "NULL Dialog ";
            panel.appendChild(l);
            return panel;
        }

        panel.title = "Test dialog: " + d.getId();

        //- Define geometry management for panel ---------------------------
        const count: number = d.getPendingVariableNames().length +
            d.getPendingCommandNames().length +
            d.getPendingDialogRefNames().length;
        let ncols: number;
        let nrows: number;
        if (d.getOrientation() === WidgetDialog.ORIENTATION_HORIZONTAL) {
            ncols = count;
            nrows = 1;
        }
        else {
            ncols = 1;
            nrows = count;
        }
        panel.classList.add("vitral-grid-panel");
        panel.style.gridTemplateColumns = "repeat(" + Math.max(1, ncols) + ", 1fr)";
        panel.style.gridTemplateRows = "repeat(" + Math.max(1, nrows) + ", auto)";

        //------------------------------------------------------------------

        for (const vn of d.getPendingVariableNames()) {
            panel.appendChild(HtmlGuiRenderer.buildVariable(gui.getVariableByName(vn), executor));
        }

        for (const commandName of d.getPendingCommandNames()) {
            const c: WidgetCommand = new WidgetCommand();
            c.setName(commandName);
            panel.appendChild(HtmlGuiRenderer.buildCommandButton(c, executor));
        }

        for (const dialogName of d.getPendingDialogRefNames()) {
            const dial: WidgetDialog = new WidgetDialog();
            dial.setId(dialogName);
            panel.appendChild(HtmlGuiRenderer.buildDialog(dial, gui, executor));
        }
        if (d.isCollapsable()) {
            const pan: HtmlCollapsablePanel = new HtmlCollapsablePanel(d.getName() ?? "", panel);
            return pan.element;
        }
        return panel;
    }

    private static createPanel(): HTMLDivElement {
        const p: HTMLDivElement = document.createElement("div");
        p.className = "vitral-panel";
        return p;
    }
}

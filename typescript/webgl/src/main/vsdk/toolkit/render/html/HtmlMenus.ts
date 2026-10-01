/**
DOM menus for the GUIs of the toolkit: the counterpart of Swing's
`JPopupMenu`, `JMenu`, `JMenuItem`, `JRadioButtonMenuItem` and `JMenuBar`,
which the Swing GUI renderer builds from the `Widget` definitions.

A popup is a `div` posted on the document body (or on the full screen element,
see `HtmlOverlayHost`) with fixed position, so it
floats over everything (as Swing's heavyweight popups float over an OpenGL
canvas). As in Swing:
  - Only one tree of popups is open at a time; a press outside it closes it
    (the press still reaches what is below, which can ask a
    `PopupDismissClickFilter`-like filter whether to ignore it).
  - The keyboard navigates it: up and down move, enter and space choose, right
    opens a submenu, left and escape close the current level, and the
    mnemonic letter of an item chooses it.
  - The mnemonic letter of each text is underlined.

Texts are plain text: they are never parsed as HTML.
*/

import { attachToOverlayHost, detachFromOverlayHost } from "./HtmlOverlayHost.js";

/** Kinds of the items of a popup. */
export type HtmlMenuItemKind = "command" | "radio" | "submenu" | "separator";

/**
One item of an `HtmlPopupMenu`.
*/
export class HtmlMenuItem {
    public readonly element: HTMLElement;
    public readonly kind: HtmlMenuItemKind;
    public readonly mnemonic: string;
    public readonly submenu: HtmlPopupMenu | null;
    private readonly action: (() => void) | null;
    private selected: boolean;

    /** @internal */
    public constructor(element: HTMLElement, kind: HtmlMenuItemKind, mnemonic: string,
                       action: (() => void) | null, submenu: HtmlPopupMenu | null, selected: boolean) {
        this.element = element;
        this.kind = kind;
        this.mnemonic = mnemonic;
        this.action = action;
        this.submenu = submenu;
        this.selected = selected;
    }

    /**
    @return true for a radio item that is marked
    */
    public isSelected(): boolean {
        return this.selected;
    }

    /**
    Marks or unmarks a radio item.
    */
    public setSelected(selected: boolean): void {
        this.selected = selected;
        this.element.setAttribute("aria-checked", selected ? "true" : "false");
        this.element.classList.toggle("vitral-menu-item-checked", selected);
    }

    /** @internal Runs the action of the item. */
    public fire(): void {
        if (this.action !== null) {
            this.action();
        }
    }
}

/**
Writes a text with its mnemonic letter underlined (the first occurrence of
the letter, ignoring case, as Swing does).
@param target element that receives the text
@param text text to write
@param mnemonic mnemonic letter, or `"\0"` (or empty) for none
*/
export function appendMnemonicText(target: HTMLElement, text: string, mnemonic: string): void {
    const index: number = mnemonic.length === 1 && mnemonic !== "\0"
        ? text.toLowerCase().indexOf(mnemonic.toLowerCase())
        : -1;

    if (index < 0) {
        target.appendChild(document.createTextNode(text));
        return;
    }
    target.appendChild(document.createTextNode(text.substring(0, index)));
    const underlined: HTMLElement = document.createElement("u");
    underlined.textContent = text.charAt(index);
    target.appendChild(underlined);
    target.appendChild(document.createTextNode(text.substring(index + 1)));
}

/**
The open popups, from the root to the deepest submenu, and the global
listeners that close them.
*/
class HtmlMenuManager {
    private static readonly openMenus: HtmlPopupMenu[] = [];
    private static listening: boolean = false;

    public static opened(menu: HtmlPopupMenu, parent: HtmlPopupMenu | null): void {
        if (parent === null) {
            HtmlMenuManager.closeAll();
        }
        else {
            // Close the submenus of the parent that are open
            const index: number = HtmlMenuManager.openMenus.indexOf(parent);
            while (HtmlMenuManager.openMenus.length > index + 1) {
                HtmlMenuManager.openMenus[HtmlMenuManager.openMenus.length - 1]!.hide();
            }
        }
        HtmlMenuManager.openMenus.push(menu);
        HtmlMenuManager.listen();
    }

    public static closed(menu: HtmlPopupMenu): void {
        const index: number = HtmlMenuManager.openMenus.indexOf(menu);
        if (index >= 0) {
            HtmlMenuManager.openMenus.splice(index, 1);
        }
    }

    public static closeAll(): void {
        while (HtmlMenuManager.openMenus.length > 0) {
            HtmlMenuManager.openMenus[0]!.hide();
        }
    }

    public static deepest(): HtmlPopupMenu | null {
        const n: number = HtmlMenuManager.openMenus.length;
        return n > 0 ? HtmlMenuManager.openMenus[n - 1]! : null;
    }

    public static isInsideOpenMenu(target: EventTarget | null): boolean {
        if (!(target instanceof Node)) {
            return false;
        }
        for (const menu of HtmlMenuManager.openMenus) {
            if (menu.element.contains(target) || menu.isOwnedBy(target)) {
                return true;
            }
        }
        return false;
    }

    private static listen(): void {
        if (HtmlMenuManager.listening) {
            return;
        }
        HtmlMenuManager.listening = true;
        document.addEventListener("pointerdown", (event: PointerEvent): void => {
            if (HtmlMenuManager.openMenus.length > 0 && !HtmlMenuManager.isInsideOpenMenu(event.target)) {
                HtmlMenuManager.closeAll();
            }
        }, true);
        document.addEventListener("keydown", (event: KeyboardEvent): void => {
            const menu: HtmlPopupMenu | null = HtmlMenuManager.deepest();
            if (menu !== null && menu.processKey(event)) {
                event.preventDefault();
                event.stopPropagation();
            }
        }, true);
        window.addEventListener("blur", (): void => HtmlMenuManager.closeAll());
        window.addEventListener("resize", (): void => HtmlMenuManager.closeAll());
    }
}

/**
A popup menu (Swing's `JPopupMenu`, also the popup of a `JMenu`).
*/
export class HtmlPopupMenu {
    public readonly element: HTMLDivElement;
    private readonly items: HtmlMenuItem[] = [];
    private readonly closeListeners: (() => void)[] = [];
    private parentMenu: HtmlPopupMenu | null = null;
    private owner: HTMLElement | null = null;
    private activeIndex: number = -1;
    private visible: boolean = false;
    /// Left / right keys of a menubar menu move to the neighbor menus
    private horizontalNavigation: ((direction: number) => void) | null = null;

    public constructor() {
        this.element = document.createElement("div");
        this.element.className = "vitral-menu";
        this.element.setAttribute("role", "menu");
        this.element.tabIndex = -1;
    }

    /**
    Adds an item that runs an action (Swing's `JMenuItem`).
    @param text text of the item
    @param mnemonic mnemonic letter, or `"\0"` for none
    @param action what the item does
    @return the item
    */
    public addItem(text: string, mnemonic: string, action: () => void): HtmlMenuItem {
        const element: HTMLElement = this.createItemElement("menuitem", text, mnemonic);
        return this.register(new HtmlMenuItem(element, "command", mnemonic, action, null, false));
    }

    /**
    Adds a radio item (Swing's `JRadioButtonMenuItem`).
    @param text text of the item
    @param mnemonic mnemonic letter, or `"\0"` for none
    @param selected true if the item is marked
    @param action what the item does
    @return the item
    */
    public addRadioItem(text: string, mnemonic: string, selected: boolean, action: () => void): HtmlMenuItem {
        const element: HTMLElement = this.createItemElement("menuitemradio", text, mnemonic);
        const item: HtmlMenuItem = new HtmlMenuItem(element, "radio", mnemonic, action, null, selected);
        item.setSelected(selected);
        return this.register(item);
    }

    /**
    Adds an item that opens a submenu (Swing's `JMenu` inside a menu).
    @param text text of the item
    @param mnemonic mnemonic letter, or `"\0"` for none
    @param submenu the submenu
    @return the item
    */
    public addSubmenu(text: string, mnemonic: string, submenu: HtmlPopupMenu): HtmlMenuItem {
        const element: HTMLElement = this.createItemElement("menuitem", text, mnemonic);
        element.setAttribute("aria-haspopup", "menu");
        element.classList.add("vitral-menu-item-submenu");
        return this.register(new HtmlMenuItem(element, "submenu", mnemonic, null, submenu, false));
    }

    /**
    Adds a separator line.
    */
    public addSeparator(): void {
        const element: HTMLElement = document.createElement("div");
        element.className = "vitral-menu-separator";
        element.setAttribute("role", "separator");
        this.element.appendChild(element);
        this.items.push(new HtmlMenuItem(element, "separator", "\0", null, null, false));
    }

    /**
    @return the items, in order (separators included)
    */
    public getItems(): readonly HtmlMenuItem[] {
        return this.items;
    }

    /**
    @param listener called each time the menu closes
    */
    public addCloseListener(listener: () => void): void {
        this.closeListeners.push(listener);
    }

    /**
    @param navigation called with -1 / +1 when left / right are pressed in
    this menu and it is not a submenu (used by the menubar)
    */
    public setHorizontalNavigation(navigation: ((direction: number) => void) | null): void {
        this.horizontalNavigation = navigation;
    }

    /**
    @return true if the menu is shown
    */
    public isVisible(): boolean {
        return this.visible;
    }

    /** @internal true if the target belongs to the element that opened the menu */
    public isOwnedBy(target: Node): boolean {
        return this.owner !== null && this.owner.contains(target);
    }

    /**
    Shows the menu with its upper left corner at a point of the window,
    moved if needed so it fits in the window.
    @param clientX horizontal position, in CSS pixels of the window
    @param clientY vertical position, in CSS pixels of the window
    @param parent menu this one is a submenu of, or null for a root menu
    @param owner element that opened the menu (presses over it do not close
    the menu), or null
    */
    public show(clientX: number, clientY: number, parent: HtmlPopupMenu | null = null,
                owner: HTMLElement | null = null): void {
        HtmlMenuManager.opened(this, parent);
        this.parentMenu = parent;
        this.owner = owner;
        this.visible = true;
        this.activeIndex = -1;
        for (const item of this.items) {
            item.element.classList.remove("vitral-menu-item-active");
        }
        attachToOverlayHost(this.element);
        this.element.style.left = "0px";
        this.element.style.top = "0px";

        const width: number = this.element.offsetWidth;
        const height: number = this.element.offsetHeight;
        const x: number = Math.max(0, Math.min(clientX, window.innerWidth - width));
        const y: number = Math.max(0, Math.min(clientY, window.innerHeight - height));
        this.element.style.left = x + "px";
        this.element.style.top = y + "px";
        this.element.focus({ preventScroll: true });
    }

    /**
    Hides the menu and its open submenus.
    */
    public hide(): void {
        if (!this.visible) {
            return;
        }
        for (const item of this.items) {
            if (item.submenu !== null) {
                item.submenu.hide();
            }
        }
        this.visible = false;
        HtmlMenuManager.closed(this);
        detachFromOverlayHost(this.element);
        for (const listener of this.closeListeners) {
            listener();
        }
    }

    /**
    Makes an item the one the keyboard works over (Swing's
    `MenuSelectionManager.setSelectedPath`).
    @param item an item of this menu
    */
    public setActiveItem(item: HtmlMenuItem | null): void {
        this.setActiveIndex(item === null ? -1 : this.items.indexOf(item));
    }

    /**
    Hides every open menu.
    */
    public static closeAll(): void {
        HtmlMenuManager.closeAll();
    }

    /** @internal Processes a key over the deepest open menu. */
    public processKey(event: KeyboardEvent): boolean {
        switch (event.key) {
            case "ArrowDown":
                this.moveActive(1);
                return true;
            case "ArrowUp":
                this.moveActive(-1);
                return true;
            case "ArrowRight": {
                const item: HtmlMenuItem | undefined = this.items[this.activeIndex];
                if (item !== undefined && item.submenu !== null) {
                    this.openSubmenu(item);
                    item.submenu.moveActive(1);
                }
                else if (this.horizontalNavigation !== null) {
                    this.horizontalNavigation(1);
                }
                else if (this.rootMenu().horizontalNavigation !== null) {
                    this.rootMenu().horizontalNavigation!(1);
                }
                return true;
            }
            case "ArrowLeft":
                if (this.parentMenu !== null) {
                    this.hide();
                }
                else if (this.horizontalNavigation !== null) {
                    this.horizontalNavigation(-1);
                }
                return true;
            case "Escape":
                this.hide();
                return true;
            case "Enter":
            case " ": {
                const item: HtmlMenuItem | undefined = this.items[this.activeIndex];
                if (item !== undefined) {
                    this.choose(item);
                }
                return true;
            }
            default:
                break;
        }
        if (event.key.length === 1 && !event.ctrlKey && !event.metaKey) {
            const letter: string = event.key.toLowerCase();
            for (const item of this.items) {
                if (item.kind !== "separator" && item.mnemonic.toLowerCase() === letter) {
                    this.setActiveItem(item);
                    this.choose(item);
                    return true;
                }
            }
        }
        return false;
    }

    private rootMenu(): HtmlPopupMenu {
        let menu: HtmlPopupMenu = this;
        while (menu.parentMenu !== null) {
            menu = menu.parentMenu;
        }
        return menu;
    }

    private createItemElement(role: string, text: string, mnemonic: string): HTMLElement {
        const element: HTMLElement = document.createElement("div");
        element.className = "vitral-menu-item";
        element.setAttribute("role", role);
        const label: HTMLElement = document.createElement("span");
        label.className = "vitral-menu-item-text";
        appendMnemonicText(label, text, mnemonic);
        element.appendChild(label);
        return element;
    }

    private register(item: HtmlMenuItem): HtmlMenuItem {
        this.items.push(item);
        this.element.appendChild(item.element);
        item.element.addEventListener("pointerenter", (): void => {
            this.setActiveItem(item);
            if (item.submenu !== null) {
                this.openSubmenu(item);
            }
            else {
                this.closeSubmenus();
            }
        });
        item.element.addEventListener("click", (event: MouseEvent): void => {
            event.stopPropagation();
            this.choose(item);
        });
        return item;
    }

    private choose(item: HtmlMenuItem): void {
        if (item.kind === "separator") {
            return;
        }
        if (item.submenu !== null) {
            this.openSubmenu(item);
            item.submenu.moveActive(1);
            return;
        }
        if (item.kind === "radio") {
            for (const other of this.items) {
                if (other.kind === "radio") {
                    other.setSelected(other === item);
                }
            }
        }
        HtmlMenuManager.closeAll();
        item.fire();
    }

    private openSubmenu(item: HtmlMenuItem): void {
        if (item.submenu === null || item.submenu.isVisible()) {
            return;
        }
        const box: DOMRect = item.element.getBoundingClientRect();
        item.submenu.show(box.right, box.top, this, item.element);
    }

    private closeSubmenus(): void {
        for (const item of this.items) {
            if (item.submenu !== null) {
                item.submenu.hide();
            }
        }
    }

    private moveActive(step: number): void {
        const n: number = this.items.length;
        if (n === 0) {
            return;
        }
        let index: number = this.activeIndex;
        for (let tries: number = 0; tries < n; tries++) {
            index = index < 0 ? (step > 0 ? 0 : n - 1) : (index + step + n) % n;
            if (this.items[index]!.kind !== "separator") {
                this.setActiveIndex(index);
                return;
            }
        }
    }

    private setActiveIndex(index: number): void {
        this.activeIndex = index;
        this.items.forEach((item: HtmlMenuItem, i: number): void => {
            item.element.classList.toggle("vitral-menu-item-active", i === index);
        });
    }
}

/**
A menu bar (Swing's `JMenuBar`): a row of titles, each opening its popup.
Once a menu is open, pointing at another title opens that one, and left /
right move between them. `Alt` + the mnemonic of a title opens it.
*/
export class HtmlMenuBar {
    public readonly element: HTMLDivElement;
    private readonly titles: HTMLElement[] = [];
    private readonly menus: HtmlPopupMenu[] = [];
    private readonly mnemonics: string[] = [];
    private openIndex: number = -1;
    private readonly keyListener: (event: KeyboardEvent) => void;

    public constructor() {
        this.element = document.createElement("div");
        this.element.className = "vitral-menubar";
        this.element.setAttribute("role", "menubar");
        this.keyListener = (event: KeyboardEvent): void => {
            if (!event.altKey || event.ctrlKey || event.metaKey || !this.element.isConnected) {
                return;
            }
            const letter: string = event.code.startsWith("Key") ? event.code.substring(3).toLowerCase() : event.key.toLowerCase();
            const index: number = this.mnemonics.findIndex((m: string): boolean => m.toLowerCase() === letter);
            if (index >= 0) {
                event.preventDefault();
                event.stopPropagation();
                this.open(index);
            }
        };
        document.addEventListener("keydown", this.keyListener, true);
    }

    /**
    Adds a menu (Swing's `JMenuBar.add(JMenu)`).
    @param text title of the menu
    @param mnemonic mnemonic letter, or `"\0"` for none
    @param menu its popup
    */
    public addMenu(text: string, mnemonic: string, menu: HtmlPopupMenu): void {
        const index: number = this.titles.length;
        const title: HTMLElement = document.createElement("div");
        title.className = "vitral-menubar-title";
        title.setAttribute("role", "menuitem");
        title.setAttribute("aria-haspopup", "menu");
        appendMnemonicText(title, text, mnemonic);
        title.addEventListener("pointerdown", (event: PointerEvent): void => {
            event.preventDefault();
            if (this.openIndex === index && menu.isVisible()) {
                menu.hide();
            }
            else {
                this.open(index);
            }
        });
        title.addEventListener("pointerenter", (): void => {
            if (this.openIndex >= 0 && this.openIndex !== index && this.menus[this.openIndex]!.isVisible()) {
                this.open(index);
            }
        });
        menu.setHorizontalNavigation((direction: number): void => {
            const n: number = this.menus.length;
            this.open((index + direction + n) % n);
        });
        menu.addCloseListener((): void => {
            title.classList.remove("vitral-menubar-title-open");
            if (this.openIndex === index) {
                this.openIndex = -1;
            }
        });
        this.titles.push(title);
        this.menus.push(menu);
        this.mnemonics.push(mnemonic === "\0" ? "" : mnemonic);
        this.element.appendChild(title);
    }

    /**
    Opens one of the menus.
    @param index position of the menu
    */
    public open(index: number): void {
        const title: HTMLElement | undefined = this.titles[index];
        const menu: HtmlPopupMenu | undefined = this.menus[index];
        if (title === undefined || menu === undefined) {
            return;
        }
        const box: DOMRect = title.getBoundingClientRect();
        menu.show(box.left, box.bottom, null, title);
        this.openIndex = index;
        title.classList.add("vitral-menubar-title-open");
    }

    /**
    Removes the keyboard listener of the bar (when the GUI is destroyed).
    */
    public dispose(): void {
        document.removeEventListener("keydown", this.keyListener, true);
        HtmlPopupMenu.closeAll();
    }
}

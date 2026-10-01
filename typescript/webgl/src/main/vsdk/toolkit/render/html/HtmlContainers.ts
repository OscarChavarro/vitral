/**
DOM containers for the GUIs of the toolkit: the counterparts of Swing's
`JTabbedPane`, `JSplitPane` and of the `JFrame` / `JDialog` windows an
application opens over its main window. They are styled by
`styles/vitral-html-gui.scss` of this package.
*/

import { attachToOverlayHost, detachFromOverlayHost } from "./HtmlOverlayHost.js";

/**
Swing's `JTabbedPane`: a row of tabs, each showing one content element.
*/
export class HtmlTabbedPane {
    public readonly element: HTMLDivElement;
    private readonly tabsRow: HTMLDivElement;
    private readonly contentArea: HTMLDivElement;
    private readonly tabs: HTMLElement[] = [];
    private readonly contents: HTMLElement[] = [];
    private readonly changeListeners: ((selectedIndex: number) => void)[] = [];
    private selectedIndex: number = -1;

    public constructor() {
        this.element = document.createElement("div");
        this.element.className = "vitral-tabbed-pane";
        this.tabsRow = document.createElement("div");
        this.tabsRow.className = "vitral-tabbed-pane-tabs";
        this.tabsRow.setAttribute("role", "tablist");
        this.contentArea = document.createElement("div");
        this.contentArea.className = "vitral-tabbed-pane-content";
        this.element.appendChild(this.tabsRow);
        this.element.appendChild(this.contentArea);
    }

    /**
    Swing's `addTab(title, icon, component, tip)`; the first tab added is
    selected.
    @param title text of the tab
    @param content element shown when the tab is selected
    @param tip tooltip of the tab, or null
    */
    public addTab(title: string, content: HTMLElement, tip: string | null = null): void {
        const index: number = this.tabs.length;
        const tab: HTMLDivElement = document.createElement("div");
        tab.className = "vitral-tabbed-pane-tab";
        tab.setAttribute("role", "tab");
        tab.textContent = title;
        if (tip !== null) {
            tab.title = tip;
        }
        tab.addEventListener("click", (): void => this.setSelectedIndex(index));
        this.tabs.push(tab);
        this.contents.push(content);
        this.tabsRow.appendChild(tab);
        content.hidden = true;
        this.contentArea.appendChild(content);
        if (this.selectedIndex < 0) {
            this.setSelectedIndex(0);
        }
    }

    /**
    @return the index of the selected tab, or -1 if there is none
    */
    public getSelectedIndex(): number {
        return this.selectedIndex;
    }

    /**
    Selects a tab, notifying the change listeners if it changed.
    @param index index of the tab
    */
    public setSelectedIndex(index: number): void {
        if (index < 0 || index >= this.tabs.length || index === this.selectedIndex) {
            return;
        }
        this.selectedIndex = index;
        this.tabs.forEach((tab: HTMLElement, i: number): void => {
            tab.classList.toggle("vitral-tabbed-pane-tab-selected", i === index);
            tab.setAttribute("aria-selected", i === index ? "true" : "false");
        });
        this.contents.forEach((content: HTMLElement, i: number): void => {
            content.hidden = i !== index;
        });
        for (const listener of this.changeListeners) {
            listener(index);
        }
    }

    /**
    Swing's `getModel().addChangeListener`.
    @param listener called with the index of the selected tab when it changes
    */
    public addChangeListener(listener: (selectedIndex: number) => void): void {
        this.changeListeners.push(listener);
    }
}

/**
Swing's horizontal `JSplitPane`, in continuous layout: two elements side by
side, separated by a divider that the user drags. The left element takes the
extra space when the pane grows (`setResizeWeight(1.0)`).
*/
export class HtmlSplitPane {
    public readonly element: HTMLDivElement;
    private readonly leftArea: HTMLDivElement;
    private readonly rightArea: HTMLDivElement;
    private readonly divider: HTMLDivElement;
    private minLeft: number = 0;
    private minRight: number = 0;
    /// Width of the right element, kept while the pane resizes
    private rightWidth: number = 320;
    private readonly resizeListeners: (() => void)[] = [];

    /**
    @param left element on the left
    @param right element on the right
    */
    public constructor(left: HTMLElement, right: HTMLElement) {
        this.element = document.createElement("div");
        this.element.className = "vitral-split-pane";
        this.leftArea = document.createElement("div");
        this.leftArea.className = "vitral-split-pane-left";
        this.rightArea = document.createElement("div");
        this.rightArea.className = "vitral-split-pane-right";
        this.divider = document.createElement("div");
        this.divider.className = "vitral-split-pane-divider";
        this.divider.setAttribute("role", "separator");
        this.leftArea.appendChild(left);
        this.rightArea.appendChild(right);
        this.element.appendChild(this.leftArea);
        this.element.appendChild(this.divider);
        this.element.appendChild(this.rightArea);
        this.leftArea.style.flex = "1 1 auto";
        this.applyRightWidth();

        this.divider.addEventListener("pointerdown", (event: PointerEvent): void => {
            event.preventDefault();
            this.divider.setPointerCapture(event.pointerId);
            const startX: number = event.clientX;
            const startWidth: number = this.rightWidth;
            const move = (moveEvent: PointerEvent): void => {
                this.setRightWidth(startWidth - (moveEvent.clientX - startX));
            };
            const up = (): void => {
                this.divider.removeEventListener("pointermove", move);
                this.divider.removeEventListener("pointerup", up);
                this.divider.removeEventListener("pointercancel", up);
            };
            this.divider.addEventListener("pointermove", move);
            this.divider.addEventListener("pointerup", up);
            this.divider.addEventListener("pointercancel", up);
        });
    }

    /**
    Swing's `setMinimumSize` of the two elements.
    @param minLeft minimum width of the left element, in CSS pixels
    @param minRight minimum width of the right element, in CSS pixels
    */
    public setMinimumWidths(minLeft: number, minRight: number): void {
        this.minLeft = minLeft;
        this.minRight = minRight;
        this.leftArea.style.minWidth = minLeft + "px";
        this.setRightWidth(this.rightWidth);
    }

    /**
    Places the divider so the right element has a width.
    @param width width of the right element, in CSS pixels
    */
    public setRightWidth(width: number): void {
        const total: number = this.element.clientWidth;
        let w: number = Math.max(this.minRight, width);
        if (total > 0) {
            w = Math.min(w, Math.max(this.minRight, total - this.minLeft - this.divider.offsetWidth));
        }
        this.rightWidth = w;
        this.applyRightWidth();
        for (const listener of this.resizeListeners) {
            listener();
        }
    }

    /**
    @param listener called when the divider moves
    */
    public addResizeListener(listener: () => void): void {
        this.resizeListeners.push(listener);
    }

    private applyRightWidth(): void {
        this.rightArea.style.flex = "0 0 " + this.rightWidth + "px";
    }
}

/**
A window over the page (Swing's `JFrame` or `JDialog` opened by an
application): a title bar that drags it, a close button, an optional menu
bar and a content area. It can be resized from its lower right corner.
*/
export class HtmlWindow {
    public readonly element: HTMLDivElement;
    public readonly content: HTMLDivElement;
    private readonly titleText: HTMLSpanElement;
    private readonly menubarArea: HTMLDivElement;
    private readonly closeListeners: (() => void)[] = [];
    private backdrop: HTMLDivElement | null = null;
    private visible: boolean = false;

    /**
    @param title title of the window
    @param modal true to block the page while the window is shown
    */
    public constructor(title: string, private readonly modal: boolean = false) {
        this.element = document.createElement("div");
        this.element.className = "vitral-window vitral-gui";
        this.element.setAttribute("role", modal ? "dialog" : "region");

        const titleBar: HTMLDivElement = document.createElement("div");
        titleBar.className = "vitral-window-title";
        this.titleText = document.createElement("span");
        this.titleText.className = "vitral-window-title-text";
        this.titleText.textContent = title;
        const close: HTMLButtonElement = document.createElement("button");
        close.type = "button";
        close.className = "vitral-button vitral-window-close";
        close.textContent = "×";
        close.title = "Close";
        close.addEventListener("click", (): void => this.dispose());
        titleBar.appendChild(this.titleText);
        titleBar.appendChild(close);

        this.menubarArea = document.createElement("div");
        this.menubarArea.className = "vitral-window-menubar";
        this.content = document.createElement("div");
        this.content.className = "vitral-window-content";

        this.element.appendChild(titleBar);
        this.element.appendChild(this.menubarArea);
        this.element.appendChild(this.content);

        titleBar.addEventListener("pointerdown", (event: PointerEvent): void => {
            if (event.target instanceof HTMLButtonElement) {
                return;
            }
            event.preventDefault();
            titleBar.setPointerCapture(event.pointerId);
            const box: DOMRect = this.element.getBoundingClientRect();
            const dx: number = event.clientX - box.left;
            const dy: number = event.clientY - box.top;
            const move = (moveEvent: PointerEvent): void => {
                this.setLocation(moveEvent.clientX - dx, moveEvent.clientY - dy);
            };
            const up = (): void => {
                titleBar.removeEventListener("pointermove", move);
                titleBar.removeEventListener("pointerup", up);
            };
            titleBar.addEventListener("pointermove", move);
            titleBar.addEventListener("pointerup", up);
        });
    }

    /**
    @param title new title of the window
    */
    public setTitle(title: string): void {
        this.titleText.textContent = title;
    }

    /**
    Swing's `JFrame.setJMenuBar`.
    @param menubar element of the menu bar
    */
    public setMenuBar(menubar: HTMLElement): void {
        this.menubarArea.replaceChildren(menubar);
    }

    /**
    @param x horizontal position of the upper left corner, in CSS pixels
    @param y vertical position of the upper left corner, in CSS pixels
    */
    public setLocation(x: number, y: number): void {
        const maxX: number = Math.max(0, window.innerWidth - 40);
        const maxY: number = Math.max(0, window.innerHeight - 24);
        this.element.style.left = Math.max(0, Math.min(maxX, x)) + "px";
        this.element.style.top = Math.max(0, Math.min(maxY, y)) + "px";
    }

    /**
    @param width width of the window, in CSS pixels
    @param height height of the window, in CSS pixels
    */
    public setSize(width: number, height: number): void {
        this.element.style.width = width + "px";
        this.element.style.height = height + "px";
    }

    /**
    Shows the window (centered in the page the first time).
    */
    public setVisible(visible: boolean): void {
        if (visible === this.visible) {
            return;
        }
        this.visible = visible;
        if (visible) {
            if (this.modal) {
                this.backdrop = document.createElement("div");
                this.backdrop.className = "vitral-window-modal-backdrop";
                attachToOverlayHost(this.backdrop);
            }
            attachToOverlayHost(this.element);
            if (this.element.style.left === "") {
                const box: DOMRect = this.element.getBoundingClientRect();
                this.setLocation((window.innerWidth - box.width) / 2, (window.innerHeight - box.height) / 3);
            }
        }
        else {
            detachFromOverlayHost(this.element);
            if (this.backdrop !== null) {
                detachFromOverlayHost(this.backdrop);
                this.backdrop = null;
            }
        }
    }

    /**
    @return true if the window is shown
    */
    public isVisible(): boolean {
        return this.visible;
    }

    /**
    @param listener called when the window is closed (`dispose`)
    */
    public addCloseListener(listener: () => void): void {
        this.closeListeners.push(listener);
    }

    /**
    Swing's `dispose`: hides the window for good.
    */
    public dispose(): void {
        const wasVisible: boolean = this.visible;
        this.setVisible(false);
        if (wasVisible) {
            for (const listener of this.closeListeners) {
                listener();
            }
        }
    }
}

import { ColorRgb } from "../../common/color/ColorRgb.js";
import type { Widget } from "../widget/Widget.js";
import type { WidgetCommand } from "../widget/WidgetCommand.js";
import type { WidgetMenu } from "../widget/WidgetMenu.js";
import { WidgetMenuItem } from "../widget/WidgetMenuItem.js";
import { Viewport } from "./Viewport.js";
import { ViewportElementScaler } from "./ViewportElementScaler.js";
import { ViewportSetCommands } from "./ViewportSetCommands.js";

/**
A `ViewportSet` models one rectangular drawing area of the application (for
example one canvas / one display / one screen) that is partitioned in one or
more `Viewport`s. The set knows which viewport is selected (the one receiving
interaction), how the viewports are arranged (layout style, or one viewport
maximized) and the size in pixels of the whole area.

An application can have several `ViewportSet`s, conceptually one for each
display available to the user (see `ApplicationModel`).

This class is a plain model object: it depends only on JDK and vitral-base
classes, and knows nothing about the GUI or rendering technology used to
present it.

The layout arrangements are the ones defined for 2, 3 and 4 viewports, plus a
last one for each of them where only the selected viewport is visible (see
`getLayoutStyleCount`); the percent-based area of each viewport has its origin
at the lower left corner of the set area. If the set has more viewports than the supported layouts, only
the first one is shown, maximized.
*/
export class ViewportSet {
    private static readonly T1 = 1.0 / 3.0;
    private static readonly T2 = 2.0 / 3.0;
    private static readonly H = 0.5;

    // Each layout has one {startX, startY, sizeX, sizeY} row per viewport
    private static readonly LAYOUTS_2: readonly (readonly (readonly number[])[])[] = [
        [[0, 0, ViewportSet.H, 1], [ViewportSet.H, 0, ViewportSet.H, 1]],
        [[0, ViewportSet.H, 1, ViewportSet.H], [0, 0, 1, ViewportSet.H]],
    ];

    private static readonly LAYOUTS_3: readonly (readonly (readonly number[])[])[] = [
        [[0, 0, ViewportSet.H, ViewportSet.H], [0, ViewportSet.H, ViewportSet.H, ViewportSet.H], [ViewportSet.H, 0, ViewportSet.H, 1]],
        [[0, 0, ViewportSet.H, 1], [ViewportSet.H, 0, ViewportSet.H, ViewportSet.H], [ViewportSet.H, ViewportSet.H, ViewportSet.H, ViewportSet.H]],
        [[0, 0, ViewportSet.H, ViewportSet.H], [ViewportSet.H, 0, ViewportSet.H, ViewportSet.H], [0, ViewportSet.H, 1, ViewportSet.H]],
        [[0, 0, 1, ViewportSet.H], [0, ViewportSet.H, ViewportSet.H, ViewportSet.H], [ViewportSet.H, ViewportSet.H, ViewportSet.H, ViewportSet.H]],
        [[0, 0, ViewportSet.T1, 1], [ViewportSet.T1, 0, ViewportSet.T1, 1], [ViewportSet.T2, 0, ViewportSet.T1, 1]],
        [[0, 0, 1, ViewportSet.T1], [0, ViewportSet.T1, 1, ViewportSet.T1], [0, ViewportSet.T2, 1, ViewportSet.T1]],
    ];

    private static readonly LAYOUTS_4: readonly (readonly (readonly number[])[])[] = [
        [[0, 0, ViewportSet.T1, ViewportSet.T1], [ViewportSet.T1, 0, ViewportSet.T2, 1], [0, ViewportSet.T2, ViewportSet.T1, ViewportSet.T1], [0, ViewportSet.T1, ViewportSet.T1, ViewportSet.T1]],
        [[0, 0, ViewportSet.H, ViewportSet.H], [ViewportSet.H, 0, ViewportSet.H, ViewportSet.H], [0, ViewportSet.H, ViewportSet.H, ViewportSet.H], [ViewportSet.H, ViewportSet.H, ViewportSet.H, ViewportSet.H]],
        [[0, 0, ViewportSet.H, 1], [ViewportSet.H, 0, ViewportSet.H, ViewportSet.T1], [ViewportSet.H, ViewportSet.T1, ViewportSet.H, ViewportSet.T1], [ViewportSet.H, ViewportSet.T2, ViewportSet.H, ViewportSet.T1]],
        [[0, 0, ViewportSet.T1, ViewportSet.H], [ViewportSet.T1, 0, ViewportSet.T1, ViewportSet.H], [ViewportSet.T2, 0, ViewportSet.T1, ViewportSet.H], [0, ViewportSet.H, 1, ViewportSet.H]],
        [[0, 0, 1, ViewportSet.H], [0, ViewportSet.H, ViewportSet.T1, ViewportSet.H], [ViewportSet.T1, ViewportSet.H, ViewportSet.T1, ViewportSet.H], [ViewportSet.T2, ViewportSet.H, ViewportSet.T1, ViewportSet.H]],
    ];

    private name: string;
    private readonly viewports: Viewport[];
    private selectedViewportIndex: number;
    private layoutStyle: number;
    private fullViewport: boolean;
    private sizeXInPixels: number;
    private sizeYInPixels: number;
    private titleColor: ColorRgb;
    private selectedTitleColor: ColorRgb;
    private elementScaler: ViewportElementScaler;
    private i18nContext: Widget | null;

    public constructor() {
        this.name = "Viewport set";
        this.viewports = [];
        this.selectedViewportIndex = 0;
        this.layoutStyle = 0;
        this.fullViewport = false;
        this.sizeXInPixels = 0;
        this.sizeYInPixels = 0;
        this.titleColor = new ColorRgb(1, 1, 1);
        this.selectedTitleColor = new ColorRgb(1, 1, 0);
        this.elementScaler = new ViewportElementScaler();
        this.i18nContext = null;
    }

    /**
    Creates a set with the standard four viewports arrangement: Left,
    Perspective, Top and Front, with the perspective one selected.
    @return a new set, with its layout already updated
    */
    public static createStandardSet(name: string): ViewportSet {
        const set = new ViewportSet();
        const numViews = 4;
        let i: number;

        set.setName(name);
        for (i = 0; i < numViews; i++) {
            const viewport = new Viewport();
            viewport.applyDefaultConfiguration(numViews, i);
            set.addViewport(viewport);
        }
        set.setSelectedViewportIndex(1);
        set.updateLayout();
        return set;
    }

    public getName(): string {
        return this.name;
    }

    public setName(name: string): void {
        this.name = name;
    }

    /**
    @return a read-only view of the viewports; use `addViewport` and
    `removeViewport` to change the list
    */
    public getViewports(): readonly Viewport[] {
        return this.viewports;
    }

    public getViewportCount(): number {
        return this.viewports.length;
    }

    public getViewport(index: number): Viewport {
        const viewport: Viewport | undefined = this.viewports[index];

        if (viewport === undefined) {
            throw new RangeError("Index " + index + " out of bounds for length " + this.viewports.length);
        }
        return viewport;
    }

    /**
    Adds a viewport at the end of the set and updates the layout.
    */
    public addViewport(viewport: Viewport | null): void {
        if (viewport === null) {
            return;
        }
        this.viewports.push(viewport);
        this.updateLayout();
    }

    /**
    Removes the viewport at the given index (if any) and updates the layout.
    @return the removed viewport, or null if the index is not valid
    */
    public removeViewport(index: number): Viewport | null {
        if (index < 0 || index >= this.viewports.length) {
            return null;
        }
        const removed: Viewport = this.viewports.splice(index, 1)[0]!;
        this.updateLayout();
        return removed;
    }

    public getSelectedViewportIndex(): number {
        return this.selectedViewportIndex;
    }

    public setSelectedViewportIndex(selectedViewportIndex: number): void {
        this.selectedViewportIndex = selectedViewportIndex;
    }

    /**
    @return the selected viewport, or null if the set is empty
    */
    public getSelectedViewport(): Viewport | null {
        if (this.viewports.length === 0) {
            return null;
        }
        this.clampSelection();
        return this.viewports[this.selectedViewportIndex]!;
    }

    public isSelected(viewport: Viewport | null): boolean {
        return viewport !== null && viewport === this.getSelectedViewport();
    }

    /**
    Makes the given viewport the selected one, if it belongs to this set.
    @return true if the selection was applied
    */
    public selectViewport(viewport: Viewport | null): boolean {
        const index: number = viewport === null ? -1 : this.viewports.indexOf(viewport);
        if (index < 0) {
            return false;
        }
        this.selectedViewportIndex = index;
        return true;
    }

    /**
    Selects the next viewport, cyclically, and updates the layout (which, if
    a viewport is maximized, shows the new selected one).
    */
    public selectNextViewport(): void {
        if (this.viewports.length === 0) {
            return;
        }
        this.selectedViewportIndex = (this.selectedViewportIndex + 1) % this.viewports.length;
        this.updateLayout();
    }

    public getLayoutStyle(): number {
        return this.layoutStyle;
    }

    public setLayoutStyle(layoutStyle: number): void {
        this.layoutStyle = Math.max(0, layoutStyle);
    }

    /**
    @return the number of layout styles available for the current number of
    viewports: the defined arrangements plus the one showing only the selected
    viewport (a set with a single viewport has just one)
    */
    public getLayoutStyleCount(): number {
        switch (this.viewports.length) {
            case 2:
                return ViewportSet.LAYOUTS_2.length + 1;
            case 3:
                return ViewportSet.LAYOUTS_3.length + 1;
            case 4:
                return ViewportSet.LAYOUTS_4.length + 1;
            default:
                return 1;
        }
    }

    /**
    Selects the next arrangement of the viewports and updates the layout.
    The last arrangement shows only the selected viewport.
    */
    public selectNextLayoutStyle(): void {
        this.layoutStyle++;
        if (this.layoutStyle < 0) {
            this.layoutStyle = 0;
        }
        this.updateLayout();
    }

    public isFullViewport(): boolean {
        return this.fullViewport;
    }

    public setFullViewport(fullViewport: boolean): void {
        this.fullViewport = fullViewport;
    }

    /**
    Maximizes the selected viewport, or restores the layout if it is already
    maximized, and updates the layout.
    */
    public toggleFullViewport(): void {
        this.fullViewport = !this.fullViewport;
        this.updateLayout();
    }

    /**
    @return the color used to draw the name of the viewports that are not
    selected (white by default)
    */
    public getTitleColor(): ColorRgb {
        return this.titleColor;
    }

    /**
    @param titleColor the color for the names of non selected viewports; a
    null value is ignored
    */
    public setTitleColor(titleColor: ColorRgb | null): void {
        if (titleColor !== null) {
            this.titleColor = titleColor;
        }
    }

    /**
    @return the color used to draw the name of the selected viewport (yellow
    by default)
    */
    public getSelectedTitleColor(): ColorRgb {
        return this.selectedTitleColor;
    }

    /**
    @param selectedTitleColor the color for the name of the selected viewport;
    a null value is ignored
    */
    public setSelectedTitleColor(selectedTitleColor: ColorRgb | null): void {
        if (selectedTitleColor !== null) {
            this.selectedTitleColor = selectedTitleColor;
        }
    }

    /**
    @return the I18N context (the GUI definition, in the language currently
    selected by the user) used to present the texts of this set, or null if
    there is none
    */
    public getI18nContext(): Widget | null {
        return this.i18nContext;
    }

    /**
    Sets the I18N context used to present the texts of this set. It must be
    updated whenever the user changes the language, so the set is presented
    with the messages of the new one. With a null context the default texts
    are used.
    */
    public setI18nContext(i18nContext: Widget | null): void {
        this.i18nContext = i18nContext;
    }

    /**
    @param viewport a viewport of this set
    @return the name to present for the viewport: the text of its projection
    location in the `VIEWPORT_SET_PROJECTION_LOCATION` popup of the I18N
    context (falling back to the text of the command, and to the default name
    of the camera if the context does not define them)
    */
    public getTitleFor(viewport: Viewport): string {
        const command: string = viewport.getProjectionLocationCommand();
        let name: string | null = this.findMenuItemName(ViewportSetCommands.POPUP_PROJECTION_LOCATION, command);

        if (name === null && this.i18nContext !== null) {
            const widgetCommand: WidgetCommand | null = this.i18nContext.getCommandByName(command);
            if (widgetCommand !== null) {
                name = widgetCommand.getName();
            }
        }
        if (name === null || name.length === 0) {
            return String(viewport.getTitle());
        }
        return name;
    }

    private findMenuItemName(popupName: string, command: string): string | null {
        if (this.i18nContext === null) {
            return null;
        }

        const popup: WidgetMenu | null = this.i18nContext.getPopup(popupName);
        if (popup === null) {
            return null;
        }

        for (const element of popup.getChildren()) {
            if (element instanceof WidgetMenuItem) {
                const item: WidgetMenuItem = element;
                if (!item.isSeparator() && command === item.getCommandName()) {
                    return item.getName();
                }
            }
        }
        return null;
    }

    /**
    @return the scaler that gives the size of the texts of this set for the
    resolution of the screen where the set is presented
    */
    public getElementScaler(): ViewportElementScaler {
        return this.elementScaler;
    }

    /**
    @param elementScaler the scaler for the elements of this set; a null value is
    ignored
    */
    public setElementScaler(elementScaler: ViewportElementScaler | null): void {
        if (elementScaler !== null) {
            this.elementScaler = elementScaler;
        }
    }

    /**
    @param viewport a viewport of this set
    @return the color to draw the name of the given viewport, depending on
    whether it is the selected one
    */
    public getTitleColorFor(viewport: Viewport): ColorRgb {
        if (this.isSelected(viewport)) {
            return this.selectedTitleColor;
        }
        return this.titleColor;
    }

    public getSizeXInPixels(): number {
        return this.sizeXInPixels;
    }

    public setSizeXInPixels(sizeXInPixels: number): void {
        this.sizeXInPixels = sizeXInPixels;
    }

    public getSizeYInPixels(): number {
        return this.sizeYInPixels;
    }

    public setSizeYInPixels(sizeYInPixels: number): void {
        this.sizeYInPixels = sizeYInPixels;
    }

    /**
    Sets the size in pixels of the area containing the viewports and updates
    the pixel area of each one.
    */
    public resize(sizeXInPixels: number, sizeYInPixels: number): void {
        this.sizeXInPixels = sizeXInPixels;
        this.sizeYInPixels = sizeYInPixels;
        this.updatePixelAreas();
    }

    /**
    Recalculates the pixel area of every viewport from the current size in
    pixels of the set.
    */
    public updatePixelAreas(): void {
        for (const viewport of this.viewports) {
            viewport.updatePixelArea(this.sizeXInPixels, this.sizeYInPixels);
        }
    }

    public countActiveViewports(): number {
        let n = 0;

        for (const viewport of this.viewports) {
            if (viewport.isActive()) {
                n++;
            }
        }
        return n;
    }

    /**
    Finds the active viewport under a point given in pixels of the set area,
    with origin at the upper left corner. If several viewports contain the
    point (points over shared borders), the last one in the list is returned.
    @return the viewport under the point, or null if none
    */
    public findViewportAt(x: number, y: number): Viewport | null {
        if (this.sizeXInPixels <= 0 || this.sizeYInPixels <= 0) {
            return null;
        }

        const xPercent: number = x / this.sizeXInPixels;
        const yPercent: number = 1 - y / this.sizeYInPixels;
        let found: Viewport | null = null;

        for (const viewport of this.viewports) {
            if (viewport.isActive() && viewport.contains(xPercent, yPercent)) {
                found = viewport;
            }
        }
        return found;
    }

    /**
    Translates a x coordinate given in pixels of the set area to pixels of the
    given viewport.
    @return x relative to the left of the viewport
    */
    public toViewportX(viewport: Viewport, x: number): number {
        return x - viewport.getPixelStartX();
    }

    /**
    Translates a y coordinate given in pixels of the set area (origin at the
    upper left corner) to pixels of the given viewport (same convention).
    @return y relative to the top of the viewport
    */
    public toViewportY(viewport: Viewport, y: number): number {
        return y + viewport.getPixelSizeY() - (this.sizeYInPixels - viewport.getPixelStartY());
    }

    /**
    Translates a x coordinate given in pixels of the given viewport to pixels
    of the set area. It is the inverse of `toViewportX`.
    @param x x relative to the left of the viewport
    @return x in the set area
    */
    public toSetX(viewport: Viewport, x: number): number {
        return x + viewport.getPixelStartX();
    }

    /**
    Translates a y coordinate given in pixels of the given viewport (origin
    at its upper left corner) to pixels of the set area (same convention). It
    is the inverse of `toViewportY`.
    @param y y relative to the top of the viewport
    @return y in the set area
    */
    public toSetY(viewport: Viewport, y: number): number {
        return y - viewport.getPixelSizeY() + (this.sizeYInPixels - viewport.getPixelStartY());
    }

    /**
    Updates the percent-based area and the active status of the viewports
    according to the selection, the maximized status and the layout style.
    */
    public updateLayout(): void {
        const n: number = this.viewports.length;
        let i: number;

        this.clampSelection();
        if (n === 0) {
            return;
        }

        if (this.fullViewport) {
            this.showOnlySelectedViewport();
            return;
        }

        switch (n) {
            case 1:
                this.viewports[0]!.setActive(true);
                this.viewports[0]!.setPercentArea(0, 0, 1, 1);
                break;
            case 2:
                this.applyLayouts(ViewportSet.LAYOUTS_2);
                break;
            case 3:
                this.applyLayouts(ViewportSet.LAYOUTS_3);
                break;
            case 4:
                this.applyLayouts(ViewportSet.LAYOUTS_4);
                break;
            default:
                // Not supported layout: first viewport is shown maximized
                this.selectedViewportIndex = 0;
                for (i = 0; i < n; i++) {
                    this.viewports[i]!.setActive(i === 0);
                }
                this.viewports[0]!.setPercentArea(0, 0, 1, 1);
                break;
        }
    }

    /**
    Applies the layout selected by `layoutStyle` among the given ones. After
    the last defined layout there is one more style, where only the selected
    viewport is visible.
    */
    private applyLayouts(layouts: readonly (readonly (readonly number[])[])[]): void {
        const style: number = this.layoutStyle % (layouts.length + 1);

        if (style === layouts.length) {
            this.showOnlySelectedViewport();
        } else {
            this.applyLayout(layouts[style]!);
        }
    }

    /**
    Shows the selected viewport using all the area, hiding the others.
    */
    private showOnlySelectedViewport(): void {
        let i: number;

        for (i = 0; i < this.viewports.length; i++) {
            const viewport: Viewport = this.viewports[i]!;
            viewport.setActive(i === this.selectedViewportIndex);
            if (i === this.selectedViewportIndex) {
                viewport.setPercentArea(0, 0, 1, 1);
            }
        }
    }

    private applyLayout(areas: readonly (readonly number[])[]): void {
        let i: number;

        for (i = 0; i < areas.length; i++) {
            const viewport: Viewport = this.viewports[i]!;
            const area: readonly number[] = areas[i]!;
            viewport.setActive(true);
            viewport.setPercentArea(area[0]!, area[1]!, area[2]!, area[3]!);
        }
    }

    private clampSelection(): void {
        if (this.selectedViewportIndex < 0 || this.selectedViewportIndex >= this.viewports.length) {
            this.selectedViewportIndex = 0;
        }
    }
}

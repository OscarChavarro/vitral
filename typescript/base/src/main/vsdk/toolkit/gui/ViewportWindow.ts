import { PresentationElement } from "./PresentationElement.js";

/**
A `ViewportWindow` represents an specific 2D area, to be used as the viewport
inside a canvas or any GUI element / frame / dialog. Any specific GUI
technology used by a VitralSDK program (i.e. Jogl, Awt, Swing, PostScript area)
may contain one ore more `ViewportWindow`s, arranged geometrically in 2D,
ussually forming a partition (or at least a set of non overlaping 2D
rectangles).

The ViewportWindow covers an area defined in terms of percentages.  For example,
a GUI window canvas with a single ViewportWindow covering all of its area,
will have a view located at start percent (0%, 0%) with size (100%, 100%). When
the containing canvas resizes, the internal `ViewportWindow`s will resize
accordingly and will keep its ocupation percentages updated (see
`viewportStartXPercent`, `viewportStartYPercent`, `viewportSizeXPercent` and
`viewportSizeYPercent` attributes).

A ViewportWindow can be managed by a ViewportWindowSet (forming in that way
a multiple viewport application). A ViewportWindow has an associated border
(which can be zero). Current VitralSDK visualization process should be
projected on the ViewportWindow's defined area, leaving `border` pixels
out of the projection area (the border is assigned a color and drawed to
mark the limits between ViewportWindow's).

A ViewportWindow can be `active` or inactive.  An active status means that
current viewport is visible and should be rendered. An inactive status means
that viewport is not visible and thus should not be rendered. This behavior
is designed to support applications with multiple views, where some of the
views are not rendered all the time.

A ViewportWindow can be `selected`. In applications with multiple viewports
sharing one windows canvas, this is usually used to distinguish a viewport
by drawing its border in some highlighted color, and give feedback to the
user about the viewport recieving interaction events.
*/
export class ViewportWindow extends PresentationElement {
    // Occupancy specification with respect to containing canvas
    protected viewportStartXPercent: number;
    protected viewportStartYPercent: number;
    protected viewportSizeXPercent: number;
    protected viewportSizeYPercent: number;

    protected border: number; // In pixels

    protected selected: boolean;
    protected active: boolean;

    public constructor() {
        super();
        this.viewportStartXPercent = 0.0;
        this.viewportStartYPercent = 0.0;
        this.viewportSizeXPercent = 1.0;
        this.viewportSizeYPercent = 1.0;

        this.border = 2;

        this.selected = true;
        this.active = true;
    }

    public getBorder(): number {
        return this.border;
    }

    public setBorder(b: number): void {
        this.border = b;
    }

    public getViewportStartXPercent(): number {
        return this.viewportStartXPercent;
    }

    public getViewportStartYPercent(): number {
        return this.viewportStartYPercent;
    }

    public getViewportSizeXPercent(): number {
        return this.viewportSizeXPercent;
    }

    public getViewportSizeYPercent(): number {
        return this.viewportSizeYPercent;
    }

    public setViewportStartXPercent(viewportStartXPercent: number): void {
        this.viewportStartXPercent = viewportStartXPercent;
    }

    public setViewportStartYPercent(viewportStartYPercent: number): void {
        this.viewportStartYPercent = viewportStartYPercent;
    }

    public setViewportSizeXPercent(viewportSizeXPercent: number): void {
        this.viewportSizeXPercent = viewportSizeXPercent;
    }

    public setViewportSizeYPercent(viewportSizeYPercent: number): void {
        this.viewportSizeYPercent = viewportSizeYPercent;
    }

    public setSelected(val: boolean): void {
        this.selected = val;
    }

    public getSelected(): boolean {
        return this.selected;
    }

    public isActive(): boolean {
        return this.active;
    }

    public setActive(val: boolean): void {
        this.active = val;
    }

    /**
    Given a point (x, y) on viewport's percent space, this method determines
    if the point is inside the viewport window.
    */
    public inside(x: number, y: number): boolean {
        if (
            x >= this.viewportStartXPercent &&
            x <= this.viewportStartXPercent + this.viewportSizeXPercent &&
            y >= this.viewportStartYPercent &&
            y <= this.viewportStartYPercent + this.viewportSizeYPercent
        ) {
            return true;
        }
        return false;
    }
}

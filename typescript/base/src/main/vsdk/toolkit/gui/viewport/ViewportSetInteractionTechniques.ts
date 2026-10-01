import { KeyEvent } from "../KeyEvent.js";
import { MouseEvent } from "../MouseEvent.js";
import type { Viewport } from "./Viewport.js";
import type { ViewportSet } from "./ViewportSet.js";
import type { ViewportSetInteractionListener } from "./ViewportSetInteractionListener.js";

/**
Interaction techniques over a `ViewportSet`: selection of the viewport under
the pointer, layout control and per-viewport display commands. It processes
only vitral events, so callers must convert events from the GUI technology
in use (i.e. with `AwtSystem`) before calling it.

Mouse events must have coordinates in pixels of the `ViewportSet` area, with
origin at its upper left corner.

Mouse: pressing over a viewport selects it. If it was already selected,
pressing and releasing over its title requests the menu to change its
projection location, through the `ViewportSetInteractionListener`.

Keyboard commands:
  - `;` selects the next viewport, `,` selects the next layout style (the
    last one shows only the selected viewport) and Alt+`w` maximizes /
    restores the selected viewport.
  - Over the selected viewport: `g` toggles the grid, `t`, `l`, `f`, `b` and
    `p` select the Top, Left, Front, Bottom and Perspective cameras, `.` (and
    `9`) toggles the render mode between GPU (z-buffer) and CPU (raytracing)
    and `0` cycles the requested size.
*/
export class ViewportSetInteractionTechniques {
    private readonly viewportSet: ViewportSet;
    private listener: ViewportSetInteractionListener | null;
    private titlePressArmed: boolean;

    public constructor(viewportSet: ViewportSet) {
        this.viewportSet = viewportSet;
        this.listener = null;
        this.titlePressArmed = false;
    }

    /**
    @param listener who receives the requests derived from interaction, or
    null for none
    */
    public setListener(listener: ViewportSetInteractionListener | null): void {
        this.listener = listener;
    }

    public getViewportSet(): ViewportSet {
        return this.viewportSet;
    }

    /**
    Processes one of the standard commands of the viewport set (see
    `ViewportSetCommands`), i.e. from its popup menus. Java's overload without
    a viewport works over the selected one; the one with a viewport, over the
    given one.
    @param command the id of the command, starting with `IDV_`
    @param viewport viewport to work over; the selected one when not given
    @return true if the command was a viewport set one and was processed
    */
    public processCommand(command: string, viewport?: Viewport | null): boolean {
        const target: Viewport | null = viewport === undefined ? this.viewportSet.getSelectedViewport() : viewport;

        if (target === null) {
            return false;
        }
        return target.selectProjectionLocation(command) || target.selectRenderMode(command);
    }

    /**
    @return true if the event was a viewport set command and was processed
    */
    public processKeyPressedEvent(event: KeyEvent | null): boolean {
        if (event === null) {
            return false;
        }

        switch (event.unicodeId) {
            case KeyEvent.charCode(";"):
                this.viewportSet.selectNextViewport();
                return true;
            case KeyEvent.charCode(","):
                this.viewportSet.selectNextLayoutStyle();
                return true;
            case KeyEvent.charCode("w"):
                if ((event.modifierMask & KeyEvent.MASK_ALT) !== 0) {
                    this.viewportSet.toggleFullViewport();
                    return true;
                }
                return false;
            default:
                return this.processSelectedViewportCommand(event);
        }
    }

    private processSelectedViewportCommand(event: KeyEvent): boolean {
        const viewport: Viewport | null = this.viewportSet.getSelectedViewport();

        if (viewport === null) {
            return false;
        }

        if (event.keycode === KeyEvent.KEY_9) {
            viewport.toggleRenderMode();
            return true;
        }
        if (event.keycode === KeyEvent.KEY_NUM0) {
            viewport.cycleRequestedSize();
            return true;
        }

        switch (event.unicodeId) {
            case KeyEvent.charCode("."):
                viewport.toggleRenderMode();
                return true;
            case KeyEvent.charCode("g"):
                viewport.toggleGrid();
                return true;
            case KeyEvent.charCode("t"):
                viewport.setActiveCamera(viewport.getTopCamera());
                return true;
            case KeyEvent.charCode("l"):
                viewport.setActiveCamera(viewport.getLeftCamera());
                return true;
            case KeyEvent.charCode("f"):
                viewport.setActiveCamera(viewport.getFrontCamera());
                return true;
            case KeyEvent.charCode("b"):
                viewport.setActiveCamera(viewport.getBottomCamera());
                return true;
            case KeyEvent.charCode("p"):
                viewport.setActiveCamera(viewport.getPerspectiveCamera());
                return true;
            case KeyEvent.charCode("0"):
                viewport.cycleRequestedSize();
                return true;
            default:
                return false;
        }
    }

    /**
    @return the active viewport under the pointer, or null if none
    */
    public findViewportAt(event: MouseEvent): Viewport | null {
        return this.viewportSet.findViewportAt(event.getX(), event.getY());
    }

    /**
    @param event pointer position, in the `ViewportSet` area
    @return true if the pointer is over the title (HUD) of a viewport, that
    is, over an area where clicking has an effect
    */
    public isPointerOverTitle(event: MouseEvent): boolean {
        const viewport: Viewport | null = this.findViewportAt(event);

        return viewport !== null && this.isOverTitle(event, viewport);
    }

    /**
    Selects the viewport under the pointer. If it was already the selected
    one and the press is over its title, the following click over the title
    will request the projection location menu.
    @return true if there is a viewport under the pointer
    */
    public processMousePressedEvent(event: MouseEvent): boolean {
        const viewport: Viewport | null = this.findViewportAt(event);

        // Must be evaluated before the selection changes
        this.titlePressArmed =
            viewport !== null &&
            event.getButton() === MouseEvent.BUTTON1 &&
            this.viewportSet.isSelected(viewport) &&
            this.isOverTitle(event, viewport);

        return this.selectViewportUnderPointer(event);
    }

    /**
    Selects the viewport under the pointer. If the press was over the title
    of the already selected viewport and the button is released over it too,
    the projection location menu is requested. (It is done on the release, and
    not on the click event, because GUI technologies do not generate clicks if
    the pointer moves slightly between the press and the release.)
    @return true if there is a viewport under the pointer
    */
    public processMouseReleasedEvent(event: MouseEvent): boolean {
        const titleClick: boolean = this.titlePressArmed;
        const viewport: Viewport | null = this.findViewportAt(event);

        this.titlePressArmed = false;
        const selected: boolean = this.selectViewportUnderPointer(event);

        if (
            titleClick &&
            viewport !== null &&
            this.listener !== null &&
            this.viewportSet.isSelected(viewport) &&
            this.isOverTitle(event, viewport)
        ) {
            // Just below the title, aligned with it
            const x: number = viewport.getPixelStartX() + viewport.getTitleAreaStartX();
            const viewportTop: number =
                this.viewportSet.getSizeYInPixels() - (viewport.getPixelStartY() + viewport.getPixelSizeY());
            const y: number = viewportTop + viewport.getTitleAreaStartY() + viewport.getTitleAreaSizeY();
            this.listener.projectionLocationMenuRequested(viewport, x, y);
        }
        return selected;
    }

    public processMouseClickedEvent(event: MouseEvent): boolean {
        this.titlePressArmed = false;
        return this.selectViewportUnderPointer(event);
    }

    public processMouseDraggedEvent(event: MouseEvent): boolean {
        return this.selectViewportUnderPointer(event);
    }

    private selectViewportUnderPointer(event: MouseEvent): boolean {
        const viewport: Viewport | null = this.findViewportAt(event);

        if (viewport === null) {
            return false;
        }
        return this.viewportSet.selectViewport(viewport);
    }

    private isOverTitle(event: MouseEvent, viewport: Viewport): boolean {
        return viewport.isOverTitle(
            this.viewportSet.toViewportX(viewport, event.getX()),
            this.viewportSet.toViewportY(viewport, event.getY()),
        );
    }

    /**
    Creates a copy of a mouse event, with its coordinates translated from the
    `ViewportSet` area to the given viewport (both with origin at the upper
    left corner).
    @return the translated event
    */
    public toViewportEvent(event: MouseEvent, viewport: Viewport): MouseEvent {
        const translated = new MouseEvent();

        translated.setX(this.viewportSet.toViewportX(viewport, event.getX()));
        translated.setY(this.viewportSet.toViewportY(viewport, event.getY()));
        translated.setButton(event.getButton());
        translated.setModifiers(event.getModifiers());
        translated.setClicks(event.getClicks());
        return translated;
    }
}

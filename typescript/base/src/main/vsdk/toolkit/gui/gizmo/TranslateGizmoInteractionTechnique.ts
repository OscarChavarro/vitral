import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Camera } from "../../environment/camera/Camera.js";
import type { Ray } from "../../environment/geometry/element/Ray.js";
import { InfinitePlane } from "../../environment/geometry/surface/InfinitePlane.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { KeyEvent } from "../KeyEvent.js";
import { MouseEvent } from "../MouseEvent.js";
import type { Viewport } from "../viewport/Viewport.js";
import type { InputGizmo } from "./InputGizmo.js";
import { TranslateGizmo } from "./TranslateGizmo.js";

/**
Java record `TranslateGizmoInteractionTechnique.CursorWarp`: request to place
the cursor at a position of the viewport where the gesture started, in pixels
of that viewport (origin at its upper left corner). The record accessors keep
their Java names.
*/
export class CursorWarp {
    public constructor(
        private readonly warpX: number,
        private readonly warpY: number,
    ) {}

    public x(): number {
        return this.warpX;
    }

    public y(): number {
        return this.warpY;
    }
}

/**
Interaction technique to move a `TranslateGizmo` with the keyboard and the
mouse, processing only vitral events.

Mouse events must have coordinates in pixels of the viewport they are
processed for, with origin at its upper left corner.

Infinite drag: if the gesture is started with
`processMousePressedEvent(MouseEvent, Viewport)` over a handle of the gizmo,
the gesture belongs to that viewport until the button is released (see
`getDragViewport`), so the caller must keep on feeding it the events of the
gesture, whatever viewport the cursor is over. When the cursor leaves the
viewport, the technique keeps on working with "virtual" cursor coordinates
(the real ones plus the size of the viewport for each time the cursor wrapped
around) so the movement is continuous, and, if cursor wrapping is enabled,
requests to place the cursor at the opposite side of the viewport (see
`consumeCursorWarp`). Cursors are placed by the caller, as this technique knows
nothing about the GUI technology in use.

Numeric input: the technique also feeds the keyboard to the `InputGizmo` of the
gizmo, that shows (and lets the user type) its coordinates. Typing a
number and pressing ENTER moves the gizmo (so the caller, that applies its
movement to the things it manipulates, moves them precisely there); moving the
gizmo any other way discards what was typed.
*/
export class TranslateGizmoInteractionTechnique {
    private static readonly KEY_MOVEMENT_STEP = 0.1;

    /// Events received after a cursor warp request and before the cursor
    /// arrives to its new position still have the old position: they are
    /// ignored, up to this number of them (if the warp never happens, cursor
    /// wrapping is given up for the rest of the gesture)
    private static readonly MAX_STALE_EVENTS_AFTER_WARP = 20;
    private static readonly MIN_WARP_ARRIVAL_TOLERANCE = 8;

    /// sin^2 of the minimum angle (10 degrees) between an axis and the view ray
    private static readonly AXIS_PARALLEL_TO_RAY_LIMIT = Math.pow(Math.sin(JavaMath.toRadians(10.0)), 2);

    /// Interaction techniques used to move the gizmo depending on the group
    private static readonly MOVE_ALONG_AXIS = 1;
    private static readonly MOVE_OVER_PLANE = 2;

    private readonly gizmo: TranslateGizmo;

    private lastDeltaPosition: Vector3Dd;
    private active: boolean;

    // State of the gesture in course (drag confined to a viewport)
    private cursorWrapEnabled: boolean;
    private dragViewport: Viewport | null = null;
    private dragSelection = TranslateGizmo.NULL_GROUP;
    private wrappingSuspended = false;
    private wrapOffsetX = 0;
    private wrapOffsetY = 0;
    private lastVirtualX = 0;
    private lastVirtualY = 0;
    private awaitingWarp = false;
    private warpTargetX = 0;
    private warpTargetY = 0;
    private staleEvents = 0;
    private pendingWarp: CursorWarp | null = null;

    /**
    @param gizmo gizmo manipulated by this technique
    */
    public constructor(gizmo: TranslateGizmo) {
        this.gizmo = gizmo;
        this.lastDeltaPosition = new Vector3Dd();
        this.active = false;
        this.cursorWrapEnabled = false;
        this.endGesture();
    }

    /**
    @return the gizmo manipulated by this technique
    */
    public getGizmo(): TranslateGizmo {
        return this.gizmo;
    }

    /**
    @param keyEvent key press
    @return true if the input gizmo uses the key (digits, `-`, decimal point,
    TAB, BACKSPACE, and ENTER and ESC while editing), so the caller must
    not process it as any other command
    */
    public isInputGizmoKey(keyEvent: KeyEvent): boolean {
        return this.gizmo.getInputGizmo().consumesKey(keyEvent);
    }

    /**
    @return true if the last cursor position was over an axis or plane handle
    of the gizmo
    */
    public isActive(): boolean {
        return this.active;
    }

    /**
    @return the viewport where the gesture in course started, or null if there
    is no gesture in course, that is, if the button was not pressed over a
    handle of the gizmo or it has been released
    */
    public getDragViewport(): Viewport | null {
        return this.dragViewport;
    }

    /**
    Enables the wrapping of the cursor around the viewport while dragging. It
    should be enabled only if the caller is able to place the cursor when a
    warp is requested. It is disabled by default: the gesture is still
    confined to its viewport, but the cursor can leave it.
    */
    public setCursorWrapEnabled(cursorWrapEnabled: boolean): void {
        this.cursorWrapEnabled = cursorWrapEnabled;
    }

    public isCursorWrapEnabled(): boolean {
        return this.cursorWrapEnabled;
    }

    /**
    The caller should place the cursor as requested after processing each
    dragged event, and events already in course will be ignored by the
    technique until the cursor arrives.
    @return the pending request to place the cursor, or null if there is
    none. The request is discarded after being returned.
    */
    public consumeCursorWarp(): CursorWarp | null {
        const warp: CursorWarp | null = this.pendingWarp;

        this.pendingWarp = null;
        return warp;
    }

    public processMouseEvent(_mouseEvent: MouseEvent): boolean {
        return false;
    }

    public processKeyPressedEvent(keyEvent: KeyEvent): boolean {
        let updateNeeded = false;

        this.gizmo.setSelectedResizing(true);
        this.gizmo.updateGeometryState();

        const inputGizmo: InputGizmo = this.gizmo.getInputGizmo();

        if (inputGizmo.processKeyPressedEvent(keyEvent)) {
            if (inputGizmo.consumeCommit()) {
                const target: number[] = inputGizmo.getValuesWithEdits();

                inputGizmo.cancelEditing();
                this.gizmo.setPosition(new Vector3Dd(target[0]!, target[1]!, target[2]!));
                return true;
            }
            return false;
        }

        if (keyEvent.unicodeId !== KeyEvent.KEY_NONE) {
            const p: Vector3Dd = this.gizmo.getPosition();
            const isMovementKey: boolean = "xXyYzZ".indexOf(String.fromCharCode(keyEvent.unicodeId)) >= 0;
            const step: number = TranslateGizmoInteractionTechnique.KEY_MOVEMENT_STEP;

            if (isMovementKey) {
                this.gizmo.getInputGizmo().cancelEditing();
            }

            switch (keyEvent.unicodeId) {
                case KeyEvent.charCode("x"):
                    this.gizmo.setPosition(new Vector3Dd(p.x() - step, p.y(), p.z()));
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("X"):
                    this.gizmo.setPosition(new Vector3Dd(p.x() + step, p.y(), p.z()));
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("y"):
                    this.gizmo.setPosition(new Vector3Dd(p.x(), p.y() - step, p.z()));
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("Y"):
                    this.gizmo.setPosition(new Vector3Dd(p.x(), p.y() + step, p.z()));
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("z"):
                    this.gizmo.setPosition(new Vector3Dd(p.x(), p.y(), p.z() - step));
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("Z"):
                    this.gizmo.setPosition(new Vector3Dd(p.x(), p.y(), p.z() + step));
                    updateNeeded = true;
                    break;
                default:
                    break;
            }
        }

        return updateNeeded;
    }

    public processKeyReleasedEvent(_keyEvent: KeyEvent): boolean {
        return false;
    }

    /**
    Java's two `processMousePressedEvent` overloads. With a viewport, it
    processes the press of a mouse button starting a gesture confined to the
    given viewport if a handle of the gizmo is selected (that is, if dragging
    is going to move the gizmo); without it, the gesture is not confined to
    any viewport.
    @param e event with coordinates relative to the viewport
    @param viewport viewport where the button was pressed, if the gesture is
    confined to it
    @return false (a press never changes the gizmo)
    */
    public processMousePressedEvent(e: MouseEvent, viewport?: Viewport | null): boolean {
        if (viewport === undefined) {
            return this.processUnconfinedMousePressedEvent(e);
        }
        const changed: boolean = this.processUnconfinedMousePressedEvent(e);

        if (viewport !== null && this.gizmo.getCurrentSelection() !== TranslateGizmo.NULL_GROUP) {
            this.dragViewport = viewport;
            this.dragSelection = this.gizmo.getCurrentSelection();
            this.lastVirtualX = e.getX();
            this.lastVirtualY = e.getY();
        }
        return changed;
    }

    private processUnconfinedMousePressedEvent(e: MouseEvent): boolean {
        this.endGesture();
        const p: Vector3Dd | null = this.calculateInteractionPoint(e);

        if (p === null) {
            this.lastDeltaPosition = new Vector3Dd();
        } else {
            this.lastDeltaPosition = p.subtract(this.gizmo.getPosition());
        }
        return false;
    }

    public processMouseReleasedEvent(_e: MouseEvent): boolean {
        this.endGesture();
        this.gizmo.setSelectedResizing(true);
        this.gizmo.updateGeometryState();
        return true;
    }

    public processMouseClickedEvent(e: MouseEvent): boolean {
        this.gizmo.setSelectedResizing(true);
        this.gizmo.updateGeometryState();

        const previousSelection: number = this.gizmo.getCurrentSelection();
        let selection: number = this.calculateSelection(e.getX(), e.getY());

        if (selection === TranslateGizmo.NULL_GROUP) {
            selection = previousSelection;
        }
        this.gizmo.setPersistentSelection(selection);

        return selection !== previousSelection;
    }

    public processMouseMovedEvent(e: MouseEvent): boolean {
        if (this.dragViewport !== null) {
            // The selection of the gizmo is fixed while dragging. Moved events
            // still can come (i.e. when the cursor is placed by the caller)
            // and they inform the cursor arrived to its new position
            this.checkWarpArrival(e.getX(), e.getY());
            return false;
        }

        this.gizmo.setSelectedResizing(true);
        this.gizmo.updateGeometryState();

        const previousSelection: number = this.gizmo.getCurrentSelection();
        const selection: number = this.calculateSelection(e.getX(), e.getY());

        this.gizmo.setVolatileSelection(selection);

        return selection !== previousSelection;
    }

    public processMouseDraggedEvent(e: MouseEvent): boolean {
        let event: MouseEvent | null = e;

        if (this.dragViewport !== null) {
            event = this.toVirtualEvent(e);
            if (event === null) {
                // Old event, received before the cursor arrives to its new place
                return false;
            }
        }
        const p: Vector3Dd | null = this.calculateInteractionPoint(event);

        if (p === null) {
            return false;
        }

        this.gizmo.getInputGizmo().cancelEditing();
        this.gizmo.setPosition(p.subtract(this.lastDeltaPosition));
        this.gizmo.setSelectedResizing(false);
        return true;
    }

    public processMouseWheelEvent(_e: MouseEvent): boolean {
        return false;
    }

    private endGesture(): void {
        this.dragViewport = null;
        this.dragSelection = TranslateGizmo.NULL_GROUP;
        this.wrappingSuspended = false;
        this.wrapOffsetX = 0;
        this.wrapOffsetY = 0;
        this.lastVirtualX = 0;
        this.lastVirtualY = 0;
        this.awaitingWarp = false;
        this.warpTargetX = 0;
        this.warpTargetY = 0;
        this.staleEvents = 0;
        this.pendingWarp = null;
    }

    /**
    Checks if the cursor arrived to the place it was requested to be placed
    at, ending the wait for it.
    @return true if the technique was not waiting for the cursor or it arrived
    */
    private checkWarpArrival(x: number, y: number): boolean {
        if (!this.awaitingWarp || this.dragViewport === null) {
            return true;
        }
        const tolerance: number = Math.max(
            TranslateGizmoInteractionTechnique.MIN_WARP_ARRIVAL_TOLERANCE,
            Math.trunc(Math.min(this.dragViewport.getPixelSizeX(), this.dragViewport.getPixelSizeY()) / 4),
        );

        if (Math.abs(x - this.warpTargetX) <= tolerance && Math.abs(y - this.warpTargetY) <= tolerance) {
            this.awaitingWarp = false;
            return true;
        }
        return false;
    }

    /**
    Converts a dragged event to "virtual" coordinates, which are continuous
    even if the cursor wraps around the viewport (and can be out of it), and
    requests to place the cursor if it left the viewport.
    @return the event with virtual coordinates, or null if the event must be
    ignored
    */
    private toVirtualEvent(e: MouseEvent): MouseEvent | null {
        const dragViewport: Viewport = this.dragViewport!;
        const x: number = e.getX();
        const y: number = e.getY();
        const width: number = dragViewport.getPixelSizeX();
        const height: number = dragViewport.getPixelSizeY();

        if (this.awaitingWarp) {
            if (this.checkWarpArrival(x, y)) {
                // Cursor already at its new place
            } else if (++this.staleEvents > TranslateGizmoInteractionTechnique.MAX_STALE_EVENTS_AFTER_WARP) {
                // The cursor was not placed: go on without wrapping, keeping
                // the continuity of the virtual coordinates
                this.wrappingSuspended = true;
                this.awaitingWarp = false;
                this.wrapOffsetX = this.lastVirtualX - x;
                this.wrapOffsetY = this.lastVirtualY - y;
            } else {
                return null;
            }
        }

        const virtualX: number = x + this.wrapOffsetX;
        const virtualY: number = y + this.wrapOffsetY;

        if (
            this.cursorWrapEnabled &&
            !this.wrappingSuspended &&
            width > 0 &&
            height > 0 &&
            (x < 0 || x >= width || y < 0 || y >= height)
        ) {
            const wrappedX: number = JavaMath.floorMod(x, width);
            const wrappedY: number = JavaMath.floorMod(y, height);

            // The virtual position does not change: only the real one does
            this.wrapOffsetX += x - wrappedX;
            this.wrapOffsetY += y - wrappedY;
            this.warpTargetX = wrappedX;
            this.warpTargetY = wrappedY;
            this.awaitingWarp = true;
            this.staleEvents = 0;
            this.pendingWarp = new CursorWarp(wrappedX, wrappedY);
        }
        this.lastVirtualX = virtualX;
        this.lastVirtualY = virtualY;

        const virtual = new MouseEvent();

        virtual.setX(virtualX);
        virtual.setY(virtualY);
        virtual.setButton(e.getButton());
        virtual.setModifiers(e.getModifiers());
        virtual.setClicks(e.getClicks());
        return virtual;
    }

    /**
    Given a pixel coordinate, traces a ray from the camera of the gizmo to its
    geometry and determines the group of the nearest element hit. Updates the
    active state.

    @return one of the `*_GROUP` constants of the gizmo
    */
    private calculateSelection(x: number, y: number): number {
        const camera: Camera = this.gizmo.getCamera();
        const elements: SimpleBody[] = this.gizmo.getElements();

        camera.updateVectors();
        let r: Ray = camera.generateRay(x, y);
        let nearestDistance: number = Number.MAX_VALUE;
        let nearestElement = -1;
        let index = 1;

        // Box elements are only for display, they do not affect selections
        for (let i = 0; index <= TranslateGizmo.XZX_SEGMENT_ELEMENT && i < elements.length; index++, i++) {
            r = r.withT(Number.MAX_VALUE);
            const element: SimpleBody = elements[i]!;

            const hit: Ray | null = element.getGeometry() !== null ? element.doIntersectionFirstHit(r) : null;
            if (hit !== null && hit.getT() < nearestDistance) {
                nearestDistance = hit.getT();
                nearestElement = index;
            }
        }

        let selection: number;
        switch (nearestElement) {
            case TranslateGizmo.X_AXIS_ELEMENT:
                selection = TranslateGizmo.X_AXIS_GROUP;
                break;
            case TranslateGizmo.Y_AXIS_ELEMENT:
                selection = TranslateGizmo.Y_AXIS_GROUP;
                break;
            case TranslateGizmo.Z_AXIS_ELEMENT:
                selection = TranslateGizmo.Z_AXIS_GROUP;
                break;
            case TranslateGizmo.XYY_SEGMENT_ELEMENT:
            case TranslateGizmo.XYX_SEGMENT_ELEMENT:
                selection = TranslateGizmo.XY_PLANE_GROUP;
                break;
            case TranslateGizmo.YZZ_SEGMENT_ELEMENT:
            case TranslateGizmo.YZY_SEGMENT_ELEMENT:
                selection = TranslateGizmo.YZ_PLANE_GROUP;
                break;
            case TranslateGizmo.XZZ_SEGMENT_ELEMENT:
            case TranslateGizmo.XZX_SEGMENT_ELEMENT:
                selection = TranslateGizmo.XZ_PLANE_GROUP;
                break;
            default:
                selection = TranslateGizmo.NULL_GROUP;
                break;
        }

        this.active = selection !== TranslateGizmo.NULL_GROUP;

        return selection;
    }

    /**
    Calculates the point of the space the cursor points to, constrained by the
    group of the gizmo currently selected: over the line of the axis for axis
    groups, or over the plane for plane groups.

    @return the point, or null if there is no group selected or the cursor
    does not define a point for it
    */
    private calculateInteractionPoint(e: MouseEvent): Vector3Dd | null {
        const camera: Camera = this.gizmo.getCamera();
        let v: Vector3Dd | null = null;
        const group: number = this.dragViewport !== null ? this.dragSelection : this.gizmo.getCurrentSelection();
        let technique: number;

        switch (group) {
            case TranslateGizmo.X_AXIS_GROUP:
                v = new Vector3Dd(1, 0, 0);
                technique = TranslateGizmoInteractionTechnique.MOVE_ALONG_AXIS;
                break;
            case TranslateGizmo.Y_AXIS_GROUP:
                v = new Vector3Dd(0, 1, 0);
                technique = TranslateGizmoInteractionTechnique.MOVE_ALONG_AXIS;
                break;
            case TranslateGizmo.Z_AXIS_GROUP:
                v = new Vector3Dd(0, 0, 1);
                technique = TranslateGizmoInteractionTechnique.MOVE_ALONG_AXIS;
                break;
            case TranslateGizmo.XY_PLANE_GROUP:
                v = new Vector3Dd(0, 0, 1);
                technique = TranslateGizmoInteractionTechnique.MOVE_OVER_PLANE;
                break;
            case TranslateGizmo.YZ_PLANE_GROUP:
                v = new Vector3Dd(1, 0, 0);
                technique = TranslateGizmoInteractionTechnique.MOVE_OVER_PLANE;
                break;
            case TranslateGizmo.XZ_PLANE_GROUP:
                v = new Vector3Dd(0, 1, 0);
                technique = TranslateGizmoInteractionTechnique.MOVE_OVER_PLANE;
                break;
            default:
                technique = 0;
                break;
        }

        if (v === null) {
            return null;
        }

        const o: Vector3Dd = this.gizmo.getPosition();
        const mouseX: number = e.getX();
        const mouseY: number = e.getY();

        camera.updateVectors();

        if (technique === TranslateGizmoInteractionTechnique.MOVE_OVER_PLANE) {
            const r: Ray = camera.generateRay(mouseX, mouseY);

            if (r.getDirection().dotProduct(v) > 0) {
                v = v.multiply(-1);
            }
            const plane = new InfinitePlane(v, o);
            const hit: Ray | null = plane.doIntersectionFirstHit(r);

            if (hit === null) {
                return o;
            }
            return hit.getDirection().multiply(hit.getT()).add(hit.getOrigin());
        }

        // Move along an axis: closest point of the axis line to the cursor's
        // projector ray. It depends only on the cursor position (never on its
        // last movement), so it is continuous for perspective and orthogonal
        // cameras alike.
        const r: Ray = camera.generateRay(mouseX, mouseY);
        const axis: Vector3Dd = v.normalized();
        const rayDirection: Vector3Dd = r.getDirection().normalized();
        const w0: Vector3Dd = o.subtract(r.getOrigin());
        const b: number = axis.dotProduct(rayDirection);
        const denominator: number = 1.0 - b * b;

        if (denominator < TranslateGizmoInteractionTechnique.AXIS_PARALLEL_TO_RAY_LIMIT) {
            // Looking (almost) along the axis: cursor does not define a point
            return null;
        }

        const s: number = (b * rayDirection.dotProduct(w0) - axis.dotProduct(w0)) / denominator;

        return o.add(axis.multiply(s));
    }
}

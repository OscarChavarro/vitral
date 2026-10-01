import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Camera } from "../../environment/camera/Camera.js";
import type { Ray } from "../../environment/geometry/element/Ray.js";
import type { KeyEvent } from "../KeyEvent.js";
import type { MouseEvent } from "../MouseEvent.js";
import type { Viewport } from "../viewport/Viewport.js";
import { ScaleGizmo } from "./ScaleGizmo.js";

/**
Interaction technique to work with a `ScaleGizmo` using the keyboard and the
mouse, processing only vitral events, following the structure of
`RotateGizmoInteractionTechnique`.

Mouse events must have coordinates in pixels of the viewport the gizmo is seen
in, with origin at its upper left corner.

- Hover: the handle under the cursor (an axis, a two-axis band, or the
  uniform, all-axis triangle fan) is the volatile selection of the gizmo (see
  `ScaleGizmo.pickElement`).
- Click: the handle under the cursor becomes the persistent selection; a
  click over no handle keeps the selection it has.
- Drag: pressing over a handle and dragging scales every axis the handle's
  group includes (see `ScaleGizmo.groupIncludesAxis`) by the same factor: the
  ratio between the distance from the cursor to the projected origin of the
  gizmo while dragging, and that distance when the gesture started (so
  dragging away from the gizmo grows the selected axes, and towards it
  shrinks them). The gesture belongs to the viewport where it started, if it
  was given (see `getDragViewport`), so the caller must keep on feeding it the
  events of the gesture, whatever viewport the cursor is over.
- Numeric input: the technique also feeds the keyboard to the `InputGizmo` of
  the gizmo, that shows (and lets the user type) its scale factors.
*/
export class ScaleGizmoInteractionTechnique {
    /// Below this distance (in pixels) from the projected origin, the drag
    /// ratio is not measured (it would be too sensitive, or undefined at 0)
    private static readonly MIN_DRAG_DISTANCE = 4.0;

    private readonly gizmo: ScaleGizmo;
    private active: boolean;

    // State of the gesture in course (drag of a handle)
    private dragViewport: Viewport | null = null;
    private dragGroup: number = ScaleGizmo.NULL_GROUP;
    private dragStartScale: Vector3Dd | null = null;
    private dragStartDistance = 0.0;

    /**
    @param gizmo gizmo manipulated by this technique
    */
    public constructor(gizmo: ScaleGizmo) {
        this.gizmo = gizmo;
        this.active = false;
        this.endGesture();
    }

    /**
    @return the gizmo manipulated by this technique
    */
    public getGizmo(): ScaleGizmo {
        return this.gizmo;
    }

    /**
    @return true if the last cursor position was over a handle of the gizmo
    */
    public isActive(): boolean {
        return this.active;
    }

    /**
    @return the viewport where the gesture in course started, or null if there
    is no gesture in course, or it was started without a viewport
    */
    public getDragViewport(): Viewport | null {
        return this.dragViewport;
    }

    /**
    @param keyEvent key press
    @return true if the input gizmo uses the key (digits, `-`, decimal point,
    TAB, BACKSPACE, arrows, and ENTER and ESC while editing), so the caller
    must not process it as any other command
    */
    public isInputGizmoKey(keyEvent: KeyEvent): boolean {
        return this.gizmo.getInputGizmo().consumesKey(keyEvent);
    }

    /**
    Feeds the key press to the gizmo (see `ScaleGizmo.processKeyPressedEvent`).
    @param keyEvent key press
    @return true if the scale factors changed
    */
    public processKeyPressedEvent(keyEvent: KeyEvent): boolean {
        return this.gizmo.processKeyPressedEvent(keyEvent);
    }

    public processKeyReleasedEvent(_keyEvent: KeyEvent): boolean {
        return false;
    }

    /**
    Java's two `processMousePressedEvent` overloads. It processes the press
    of a mouse button: the gesture only begins if a handle is under the cursor
    (which is found again here, as the press can come without a previous
    movement), since a press anywhere else must not scale anything, whatever
    the gizmo has selected. With a viewport, the gesture is confined to it.
    @param e event with coordinates relative to the viewport
    @param viewport viewport where the button was pressed, if the gesture is
    confined to it
    @return false (a press never changes the gizmo)
    */
    public processMousePressedEvent(e: MouseEvent, viewport?: Viewport | null): boolean {
        this.endGesture();

        const selection: number = this.calculateSelection(e.getX(), e.getY());

        if (selection !== ScaleGizmo.NULL_GROUP) {
            this.gizmo.setVolatileSelection(selection);
            this.dragGroup = selection;
            this.dragStartScale = this.gizmo.getScale();
            this.dragStartDistance = this.distanceToOrigin(e.getX(), e.getY());
        }
        if (viewport !== undefined) {
            this.dragViewport = this.isDragging() ? viewport : null;
        }
        return false;
    }

    /**
    @return true if a handle of the gizmo is being dragged
    */
    public isDragging(): boolean {
        return this.dragGroup !== ScaleGizmo.NULL_GROUP;
    }

    public processMouseReleasedEvent(_e: MouseEvent): boolean {
        this.endGesture();
        return false;
    }

    /**
    A click over a handle chooses it as the persistent selection; a click
    over no handle keeps the selection it has.
    @param e event with coordinates relative to the viewport
    @return true if the persistent selection changed
    */
    public processMouseClickedEvent(e: MouseEvent): boolean {
        const previousSelection: number = this.gizmo.getCurrentSelection();
        let selection: number = this.calculateSelection(e.getX(), e.getY());

        if (selection === ScaleGizmo.NULL_GROUP) {
            selection = previousSelection;
        }
        this.gizmo.setPersistentSelection(selection);

        return selection !== previousSelection;
    }

    /**
    While no gesture is in course, the handle under the cursor becomes the
    volatile selection.
    @param e event with coordinates relative to the viewport
    @return true if the volatile selection changed
    */
    public processMouseMovedEvent(e: MouseEvent): boolean {
        if (this.isDragging()) {
            // The selection of the gizmo is fixed while dragging
            return false;
        }

        const previousSelection: number = this.gizmo.getCurrentSelection();
        const selection: number = this.calculateSelection(e.getX(), e.getY());

        this.gizmo.setVolatileSelection(selection);

        return selection !== previousSelection;
    }

    /**
    Scales every axis of the group being dragged by the ratio between the
    current and the starting distance from the cursor to the projected origin
    of the gizmo.
    @param e event with coordinates relative to the viewport
    @return true if the scale factors changed
    */
    public processMouseDraggedEvent(e: MouseEvent): boolean {
        if (!this.isDragging() || this.dragStartScale === null) {
            return false;
        }

        const distance: number = this.distanceToOrigin(e.getX(), e.getY());
        const ratio: number =
            this.dragStartDistance >= ScaleGizmoInteractionTechnique.MIN_DRAG_DISTANCE ? distance / this.dragStartDistance : 1.0;
        const newScale = new Vector3Dd(
            this.scaledIfIncluded(0, ratio),
            this.scaledIfIncluded(1, ratio),
            this.scaledIfIncluded(2, ratio),
        );

        this.gizmo.getInputGizmo().cancelEditing();
        this.gizmo.setScale(newScale);
        return true;
    }

    public processMouseWheelEvent(_e: MouseEvent): boolean {
        return false;
    }

    private scaledIfIncluded(axis: number, ratio: number): number {
        const dragStartScale: Vector3Dd = this.dragStartScale!;
        let value: number;

        switch (axis) {
            case 0:
                value = dragStartScale.x();
                break;
            case 1:
                value = dragStartScale.y();
                break;
            default:
                value = dragStartScale.z();
                break;
        }

        return ScaleGizmo.groupIncludesAxis(this.dragGroup, axis) ? value * ratio : value;
    }

    private endGesture(): void {
        this.dragViewport = null;
        this.dragGroup = ScaleGizmo.NULL_GROUP;
        this.dragStartScale = null;
        this.dragStartDistance = 0.0;
    }

    /**
    Given a pixel coordinate, traces a ray from the camera of the gizmo to its
    geometry and determines the group of the handle hit. Updates the active
    state.
    @return one of the `*_GROUP` constants of the gizmo
    */
    private calculateSelection(x: number, y: number): number {
        const camera: Camera | null = this.gizmo.getCamera();

        if (camera === null) {
            this.active = false;
            return ScaleGizmo.NULL_GROUP;
        }
        camera.updateVectors();

        const ray: Ray = camera.generateRay(x, y);
        const selection: number = this.gizmo.pickElement(ray);

        this.active = selection !== ScaleGizmo.NULL_GROUP;

        return selection;
    }

    /**
    @return the pixel distance between (x, y) and the projected origin of the
    gizmo, or 0 if the camera or the origin can not be projected
    */
    private distanceToOrigin(x: number, y: number): number {
        const camera: Camera | null = this.gizmo.getCamera();

        if (camera === null) {
            return 0.0;
        }
        camera.updateVectors();

        const projected: Vector3Dd | null = camera.projectPointUsingRayMethodResult(this.gizmo.getPosition());

        if (projected === null) {
            return 0.0;
        }

        const dx: number = x - projected.x();
        const dy: number = y - projected.y();

        return Math.sqrt(dx * dx + dy * dy);
    }
}

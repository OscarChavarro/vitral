import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { VSDK } from "../../common/VSDK.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import type { Ray } from "../../environment/geometry/element/Ray.js";
import { KeyEvent } from "../KeyEvent.js";
import type { MouseEvent } from "../MouseEvent.js";
import type { Viewport } from "../viewport/Viewport.js";
import type { InputGizmo } from "./InputGizmo.js";
import { RotateGizmo } from "./RotateGizmo.js";

/**
Interaction technique to work with a `RotateGizmo` using the keyboard and the
mouse, processing only vitral events.

Mouse events must have coordinates in pixels of the viewport the gizmo is seen
in, with origin at its upper left corner.

- Hover: the ring under the cursor is the volatile selection of the gizmo.
- Click: the ring under the cursor becomes the persistent selection of the
  gizmo; a click over no ring keeps the selection it has.
- Drag: pressing over a ring and dragging rotates the gizmo around the axis of
  the ring, so the point of the ring grabbed keeps under the cursor: the angle
  of the cursor around the axis is measured in the plane of the ring, and the
  gizmo takes the orientation it had when pressed plus that angle (so there
  is no drift, and turning several times is possible). While dragging the gizmo
  shows the arc swept (see `RotateGizmo.setArc`), which disappears on release.
  The gesture belongs to the viewport where it started, if it was given (see
  `getDragViewport`), so the caller must keep on feeding it the events of the
  gesture, whatever viewport the cursor is over. The angle is not updated
  while the plane of the ring is seen almost edge on, as the cursor does not
  define a point of it.
- Numeric input: the keyboard is fed to the `InputGizmo` of the gizmo, that
  shows (and lets the user type) the angles, in degrees, of its orientation.
  Typing numbers and pressing ENTER (or stepping them with the arrow keys)
  rotates the gizmo to exactly that orientation (so the caller, that applies
  its orientation to the things it manipulates, orients them precisely so);
  rotating the gizmo any other way discards what was typed.
- The keys `x`, `y`, `z` (`X`, `Y`, `Z`) rotate the gizmo one degree
  clockwise (counterclockwise) around its own axes.
*/
export class RotateGizmoInteractionTechnique {
    private static readonly KEY_ROTATION_STEP = JavaMath.toRadians(1.0);

    /// Minimum cosine of the angle between the view ray and the axis of the
    /// ring (that is, sine of the angle with its plane) to measure angles
    private static readonly MIN_RAY_TO_AXIS_COSINE = 0.05;

    private readonly gizmo: RotateGizmo;
    private active: boolean;

    // State of the gesture in course (drag of a ring)
    private dragRing = -1;
    private dragViewport: Viewport | null = null;
    private dragStartTransformation: Matrix4x4d | null = null;
    private dragU: Vector3Dd | null = null;
    private dragV: Vector3Dd | null = null;
    private dragAxis: Vector3Dd | null = null;
    private dragStartAngle = 0;
    private dragLastAngle: number = Number.NaN;
    private dragSweep = 0;

    /**
    @param gizmo gizmo manipulated by this technique
    */
    public constructor(gizmo: RotateGizmo) {
        this.gizmo = gizmo;
        this.active = false;
        this.endGesture();
    }

    /**
    @return the gizmo manipulated by this technique
    */
    public getGizmo(): RotateGizmo {
        return this.gizmo;
    }

    /**
    @return true if the last cursor position was over a ring of the gizmo
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
    @return true if a ring is being dragged
    */
    public isDragging(): boolean {
        return this.dragRing >= 0;
    }

    /**
    @param keyEvent key press
    @return true if the input gizmo uses the key (digits, `-`, decimal point,
    TAB, BACKSPACE, arrows, and ENTER and ESC while editing), so the caller must
    not process it as any other command
    */
    public isInputGizmoKey(keyEvent: KeyEvent): boolean {
        return this.gizmo.getInputGizmo().consumesKey(keyEvent);
    }

    /**
    Processes a key press.
    @param keyEvent key press
    @return true if the orientation of the gizmo changed
    */
    public processKeyPressedEvent(keyEvent: KeyEvent): boolean {
        const inputGizmo: InputGizmo = this.gizmo.getInputGizmo();

        if (inputGizmo.processKeyPressedEvent(keyEvent)) {
            if (inputGizmo.consumeCommit()) {
                const values: number[] = inputGizmo.getValuesWithEdits();

                inputGizmo.cancelEditing();

                let rotation: Matrix4x4d = RotateGizmo.createRotationFromAnglesInDegrees(
                    values[0]!,
                    values[1]!,
                    values[2]!,
                ).withoutTranslation();
                const cameraDeltaDegrees: number = values[RotateGizmo.CAMERA_INPUT_FIELD_INDEX]!;

                if (Math.abs(cameraDeltaDegrees) > VSDK.EPSILON) {
                    // The fourth field is relative: it rotates on top of the
                    // orientation given by the other three, around the
                    // current camera axis, and goes back to 0
                    const cameraDelta: Matrix4x4d = new Matrix4x4d().axisRotation(
                        JavaMath.toRadians(cameraDeltaDegrees),
                        this.gizmo.getCameraAxisDirection(),
                    );

                    rotation = cameraDelta.multiply(rotation);
                }
                this.gizmo.setTransformationMatrix(rotation.withTranslation(this.gizmo.getPosition()));
                return true;
            }
            return false;
        }

        let axis: Vector3Dd;

        switch (keyEvent.unicodeId) {
            case KeyEvent.charCode("x"):
            case KeyEvent.charCode("X"):
                axis = new Vector3Dd(1, 0, 0);
                break;
            case KeyEvent.charCode("y"):
            case KeyEvent.charCode("Y"):
                axis = new Vector3Dd(0, 1, 0);
                break;
            case KeyEvent.charCode("z"):
            case KeyEvent.charCode("Z"):
                axis = new Vector3Dd(0, 0, 1);
                break;
            default:
                return false;
        }

        const character: string = String.fromCharCode(keyEvent.unicodeId);
        const angle: number =
            character === character.toUpperCase()
                ? RotateGizmoInteractionTechnique.KEY_ROTATION_STEP
                : -RotateGizmoInteractionTechnique.KEY_ROTATION_STEP;
        const delta: Matrix4x4d = new Matrix4x4d().axisRotation(angle, axis);

        inputGizmo.cancelEditing();
        this.gizmo.setTransformationMatrix(this.gizmo.getTransformationMatrix().multiply(delta));
        return true;
    }

    public processKeyReleasedEvent(_keyEvent: KeyEvent): boolean {
        return false;
    }

    /**
    Java's two `processMousePressedEvent` overloads. It processes the press
    of a mouse button: if the cursor is over a ring, the ring becomes the
    chosen one and a gesture to rotate around it starts, confined to the given
    viewport when one is given. The caller also needs to know if the cursor is
    over a ring (see `isActive`), to let a press over the gizmo grab it,
    instead of selecting what is behind.
    @param e event with coordinates relative to the viewport
    @param viewport viewport where the button was pressed, if the gesture is
    confined to it
    @return false (a press never changes the gizmo)
    */
    public processMousePressedEvent(e: MouseEvent, viewport?: Viewport | null): boolean {
        this.endGesture();
        const selection: number = this.calculateSelection(e.getX(), e.getY());

        if (selection !== RotateGizmo.NULL_GROUP) {
            // Groups are 1-based (X=1, Y=2, Z=3, camera=4); ring indexes are
            // 0-based (0, 1, 2, and RotateGizmo.CAMERA_RING_INDEX for the
            // camera ring), so they line up as `selection - 1`
            this.beginGesture(selection - 1, e.getX(), e.getY());
        }
        if (viewport !== undefined) {
            this.dragViewport = this.isDragging() ? viewport : null;
        }
        return false;
    }

    /**
    Ends the gesture in course, if there is one, so the arc disappears.
    @return true if there was a gesture, so the gizmo must be drawn again
    */
    public processMouseReleasedEvent(_e: MouseEvent): boolean {
        const wasDragging: boolean = this.isDragging();

        this.endGesture();
        return wasDragging;
    }

    /**
    Makes the ring under the cursor the persistent selection, if there is one.
    @param e event with coordinates relative to the viewport
    @return true if the current selection changed
    */
    public processMouseClickedEvent(e: MouseEvent): boolean {
        const previousSelection: number = this.gizmo.getCurrentSelection();
        let selection: number = this.calculateSelection(e.getX(), e.getY());

        if (selection === RotateGizmo.NULL_GROUP) {
            selection = previousSelection;
        }
        this.gizmo.setPersistentSelection(selection);
        return selection !== previousSelection;
    }

    /**
    Makes the ring under the cursor the volatile selection.
    @param e event with coordinates relative to the viewport
    @return true if the current selection changed, so the gizmo must be drawn
    again
    */
    public processMouseMovedEvent(e: MouseEvent): boolean {
        if (this.isDragging()) {
            // The ring is fixed while dragging
            return false;
        }
        const previousSelection: number = this.gizmo.getCurrentSelection();
        const selection: number = this.calculateSelection(e.getX(), e.getY());

        this.gizmo.setVolatileSelection(selection);
        return this.gizmo.getCurrentSelection() !== previousSelection;
    }

    /**
    Rotates the gizmo around the axis of the ring being dragged, so the point
    of the ring under the cursor when the gesture started follows it.
    @param e event with coordinates relative to the viewport of the gesture
    (they can be out of it)
    @return true if the gizmo changed, so it must be drawn again and what it
    manipulates must be updated
    */
    public processMouseDraggedEvent(e: MouseEvent): boolean {
        if (!this.isDragging()) {
            return false;
        }

        const angle: number = this.calculateAngle(e.getX(), e.getY());

        if (Number.isNaN(angle)) {
            return false;
        }
        if (Number.isNaN(this.dragLastAngle)) {
            // First point of the plane the cursor defines: the arc starts here
            this.dragStartAngle = angle;
        } else {
            let delta: number = angle - this.dragLastAngle;

            // The shortest way from the previous angle: the sweep is
            // continuous when the cursor goes around the axis
            delta -= 2 * Math.PI * JavaMath.round(delta / (2 * Math.PI));
            this.dragSweep += delta;
        }
        this.dragLastAngle = angle;

        const dragStartTransformation: Matrix4x4d = this.dragStartTransformation!;

        // `dragAxis` is fixed (in world space) since the gesture started, so
        // rotating around it, then keeping the position the gizmo had when
        // pressed, is equivalent to rotating around the matching local axis
        // of `dragStartTransformation` (used for the X, Y and Z rings) but
        // also works for the camera ring, whose axis has no local counterpart
        const rotationOnly: Matrix4x4d = new Matrix4x4d()
            .axisRotation(this.dragSweep, this.dragAxis!)
            .multiply(new Matrix4x4d(dragStartTransformation).withoutTranslation());

        this.gizmo.getInputGizmo().cancelEditing();
        this.gizmo.setTransformationMatrix(rotationOnly.withTranslation(dragStartTransformation.extractTranslation()));
        this.gizmo.setArc(this.dragRing, this.dragU!, this.dragV!, this.dragStartAngle, this.dragSweep);
        return true;
    }

    public processMouseWheelEvent(_e: MouseEvent): boolean {
        return false;
    }

    /**
    Starts the gesture to rotate around a ring, keeping the plane of the ring
    (and the directions angles are measured from) as they are now. The angle
    of the point grabbed is measured at once, so the gizmo follows the cursor
    from the place it was pressed at.
    */
    private beginGesture(ring: number, x: number, y: number): void {
        this.dragRing = ring;
        this.dragStartTransformation = new Matrix4x4d(this.gizmo.getTransformationMatrix());
        if (ring === RotateGizmo.CAMERA_RING_INDEX) {
            this.dragAxis = this.gizmo.getCameraAxisDirection();
            this.dragU = this.gizmo.getCameraPlaneRightDirection();
            this.dragV = this.gizmo.getCameraPlaneUpDirection();
            this.gizmo.setPersistentSelection(RotateGizmo.CAMERA_RING_GROUP);
        } else {
            this.dragAxis = this.gizmo.getAxisDirection(ring);
            this.dragU = this.gizmo.getAxisDirection((ring + 1) % RotateGizmo.RING_COUNT);
            this.dragV = this.gizmo.getAxisDirection((ring + 2) % RotateGizmo.RING_COUNT);
            this.gizmo.setPersistentSelection(RotateGizmo.groupOfRing(ring));
        }
        this.dragStartAngle = 0;
        this.dragLastAngle = Number.NaN;
        this.dragSweep = 0;
        this.gizmo.clearArc();

        // NaN if the ring is seen edge on: the first angle is taken when dragging
        const angle: number = this.calculateAngle(x, y);

        if (!Number.isNaN(angle)) {
            this.dragStartAngle = angle;
            this.dragLastAngle = angle;
        }
    }

    private endGesture(): void {
        this.dragRing = -1;
        this.dragViewport = null;
        this.dragStartTransformation = null;
        this.dragU = null;
        this.dragV = null;
        this.dragAxis = null;
        this.dragStartAngle = 0;
        this.dragLastAngle = Number.NaN;
        this.dragSweep = 0;
        this.gizmo.clearArc();
    }

    /**
    Calculates the angle, around the axis of the ring being dragged, of the
    point of its plane the cursor points to.
    @return the angle in radians, measured from `dragU` towards `dragV`, or NaN
    if the cursor does not define a point of the plane (it is seen edge on, or
    the point is behind the camera)
    */
    private calculateAngle(x: number, y: number): number {
        const camera: Camera = this.gizmo.getCamera();
        const dragAxis: Vector3Dd = this.dragAxis!;

        camera.updateVectors();
        const ray: Ray = camera.generateRay(x, y);
        const direction: Vector3Dd = ray.getDirection().normalized();
        const cosine: number = direction.dotProduct(dragAxis);

        if (Math.abs(cosine) < RotateGizmoInteractionTechnique.MIN_RAY_TO_AXIS_COSINE) {
            return Number.NaN;
        }

        const center: Vector3Dd = this.dragStartTransformation!.extractTranslation();
        const distance: number = center.subtract(ray.getOrigin()).dotProduct(dragAxis) / cosine;

        // The rays of an orthogonal camera start at a plane, and the gizmo can
        // be behind it
        if (distance <= 0 && camera.getProjectionMode() !== Camera.PROJECTION_MODE_ORTHOGONAL) {
            return Number.NaN;
        }

        const fromCenter: Vector3Dd = ray.getOrigin().add(direction.multiply(distance)).subtract(center);

        return Math.atan2(fromCenter.dotProduct(this.dragV!), fromCenter.dotProduct(this.dragU!));
    }

    /**
    Given a pixel coordinate, traces a ray from the camera of the gizmo to its
    rings and determines the nearest one hit. Updates the active state.

    @return one of the `*_RING_GROUP` constants of the gizmo, or `NULL_GROUP`
    */
    private calculateSelection(x: number, y: number): number {
        const camera: Camera = this.gizmo.getCamera();

        this.gizmo.updateGeometryState();
        camera.updateVectors();
        const ray: Ray = camera.generateRay(x, y);
        const selection: number = this.gizmo.pickRing(ray);

        this.active = selection !== RotateGizmo.NULL_GROUP;
        return selection;
    }
}

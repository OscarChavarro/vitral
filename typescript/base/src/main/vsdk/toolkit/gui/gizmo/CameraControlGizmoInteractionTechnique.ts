import { Double } from "../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import type { MouseEvent } from "../MouseEvent.js";
import { CameraControlGizmo } from "./CameraControlGizmo.js";
import { CameraControlGizmoAnimation } from "./CameraControlGizmoAnimation.js";

/**
Interaction technique to control the camera of a view with a
`CameraControlGizmo` (a ViewCube), processing only vitral events.

Mouse events must have coordinates in pixels of the viewport the gizmo is
seen in, with origin at its upper left corner (as the area of the gizmo, see
`CameraControlGizmo.placeInViewport`).

- Hover: the gizmo becomes active (opaque) while the cursor is over its
  area, and the region under the cursor is highlighted.
- Click (press and release without dragging) over a region of the cube: the
  camera turns smoothly to the view of the region (an orthogonal view for a
  face, a three-fourths view for an edge or a corner), orbiting the point it
  looks at (see `CameraControlGizmoAnimation`).
- Drag over the cube: the camera orbits freely, following the cursor
  (horizontal movement turns around the vertical axis Z, vertical movement
  changes the elevation, which is kept short of the poles so the scene stays
  upright).
- Drag over the compass: the camera turns around the vertical axis Z, so the
  point of the compass grabbed keeps under the cursor.

A press over the area of the gizmo starts a gesture that belongs to it (see
`isGestureInCourse`): the caller must keep on feeding it the events of the
gesture, and must not process them as other techniques (i.e. selection or
camera control of the view). Any other interaction with the camera must call
`cancelAnimation`.

The animation does not depend on a clock: the caller must call
`advanceAnimation` with the current time while `isAnimating()`, drawing the
view again after each call.
*/
export class CameraControlGizmoInteractionTechnique {
    /// Cursor movement, in pixels, from the press that turns a click into a drag
    public static readonly DRAG_THRESHOLD_IN_PIXELS: number = 3;
    /// Angle turned by each pixel of free orbit, for a gizmo of base size
    public static readonly ORBIT_RADIANS_PER_PIXEL: number = JavaMath.toRadians(0.6);
    /// Maximum elevation of the camera while orbiting freely
    public static readonly MAX_ELEVATION: number = JavaMath.toRadians(89.0);

    private readonly gizmo: CameraControlGizmo;
    private animationDurationInSeconds: number;
    private animation: CameraControlGizmoAnimation | null;

    // State of the gesture in course
    private gestureInCourse: boolean = false;
    private dragging: boolean = false;
    private pressRegion: number = CameraControlGizmo.NO_REGION;
    private pressX: number = 0;
    private pressY: number = 0;
    private pressHudCamera: Camera | null = null;
    private pressRotation: Matrix4x4d | null = null;
    private pressPivot: Vector3Dd | null = null;
    private pressDistance: number = 0;
    private pressCompassAngle: number = Double.NaN;

    /**
    @param gizmo gizmo used by this technique; its camera (see
    `CameraControlGizmo.setCamera`) is the one controlled
    */
    public constructor(gizmo: CameraControlGizmo) {
        this.gizmo = gizmo;
        this.animationDurationInSeconds = CameraControlGizmoAnimation.DEFAULT_DURATION_IN_SECONDS;
        this.animation = null;
        this.endGesture();
    }

    public getGizmo(): CameraControlGizmo {
        return this.gizmo;
    }

    public getAnimationDurationInSeconds(): number {
        return this.animationDurationInSeconds;
    }

    public setAnimationDurationInSeconds(seconds: number): void {
        this.animationDurationInSeconds = seconds;
    }

    /**
    @return true if the cursor is over the gizmo, or a gesture of the gizmo is
    in course
    */
    public isActive(): boolean {
        return this.gizmo.isActive() || this.gestureInCourse;
    }

    /**
    @return true between a press over the gizmo and its release
    */
    public isGestureInCourse(): boolean {
        return this.gestureInCourse;
    }

    /**
    @return true if the camera is turning to a view
    */
    public isAnimating(): boolean {
        return this.animation !== null && this.animation.isRunning();
    }

    /**
    Moves the camera along the animation in course.
    @param nanoTime current time, in nanoseconds
    @return true if the camera changed, so the view must be drawn again
    */
    public advanceAnimation(nanoTime: number): boolean {
        if (!this.isAnimating()) {
            return false;
        }
        const changed = this.animation!.advance(nanoTime);
        this.gizmo.updateHudCamera();
        return changed;
    }

    /**
    Stops the animation in course, leaving the camera where it is.
    */
    public cancelAnimation(): void {
        if (this.animation !== null) {
            this.animation.cancel();
            this.animation = null;
        }
    }

    /**
    Starts the turn of the camera to the view of a region of the cube.
    @param region a region of the cube
    @return true if an animation started
    */
    public animateToRegion(region: number): boolean {
        const camera = this.gizmo.getCamera();

        if (camera === null || region < 0 || region >= CameraControlGizmo.NUMBER_OF_CUBE_REGIONS) {
            return false;
        }
        this.cancelAnimation();
        const target = this.gizmo.calculateViewRotation(region, camera.getRotation());
        if (target === null) {
            return false;
        }
        this.animation = new CameraControlGizmoAnimation(camera, target, this.animationDurationInSeconds);
        return true;
    }

    //= Mouse =============================================================

    /**
    Updates the highlight of the gizmo for the cursor position.
    @param e event with coordinates relative to the viewport
    @return true if the gizmo changed, so it must be drawn again
    */
    public processMouseMovedEvent(e: MouseEvent): boolean {
        if (this.gestureInCourse) {
            return false;
        }
        return this.updateHover(e.getX(), e.getY());
    }

    /**
    Called when the cursor leaves the viewport: the gizmo is not active.
    @return true if the gizmo changed
    */
    public processMouseExitedEvent(): boolean {
        if (this.gestureInCourse) {
            return false;
        }
        const changed = this.gizmo.isActive() || this.gizmo.getVolatileSelection() !== CameraControlGizmo.NO_REGION;
        this.gizmo.setActive(false);
        this.gizmo.setVolatileSelection(CameraControlGizmo.NO_REGION);
        return changed;
    }

    /**
    Processes the press of a mouse button. If the cursor is over the area of
    the gizmo, a gesture of the gizmo starts (so the caller must not process
    the press in any other way: see `isGestureInCourse`).
    @param e event with coordinates relative to the viewport
    @return true if the press was taken by the gizmo
    */
    public processMousePressedEvent(e: MouseEvent): boolean {
        this.endGesture();
        if (!this.gizmo.containsPixel(e.getX(), e.getY()) || this.gizmo.getCamera() === null) {
            return false;
        }
        this.updateHover(e.getX(), e.getY());
        this.cancelAnimation();

        const camera = this.gizmo.getCamera()!;
        this.gestureInCourse = true;
        this.pressRegion = this.gizmo.pickRegion(e.getX(), e.getY());
        this.pressX = e.getX();
        this.pressY = e.getY();
        this.pressHudCamera = new Camera(this.gizmo.getHudCamera());
        this.pressRotation = camera.getRotation().withoutTranslation();
        this.pressPivot = camera.getFocusedPosition();
        this.pressDistance = this.pressPivot.subtract(camera.getPosition()).length();
        this.pressCompassAngle = CameraControlGizmo.compassAngleAt(
            e.getX() - this.gizmo.getHudX(),
            e.getY() - this.gizmo.getHudY(),
            this.pressHudCamera,
        );
        return true;
    }

    /**
    Orbits the camera while dragging over the gizmo.
    @param e event with coordinates relative to the viewport (they can be out
    of the gizmo, or the viewport)
    @return true if the camera changed, so the view must be drawn again
    */
    public processMouseDraggedEvent(e: MouseEvent): boolean {
        if (!this.gestureInCourse) {
            return false;
        }
        const dx = e.getX() - this.pressX;
        const dy = e.getY() - this.pressY;

        if (!this.dragging) {
            if (
                JavaMath.abs(dx) < CameraControlGizmoInteractionTechnique.DRAG_THRESHOLD_IN_PIXELS &&
                JavaMath.abs(dy) < CameraControlGizmoInteractionTechnique.DRAG_THRESHOLD_IN_PIXELS
            ) {
                return false;
            }
            this.dragging = true;
        }

        let rotation: Matrix4x4d | null = null;
        if (this.pressRegion === CameraControlGizmo.COMPASS_REGION) {
            rotation = this.compassRotation(e.getX(), e.getY(), dx);
        }
        if (rotation === null) {
            rotation = this.orbitRotation(dx, dy);
        }
        if (rotation === null) {
            return false;
        }
        this.applyRotation(rotation);
        return true;
    }

    /**
    Ends the gesture of the gizmo. If there was no drag, it was a click over
    a region: the camera starts turning to its view.
    @param e event with coordinates relative to the viewport
    @return true if the gesture was of the gizmo (so the caller must not
    process the release in any other way)
    */
    public processMouseReleasedEvent(e: MouseEvent): boolean {
        if (!this.gestureInCourse) {
            return false;
        }
        const wasDragging = this.dragging;
        const region = this.pressRegion;

        this.endGesture();
        if (!wasDragging && region >= 0 && region < CameraControlGizmo.NUMBER_OF_CUBE_REGIONS) {
            this.animateToRegion(region);
        }
        this.updateHover(e.getX(), e.getY());
        return true;
    }

    //= Gesture computations ==============================================

    /**
    Free orbit: the azimuth and elevation of the camera at the press change
    with the cursor movement.
    */
    private orbitRotation(dx: number, dy: number): Matrix4x4d | null {
        const pressRotation = this.pressRotation!;
        const scale =
            CameraControlGizmoInteractionTechnique.ORBIT_RADIANS_PER_PIXEL *
            (this.gizmo.getBaseSizeInPixels() / JavaMath.max(1, this.gizmo.getSizeInPixels()));
        const front = new Vector3Dd(
            pressRotation.get(0, 0),
            pressRotation.get(1, 0),
            pressRotation.get(2, 0),
        ).normalized();
        let elevation = Math.asin(JavaMath.max(-1.0, JavaMath.min(1.0, front.z())));
        let azimuth = Math.atan2(front.y(), front.x());

        // Dragging moves the cube with the cursor: right turns the scene
        // counterclockwise (seen from above), down shows more of its top
        azimuth -= dx * scale;
        elevation -= dy * scale;
        elevation = JavaMath.max(
            -CameraControlGizmoInteractionTechnique.MAX_ELEVATION,
            JavaMath.min(CameraControlGizmoInteractionTechnique.MAX_ELEVATION, elevation),
        );

        const newFront = new Vector3Dd(
            JavaMath.cos(elevation) * JavaMath.cos(azimuth),
            JavaMath.cos(elevation) * JavaMath.sin(azimuth),
            JavaMath.sin(elevation),
        );
        return CameraControlGizmo.createCameraRotation(newFront, new Vector3Dd(0, 0, 1));
    }

    /**
    Turn around Z that keeps the point of the compass grabbed under the
    cursor, measured with the HUD camera of the press (so there is no drift).
    @return the rotation, or null if the compass is seen almost edge on (then
    the free orbit is used)
    */
    private compassRotation(x: number, y: number, _dx: number): Matrix4x4d | null {
        if (Double.isNaN(this.pressCompassAngle)) {
            return null;
        }
        const angle = CameraControlGizmo.compassAngleAt(
            x - this.gizmo.getHudX(),
            y - this.gizmo.getHudY(),
            this.pressHudCamera!,
        );
        if (Double.isNaN(angle)) {
            return null;
        }
        const sweep = angle - this.pressCompassAngle;
        // The scene turns by the sweep, so the camera turns the other way
        return new Matrix4x4d().axisRotation(-sweep, new Vector3Dd(0, 0, 1)).multiply(this.pressRotation!);
    }

    private applyRotation(rotation: Matrix4x4d): void {
        const camera = this.gizmo.getCamera()!;
        const front = new Vector3Dd(rotation.get(0, 0), rotation.get(1, 0), rotation.get(2, 0)).normalized();

        camera.setRotation(rotation);
        camera.setPosition(this.pressPivot!.subtract(front.multiply(this.pressDistance)));
        camera.updateVectors();
        this.gizmo.updateHudCamera();
    }

    private updateHover(x: number, y: number): boolean {
        const wasActive = this.gizmo.isActive();
        const previous = this.gizmo.getVolatileSelection();
        const inside = this.gizmo.containsPixel(x, y);

        this.gizmo.setActive(inside);
        this.gizmo.setVolatileSelection(inside ? this.gizmo.pickRegion(x, y) : CameraControlGizmo.NO_REGION);
        return wasActive !== this.gizmo.isActive() || previous !== this.gizmo.getVolatileSelection();
    }

    private endGesture(): void {
        this.gestureInCourse = false;
        this.dragging = false;
        this.pressRegion = CameraControlGizmo.NO_REGION;
        this.pressX = 0;
        this.pressY = 0;
        this.pressHudCamera = null;
        this.pressRotation = null;
        this.pressPivot = null;
        this.pressDistance = 0;
        this.pressCompassAngle = Double.NaN;
    }
}

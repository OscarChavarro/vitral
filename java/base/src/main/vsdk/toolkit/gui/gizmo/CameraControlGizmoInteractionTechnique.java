package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.MouseEvent;

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
public class CameraControlGizmoInteractionTechnique
{
    /// Cursor movement, in pixels, from the press that turns a click into a drag
    public static final int DRAG_THRESHOLD_IN_PIXELS = 3;
    /// Angle turned by each pixel of free orbit, for a gizmo of base size
    public static final double ORBIT_RADIANS_PER_PIXEL = Math.toRadians(0.6);
    /// Maximum elevation of the camera while orbiting freely
    public static final double MAX_ELEVATION = Math.toRadians(89.0);

    private final CameraControlGizmo gizmo;
    private double animationDurationInSeconds;
    private CameraControlGizmoAnimation animation;

    // State of the gesture in course
    private boolean gestureInCourse;
    private boolean dragging;
    private int pressRegion;
    private int pressX;
    private int pressY;
    private Camera pressHudCamera;
    private Matrix4x4d pressRotation;
    private Vector3Dd pressPivot;
    private double pressDistance;
    private double pressCompassAngle;

    /**
    @param gizmo gizmo used by this technique; its camera (see
    `CameraControlGizmo.setCamera`) is the one controlled
    */
    public CameraControlGizmoInteractionTechnique(CameraControlGizmo gizmo)
    {
        this.gizmo = gizmo;
        animationDurationInSeconds = CameraControlGizmoAnimation.DEFAULT_DURATION_IN_SECONDS;
        animation = null;
        endGesture();
    }

    public CameraControlGizmo getGizmo()
    {
        return gizmo;
    }

    public double getAnimationDurationInSeconds()
    {
        return animationDurationInSeconds;
    }

    public void setAnimationDurationInSeconds(double seconds)
    {
        animationDurationInSeconds = seconds;
    }

    /**
    @return true if the cursor is over the gizmo, or a gesture of the gizmo is
    in course
    */
    public boolean isActive()
    {
        return gizmo.isActive() || gestureInCourse;
    }

    /**
    @return true between a press over the gizmo and its release
    */
    public boolean isGestureInCourse()
    {
        return gestureInCourse;
    }

    /**
    @return true if the camera is turning to a view
    */
    public boolean isAnimating()
    {
        return animation != null && animation.isRunning();
    }

    /**
    Moves the camera along the animation in course.
    @param nanoTime current time, in nanoseconds
    @return true if the camera changed, so the view must be drawn again
    */
    public boolean advanceAnimation(long nanoTime)
    {
        if ( !isAnimating() ) {
            return false;
        }
        boolean changed = animation.advance(nanoTime);
        gizmo.updateHudCamera();
        return changed;
    }

    /**
    Stops the animation in course, leaving the camera where it is.
    */
    public void cancelAnimation()
    {
        if ( animation != null ) {
            animation.cancel();
            animation = null;
        }
    }

    /**
    Starts the turn of the camera to the view of a region of the cube.
    @param region a region of the cube
    @return true if an animation started
    */
    public boolean animateToRegion(int region)
    {
        Camera camera = gizmo.getCamera();

        if ( camera == null || region < 0 ||
             region >= CameraControlGizmo.NUMBER_OF_CUBE_REGIONS ) {
            return false;
        }
        cancelAnimation();
        Matrix4x4d target = gizmo.calculateViewRotation(region, camera.getRotation());
        if ( target == null ) {
            return false;
        }
        animation = new CameraControlGizmoAnimation(camera, target,
            animationDurationInSeconds);
        return true;
    }

    //= Mouse =============================================================

    /**
    Updates the highlight of the gizmo for the cursor position.
    @param e event with coordinates relative to the viewport
    @return true if the gizmo changed, so it must be drawn again
    */
    public boolean processMouseMovedEvent(MouseEvent e)
    {
        if ( gestureInCourse ) {
            return false;
        }
        return updateHover(e.getX(), e.getY());
    }

    /**
    Called when the cursor leaves the viewport: the gizmo is not active.
    @return true if the gizmo changed
    */
    public boolean processMouseExitedEvent()
    {
        if ( gestureInCourse ) {
            return false;
        }
        boolean changed = gizmo.isActive() ||
            gizmo.getVolatileSelection() != CameraControlGizmo.NO_REGION;
        gizmo.setActive(false);
        gizmo.setVolatileSelection(CameraControlGizmo.NO_REGION);
        return changed;
    }

    /**
    Processes the press of a mouse button. If the cursor is over the area of
    the gizmo, a gesture of the gizmo starts (so the caller must not process
    the press in any other way: see `isGestureInCourse`).
    @param e event with coordinates relative to the viewport
    @return true if the press was taken by the gizmo
    */
    public boolean processMousePressedEvent(MouseEvent e)
    {
        endGesture();
        if ( !gizmo.containsPixel(e.getX(), e.getY()) ||
             gizmo.getCamera() == null ) {
            return false;
        }
        updateHover(e.getX(), e.getY());
        cancelAnimation();

        Camera camera = gizmo.getCamera();
        gestureInCourse = true;
        pressRegion = gizmo.pickRegion(e.getX(), e.getY());
        pressX = e.getX();
        pressY = e.getY();
        pressHudCamera = new Camera(gizmo.getHudCamera());
        pressRotation = camera.getRotation().withoutTranslation();
        pressPivot = camera.getFocusedPosition();
        pressDistance = pressPivot.subtract(camera.getPosition()).length();
        pressCompassAngle = CameraControlGizmo.compassAngleAt(
            e.getX() - gizmo.getHudX(), e.getY() - gizmo.getHudY(), pressHudCamera);
        return true;
    }

    /**
    Orbits the camera while dragging over the gizmo.
    @param e event with coordinates relative to the viewport (they can be out
    of the gizmo, or the viewport)
    @return true if the camera changed, so the view must be drawn again
    */
    public boolean processMouseDraggedEvent(MouseEvent e)
    {
        if ( !gestureInCourse ) {
            return false;
        }
        int dx = e.getX() - pressX;
        int dy = e.getY() - pressY;

        if ( !dragging ) {
            if ( Math.abs(dx) < DRAG_THRESHOLD_IN_PIXELS &&
                 Math.abs(dy) < DRAG_THRESHOLD_IN_PIXELS ) {
                return false;
            }
            dragging = true;
        }

        Matrix4x4d rotation = null;
        if ( pressRegion == CameraControlGizmo.COMPASS_REGION ) {
            rotation = compassRotation(e.getX(), e.getY(), dx);
        }
        if ( rotation == null ) {
            rotation = orbitRotation(dx, dy);
        }
        if ( rotation == null ) {
            return false;
        }
        applyRotation(rotation);
        return true;
    }

    /**
    Ends the gesture of the gizmo. If there was no drag, it was a click over
    a region: the camera starts turning to its view.
    @param e event with coordinates relative to the viewport
    @return true if the gesture was of the gizmo (so the caller must not
    process the release in any other way)
    */
    public boolean processMouseReleasedEvent(MouseEvent e)
    {
        if ( !gestureInCourse ) {
            return false;
        }
        boolean wasDragging = dragging;
        int region = pressRegion;

        endGesture();
        if ( !wasDragging && region >= 0 &&
             region < CameraControlGizmo.NUMBER_OF_CUBE_REGIONS ) {
            animateToRegion(region);
        }
        updateHover(e.getX(), e.getY());
        return true;
    }

    //= Gesture computations ==============================================

    /**
    Free orbit: the azimuth and elevation of the camera at the press change
    with the cursor movement.
    */
    private Matrix4x4d orbitRotation(int dx, int dy)
    {
        double scale = ORBIT_RADIANS_PER_PIXEL *
            ((double)gizmo.getBaseSizeInPixels() / Math.max(1, gizmo.getSizeInPixels()));
        Vector3Dd front = new Vector3Dd(pressRotation.get(0, 0),
            pressRotation.get(1, 0), pressRotation.get(2, 0)).normalized();
        double elevation = Math.asin(Math.max(-1.0, Math.min(1.0, front.z())));
        double azimuth = Math.atan2(front.y(), front.x());

        // Dragging moves the cube with the cursor: right turns the scene
        // counterclockwise (seen from above), down shows more of its top
        azimuth -= dx * scale;
        elevation -= dy * scale;
        elevation = Math.max(-MAX_ELEVATION, Math.min(MAX_ELEVATION, elevation));

        Vector3Dd newFront = new Vector3Dd(
            Math.cos(elevation) * Math.cos(azimuth),
            Math.cos(elevation) * Math.sin(azimuth),
            Math.sin(elevation));
        return CameraControlGizmo.createCameraRotation(newFront, new Vector3Dd(0, 0, 1));
    }

    /**
    Turn around Z that keeps the point of the compass grabbed under the
    cursor, measured with the HUD camera of the press (so there is no drift).
    @return the rotation, or null if the compass is seen almost edge on (then
    the free orbit is used)
    */
    private Matrix4x4d compassRotation(int x, int y, int dx)
    {
        if ( Double.isNaN(pressCompassAngle) ) {
            return null;
        }
        double angle = CameraControlGizmo.compassAngleAt(x - gizmo.getHudX(),
            y - gizmo.getHudY(), pressHudCamera);
        if ( Double.isNaN(angle) ) {
            return null;
        }
        double sweep = angle - pressCompassAngle;
        // The scene turns by the sweep, so the camera turns the other way
        return new Matrix4x4d().axisRotation(-sweep, new Vector3Dd(0, 0, 1))
            .multiply(pressRotation);
    }

    private void applyRotation(Matrix4x4d rotation)
    {
        Camera camera = gizmo.getCamera();
        Vector3Dd front = new Vector3Dd(rotation.get(0, 0), rotation.get(1, 0),
            rotation.get(2, 0)).normalized();

        camera.setRotation(rotation);
        camera.setPosition(pressPivot.subtract(front.multiply(pressDistance)));
        camera.updateVectors();
        gizmo.updateHudCamera();
    }

    private boolean updateHover(int x, int y)
    {
        boolean wasActive = gizmo.isActive();
        int previous = gizmo.getVolatileSelection();
        boolean inside = gizmo.containsPixel(x, y);

        gizmo.setActive(inside);
        gizmo.setVolatileSelection(inside ? gizmo.pickRegion(x, y) :
            CameraControlGizmo.NO_REGION);
        return wasActive != gizmo.isActive() ||
            previous != gizmo.getVolatileSelection();
    }

    private void endGesture()
    {
        gestureInCourse = false;
        dragging = false;
        pressRegion = CameraControlGizmo.NO_REGION;
        pressX = 0;
        pressY = 0;
        pressHudCamera = null;
        pressRotation = null;
        pressPivot = null;
        pressDistance = 0;
        pressCompassAngle = Double.NaN;
    }
}

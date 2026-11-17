#ifndef __CMR_CTRL_GZM_INTRCTN_TCHNQ__
#define __CMR_CTRL_GZM_INTRCTN_TCHNQ__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/gui/MouseEvent.h"

class Camera;
class CameraControlGizmo;
class CameraControlGizmoAnimation;

/**
Interaction technique to control the camera of a view with a
`CameraControlGizmo` (a ViewCube), processing only vitral events.

Mouse events must have coordinates in pixels of the viewport the gizmo is
seen in, with origin at its upper left corner (as the area of the gizmo, see
`CameraControlGizmo::placeInViewport`).

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

Turbo C++ port note: the class name has more than 32 characters; it is
renamed `CameraCtrlGizmoIntrctTechnique` for that compiler (see
`scripts/class_renames_32.csv`).
*/
class CameraControlGizmoInteractionTechnique {
public:
    /// Cursor movement, in pixels, from the press that turns a click into a
    /// drag
    enum { DRAG_THRESHOLD_IN_PIXELS = 3 };
    /// Angle turned by each pixel of free orbit, for a gizmo of base size
    static const double ORBIT_RADIANS_PER_PIXEL;
    /// Maximum elevation of the camera while orbiting freely
    static const double MAX_ELEVATION;

private:
    CameraControlGizmo* gizmo;
    double animationDurationInSeconds;
    CameraControlGizmoAnimation* animation;

    // State of the gesture in course
    BOOLEAN gestureInCourse;
    BOOLEAN dragging;
    int pressRegion;
    int pressX;
    int pressY;
    /// Copy of the HUD camera at the press (owned), or null
    Camera* pressHudCamera;
    Matrix4x4d pressRotation;
    Vector3Dd pressPivot;
    double pressDistance;
    double pressCompassAngle;

    //= Gesture computations ============================================

    /**
    Free orbit: the azimuth and elevation of the camera at the press change
    with the cursor movement.
    @return false if the rotation could not be calculated (Java: null)
    */
    BOOLEAN orbitRotation(int dx, int dy, Matrix4x4d& out);

    /**
    Turn around Z that keeps the point of the compass grabbed under the
    cursor, measured with the HUD camera of the press (so there is no drift).
    @return false if the compass is seen almost edge on (Java: null; then
    the free orbit is used)
    */
    BOOLEAN compassRotation(int x, int y, int dx, Matrix4x4d& out);

    void applyRotation(const Matrix4x4d& rotation);
    BOOLEAN updateHover(int x, int y);
    void endGesture();

    // Not copied: owns the animation and the camera of the press
    CameraControlGizmoInteractionTechnique(
        const CameraControlGizmoInteractionTechnique&);
    CameraControlGizmoInteractionTechnique& operator=(
        const CameraControlGizmoInteractionTechnique&);

public:
    /**
    @param gizmo gizmo used by this technique (not owned); its camera (see
    `CameraControlGizmo::setCamera`) is the one controlled
    */
    explicit CameraControlGizmoInteractionTechnique(CameraControlGizmo* gizmo);
    virtual ~CameraControlGizmoInteractionTechnique();

    CameraControlGizmo* getGizmo() const;

    double getAnimationDurationInSeconds() const;
    void setAnimationDurationInSeconds(double seconds);

    /**
    @return true if the cursor is over the gizmo, or a gesture of the gizmo
    is in course
    */
    BOOLEAN isActive() const;

    /**
    @return true between a press over the gizmo and its release
    */
    BOOLEAN isGestureInCourse() const;

    /**
    @return true if the camera is turning to a view
    */
    BOOLEAN isAnimating() const;

    /**
    Moves the camera along the animation in course.
    @param nanoTime current time, in nanoseconds
    @return true if the camera changed, so the view must be drawn again
    */
    BOOLEAN advanceAnimation(long long nanoTime);

    /**
    Stops the animation in course, leaving the camera where it is.
    */
    void cancelAnimation();

    /**
    Starts the turn of the camera to the view of a region of the cube.
    @param region a region of the cube
    @return true if an animation started
    */
    BOOLEAN animateToRegion(int region);

    //= Mouse ===========================================================

    /**
    Updates the highlight of the gizmo for the cursor position.
    @param e event with coordinates relative to the viewport
    @return true if the gizmo changed, so it must be drawn again
    */
    BOOLEAN processMouseMovedEvent(const MouseEvent& e);

    /**
    Called when the cursor leaves the viewport: the gizmo is not active.
    @return true if the gizmo changed
    */
    BOOLEAN processMouseExitedEvent();

    /**
    Processes the press of a mouse button. If the cursor is over the area of
    the gizmo, a gesture of the gizmo starts (so the caller must not process
    the press in any other way: see `isGestureInCourse`).
    @param e event with coordinates relative to the viewport
    @return true if the press was taken by the gizmo
    */
    BOOLEAN processMousePressedEvent(const MouseEvent& e);

    /**
    Orbits the camera while dragging over the gizmo.
    @param e event with coordinates relative to the viewport (they can be out
    of the gizmo, or the viewport)
    @return true if the camera changed, so the view must be drawn again
    */
    BOOLEAN processMouseDraggedEvent(const MouseEvent& e);

    /**
    Ends the gesture of the gizmo. If there was no drag, it was a click over
    a region: the camera starts turning to its view.
    @param e event with coordinates relative to the viewport
    @return true if the gesture was of the gizmo (so the caller must not
    process the release in any other way)
    */
    BOOLEAN processMouseReleasedEvent(const MouseEvent& e);
};

#endif

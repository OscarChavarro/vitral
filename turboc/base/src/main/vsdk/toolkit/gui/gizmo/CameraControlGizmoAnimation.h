#ifndef __CAMERA_CONTROL_GIZMO_ANIM__
#define __CAMERA_CONTROL_GIZMO_ANIM__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Camera;

/**
Smooth turn of a camera around the point it looks at, from its current
orientation to a target one, as triggered by a `CameraControlGizmo`.

The camera orbits its pivot (`Camera::getFocusedPosition()` when the
animation starts) keeping its distance: at each instant its orientation is
the initial one turned around a fixed axis (the axis of the relative rotation
from the initial to the target orientation, so the path is the shortest
one, as a spherical linear interpolation), by a fraction of the total angle
that follows an ease in / ease out curve over the duration.

It depends on no clock nor thread: the application calls `advance` with the
current time (i.e. in each frame, or from a timer) while `isRunning()`, and
draws the view again after each call.

C++ port note: times are `long long` nanoseconds, as `System::nanoTime()`
(a 32 bit `long` on Turbo C++, see `VSDK_LL`).
*/
class CameraControlGizmoAnimation {
public:
    static const double DEFAULT_DURATION_IN_SECONDS;

private:
    Camera* camera;
    Vector3Dd pivot;
    double distance;
    Matrix4x4d startRotation;
    Matrix4x4d targetRotation;
    Vector3Dd axis;
    double angle;
    long long durationInNanoseconds;
    long long startTime;
    BOOLEAN started;
    BOOLEAN running;

    void applyFraction(double fraction);
    void applyRotation(const Matrix4x4d& rotation);

public:
    /**
    @param camera camera to turn (not owned)
    @param targetRotation orientation to reach (columns front, left, up, as
    `Camera::getRotation()`)
    @param durationInSeconds duration of the turn (0 to jump at once in the
    first `advance`)
    */
    CameraControlGizmoAnimation(Camera* camera,
        const Matrix4x4d& targetRotation, double durationInSeconds);

    /**
    @return true while the camera has not reached the target orientation
    */
    BOOLEAN isRunning() const;

    Camera* getCamera() const;

    /**
    @return the total angle of the turn, in radians (from 0 to PI)
    */
    double getAngle() const;

    /**
    Places the camera where it must be at the given time. The first call
    defines the start of the animation.
    @param nanoTime current time, in nanoseconds (i.e. `System::nanoTime()`)
    @return true if the camera changed (so the view must be drawn again)
    */
    BOOLEAN advance(long long nanoTime);

    /**
    Places the camera at the target orientation at once, ending the
    animation.
    */
    void finish();

    /**
    Stops the animation where it is (i.e. the user took control of the
    camera).
    */
    void cancel();

    /**
    Smoothstep: zero speed at both ends.
    */
    static double easeInOut(double s);

    /**
    Axis and angle of a rotation matrix (its upper 3x3 part).
    @param out receives {x, y, z, angle}: a unit axis and an angle from 0 to
    PI
    */
    static void extractAxisAngle(const Matrix4x4d& r, double out[4]);
};

#endif

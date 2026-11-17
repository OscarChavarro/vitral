package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;

/**
Smooth turn of a camera around the point it looks at, from its current
orientation to a target one, as triggered by a `CameraControlGizmo`.

The camera orbits its pivot (`Camera.getFocusedPosition()` when the
animation starts) keeping its distance: at each instant its orientation is
the initial one turned around a fixed axis (the axis of the relative rotation
from the initial to the target orientation, so the path is the shortest
one, as a spherical linear interpolation), by a fraction of the total angle
that follows an ease in / ease out curve over the duration.

It depends on no clock nor thread: the application calls `advance` with the
current time (i.e. in each frame, or from a timer) while `isRunning()`, and
draws the view again after each call.
*/
public class CameraControlGizmoAnimation
{
    public static final double DEFAULT_DURATION_IN_SECONDS = 0.45;

    private final Camera camera;
    private final Vector3Dd pivot;
    private final double distance;
    private final Matrix4x4d startRotation;
    private final Matrix4x4d targetRotation;
    private final Vector3Dd axis;
    private final double angle;
    private final long durationInNanoseconds;
    private long startTime;
    private boolean started;
    private boolean running;

    /**
    @param camera camera to turn
    @param targetRotation orientation to reach (columns front, left, up, as
    `Camera.getRotation()`)
    @param durationInSeconds duration of the turn (0 to jump at once in the
    first `advance`)
    */
    public CameraControlGizmoAnimation(Camera camera, Matrix4x4d targetRotation,
        double durationInSeconds)
    {
        this.camera = camera;
        this.pivot = camera.getFocusedPosition();
        this.distance = camera.getFocusedPosition().subtract(camera.getPosition()).length();
        this.startRotation = camera.getRotation().withoutTranslation();
        this.targetRotation = new Matrix4x4d(targetRotation).withoutTranslation();
        this.durationInNanoseconds = Math.max(0, (long)(durationInSeconds * 1.0e9));

        double[] axisAngle = extractAxisAngle(
            this.targetRotation.multiply(startRotation.transpose()));
        this.axis = new Vector3Dd(axisAngle[0], axisAngle[1], axisAngle[2]);
        this.angle = axisAngle[3];
        this.started = false;
        this.running = true;
    }

    /**
    @return true while the camera has not reached the target orientation
    */
    public boolean isRunning()
    {
        return running;
    }

    public Camera getCamera()
    {
        return camera;
    }

    /**
    @return the total angle of the turn, in radians (from 0 to PI)
    */
    public double getAngle()
    {
        return angle;
    }

    /**
    Places the camera where it must be at the given time. The first call
    defines the start of the animation.
    @param nanoTime current time, in nanoseconds (i.e. `System.nanoTime()`)
    @return true if the camera changed (so the view must be drawn again)
    */
    public boolean advance(long nanoTime)
    {
        if ( !running ) {
            return false;
        }
        if ( !started ) {
            started = true;
            startTime = nanoTime;
        }
        double s = 1.0;
        if ( durationInNanoseconds > 0 ) {
            s = (double)(nanoTime - startTime) / (double)durationInNanoseconds;
        }
        if ( s >= 1.0 ) {
            finish();
            return true;
        }
        applyFraction(easeInOut(Math.max(0.0, s)));
        return true;
    }

    /**
    Places the camera at the target orientation at once, ending the
    animation.
    */
    public void finish()
    {
        running = false;
        applyRotation(targetRotation);
    }

    /**
    Stops the animation where it is (i.e. the user took control of the
    camera).
    */
    public void cancel()
    {
        running = false;
    }

    /**
    Smoothstep: zero speed at both ends.
    */
    static double easeInOut(double s)
    {
        return s * s * (3 - 2 * s);
    }

    private void applyFraction(double fraction)
    {
        if ( angle == 0.0 ) {
            applyRotation(targetRotation);
            return;
        }
        Matrix4x4d partial = new Matrix4x4d().axisRotation(angle * fraction, axis);
        applyRotation(partial.multiply(startRotation));
    }

    private void applyRotation(Matrix4x4d rotation)
    {
        Vector3Dd front = new Vector3Dd(rotation.get(0, 0), rotation.get(1, 0),
            rotation.get(2, 0)).normalized();

        camera.setRotation(rotation);
        camera.setPosition(pivot.subtract(front.multiply(distance)));
        camera.updateVectors();
    }

    /**
    Axis and angle of a rotation matrix (its upper 3x3 part).
    @return {x, y, z, angle}: a unit axis and an angle from 0 to PI
    */
    static double[] extractAxisAngle(Matrix4x4d r)
    {
        double trace = r.get(0, 0) + r.get(1, 1) + r.get(2, 2);
        double cosine = Math.max(-1.0, Math.min(1.0, (trace - 1.0) / 2.0));
        double angle = Math.acos(cosine);
        double x = r.get(2, 1) - r.get(1, 2);
        double y = r.get(0, 2) - r.get(2, 0);
        double z = r.get(1, 0) - r.get(0, 1);
        double length = Math.sqrt(x * x + y * y + z * z);

        if ( angle < 1e-9 ) {
            return new double[] {0, 0, 1, 0};
        }
        if ( length > 1e-6 ) {
            return new double[] {x / length, y / length, z / length, angle};
        }

        // Half turn: R = 2 a a^T - I, the axis comes from the diagonal
        double xx = Math.sqrt(Math.max(0, (r.get(0, 0) + 1) / 2));
        double yy = Math.sqrt(Math.max(0, (r.get(1, 1) + 1) / 2));
        double zz = Math.sqrt(Math.max(0, (r.get(2, 2) + 1) / 2));
        if ( xx >= yy && xx >= zz ) {
            yy = (r.get(0, 1) + r.get(1, 0)) / (4 * xx);
            zz = (r.get(0, 2) + r.get(2, 0)) / (4 * xx);
        }
        else if ( yy >= zz ) {
            xx = (r.get(0, 1) + r.get(1, 0)) / (4 * yy);
            zz = (r.get(1, 2) + r.get(2, 1)) / (4 * yy);
        }
        else {
            xx = (r.get(0, 2) + r.get(2, 0)) / (4 * zz);
            yy = (r.get(1, 2) + r.get(2, 1)) / (4 * zz);
        }
        double l = Math.sqrt(xx * xx + yy * yy + zz * zz);
        return new double[] {xx / l, yy / l, zz / l, Math.PI};
    }
}

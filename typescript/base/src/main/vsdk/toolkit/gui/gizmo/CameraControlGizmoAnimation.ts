import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Camera } from "../../environment/camera/Camera.js";

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

Java's `long` nanosecond times are numbers here (exact up to 2^53 ns, more
than 104 days).
*/
export class CameraControlGizmoAnimation {
    public static readonly DEFAULT_DURATION_IN_SECONDS: number = 0.45;

    private readonly camera: Camera;
    private readonly pivot: Vector3Dd;
    private readonly distance: number;
    private readonly startRotation: Matrix4x4d;
    private readonly targetRotation: Matrix4x4d;
    private readonly axis: Vector3Dd;
    private readonly angle: number;
    private readonly durationInNanoseconds: number;
    private startTime: number = 0;
    private started: boolean;
    private running: boolean;

    /**
    @param camera camera to turn
    @param targetRotation orientation to reach (columns front, left, up, as
    `Camera.getRotation()`)
    @param durationInSeconds duration of the turn (0 to jump at once in the
    first `advance`)
    */
    public constructor(camera: Camera, targetRotation: Matrix4x4d, durationInSeconds: number) {
        this.camera = camera;
        this.pivot = camera.getFocusedPosition();
        this.distance = camera.getFocusedPosition().subtract(camera.getPosition()).length();
        this.startRotation = camera.getRotation().withoutTranslation();
        this.targetRotation = new Matrix4x4d(targetRotation).withoutTranslation();
        // Java's (long) cast truncates towards zero
        this.durationInNanoseconds = JavaMath.max(0, Math.trunc(durationInSeconds * 1.0e9));

        const axisAngle = CameraControlGizmoAnimation.extractAxisAngle(
            this.targetRotation.multiply(this.startRotation.transpose()),
        );
        this.axis = new Vector3Dd(axisAngle[0]!, axisAngle[1]!, axisAngle[2]!);
        this.angle = axisAngle[3]!;
        this.started = false;
        this.running = true;
    }

    /**
    @return true while the camera has not reached the target orientation
    */
    public isRunning(): boolean {
        return this.running;
    }

    public getCamera(): Camera {
        return this.camera;
    }

    /**
    @return the total angle of the turn, in radians (from 0 to PI)
    */
    public getAngle(): number {
        return this.angle;
    }

    /**
    Places the camera where it must be at the given time. The first call
    defines the start of the animation.
    @param nanoTime current time, in nanoseconds (i.e. `System.nanoTime()`)
    @return true if the camera changed (so the view must be drawn again)
    */
    public advance(nanoTime: number): boolean {
        if (!this.running) {
            return false;
        }
        if (!this.started) {
            this.started = true;
            this.startTime = nanoTime;
        }
        let s = 1.0;
        if (this.durationInNanoseconds > 0) {
            s = (nanoTime - this.startTime) / this.durationInNanoseconds;
        }
        if (s >= 1.0) {
            this.finish();
            return true;
        }
        this.applyFraction(CameraControlGizmoAnimation.easeInOut(JavaMath.max(0.0, s)));
        return true;
    }

    /**
    Places the camera at the target orientation at once, ending the
    animation.
    */
    public finish(): void {
        this.running = false;
        this.applyRotation(this.targetRotation);
    }

    /**
    Stops the animation where it is (i.e. the user took control of the
    camera).
    */
    public cancel(): void {
        this.running = false;
    }

    /**
    Smoothstep: zero speed at both ends. Package private in Java.
    */
    public static easeInOut(s: number): number {
        return s * s * (3 - 2 * s);
    }

    private applyFraction(fraction: number): void {
        if (this.angle === 0.0) {
            this.applyRotation(this.targetRotation);
            return;
        }
        const partial = new Matrix4x4d().axisRotation(this.angle * fraction, this.axis);
        this.applyRotation(partial.multiply(this.startRotation));
    }

    private applyRotation(rotation: Matrix4x4d): void {
        const front = new Vector3Dd(rotation.get(0, 0), rotation.get(1, 0), rotation.get(2, 0)).normalized();

        this.camera.setRotation(rotation);
        this.camera.setPosition(this.pivot.subtract(front.multiply(this.distance)));
        this.camera.updateVectors();
    }

    /**
    Axis and angle of a rotation matrix (its upper 3x3 part). Package private
    in Java.
    @return {x, y, z, angle}: a unit axis and an angle from 0 to PI
    */
    public static extractAxisAngle(r: Matrix4x4d): number[] {
        const trace = r.get(0, 0) + r.get(1, 1) + r.get(2, 2);
        const cosine = JavaMath.max(-1.0, JavaMath.min(1.0, (trace - 1.0) / 2.0));
        const angle = JavaMath.acos(cosine);
        const x = r.get(2, 1) - r.get(1, 2);
        const y = r.get(0, 2) - r.get(2, 0);
        const z = r.get(1, 0) - r.get(0, 1);
        const length = JavaMath.sqrt(x * x + y * y + z * z);

        if (angle < 1e-9) {
            return [0, 0, 1, 0];
        }
        if (length > 1e-6) {
            return [x / length, y / length, z / length, angle];
        }

        // Half turn: R = 2 a a^T - I, the axis comes from the diagonal
        let xx = JavaMath.sqrt(JavaMath.max(0, (r.get(0, 0) + 1) / 2));
        let yy = JavaMath.sqrt(JavaMath.max(0, (r.get(1, 1) + 1) / 2));
        let zz = JavaMath.sqrt(JavaMath.max(0, (r.get(2, 2) + 1) / 2));
        if (xx >= yy && xx >= zz) {
            yy = (r.get(0, 1) + r.get(1, 0)) / (4 * xx);
            zz = (r.get(0, 2) + r.get(2, 0)) / (4 * xx);
        } else if (yy >= zz) {
            xx = (r.get(0, 1) + r.get(1, 0)) / (4 * yy);
            zz = (r.get(1, 2) + r.get(2, 1)) / (4 * yy);
        } else {
            xx = (r.get(0, 2) + r.get(2, 0)) / (4 * zz);
            yy = (r.get(1, 2) + r.get(2, 1)) / (4 * zz);
        }
        const l = JavaMath.sqrt(xx * xx + yy * yy + zz * zz);
        return [xx / l, yy / l, zz / l, JavaMath.PI];
    }
}

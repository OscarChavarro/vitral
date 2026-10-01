import { Math as JavaMath } from "../../../java/lang/Math.js";
import { Matrix4x4d } from "../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../environment/camera/Camera.js";
import { CameraController } from "./CameraController.js";
import { KeyEvent } from "./KeyEvent.js";
import { MouseEvent } from "./MouseEvent.js";

export class CameraControllerAquynza extends CameraController {
    private camera: Camera;
    private oldMouseX: number;
    private oldMouseY: number;
    private deltaMovement: number;

    public constructor(camera: Camera) {
        super();
        this.camera = camera;
        this.oldMouseX = 0;
        this.oldMouseY = 0;
        this.deltaMovement = 0.25;
    }

    public getDeltaMovement(): number {
        return this.deltaMovement;
    }

    public override setDeltaMovement(value: number): void {
        this.deltaMovement = value;
    }

    private augmentLogarithmic(val: number, EPSILON: number): number {
        if (val < 0.001) val += 0.0001;
        else if (val < 0.01) val += 0.001;
        else if (val < 0.1 - EPSILON) val += 0.01;
        else if (val < 1 - EPSILON) val += 0.1;
        else if (val < 10 - EPSILON) val += 1;
        else if (val < 100 - EPSILON) val += 10;
        else if (val < 1000 - EPSILON) val += 100;
        else if (val < 10000 - EPSILON) val += 1000;
        else if (val < 100000 - EPSILON) val += 10000;
        else if (val < 1000000 - EPSILON) val += 100000;
        else if (val < 10000000 - EPSILON) val *= 2;
        else val = 10000000;

        return val;
    }

    private diminishLogarithmic(val: number, EPSILON: number): number {
        if (val > 10000000 + EPSILON) val /= 2;
        else if (val > 1000000 + EPSILON) val -= 1000000;
        else if (val > 100000 + EPSILON) val -= 100000;
        else if (val > 10000 + EPSILON) val -= 10000;
        else if (val > 1000 + EPSILON) val -= 1000;
        else if (val > 100 + EPSILON) val -= 100;
        else if (val > 10 + EPSILON) val -= 10;
        else if (val > 1 + EPSILON) val -= 1;
        else if (val > 0.1 + EPSILON) val -= 0.1;
        else if (val > 0.01 + EPSILON) val -= 0.01;
        else if (val > 0.001 + EPSILON) val -= 0.001;
        else if (val > 0.0001 + EPSILON) val -= 0.0001;
        else val = 0.0001;
        return val;
    }

    public override processKeyPressedEvent(keyEvent: KeyEvent): boolean {
        // Local copy of the Camera's internal parameters
        let eyePosition: Vector3Dd;
        let focusedPosition: Vector3Dd;
        let R: Matrix4x4d; // Camera rotation matrix
        let projectionMode: number;
        let fov: number;
        let orthogonalZoom: number;
        let nearPlaneDistance: number;
        let farPlaneDistance: number;

        // Internal variables to control the interaction
        let yaw: number;
        let pitch: number;
        let roll: number;
        let angleInc: number;
        let updated = false;
        const EPSILON = 0.0001;

        // 1. Obtain a copy of the camera's internal parameters
        eyePosition = this.camera.getPosition();
        focusedPosition = this.camera.getFocusedPosition();
        R = this.camera.getRotation();
        projectionMode = this.camera.getProjectionMode();
        fov = this.camera.getFov();
        orthogonalZoom = this.camera.getOrthogonalZoom();
        nearPlaneDistance = this.camera.getNearPlaneDistance();
        farPlaneDistance = this.camera.getFarPlaneDistance();

        // 2. Calculate variables used for interaction manipulation
        yaw = R.obtainEulerYawAngle();
        pitch = R.obtainEulerPitchAngle();
        roll = R.obtainEulerRollAngle();

        if (fov > 90) angleInc = JavaMath.toRadians(10);
        else if (fov > 45) angleInc = JavaMath.toRadians(5);
        else if (fov > 15) angleInc = JavaMath.toRadians(2.5);
        else if (fov > 5) angleInc = JavaMath.toRadians(1);
        else angleInc = JavaMath.toRadians(0.1);

        // 3. Event processing: update the copy of the camera's internal parameters
        switch (keyEvent.keycode) {
            case KeyEvent.KEY_UP:
                pitch -= angleInc;
                if (pitch < JavaMath.toRadians(-90)) pitch = JavaMath.toRadians(-90);
                updated = true;
                break;
            case KeyEvent.KEY_DOWN:
                pitch += angleInc;
                if (pitch > JavaMath.toRadians(90)) pitch = JavaMath.toRadians(90);
                updated = true;
                break;
            case KeyEvent.KEY_LEFT:
                yaw += angleInc;
                while (yaw >= JavaMath.toRadians(360)) yaw -= JavaMath.toRadians(360);
                updated = true;
                break;
            case KeyEvent.KEY_RIGHT:
                yaw -= angleInc;
                while (yaw < 0) yaw += JavaMath.toRadians(360);
                updated = true;
                break;

            // Position
            case KeyEvent.KEY_x:
                eyePosition = eyePosition.withX(eyePosition.x() - this.deltaMovement);
                focusedPosition = focusedPosition.withX(focusedPosition.x() - this.deltaMovement);
                updated = true;
                break;
            case KeyEvent.KEY_X:
                eyePosition = eyePosition.withX(eyePosition.x() + this.deltaMovement);
                focusedPosition = focusedPosition.withX(focusedPosition.x() + this.deltaMovement);
                updated = true;
                break;
            case KeyEvent.KEY_y:
                eyePosition = eyePosition.withY(eyePosition.y() - this.deltaMovement);
                focusedPosition = focusedPosition.withY(focusedPosition.y() - this.deltaMovement);
                updated = true;
                break;
            case KeyEvent.KEY_Y:
                eyePosition = eyePosition.withY(eyePosition.y() + this.deltaMovement);
                focusedPosition = focusedPosition.withY(focusedPosition.y() + this.deltaMovement);
                updated = true;
                break;
            case KeyEvent.KEY_z:
                eyePosition = eyePosition.withZ(eyePosition.z() - this.deltaMovement);
                focusedPosition = focusedPosition.withZ(focusedPosition.z() - this.deltaMovement);
                updated = true;
                break;
            case KeyEvent.KEY_Z:
                eyePosition = eyePosition.withZ(eyePosition.z() + this.deltaMovement);
                focusedPosition = focusedPosition.withZ(focusedPosition.z() + this.deltaMovement);
                updated = true;
                break;
            // Rotation
            case KeyEvent.KEY_S:
                roll -= JavaMath.toRadians(5);
                while (roll < 0) roll += JavaMath.toRadians(360);
                updated = true;
                break;
            case KeyEvent.KEY_s:
                roll += JavaMath.toRadians(5);
                while (roll > JavaMath.toRadians(360)) roll -= JavaMath.toRadians(360);
                updated = true;
                break;

            // View volume modification
            case KeyEvent.KEY_A:
                if (this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
                    orthogonalZoom /= 2;
                } else {
                    if (fov < 0.1 - EPSILON) fov += 0.1;
                    else if (fov < 1 - EPSILON) fov++;
                    else if (fov < 175 - EPSILON) fov += 5;
                }
                updated = true;
                break;
            case KeyEvent.KEY_a:
                if (this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
                    orthogonalZoom *= 2;
                } else {
                    if (fov > 5 + EPSILON) fov -= 5;
                    else if (fov > 1 + EPSILON) fov--;
                    else if (fov > 0.1 + EPSILON) fov -= 0.1;
                }
                updated = true;
                break;

            case KeyEvent.KEY_N:
                nearPlaneDistance = this.augmentLogarithmic(nearPlaneDistance, EPSILON);
                updated = true;
                break;
            case KeyEvent.KEY_n:
                nearPlaneDistance = this.diminishLogarithmic(nearPlaneDistance, EPSILON);
                updated = true;
                break;

            case KeyEvent.KEY_F:
                farPlaneDistance = this.augmentLogarithmic(farPlaneDistance, EPSILON);
                updated = true;
                break;
            case KeyEvent.KEY_f:
                farPlaneDistance = this.diminishLogarithmic(farPlaneDistance, EPSILON);
                updated = true;
                break;

            case KeyEvent.KEY_p: // Rote el modo de proyeccion
                switch (projectionMode) {
                    case Camera.PROJECTION_MODE_PERSPECTIVE:
                        projectionMode = Camera.PROJECTION_MODE_ORTHOGONAL;
                        break;
                    default:
                        projectionMode = Camera.PROJECTION_MODE_PERSPECTIVE;
                        break;
                }
                updated = true;
                break;

            // Queries
            case KeyEvent.KEY_i:
                console.log(String(this.camera));
                break;
        }

        // 4. Update camera's internal parameters from local copy
        R = R.eulerAnglesRotation(yaw, pitch, roll);

        this.camera.setPosition(eyePosition);
        this.camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
        this.camera.setRotation(R);
        this.camera.setOrthogonalZoom(orthogonalZoom);
        this.camera.setFov(fov);
        this.camera.setProjectionMode(projectionMode);
        this.camera.setNearPlaneDistance(nearPlaneDistance);
        this.camera.setFarPlaneDistance(farPlaneDistance);

        return updated;
    }

    public override processKeyReleasedEvent(_keyEvent: KeyEvent): boolean {
        return false;
    }

    public override processMousePressedEvent(e: MouseEvent): boolean {
        this.oldMouseX = e.getX();
        this.oldMouseY = e.getY();
        return false;
    }

    public override processMouseReleasedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public override processMouseClickedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public override processMouseMovedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public override processMouseDraggedEvent(e: MouseEvent): boolean {
        //------------------------------------------------------------
        let deltaX: number;
        let deltaY: number;
        let updated = false;
        const senseFactor: number = this.deltaMovement / 5;

        deltaX = e.getX() - this.oldMouseX;
        deltaY = e.getY() - this.oldMouseY;

        if (deltaX > 5) deltaX = 5;
        if (deltaX < -5) deltaX = -5;
        if (deltaY > 5) deltaY = 5;
        if (deltaY < -5) deltaY = -5;

        //------------------------------------------------------------
        let R: Matrix4x4d; // Camera rotation matrix
        let DR: Matrix4x4d;
        let eyePosition: Vector3Dd;
        let focusedPosition: Vector3Dd;
        let ax: number;
        let ay: number;

        // Obtain a copy of the camera's internal parameters
        eyePosition = this.camera.getPosition();
        focusedPosition = this.camera.getFocusedPosition();

        const modifiers: number = e.getModifiers();

        R = this.camera.getRotation();
        const u = new Vector3Dd(R.get(0, 0), R.get(1, 0), R.get(2, 0));
        const v = new Vector3Dd(R.get(0, 1), R.get(1, 1), R.get(2, 1));
        const w = new Vector3Dd(R.get(0, 2), R.get(1, 2), R.get(2, 2));

        if ((modifiers & MouseEvent.BUTTON1_DOWN_MASK) !== 0) {
            // Turn
            ax = -Math.min(2, 0.01 * deltaX);
            ay = Math.min(2, 0.01 * deltaY);

            DR = new Matrix4x4d().axisRotation(ay, v.x(), v.y(), v.z());
            R = DR.multiply(R);

            DR = new Matrix4x4d().axisRotation(ax, w.x(), w.y(), w.z());
            R = DR.multiply(R);

            updated = true;
        } else if ((modifiers & MouseEvent.BUTTON2_DOWN_MASK) !== 0) {
            // Move
            eyePosition = eyePosition.subtract(v.multiply(senseFactor * deltaX));
            eyePosition = eyePosition.subtract(w.multiply(senseFactor * deltaY));
            focusedPosition = focusedPosition.subtract(v.multiply(senseFactor * deltaX));
            focusedPosition = focusedPosition.subtract(w.multiply(senseFactor * deltaY));
            updated = true;
        } else if ((modifiers & MouseEvent.BUTTON3_DOWN_MASK) !== 0) {
            // Advance
            eyePosition = eyePosition.subtract(u.multiply(senseFactor * deltaY));
            ax = Math.min(2, 0.01 * deltaX);
            DR = new Matrix4x4d().axisRotation(ax, u.x(), u.y(), u.z());
            R = DR.multiply(R);
            updated = true;
        }

        // Update camera's internal parameters from local copy
        //R = R.eulerAnglesRotation(yaw, pitch, roll);
        this.camera.setPosition(eyePosition);
        this.camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
        this.camera.setRotation(R);

        //------------------------------------------------------------
        this.oldMouseX = e.getX();
        this.oldMouseY = e.getY();
        return updated;
    }

    public override processMouseWheelEvent(e: MouseEvent): boolean {
        //------------------------------------------------------------
        let fov: number;
        const EPSILON = 0.0001;
        let orthogonalZoom: number;

        fov = this.camera.getFov();
        orthogonalZoom = this.camera.getOrthogonalZoom();

        // Java computes an unused angle increment here, from the same field
        // of view thresholds as `processKeyPressedEvent`

        const clicks: number = e.getClicks();

        //------------------------------------------------------------
        if (clicks > 0) {
            if (this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
                orthogonalZoom /= 2 * clicks;
            } else {
                if (fov < 0.1 - EPSILON) fov += 0.1 * clicks;
                else if (fov < 1 - EPSILON) fov += clicks;
                else if (fov < 175 - EPSILON) fov += 5 * clicks;
            }
        } else if (clicks < 0) {
            if (this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
                orthogonalZoom *= 2 * clicks;
            } else {
                if (fov > 5 + EPSILON) fov += 5 * clicks;
                else if (fov > 1 + EPSILON) fov += clicks;
                else if (fov > 0.1 + EPSILON) fov += 0.1 * clicks;
            }
        }

        //------------------------------------------------------------
        this.camera.setFov(fov);
        this.camera.setOrthogonalZoom(orthogonalZoom);

        return true;
    }

    public override getCamera(): Camera {
        return this.camera;
    }

    public override setCamera(camera: Camera): void {
        this.camera = camera;
    }
}

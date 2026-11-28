import { Math as JavaMath } from "../../../java/lang/Math.js";
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import { Matrix4x4d } from "../common/linealAlgebra/Matrix4x4d.js";
import { Camera } from "../environment/camera/Camera.js";
import { CameraController } from "./CameraController.js";
import { KeyEvent } from "./KeyEvent.js";
import { MouseEvent } from "./MouseEvent.js";

export class CameraControllerOrbiter extends CameraController {
    private camera: Camera;
    private oldMouseX: number;
    private oldMouseY: number;
    private deltaMovement: number;
    private pointOfInterest: Vector3Dd;

    public constructor(camera: Camera) {
        super();
        this.camera = camera;
        this.oldMouseX = 0;
        this.oldMouseY = 0;
        this.deltaMovement = 0.25;
        this.pointOfInterest = new Vector3Dd(0, 0, 0);
    }

    public getDeltaMovement(): number {
        return this.deltaMovement;
    }

    public override setDeltaMovement(val: number): void {
        this.deltaMovement = val;
    }

    public getPointOfInterest(): Vector3Dd {
        return new Vector3Dd(this.pointOfInterest);
    }

    public setPointOfInterest(pointOfInterest: Vector3Dd | null): void {
        if (pointOfInterest === null) {
            this.pointOfInterest = new Vector3Dd(0, 0, 0);
            return;
        }
        this.pointOfInterest = Vector3Dd.copyOf(pointOfInterest);
    }

    private syncPointOfInterestWithFocusedPosition(focusedPosition: Vector3Dd): void {
        this.pointOfInterest = Vector3Dd.copyOf(focusedPosition);
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

    private orbitAroundPointOfInterest(yawDelta: number, pitchDelta: number): boolean {
        const eyePosition: Vector3Dd = this.camera.getPosition();
        let offset: Vector3Dd = eyePosition.subtract(this.pointOfInterest);

        if (offset.length() < 1e-9) {
            return false;
        }

        const R: Matrix4x4d = this.camera.getRotation();
        let DR: Matrix4x4d = new Matrix4x4d();
        let axisLeft: Vector3Dd = new Vector3Dd(R.get(0, 1), R.get(1, 1), R.get(2, 1));
        let axisUp: Vector3Dd = new Vector3Dd(R.get(0, 2), R.get(1, 2), R.get(2, 2));
        axisLeft = axisLeft.normalized();
        axisUp = axisUp.normalized();

        if (Math.abs(pitchDelta) > 0) {
            DR = new Matrix4x4d().axisRotation(pitchDelta, axisLeft.x(), axisLeft.y(), axisLeft.z());
            offset = DR.multiply(offset);
        }
        if (Math.abs(yawDelta) > 0) {
            DR = new Matrix4x4d().axisRotation(yawDelta, axisUp.x(), axisUp.y(), axisUp.z());
            offset = DR.multiply(offset);
        }

        const newEyePosition: Vector3Dd = this.pointOfInterest.add(offset);
        let newFrontDirection: Vector3Dd = this.pointOfInterest.subtract(newEyePosition);
        newFrontDirection = newFrontDirection.normalized();
        const upHint: Vector3Dd = this.camera.getUp();
        if (Math.abs(newFrontDirection.dotProduct(upHint)) > 0.99999) {
            return false;
        }

        this.camera.setPosition(newEyePosition);
        this.camera.setFocusedPositionMaintainingOrthogonality(this.pointOfInterest);
        return true;
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
        let angleInc: number;
        let updated = false;
        const EPSILON = 0.0001;
        let roll: number;

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
        roll = R.obtainEulerRollAngle();

        if (fov > 90) angleInc = JavaMath.toRadians(10);
        else if (fov > 45) angleInc = JavaMath.toRadians(5);
        else if (fov > 15) angleInc = JavaMath.toRadians(2.5);
        else if (fov > 5) angleInc = JavaMath.toRadians(1);
        else angleInc = JavaMath.toRadians(0.1);

        // 3. Event processing: update the copy of the camera's internal parameters
        switch (keyEvent.keycode) {
            case KeyEvent.KEY_UP:
                updated = this.orbitAroundPointOfInterest(0, -angleInc);
                break;
            case KeyEvent.KEY_DOWN:
                updated = this.orbitAroundPointOfInterest(0, angleInc);
                break;
            case KeyEvent.KEY_LEFT:
                updated = this.orbitAroundPointOfInterest(angleInc, 0);
                break;
            case KeyEvent.KEY_RIGHT:
                updated = this.orbitAroundPointOfInterest(-angleInc, 0);
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
                R = R.eulerAnglesRotation(R.obtainEulerYawAngle(), R.obtainEulerPitchAngle(), roll);
                updated = true;
                break;
            case KeyEvent.KEY_s:
                roll += JavaMath.toRadians(5);
                while (roll > JavaMath.toRadians(360)) roll -= JavaMath.toRadians(360);
                R = R.eulerAnglesRotation(R.obtainEulerYawAngle(), R.obtainEulerPitchAngle(), roll);
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

            case KeyEvent.KEY_p: // Rotate projection mode
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
        if (
            keyEvent.keycode !== KeyEvent.KEY_UP &&
            keyEvent.keycode !== KeyEvent.KEY_DOWN &&
            keyEvent.keycode !== KeyEvent.KEY_LEFT &&
            keyEvent.keycode !== KeyEvent.KEY_RIGHT
        ) {
            this.camera.setPosition(eyePosition);
            this.camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
            this.camera.setRotation(R);

            if (
                keyEvent.keycode === KeyEvent.KEY_x ||
                keyEvent.keycode === KeyEvent.KEY_X ||
                keyEvent.keycode === KeyEvent.KEY_y ||
                keyEvent.keycode === KeyEvent.KEY_Y ||
                keyEvent.keycode === KeyEvent.KEY_z ||
                keyEvent.keycode === KeyEvent.KEY_Z
            ) {
                this.syncPointOfInterestWithFocusedPosition(focusedPosition);
            }
        }
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
        let ax: number, ay: number;

        // Obtain a copy of the camera's internal parameters
        eyePosition = this.camera.getPosition();
        focusedPosition = this.camera.getFocusedPosition();

        let modifiers: number;

        modifiers = e.getModifiers();

        R = this.camera.getRotation();
        const u: Vector3Dd = new Vector3Dd(R.get(0, 0), R.get(1, 0), R.get(2, 0));
        const v: Vector3Dd = new Vector3Dd(R.get(0, 1), R.get(1, 1), R.get(2, 1));
        const w: Vector3Dd = new Vector3Dd(R.get(0, 2), R.get(1, 2), R.get(2, 2));

        if ((modifiers & MouseEvent.BUTTON1_DOWN_MASK) !== 0) {
            // Orbit around point of interest
            ax = -Math.min(2, 0.01 * deltaX);
            ay = Math.min(2, 0.01 * deltaY);
            updated = this.orbitAroundPointOfInterest(ax, ay);
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
        if ((modifiers & MouseEvent.BUTTON1_DOWN_MASK) === 0) {
            this.camera.setPosition(eyePosition);
            this.camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
            this.camera.setRotation(R);

            if ((modifiers & MouseEvent.BUTTON2_DOWN_MASK) !== 0) {
                this.syncPointOfInterestWithFocusedPosition(focusedPosition);
            }
        }

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

        // Java computes an unused `angleInc` here from the field of view.

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

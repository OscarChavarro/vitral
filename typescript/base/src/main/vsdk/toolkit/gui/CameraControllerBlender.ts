import { Math as JavaMath } from "../../../java/lang/Math.js";
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import { Matrix4x4d } from "../common/linealAlgebra/Matrix4x4d.js";
import { Camera } from "../environment/camera/Camera.js";
import { CameraController } from "./CameraController.js";
import { KeyEvent } from "./KeyEvent.js";
import type { MouseEvent } from "./MouseEvent.js";

export class CameraControllerBlender extends CameraController {
    private camera: Camera;

    public constructor(camera: Camera) {
        super();
        this.camera = camera;
    }

    // Java keeps the two logarithmic helpers below, only used by the
    // commented-out character branch of `processKeyPressedEvent`.
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
        // (Java also reads `unicodeId` and computes an unused `angleInc`
        // from the field of view.)
        let keycode: number;
        const deltaMov = 100.0;
        let updated = false;
        let mask: number;
        const CTRL_MASK = 2; //java.awt.event.KeyEvent.CTRL_DOWN_MASK;
        let u: Vector3Dd, v: Vector3Dd, w: Vector3Dd;
        //LRR16ene2015.
        let tranMat: Matrix4x4d = new Matrix4x4d();
        let angRot: number;

        angRot = JavaMath.toRadians(5);
        // 1. Obtain a copy of the camera's internal parameters
        eyePosition = this.camera.getPosition();
        focusedPosition = this.camera.getFocusedPosition();
        R = this.camera.getRotation();
        projectionMode = this.camera.getProjectionMode();
        fov = this.camera.getFov();
        nearPlaneDistance = this.camera.getNearPlaneDistance();
        farPlaneDistance = this.camera.getFarPlaneDistance();

        // 2. Calculate variables used for interaction manipulation
        keycode = keyEvent.keycode;
        mask = keyEvent.modifierMask;

        u = new Vector3Dd(R.get(0, 0), R.get(1, 0), R.get(2, 0));
        v = new Vector3Dd(R.get(0, 1), R.get(1, 1), R.get(2, 1));
        w = new Vector3Dd(R.get(0, 2), R.get(1, 2), R.get(2, 2));

        // 3. Event processing: update the copy of the camera's internal parameters
        switch (keycode) {
            case KeyEvent.KEY_X:
                // Java keeps the shifted/unshifted X translation commented out.
                break;

            case KeyEvent.KEY_NUM5: // Rote el modo de proyeccion
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

            case KeyEvent.KEY_NUM4:
                if ((mask & CTRL_MASK) === 0) {
                    // Rotation
                    tranMat = tranMat.axisRotation(-angRot, 0, 0, 1);
                    R = tranMat.multiply(R);
                    eyePosition = tranMat.multiply(eyePosition);
                    updated = true;
                } else if ((mask & CTRL_MASK) !== 0) {
                    // Translation
                    eyePosition = eyePosition.add(v.multiply(deltaMov));
                    focusedPosition = focusedPosition.add(v.multiply(deltaMov));
                    updated = true;
                }
                break;

            case KeyEvent.KEY_NUM6:
                if ((mask & CTRL_MASK) === 0) {
                    // Rotation
                    tranMat = tranMat.axisRotation(angRot, 0, 0, 1);
                    R = tranMat.multiply(R);
                    eyePosition = tranMat.multiply(eyePosition);
                    updated = true;
                } else if ((mask & CTRL_MASK) !== 0) {
                    // Translation
                    eyePosition = eyePosition.subtract(v.multiply(deltaMov));
                    focusedPosition = focusedPosition.subtract(v.multiply(deltaMov));
                    updated = true;
                }
                break;

            case KeyEvent.KEY_NUM2:
                if ((mask & CTRL_MASK) === 0) {
                    // Rotation
                    tranMat = tranMat.axisRotation(-angRot, v);
                    R = tranMat.multiply(R);
                    eyePosition = tranMat.multiply(eyePosition);
                    updated = true;
                } else if ((mask & CTRL_MASK) !== 0) {
                    // Translation
                    eyePosition = eyePosition.subtract(w.multiply(deltaMov));
                    focusedPosition = focusedPosition.subtract(w.multiply(deltaMov));
                    updated = true;
                }
                break;

            case KeyEvent.KEY_NUM8:
                if ((mask & CTRL_MASK) === 0) {
                    // Rotation
                    tranMat = tranMat.axisRotation(angRot, v);
                    R = tranMat.multiply(R);
                    eyePosition = tranMat.multiply(eyePosition);
                    updated = true;
                } else if ((mask & CTRL_MASK) !== 0) {
                    // Translation
                    eyePosition = eyePosition.add(w.multiply(deltaMov));
                    focusedPosition = focusedPosition.add(w.multiply(deltaMov));
                    updated = true;
                }
                break;

            case KeyEvent.KEY_NUM1:
                updated = true;
                R = R.axisRotation(0, 0, 0, 1);
                break;

            case 107: // Warning: How to tell java.awt.event.KeyEvent.VK_NUMPAD_PLUS: ?
                // Translation
                eyePosition = eyePosition.add(u.multiply(deltaMov));
                focusedPosition = focusedPosition.add(u.multiply(deltaMov));
                updated = true;
                break;

            case 109: // Warning: How to tell java.awt.event.KeyEvent.VK_NUMPAD_LESS: ?
                // Translation
                eyePosition = eyePosition.subtract(u.multiply(deltaMov));
                focusedPosition = focusedPosition.subtract(u.multiply(deltaMov));
                updated = true;
                break;
        }

        // Heuristic to simulate Blender's orthogonal behavior
        const d: number = eyePosition.length();
        if (d < 0.1) {
            orthogonalZoom = 10;
        } else {
            orthogonalZoom = 1 / d;
        }

        // 4. Update camera's internal parameters from local copy
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

    public override processMousePressedEvent(_e: MouseEvent): boolean {
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

    public override processMouseDraggedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public override processMouseWheelEvent(_e: MouseEvent): boolean {
        return false;
    }

    public override getCamera(): Camera {
        return this.camera;
    }

    public override setCamera(camera: Camera): void {
        this.camera = camera;
    }

    public override setDeltaMovement(_factor: number): void {}
}

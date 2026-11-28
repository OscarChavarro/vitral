import { Math as JavaMath } from "../../../java/lang/Math.js";
import type { Ray } from "../environment/geometry/element/Ray.js";
import type { Matrix4x4d } from "../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../environment/camera/Camera.js";
import { InfinitePlane } from "../environment/geometry/surface/InfinitePlane.js";
import { CameraController } from "./CameraController.js";
import { KeyEvent } from "./KeyEvent.js";
import type { MouseEvent } from "./MouseEvent.js";

export class CameraControllerGoogleEarth extends CameraController {
    private camera: Camera;
    private jumpStep: number;
    private xOld = 0;
    private yOld = 0;

    /**
    @param camera
    */
    public constructor(camera: Camera) {
        super();
        this.camera = camera;
        this.jumpStep = 0.00000000000000001;
    }

    /**
    @param mouseEvent
    @return true if current event leads to a model update, false if not.
    */
    public processMouseEvent(_mouseEvent: MouseEvent): boolean {
        return false; //To change body of generated methods, choose Tools | Templates.
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    public override processMousePressedEvent(e: MouseEvent): boolean {
        this.setxOld(e.getX());
        this.setyOld(e.getY());

        // Procedure for line selection (Java discards the generated ray)
        this.camera.updateVectors();
        this.camera.generateRay(this.getxOld(), this.getyOld());

        return false;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    public override processMouseReleasedEvent(_e: MouseEvent): boolean {
        return false;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    public override processMouseClickedEvent(_e: MouseEvent): boolean {
        return false;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    public override processMouseMovedEvent(_e: MouseEvent): boolean {
        return false;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    public override processMouseDraggedEvent(e: MouseEvent): boolean {
        //---------------------------------------------------------------------
        // 1. Initial and final position
        const PrevX: number = this.getxOld();
        const PrevY: number = this.getyOld();

        const x: number = e.getX();
        const y: number = e.getY();

        //---------------------------------------------------------------------
        // 2. Ray 1 and 2 calculation
        const RayA: Ray = this.camera.generateRay(PrevX, PrevY);
        const RayB: Ray = this.camera.generateRay(x, y);

        //----------------------------------------------------------------------
        // 3. Intersection with the infinite plane
        const infinitePlane = new InfinitePlane(new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 0));
        const hitA: Ray | null = infinitePlane.doIntersectionFirstHit(RayA);
        const hitB: Ray | null = infinitePlane.doIntersectionFirstHit(RayB);
        if (hitA === null || hitB === null) {
            return false;
        }

        //----------------------------------------------------------------------
        // 4. Distance between RayA and RayB
        let pA: Vector3Dd = new Vector3Dd();
        let pB: Vector3Dd = new Vector3Dd();

        pA = pA.withX(hitA.getOrigin().x() + hitA.getDirection().x() * hitA.getT());
        pA = pA.withY(hitA.getOrigin().y() + hitA.getDirection().y() * hitA.getT());
        pA = pA.withZ(hitA.getOrigin().z() + hitA.getDirection().z() * hitA.getT());

        pB = pB.withX(hitB.getOrigin().x() + hitB.getDirection().x() * hitB.getT());
        pB = pB.withY(hitB.getOrigin().y() + hitB.getDirection().y() * hitB.getT());
        pB = pB.withZ(hitB.getOrigin().z() + hitB.getDirection().z() * hitB.getT());

        const d: Vector3Dd = pB.subtract(pA);

        //----------------------------------------------------------------------
        // 5. Move the camera
        const currentPosition: Vector3Dd = this.camera.getPosition();
        this.camera.setPosition(
            new Vector3Dd(currentPosition.x() - d.x(), currentPosition.y() - d.y(), currentPosition.z()),
        );

        this.setxOld(x);
        this.setyOld(y);

        return true;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    public override processMouseWheelEvent(e: MouseEvent): boolean {
        // Local copy of the Camera's internal parameters
        let eyePosition: Vector3Dd;
        let focusedPosition: Vector3Dd;
        let R: Matrix4x4d; // Camera rotation matrix
        let projectionMode: number;
        let fov: number;
        let orthogonalZoom: number;
        let nearPlaneDistance: number;
        let farPlaneDistance: number;

        // 1. Obtain a copy of the camera's internal parameters
        eyePosition = this.camera.getPosition();
        focusedPosition = this.camera.getFocusedPosition();
        R = this.camera.getRotation();
        projectionMode = this.camera.getProjectionMode();
        fov = this.camera.getFov();
        orthogonalZoom = this.camera.getOrthogonalZoom();
        nearPlaneDistance = this.camera.getNearPlaneDistance();
        farPlaneDistance = this.camera.getFarPlaneDistance();

        const clicks: number = e.getClicks(); //();
        let updated = false;
        let altura: number = eyePosition.z();
        //------------------------------------------------------------
        if (clicks < 0) {
            // Adjust the delta so the zoom matches the image size
            const expo: number = JavaMath.round(JavaMath.log10(eyePosition.z())) - 1;
            this.jumpStep = Math.pow(10, expo); //

            // Lower limit
            if (eyePosition.z() - this.jumpStep <= 12) {
                return false;
            } // Photos 0.0000000000000000000001

            nearPlaneDistance = altura * 0.1;
            farPlaneDistance = altura * 110;

            eyePosition = eyePosition.withZ(eyePosition.z() - this.jumpStep);
            focusedPosition = focusedPosition.withZ(focusedPosition.z() - this.jumpStep);

            updated = true;
        } else if (clicks > 0) {
            // Upper limit
            if (eyePosition.z() + this.jumpStep >= Math.pow(10, 24)) {
                return false;
            } // For photos Math.pow(10, 25)

            // Adjust the delta so the zoom matches the image size
            this.jumpStep = Math.pow(10, JavaMath.round(JavaMath.log10(eyePosition.z())) - 1);

            altura = eyePosition.z();

            nearPlaneDistance = altura * 0.1;
            farPlaneDistance = altura * 110;

            eyePosition = eyePosition.withZ(eyePosition.z() + this.jumpStep);
            focusedPosition = focusedPosition.withZ(focusedPosition.z() + this.jumpStep);
            updated = true;
        }

        // 4. Update camera's internal parameters from local copy
        //      R = R.eulerAnglesRotation(yaw, pitch, roll);
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

    /**
    @return current camera being under control
    */
    public override getCamera(): Camera {
        return this.camera;
    }

    /**
    @param camera
    */
    public override setCamera(camera: Camera): void {
        this.camera = camera;
    }

    /**
    @param keyEvent
    @return true if current event leads to a model update, false if not.
    */
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
        // Java declares `expo` and `altura` inside the switch, shared by
        // the KEY_z and KEY_Z cases.
        let expo: number;
        let altura: number;

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

        if (fov > 90) {
            angleInc = JavaMath.toRadians(10);
        } else if (fov > 45) {
            angleInc = JavaMath.toRadians(5);
        } else if (fov > 15) {
            angleInc = JavaMath.toRadians(2.5);
        } else if (fov > 5) {
            angleInc = JavaMath.toRadians(1);
        } else {
            angleInc = JavaMath.toRadians(0.1);
        }

        // 3. Event processing: update the copy of the camera's internal parameters
        switch (keyEvent.keycode) {
            case KeyEvent.KEY_UP:
                pitch -= angleInc;
                if (pitch < JavaMath.toRadians(-90)) {
                    pitch = JavaMath.toRadians(-90);
                }
                updated = true;
                break;
            case KeyEvent.KEY_DOWN:
                pitch += angleInc;
                if (pitch > JavaMath.toRadians(90)) {
                    pitch = JavaMath.toRadians(90);
                }
                updated = true;
                break;
            case KeyEvent.KEY_LEFT:
                yaw += angleInc;
                while (yaw >= JavaMath.toRadians(360)) {
                    yaw -= JavaMath.toRadians(360);
                }
                updated = true;
                break;
            case KeyEvent.KEY_RIGHT:
                yaw -= angleInc;
                while (yaw < 0) {
                    yaw += JavaMath.toRadians(360);
                }
                updated = true;
                break;

            // Position
            case KeyEvent.KEY_x:
                eyePosition = eyePosition.withX(eyePosition.x() - this.jumpStep);
                focusedPosition = focusedPosition.withX(focusedPosition.x() - this.jumpStep);
                updated = true;
                break;
            case KeyEvent.KEY_X:
                eyePosition = eyePosition.withX(eyePosition.x() + this.jumpStep);
                focusedPosition = focusedPosition.withX(focusedPosition.x() + this.jumpStep);
                updated = true;
                break;
            case KeyEvent.KEY_y:
                eyePosition = eyePosition.withY(eyePosition.y() - this.jumpStep);
                focusedPosition = focusedPosition.withY(focusedPosition.y() - this.jumpStep);
                updated = true;
                break;
            case KeyEvent.KEY_Y:
                eyePosition = eyePosition.withY(eyePosition.y() + this.jumpStep); //focusedPosition.y() += deltaMov;
                updated = true;
                break;
            case KeyEvent.KEY_z:
                // Adjust the delta so the zoom matches the image size
                expo = JavaMath.round(JavaMath.log10(eyePosition.z())) - 1;
                this.jumpStep = Math.pow(10, expo); //

                altura = eyePosition.z();

                // Lower limit
                if (eyePosition.z() - this.jumpStep <= 12) {
                    break;
                } // Photos 0.0000000000000000000001

                nearPlaneDistance = altura * 0.1;
                farPlaneDistance = altura * 110;

                eyePosition = eyePosition.withZ(eyePosition.z() - this.jumpStep);
                focusedPosition = focusedPosition.withZ(focusedPosition.z() - this.jumpStep);
                updated = true;
                break;
            //---------------------------------------------------------------

            case KeyEvent.KEY_Z:
                // Upper limit
                if (eyePosition.z() + this.jumpStep >= Math.pow(10, 4)) {
                    break;
                } // For photos Math.pow(10, 25)

                // Adjust the delta so the zoom matches the image size
                this.jumpStep = Math.pow(10, JavaMath.round(JavaMath.log10(eyePosition.z())) - 1);

                altura = eyePosition.z();

                nearPlaneDistance = altura * 0.1;
                farPlaneDistance = altura * 110;

                eyePosition = eyePosition.withZ(eyePosition.z() + this.jumpStep);
                focusedPosition = focusedPosition.withZ(focusedPosition.z() + this.jumpStep);
                updated = true;
                break;
            // Rotation
            case KeyEvent.KEY_S:
                roll -= JavaMath.toRadians(5);
                while (roll < 0) {
                    roll += JavaMath.toRadians(360);
                }
                updated = true;
                break;
            case KeyEvent.KEY_s:
                roll += JavaMath.toRadians(5);
                while (roll > JavaMath.toRadians(360)) {
                    roll -= JavaMath.toRadians(360);
                }
                updated = true;
                break;

            // View volume modification
            case KeyEvent.KEY_A:
                if (this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
                    orthogonalZoom /= 2;
                } else {
                    if (fov < 0.1 - EPSILON) {
                        fov += 0.1;
                    } else if (fov < 1 - EPSILON) {
                        fov++;
                    } else if (fov < 175 - EPSILON) {
                        fov += 5;
                    }
                }
                updated = true;
                break;
            case KeyEvent.KEY_a:
                if (this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
                    orthogonalZoom *= 2;
                } else {
                    if (fov > 5 + EPSILON) {
                        fov -= 5;
                    } else if (fov > 1 + EPSILON) {
                        fov--;
                    } else if (fov > 0.1 + EPSILON) {
                        fov -= 0.1;
                    }
                }
                updated = true;
                break;

            case KeyEvent.KEY_N:
                nearPlaneDistance = nearPlaneDistance + 0.5; // augmentLogarithmic(nearPlaneDistance, EPSILON);
                updated = true;
                break;
            case KeyEvent.KEY_n:
                nearPlaneDistance = nearPlaneDistance - 0.5; //diminishLogarithmic(nearPlaneDistance, EPSILON);
                updated = true;
                break;

            case KeyEvent.KEY_F:
                farPlaneDistance = farPlaneDistance + 0.5; //augmentLogarithmic(farPlaneDistance, EPSILON);
                updated = true;
                break;
            case KeyEvent.KEY_f:
                farPlaneDistance = farPlaneDistance - 0.5; //diminishLogarithmic(farPlaneDistance, EPSILON);
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

    /**
    @param keyEvent
    @return true if current event leads to a model update, false if not.
    */
    public override processKeyReleasedEvent(_keyEvent: KeyEvent): boolean {
        return false; //To change body of generated methods, choose Tools | Templates.
    }

    /**
    @param factor
    */
    public override setDeltaMovement(_factor: number): void {}

    /**
    @return the xOld
    */
    public getxOld(): number {
        return this.xOld;
    }

    /**
    @param xOld the xOld to set
    */
    public setxOld(xOld: number): void {
        this.xOld = xOld;
    }

    /**
    @return the yOld
    */
    public getyOld(): number {
        return this.yOld;
    }

    /**
    @param yOld the yOld to set
    */
    public setyOld(yOld: number): void {
        this.yOld = yOld;
    }

    /**
    @param jumpValue
    */
    public zoomOut(jumpValue: number): void {
        let eyePosition: Vector3Dd;
        let focusedPosition: Vector3Dd;

        eyePosition = this.camera.getPosition();
        focusedPosition = this.camera.getFocusedPosition();

        const altura: number = eyePosition.z();

        const nearPlaneDistance: number = altura * 0.1;
        const farPlaneDistance: number = altura * 110;

        eyePosition = eyePosition.withZ(eyePosition.z() + jumpValue);
        focusedPosition = focusedPosition.withZ(focusedPosition.z() + jumpValue);

        this.camera.setPosition(eyePosition);
        this.camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
        this.camera.setNearPlaneDistance(nearPlaneDistance);
        this.camera.setFarPlaneDistance(farPlaneDistance);
    }

    /**
    @param jumpValue
    */
    public zoomIn(jumpValue: number): void {
        let eyePosition: Vector3Dd;
        let focusedPosition: Vector3Dd;

        eyePosition = this.camera.getPosition();
        focusedPosition = this.camera.getFocusedPosition();

        const altura: number = eyePosition.z();
        if (eyePosition.z() - jumpValue >= 12) {
            const nearPlaneDistance: number = altura * 0.1;
            const farPlaneDistance: number = altura * 110;

            eyePosition = eyePosition.withZ(eyePosition.z() - jumpValue);
            focusedPosition = focusedPosition.withZ(focusedPosition.z() - jumpValue);

            this.camera.setPosition(eyePosition);
            this.camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
            this.camera.setNearPlaneDistance(nearPlaneDistance);
            this.camera.setFarPlaneDistance(farPlaneDistance);
        }
    }
}

import { Double } from "../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { VSDK } from "../../common/VSDK.js";
import { Logger } from "../../common/logging/Logger.js";
import { Entity } from "../../common/Entity.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Vector4Dd } from "../../common/linealAlgebra/Vector4Dd.js";
import { Ray } from "../geometry/element/Ray.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Geometry } from "../geometry/Geometry.js";
import { InfinitePlane } from "../geometry/surface/InfinitePlane.js";
import { CameraSnapshot } from "./CameraSnapshot.js";

export class Camera extends Entity {
    // Global constants
    public static readonly OPCODE_FAR = 0x01 << 1;
    public static readonly OPCODE_NEAR = 0x01 << 2;
    public static readonly OPCODE_RIGHT = 0x01 << 3;
    public static readonly OPCODE_LEFT = 0x01 << 4;
    public static readonly OPCODE_UP = 0x01 << 5;
    public static readonly OPCODE_DOWN = 0x01 << 6;

    public static readonly PROJECTION_MODE_ORTHOGONAL = 4;
    public static readonly PROJECTION_MODE_PERSPECTIVE = 5;

    // Basic Camera Model
    private up: Vector3Dd;
    private front: Vector3Dd;
    private left: Vector3Dd;
    private position: Vector3Dd;
    private focalDistance: number;
    private projectionMode: number;

    /// This is vertical field of vision
    private fov: number;
    private orthogonalZoom: number;
    private nearPlaneDistance: number;
    private farPlaneDistance: number;

    /// This string should be used for specific application defined
    /// functionality. Can be null.
    private name: string | null = null;

    /**
    Una `Camera` debe saber de qu&eacute; tama&ntilde;o es el viewport para
    el cual est&aacute; generando una proyecci&oacute;n, para poder modificar
    sus par&aacute;metros internos en funci&oacute;n del &aacute;ngulo de
    vision (`fov`) y de la actual proporci&oacute;n de ancho/alto del
    viewport. Las variables internas `viewportXSize` y `viewportYSize`
    representan el tama&nacute;o en pixels para el viewport, y son valores
    que solo pueden ser cambiados por el m&eacute;todo
    `Camera::updateViewportResize`. Estos dos valores son para uso interno de
    la clase c&aacute;mara y no pueden ser consultados (i.e. son una copia
    de la configuraci&oacute;n del viewport, que debe ser administrado por
    la aplicaci&oacute;n que use `Camera`s).
    */
    private viewportXSize: number;
    /// Check `viewportXSize`'s documentation
    private viewportYSize: number;

    // Private values which are preprocessed to speed up calculations
    private frontWithScale!: Vector3Dd;
    private upWithScale!: Vector3Dd;
    private rightWithScale!: Vector3Dd;

    /**
    cpp `Camera.cpp` static helper: `degrees * PI / 180.0`. It is not
    `Math.toRadians` (`degrees * 0.017453292519943295`), which rounds some
    angles one ulp differently and shifts the primary rays.
    */
    private static degreesToRadians(degrees: number): number {
        return (degrees * Math.PI) / 180.0;
    }
    private normalizingTransformation!: Matrix4x4d;
    private modificationVersion = 0;

    public constructor();
    public constructor(b: Camera);
    public constructor(b?: Camera) {
        super();
        if (b === undefined) {
            this.position = new Vector3Dd(0, -5, 1);

            this.up = new Vector3Dd(0, 0, 1);
            this.front = new Vector3Dd(0, 1, 0);
            this.left = new Vector3Dd(-1, 0, 0);

            this.fov = 60;
            this.viewportXSize = 320;
            this.viewportYSize = 320;

            this.projectionMode = Camera.PROJECTION_MODE_PERSPECTIVE;
            this.orthogonalZoom = 1;
            this.nearPlaneDistance = 0.05;
            this.farPlaneDistance = 100;

            this.focalDistance = 10;
            this.updateVectors();
            return;
        }
        this.position = new Vector3Dd(b.position);

        this.up = new Vector3Dd(b.up);
        this.front = new Vector3Dd(b.front);
        this.left = new Vector3Dd(b.left);

        this.fov = b.fov;
        this.viewportXSize = b.viewportXSize;
        this.viewportYSize = b.viewportYSize;

        this.projectionMode = b.projectionMode;
        this.orthogonalZoom = b.orthogonalZoom;
        this.nearPlaneDistance = b.nearPlaneDistance;
        this.farPlaneDistance = b.farPlaneDistance;

        this.focalDistance = b.focalDistance;

        this.updateVectors();
    }

    public getNormalizingTransformation(): Matrix4x4d {
        return this.normalizingTransformation;
    }

    public getModificationVersion(): number {
        return this.modificationVersion;
    }

    private markModified(): void {
        this.modificationVersion++;
    }

    public getName(): string | null {
        return this.name;
    }

    public setName(n: string | null): void {
        this.name = n;
        this.markModified();
    }

    public getViewportXSize(): number {
        return this.viewportXSize;
    }

    public getViewportYSize(): number {
        return this.viewportYSize;
    }

    public getPosition(): Vector3Dd {
        return this.position;
    }

    public setPosition(position: Vector3Dd): void {
        this.position = new Vector3Dd(position);
        this.markModified();
    }

    public getFocusedPosition(): Vector3Dd {
        let partial: Vector3Dd;
        let result: Vector3Dd;
        partial = this.front.multiply(this.focalDistance);
        result = this.position.add(partial);
        return result;
    }

    /**
    This method changes the `front` unit vector and the `focalDistance` based
    on `focusedPosition` parameter and current `eyePosition` value, WITHOUT
    changing any other vector. This method does NOT change the value of `up`
    of `left` vectors and can be used in advanced applications to directly
    access basic camera parameters.
    @param focusedPosition
    */
    public setFocusedPositionDirect(focusedPosition: Vector3Dd): void {
        let partial: Vector3Dd;

        partial = focusedPosition.subtract(this.position);
        this.front = new Vector3Dd(partial);
        this.focalDistance = this.front.length();
        this.front = this.front.normalized();
        this.markModified();
    }

    /**
    This method changes the `front` unit vector, the `focalDistance`, the
    `left` vector and the `up` vector, based on:
      - `focusedPosition` parameter
      - current `eyePosition` value
      - current `up` vector (taken as a hint)
    This method CAN change the value of `up` and `left` vectors, to
    allow the user specify a left-handed orthogonal reference frame formed by
    the vectors [up, front, left]. The initial up vector is taken as a hint
    to specify a new up vector, similar to the original but forming a 90
    degree angle with the front direction. The left vector is always changed
    to form a third orthogonal vectors to former ones. Note that the three
    resulting vectors are left normalized.
    \todo
    This method FAILS if the initial value of `up` is parallel to the
    `front` direction. Validation and exception handling are needed.
    @param focusedPosition
    */
    public setFocusedPositionMaintainingOrthogonality(focusedPosition: Vector3Dd): void {
        this.front = focusedPosition.subtract(this.position);
        this.focalDistance = this.front.length();
        this.front = this.front.normalized();

        this.left = this.up.crossProduct(this.front);
        this.left = this.left.normalized();

        this.up = this.front.crossProduct(this.left);
        this.up = this.up.normalized();
        this.markModified();
    }

    public getUp(): Vector3Dd {
        return this.up;
    }

    public getUpWithScale(): Vector3Dd {
        return this.upWithScale;
    }

    public getRightWithScale(): Vector3Dd {
        return this.rightWithScale;
    }

    public getFront(): Vector3Dd {
        return this.front;
    }

    public getLeft(): Vector3Dd {
        return this.left;
    }

    /**
    This method overwrites current `up` vector value, without considering the
    ill-case of getting `up` and `front` vectors pointing in the same
    direction.

    For non-advanced programmers, it is desireable to invoke the
    `setUpMaintainingOrthogonality` method instead of this one.
    @param up
    */
    public setUpDirect(up: Vector3Dd): void {
        this.up = new Vector3Dd(up);
        this.markModified();
    }

    /**
    This method overwrites current `left` vector value, without considering the
    ill-case of getting `left` and `front` (or `up` vectors pointing in the
    same direction.

    For non-advanced programmers, it is desireable to invoke the
    `setLeftMaintainingOrthogonality` method instead of this one.
    @param left
    */
    public setLeftDirect(left: Vector3Dd): void {
        this.left = new Vector3Dd(left);
        this.markModified();
    }

    /**
    En este metodo no se tiene en cuenta si up y front quedan mirando
    para el mismo lado.
    @param up
     */
    public setUpMaintainingOrthogonality(up: Vector3Dd): void {
        up = up.normalized();

        this.left = up.crossProduct(this.front);
        this.left = this.left.normalized();

        this.up = this.front.crossProduct(this.left);
        // As in Java, the field ends up as the normalized parameter.
        this.up = up.normalized();
        this.markModified();
    }

    public getFov(): number {
        return this.fov;
    }

    public setFov(fov: number): void {
        this.fov = fov;
        this.markModified();
    }

    public getNearPlaneDistance(): number {
        return this.nearPlaneDistance;
    }

    public setNearPlaneDistance(nearPlaneDistance: number): void {
        this.nearPlaneDistance = nearPlaneDistance;
        this.markModified();
    }

    public getFarPlaneDistance(): number {
        return this.farPlaneDistance;
    }

    public setFarPlaneDistance(farPlaneDistance: number): void {
        this.farPlaneDistance = farPlaneDistance;
        this.markModified();
    }

    public getProjectionMode(): number {
        return this.projectionMode;
    }

    public setProjectionMode(projectionMode: number): void {
        this.projectionMode = projectionMode;
        this.markModified();
    }

    public updateViewportResize(dx: number, dy: number): void {
        this.viewportXSize = dx | 0;
        this.viewportYSize = dy | 0;
        this.updateVectors();
        this.markModified();
    }

    /**
    PRE:
      - focusedPosition y eyePosition tienen que ser diferentes!
    POST:
      - left queda normalizado
      - up queda normalizado
    \todo  Document the way in which vectors are calculated, acording to
    the projection transformation.
    */
    public updateVectors(): void {
        this.up = this.up.normalized();
        this.left = this.left.normalized();
        this.front = this.front.normalized();

        const fovFactor: number = this.viewportXSize / this.viewportYSize;
        this.frontWithScale = this.front.multiply(0.5);
        this.upWithScale = this.up.multiply(Math.tan(Camera.degreesToRadians(this.fov / 2)));
        this.rightWithScale = this.left.multiply(-fovFactor * Math.tan(Camera.degreesToRadians(this.fov / 2)));

        //-----------------------------------------------------------------
        /*
        The normalizing transformation of current camera is such that transforms
        points in space to make it lie in the canonical view volume space,
        and it is calculated following the mechanism described on sections
        [FOLE1992].6.5.1 and [FOLE1992].6.5.2.
        */
        this.normalizingTransformation = new Matrix4x4d();
        // 1. Translate the "VRP" point to the origin
        let VRP: Vector3Dd;
        let T1: Matrix4x4d = new Matrix4x4d();

        // Warning: near plane clipping
        VRP = this.position.add(this.front.multiply(this.nearPlaneDistance));
        T1 = T1.translation(VRP.multiply(-1));

        // 2. Rotate the "VRC" coordinate system such as the front axis
        //    become the -z axis
        let R1: Matrix4x4d;
        R1 = this.getRotation();
        R1 = R1.invert();

        let R2: Matrix4x4d = new Matrix4x4d();
        R2 = R2.eulerAnglesRotation(Camera.degreesToRadians(90), Camera.degreesToRadians(-90), 0);
        const RTOTAL: Matrix4x4d = R2.multiply(R1);

        // 3. Translate such that the center of projection is at the origin
        let T2: Matrix4x4d = new Matrix4x4d();
        T2 = T2.translation(0, 0, -this.nearPlaneDistance);

        // 4. Shear such that the center line of the view volume becomes the
        //    z axis

        // 5. Scale such that the view volume becomes the canonical perspective
        //    view volume
        let S1: Matrix4x4d = new Matrix4x4d();
        const S2: Matrix4x4d = new Matrix4x4d();
        let ddx: number, ddy: number;

        // 5.1. Non proportional scaling to adjust the slopes of the piramid
        // planes to fix 45 degrees in u and v directions
        ddx = this.rightWithScale.length();
        ddy = this.upWithScale.length();
        S1 = S1.scale(ddx, ddy, 1);
        S1 = S1.invert();

        // 5.2. Proportional scaling to adjust near / far clipping planes
        // maintaining the piramid form
        //ddz = 2.0/(farPlaneDistance - nearPlaneDistance);
        //S2 = S2.scale(1, 1, ddz);
        //S2 = S2.invert();

        // Compose final transformation
        const T3: Matrix4x4d = new Matrix4x4d();
        //T3 = T3.translation(0, 0, nearPlaneDistance);

        this.normalizingTransformation = T3.multiply(S2.multiply(S1.multiply(T2.multiply(RTOTAL.multiply(T1)))));
    }

    /**
    As cpp `Camera::exportToCameraSnapshot` (a const method): the snapshot is
    taken from a copy resized to the given viewport, and this camera is left
    unchanged in its vectors. The copy constructor and the resize both call
    `updateVectors`, so the snapshot vectors are normalized as many times as
    in cpp, which matters in the last bit of nearly unit vectors.
    Java only: this camera keeps the historic side effect of taking the
    viewport size (without renormalizing its vectors), which callers such as
    `calculateProjectionMatrix` users rely on.
    The no-argument Java overload passes `(int)viewportXSize` and
    `(int)viewportYSize`.
    */
    public exportToCameraSnapshot(): CameraSnapshot;
    public exportToCameraSnapshot(viewportXSize: number, viewportYSize: number): CameraSnapshot;
    public exportToCameraSnapshot(viewportXSize?: number, viewportYSize?: number): CameraSnapshot {
        if (viewportXSize === undefined || viewportYSize === undefined) {
            return this.exportToCameraSnapshot(Math.trunc(this.viewportXSize), Math.trunc(this.viewportYSize));
        }
        const tmp: Camera = new Camera(this);
        if (this.viewportXSize !== viewportXSize || this.viewportYSize !== viewportYSize) {
            this.viewportXSize = viewportXSize;
            this.viewportYSize = viewportYSize;
            this.markModified();
        }
        tmp.viewportXSize = viewportXSize;
        tmp.viewportYSize = viewportYSize;
        tmp.updateVectors();
        return new CameraSnapshot(
            tmp.position,
            tmp.front,
            tmp.left,
            tmp.up,
            tmp.projectionMode,
            tmp.orthogonalZoom,
            tmp.viewportXSize,
            tmp.viewportYSize,
            tmp.frontWithScale,
            tmp.upWithScale,
            tmp.rightWithScale,
            tmp.nearPlaneDistance,
            tmp.farPlaneDistance,
        );
    }

    /**
    Given a 2D integer coordinate in viewport space, this method calculates a
    proyector ray that emanates from the eye position and passes over the
    (u, v) float coordinate in the projection plane. Note that the (u, v)
    coordinate correspond to the (x, y) coordinate.

    This method is of vital importance to many fundamental algorithms of
    visualization (i.e. ray casting, ray tracing, radiosity), object selection
    and others (simulation, colision detection, visual debugging, etc.). As it
    is important to improve the efficiency of this method, some precalculated
    values are stored in the class attributes `frontWithScale`, `upWithScale` and
    `rightWithScale`, which values are stored in the `updateVectors`
    method, leading to the following precondition:

    PRE:
      - At least a call to the updateVectors method must be done before calling
        this method, and after changing any camera parameter the updateVectors
        method must be called again to reflect the changes in this calculation.

    /todo this method should be named "generateProjectorRay"
    @param x
    @param y
    @return a new ray fired from current camera eye location and passing thru
    projection surface at a point equivalent to (x, y) integer projected pixel      coordinate
    */
    public generateRay(x: number, y: number): Ray {
        // 1. Convert integer image coordinates into values in the range [-0.5, 0.5]
        const u: number = (x - this.viewportXSize / 2.0) / this.viewportXSize;
        const v: number = (this.viewportYSize - y - 1 - this.viewportYSize / 2.0) / this.viewportYSize;

        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            const fovFactor: number = this.viewportXSize / this.viewportYSize;
            const duScale: number = -fovFactor * ((2 * u) / this.orthogonalZoom);
            const dvScale: number = (2 * v) / this.orthogonalZoom;
            const origin: Vector3Dd = new Vector3Dd(
                this.position.x() + this.left.x() * duScale + this.up.x() * dvScale,
                this.position.y() + this.left.y() * duScale + this.up.y() * dvScale,
                this.position.z() + this.left.z() * duScale + this.up.z() * dvScale,
            );
            return new Ray(origin, this.front);
        }

        // 2. Default behavior is to assume planar perspective projection
        const direction: Vector3Dd = new Vector3Dd(
            this.rightWithScale.x() * u + this.upWithScale.x() * v + this.frontWithScale.x(),
            this.rightWithScale.y() * u + this.upWithScale.y() * v + this.frontWithScale.y(),
            this.rightWithScale.z() * u + this.upWithScale.z() * v + this.frontWithScale.z(),
        );

        // 3. Build up and return a ray with origin in the eye position
        return new Ray(this.position, direction);
    }

    public getOrthogonalZoom(): number {
        return this.orthogonalZoom;
    }

    public setOrthogonalZoom(orthogonalZoom: number): void {
        this.orthogonalZoom = orthogonalZoom;
        this.markModified();
    }

    public setRotation(R: Matrix4x4d): void {
        this.up = this.up.withX(R.get(0, 2));
        this.up = this.up.withY(R.get(1, 2));
        this.up = this.up.withZ(R.get(2, 2));
        this.up = this.up.normalized();

        this.front = this.front.withX(R.get(0, 0));
        this.front = this.front.withY(R.get(1, 0));
        this.front = this.front.withZ(R.get(2, 0));
        this.front = this.front.normalized();

        this.left = this.left.withX(R.get(0, 1));
        this.left = this.left.withY(R.get(1, 1));
        this.left = this.left.withZ(R.get(2, 1));
        this.left = this.left.normalized();
        this.markModified();
    }

    public getRotation(): Matrix4x4d {
        //------------------------------------------------------------
        const R: Matrix4x4d = new Matrix4x4d()
            .withVal(0, 0, this.front.x())
            .withVal(0, 1, this.left.x())
            .withVal(0, 2, this.up.x())
            .withVal(1, 0, this.front.y())
            .withVal(1, 1, this.left.y())
            .withVal(1, 2, this.up.y())
            .withVal(2, 0, this.front.z())
            .withVal(2, 1, this.left.z())
            .withVal(2, 2, this.up.z());

        return R;
    }

    /**
    Note that projectionMatrix = transformationMatrix*viewVolumeMatrix
    @return projection matrix representing view volume component
    */
    public calculateViewVolumeMatrix(): Matrix4x4d {
        //- Calculate the base projection matrix ----------------------------
        let leftDistance: number, rightDistance: number, upDistance: number, downDistance: number, aspect: number;
        let P: Matrix4x4d = new Matrix4x4d();

        aspect = this.viewportXSize / this.viewportYSize;
        switch (this.projectionMode) {
            case Camera.PROJECTION_MODE_ORTHOGONAL:
                P = P.orthogonalProjection(
                    -aspect / this.orthogonalZoom,
                    aspect / this.orthogonalZoom,
                    -1 / this.orthogonalZoom,
                    1 / this.orthogonalZoom,
                    this.nearPlaneDistance,
                    this.farPlaneDistance,
                );
                break;
            case Camera.PROJECTION_MODE_PERSPECTIVE:
                upDistance = this.nearPlaneDistance * Math.tan(Camera.degreesToRadians(this.fov / 2));
                downDistance = -upDistance;
                leftDistance = aspect * downDistance;
                rightDistance = aspect * upDistance;
                P = P.frustumProjection(
                    leftDistance,
                    rightDistance,
                    downDistance,
                    upDistance,
                    this.nearPlaneDistance,
                    this.farPlaneDistance,
                );
                break;
        }
        return P;
    }

    /**
    Note that projectionMatrix = transformationMatrix*viewVolumeMatrix.
    @return transformation component for projection matrix for current camera
    */
    public calculateTransformationMatrix(): Matrix4x4d {
        //- Take into account the camera position and orientation ----------
        let R: Matrix4x4d;
        let R1: Matrix4x4d;
        let T1: Matrix4x4d = new Matrix4x4d();
        let R_adic2: Matrix4x4d = new Matrix4x4d();
        let R_adic1: Matrix4x4d = new Matrix4x4d();

        R1 = this.getRotation();
        R1 = R1.invert();

        T1 = T1.translation(-this.position.x(), -this.position.y(), -this.position.z());
        R_adic2 = R_adic2.axisRotation(Camera.degreesToRadians(90), 0, 0, 1);
        R_adic1 = R_adic1.axisRotation(Camera.degreesToRadians(-90), 1, 0, 0);

        R = R_adic1.multiply(R_adic2.multiply(R1.multiply(T1)));

        return R;
    }

    /**
    Note that projectionMatrix = transformationMatrix*viewVolumeMatrix
    @return projection matrix for current camera, including transformation
    and view volume components
    */
    public calculateProjectionMatrix(): Matrix4x4d {
        const P: Matrix4x4d = this.calculateViewVolumeMatrix();
        const R: Matrix4x4d = this.calculateTransformationMatrix();
        return P.multiply(R);
    }

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    @return human readable representation of current camera's information
    */
    public override toString(): string {
        //------------------------------------------------------------
        let msg: string;

        msg = "<Camera>:\n";
        msg += '  - Name: "' + this.getName() + '"\n';
        if (this.projectionMode === Camera.PROJECTION_MODE_PERSPECTIVE) {
            msg = msg + "  - Camera in PERSPECTIVE projection mode\n";
        } else if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            msg = msg + "  - Camera in PARALEL projection mode\n";
            msg = msg + "  - Orthogonal zoom = " + Double.toString(this.orthogonalZoom) + "\n";
        } else {
            msg = msg + "  - UNKNOWN Camera projection mode!\n";
        }

        msg = msg + "  - eyePosition(x, y, z) = " + this.position + "\n";
        msg =
            msg +
            "  - focusedPointPosition(x, y, z) = " +
            this.position.add(this.front.multiply(this.focalDistance)) +
            "\n";

        //------------------------------------------------------------
        let R: Matrix4x4d, TP: Matrix4x4d;
        let yaw: number, pitch: number, roll: number;

        TP = this.calculateProjectionMatrix();
        R = this.getRotation();
        yaw = R.obtainEulerYawAngle();
        pitch = R.obtainEulerPitchAngle();
        roll = R.obtainEulerRollAngle();

        msg =
            msg +
            "  - Rotation yaw/pitch/roll: <" +
            VSDK.formatDouble(yaw) +
            ", " +
            VSDK.formatDouble(pitch) +
            ", " +
            VSDK.formatDouble(roll) +
            "> RAD (<" +
            VSDK.formatDouble(JavaMath.toDegrees(yaw)) +
            ", " +
            VSDK.formatDouble(JavaMath.toDegrees(pitch)) +
            ", " +
            VSDK.formatDouble(JavaMath.toDegrees(roll)) +
            "> DEG)\n";

        msg = msg + "  - Rotation quaternion: " + R.exportToQuaternion() + "\n";

        //------------------------------------------------------------
        this.updateVectors();
        msg = msg + "  - Reference frame:\n";
        msg = msg + "    . Vector UP = " + this.up + " (length " + VSDK.formatDouble(this.up.length()) + ")\n";
        msg = msg + "    . Vector FRONT = " + this.front + " (length " + VSDK.formatDouble(this.front.length()) + ")\n";
        // As in Java, the LEFT line reports the length of FRONT.
        msg = msg + "    . Vector LEFT = " + this.left + " (length " + VSDK.formatDouble(this.front.length()) + ")\n";
        msg = msg + "  - Reference frame with scales:\n";
        msg =
            msg +
            "    . Vector UP' = " +
            this.upWithScale +
            " (length " +
            VSDK.formatDouble(this.upWithScale.length()) +
            ")\n";
        msg =
            msg +
            "    . Vector FRONT' = " +
            this.frontWithScale +
            " (length " +
            VSDK.formatDouble(this.frontWithScale.length()) +
            ")\n";
        msg =
            msg +
            "    . Vector RIGHT' = " +
            this.rightWithScale +
            " (length " +
            VSDK.formatDouble(this.rightWithScale.length()) +
            ")\n";
        msg = msg + "  - fov = " + VSDK.formatDouble(this.fov) + "\n";
        msg = msg + "  - nearPlaneDistance = " + VSDK.formatDouble(this.nearPlaneDistance) + "\n";
        msg = msg + "  - farPlaneDistance = " + VSDK.formatDouble(this.farPlaneDistance) + "\n";
        msg =
            msg +
            "  - Viewport size in pixels = (" +
            VSDK.formatDouble(this.viewportXSize) +
            ", " +
            VSDK.formatDouble(this.viewportYSize) +
            ")\n";
        msg = msg + "  - Transformation * projection matrix:" + TP;

        //------------------------------------------------------------
        let P: Matrix4x4d;
        let leftDistance: number, rightDistance: number, upDistance: number, downDistance: number, aspect: number;

        P = new Matrix4x4d();
        aspect = this.viewportXSize / this.viewportYSize;
        if (this.projectionMode === Camera.PROJECTION_MODE_PERSPECTIVE) {
            upDistance = this.nearPlaneDistance * Math.tan(Camera.degreesToRadians(this.fov / 2));
            downDistance = -upDistance;
            leftDistance = aspect * downDistance;
            rightDistance = aspect * upDistance;
            P = P.frustumProjection(
                leftDistance,
                rightDistance,
                downDistance,
                upDistance,
                this.nearPlaneDistance,
                this.farPlaneDistance,
            );
            msg = msg + "  - Projection matrix:" + P;
        } else if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            P = P.orthogonalProjection(
                -aspect / this.orthogonalZoom,
                aspect / this.orthogonalZoom,
                -1 / this.orthogonalZoom,
                1 / this.orthogonalZoom,
                this.nearPlaneDistance,
                this.farPlaneDistance,
            );
            msg = msg + "  - Projection matrix:" + P;
        }

        //------------------------------------------------------------
        return msg;
    }

    /**
    Given `this` camera and the pixel (x, y) in its viewport, this method
    calculates an infinite plane that pass by the corresponding proyector
    ray origin and by the proyection plane (u, v) point, where (u, v) is
    the proyection of pixel (x, y). The plane is perpendicular to the v
    direction.
    @param x
    @param y
    @return a new plane as part of the 6 planes representing current camera's
    viewing volume
    */
    public calculateUPlaneAtPixel(x: number, _y: number): InfinitePlane {
        // 1. Calculate the angle between the front vector and the plane
        this.updateVectors();
        const u: number = (x - this.viewportXSize / 2.0) / this.viewportXSize;
        return this.calculateUPlane(u);
    }

    /**
    Returns an outward pointing "horizontal" plane with respect to current
    camera (horizontal is left-right direction). Outwards means that the
    plane normal points outward the visualization volume / frustum.

    Camera visualization limits for u are inside the interval [-0.5, 0.5].

    PRE: updateVectors() must be called before this method if camera model
    is new or recently changed.
    @param u
    @return a new plane as part of the 6 planes representing current camera's
    viewing volume
    */
    public calculateUPlane(u: number): InfinitePlane {
        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            u /= this.orthogonalZoom;
            u *= 2 * (this.viewportXSize / this.viewportYSize);
            let right: Vector3Dd = this.left.multiply(-1);
            right = right.normalized();
            if (u > 0) {
                return new InfinitePlane(right, this.position.add(right.multiply(u)));
            } else {
                return new InfinitePlane(this.left, this.position.add(right.multiply(u)));
            }
        }

        const du: Vector3Dd = this.rightWithScale.multiply(u);
        let f: Vector3Dd = new Vector3Dd(this.front);
        let dir: Vector3Dd = du.add(this.frontWithScale);

        f = f.normalized();

        let alpha: number;

        dir = dir.normalized();

        alpha = Math.acos(f.dotProduct(dir));
        if (u > 0) alpha *= -1;

        // 2. Calculate the plane normal
        let R: Matrix4x4d = new Matrix4x4d();
        let n: Vector3Dd;

        R = R.axisRotation(alpha, this.up);
        n = R.multiply(du);
        n = n.normalized();

        // 3. Build the plane and return
        let plane: InfinitePlane;

        plane = new InfinitePlane(n, this.position);

        return plane;
    }

    /**
    Given `this` camera and the pixel (x, y) in its viewport, this method
    calculates an infinite plane that pass by the corresponding proyector
    ray origin and by the projection plane (u, v) point, where (u, v) is
    the projection of pixel (x, y). The plane is perpendicular to the v
    direction.
    @param x
    @param y
    @return a new plane as part of the 6 planes representing current camera's
    viewing volume
    */
    public calculateVPlaneAtPixel(_x: number, y: number): InfinitePlane {
        // 1. Calculate the angle between the front vector and the plane
        this.updateVectors();
        const v: number = (this.viewportYSize - y - 1 - this.viewportYSize / 2.0) / this.viewportYSize;
        return this.calculateVPlane(v);
    }

    /**
    PRE: updateVectors() must be called before this method if camera model
    is new or recently changed.
    @param v
    @return a new plane as part of the 6 planes representing current camera's
    viewing volume
    */
    public calculateVPlane(v: number): InfinitePlane {
        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            v /= this.orthogonalZoom;
            v *= 2;
            // Java keeps an unused copy (`up2`) of the field before normalizing it.
            this.up = this.up.normalized();
            if (v > 0) {
                return new InfinitePlane(this.up, this.position.add(this.up.multiply(v)));
            } else {
                const down: Vector3Dd = this.up.multiply(-1);
                return new InfinitePlane(down, this.position.add(this.up.multiply(v)));
            }
        }

        const dv: Vector3Dd = this.upWithScale.multiply(v);
        let f: Vector3Dd = new Vector3Dd(this.front);
        let dir: Vector3Dd = dv.add(this.frontWithScale);

        f = f.normalized();

        let alpha: number;

        dir = dir.normalized();

        alpha = Math.acos(f.dotProduct(dir));
        if (v > 0) alpha *= -1;

        // 2. Calculate the plane normal
        let R: Matrix4x4d = new Matrix4x4d();
        let n: Vector3Dd;

        R = R.axisRotation(alpha, this.left);
        n = R.multiply(dv);
        n = n.normalized();

        // 3. Build the plane and return
        let plane: InfinitePlane;

        plane = new InfinitePlane(n, this.position);

        return plane;
    }

    /**
    PRE: updateVectors() must be called before this method if camera model
    is new or recently changed.

    WARNING: This is currently considering only the perspective case!
    TODO: The paralel projection case!
    @return a new plane as part of the 6 planes representing current camera's
    viewing volume
    */
    public calculateNearPlane(): InfinitePlane {
        let plane: InfinitePlane;

        let f: Vector3Dd = new Vector3Dd(this.front);
        f = f.normalized();
        const back: Vector3Dd = f.multiply(-1);
        f = f.multiply(this.nearPlaneDistance);
        const c: Vector3Dd = this.position.add(f);

        plane = new InfinitePlane(back, c);

        return plane;
    }

    /**
    PRE: updateVectors() must be called before this method if camera model
    is new or recently changed.

    WARNING: This is currently considering only the perspective case!
    TODO: The paralel projection case!
    @return a new plane as part of the 6 planes representing current camera's
    viewing volume
    */
    public calculateFarPlane(): InfinitePlane {
        let plane: InfinitePlane;

        let f: Vector3Dd = new Vector3Dd(this.front);
        f = f.normalized();
        f = f.multiply(this.farPlaneDistance);
        const c: Vector3Dd = this.position.add(f);
        plane = new InfinitePlane(this.front, c);

        return plane;
    }

    /**
    Java declares three private `calculateOutcodeBits` overloads. TypeScript
    has one entry point that dispatches on the argument list:
    - `(Vector4Dd)`: homogeneous clipping space;
    - `(Vector3Dd)`: canonical view volume;
    - `(Vector3Dd, right, left, up, down, far, near)`: the six camera planes.
    */
    private calculateOutcodeBits(p: Vector4Dd): number;
    private calculateOutcodeBits(p: Vector3Dd): number;
    private calculateOutcodeBits(
        p: Vector3Dd,
        right: InfinitePlane,
        left: InfinitePlane,
        up: InfinitePlane,
        down: InfinitePlane,
        far: InfinitePlane,
        near: InfinitePlane,
    ): number;
    private calculateOutcodeBits(
        p: Vector4Dd | Vector3Dd,
        right?: InfinitePlane,
        left?: InfinitePlane,
        up?: InfinitePlane,
        down?: InfinitePlane,
        far?: InfinitePlane,
        near?: InfinitePlane,
    ): number {
        if (p instanceof Vector4Dd) {
            return this.calculateOutcodeBitsHomogeneous(p);
        }
        if (right === undefined) {
            return this.calculateOutcodeBitsCanonic(p);
        }
        return this.calculateOutcodeBitsPlanes(p, right, left!, up!, down!, far!, near!);
    }

    /**
    Given a point in "clipping coordinates space", this method calculates
    a six bit opcode, as explained in [FOLE1992].6.5.3, suitable for use in the
    Cohen-Suterland line clipping algorithm, taking the input point in
    homogeneous space, as noted in [FOLE1992].6.5.4.

    Note that the "clipping coodinate space" is the result of transforming
    world coordinate space with the composed transform-project matrix for
    current camera, as returned by the `calculateProjectionMatrix` method.

    Note that in VSDK, the clipping space correspond to the frustum for
    the minmax cube from <-1, -1, -1> to <1, 1, 1>.

    WARNING: This algoritm FAILS when the point to be tested is in the
    plane passing through eye position of the camera and paralel to near
    plane! In this case, W gets 0 value, and points are not correctly
    classified.
    \todo : check this method... currently disabled due to non working cases!
    */
    private calculateOutcodeBitsHomogeneous(p: Vector4Dd): number {
        let bits = 0x00;

        if (p.w() > 0) {
            if (p.x() > p.w()) bits |= Camera.OPCODE_RIGHT;
            if (p.x() < -p.w()) bits |= Camera.OPCODE_LEFT;
            if (p.y() > p.w()) bits |= Camera.OPCODE_UP;
            if (p.y() < -p.w()) bits |= Camera.OPCODE_DOWN;
            if (p.z() > p.w()) bits |= Camera.OPCODE_FAR;
            if (p.z() < -p.w()) bits |= Camera.OPCODE_NEAR;
        } else {
            if (p.x() > -p.w()) bits |= Camera.OPCODE_RIGHT;
            if (p.x() < p.w()) bits |= Camera.OPCODE_LEFT;
            if (p.y() > -p.w()) bits |= Camera.OPCODE_UP;
            if (p.y() < p.w()) bits |= Camera.OPCODE_DOWN;
            if (p.z() > -p.w()) bits |= Camera.OPCODE_FAR;
            if (p.z() < p.w()) bits |= Camera.OPCODE_NEAR;
        }
        return bits;
    }

    private fpd(): number {
        return (this.farPlaneDistance - this.nearPlaneDistance) / this.nearPlaneDistance;
    }

    /**
    Given a point in "clipping coordinates space", this method calculates
    a six bit opcode, as explained in [FOLE1992].6.5.3, suitable for use in the
    Cohen-Suterland line clipping algorithm.

    Note that in VSDK, the clipping space correspond to the frustum for
    the minmax cube from <-1, -1, -1> to <1, 1, 1>.

    WARNING: Currently is only implementing the perspective case!
    */
    private calculateOutcodeBitsCanonic(p: Vector3Dd): number {
        let bits = 0x00;

        if (p.z() + p.x() - 1 > 0) bits |= Camera.OPCODE_RIGHT;
        if (p.z() - p.x() - 1 > 0) bits |= Camera.OPCODE_LEFT;
        if (p.z() + p.y() - 1 > 0) bits |= Camera.OPCODE_UP;
        if (p.z() - p.y() - 1 > 0) bits |= Camera.OPCODE_DOWN;
        // Warning: near plane clipping
        if (p.z() > 0) bits |= Camera.OPCODE_NEAR;
        if (p.z() < -this.fpd()) bits |= Camera.OPCODE_FAR;

        return bits;
    }

    /**
    Given a point in world space, this method calculates a six bit opcode,
    as explained in [FOLE1992].6.5.3, suitable for use in the Cohen-Suterland
    line clipping algorithm. The camera view volume should be represented
    by its six bounding planes.
    */
    private calculateOutcodeBitsPlanes(
        p: Vector3Dd,
        right: InfinitePlane,
        left: InfinitePlane,
        up: InfinitePlane,
        down: InfinitePlane,
        far: InfinitePlane,
        near: InfinitePlane,
    ): number {
        let bits = 0x00;

        if (right.doContainmentTestHalfSpace(p, VSDK.EPSILON) === Geometry.OUTSIDE) {
            bits |= Camera.OPCODE_RIGHT;
        }
        if (left.doContainmentTestHalfSpace(p, VSDK.EPSILON) === Geometry.OUTSIDE) {
            bits |= Camera.OPCODE_LEFT;
        }
        if (up.doContainmentTestHalfSpace(p, VSDK.EPSILON) === Geometry.OUTSIDE) {
            bits |= Camera.OPCODE_UP;
        }
        if (down.doContainmentTestHalfSpace(p, VSDK.EPSILON) === Geometry.OUTSIDE) {
            bits |= Camera.OPCODE_DOWN;
        }
        if (far.doContainmentTestHalfSpace(p, VSDK.EPSILON) === Geometry.OUTSIDE) {
            bits |= Camera.OPCODE_FAR;
        }
        if (near.doContainmentTestHalfSpace(p, VSDK.EPSILON) === Geometry.OUTSIDE) {
            bits |= Camera.OPCODE_NEAR;
        }

        return bits;
    }

    /**
    This method implements the Cohen-Sutherland line clipping algorithm with
    respect to the view volume defined by current camera. Recieves the two
    line endpoints and return true if any part of this line lies inside the
    view volume.  In the case the line crosses the view volume, the new
    resulting endpoints are calculated and returned.

    This algorithm structure follows the one proposed in [FOLE1992].3.12.3,
    generalizing it to the 3D case, as noted in [FOLE1992].6.5.3.

    As in Java, `clippedPoint0` and `clippedPoint1` are immutable values
    rebound locally, so the clipped endpoints are not visible to the caller.
    @param point0
    @param point1
    @param clippedPoint0
    @param clippedPoint1
    @return true if given 3D line has at least a segment inside current camera
    view volume, false if line is to be totally removed on an clipping operation
    */
    public clipLineCohenSutherlandPlanes(
        point0: Vector3Dd,
        point1: Vector3Dd,
        clippedPoint0: Vector3Dd,
        clippedPoint1: Vector3Dd,
    ): boolean {
        //- Local variables definition ------------------------------------
        let outcode0: number; // 6bit containment code for point0
        let outcode1: number; // 6bit containment code for point1
        let outcodeout: number; // Selected endpoint code for iteration
        let clippingMidPoint: Vector3Dd; // Selected endpoint clipped for iteration
        let testRay: Ray; // Ray use for general line/plane clipping
        let dirFromP0ToP1: Vector3Dd; // Temporary for testRay construction
        let rightPlane: InfinitePlane; // 6 planes defining current camera
        let leftPlane: InfinitePlane; //   view volume. Note that intersection
        let upPlane: InfinitePlane; //   tests are done against these planes
        let downPlane: InfinitePlane; //   using general case non-optimal
        let nearPlane: InfinitePlane; //   intersections! This sould be
        let farPlane: InfinitePlane; //   optimized
        let clippingPlane: InfinitePlane | null; // Selected plane for each iteration

        //- Algorithm initial state ---------------------------------------
        clippedPoint0 = clippedPoint0.withX(point0.x());
        clippedPoint0 = clippedPoint0.withY(point0.y());
        clippedPoint0 = clippedPoint0.withZ(point0.z());
        clippedPoint1 = clippedPoint1.withX(point1.x());
        clippedPoint1 = clippedPoint1.withY(point1.y());
        clippedPoint1 = clippedPoint1.withZ(point1.z());
        this.updateVectors();
        clippingMidPoint = new Vector3Dd();
        rightPlane = this.calculateUPlane(0.5);
        leftPlane = this.calculateUPlane(-0.5);
        upPlane = this.calculateVPlane(0.5);
        downPlane = this.calculateVPlane(-0.5);
        nearPlane = this.calculateNearPlane();
        farPlane = this.calculateFarPlane();
        // As in Java, the near plane is passed in the `far` slot and vice versa.
        outcode0 = this.calculateOutcodeBits(point0, rightPlane, leftPlane, upPlane, downPlane, nearPlane, farPlane);
        outcode1 = this.calculateOutcodeBits(point1, rightPlane, leftPlane, upPlane, downPlane, nearPlane, farPlane);
        dirFromP0ToP1 = point1.subtract(point0);
        dirFromP0ToP1 = dirFromP0ToP1.normalized();

        //- Main Cohen-Sutherland iteration cycle (incremental clipping) --
        let linePasses = false; // Algorithm return value
        let done = false; // Iteration exit condition
        do {
            //- Trivial cases: trivial accept and trivial reject ----------
            if (outcode0 === 0x0 && outcode1 === 0x0) {
                linePasses = true;
                done = true;
            } else if ((outcode0 & outcode1) !== 0x0) {
                linePasses = false;
                done = true;
            }
            //- Iterative cases: clipping with each of the 6 planes -------
            else {
                if (dirFromP0ToP1.length() < VSDK.EPSILON) {
                    // continue;
                    return false;
                }
                //--------------------------------------------------
                if (outcode0 !== 0) {
                    outcodeout = outcode0;
                } else {
                    outcodeout = outcode1;
                }
                testRay = new Ray(point0, dirFromP0ToP1);

                //--------------------------------------------------
                clippingPlane = null;
                if ((Camera.OPCODE_UP & outcodeout) !== 0x0) {
                    clippingPlane = upPlane;
                } else if ((Camera.OPCODE_DOWN & outcodeout) !== 0x0) {
                    clippingPlane = downPlane;
                } else if ((Camera.OPCODE_LEFT & outcodeout) !== 0x0) {
                    clippingPlane = leftPlane;
                } else if ((Camera.OPCODE_RIGHT & outcodeout) !== 0x0) {
                    clippingPlane = rightPlane;
                } else if ((Camera.OPCODE_NEAR & outcodeout) !== 0x0) {
                    // Warning: Why test with the contrary plane?
                    clippingPlane = farPlane;
                } else if ((Camera.OPCODE_FAR & outcodeout) !== 0x0) {
                    // Warning: Why test with the contrary plane?
                    clippingPlane = nearPlane;
                } else {
                    // Not possible: non implemented case!
                    Logger.reportMessage(
                        this,
                        VSDK.WARNING,
                        "clipLineCohenSutherlandPlanes",
                        "Unusal ray case, check code and data",
                    );
                }

                if (clippingPlane !== null) {
                    const hit: Ray | null = clippingPlane.doIntersectionFirstHit(testRay);
                    if (hit === null) {
                        Logger.reportMessage(
                            this,
                            VSDK.WARNING,
                            "clipLineCohenSutherlandPlanes",
                            "Unusal ray assembly, check code and data",
                        );
                    }
                    // Java dereferences `hit` here even when it is null.
                    clippingMidPoint = hit!.getOrigin().add(hit!.getDirection().multiply(hit!.getT()));
                    linePasses = true;
                }

                //--------------------------------------------------
                if (outcodeout === outcode0) {
                    clippedPoint0 = clippedPoint0.withX(clippingMidPoint.x());
                    clippedPoint0 = clippedPoint0.withY(clippingMidPoint.y());
                    clippedPoint0 = clippedPoint0.withZ(clippingMidPoint.z());
                    outcode0 = this.calculateOutcodeBits(
                        clippedPoint0,
                        rightPlane,
                        leftPlane,
                        upPlane,
                        downPlane,
                        nearPlane,
                        farPlane,
                    );
                } else {
                    clippedPoint1 = clippedPoint1.withX(clippingMidPoint.x());
                    clippedPoint1 = clippedPoint1.withY(clippingMidPoint.y());
                    clippedPoint1 = clippedPoint1.withZ(clippingMidPoint.z());
                    outcode1 = this.calculateOutcodeBits(
                        clippedPoint1,
                        rightPlane,
                        leftPlane,
                        upPlane,
                        downPlane,
                        nearPlane,
                        farPlane,
                    );
                }
            }
        } while (!done);

        return linePasses;
    }

    /**
    This method implements the Cohen-Sutherland line clipping algorithm with
    respect to the view volume defined by current camera. Recieves the two
    line endpoints and return true if any part of this line lies inside the
    view volume.  In the case the line crosses the view volume, the new
    resulting endpoints are calculated and returned.

    This algorithm structure follows the one proposed in [FOLE1992].3.12.3,
    generalizing it to the 3D case, as noted in [FOLE1992].6.5.3.

    The resulting clipped points are in the canonical volume reference, ready
    for projection.  If 3D clipped points are needed, they must be premultiplied
    by the inverse of the normalizingTransformation.

    As in Java, `clippedPoint0` and `clippedPoint1` are immutable values
    rebound locally, so the clipped endpoints are not visible to the caller.
    @param point0
    @param point1
    @param clippedPoint0
    @param clippedPoint1
    @return true if at least a segment of the given 3D line is visible from
    current camera
    */
    public clipLineCohenSutherlandCanonicVolume(
        point0: Vector3Dd,
        point1: Vector3Dd,
        clippedPoint0: Vector3Dd,
        clippedPoint1: Vector3Dd,
    ): boolean {
        //- Local variables definition ------------------------------------
        let outcode0: number; // 6bit containment code for point0
        let outcode1: number; // 6bit containment code for point1
        let outcodeout: number; // Selected endpoint code for iteration
        let clippingMidPoint: Vector3Dd; // Selected endpoint clipped for iteration
        let testRay: Ray; // Ray use for general line/plane clipping
        let dirFromP0ToP1: Vector3Dd; // Temporary for testRay construction
        let l: number; // Length of dirFromP0ToP1
        let planeId: number; // A number from 1 to 6 identifying which
        // plane intersection is being tested

        //- Algorithm initial state ---------------------------------------
        let pp0: Vector3Dd, pp1: Vector3Dd;

        pp0 = this.normalizingTransformation.multiply(point0);
        pp1 = this.normalizingTransformation.multiply(point1);

        clippedPoint0 = clippedPoint0.withX(pp0.x());
        clippedPoint0 = clippedPoint0.withY(pp0.y());
        clippedPoint0 = clippedPoint0.withZ(pp0.z());
        clippedPoint1 = clippedPoint1.withX(pp1.x());
        clippedPoint1 = clippedPoint1.withY(pp1.y());
        clippedPoint1 = clippedPoint1.withZ(pp1.z());
        this.updateVectors();
        clippingMidPoint = new Vector3Dd();
        outcode0 = this.calculateOutcodeBits(pp0);
        outcode1 = this.calculateOutcodeBits(pp1);

        //- Main Cohen-Sutherland iteration cycle (incremental clipping) --
        let linePasses = false; // Algorithm return value
        let done = false; // Iteration exit condition
        do {
            //- Trivial cases: trivial accept and trivial reject ----------
            if (outcode0 === 0x0 && outcode1 === 0x0) {
                linePasses = true;
                done = true;
            } else if ((outcode0 & outcode1) !== 0x0) {
                linePasses = false;
                done = true;
            }

            //- Iterative cases: clipping with each of the 6 planes -------
            else {
                //--------------------------------------------------
                dirFromP0ToP1 = clippedPoint1.subtract(clippedPoint0);
                l = dirFromP0ToP1.length();
                if (l < VSDK.EPSILON) {
                    // continue;
                    return false;
                }
                dirFromP0ToP1 = new Vector3Dd(dirFromP0ToP1.x() / l, dirFromP0ToP1.y() / l, dirFromP0ToP1.z() / l);

                //--------------------------------------------------
                if (outcode0 !== 0) {
                    outcodeout = outcode0;
                } else {
                    outcodeout = outcode1;
                }
                testRay = new Ray(clippedPoint0, dirFromP0ToP1);

                //--------------------------------------------------
                planeId = 0;
                if ((Camera.OPCODE_UP & outcodeout) !== 0x0) {
                    planeId = 1; // up plane;
                } else if ((Camera.OPCODE_DOWN & outcodeout) !== 0x0) {
                    planeId = 2; // down plane;
                } else if ((Camera.OPCODE_LEFT & outcodeout) !== 0x0) {
                    planeId = 3; // left plane;
                } else if ((Camera.OPCODE_RIGHT & outcodeout) !== 0x0) {
                    planeId = 4; // right plane;
                } else if ((Camera.OPCODE_NEAR & outcodeout) !== 0x0) {
                    planeId = 5; // near plane
                } else if ((Camera.OPCODE_FAR & outcodeout) !== 0x0) {
                    planeId = 6; // far plane;
                } else {
                    // Not possible: non implemented case!
                    Logger.reportMessage(
                        this,
                        VSDK.WARNING,
                        "clipLineCohenSutherlandCanonicVolume",
                        "Unusal case, check code and data",
                    );
                }

                let de: number;
                de = outcodeout === outcode1 ? -VSDK.EPSILON : VSDK.EPSILON;

                switch (planeId) {
                    case 1: // up plane
                        testRay = testRay.withT(
                            (l * (1 - clippedPoint0.z() - clippedPoint0.y())) /
                                (clippedPoint1.z() - clippedPoint0.z() + clippedPoint1.y() - clippedPoint0.y()),
                        );
                        clippingMidPoint = testRay
                            .getOrigin()
                            .add(testRay.getDirection().multiply(testRay.getT() + de));
                        linePasses = true;
                        break;
                    case 2: // down plane
                        testRay = testRay.withT(
                            (l * (clippedPoint0.y() - clippedPoint0.z() + 1)) /
                                (clippedPoint1.z() - clippedPoint0.z() - clippedPoint1.y() + clippedPoint0.y()),
                        );
                        clippingMidPoint = testRay
                            .getOrigin()
                            .add(testRay.getDirection().multiply(testRay.getT() + de));
                        linePasses = true;
                        break;
                    case 3: // left plane
                        testRay = testRay.withT(
                            (l * (clippedPoint0.x() - clippedPoint0.z() + 1)) /
                                (clippedPoint1.z() - clippedPoint0.z() - clippedPoint1.x() + clippedPoint0.x()),
                        );
                        clippingMidPoint = testRay
                            .getOrigin()
                            .add(testRay.getDirection().multiply(testRay.getT() + de));
                        linePasses = true;
                        break;
                    case 4: // right plane
                        testRay = testRay.withT(
                            (l * (1 - clippedPoint0.z() - clippedPoint0.x())) /
                                (clippedPoint1.z() - clippedPoint0.z() + clippedPoint1.x() - clippedPoint0.x()),
                        );
                        clippingMidPoint = testRay
                            .getOrigin()
                            .add(testRay.getDirection().multiply(testRay.getT() + de));
                        linePasses = true;
                        break;
                    case 5: // near plane
                        // Warning: near plane clipping
                        testRay = testRay.withT(
                            /*(1-nearPlaneDistance)*/ (-clippedPoint0.z() * l) /
                                (clippedPoint1.z() - clippedPoint0.z()),
                        );
                        clippingMidPoint = testRay
                            .getOrigin()
                            .add(testRay.getDirection().multiply(testRay.getT() + de));
                        linePasses = true;
                        break;
                    case 6: // far plane
                        testRay = testRay.withT(
                            ((-this.fpd() - clippedPoint0.z()) * l) / (clippedPoint1.z() - clippedPoint0.z()),
                        );
                        clippingMidPoint = testRay
                            .getOrigin()
                            .add(testRay.getDirection().multiply(testRay.getT() + de));
                        linePasses = true;
                        break;
                }

                //--------------------------------------------------
                if (outcodeout === outcode0) {
                    clippedPoint0 = clippedPoint0.withX(clippingMidPoint.x());
                    clippedPoint0 = clippedPoint0.withY(clippingMidPoint.y());
                    clippedPoint0 = clippedPoint0.withZ(clippingMidPoint.z());
                    outcode0 = this.calculateOutcodeBits(clippedPoint0);
                } else {
                    clippedPoint1 = clippedPoint1.withX(clippingMidPoint.x());
                    clippedPoint1 = clippedPoint1.withY(clippingMidPoint.y());
                    clippedPoint1 = clippedPoint1.withZ(clippingMidPoint.z());
                    outcode1 = this.calculateOutcodeBits(clippedPoint1);
                }
            }
        } while (!done);

        return linePasses;
    }

    /**
    @param worldPosition point to project, in world coordinates
    @return the projected point in viewport pixel coordinates (z is 0), or
    null if it lies outside the viewport
    */
    public projectPoint(worldPosition: Vector3Dd): Vector3Dd | null {
        const NT: Matrix4x4d = this.getNormalizingTransformation();
        let p: Vector3Dd = NT.multiply(worldPosition);
        p = new Vector3Dd(p.x() / -p.z(), p.y() / -p.z(), p.z() / p.z());

        if (p.x() < -1.0 || p.x() > 1.0 || p.y() < -1.0 || p.y() > 1.0) {
            return null;
        }

        return new Vector3Dd(
            ((p.x() + 1.0) / 2.0) * this.getViewportXSize(),
            this.getViewportYSize() - ((p.y() + 1.0) / 2.0) * this.getViewportYSize(),
            0.0,
        );
    }

    /**
    Given a point `inPoint` in world coordinates, this method calculates a
    pixel's  coordinate in viewport space (in the form <u, v, 0>).

    Note that the integer part of the returned `outProjected` point (in
    its x and y coordinates) can be used as an input to a putPixel type
    image operation for display, and the same coordinate as double, is
    usefull as an input for antialiased versions of line rasterizers.

    This method is fundamental for visualization operations, where some
    examples includes but are not limited to:
    - Given two (previously clipped) line endpoints in world space,
      projected positions can be used for 2D line drawing for wireframe
      and hidden line rendering algorithms (caligraphic phase prior to
      2D rasterizing).
    - For interaction techniques, 2D feedback over 2D visualization.
    - As part of complex interaction techniques involving a 3D point image
      in camera viewport, for example to calculate relative movement in 3D
      with respect to aparent current view from camera.
    - When estimating the projected area of a polygon from camera's point
      of view, for example as part of a level of detail selection criteria.

    TODO: Current implementation is suspicious. It should use a simple
    multiplication of point with projection matrix... but this idea is not
    working.
    @param inPoint point to project, in world coordinates
    @return the projected point in viewport pixel coordinates (z is 0), or
    null if the point can not be projected or lies outside the viewport
    */
    public projectPointUsingRayMethod(inPoint: Vector3Dd): Vector3Dd | null {
        // 1. Calculate vectors
        let upCopy: Vector3Dd | null = null;
        let rightCopy: Vector3Dd | null = null;
        let fovFactor: number;
        let scaleFactor: number;

        fovFactor = this.viewportXSize / this.viewportYSize;
        this.updateVectors(); // Should be made a prerequisite for efficiency!

        // 2. Calculate projection plane
        let p: Vector3Dd;
        let center: Vector3Dd;
        let viewPlane: InfinitePlane;

        center = new Vector3Dd(this.front);
        p = this.getPosition();
        center = center.normalized();
        // As in Java, the scaled vector is discarded: the view plane passes
        // through position + normalized(front).
        center.multiply(this.nearPlaneDistance);
        center = center.add(p);
        viewPlane = new InfinitePlane(this.front.multiply(-1), center);

        // 3. Calculate projected global coordinates XYZ
        let projected: Vector3Dd;

        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            projected = viewPlane.projectPoint(inPoint).subtract(center).multiply(this.orthogonalZoom);
        } else {
            // 3.1. Vector calculation for perspective case
            upCopy = new Vector3Dd(this.up);
            rightCopy = new Vector3Dd(this.left.multiply(-1));
            scaleFactor = 1.0 / Math.tan(Camera.degreesToRadians(this.fov / 2));
            upCopy = upCopy.normalized();
            upCopy = upCopy.multiply(scaleFactor);
            rightCopy = rightCopy.normalized();
            rightCopy = rightCopy.multiply(scaleFactor).multiply(1 / fovFactor);

            // 3.2. Calculate projector for perspective case
            let r: Ray;
            r = new Ray(p, inPoint.subtract(p));

            // 3.3. Project point in view plane from perspective eyepoint
            const hit: Ray | null = viewPlane.doIntersectionFirstHit(r);
            if (hit === null || r.getDirection().length() < VSDK.EPSILON) {
                return null;
            }
            projected = hit.getOrigin().add(hit.getDirection().multiply(hit.getT())).subtract(center);
            // 3.4. Clip projected point in viewport
            if (projected.x() < -1 || projected.x() > 1 || projected.y() < -1 || projected.y() > 1) {
                return null;
            }
        }

        // 4. Scale point to viewport
        let x: number;
        let y: number;

        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            x =
                this.viewportXSize / 2 +
                (projected.dotProduct(this.left.multiply(-1)) / (2 * fovFactor)) * this.viewportXSize;
            y = ((projected.dotProduct(this.up) * -1 + 1) / 2) * this.viewportYSize;
        } else {
            x = (projected.dotProduct(rightCopy!) / 2 + 0.5) * this.viewportXSize;
            y = (1 - (projected.dotProduct(upCopy!) / 2 + 0.5)) * this.viewportYSize;
        }

        return new Vector3Dd(x, y, 0.0);
    }

    /**
    Return 6 outward pointing planes bounding the view volume / frustum
    for current camera.
    PRE: updateVectors method should be called before calling this method.
    @return return a set of 6 planes, representing the current cameras view
    volume
    */
    public getBoundingPlanes(): InfinitePlane[] {
        let planes: InfinitePlane[];

        planes = new Array<InfinitePlane>(6);

        planes[0] = this.calculateUPlane(-0.5);
        planes[1] = this.calculateUPlane(0.5);
        planes[2] = this.calculateVPlane(-0.5);
        planes[3] = this.calculateVPlane(0.5);
        planes[4] = this.calculateNearPlane();
        planes[5] = this.calculateFarPlane();

        return planes;
    }

    /**
    Given a point description of a paralelogram or other convex polyhedra
    this method determines if that paralelogram is visible.
    In other words, this method determines if the paralelogram and the
    view volume intersects.
    @param cornerCoordinates
    @return if a convex polyhedra for given point set can be visible from
    current camera
    */
    public boundingConvexPolyhedraIsVisible(cornerCoordinates: Vector3Dd[]): boolean {
        const viewVolumePlanes: InfinitePlane[] = this.getBoundingPlanes();

        let i: number, j: number;
        let isOutside: boolean;
        let p: Vector3Dd;

        for (i = 0; i < 6; i++) {
            isOutside = true;
            for (j = 0; j < cornerCoordinates.length; j++) {
                p = cornerCoordinates[j]!;
                if (viewVolumePlanes[i]!.doContainmentTestHalfSpace(p, VSDK.EPSILON) === 1) {
                    isOutside = false;
                    break;
                }
            }
            if (isOutside) return false;
        }

        return true;
    }

    /**
    Java `clone(Camera other)` overloads the inherited `Entity.clone()`;
    without an argument this delegates to `Entity.clone()` as Java does.
    */
    public override clone(): unknown;
    public override clone(other: Camera): void;
    public override clone(other?: Camera): unknown {
        if (other === undefined) {
            return super.clone();
        }
        this.focalDistance = other.focalDistance;
        this.projectionMode = other.projectionMode;
        this.fov = other.fov;
        this.orthogonalZoom = other.orthogonalZoom;
        this.nearPlaneDistance = other.nearPlaneDistance;
        this.farPlaneDistance = other.farPlaneDistance;
        this.viewportXSize = other.viewportXSize;
        this.viewportYSize = other.viewportYSize;
        this.up = other.up;
        this.front = other.front;
        this.left = other.left;
        this.position = other.position;
        this.name = other.name;
        this.frontWithScale = other.frontWithScale;
        this.upWithScale = other.upWithScale;
        this.rightWithScale = other.rightWithScale;
        this.normalizingTransformation = other.normalizingTransformation;
        return undefined;
    }

    /**
    Given a point in screen coordinates (x, y, 0), for example from a user
    generated mouse click, this method calculates the corresponding 3D point
    such as when located in the unit square (from <-0.5, -0.5, 0> to
    <0.5, 0.5, 0>) the projected point is localized visually on the given
    input viewport coordinate if camera is at OpenGL compatible neutral
    configuration (at position 0, 0, 2, looking down to position 0, 0, 0 with
    up vector 0, 1, 0).

    When configuring graphics library to draw on viewport coordinates is
    recommended to not use this method, but viewport2UnitSquareTransform.

    As in Java, `inOutUnitSquarePoint` is an immutable value rebound
    locally, so the result is not visible to the caller.
    @param inViewportPoint
    @param inOutUnitSquarePoint
    */
    public convertViewportPointToUnitSquare(inViewportPoint: Vector3Dd, inOutUnitSquarePoint: Vector3Dd): void {
        const a: number = 1.0 / this.viewportXSize;
        const f: number = -1.0 / this.viewportYSize;
        const d = -0.5;
        const h = 0.5;
        inOutUnitSquarePoint = inOutUnitSquarePoint.withX(a * inViewportPoint.x() + d);
        inOutUnitSquarePoint = inOutUnitSquarePoint.withY(f * inViewportPoint.y() + h);
        // eslint-disable-next-line @typescript-eslint/no-unused-vars -- Java's dead store on an immutable value
        inOutUnitSquarePoint = inOutUnitSquarePoint.withZ(0);
    }

    /**
    Creates a transformation matrix to convert points in the unit square
    (from <-0.5, -0.5, 0> to <0.5, 0.5, 0>) viewport coordinates.
    @return
    */
    public viewport2UnitSquareTransform(): Matrix4x4d {
        let T: Matrix4x4d;
        T = new Matrix4x4d();

        const a: number = 1.0 / this.viewportXSize;
        const f: number = -1.0 / this.viewportYSize;
        const d = -0.5;
        const h = 0.5;

        T = T.withVal(0, 0, a);
        T = T.withVal(1, 1, f);
        T = T.withVal(0, 3, d);
        T = T.withVal(1, 3, h);

        return T;
    }
}

import { Entity } from "../../common/Entity.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Ray } from "../geometry/element/Ray.js";
import { InfinitePlane } from "../geometry/surface/InfinitePlane.js";
import { CameraSnapshot } from "./CameraSnapshot.js";

/** Camera model, projection math, clipping planes, and viewport ray generation. */
export class Camera extends Entity {
    public static readonly OPCODE_FAR = 0x01 << 1;
    public static readonly OPCODE_NEAR = 0x01 << 2;
    public static readonly OPCODE_RIGHT = 0x01 << 3;
    public static readonly OPCODE_LEFT = 0x01 << 4;
    public static readonly OPCODE_UP = 0x01 << 5;
    public static readonly OPCODE_DOWN = 0x01 << 6;
    public static readonly PROJECTION_MODE_ORTHOGONAL = 4;
    public static readonly PROJECTION_MODE_PERSPECTIVE = 5;

    private up: Vector3Dd;
    private front: Vector3Dd;
    private left: Vector3Dd;
    private eyePosition: Vector3Dd;
    private focalDistance: number;
    private projectionMode: number;
    private fov: number;
    private orthogonalZoom: number;
    private nearPlaneDistance: number;
    private farPlaneDistance: number;
    private name: string | null = null;
    private viewportXSize: number;
    private viewportYSize: number;
    private dir = new Vector3Dd();
    private upWithScale = new Vector3Dd();
    private rightWithScale = new Vector3Dd();
    private normalizingTransformation = new Matrix4x4d();
    private modificationVersion = 0;

    public constructor(other?: Camera) {
        super();
        if (other !== undefined) {
            this.eyePosition = new Vector3Dd(other.eyePosition);
            this.up = new Vector3Dd(other.up);
            this.front = new Vector3Dd(other.front);
            this.left = new Vector3Dd(other.left);
            this.focalDistance = other.focalDistance;
            this.projectionMode = other.projectionMode;
            this.fov = other.fov;
            this.orthogonalZoom = other.orthogonalZoom;
            this.nearPlaneDistance = other.nearPlaneDistance;
            this.farPlaneDistance = other.farPlaneDistance;
            this.viewportXSize = other.viewportXSize;
            this.viewportYSize = other.viewportYSize;
            this.name = other.name;
        } else {
            this.eyePosition = new Vector3Dd(0, -5, 1);
            this.up = new Vector3Dd(0, 0, 1);
            this.front = new Vector3Dd(0, 1, 0);
            this.left = new Vector3Dd(-1, 0, 0);
            this.focalDistance = 10;
            this.projectionMode = Camera.PROJECTION_MODE_PERSPECTIVE;
            this.fov = 60;
            this.orthogonalZoom = 1;
            this.nearPlaneDistance = 0.05;
            this.farPlaneDistance = 100;
            this.viewportXSize = 320;
            this.viewportYSize = 320;
        }
        this.updateVectors();
    }

    public getNormalizingTransformation(): Matrix4x4d {
        return this.normalizingTransformation;
    }
    public getModificationVersion(): number {
        return this.modificationVersion;
    }
    public getName(): string | null {
        return this.name;
    }
    public setName(name: string | null): void {
        this.name = name;
        this.markModified();
    }
    public getViewportXSize(): number {
        return this.viewportXSize;
    }
    public getViewportYSize(): number {
        return this.viewportYSize;
    }
    public getPosition(): Vector3Dd {
        return this.eyePosition;
    }
    public setPosition(position: Vector3Dd): void {
        this.eyePosition = new Vector3Dd(position);
        this.markModified();
    }
    public getFocusedPosition(): Vector3Dd {
        return this.eyePosition.add(this.front.multiply(this.focalDistance));
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
    public getFov(): number {
        return this.fov;
    }
    public getNearPlaneDistance(): number {
        return this.nearPlaneDistance;
    }
    public getFarPlaneDistance(): number {
        return this.farPlaneDistance;
    }
    public getProjectionMode(): number {
        return this.projectionMode;
    }
    public getOrthogonalZoom(): number {
        return this.orthogonalZoom;
    }

    public setFocusedPositionDirect(position: Vector3Dd): void {
        this.front = position.subtract(this.eyePosition);
        this.focalDistance = this.front.length();
        this.front = this.front.normalized();
        this.markModified();
    }
    public setFocusedPositionMaintainingOrthogonality(position: Vector3Dd): void {
        this.setFocusedPositionDirect(position);
        this.left = this.up.crossProduct(this.front).normalized();
        this.up = this.front.crossProduct(this.left).normalized();
    }
    public setUpDirect(up: Vector3Dd): void {
        this.up = new Vector3Dd(up);
        this.markModified();
    }
    public setLeftDirect(left: Vector3Dd): void {
        this.left = new Vector3Dd(left);
        this.markModified();
    }
    public setUpMaintainingOrthogonality(up: Vector3Dd): void {
        this.up = up.normalized();
        this.left = this.up.crossProduct(this.front).normalized();
        this.up = this.front.crossProduct(this.left).normalized();
        this.markModified();
    }
    public setFov(fov: number): void {
        this.fov = fov;
        this.markModified();
    }
    public setNearPlaneDistance(distance: number): void {
        this.nearPlaneDistance = distance;
        this.markModified();
    }
    public setFarPlaneDistance(distance: number): void {
        this.farPlaneDistance = distance;
        this.markModified();
    }
    public setProjectionMode(mode: number): void {
        this.projectionMode = mode;
        this.markModified();
    }
    public setOrthogonalZoom(zoom: number): void {
        this.orthogonalZoom = zoom;
        this.markModified();
    }

    public updateViewportResize(width: number, height: number): void {
        this.viewportXSize = width;
        this.viewportYSize = height;
        this.updateVectors();
        this.markModified();
    }
    public updateVectors(): void {
        this.up = this.up.normalized();
        this.left = this.left.normalized();
        this.front = this.front.normalized();
        const aspect = this.viewportXSize / this.viewportYSize;
        const tangent = Math.tan((this.fov * Math.PI) / 360);
        this.dir = this.front.multiply(0.5);
        this.upWithScale = this.up.multiply(tangent);
        this.rightWithScale = this.left.multiply(-aspect * tangent);
        const vrp = this.eyePosition.add(this.front.multiply(this.nearPlaneDistance));
        const translateToVrp = new Matrix4x4d().translation(vrp.multiply(-1));
        const r1 = this.getRotation().invert();
        const r2 = new Matrix4x4d()
            .axisRotation(Math.PI / 2, 0, 0, 1)
            .multiply(new Matrix4x4d().axisRotation(-Math.PI / 2, 0, -1, 0));
        const translateNear = new Matrix4x4d().translation(0, 0, -this.nearPlaneDistance);
        const scale = new Matrix4x4d().scale(this.rightWithScale.length(), this.upWithScale.length(), 1).invert();
        this.normalizingTransformation = scale.multiply(
            translateNear.multiply(r2.multiply(r1.multiply(translateToVrp))),
        );
    }
    public exportToCameraSnapshot(width = this.viewportXSize, height = this.viewportYSize): CameraSnapshot {
        this.updateViewportResize(width, height);
        return new CameraSnapshot(
            this.eyePosition,
            this.front,
            this.left,
            this.up,
            this.projectionMode,
            this.orthogonalZoom,
            width,
            height,
            this.dir,
            this.upWithScale,
            this.rightWithScale,
        );
    }
    public generateRay(x: number, y: number): Ray {
        const u = (x - this.viewportXSize / 2) / this.viewportXSize;
        const v = (this.viewportYSize - y - 1 - this.viewportYSize / 2) / this.viewportYSize;
        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            const scaleX = (-(this.viewportXSize / this.viewportYSize) * 2 * u) / this.orthogonalZoom;
            const scaleY = (2 * v) / this.orthogonalZoom;
            return new Ray(this.eyePosition.add(this.left.multiply(scaleX)).add(this.up.multiply(scaleY)), this.front);
        }
        return new Ray(
            this.eyePosition,
            this.rightWithScale.multiply(u).add(this.upWithScale.multiply(v)).add(this.dir),
        );
    }
    public setRotation(rotation: Matrix4x4d): void {
        this.front = new Vector3Dd(rotation.get(0, 0), rotation.get(1, 0), rotation.get(2, 0)).normalized();
        this.left = new Vector3Dd(rotation.get(0, 1), rotation.get(1, 1), rotation.get(2, 1)).normalized();
        this.up = new Vector3Dd(rotation.get(0, 2), rotation.get(1, 2), rotation.get(2, 2)).normalized();
        this.markModified();
    }
    public getRotation(): Matrix4x4d {
        return new Matrix4x4d([
            [this.front.x(), this.left.x(), this.up.x(), 0],
            [this.front.y(), this.left.y(), this.up.y(), 0],
            [this.front.z(), this.left.z(), this.up.z(), 0],
            [0, 0, 0, 1],
        ]);
    }
    public calculateViewVolumeMatrix(): Matrix4x4d {
        const aspect = this.viewportXSize / this.viewportYSize;
        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            const left = -aspect / this.orthogonalZoom,
                right = -left,
                bottom = -1 / this.orthogonalZoom,
                top = -bottom;
            return new Matrix4x4d([
                [2 / (right - left), 0, 0, -(right + left) / (right - left)],
                [0, 2 / (top - bottom), 0, -(top + bottom) / (top - bottom)],
                [
                    0,
                    0,
                    -2 / (this.farPlaneDistance - this.nearPlaneDistance),
                    -(this.farPlaneDistance + this.nearPlaneDistance) /
                        (this.farPlaneDistance - this.nearPlaneDistance),
                ],
                [0, 0, 0, 1],
            ]);
        }
        const top = this.nearPlaneDistance * Math.tan((this.fov * Math.PI) / 360),
            bottom = -top,
            right = aspect * top,
            left = -right;
        return new Matrix4x4d([
            [(2 * this.nearPlaneDistance) / (right - left), 0, (right + left) / (right - left), 0],
            [0, (2 * this.nearPlaneDistance) / (top - bottom), (top + bottom) / (top - bottom), 0],
            [
                0,
                0,
                -(this.farPlaneDistance + this.nearPlaneDistance) / (this.farPlaneDistance - this.nearPlaneDistance),
                -(2 * this.farPlaneDistance * this.nearPlaneDistance) /
                    (this.farPlaneDistance - this.nearPlaneDistance),
            ],
            [0, 0, -1, 0],
        ]);
    }
    public calculateTransformationMatrix(): Matrix4x4d {
        const rotation = this.getRotation().invert();
        const translation = new Matrix4x4d().translation(this.eyePosition.multiply(-1));
        const z90 = new Matrix4x4d().axisRotation(Math.PI / 2, 0, 0, 1);
        const xMinus90 = new Matrix4x4d().axisRotation(-Math.PI / 2, 1, 0, 0);
        return xMinus90.multiply(z90.multiply(rotation.multiply(translation)));
    }
    public calculateProjectionMatrix(): Matrix4x4d {
        return this.calculateViewVolumeMatrix().multiply(this.calculateTransformationMatrix());
    }
    public calculateUPlaneAtPixel(x: number, _y: number): InfinitePlane {
        this.updateVectors();
        return this.calculateUPlane((x - this.viewportXSize / 2) / this.viewportXSize);
    }
    public calculateUPlane(u: number): InfinitePlane {
        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            const right = this.left.multiply(-1).normalized();
            const scale = (u * 2 * this.viewportXSize) / this.viewportYSize / this.orthogonalZoom;
            return new InfinitePlane(scale > 0 ? right : this.left, this.eyePosition.add(right.multiply(scale)));
        }
        const du = this.rightWithScale.multiply(u),
            angle = Math.acos(this.front.normalized().dotProduct(du.add(this.dir).normalized())) * (u > 0 ? -1 : 1);
        return new InfinitePlane(
            new Matrix4x4d().axisRotation(angle, this.up).multiply(du).normalized(),
            this.eyePosition,
        );
    }
    public calculateVPlaneAtPixel(_x: number, y: number): InfinitePlane {
        this.updateVectors();
        return this.calculateVPlane((this.viewportYSize - y - 1 - this.viewportYSize / 2) / this.viewportYSize);
    }
    public calculateVPlane(v: number): InfinitePlane {
        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            const scale = (v * 2) / this.orthogonalZoom;
            return new InfinitePlane(
                scale > 0 ? this.up : this.up.multiply(-1),
                this.eyePosition.add(this.up.multiply(scale)),
            );
        }
        const dv = this.upWithScale.multiply(v),
            angle = Math.acos(this.front.normalized().dotProduct(dv.add(this.dir).normalized())) * (v > 0 ? -1 : 1);
        return new InfinitePlane(
            new Matrix4x4d().axisRotation(angle, this.left).multiply(dv).normalized(),
            this.eyePosition,
        );
    }
    public calculateNearPlane(): InfinitePlane {
        return new InfinitePlane(
            this.front.normalized().multiply(-1),
            this.eyePosition.add(this.front.normalized().multiply(this.nearPlaneDistance)),
        );
    }
    public calculateFarPlane(): InfinitePlane {
        return new InfinitePlane(
            this.front.normalized(),
            this.eyePosition.add(this.front.normalized().multiply(this.farPlaneDistance)),
        );
    }
    public getBoundingPlanes(): InfinitePlane[] {
        return [
            this.calculateUPlane(-0.5),
            this.calculateUPlane(0.5),
            this.calculateVPlane(-0.5),
            this.calculateVPlane(0.5),
            this.calculateNearPlane(),
            this.calculateFarPlane(),
        ];
    }
    public boundingConvexPolyhedraIsVisible(corners: Vector3Dd[]): boolean {
        return this.getBoundingPlanes().every((plane) =>
            corners.some((point) => plane.doContainmentTestHalfSpace(point, 1e-8) !== InfinitePlane.OUTSIDE),
        );
    }
    /**
     * Java-compatible clipping predicate.  Java's immutable-looking output
     * parameters are not observable in TypeScript; use the Result variant to
     * obtain the clipped endpoints.
     */
    public clipLineCohenSutherlandPlanes(
        point0: Vector3Dd,
        point1: Vector3Dd,
        _clippedPoint0 = new Vector3Dd(),
        _clippedPoint1 = new Vector3Dd(),
    ): boolean {
        return this.clipLineCohenSutherlandPlanesResult(point0, point1) !== null;
    }
    public clipLineCohenSutherlandPlanesResult(point0: Vector3Dd, point1: Vector3Dd): [Vector3Dd, Vector3Dd] | null {
        let a = point0;
        let b = point1;
        for (const plane of this.getBoundingPlanes()) {
            const da = plane.pointDistance(a);
            const db = plane.pointDistance(b);
            if (da > 0 && db > 0) return null;
            if (da > 0 === db > 0) continue;
            const t = da / (da - db);
            const intersection = a.add(b.subtract(a).multiply(t));
            if (da > 0) a = intersection;
            else b = intersection;
        }
        return [a, b];
    }
    public clipLineCohenSutherlandCanonicVolume(
        point0: Vector3Dd,
        point1: Vector3Dd,
        _clippedPoint0 = new Vector3Dd(),
        _clippedPoint1 = new Vector3Dd(),
    ): boolean {
        return this.clipLineCohenSutherlandCanonicVolumeResult(point0, point1) !== null;
    }
    public clipLineCohenSutherlandCanonicVolumeResult(
        point0: Vector3Dd,
        point1: Vector3Dd,
    ): [Vector3Dd, Vector3Dd] | null {
        let a = this.normalizingTransformation.multiply(point0);
        let b = this.normalizingTransformation.multiply(point1);
        const far = (this.farPlaneDistance - this.nearPlaneDistance) / this.nearPlaneDistance;
        const constraints: ((point: Vector3Dd) => number)[] = [
            (point) => point.z() + point.y() - 1,
            (point) => point.z() - point.y() - 1,
            (point) => point.z() - point.x() - 1,
            (point) => point.z() + point.x() - 1,
            (point) => point.z(),
            (point) => -point.z() - far,
        ];
        for (const constraint of constraints) {
            const da = constraint(a);
            const db = constraint(b);
            if (da > 0 && db > 0) return null;
            if (da > 0 === db > 0) continue;
            const hit = a.add(b.subtract(a).multiply(da / (da - db)));
            if (da > 0) a = hit;
            else b = hit;
        }
        return [a, b];
    }
    public projectPoint(worldPosition: Vector3Dd, _projectedPosition = new Vector3Dd()): boolean {
        return this.projectPointResult(worldPosition) !== null;
    }
    public projectPointResult(worldPosition: Vector3Dd): Vector3Dd | null {
        const p = this.normalizingTransformation.multiply(worldPosition);
        if (Math.abs(p.z()) < Number.EPSILON) return null;
        const x = p.x() / -p.z();
        const y = p.y() / -p.z();
        if (x < -1 || x > 1 || y < -1 || y > 1) return null;
        return new Vector3Dd(
            ((x + 1) / 2) * this.viewportXSize,
            this.viewportYSize - ((y + 1) / 2) * this.viewportYSize,
            0,
        );
    }
    public projectPointUsingRayMethod(inPoint: Vector3Dd, _outProjected = new Vector3Dd()): boolean {
        return this.projectPointUsingRayMethodResult(inPoint) !== null;
    }
    public projectPointUsingRayMethodResult(inPoint: Vector3Dd): Vector3Dd | null {
        this.updateVectors();
        const center = this.eyePosition.add(this.front.normalized().multiply(this.nearPlaneDistance));
        const viewPlane = new InfinitePlane(this.front.multiply(-1), center);
        let projected: Vector3Dd;
        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            projected = viewPlane.projectPoint(inPoint).subtract(center).multiply(this.orthogonalZoom);
            const aspect = this.viewportXSize / this.viewportYSize;
            return new Vector3Dd(
                this.viewportXSize / 2 +
                    (projected.dotProduct(this.left.multiply(-1)) / (2 * aspect)) * this.viewportXSize,
                ((-projected.dotProduct(this.up) + 1) / 2) * this.viewportYSize,
                0,
            );
        }
        const ray = new Ray(this.eyePosition, inPoint.subtract(this.eyePosition));
        const hit = viewPlane.intersectRay(ray);
        if (hit === null || ray.getDirection().length() === 0) return null;
        projected = hit.getOrigin().add(hit.getDirection().multiply(hit.getT())).subtract(center);
        const aspect = this.viewportXSize / this.viewportYSize;
        const scale = 1 / Math.tan((this.fov * Math.PI) / 360);
        const right = this.left
            .multiply(-1)
            .normalized()
            .multiply(scale / aspect);
        const up = this.up.normalized().multiply(scale);
        const x = projected.dotProduct(right);
        const y = projected.dotProduct(up);
        if (x < -1 || x > 1 || y < -1 || y > 1) return null;
        return new Vector3Dd((x / 2 + 0.5) * this.viewportXSize, (1 - (y / 2 + 0.5)) * this.viewportYSize, 0);
    }
    public override clone(other?: Camera): Camera | void {
        if (other === undefined) return new Camera(this);
        this.eyePosition = new Vector3Dd(other.eyePosition);
        this.up = new Vector3Dd(other.up);
        this.front = new Vector3Dd(other.front);
        this.left = new Vector3Dd(other.left);
        this.focalDistance = other.focalDistance;
        this.projectionMode = other.projectionMode;
        this.fov = other.fov;
        this.orthogonalZoom = other.orthogonalZoom;
        this.nearPlaneDistance = other.nearPlaneDistance;
        this.farPlaneDistance = other.farPlaneDistance;
        this.viewportXSize = other.viewportXSize;
        this.viewportYSize = other.viewportYSize;
        this.name = other.name;
        this.updateVectors();
        this.markModified();
    }
    public convertViewportPointToUnitSquare(point: Vector3Dd): Vector3Dd {
        return new Vector3Dd(point.x() / this.viewportXSize - 0.5, 0.5 - point.y() / this.viewportYSize, 0);
    }
    public viewport2UnitSquareTransform(): Matrix4x4d {
        return new Matrix4x4d([
            [1 / this.viewportXSize, 0, 0, -0.5],
            [0, -1 / this.viewportYSize, 0, 0.5],
            [0, 0, 1, 0],
            [0, 0, 0, 1],
        ]);
    }
    public override toString(): string {
        const mode =
            this.projectionMode === Camera.PROJECTION_MODE_PERSPECTIVE
                ? "PERSPECTIVE"
                : this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL
                  ? "PARALEL"
                  : "UNKNOWN";
        return `<Camera>:\n  - Name: \"${this.name}\"\n  - Camera in ${mode} projection mode\n  - eyePosition(x, y, z) = ${this.eyePosition}\n  - focusedPointPosition(x, y, z) = ${this.getFocusedPosition()}\n  - fov = ${this.fov}\n  - nearPlaneDistance = ${this.nearPlaneDistance}\n  - farPlaneDistance = ${this.farPlaneDistance}\n  - Viewport size in pixels = (${this.viewportXSize}, ${this.viewportYSize})`;
    }
    private markModified(): void {
        this.modificationVersion++;
    }
}

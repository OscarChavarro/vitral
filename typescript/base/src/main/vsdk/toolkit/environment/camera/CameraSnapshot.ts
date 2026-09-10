import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";

/** Immutable camera state exported for consistent rendering of one frame. */
export class CameraSnapshot {
    private readonly eyePosition: Vector3Dd;
    private readonly front: Vector3Dd;
    private readonly left: Vector3Dd;
    private readonly up: Vector3Dd;
    private readonly dir: Vector3Dd;
    private readonly upWithScale: Vector3Dd;
    private readonly rightWithScale: Vector3Dd;

    public constructor(
        eyePosition: Vector3Dd,
        front: Vector3Dd,
        left: Vector3Dd,
        up: Vector3Dd,
        private readonly projectionMode: number,
        private readonly orthogonalZoom: number,
        private readonly viewportXSize: number,
        private readonly viewportYSize: number,
        dir: Vector3Dd,
        upWithScale: Vector3Dd,
        rightWithScale: Vector3Dd,
    ) {
        this.eyePosition = new Vector3Dd(eyePosition);
        this.front = new Vector3Dd(front);
        this.left = new Vector3Dd(left);
        this.up = new Vector3Dd(up);
        this.dir = new Vector3Dd(dir);
        this.upWithScale = new Vector3Dd(upWithScale);
        this.rightWithScale = new Vector3Dd(rightWithScale);
    }

    public getEyePosition(): Vector3Dd {
        return this.eyePosition;
    }
    public getFront(): Vector3Dd {
        return this.front;
    }
    public getLeft(): Vector3Dd {
        return this.left;
    }
    public getUp(): Vector3Dd {
        return this.up;
    }
    public getProjectionMode(): number {
        return this.projectionMode;
    }
    public getOrthogonalZoom(): number {
        return this.orthogonalZoom;
    }
    public getViewportXSize(): number {
        return this.viewportXSize;
    }
    public getViewportYSize(): number {
        return this.viewportYSize;
    }
    public getDir(): Vector3Dd {
        return this.dir;
    }
    public getUpWithScale(): Vector3Dd {
        return this.upWithScale;
    }
    public getRightWithScale(): Vector3Dd {
        return this.rightWithScale;
    }
}

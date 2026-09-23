import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import type { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import type { CameraSnapshot } from "../../environment/camera/CameraSnapshot.js";
import { DepthBufferMode } from "./DepthBufferMode.js";

/**
Converts the distances of the primary rays of a raytracer into the values of
a depth buffer (see `DepthBufferMode`). The OpenGL conversion follows the
same projection matrices used by `Camera.calculateViewVolumeMatrix`
(`Matrix4x4d.frustumProjection` and `Matrix4x4d.orthogonalProjection`, as
`glFrustum` and `glOrtho`), so a depth encoded here compares correctly, with
the usual `LESS` / `LEQUAL` depth functions, against the fragments of
geometry rasterized with that camera.

An instance is immutable.

TypeScript counterpart of Java's `vsdk.toolkit.render.raytracing.DepthBufferEncoder`.
*/
export class DepthBufferEncoder {
    private readonly projectionMode: number;
    private readonly nearPlaneDistance: number;
    private readonly farPlaneDistance: number;
    private readonly eyePosition: Vector3Dd;
    private readonly front: Vector3Dd;

    /**
    @param mode kind of depth buffer to produce
    @param camera camera that generates the primary rays
    @param depthRangeNear near value of `depthRange` (0 by default)
    @param depthRangeFar far value of `depthRange` (1 by default)
    */
    public constructor(
        private readonly mode: DepthBufferMode,
        camera: CameraSnapshot,
        private readonly depthRangeNear: number = 0.0,
        private readonly depthRangeFar: number = 1.0,
    ) {
        if (mode === DepthBufferMode.OPENGL_DEPTH && !(camera.getNearPlaneDistance() < camera.getFarPlaneDistance())) {
            throw new IllegalArgumentException(
                "OPENGL_DEPTH needs a camera with near plane distance < far plane distance",
            );
        }
        this.projectionMode = camera.getProjectionMode();
        this.nearPlaneDistance = camera.getNearPlaneDistance();
        this.farPlaneDistance = camera.getFarPlaneDistance();
        this.eyePosition = camera.getEyePosition();
        this.front = camera.getFront();
    }

    /** @return kind of depth buffer produced */
    public getMode(): DepthBufferMode {
        return this.mode;
    }

    /**
    Encodes the depth of a primary ray.
    @param origin origin of the primary ray
    @param unitDirection unit length direction of the primary ray
    @param distance distance from the origin to the nearest hit, or
    `Infinity` if nothing was hit
    @return depth value for the buffer (rounded to 32 bit float)
    */
    public encode(origin: Vector3Dd, unitDirection: Vector3Dd, distance: number): number {
        if (this.mode !== DepthBufferMode.OPENGL_DEPTH) {
            return Math.fround(distance);
        }
        if (!Number.isFinite(distance)) {
            return Math.fround(this.depthRangeFar);
        }
        const eyeDepth: number =
            (origin.x() - this.eyePosition.x()) * this.front.x() +
            (origin.y() - this.eyePosition.y()) * this.front.y() +
            (origin.z() - this.eyePosition.z()) * this.front.z() +
            distance * unitDirection.dotProduct(this.front);
        return Math.fround(this.eyeDepthToWindowDepth(eyeDepth));
    }

    /**
    Converts a depth along the viewing direction of the camera (positive in
    front of the eye) into an OpenGL window depth, clamped to the depth range.
    @param eyeDepth distance from the eye to the point, measured along the
    front vector of the camera
    @return window space depth
    */
    public eyeDepthToWindowDepth(eyeDepth: number): number {
        const n: number = this.nearPlaneDistance;
        const f: number = this.farPlaneDistance;
        let ndcZ: number;

        if (this.projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
            // Row 3 of glOrtho applied to eye space z = -eyeDepth, w = 1
            ndcZ = (2.0 * eyeDepth - (f + n)) / (f - n);
        } else {
            // Row 3 of glFrustum over w = eyeDepth
            if (eyeDepth <= 0.0) {
                return this.depthRangeNear;
            }
            ndcZ = (f + n) / (f - n) - (2.0 * f * n) / ((f - n) * eyeDepth);
        }
        if (ndcZ < -1.0) {
            ndcZ = -1.0;
        }
        if (ndcZ > 1.0) {
            ndcZ = 1.0;
        }
        return this.depthRangeNear + ((this.depthRangeFar - this.depthRangeNear) * (ndcZ + 1.0)) / 2.0;
    }
}

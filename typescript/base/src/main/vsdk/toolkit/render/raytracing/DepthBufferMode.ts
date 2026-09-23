/**
Kinds of depth buffer a raytracer can export together with its color image
(see `ParallelRaytracer.setDepthBufferMode` and `DepthBufferEncoder`).

TypeScript counterpart of Java's `vsdk.toolkit.render.raytracing.DepthBufferMode`.
Members are string constants so that a mode survives a structured-clone hop
to a worker.
*/
export enum DepthBufferMode {
    /** No depth buffer is exported */
    NONE = "NONE",

    /**
    Native distances (identity transformation): the world space distance
    from the origin of the primary ray to its nearest hit. Pixels where the
    primary ray hits nothing get `Infinity`.
    */
    RAY_DISTANCE = "RAY_DISTANCE",

    /**
    Window space depth values, as the ones OpenGL / WebGL store in the depth
    buffer for the same camera: the eye space depth is transformed by the
    projection matrix of the camera (`Camera.calculateViewVolumeMatrix`, built
    from its near and far plane distances), divided by w, and mapped by
    `depthRange` (by default [0, 1]). Values are clamped to that range, and
    pixels where the primary ray hits nothing get the far value of the range,
    as a cleared depth buffer. This buffer can be loaded in the depth buffer
    to mix the raytraced image with rasterized geometry.
    */
    OPENGL_DEPTH = "OPENGL_DEPTH",
}

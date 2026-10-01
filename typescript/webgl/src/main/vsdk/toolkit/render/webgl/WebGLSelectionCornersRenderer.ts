import { Matrix4x4d, SelectionCorners, type Camera, type ColorRgb, type Geometry, type Vector3Dd } from "@vitral/base";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4SelectionCornersRenderer`.

Draws the "selection corners" mark around an object (see
{@link SelectionCorners}, which holds its geometry). `WebGLMeshRenderer` draws
it when the rendering configuration asks for it
(`RendererConfiguration.isSelectionCornersSet()`), so every geometry drawn
through it supports the mark; it can also be drawn directly for anything with
a bounding box.
*/
export class WebGLSelectionCornersRenderer {
    private constructor() {}

    /**
    Draws the mark around the bounding box of a geometry.
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        geometry: Geometry | null,
        camera: Camera | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        if (geometry === null) {
            return;
        }
        await WebGLSelectionCornersRenderer.drawMinMax(gl, geometry.getMinMax(), camera, localTransform);
    }

    /**
    Draws the mark around a bounding box.

    @param minmax bounding box as given by `Geometry.getMinMax()`
    @param camera camera that views the box
    @param localTransform transformation from box space to world space; null
    for the identity
    */
    public static async drawMinMax(
        gl: WebGL2RenderingContext,
        minmax: ArrayLike<number> | null,
        camera: Camera | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        if (camera === null) {
            return;
        }
        const segments: Vector3Dd[] = SelectionCorners.buildSegments(minmax);

        if (segments.length === 0) {
            return;
        }
        const c: ColorRgb = SelectionCorners.getDefaultColor();
        const positions = new Float32Array(segments.length * 3);
        const colors = new Float32Array(segments.length * 3);

        for (let i = 0; i < segments.length; i++) {
            positions[3 * i] = segments[i]!.x();
            positions[3 * i + 1] = segments[i]!.y();
            positions[3 * i + 2] = segments[i]!.z();
            colors[3 * i] = c.r();
            colors[3 * i + 1] = c.g();
            colors[3 * i + 2] = c.b();
        }
        const local: Matrix4x4d = localTransform !== null ? localTransform : Matrix4x4d.identityMatrix();

        await WebGLLineRenderer.drawLines(gl, camera.calculateProjectionMatrix().multiply(local), positions, colors, 1.0);
    }
}

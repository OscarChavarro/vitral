import { Camera, Matrix4x4d, type Geometry } from "@vitral/base";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4MinMaxRenderer`.

The twelve axis-aligned edges of a min/max box, their vertex order and their
yellow color are the Java ones, and they go out through the same line renderer.
Drawing is asynchronous here for the reason recorded across this package: a
browser reaches a GLSL source over `fetch`.
*/
export class WebGLMinMaxRenderer {
    private static readonly YELLOW_RGB: readonly number[] = [1.0, 1.0, 0.0];

    private constructor() {}

    public static async draw(
        gl: WebGL2RenderingContext,
        geometry: Geometry | null,
        camera: Camera | null,
        modelViewLocal: Matrix4x4d = Matrix4x4d.identityMatrix(),
    ): Promise<void> {
        if (geometry === null) {
            return;
        }
        await WebGLMinMaxRenderer.drawMinMax(gl, geometry.getMinMax(), camera, modelViewLocal);
    }

    public static async drawMinMax(
        gl: WebGL2RenderingContext,
        minmax: Float64Array | number[] | null,
        camera: Camera | null,
        modelViewLocal: Matrix4x4d = Matrix4x4d.identityMatrix(),
    ): Promise<void> {
        if (minmax === null || minmax.length < 6 || camera === null) {
            return;
        }

        const positions: Float32Array = WebGLMinMaxRenderer.buildMinMaxLinePositions(minmax);
        const colors: Float32Array = WebGLMinMaxRenderer.buildUniformColors(
            positions.length / 3,
            WebGLMinMaxRenderer.YELLOW_RGB,
        );

        const local: Matrix4x4d = modelViewLocal;
        const mvp: Matrix4x4d = camera.calculateProjectionMatrix().multiply(local);

        await WebGLLineRenderer.drawLines(gl, mvp, positions, colors, 1.0);
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        WebGLLineRenderer.release(gl);
    }

    private static buildUniformColors(vertexCount: number, rgb: readonly number[]): Float32Array {
        const colors = new Float32Array(vertexCount * 3);
        for (let i = 0; i < vertexCount; i++) {
            const base: number = i * 3;
            colors[base] = rgb[0]!;
            colors[base + 1] = rgb[1]!;
            colors[base + 2] = rgb[2]!;
        }
        return colors;
    }

    private static buildMinMaxLinePositions(mm: Float64Array | number[]): Float32Array {
        const x0: number = mm[0]!;
        const y0: number = mm[1]!;
        const z0: number = mm[2]!;
        const x1: number = mm[3]!;
        const y1: number = mm[4]!;
        const z1: number = mm[5]!;

        // 12 axis-aligned edges, each expressed as 2 vertices (GL_LINES)
        return new Float32Array([
            x0,
            y0,
            z0,
            x1,
            y0,
            z0,
            x0,
            y0,
            z1,
            x1,
            y0,
            z1,
            x0,
            y1,
            z0,
            x1,
            y1,
            z0,
            x0,
            y1,
            z1,
            x1,
            y1,
            z1,

            x0,
            y0,
            z0,
            x0,
            y1,
            z0,
            x1,
            y0,
            z0,
            x1,
            y1,
            z0,
            x0,
            y0,
            z1,
            x0,
            y1,
            z1,
            x1,
            y0,
            z1,
            x1,
            y1,
            z1,

            x0,
            y0,
            z0,
            x0,
            y0,
            z1,
            x1,
            y0,
            z0,
            x1,
            y0,
            z1,
            x0,
            y1,
            z0,
            x0,
            y1,
            z1,
            x1,
            y1,
            z0,
            x1,
            y1,
            z1,
        ]);
    }
}

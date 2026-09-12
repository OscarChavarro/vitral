import { Matrix4x4d } from "@vitral/base";
import { WebGLCameraRenderer } from "./WebGLCameraRenderer.js";

export class WebGLMatrixRenderer {
    public static activate(matrix: Matrix4x4d): Float32Array {
        return matrix.exportToFloatArrayColumnOrder();
    }

    public static draw(gl: WebGL2RenderingContext, matrix: Matrix4x4d): void;
    public static draw(gl: WebGL2RenderingContext, modelViewProjection: Matrix4x4d, matrix: Matrix4x4d): void;
    public static draw(
        gl: WebGL2RenderingContext,
        modelViewProjectionOrMatrix: Matrix4x4d,
        maybeMatrix?: Matrix4x4d,
    ): void {
        const modelViewProjection =
            maybeMatrix === undefined ? Matrix4x4d.identityMatrix() : modelViewProjectionOrMatrix;
        const matrix = maybeMatrix ?? modelViewProjectionOrMatrix;

        const ox = matrix.get(0, 3);
        const oy = matrix.get(1, 3);
        const oz = matrix.get(2, 3);

        const positions = new Float32Array([
            ox,
            oy,
            oz,
            ox + matrix.get(0, 0),
            oy + matrix.get(1, 0),
            oz + matrix.get(2, 0),
            ox,
            oy,
            oz,
            ox + matrix.get(0, 1),
            oy + matrix.get(1, 1),
            oz + matrix.get(2, 1),
            ox,
            oy,
            oz,
            ox + matrix.get(0, 2),
            oy + matrix.get(1, 2),
            oz + matrix.get(2, 2),
        ]);

        const colors = new Float32Array([1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1]);

        WebGLCameraRenderer.drawLines(gl, modelViewProjection, positions, colors, 1.0);
    }

    public static release(gl: WebGL2RenderingContext): void {
        WebGLCameraRenderer.dispose(gl);
    }
}

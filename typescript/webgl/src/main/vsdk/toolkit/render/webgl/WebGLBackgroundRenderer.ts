import { Matrix4x4d, SimpleBackground, Vector3Dd, type Background, type ColorRgb } from "@vitral/base";
import { WebGLColoredPrimitiveRenderer } from "./WebGLColoredPrimitiveRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4BackgroundRenderer`.

Draws the background of a scene with the WebGL pipeline. Only the
`SimpleBackground` (a solid color) is supported for now.
*/
export class WebGLBackgroundRenderer {
    private constructor() {}

    /**
    Fills the area of the current viewport with the background, without
    touching the depth buffer.

    @param gl WebGL context
    @param background background to draw
    */
    public static async draw(gl: WebGL2RenderingContext, background: Background | null): Promise<void> {
        if (!(background instanceof SimpleBackground)) {
            return;
        }
        const c: ColorRgb = background.colorInDireccion(new Vector3Dd(1, 0, 0));
        const positions: Float32Array = new Float32Array([
            -1, -1, 0,  1, -1, 0,  -1, 1, 0,  1, 1, 0,
        ]);
        const colors: Float32Array = new Float32Array(16);

        for (let i: number = 0; i < 4; i++) {
            colors[4 * i] = c.r();
            colors[4 * i + 1] = c.g();
            colors[4 * i + 2] = c.b();
            colors[4 * i + 3] = 1.0;
        }
        gl.disable(gl.DEPTH_TEST);
        gl.disable(gl.BLEND);
        gl.disable(gl.CULL_FACE);
        await WebGLColoredPrimitiveRenderer.draw(gl, Matrix4x4d.identityMatrix(),
            gl.TRIANGLE_STRIP, positions, colors);
        gl.enable(gl.DEPTH_TEST);
    }
}

import {
    GizmoVertexArrayBuilder,
    RotateGizmo,
    type Camera,
    type ColorRgb,
    type Matrix4x4d,
    type Vector3Dd,
} from "@vitral/base";
import { WebGLColoredPrimitiveRenderer } from "./WebGLColoredPrimitiveRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.gizmo.Jogl4RotateGizmoRenderer`.

Renders a {@link RotateGizmo} with the WebGL pipeline (see
{@link WebGLColoredPrimitiveRenderer}): each of its three axis rings is one or
more strips of quads facing the camera, generated in world space by the gizmo
(see {@link RotateGizmo#buildRingStrips(int)}) with the width in pixels of the
ring, and drawn with the color the gizmo gives it (the one of its axis, or
yellow if the ring is under the cursor or was chosen). Only the half of each
ring closer to the camera is drawn, unless it is seen almost face on, when it
is drawn whole.

A fourth ring, gray (or yellow if under the cursor or chosen), always whole,
is drawn around the front vector of the camera (see
{@link RotateGizmo#buildCameraRingStrip()}).

While a ring is being dragged, the arc the rotation has swept is drawn over
them as a translucent sector with the color of the axis of the ring (never
yellow, see {@link RotateGizmo#buildArcFan()}).

Usage (once per frame, after the transformation matrix of the gizmo has been
set):
<pre>
    await WebGLRotateGizmoRenderer.draw(gl, gizmo, camera);
</pre>
*/
export class WebGLRotateGizmoRenderer {
    private static readonly ARC_OPACITY: number = Math.fround(0.35);

    private constructor() {}

    /**
    Draws the gizmo over the current contents of the surface.

    @param gl WebGL context
    @param gizmo gizmo to draw; its transformation matrix must be set
    @param camera camera that views the gizmo
    */
    public static async draw(
        gl: WebGL2RenderingContext | null,
        gizmo: RotateGizmo | null,
        camera: Camera | null,
    ): Promise<void> {
        if (gl === null || gizmo === null || camera === null) {
            return;
        }
        const mvp: Matrix4x4d = camera.calculateProjectionMatrix();

        gl.disable(gl.CULL_FACE);
        gl.enable(gl.DEPTH_TEST);
        gl.depthMask(true);

        for (let ring: number = 0; ring < RotateGizmo.RING_COUNT; ring++) {
            const color: ColorRgb = gizmo.getRingColor(ring);

            for (const strip of gizmo.buildRingStrips(ring)) {
                await WebGLRotateGizmoRenderer.drawRing(gl, mvp, strip, color);
            }
        }
        await WebGLRotateGizmoRenderer.drawRing(gl, mvp, gizmo.buildCameraRingStrip(), gizmo.getCameraRingColor());

        //- Translucent arc, over the rings -------------------------------
        const fan: Vector3Dd[] = gizmo.buildArcFan();

        if (fan.length >= 3) {
            gl.enable(gl.BLEND);
            gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
            gl.depthMask(false);
            await WebGLRotateGizmoRenderer.drawArc(gl, mvp, fan, gizmo.getArcColor());
            gl.depthMask(true);
            gl.disable(gl.BLEND);
        }
    }

    private static async drawArc(gl: WebGL2RenderingContext, mvp: Matrix4x4d, fan: Vector3Dd[],
                                 c: ColorRgb): Promise<void> {
        const positions: Float32Array = GizmoVertexArrayBuilder.buildPositions(fan);
        const colors: Float32Array = GizmoVertexArrayBuilder.buildRgbaColors(fan.length, c,
            WebGLRotateGizmoRenderer.ARC_OPACITY);

        await WebGLColoredPrimitiveRenderer.draw(gl, mvp, gl.TRIANGLE_FAN, positions, colors);
    }

    private static async drawRing(gl: WebGL2RenderingContext, mvp: Matrix4x4d, strip: Vector3Dd[],
                                  c: ColorRgb): Promise<void> {
        const positions: Float32Array = GizmoVertexArrayBuilder.buildPositions(strip);
        const colors: Float32Array = GizmoVertexArrayBuilder.buildRgbaColors(strip.length, c, 1.0);

        await WebGLColoredPrimitiveRenderer.draw(gl, mvp, gl.TRIANGLE_STRIP, positions, colors);
    }
}

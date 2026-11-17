import { CameraControlGizmo, Matrix4x4d, RendererConfiguration, type Camera, type ColorRgb } from "@vitral/base";
import { WebGLGeometryRenderer } from "./WebGLGeometryRenderer.js";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";

/**
Draws the text of the label of a face of the cube (Java's nested interface
`Jogl4CameraControlGizmoRenderer.LabelPainter`).
*/
export interface WebGLCameraControlGizmoLabelPainter {
    /**
    @param gl WebGL context
    @param label text of the label
    @param x horizontal pixel of the viewport (from its left) of the
    center of the label
    @param y vertical pixel of the viewport (from its top) of the center
    of the label
    */
    drawLabel(gl: WebGL2RenderingContext, label: string, x: number, y: number): Promise<void>;
}

/**
Port of `vsdk.toolkit.render.jogl.gizmo.Jogl4CameraControlGizmoRenderer`.

Renders a {@link CameraControlGizmo} (a ViewCube) as a HUD over its area of
a viewport, with the WebGL pipeline.

The parts of the gizmo (26 boxes and the compass tube) are drawn with
`WebGLGeometryRenderer`, lit from the HUD camera and seen through it, in a
WebGL viewport restricted to the area of the gizmo, with its own depth buffer
(the depth of that area is cleared). Their edges are drawn as lines from
their boundary representations (see `CameraControlGizmo.getEdgeSegments`).
An inactive gizmo is translucent: the fragments are blended with a constant
alpha (the opacity of the gizmo), as the mesh shaders of the pipeline do not
handle the opacity of materials. Labels depend on the way images are created
and cached by the application, so they are delegated to a label painter.

All the WebGL state changed is restored. Drawing is asynchronous, as every
WebGL renderer of the toolkit is.
*/
export class WebGLCameraControlGizmoRenderer {
    private static readonly EDGE_DEPTH_BIAS: number = -0.0005;
    private static readonly MIN_LABEL_VISIBILITY: number = 0.3;

    private constructor() {}

    /**
    @param gl WebGL context
    @param gizmo gizmo to draw, already placed in the viewport (see
    `CameraControlGizmo.placeInViewport`) and with its HUD camera updated
    @param viewportStartX horizontal position, in pixels of the surface, of
    the lower left corner of the viewport
    @param viewportStartY vertical position, in pixels of the surface (from
    the bottom), of the lower left corner of the viewport
    @param viewportSizeY height of the viewport, in pixels
    @param lineWidth width of the edges, in pixels
    @param labelPainter object that draws the labels of the faces; can be
    null
    */
    public static async draw(
        gl: WebGL2RenderingContext | null,
        gizmo: CameraControlGizmo | null,
        viewportStartX: number,
        viewportStartY: number,
        viewportSizeY: number,
        lineWidth: number,
        labelPainter: WebGLCameraControlGizmoLabelPainter | null,
    ): Promise<void> {
        if (gl === null || gizmo === null || !gizmo.isVisible()) {
            return;
        }

        //-----------------------------------------------------------------
        const size: number = gizmo.getSizeInPixels();
        const x: number = viewportStartX + gizmo.getHudX();
        const y: number = viewportStartY + viewportSizeY - gizmo.getHudY() - size;
        const scissorWasEnabled: boolean = gl.isEnabled(gl.SCISSOR_TEST);
        const blendWasEnabled: boolean = gl.isEnabled(gl.BLEND);
        const previousViewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;
        const previousScissor: Int32Array = gl.getParameter(gl.SCISSOR_BOX) as Int32Array;

        // Own depth for the area of the gizmo
        gl.enable(gl.SCISSOR_TEST);
        gl.scissor(x, y, size, size);
        gl.clear(gl.DEPTH_BUFFER_BIT);
        gl.viewport(x, y, size, size);
        gl.enable(gl.DEPTH_TEST);
        gl.disable(gl.CULL_FACE);

        const opacity: number = gizmo.getOpacity();
        if (opacity < 1.0) {
            gl.enable(gl.BLEND);
            gl.blendColor(0.0, 0.0, 0.0, opacity);
            gl.blendFunc(gl.CONSTANT_ALPHA, gl.ONE_MINUS_CONSTANT_ALPHA);
        }

        //-----------------------------------------------------------------
        const hud: Camera = gizmo.getHudCamera();
        const quality: RendererConfiguration = new RendererConfiguration();
        quality.setSurfaces(true);
        quality.setWires(false);
        quality.setTexture(false);
        quality.setBumpMap(false);
        quality.setNormals(false);
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_GOURAUD);

        const projection: Matrix4x4d = hud.calculateProjectionMatrix();
        const lineColor: ColorRgb = gizmo.getLineColor();

        // All the surfaces first, so their depth hides the edges behind them
        for (let p: number = 0; p < CameraControlGizmo.NUMBER_OF_PARTS; p++) {
            const part = gizmo.getParts().get(p);
            const local: Matrix4x4d = new Matrix4x4d().translation(part.getPosition());

            // No lights: the pipeline uses a light at the camera
            await WebGLGeometryRenderer.draw(
                gl,
                part.getGeometry(),
                hud,
                null,
                part.getMaterial(),
                quality,
                null,
                null,
                local,
            );
        }
        for (let p: number = 0; p < CameraControlGizmo.NUMBER_OF_PARTS; p++) {
            const positions: Float32Array = gizmo.getEdgeSegments(p);
            const colors: Float32Array = new Float32Array(positions.length);
            for (let i: number = 0; i < colors.length; i += 3) {
                colors[i] = lineColor.r();
                colors[i + 1] = lineColor.g();
                colors[i + 2] = lineColor.b();
            }
            await WebGLLineRenderer.drawLines(
                gl,
                projection,
                positions,
                colors,
                lineWidth,
                WebGLCameraControlGizmoRenderer.EDGE_DEPTH_BIAS,
            );
        }

        //-----------------------------------------------------------------
        gl.viewport(previousViewport[0]!, previousViewport[1]!, previousViewport[2]!, previousViewport[3]!);
        gl.scissor(previousScissor[0]!, previousScissor[1]!, previousScissor[2]!, previousScissor[3]!);
        if (!scissorWasEnabled) {
            gl.disable(gl.SCISSOR_TEST);
        }
        if (opacity < 1.0) {
            gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
            if (!blendWasEnabled) {
                gl.disable(gl.BLEND);
            }
        }

        if (labelPainter !== null) {
            for (const face of gizmo.getFaceRegions()) {
                if (gizmo.faceVisibility(face) < WebGLCameraControlGizmoRenderer.MIN_LABEL_VISIBILITY) {
                    continue;
                }
                const c: number[] = gizmo.projectToViewport(gizmo.getFaceLabelPosition(face));
                await labelPainter.drawLabel(gl, gizmo.getFaceLabel(face)!, c[0]!, c[1]!);
            }
        }
        gl.enable(gl.DEPTH_TEST);
    }
}

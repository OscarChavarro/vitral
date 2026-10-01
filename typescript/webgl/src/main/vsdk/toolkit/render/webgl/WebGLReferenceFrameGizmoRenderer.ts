import {
    GizmoVertexArrayBuilder,
    Matrix4x4d,
    ReferenceFrameGizmo,
    type ColorRgb,
    type Vector3Dd,
} from "@vitral/base";
import { WebGLColoredPrimitiveRenderer } from "./WebGLColoredPrimitiveRenderer.js";

/**
Draws the text of the label of one axis of the gizmo (Java's nested interface
`Jogl4ReferenceFrameGizmoRenderer.LabelPainter`).
*/
export interface WebGLReferenceFrameGizmoLabelPainter {
    /**
    Draws the label of an axis, at the projection of the label position of
    the axis (its end) over the surface.

    @param gl WebGL context
    @param axis one of `ReferenceFrameGizmo.AXIS_X`,
    `ReferenceFrameGizmo.AXIS_Y` or `ReferenceFrameGizmo.AXIS_Z`
    @param label text of the label, as given by
    `ReferenceFrameGizmo.getAxisLabel(axis)`
    @param windowX horizontal position, in pixels of the surface, of the
    label anchor
    @param windowY vertical position, in pixels of the surface, of the
    label anchor (from the bottom)
    */
    drawLabel(gl: WebGL2RenderingContext, axis: number, label: string, windowX: number, windowY: number): Promise<void>;
}

/**
Port of `vsdk.toolkit.render.jogl.gizmo.Jogl4ReferenceFrameGizmoRenderer`.

Renders a {@link ReferenceFrameGizmo} over the lower left corner of a
viewport, with the WebGL pipeline.

The axes are drawn as colored triangle strips, so they can have the width
given by {@link ReferenceFrameGizmo#getLineWidth()}, oriented according to the
rotation of the camera (see
{@link ReferenceFrameGizmo#estimateOrientation(Matrix4x4d)}).
Text of the axis labels depends on the way images are created and cached by
the application, so it is delegated to a label painter.

Usage (once per frame, after drawing the viewport contents):
<pre>
    await WebGLReferenceFrameGizmoRenderer.draw(gl, gizmo, camera.getRotation(),
        viewportStartX, viewportStartY, labelPainter);
</pre>
*/
export class WebGLReferenceFrameGizmoRenderer {
    private constructor() {}

    /**
    Draws the gizmo in a square area of the surface, with its lower left
    corner at the given position. All the WebGL state changed is restored.

    @param gl WebGL context
    @param gizmo gizmo to draw
    @param cameraRotation rotation of the camera that views the gizmo, as
    given by `Camera.getRotation()`
    @param pixelStartX horizontal position, in pixels of the surface, of the
    lower left corner of the area of the gizmo
    @param pixelStartY vertical position, in pixels of the surface, of the
    lower left corner of the area of the gizmo
    @param labelPainter object that draws the axis labels; can be null to
    draw just the axes
    */
    public static async draw(
        gl: WebGL2RenderingContext | null,
        gizmo: ReferenceFrameGizmo | null,
        cameraRotation: Matrix4x4d | null,
        pixelStartX: number,
        pixelStartY: number,
        labelPainter: WebGLReferenceFrameGizmoLabelPainter | null,
    ): Promise<void> {
        if (gl === null || gizmo === null || cameraRotation === null || !gizmo.isVisible()) {
            return;
        }

        //-----------------------------------------------------------------
        const size: number = gizmo.getSizeInPixels();
        const orientation: Matrix4x4d = gizmo.estimateOrientation(cameraRotation);
        const previousViewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;

        gl.viewport(pixelStartX, pixelStartY, size, size);
        gl.disable(gl.DEPTH_TEST);
        gl.disable(gl.CULL_FACE);

        //-----------------------------------------------------------------
        if (labelPainter !== null) {
            for (let axis: number = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++) {
                const p: Vector3Dd = orientation.multiply(gizmo.getLabelPosition(axis));

                // Canonical space [-1, 1] covers the area of the gizmo
                const windowX: number = pixelStartX + (p.x() + 1) * size / 2;
                const windowY: number = pixelStartY + (p.y() + 1) * size / 2;

                await labelPainter.drawLabel(gl, axis, gizmo.getAxisLabel(axis), windowX, windowY);
            }
        }

        // The strips are already in the canonical space of the gizmo
        const identity: Matrix4x4d = Matrix4x4d.identityMatrix();
        for (let axis: number = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++) {
            const c: ColorRgb = gizmo.getAxisColor(axis);
            const strip: Vector3Dd[] = gizmo.buildAxisStrip(axis, cameraRotation);
            const positions: Float32Array = GizmoVertexArrayBuilder.buildPositions(strip);
            const colors: Float32Array = GizmoVertexArrayBuilder.buildRgbaColors(strip.length, c, 1.0);

            await WebGLColoredPrimitiveRenderer.draw(gl, identity, gl.TRIANGLE_STRIP,
                positions, colors);
        }

        //-----------------------------------------------------------------
        gl.viewport(previousViewport[0]!, previousViewport[1]!, previousViewport[2]!,
            previousViewport[3]!);
        gl.enable(gl.DEPTH_TEST);
    }
}

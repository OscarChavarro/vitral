import {
    Box,
    Cone,
    GizmoSolidTessellator,
    GizmoVertexArrayBuilder,
    ScaleGizmo,
    type Camera,
    type ColorRgb,
    type ContourSegment,
    type Geometry,
    type Matrix4x4d,
    type SimpleBody,
    type SimpleMaterial,
    type Vector3Dd,
} from "@vitral/base";
import { WebGLColoredPrimitiveRenderer } from "./WebGLColoredPrimitiveRenderer.js";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.gizmo.Jogl4ScaleGizmoRenderer`.

Renders a {@link ScaleGizmo} with the WebGL pipeline, following the
technique `WebGLTranslateGizmoRenderer` uses for `TranslateGizmo`:

- The three cylinders of its axes (see {@link ScaleGizmo#getElements()}) and
  the small cube at the tip of each one, as solid, colored triangles. The
  cylinders and boxes are tessellated in world space by
  {@link GizmoSolidTessellator}.
- The flat handles (the trapezoidal band of each two-axis group and the three
  triangles of the uniform one) only as their contour (see
  {@link ScaleGizmo#buildContourSegments()}), drawn as lines.
- The interior of the handle currently selected, as a translucent gray
  surface over everything else (see {@link ScaleGizmo#buildBandQuad(int)},
  {@link ScaleGizmo#buildUniformTriangles()}).

Every element is generated in world space and drawn with the projection
matrix of the camera that views the gizmo.

Usage (once per frame, after the transformation matrix and scale of the gizmo
have been set):
<pre>
    await WebGLScaleGizmoRenderer.draw(gl, gizmo, camera);
</pre>
*/
export class WebGLScaleGizmoRenderer {
    /// Opacity of the interior of the flat handle currently selected
    private static readonly HANDLE_OPACITY: number = Math.fround(0.55);
    /// Depth bias (towards the viewer) of the contour of the flat handles
    private static readonly CONTOUR_DEPTH_BIAS: number = Math.fround(-0.0002);

    private constructor() {}

    /**
    Draws the gizmo over the current contents of the surface.

    @param gl WebGL context
    @param gizmo gizmo to draw; its transformation matrix must be set
    @param camera camera that views the gizmo
    */
    public static async draw(
        gl: WebGL2RenderingContext | null,
        gizmo: ScaleGizmo | null,
        camera: Camera | null,
    ): Promise<void> {
        if (gl === null || gizmo === null || camera === null) {
            return;
        }
        const mvp: Matrix4x4d = camera.calculateProjectionMatrix();

        gl.disable(gl.CULL_FACE);
        gl.enable(gl.DEPTH_TEST);
        gl.depthMask(true);

        for (const element of gizmo.getElements()) {
            const g: Geometry | null = element.getGeometry();

            if (g instanceof Cone) {
                await WebGLScaleGizmoRenderer.drawShaft(gl, mvp, element, g);
            }
            else if (g instanceof Box) {
                await WebGLScaleGizmoRenderer.drawBox(gl, mvp, element, g);
            }
        }

        //- Contour of the flat handles -------------------------------------
        await WebGLScaleGizmoRenderer.drawContour(gl, mvp, gizmo);

        //- Interior of the handle selected, translucent over everything -----
        gl.enable(gl.BLEND);
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
        gl.depthMask(false);
        for (const group of ScaleGizmo.BAND_GROUPS) {
            if (gizmo.isBandHighlighted(group)) {
                const quad: Vector3Dd[] = gizmo.buildBandQuad(group);

                await WebGLScaleGizmoRenderer.drawStrip(gl, mvp, quad, ScaleGizmo.HANDLE_FILL_COLOR,
                    WebGLScaleGizmoRenderer.HANDLE_OPACITY, gl.TRIANGLE_STRIP);
            }
        }
        if (gizmo.isUniformHighlighted()) {
            await WebGLScaleGizmoRenderer.drawUniformHandle(gl, mvp, gizmo);
        }
        gl.depthMask(true);
        gl.disable(gl.BLEND);
    }

    /**
    Draws the outline of the two-axis bands and of the uniform handle (see
    `ScaleGizmo.buildContourSegments`), each half edge with the color the
    gizmo gives it.
    */
    private static async drawContour(gl: WebGL2RenderingContext, mvp: Matrix4x4d, gizmo: ScaleGizmo): Promise<void> {
        const segments: ContourSegment[] = gizmo.buildContourSegments();

        if (segments.length === 0) {
            return;
        }

        const positions: Float32Array = new Float32Array(segments.length * 2 * 3);
        const colors: Float32Array = new Float32Array(segments.length * 2 * 3);
        let vertex: number = 0;

        for (const segment of segments) {
            GizmoVertexArrayBuilder.putVertex(positions, vertex, segment.start());
            GizmoVertexArrayBuilder.putRgb(colors, vertex, segment.color());
            vertex++;
            GizmoVertexArrayBuilder.putVertex(positions, vertex, segment.end());
            GizmoVertexArrayBuilder.putRgb(colors, vertex, segment.color());
            vertex++;
        }
        // Slightly towards the viewer, so the translucent interior of a
        // selected handle does not z-fight with its own outline
        await WebGLLineRenderer.drawLines(gl, mvp, positions, colors,
            Math.fround(gizmo.getLineWidth()), WebGLScaleGizmoRenderer.CONTOUR_DEPTH_BIAS);
    }

    /**
    Draws the lateral surface of a shaft: a cylinder (equal base and top
    radius) from the local origin of `element`, growing along its local +Z.
    */
    private static async drawShaft(gl: WebGL2RenderingContext, mvp: Matrix4x4d, element: SimpleBody,
                                   cone: Cone): Promise<void> {
        const material: SimpleMaterial | null = element.getMaterial();

        if (material === null) {
            return;
        }
        const c: ColorRgb = material.getDiffuse();
        const local: Matrix4x4d = GizmoSolidTessellator.localTransform(element);
        const strip: Vector3Dd[] = GizmoSolidTessellator.buildShaftStrip(
            local, cone.getBottomRadius(), cone.getTopRadius(), cone.getHeight());

        await WebGLScaleGizmoRenderer.drawStrip(gl, mvp, strip, c, 1.0, gl.TRIANGLE_STRIP);
    }

    /**
    Draws a box as 6 solid quads, centered at the local origin of `element`.
    */
    private static async drawBox(gl: WebGL2RenderingContext, mvp: Matrix4x4d, element: SimpleBody,
                                 box: Box): Promise<void> {
        const material: SimpleMaterial | null = element.getMaterial();

        if (material === null) {
            return;
        }
        const c: ColorRgb = material.getDiffuse();
        const local: Matrix4x4d = GizmoSolidTessellator.localTransform(element);
        const faces: Vector3Dd[][] = GizmoSolidTessellator.buildBoxFaceStrips(local, box.getSize());

        for (const face of faces) {
            await WebGLScaleGizmoRenderer.drawStrip(gl, mvp, face, c, 1.0, gl.TRIANGLE_STRIP);
        }
    }

    private static async drawStrip(gl: WebGL2RenderingContext, mvp: Matrix4x4d, points: Vector3Dd[], c: ColorRgb,
                                   alpha: number, primitiveType: number): Promise<void> {
        const positions: Float32Array = GizmoVertexArrayBuilder.buildPositions(points);
        const colors: Float32Array = GizmoVertexArrayBuilder.buildRgbaColors(points.length, c, alpha);

        await WebGLColoredPrimitiveRenderer.draw(gl, mvp, primitiveType, positions, colors);
    }

    /**
    Fills the three triangles of the uniform handle (see
    `ScaleGizmo.buildUniformTriangles`), one in each coordinate plane of the
    frame of the gizmo.
    */
    private static async drawUniformHandle(gl: WebGL2RenderingContext, mvp: Matrix4x4d,
                                           gizmo: ScaleGizmo): Promise<void> {
        const triangles: Vector3Dd[] = gizmo.buildUniformTriangles();

        await WebGLScaleGizmoRenderer.drawStrip(gl, mvp, triangles, ScaleGizmo.HANDLE_FILL_COLOR,
            WebGLScaleGizmoRenderer.HANDLE_OPACITY, gl.TRIANGLES);
    }
}

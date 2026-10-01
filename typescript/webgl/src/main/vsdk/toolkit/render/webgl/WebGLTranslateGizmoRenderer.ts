import {
    Box,
    ColorRgb,
    Cone,
    GizmoSolidTessellator,
    GizmoVertexArrayBuilder,
    type Camera,
    type Geometry,
    type Matrix4x4d,
    type SimpleBody,
    type SimpleMaterial,
    type TranslateGizmo,
    type Vector3Dd,
} from "@vitral/base";
import { WebGLColoredPrimitiveRenderer } from "./WebGLColoredPrimitiveRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.gizmo.Jogl4TranslateGizmoRenderer`.

Renders a {@link TranslateGizmo} with the WebGL pipeline (see
{@link WebGLColoredPrimitiveRenderer}).

- The lines of the gizmo (axis shafts and plane handle segments) are drawn as
  colored triangle strips facing the camera, so they can have the width given
  by {@link TranslateGizmo#getLineWidth()}.
- The heads of the axes are drawn as cones, tessellated in world space by
  {@link GizmoSolidTessellator}, with a darker base.
- The plane handle selected is drawn as a translucent quad.

Every element is generated in world space and drawn with the projection
matrix of the camera that views the gizmo.

Usage (once per frame, after the transformation matrix of the gizmo has been
set):
<pre>
    await WebGLTranslateGizmoRenderer.draw(gl, gizmo, camera);
</pre>
*/
export class WebGLTranslateGizmoRenderer {
    private static readonly CONE_BASE_SHADE: number = 0.5;

    private constructor() {}

    /**
    Draws the gizmo over the current contents of the surface. The blending and
    depth mask states changed are restored.

    @param gl WebGL context
    @param gizmo gizmo to draw; its transformation matrix must be set
    @param camera camera that views the gizmo
    */
    public static async draw(
        gl: WebGL2RenderingContext | null,
        gizmo: TranslateGizmo | null,
        camera: Camera | null,
    ): Promise<void> {
        if (gl === null || gizmo === null || camera === null) {
            return;
        }

        const elements: SimpleBody[] = gizmo.getElements3dsmax();
        const mvp: Matrix4x4d = camera.calculateProjectionMatrix();

        gl.disable(gl.CULL_FACE);
        gl.enable(gl.DEPTH_TEST);
        gl.depthMask(true);

        //- Opaque elements -----------------------------------------------
        await WebGLTranslateGizmoRenderer.drawLines(gl, gizmo, mvp);
        for (const element of elements) {
            const g: Geometry | null = element.getGeometry();

            if (g instanceof Cone) {
                await WebGLTranslateGizmoRenderer.drawCone(gl, mvp, element, g);
            }
        }

        //- Translucent elements, over the opaque ones --------------------
        gl.enable(gl.BLEND);
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
        gl.depthMask(false);
        for (const element of elements) {
            const g: Geometry | null = element.getGeometry();

            if (g instanceof Box) {
                await WebGLTranslateGizmoRenderer.drawPlaneHandle(gl, mvp, element, g);
            }
        }

        //-----------------------------------------------------------------
        gl.depthMask(true);
        gl.disable(gl.BLEND);
    }

    private static async drawLines(gl: WebGL2RenderingContext, gizmo: TranslateGizmo, mvp: Matrix4x4d): Promise<void> {
        for (const segment of gizmo.getLineSegments()) {
            const strip: Vector3Dd[] | null = gizmo.buildLineStrip(segment);

            if (strip === null) {
                continue;
            }
            await WebGLTranslateGizmoRenderer.drawStrip(gl, mvp, strip, segment.color(), 1.0, gl.TRIANGLE_STRIP);
        }
    }

    /**
    Draws a cone whose base is at the position of the element, pointing to the
    local +Z direction of the element.
    */
    private static async drawCone(gl: WebGL2RenderingContext, mvp: Matrix4x4d, element: SimpleBody,
                                  cone: Cone): Promise<void> {
        const radius: number = cone.getBottomRadius();
        const height: number = cone.getHeight();
        const material: SimpleMaterial | null = element.getMaterial();

        if (material === null) {
            return;
        }
        const c: ColorRgb = material.getDiffuse();
        const local: Matrix4x4d = GizmoSolidTessellator.localTransform(element);
        const shade: number = WebGLTranslateGizmoRenderer.CONE_BASE_SHADE;

        // Side
        const sideFan: Vector3Dd[] = GizmoSolidTessellator.buildConeSideFan(local, radius, height);
        await WebGLTranslateGizmoRenderer.drawStrip(gl, mvp, sideFan, c, 1.0, gl.TRIANGLE_FAN);

        // Base, darker
        const dark: ColorRgb = new ColorRgb(c.r() * shade, c.g() * shade, c.b() * shade);
        const baseFan: Vector3Dd[] = GizmoSolidTessellator.buildConeBaseFan(local, radius);

        await WebGLTranslateGizmoRenderer.drawStrip(gl, mvp, baseFan, dark, 1.0, gl.TRIANGLE_FAN);
    }

    /**
    Draws the translucent square that shows a selected plane handle, centered
    at the position of the element and over its local XY plane.
    */
    private static async drawPlaneHandle(gl: WebGL2RenderingContext, mvp: Matrix4x4d, element: SimpleBody,
                                         box: Box): Promise<void> {
        const material: SimpleMaterial | null = element.getMaterial();

        if (material === null) {
            return;
        }
        const c: ColorRgb = material.getDiffuse();
        const local: Matrix4x4d = GizmoSolidTessellator.localTransform(element);
        const quad: Vector3Dd[] = GizmoSolidTessellator.buildPlaneQuad(
            local, box.getSize().x(), box.getSize().y());

        await WebGLTranslateGizmoRenderer.drawStrip(gl, mvp, quad, c, material.getOpacity(),
            gl.TRIANGLE_STRIP);
    }

    private static async drawStrip(gl: WebGL2RenderingContext, mvp: Matrix4x4d, points: Vector3Dd[], c: ColorRgb,
                                   alpha: number, primitiveType: number): Promise<void> {
        const positions: Float32Array = GizmoVertexArrayBuilder.buildPositions(points);
        const colors: Float32Array = GizmoVertexArrayBuilder.buildRgbaColors(points.length, c, alpha);

        await WebGLColoredPrimitiveRenderer.draw(gl, mvp, primitiveType, positions, colors);
    }
}

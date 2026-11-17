package vsdk.toolkit.render.jogl.gizmo;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.CameraControlGizmo;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link CameraControlGizmo} (a ViewCube) as a HUD over its area of
a viewport, with the GL4 core pipeline.

The parts of the gizmo (26 boxes and the compass tube) are drawn with
`Jogl4GeometryRenderer`, lit from the HUD camera and seen through it, in a
GL viewport restricted to the area of the gizmo, with its own depth buffer
(the depth of that area is cleared). Their edges are drawn as lines from
their boundary representations (see `CameraControlGizmo.getEdgeSegments`).
An inactive gizmo is translucent: the fragments are blended with a constant
alpha (the opacity of the gizmo), as the mesh shaders of the pipeline do not
handle the opacity of materials. Labels depend on the way images are created
and cached by the application, so they are delegated to a `LabelPainter`.

All the OpenGL state changed is restored.
*/
public class Jogl4CameraControlGizmoRenderer extends Jogl4Renderer {

    /**
    Draws the text of the label of a face of the cube.
    */
    public interface LabelPainter {
        /**
        @param gl OpenGL context
        @param label text of the label
        @param x horizontal pixel of the viewport (from its left) of the
        center of the label
        @param y vertical pixel of the viewport (from its top) of the center
        of the label
        */
        void drawLabel(GL4 gl, String label, double x, double y);
    }

    private static final float EDGE_DEPTH_BIAS = -0.0005f;
    private static final double MIN_LABEL_VISIBILITY = 0.3;

    private Jogl4CameraControlGizmoRenderer() {
    }

    /**
    @param gl OpenGL context
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
    public static void draw(
        GL4 gl,
        CameraControlGizmo gizmo,
        int viewportStartX,
        int viewportStartY,
        int viewportSizeY,
        float lineWidth,
        LabelPainter labelPainter)
    {
        if ( gl == null || gizmo == null || !gizmo.isVisible() ) {
            return;
        }

        //-----------------------------------------------------------------
        int size = gizmo.getSizeInPixels();
        int x = viewportStartX + gizmo.getHudX();
        int y = viewportStartY + viewportSizeY - gizmo.getHudY() - size;
        int[] previousViewport = new int[4];
        boolean scissorWasEnabled = gl.glIsEnabled(GL4.GL_SCISSOR_TEST);
        boolean blendWasEnabled = gl.glIsEnabled(GL4.GL_BLEND);
        int[] previousScissor = new int[4];

        gl.glGetIntegerv(GL4.GL_VIEWPORT, previousViewport, 0);
        gl.glGetIntegerv(GL4.GL_SCISSOR_BOX, previousScissor, 0);

        // Own depth for the area of the gizmo
        gl.glEnable(GL4.GL_SCISSOR_TEST);
        gl.glScissor(x, y, size, size);
        gl.glClear(GL4.GL_DEPTH_BUFFER_BIT);
        gl.glViewport(x, y, size, size);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_CULL_FACE);

        double opacity = gizmo.getOpacity();
        if ( opacity < 1.0 ) {
            gl.glEnable(GL4.GL_BLEND);
            gl.glBlendColor(0.0f, 0.0f, 0.0f, (float)opacity);
            gl.glBlendFunc(GL4.GL_CONSTANT_ALPHA, GL4.GL_ONE_MINUS_CONSTANT_ALPHA);
        }

        //-----------------------------------------------------------------
        Camera hud = gizmo.getHudCamera();
        RendererConfiguration quality = new RendererConfiguration();
        quality.setSurfaces(true);
        quality.setWires(false);
        quality.setTexture(false);
        quality.setBumpMap(false);
        quality.setNormals(false);
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_GOURAUD);

        Matrix4x4d projection = hud.calculateProjectionMatrix();
        ColorRgb lineColor = gizmo.getLineColor();

        // All the surfaces first, so their depth hides the edges behind them
        for ( int p = 0; p < CameraControlGizmo.NUMBER_OF_PARTS; p++ ) {
            SimpleBody part = gizmo.getParts().get(p);
            Matrix4x4d local = new Matrix4x4d().translation(part.getPosition());

            // No lights: the pipeline uses a light at the camera
            Jogl4GeometryRenderer.draw(gl, part.getGeometry(), hud,
                (java.util.List<Light>)null,
                part.getMaterial(), quality, null, null, local);
        }
        for ( int p = 0; p < CameraControlGizmo.NUMBER_OF_PARTS; p++ ) {
            float[] positions = gizmo.getEdgeSegments(p);
            float[] colors = new float[positions.length];
            for ( int i = 0; i < colors.length; i += 3 ) {
                colors[i] = (float)lineColor.r();
                colors[i + 1] = (float)lineColor.g();
                colors[i + 2] = (float)lineColor.b();
            }
            Jogl4LineRenderer.drawLines(gl, projection, positions, colors,
                lineWidth, EDGE_DEPTH_BIAS);
        }

        //-----------------------------------------------------------------
        gl.glViewport(previousViewport[0], previousViewport[1],
            previousViewport[2], previousViewport[3]);
        gl.glScissor(previousScissor[0], previousScissor[1],
            previousScissor[2], previousScissor[3]);
        if ( !scissorWasEnabled ) {
            gl.glDisable(GL4.GL_SCISSOR_TEST);
        }
        if ( opacity < 1.0 ) {
            gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
            if ( !blendWasEnabled ) {
                gl.glDisable(GL4.GL_BLEND);
            }
        }

        if ( labelPainter != null ) {
            for ( int face : gizmo.getFaceRegions() ) {
                if ( gizmo.faceVisibility(face) < MIN_LABEL_VISIBILITY ) {
                    continue;
                }
                double[] c = gizmo.projectToViewport(gizmo.getFaceLabelPosition(face));
                labelPainter.drawLabel(gl, gizmo.getFaceLabel(face), c[0], c[1]);
            }
        }
        gl.glEnable(GL4.GL_DEPTH_TEST);
    }
}

package vsdk.toolkit.render.jogl.gizmo;

import java.util.ArrayList;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.GizmoSolidTessellator;
import vsdk.toolkit.gui.gizmo.GizmoVertexArrayBuilder;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.render.jogl.Jogl4ColoredPrimitiveRenderer;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link ScaleGizmo} with the GL4 core pipeline, following the
technique `Jogl4TranslateGizmoRenderer` uses for `TranslateGizmo`:

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

Usage (render thread, once per frame, after the transformation matrix and
scale of the gizmo have been set):
<pre>
    Jogl4ScaleGizmoRenderer.draw(gl, gizmo, camera);
</pre>
*/
public class Jogl4ScaleGizmoRenderer extends Jogl4Renderer {
    /// Opacity of the interior of the flat handle currently selected
    private static final float HANDLE_OPACITY = 0.55f;
    /// Depth bias (towards the viewer) of the contour of the flat handles
    private static final float CONTOUR_DEPTH_BIAS = -0.0002f;

    /**
    Draws the gizmo over the current contents of the surface.

    @param gl OpenGL context
    @param gizmo gizmo to draw; its transformation matrix must be set
    @param camera camera that views the gizmo
    */
    public static void draw(GL4 gl, ScaleGizmo gizmo, Camera camera)
    {
        if ( gl == null || gizmo == null || camera == null ) {
            return;
        }
        Matrix4x4d mvp = camera.calculateProjectionMatrix();

        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);

        for ( SimpleBody element : gizmo.getElements() ) {
            Geometry g = element.getGeometry();

            if ( g instanceof Cone ) {
                drawShaft(gl, mvp, element, (Cone)g);
            }
            else if ( g instanceof Box ) {
                drawBox(gl, mvp, element, (Box)g);
            }
        }

        //- Contour of the flat handles -------------------------------------
        drawContour(gl, mvp, gizmo);

        //- Interior of the handle selected, translucent over everything -----
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthMask(false);
        for ( int group : ScaleGizmo.BAND_GROUPS ) {
            if ( gizmo.isBandHighlighted(group) ) {
                Vector3Dd[] quad = gizmo.buildBandQuad(group);

                drawStrip(gl, mvp, quad, ScaleGizmo.HANDLE_FILL_COLOR, HANDLE_OPACITY,
                    GL4.GL_TRIANGLE_STRIP);
            }
        }
        if ( gizmo.isUniformHighlighted() ) {
            drawUniformHandle(gl, mvp, gizmo);
        }
        gl.glDepthMask(true);
        gl.glDisable(GL4.GL_BLEND);
    }

    /**
    Draws the outline of the two-axis bands and of the uniform handle (see
    `ScaleGizmo.buildContourSegments`), each half edge with the color the
    gizmo gives it.
    */
    private static void drawContour(GL4 gl, Matrix4x4d mvp, ScaleGizmo gizmo)
    {
        ArrayList<ScaleGizmo.ContourSegment> segments = gizmo.buildContourSegments();

        if ( segments.isEmpty() ) {
            return;
        }

        float[] positions = new float[segments.size()*2*3];
        float[] colors = new float[segments.size()*2*3];
        int vertex = 0;

        for ( ScaleGizmo.ContourSegment segment : segments ) {
            GizmoVertexArrayBuilder.putVertex(positions, vertex, segment.start());
            GizmoVertexArrayBuilder.putRgb(colors, vertex, segment.color());
            vertex++;
            GizmoVertexArrayBuilder.putVertex(positions, vertex, segment.end());
            GizmoVertexArrayBuilder.putRgb(colors, vertex, segment.color());
            vertex++;
        }
        // Slightly towards the viewer, so the translucent interior of a
        // selected handle does not z-fight with its own outline
        Jogl4LineRenderer.drawLines(gl, mvp, positions, colors,
            (float)gizmo.getLineWidth(), CONTOUR_DEPTH_BIAS);
    }

    /**
    Draws the lateral surface of a shaft: a cylinder (equal base and top
    radius) from the local origin of `element`, growing along its local +Z.
    */
    private static void drawShaft(GL4 gl, Matrix4x4d mvp, SimpleBody element, Cone cone)
    {
        ColorRgb c = element.getMaterial().getDiffuse();
        Matrix4x4d local = GizmoSolidTessellator.localTransform(element);
        Vector3Dd[] strip = GizmoSolidTessellator.buildShaftStrip(
            local, cone.getBaseRadius(), cone.getTopRadius(), cone.getHeight());

        drawStrip(gl, mvp, strip, c, 1.0, GL4.GL_TRIANGLE_STRIP);
    }

    /**
    Draws a box as 6 solid quads, centered at the local origin of `element`.
    */
    private static void drawBox(GL4 gl, Matrix4x4d mvp, SimpleBody element, Box box)
    {
        ColorRgb c = element.getMaterial().getDiffuse();
        Matrix4x4d local = GizmoSolidTessellator.localTransform(element);
        Vector3Dd[][] faces = GizmoSolidTessellator.buildBoxFaceStrips(local, box.getSize());

        for ( Vector3Dd[] face : faces ) {
            drawStrip(gl, mvp, face, c, 1.0, GL4.GL_TRIANGLE_STRIP);
        }
    }

    private static void drawStrip(GL4 gl, Matrix4x4d mvp, Vector3Dd[] points, ColorRgb c,
                                  double alpha, int primitiveType)
    {
        float[] positions = GizmoVertexArrayBuilder.buildPositions(points);
        float[] colors = GizmoVertexArrayBuilder.buildRgbaColors(points.length, c, alpha);

        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, primitiveType, positions, colors);
    }

    /**
    Fills the three triangles of the uniform handle (see
    `ScaleGizmo.buildUniformTriangles`), one in each coordinate plane of the
    frame of the gizmo.
    */
    private static void drawUniformHandle(GL4 gl, Matrix4x4d mvp, ScaleGizmo gizmo)
    {
        Vector3Dd[] triangles = gizmo.buildUniformTriangles();

        drawStrip(gl, mvp, triangles, ScaleGizmo.HANDLE_FILL_COLOR, HANDLE_OPACITY,
            GL4.GL_TRIANGLES);
    }
}

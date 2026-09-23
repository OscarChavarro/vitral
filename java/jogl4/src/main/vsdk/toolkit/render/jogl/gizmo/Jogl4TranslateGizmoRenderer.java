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
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmoLineSegment;
import vsdk.toolkit.render.jogl.Jogl4ColoredPrimitiveRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link TranslateGizmo} with the GL4 core pipeline (see
{@link Jogl4ColoredPrimitiveRenderer}), without depending on GLU nor on the
JOGL2 renderers.

- The lines of the gizmo (axis shafts and plane handle segments) are drawn as
  colored triangle strips facing the camera, so they can have the width given
  by {@link TranslateGizmo#getLineWidth()}.
- The heads of the axes are drawn as cones, tessellated in world space by
  {@link GizmoSolidTessellator}, with a darker base.
- The plane handle selected is drawn as a translucent quad.

Every element is generated in world space and drawn with the projection
matrix of the camera that views the gizmo.

Usage (render thread, once per frame, after the transformation matrix of the
gizmo has been set):
<pre>
    Jogl4TranslateGizmoRenderer.draw(gl, gizmo, camera);
</pre>
*/
public class Jogl4TranslateGizmoRenderer extends Jogl4Renderer {
    private static final double CONE_BASE_SHADE = 0.5;

    /**
    Draws the gizmo over the current contents of the surface. The blending and
    depth mask states changed are restored.

    @param gl OpenGL context
    @param gizmo gizmo to draw; its transformation matrix must be set
    @param camera camera that views the gizmo
    */
    public static void draw(GL4 gl, TranslateGizmo gizmo, Camera camera)
    {
        if ( gl == null || gizmo == null || camera == null ) {
            return;
        }

        ArrayList<SimpleBody> elements = gizmo.getElements3dsmax();
        Matrix4x4d mvp = camera.calculateProjectionMatrix();

        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);

        //- Opaque elements -----------------------------------------------
        drawLines(gl, gizmo, mvp);
        for ( SimpleBody element : elements ) {
            Geometry g = element.getGeometry();

            if ( g instanceof Cone ) {
                drawCone(gl, mvp, element, (Cone)g);
            }
        }

        //- Translucent elements, over the opaque ones --------------------
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthMask(false);
        for ( SimpleBody element : elements ) {
            Geometry g = element.getGeometry();

            if ( g instanceof Box ) {
                drawPlaneHandle(gl, mvp, element, (Box)g);
            }
        }

        //-----------------------------------------------------------------
        gl.glDepthMask(true);
        gl.glDisable(GL4.GL_BLEND);
    }

    private static void drawLines(GL4 gl, TranslateGizmo gizmo, Matrix4x4d mvp)
    {
        for ( TranslateGizmoLineSegment segment : gizmo.getLineSegments() ) {
            Vector3Dd[] strip = gizmo.buildLineStrip(segment);

            if ( strip == null ) {
                continue;
            }
            drawStrip(gl, mvp, strip, segment.color(), 1.0, GL4.GL_TRIANGLE_STRIP);
        }
    }

    /**
    Draws a cone whose base is at the position of the element, pointing to the
    local +Z direction of the element.
    */
    private static void drawCone(GL4 gl, Matrix4x4d mvp, SimpleBody element, Cone cone)
    {
        double radius = cone.getBottomRadius();
        double height = cone.getHeight();
        ColorRgb c = element.getMaterial().getDiffuse();
        Matrix4x4d local = GizmoSolidTessellator.localTransform(element);

        // Side
        Vector3Dd[] sideFan = GizmoSolidTessellator.buildConeSideFan(local, radius, height);
        drawStrip(gl, mvp, sideFan, c, 1.0, GL4.GL_TRIANGLE_FAN);

        // Base, darker
        ColorRgb dark = new ColorRgb(c.r()*CONE_BASE_SHADE, c.g()*CONE_BASE_SHADE,
            c.b()*CONE_BASE_SHADE);
        Vector3Dd[] baseFan = GizmoSolidTessellator.buildConeBaseFan(local, radius);

        drawStrip(gl, mvp, baseFan, dark, 1.0, GL4.GL_TRIANGLE_FAN);
    }

    /**
    Draws the translucent square that shows a selected plane handle, centered
    at the position of the element and over its local XY plane.
    */
    private static void drawPlaneHandle(GL4 gl, Matrix4x4d mvp, SimpleBody element, Box box)
    {
        ColorRgb c = element.getMaterial().getDiffuse();
        Matrix4x4d local = GizmoSolidTessellator.localTransform(element);
        Vector3Dd[] quad = GizmoSolidTessellator.buildPlaneQuad(
            local, box.getSize().x(), box.getSize().y());

        drawStrip(gl, mvp, quad, c, element.getMaterial().getOpacity(), GL4.GL_TRIANGLE_STRIP);
    }

    private static void drawStrip(GL4 gl, Matrix4x4d mvp, Vector3Dd[] points, ColorRgb c,
                                  double alpha, int primitiveType)
    {
        float[] positions = GizmoVertexArrayBuilder.buildPositions(points);
        float[] colors = GizmoVertexArrayBuilder.buildRgbaColors(points.length, c, alpha);

        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, primitiveType, positions, colors);
    }
}

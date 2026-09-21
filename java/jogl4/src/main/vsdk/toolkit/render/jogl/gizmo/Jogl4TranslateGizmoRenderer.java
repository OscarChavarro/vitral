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
- The heads of the axes are drawn as cones, with a darker base.
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
    private static final int CONE_SLICES = 16;
    private static final double CONE_BASE_SHADE = 0.5;

    private static final double[] SLICE_COS = new double[CONE_SLICES + 1];
    private static final double[] SLICE_SIN = new double[CONE_SLICES + 1];

    static {
        for ( int i = 0; i <= CONE_SLICES; i++ ) {
            double angle = 2*Math.PI*i/CONE_SLICES;

            SLICE_COS[i] = Math.cos(angle);
            SLICE_SIN[i] = Math.sin(angle);
        }
    }

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
            ColorRgb c = segment.color();
            float[] positions = new float[strip.length * 3];
            float[] colors = new float[strip.length * 4];

            for ( int i = 0; i < strip.length; i++ ) {
                putVertex(positions, i, strip[i]);
                putColor(colors, i, c, 1.0);
            }
            Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, GL4.GL_TRIANGLE_STRIP,
                positions, colors);
        }
    }

    /**
    Draws a cone whose base is at the position of the element, pointing to the
    local +Z direction of the element.
    */
    private static void drawCone(GL4 gl, Matrix4x4d mvp, SimpleBody element, Cone cone)
    {
        double radius = cone.getBaseRadius();
        double height = cone.getHeight();
        ColorRgb c = element.getMaterial().getDiffuse();
        Matrix4x4d local = localTransform(element);

        // Side: apex and base ring
        float[] positions = new float[(CONE_SLICES + 2) * 3];
        float[] colors = new float[(CONE_SLICES + 2) * 4];

        putVertex(positions, 0, local.multiply(new Vector3Dd(0, 0, height)));
        putColor(colors, 0, c, 1.0);
        for ( int i = 0; i <= CONE_SLICES; i++ ) {
            putVertex(positions, i + 1,
                local.multiply(new Vector3Dd(radius*SLICE_COS[i], radius*SLICE_SIN[i], 0)));
            putColor(colors, i + 1, c, 1.0);
        }
        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, GL4.GL_TRIANGLE_FAN, positions, colors);

        // Base, darker
        ColorRgb dark = new ColorRgb(c.r()*CONE_BASE_SHADE, c.g()*CONE_BASE_SHADE,
            c.b()*CONE_BASE_SHADE);

        putVertex(positions, 0, local.multiply(new Vector3Dd(0, 0, 0)));
        putColor(colors, 0, dark, 1.0);
        for ( int i = 0; i <= CONE_SLICES; i++ ) {
            int k = CONE_SLICES - i;

            putVertex(positions, i + 1,
                local.multiply(new Vector3Dd(radius*SLICE_COS[k], radius*SLICE_SIN[k], 0)));
            putColor(colors, i + 1, dark, 1.0);
        }
        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, GL4.GL_TRIANGLE_FAN, positions, colors);
    }

    /**
    Draws the translucent square that shows a selected plane handle, centered
    at the position of the element and over its local XY plane.
    */
    private static void drawPlaneHandle(GL4 gl, Matrix4x4d mvp, SimpleBody element, Box box)
    {
        double hx = box.getSize().x()/2;
        double hy = box.getSize().y()/2;
        ColorRgb c = element.getMaterial().getDiffuse();
        Matrix4x4d local = localTransform(element);
        float[] positions = new float[4 * 3];
        float[] colors = new float[4 * 4];

        putVertex(positions, 0, local.multiply(new Vector3Dd(-hx, -hy, 0)));
        putVertex(positions, 1, local.multiply(new Vector3Dd(hx, -hy, 0)));
        putVertex(positions, 2, local.multiply(new Vector3Dd(-hx, hy, 0)));
        putVertex(positions, 3, local.multiply(new Vector3Dd(hx, hy, 0)));
        for ( int i = 0; i < 4; i++ ) {
            putColor(colors, i, c, element.getMaterial().getOpacity());
        }
        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, GL4.GL_TRIANGLE_STRIP, positions, colors);
    }

    private static Matrix4x4d localTransform(SimpleBody element)
    {
        Vector3Dd position = element.getPosition();

        return new Matrix4x4d().translation(position).multiply(element.getRotation());
    }

    private static void putVertex(float[] positions, int index, Vector3Dd v)
    {
        positions[3*index] = (float)v.x();
        positions[3*index + 1] = (float)v.y();
        positions[3*index + 2] = (float)v.z();
    }

    private static void putColor(float[] colors, int index, ColorRgb c, double alpha)
    {
        colors[4*index] = (float)c.r();
        colors[4*index + 1] = (float)c.g();
        colors[4*index + 2] = (float)c.b();
        colors[4*index + 3] = (float)alpha;
    }
}

package vsdk.toolkit.render.jogl.gizmo;

import java.util.ArrayList;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link TranslateGizmo} with the fixed function pipeline of an
OpenGL compatibility context, without depending on GLU nor on the JOGL2
renderers.

- The lines of the gizmo (axis shafts and plane handle segments) are drawn as
  colored triangle strips facing the camera, so they can have the width given
  by {@link TranslateGizmo#getLineWidth()}.
- The heads of the axes are drawn as cones, with a darker base.
- The plane handle selected is drawn as a translucent quad.

The view transformation is expected to be already active in the projection
matrix, as done by the camera renderers of the applications: every element
is drawn in world space with an identity modelview matrix.

Usage (render thread, once per frame, after the transformation matrix of the
gizmo has been set):
<pre>
    Jogl4TranslateGizmoRenderer.draw(gl, gizmo);
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
    Draws the gizmo over the current contents of the surface. All the OpenGL
    state changed is restored.

    @param gl OpenGL context
    @param gizmo gizmo to draw; its transformation matrix must be set
    */
    public static void draw(GL2 gl, TranslateGizmo gizmo)
    {
        if ( gl == null || gizmo == null ) {
            return;
        }

        ArrayList<SimpleBody> elements = gizmo.getElements3dsmax();

        //-----------------------------------------------------------------
        gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT |
            GL2.GL_POLYGON_BIT | GL2.GL_COLOR_BUFFER_BIT |
            GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_LIGHTING_BIT);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);

        //- Opaque elements -----------------------------------------------
        drawLines(gl, gizmo);
        for ( SimpleBody element : elements ) {
            Geometry g = element.getGeometry();

            if ( g instanceof Cone ) {
                drawCone(gl, element, (Cone)g);
            }
        }

        //- Translucent elements, over the opaque ones --------------------
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthMask(false);
        for ( SimpleBody element : elements ) {
            Geometry g = element.getGeometry();

            if ( g instanceof Box ) {
                drawPlaneHandle(gl, element, (Box)g);
            }
        }

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        gl.glPopAttrib();
    }

    private static void drawLines(GL2 gl, TranslateGizmo gizmo)
    {
        for ( TranslateGizmo.LineSegment segment : gizmo.getLineSegments() ) {
            Vector3Dd[] strip = gizmo.buildLineStrip(segment);

            if ( strip == null ) {
                continue;
            }
            ColorRgb c = segment.getColor();

            gl.glColor3d(c.r(), c.g(), c.b());
            gl.glBegin(GL.GL_TRIANGLE_STRIP);
            for ( Vector3Dd v : strip ) {
                gl.glVertex3d(v.x(), v.y(), v.z());
            }
            gl.glEnd();
        }
    }

    /**
    Draws a cone whose base is at the position of the element, pointing to the
    local +Z direction of the element.
    */
    private static void drawCone(GL2 gl, SimpleBody element, Cone cone)
    {
        double radius = cone.getBaseRadius();
        double height = cone.getHeight();
        ColorRgb c = element.getMaterial().getDiffuse();

        gl.glPushMatrix();
        translateAndRotate(gl, element);

        gl.glColor3d(c.r(), c.g(), c.b());
        gl.glBegin(GL.GL_TRIANGLE_FAN);
        gl.glVertex3d(0, 0, height);
        for ( int i = 0; i <= CONE_SLICES; i++ ) {
            gl.glVertex3d(radius*SLICE_COS[i], radius*SLICE_SIN[i], 0);
        }
        gl.glEnd();

        gl.glColor3d(c.r()*CONE_BASE_SHADE, c.g()*CONE_BASE_SHADE,
            c.b()*CONE_BASE_SHADE);
        gl.glBegin(GL.GL_TRIANGLE_FAN);
        gl.glVertex3d(0, 0, 0);
        for ( int i = CONE_SLICES; i >= 0; i-- ) {
            gl.glVertex3d(radius*SLICE_COS[i], radius*SLICE_SIN[i], 0);
        }
        gl.glEnd();

        gl.glPopMatrix();
    }

    /**
    Draws the translucent square that shows a selected plane handle, centered
    at the position of the element and over its local XY plane.
    */
    private static void drawPlaneHandle(GL2 gl, SimpleBody element, Box box)
    {
        double hx = box.getSize().x()/2;
        double hy = box.getSize().y()/2;
        ColorRgb c = element.getMaterial().getDiffuse();

        gl.glPushMatrix();
        translateAndRotate(gl, element);

        gl.glColor4d(c.r(), c.g(), c.b(), element.getMaterial().getOpacity());
        gl.glBegin(GL2.GL_TRIANGLE_STRIP);
        gl.glVertex3d(-hx, -hy, 0);
        gl.glVertex3d(hx, -hy, 0);
        gl.glVertex3d(-hx, hy, 0);
        gl.glVertex3d(hx, hy, 0);
        gl.glEnd();

        gl.glPopMatrix();
    }

    private static void translateAndRotate(GL2 gl, SimpleBody element)
    {
        Vector3Dd position = element.getPosition();

        gl.glTranslated(position.x(), position.y(), position.z());
        multiplyMatrix(gl, element.getRotation());
    }

    private static void multiplyMatrix(GL2 gl, Matrix4x4d matrix)
    {
        double[] columnMajor = new double[16];
        int pos = 0;

        for ( int column = 0; column < 4; column++ ) {
            for ( int row = 0; row < 4; row++ ) {
                columnMajor[pos++] = matrix.get(row, column);
            }
        }
        gl.glMultMatrixd(columnMajor, 0);
    }
}

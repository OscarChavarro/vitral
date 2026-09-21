package vsdk.toolkit.render.jogl.gizmo;

import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.gizmo.ReferenceFrameGizmo;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link ReferenceFrameGizmo} over the lower left corner of a
viewport, with the fixed function pipeline of an OpenGL compatibility context.

The axes are drawn as colored triangle strips, so they can have the width
given by {@link ReferenceFrameGizmo#getLineWidth()}, oriented according to the
rotation of the camera (see
{@link ReferenceFrameGizmo#estimateOrientation(Matrix4x4d)}).
Text of the axis labels depends on the way images are created and cached by
the application, so it is delegated to a {@link LabelPainter}.

Usage (render thread, once per frame, after drawing the viewport contents):
<pre>
    Jogl4ReferenceFrameGizmoRenderer.draw(gl, gizmo, camera.getRotation(),
        viewportStartX, viewportStartY, labelPainter);
</pre>
*/
public class Jogl4ReferenceFrameGizmoRenderer extends Jogl4Renderer {

    /**
    Draws the text of the label of one axis of the gizmo.
    */
    public interface LabelPainter {
        /**
        Draws the label of an axis. It is called with the modelview matrix
        set to the orientation of the gizmo, translated to the label position
        of the axis (its end), and the OpenGL viewport set to the area of the
        gizmo.

        @param gl OpenGL context
        @param axis one of `ReferenceFrameGizmo.AXIS_X`,
        `ReferenceFrameGizmo.AXIS_Y` or `ReferenceFrameGizmo.AXIS_Z`
        @param label text of the label, as given by
        `ReferenceFrameGizmo.getAxisLabel(axis)`
        */
        void drawLabel(GL2 gl, int axis, String label);
    }

    /**
    Draws the gizmo in a square area of the surface, with its lower left
    corner at the given position. All the OpenGL state changed is restored.

    @param gl OpenGL context
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
    public static void draw(
        GL2 gl,
        ReferenceFrameGizmo gizmo,
        Matrix4x4d cameraRotation,
        int pixelStartX,
        int pixelStartY,
        LabelPainter labelPainter)
    {
        if ( gl == null || gizmo == null || cameraRotation == null || !gizmo.isVisible() ) {
            return;
        }

        //-----------------------------------------------------------------
        gl.glPushAttrib(GL2.GL_VIEWPORT_BIT | GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT |
            GL2.GL_POLYGON_BIT);
        gl.glViewport(pixelStartX, pixelStartY, gizmo.getSizeInPixels(), gizmo.getSizeInPixels());

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);

        //-----------------------------------------------------------------
        gl.glLoadIdentity();
        multiplyMatrix(gl, gizmo.estimateOrientation(cameraRotation));

        if ( labelPainter != null ) {
            for ( int axis = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++ ) {
                Vector3Dd p = gizmo.getLabelPosition(axis);

                gl.glPushMatrix();
                gl.glTranslated(p.x(), p.y(), p.z());
                labelPainter.drawLabel(gl, axis, gizmo.getAxisLabel(axis));
                gl.glPopMatrix();
            }
        }

        // The strips are already in the canonical space of the gizmo
        gl.glLoadIdentity();
        for ( int axis = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++ ) {
            ColorRgb c = gizmo.getAxisColor(axis);
            Vector3Dd[] strip = gizmo.buildAxisStrip(axis, cameraRotation);

            gl.glColor3d(c.r(), c.g(), c.b());
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);
            for ( Vector3Dd v : strip ) {
                gl.glVertex3d(v.x(), v.y(), v.z());
            }
            gl.glEnd();
        }

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glPopAttrib();
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

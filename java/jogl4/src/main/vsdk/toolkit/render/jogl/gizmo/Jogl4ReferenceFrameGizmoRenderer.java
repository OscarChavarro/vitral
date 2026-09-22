package vsdk.toolkit.render.jogl.gizmo;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.gizmo.GizmoVertexArrayBuilder;
import vsdk.toolkit.gui.gizmo.ReferenceFrameGizmo;
import vsdk.toolkit.render.jogl.Jogl4ColoredPrimitiveRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link ReferenceFrameGizmo} over the lower left corner of a
viewport, with the GL4 core pipeline.

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
        Draws the label of an axis, at the projection of the label position of
        the axis (its end) over the surface.

        @param gl OpenGL context
        @param axis one of `ReferenceFrameGizmo.AXIS_X`,
        `ReferenceFrameGizmo.AXIS_Y` or `ReferenceFrameGizmo.AXIS_Z`
        @param label text of the label, as given by
        `ReferenceFrameGizmo.getAxisLabel(axis)`
        @param windowX horizontal position, in pixels of the surface, of the
        label anchor
        @param windowY vertical position, in pixels of the surface, of the
        label anchor (from the bottom)
        */
        void drawLabel(GL4 gl, int axis, String label, double windowX, double windowY);
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
        GL4 gl,
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
        int[] previousViewport = new int[4];
        int size = gizmo.getSizeInPixels();
        Matrix4x4d orientation = gizmo.estimateOrientation(cameraRotation);

        gl.glGetIntegerv(GL4.GL_VIEWPORT, previousViewport, 0);
        gl.glViewport(pixelStartX, pixelStartY, size, size);
        gl.glDisable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);

        //-----------------------------------------------------------------
        if ( labelPainter != null ) {
            for ( int axis = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++ ) {
                Vector3Dd p = orientation.multiply(gizmo.getLabelPosition(axis));

                // Canonical space [-1, 1] covers the area of the gizmo
                double windowX = pixelStartX + (p.x() + 1) * size / 2;
                double windowY = pixelStartY + (p.y() + 1) * size / 2;

                labelPainter.drawLabel(gl, axis, gizmo.getAxisLabel(axis), windowX, windowY);
            }
        }

        // The strips are already in the canonical space of the gizmo
        Matrix4x4d identity = Matrix4x4d.identityMatrix();
        for ( int axis = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++ ) {
            ColorRgb c = gizmo.getAxisColor(axis);
            Vector3Dd[] strip = gizmo.buildAxisStrip(axis, cameraRotation);
            float[] positions = GizmoVertexArrayBuilder.buildPositions(strip);
            float[] colors = GizmoVertexArrayBuilder.buildRgbaColors(strip.length, c, 1.0);

            Jogl4ColoredPrimitiveRenderer.draw(gl, identity, GL4.GL_TRIANGLE_STRIP,
                positions, colors);
        }

        //-----------------------------------------------------------------
        gl.glViewport(previousViewport[0], previousViewport[1], previousViewport[2],
            previousViewport[3]);
        gl.glEnable(GL4.GL_DEPTH_TEST);
    }
}

package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.render.SelectionCorners;

/**
Draws the "selection corners" mark around an object (see
{@link SelectionCorners}, which holds its geometry) with the GL4 pipeline.

`Jogl4MeshRenderer` draws it when the rendering configuration asks for it
(`RendererConfiguration.isSelectionCornersSet()`), so every geometry drawn
through it supports the mark; it can also be drawn directly for anything with
a bounding box.
*/
public final class Jogl4SelectionCornersRenderer extends Jogl4Renderer {
    private Jogl4SelectionCornersRenderer() {
    }

    /**
    Draws the mark around the bounding box of a geometry.

    @param gl OpenGL context
    @param geometry geometry the mark surrounds
    @param camera camera that views the geometry
    @param localTransform transformation from geometry space to world space;
    null for the identity
    */
    public static void draw(GL4 gl, Geometry geometry, Camera camera, Matrix4x4d localTransform)
    {
        if ( geometry == null ) {
            return;
        }
        draw(gl, geometry.getMinMax(), camera, localTransform);
    }

    /**
    Draws the mark around a bounding box.

    @param gl OpenGL context
    @param minmax bounding box as given by `Geometry.getMinMax()`
    @param camera camera that views the box
    @param localTransform transformation from box space to world space; null
    for the identity
    */
    public static void draw(GL4 gl, double[] minmax, Camera camera, Matrix4x4d localTransform)
    {
        if ( gl == null || camera == null ) {
            return;
        }
        Vector3Dd[] segments = SelectionCorners.buildSegments(minmax);

        if ( segments.length == 0 ) {
            return;
        }
        ColorRgb c = SelectionCorners.getDefaultColor();
        float[] positions = new float[segments.length * 3];
        float[] colors = new float[segments.length * 3];

        for ( int i = 0; i < segments.length; i++ ) {
            positions[3*i] = (float)segments[i].x();
            positions[3*i + 1] = (float)segments[i].y();
            positions[3*i + 2] = (float)segments[i].z();
            colors[3*i] = (float)c.r();
            colors[3*i + 1] = (float)c.g();
            colors[3*i + 2] = (float)c.b();
        }
        Matrix4x4d local = (localTransform != null) ? localTransform : Matrix4x4d.identityMatrix();

        Jogl4LineRenderer.drawLines(gl, camera.calculateProjectionMatrix().multiply(local),
            positions, colors, 1.0f);
    }
}

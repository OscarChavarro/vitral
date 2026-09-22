package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Packs the geometry the gizmos generate (arrays of `Vector3Dd` points in world
space, or in the canonical space of the gizmo) into the flat `float` arrays
every rasterization technology consumes as vertex attributes.

This class does not depend on any particular visualization technology: it only
defines the two interleaving-free layouts the gizmo renderers use, positions as
`{x, y, z}` per vertex, and colors as `{r, g, b}` or `{r, g, b, a}` per vertex.
*/
public class GizmoVertexArrayBuilder {
    /**
    Writes a point at the position of a vertex inside a positions array.

    @param positions array with 3 floats per vertex
    @param index number of the vertex to write
    @param point point to write
    */
    public static void putVertex(float[] positions, int index, Vector3Dd point)
    {
        positions[3*index] = (float)point.x();
        positions[3*index + 1] = (float)point.y();
        positions[3*index + 2] = (float)point.z();
    }

    /**
    Writes a color at the position of a vertex inside an opaque colors array.

    @param colors array with 3 floats per vertex
    @param index number of the vertex to write
    @param color color to write
    */
    public static void putRgb(float[] colors, int index, ColorRgb color)
    {
        colors[3*index] = (float)color.r();
        colors[3*index + 1] = (float)color.g();
        colors[3*index + 2] = (float)color.b();
    }

    /**
    Writes a color at the position of a vertex inside a colors array with
    opacity.

    @param colors array with 4 floats per vertex
    @param index number of the vertex to write
    @param color color to write
    @param alpha opacity of the vertex, in [0, 1]
    */
    public static void putRgba(float[] colors, int index, ColorRgb color, double alpha)
    {
        colors[4*index] = (float)color.r();
        colors[4*index + 1] = (float)color.g();
        colors[4*index + 2] = (float)color.b();
        colors[4*index + 3] = (float)alpha;
    }

    /**
    @param points points of the primitive, in the order they are drawn
    @return a positions array with the given points
    */
    public static float[] buildPositions(Vector3Dd[] points)
    {
        float[] positions = new float[points.length*3];

        for ( int i = 0; i < points.length; i++ ) {
            putVertex(positions, i, points[i]);
        }
        return positions;
    }

    /**
    @param numberOfVertexes number of vertexes of the primitive
    @param color color of every vertex
    @return an opaque colors array, with 3 floats per vertex
    */
    public static float[] buildRgbColors(int numberOfVertexes, ColorRgb color)
    {
        float[] colors = new float[numberOfVertexes*3];

        for ( int i = 0; i < numberOfVertexes; i++ ) {
            putRgb(colors, i, color);
        }
        return colors;
    }

    /**
    @param numberOfVertexes number of vertexes of the primitive
    @param color color of every vertex
    @param alpha opacity of every vertex, in [0, 1]
    @return a colors array with opacity, with 4 floats per vertex
    */
    public static float[] buildRgbaColors(int numberOfVertexes, ColorRgb color, double alpha)
    {
        float[] colors = new float[numberOfVertexes*4];

        for ( int i = 0; i < numberOfVertexes; i++ ) {
            putRgba(colors, i, color, alpha);
        }
        return colors;
    }
}

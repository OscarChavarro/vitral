import type { ColorRgb } from "../../common/color/ColorRgb.js";
import type { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";

/**
Packs the geometry the gizmos generate (arrays of `Vector3Dd` points in world
space, or in the canonical space of the gizmo) into the flat `float` arrays
every rasterization technology consumes as vertex attributes.

This class does not depend on any particular visualization technology: it only
defines the two interleaving-free layouts the gizmo renderers use, positions as
`{x, y, z}` per vertex, and colors as `{r, g, b}` or `{r, g, b, a}` per vertex.
Java's `float[]` is a `Float32Array` here, so every value is rounded to a
float exactly as Java's `(float)` cast does.
*/
export class GizmoVertexArrayBuilder {
    /**
    Writes a point at the position of a vertex inside a positions array.

    @param positions array with 3 floats per vertex
    @param index number of the vertex to write
    @param point point to write
    */
    public static putVertex(positions: Float32Array, index: number, point: Vector3Dd): void {
        positions[3 * index] = point.x();
        positions[3 * index + 1] = point.y();
        positions[3 * index + 2] = point.z();
    }

    /**
    Writes a color at the position of a vertex inside an opaque colors array.

    @param colors array with 3 floats per vertex
    @param index number of the vertex to write
    @param color color to write
    */
    public static putRgb(colors: Float32Array, index: number, color: ColorRgb): void {
        colors[3 * index] = color.r();
        colors[3 * index + 1] = color.g();
        colors[3 * index + 2] = color.b();
    }

    /**
    Writes a color at the position of a vertex inside a colors array with
    opacity.

    @param colors array with 4 floats per vertex
    @param index number of the vertex to write
    @param color color to write
    @param alpha opacity of the vertex, in [0, 1]
    */
    public static putRgba(colors: Float32Array, index: number, color: ColorRgb, alpha: number): void {
        colors[4 * index] = color.r();
        colors[4 * index + 1] = color.g();
        colors[4 * index + 2] = color.b();
        colors[4 * index + 3] = alpha;
    }

    /**
    @param points points of the primitive, in the order they are drawn
    @return a positions array with the given points
    */
    public static buildPositions(points: readonly Vector3Dd[]): Float32Array {
        const positions = new Float32Array(points.length * 3);

        for (let i = 0; i < points.length; i++) {
            GizmoVertexArrayBuilder.putVertex(positions, i, points[i]!);
        }
        return positions;
    }

    /**
    @param numberOfVertexes number of vertexes of the primitive
    @param color color of every vertex
    @return an opaque colors array, with 3 floats per vertex
    */
    public static buildRgbColors(numberOfVertexes: number, color: ColorRgb): Float32Array {
        const colors = new Float32Array(numberOfVertexes * 3);

        for (let i = 0; i < numberOfVertexes; i++) {
            GizmoVertexArrayBuilder.putRgb(colors, i, color);
        }
        return colors;
    }

    /**
    @param numberOfVertexes number of vertexes of the primitive
    @param color color of every vertex
    @param alpha opacity of every vertex, in [0, 1]
    @return a colors array with opacity, with 4 floats per vertex
    */
    public static buildRgbaColors(numberOfVertexes: number, color: ColorRgb, alpha: number): Float32Array {
        const colors = new Float32Array(numberOfVertexes * 4);

        for (let i = 0; i < numberOfVertexes; i++) {
            GizmoVertexArrayBuilder.putRgba(colors, i, color, alpha);
        }
        return colors;
    }
}

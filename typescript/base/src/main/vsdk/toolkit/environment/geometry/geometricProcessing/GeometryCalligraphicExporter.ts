import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { Geometry } from "../Geometry.js";
import { Curve } from "../curve/Curve.js";
import { FunctionalExplicitSurface } from "../surface/FunctionalExplicitSurface.js";
import { InfinitePlane } from "../surface/InfinitePlane.js";
import { Md2Mesh } from "../surface/Md2Mesh.js";
import { ParametricBiCubicPatch } from "../surface/ParametricBiCubicPatch.js";
import { QuadMesh } from "../surface/QuadMesh.js";
import { SubdividedBezierPatch } from "../surface/SubdividedBezierPatch.js";
import { TriangleMesh } from "../surface/TriangleMesh.js";
import type { TriangleMeshGroup } from "../surface/TriangleMeshGroup.js";
import { TriangleStripMesh } from "../surface/TriangleStripMesh.js";
import { Calligraphic3DBuffer } from "../../../media/Calligraphic3DBuffer.js";
import { BezierPatchTessellator } from "../../../processing/BezierPatchTessellator.js";
import { GeometryTriangulator } from "./GeometryTriangulator.js";

/**
Exports the curves and surfaces to 3D lines (`Calligraphic3DBuffer`), the
generic representation used to draw them without a 3D graphics API (i.e. as
the "free lines" of the hidden line algorithm of `HiddenLineRenderer`, or as
a plain wireframe).

Curves give their own line approximation. Surfaces are first exported to a
mesh, and the lines are the edges of the mesh, each one once (an edge shared
by two faces is not repeated). Surfaces sampled on a grid of their
parameters are exported to meshes of quadrilaterals, so their lines are the
lines of the grid, without the diagonals of a triangulation.

An infinite plane has no finite mesh: a finite square of it is exported as a
hint of its position (see `exportToCalligraphic3DBuffer(InfinitePlane,
Vector3Dd)`). A MD2 model is not a geometry: one of its key frames is
exported (see `exportToCalligraphic3DBuffer(Md2Mesh, int)`).

The volumes are not exported here: their lines are the edges of their
boundary representation (see `Volume.exportToPolyhedralBoundedSolid`).
*/
export class GeometryCalligraphicExporter {
    /// Steps along u and v of the mesh of a subdivided Bezier patch, as in
    /// its OpenGL presentation
    public static readonly BEZIER_PATCH_STEPS = 24;
    /// Side of the square shown of an infinite plane, as in its OpenGL
    /// presentation
    public static readonly INFINITE_PLANE_SIZE = 20.0;
    /// Cells along each side of the square shown of an infinite plane
    public static readonly INFINITE_PLANE_TILES = 10;

    private constructor() {}

    /**
    Java overloads `exportToCalligraphic3DBuffer(Geometry geometry)`,
    `exportToCalligraphic3DBuffer(InfinitePlane plane, Vector3Dd near)` and
    `exportToCalligraphic3DBuffer(Md2Mesh model, int frame)`.

    With a geometry (a curve or a surface): the lines of the geometry, in its
    own coordinates, or null if it is not a curve, nor a surface that can be
    exported to a mesh.

    With a plane and a point of interest `near`, in the coordinates of the
    plane (i.e. the point viewed by the camera), near which the square shown
    is centered: the lines of a square of side `INFINITE_PLANE_SIZE` of the
    plane, divided into `INFINITE_PLANE_TILES` x `INFINITE_PLANE_TILES` cells
    (see `InfinitePlane.exportToQuadMesh`), or null for a degenerated plane.

    With a MD2 model and the index of one of its key frames: the edges of the
    triangles of the frame (see `Md2Mesh.exportToTriangleMesh`), or null if
    the model has no such frame.
    */
    public static exportToCalligraphic3DBuffer(geometry: Geometry | null): Calligraphic3DBuffer | null;
    public static exportToCalligraphic3DBuffer(plane: InfinitePlane, near: Vector3Dd): Calligraphic3DBuffer | null;
    public static exportToCalligraphic3DBuffer(model: Md2Mesh, frame: number): Calligraphic3DBuffer | null;
    public static exportToCalligraphic3DBuffer(
        geometry: Geometry | Md2Mesh | null,
        second?: Vector3Dd | number,
    ): Calligraphic3DBuffer | null {
        if (geometry instanceof Md2Mesh) {
            const mesh = geometry.exportToTriangleMesh(second as number);
            return mesh !== null ? GeometryCalligraphicExporter.exportEdges(mesh) : null;
        }
        if (geometry instanceof InfinitePlane && second instanceof Vector3Dd) {
            const mesh = geometry.exportToQuadMesh(
                second,
                GeometryCalligraphicExporter.INFINITE_PLANE_SIZE,
                GeometryCalligraphicExporter.INFINITE_PLANE_TILES,
            );
            return mesh !== null ? GeometryCalligraphicExporter.exportEdges(mesh) : null;
        }

        if (geometry === null) {
            return null;
        }
        if (geometry instanceof Curve) {
            return geometry.exportToCalligraphic3DBuffer();
        }
        if (geometry instanceof ParametricBiCubicPatch) {
            return GeometryCalligraphicExporter.exportEdges(geometry.exportToQuadMesh());
        }
        if (geometry instanceof FunctionalExplicitSurface) {
            const mesh = geometry.exportToQuadMesh();
            return mesh !== null ? GeometryCalligraphicExporter.exportEdges(mesh) : null;
        }
        if (geometry instanceof SubdividedBezierPatch) {
            const mesh = BezierPatchTessellator.tessellateQuads(
                geometry,
                GeometryCalligraphicExporter.BEZIER_PATCH_STEPS,
                GeometryCalligraphicExporter.BEZIER_PATCH_STEPS,
            );
            return mesh !== null ? GeometryCalligraphicExporter.exportEdges(mesh) : null;
        }
        if (geometry instanceof InfinitePlane) {
            return GeometryCalligraphicExporter.exportToCalligraphic3DBuffer(geometry, new Vector3Dd(0, 0, 0));
        }
        if (geometry instanceof QuadMesh) {
            return GeometryCalligraphicExporter.exportEdges(geometry);
        }
        if (geometry instanceof TriangleStripMesh) {
            const mesh = geometry.exportToTriangleMesh();
            return mesh !== null ? GeometryCalligraphicExporter.exportEdges(mesh) : null;
        }

        const meshes: TriangleMeshGroup | null = GeometryTriangulator.exportToTriangleMeshGroup(geometry);
        if (meshes === null) {
            return null;
        }
        const lines = new Calligraphic3DBuffer();
        for (const element of meshes.getMeshes()) {
            const mesh = element as TriangleMesh;
            GeometryCalligraphicExporter.addEdges(
                lines,
                mesh.getVertexPositions(),
                mesh.getTriangleIndexes(),
                mesh.getNumTriangles(),
                3,
            );
        }
        return lines;
    }

    /**
    Java overloads `exportEdges(QuadMesh mesh)` (the edges of the
    quadrilaterals of the mesh, each one once) and
    `exportEdges(TriangleMesh mesh)` (the edges of the triangles of the mesh,
    each one once).
    */
    public static exportEdges(mesh: QuadMesh): Calligraphic3DBuffer;
    public static exportEdges(mesh: TriangleMesh): Calligraphic3DBuffer;
    public static exportEdges(mesh: QuadMesh | TriangleMesh): Calligraphic3DBuffer {
        const lines = new Calligraphic3DBuffer();

        if (mesh instanceof QuadMesh) {
            GeometryCalligraphicExporter.addEdges(
                lines,
                mesh.getVertexPositions(),
                mesh.getQuadIndices(),
                mesh.getNumQuads(),
                4,
            );
            return lines;
        }
        GeometryCalligraphicExporter.addEdges(
            lines,
            mesh.getVertexPositions(),
            mesh.getTriangleIndexes(),
            mesh.getNumTriangles(),
            3,
        );
        return lines;
    }

    /**
    Adds the edges of the faces of a mesh, skipping the ones already added
    (by the vertex indexes of their ends, in any order).
    @param lines buffer receiving the edges
    @param positions x, y, z of each vertex
    @param faces indexes of the vertices of each face, `sides` per face
    @param numFaces number of faces
    @param sides number of vertices of each face
    */
    private static addEdges(
        lines: Calligraphic3DBuffer,
        positions: Float64Array | null,
        faces: Int32Array | null,
        numFaces: number,
        sides: number,
    ): void {
        if (positions === null || faces === null) {
            return;
        }
        const numVertices = Math.trunc(positions.length / 3);
        // Java HashSet<Long>, only used to test membership
        const added = new Set<number>();
        let f: number;
        let k: number;

        for (f = 0; f < numFaces && sides * f + sides <= faces.length; f++) {
            for (k = 0; k < sides; k++) {
                const a = faces[sides * f + k]!;
                const b = faces[sides * f + ((k + 1) % sides)]!;
                if (a === b || a < 0 || b < 0 || a >= numVertices || b >= numVertices) {
                    continue;
                }
                const key = Math.min(a, b) * numVertices + Math.max(a, b);
                if (added.has(key)) {
                    continue;
                }
                added.add(key);
                lines.add3DLine(
                    positions[3 * a]!,
                    positions[3 * a + 1]!,
                    positions[3 * a + 2]!,
                    positions[3 * b]!,
                    positions[3 * b + 1]!,
                    positions[3 * b + 2]!,
                );
            }
        }
    }
}

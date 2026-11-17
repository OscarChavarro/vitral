package vsdk.toolkit.environment.geometry.geometricProcessing;

import java.util.HashSet;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.curve.Curve;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.Md2Mesh;
import vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.surface.QuadMesh;
import vsdk.toolkit.environment.geometry.surface.SubdividedBezierPatch;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.surface.TriangleStripMesh;
import vsdk.toolkit.media.Calligraphic3DBuffer;
import vsdk.toolkit.processing.BezierPatchTessellator;

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
public class GeometryCalligraphicExporter
{
    /// Steps along u and v of the mesh of a subdivided Bezier patch, as in
    /// its OpenGL presentation
    public static final int BEZIER_PATCH_STEPS = 24;
    /// Side of the square shown of an infinite plane, as in its OpenGL
    /// presentation
    public static final double INFINITE_PLANE_SIZE = 20.0;
    /// Cells along each side of the square shown of an infinite plane
    public static final int INFINITE_PLANE_TILES = 10;

    private GeometryCalligraphicExporter()
    {
    }

    /**
    @param geometry a curve or a surface
    @return the lines of the geometry, in its own coordinates, or null if it
    is not a curve, nor a surface that can be exported to a mesh
    */
    public static Calligraphic3DBuffer exportToCalligraphic3DBuffer(Geometry geometry)
    {
        if ( geometry == null ) {
            return null;
        }
        if ( geometry instanceof Curve curve ) {
            return curve.exportToCalligraphic3DBuffer();
        }
        if ( geometry instanceof ParametricBiCubicPatch patch ) {
            return exportEdges(patch.exportToQuadMesh());
        }
        if ( geometry instanceof FunctionalExplicitSurface surface ) {
            QuadMesh mesh = surface.exportToQuadMesh();
            return mesh != null ? exportEdges(mesh) : null;
        }
        if ( geometry instanceof SubdividedBezierPatch patch ) {
            QuadMesh mesh = BezierPatchTessellator.tessellateQuads(patch,
                BEZIER_PATCH_STEPS, BEZIER_PATCH_STEPS);
            return mesh != null ? exportEdges(mesh) : null;
        }
        if ( geometry instanceof InfinitePlane plane ) {
            return exportToCalligraphic3DBuffer(plane, new Vector3Dd(0, 0, 0));
        }
        if ( geometry instanceof QuadMesh mesh ) {
            return exportEdges(mesh);
        }
        if ( geometry instanceof TriangleStripMesh strips ) {
            TriangleMesh mesh = strips.exportToTriangleMesh();
            return mesh != null ? exportEdges(mesh) : null;
        }

        TriangleMeshGroup meshes = GeometryTriangulator.exportToTriangleMeshGroup(geometry);
        if ( meshes == null ) {
            return null;
        }
        Calligraphic3DBuffer lines = new Calligraphic3DBuffer();
        for ( TriangleMesh mesh : meshes.getMeshes() ) {
            addEdges(lines, mesh.getVertexPositions(), mesh.getTriangleIndexes(),
                mesh.getNumTriangles(), 3);
        }
        return lines;
    }

    /**
    @param plane
    @param near point of interest, in the coordinates of the plane (i.e. the
    point viewed by the camera), near which the square shown is centered
    @return the lines of a square of side `INFINITE_PLANE_SIZE` of the plane,
    divided into `INFINITE_PLANE_TILES` x `INFINITE_PLANE_TILES` cells (see
    `InfinitePlane.exportToQuadMesh`), or null for a degenerated plane
    */
    public static Calligraphic3DBuffer exportToCalligraphic3DBuffer(
        InfinitePlane plane, Vector3Dd near)
    {
        QuadMesh mesh = plane.exportToQuadMesh(near, INFINITE_PLANE_SIZE,
            INFINITE_PLANE_TILES);
        return mesh != null ? exportEdges(mesh) : null;
    }

    /**
    @param model
    @param frame index of a key frame of the model
    @return the edges of the triangles of the frame (see
    `Md2Mesh.exportToTriangleMesh`), or null if the model has no such frame
    */
    public static Calligraphic3DBuffer exportToCalligraphic3DBuffer(
        Md2Mesh model, int frame)
    {
        TriangleMesh mesh = model.exportToTriangleMesh(frame);
        return mesh != null ? exportEdges(mesh) : null;
    }

    /**
    @param mesh
    @return the edges of the quadrilaterals of the mesh, each one once
    */
    public static Calligraphic3DBuffer exportEdges(QuadMesh mesh)
    {
        Calligraphic3DBuffer lines = new Calligraphic3DBuffer();

        addEdges(lines, mesh.getVertexPositions(), mesh.getQuadIndices(),
            mesh.getNumQuads(), 4);
        return lines;
    }

    /**
    @param mesh
    @return the edges of the triangles of the mesh, each one once
    */
    public static Calligraphic3DBuffer exportEdges(TriangleMesh mesh)
    {
        Calligraphic3DBuffer lines = new Calligraphic3DBuffer();

        addEdges(lines, mesh.getVertexPositions(), mesh.getTriangleIndexes(),
            mesh.getNumTriangles(), 3);
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
    private static void addEdges(Calligraphic3DBuffer lines, double[] positions,
                                 int[] faces, int numFaces, int sides)
    {
        if ( positions == null || faces == null ) {
            return;
        }
        long numVertices = positions.length / 3;
        HashSet<Long> added = new HashSet<Long>();
        int f;
        int k;

        for ( f = 0; f < numFaces && sides*f + sides <= faces.length; f++ ) {
            for ( k = 0; k < sides; k++ ) {
                int a = faces[sides*f + k];
                int b = faces[sides*f + (k + 1) % sides];
                if ( a == b || a < 0 || b < 0 || a >= numVertices ||
                     b >= numVertices ) {
                    continue;
                }
                long key = Math.min(a, b) * numVertices + Math.max(a, b);
                if ( !added.add(key) ) {
                    continue;
                }
                lines.add3DLine(positions[3*a], positions[3*a + 1],
                    positions[3*a + 2], positions[3*b], positions[3*b + 1],
                    positions[3*b + 2]);
            }
        }
    }
}

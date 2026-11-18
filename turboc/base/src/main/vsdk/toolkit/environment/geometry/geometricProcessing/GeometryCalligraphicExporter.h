#ifndef __GEOMETRY_CALLIGRAPHIC_EXPRT__
#define __GEOMETRY_CALLIGRAPHIC_EXPRT__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Calligraphic3DBuffer;
class Geometry;
class InfinitePlane;
class Md2Mesh;
class QuadMesh;
class TriangleMesh;

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
boundary representation (see `Volume::exportToPolyhedralBoundedSolid`).

C++ port note: every exported buffer is new, owned by the caller.
*/
class GeometryCalligraphicExporter {
public:
    /// Steps along u and v of the mesh of a subdivided Bezier patch, as in
    /// its OpenGL presentation
    enum { BEZIER_PATCH_STEPS = 24 };
    /// Side of the square shown of an infinite plane, as in its OpenGL
    /// presentation
    static const double INFINITE_PLANE_SIZE;
    /// Cells along each side of the square shown of an infinite plane
    enum { INFINITE_PLANE_TILES = 10 };

private:
    GeometryCalligraphicExporter();

    /**
    Adds the edges of the faces of a mesh, skipping the ones already added
    (by the vertex indexes of their ends, in any order).
    @param lines buffer receiving the edges
    @param positions x, y, z of each vertex
    @param faces indexes of the vertices of each face, `sides` per face
    @param numFaces number of faces
    @param sides number of vertices of each face
    */
    static void addEdges(Calligraphic3DBuffer* lines,
        const ArrayList<double>& positions, const ArrayList<int>& faces,
        int numFaces, int sides);

public:
    /**
    @param geometry a curve or a surface
    @return the lines of the geometry, in its own coordinates, or null if it
    is not a curve, nor a surface that can be exported to a mesh
    */
    static Calligraphic3DBuffer* exportToCalligraphic3DBuffer(Geometry* geometry);

    /**
    @param plane
    @param nearPoint point of interest, in the coordinates of the plane (i.e. the
    point viewed by the camera), near which the square shown is centered
    @return the lines of a square of side `INFINITE_PLANE_SIZE` of the plane,
    divided into `INFINITE_PLANE_TILES` x `INFINITE_PLANE_TILES` cells (see
    `InfinitePlane::exportToQuadMesh`), or null for a degenerated plane
    */
    static Calligraphic3DBuffer* exportToCalligraphic3DBuffer(
        const InfinitePlane& plane, const Vector3Dd& nearPoint);

    /**
    @param model
    @param frame index of a key frame of the model
    @return the edges of the triangles of the frame (see
    `Md2Mesh::exportToTriangleMesh`), or null if the model has no such frame
    */
    static Calligraphic3DBuffer* exportToCalligraphic3DBuffer(
        const Md2Mesh& model, int frame);

    /**
    @param mesh
    @return the edges of the quadrilaterals of the mesh, each one once
    */
    static Calligraphic3DBuffer* exportEdges(QuadMesh& mesh);

    /**
    @param mesh
    @return the edges of the triangles of the mesh, each one once
    */
    static Calligraphic3DBuffer* exportEdges(TriangleMesh& mesh);
};

#endif

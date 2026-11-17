#ifndef __OPEN_GL_4_POLYHEDRAL_BOUNDED_SOLID_DEBUG_RENDERER__
#define __OPEN_GL_4_POLYHEDRAL_BOUNDED_SOLID_DEBUG_RENDERER__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Camera;
class PolyhedralBoundedSolid;
class RendererConfiguration;

/**
Debugging overlays of a `PolyhedralBoundedSolid`: wires (with the debug
colors of the edges), points (with the debug colors of the vertices),
normals, bounding volume, selection corners, highlighted degenerate faces,
the oriented boundaries of the faces, a face filled in red, and the edges
classified by visibility with their quantitative invisibility samples.

C++ counterpart of Java's `Jogl4PolyhedralBoundedSolidDebugRenderer`. C++
port note: the OpenGL context is the current one (there is no `gl`
parameter).
*/
class OpenGL4PolyhedralBoundedSolidDebugRenderer {
public:
    static void drawDebugOverlays(PolyhedralBoundedSolid* solid,
                                  Camera* camera,
                                  const RendererConfiguration* quality,
                                  const Matrix4x4d& modelViewProjection);

    /**
    @param faceIndex index of the face, or -1 for all the faces
    */
    static void drawDebugFaceBoundary(PolyhedralBoundedSolid* solid,
                                      int faceIndex,
                                      const Matrix4x4d& modelViewProjection);

    static void drawDebugFace(PolyhedralBoundedSolid* solid, int faceIndex,
                              const Matrix4x4d& modelMatrix,
                              const Matrix4x4d& modelViewProjection,
                              Camera* camera);

    /**
    @param camera camera used to classify the edges by visibility, or null
    @param edgeIndex index of the edge, or -1 for all the edges
    */
    static void drawDebugEdges(PolyhedralBoundedSolid* solid, Camera* camera,
                               int edgeIndex,
                               const Matrix4x4d& modelViewProjection);

private:
    OpenGL4PolyhedralBoundedSolidDebugRenderer() {}
};

#endif

#ifndef __OPEN_GL_1_POLYHEDRAL_BOUNDED_SOLID_RENDERER__
#define __OPEN_GL_1_POLYHEDRAL_BOUNDED_SOLID_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Camera;
class Light;
class PolyhedralBoundedSolid;
class RendererConfiguration;
class SimpleMaterial;

/**
Draws a `PolyhedralBoundedSolid` (boundary representation of [MANT1988])
with the OpenGL 1.2 pipeline: its faces (planar polygons, maybe with holes)
are tessellated with the GLU tessellator at each frame, as
`Jogl2PolyhedralBoundedSolidRenderer` does in Java, and drawn by
`OpenGL1MeshRenderer` with the fixed function state of the shading type
(smooth shading averages the normals of the faces sharing each vertex within
the smoothing threshold of the configuration).

OpenGL 1.2 (fixed function) counterpart of Java's
`Jogl4PolyhedralBoundedSolidRenderer` (as used by `Jogl4GeometryRenderer`),
mirroring `OpenGL4PolyhedralBoundedSolidRenderer`. As there, textures and
normal maps are not supported; the debugging overlays of the GL4 renderer
are not available.
*/
class OpenGL1PolyhedralBoundedSolidRenderer {
public:
    /**
    Draws a brep with the given material and lights.

    @param solid solid to draw
    @param camera camera that views the solid
    @param lights lights of the scene, or null for none
    @param material material of the solid
    @param quality bits of rendering configuration
    @param modelMatrix transformation from solid space to world space
    */
    static void draw(PolyhedralBoundedSolid* solid, Camera* camera,
                     const java::ArrayList<Light*>* lights,
                     const SimpleMaterial* material,
                     const RendererConfiguration* quality,
                     const Matrix4x4d& modelMatrix);

private:
    OpenGL1PolyhedralBoundedSolidRenderer();
};

#endif

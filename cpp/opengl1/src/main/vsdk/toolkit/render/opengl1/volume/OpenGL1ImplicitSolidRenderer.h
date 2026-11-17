#ifndef __OPEN_GL_1_IMPLICIT_SOLID_RENDERER__
#define __OPEN_GL_1_IMPLICIT_SOLID_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class ImplicitSolid;
class Light;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class Vector3Dd;

/**
Renders an `ImplicitSolid` (i.e. a `BlobSolid` or a `PolynomialSurface`)
with the OpenGL 1.2 pipeline, from the `SolidBoundaryTessellator` mesh of its bounds
(see `_OpenGL1SolidBoundaryMeshBuilder`), cached by `OpenGL1GeometryRenderer`.
Blobs and polynomial surfaces are identified by their parameters (so editing
them produces a new mesh), other implicit solids by identity.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4ImplicitSolidRenderer`, mirroring
`OpenGL1ImplicitSolidRenderer`.
*/
class OpenGL1ImplicitSolidRenderer {
public:
    /**
    Draws the solid, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param solid solid to draw
    @param camera camera that views the solid
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the solid
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from solid space to world space
    */
    static void draw(ImplicitSolid* solid, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param solid a solid
    @return a string that identifies the mesh of the solid
    */
    static java::String meshKey(ImplicitSolid& solid);

    /**
    @param solid solid to tessellate
    @return a new mesh (owned by the caller), or null if the solid can not
    be tessellated
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(ImplicitSolid& solid);

private:
    OpenGL1ImplicitSolidRenderer();

    /** Exact text of the coordinates, as `Vector3Dd` printing rounds them */
    static java::String keyOf(const Vector3Dd& v);
};

#endif

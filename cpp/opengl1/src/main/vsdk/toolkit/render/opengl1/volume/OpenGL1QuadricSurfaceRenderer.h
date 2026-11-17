#ifndef __OPEN_GL_1_QUADRIC_SURFACE_RENDERER__
#define __OPEN_GL_1_QUADRIC_SURFACE_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class Light;
class QuadricSurface;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class Vector3Dd;

/**
Renders a `QuadricSurface` with the OpenGL 1.2 pipeline, from the
`SolidBoundaryTessellator` mesh of its bounds (see
`_OpenGL1SolidBoundaryMeshBuilder`), cached by `OpenGL1GeometryRenderer`,
identified by the coefficients of the quadric.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4QuadricSurfaceRenderer`, mirroring
`OpenGL1QuadricSurfaceRenderer`.
*/
class OpenGL1QuadricSurfaceRenderer {
public:
    /**
    Draws the quadric, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param quadric quadric to draw
    @param camera camera that views the quadric
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the quadric
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from quadric space to world space
    */
    static void draw(QuadricSurface* quadric, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param quadric a quadric
    @return a string that identifies the mesh of the quadric
    */
    static java::String meshKey(QuadricSurface& quadric);

    /**
    @param quadric quadric to tessellate
    @return a new mesh (owned by the caller), or null if the quadric can not
    be tessellated
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(QuadricSurface& quadric);

private:
    OpenGL1QuadricSurfaceRenderer();

    /** Exact text of the coordinates, as `Vector3Dd` printing rounds them */
    static java::String keyOf(const Vector3Dd& v);
};

#endif

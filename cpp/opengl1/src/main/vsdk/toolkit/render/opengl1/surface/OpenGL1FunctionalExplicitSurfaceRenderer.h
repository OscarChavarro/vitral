#ifndef __OPEN_GL_1_FUNCTIONAL_EXPLICIT_SURFACE_RENDERER__
#define __OPEN_GL_1_FUNCTIONAL_EXPLICIT_SURFACE_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class FunctionalExplicitSurface;
class Light;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;

/**
Renders a `FunctionalExplicitSurface` (height field z = f(x, y)) with the
OpenGL 1.2 pipeline, from its internal triangle mesh, visible from both sides, with
texture coordinates spanning its (x, y) bounds. The mesh is cached by
`OpenGL1GeometryRenderer`.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4FunctionalExplicitSurfaceRenderer`, mirroring
`OpenGL1FunctionalExplicitSurfaceRenderer`.
*/
class OpenGL1FunctionalExplicitSurfaceRenderer {
public:
    /**
    Draws the surface, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param surface surface to draw
    @param camera camera that views the surface
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the surface
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from surface space to world space
    */
    static void draw(FunctionalExplicitSurface* surface, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param surface a surface
    @return a string that identifies the mesh of the surface
    */
    static java::String meshKey(FunctionalExplicitSurface& surface);

    /**
    @param surface surface to tessellate
    @return a new mesh (owned by the caller), or null if the surface can not
    be tessellated
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(FunctionalExplicitSurface& surface);

private:
    OpenGL1FunctionalExplicitSurfaceRenderer();
};

#endif

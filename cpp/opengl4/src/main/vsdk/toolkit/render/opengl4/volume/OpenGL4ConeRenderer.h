#ifndef __OPEN_GL_4_CONE_RENDERER__
#define __OPEN_GL_4_CONE_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Camera;
class Cone;
class Light;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;

/**
Renders a `Cone` (also cylinders and truncated cones) with the GL4
pipeline: it is tessellated by `buildMesh` into its side and its caps, and
the mesh is cached by `OpenGL4GeometryRenderer`, identified by the parameters
of the cone.

C++ counterpart of Java's `Jogl4ConeRenderer`.
*/
class OpenGL4ConeRenderer {
public:
    /**
    Draws the cone, with the passes selected by the configuration (see
    `OpenGL4MeshRenderer`).

    @param cone cone to draw
    @param camera camera that views the cone
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the cone
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from cone space to world space
    */
    static void draw(Cone* cone, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param cone a cone
    @return a string that identifies the mesh of the cone
    */
    static java::String meshKey(const Cone& cone);

    /**
    @param cone cone to tessellate
    @return a new mesh (owned by the caller), or null if the cone can not
    be tessellated
    */
    static OpenGL4MeshRenderer::Mesh* buildMesh(const Cone& cone);

    /// Divisions around the axis
    static const int SLICES = 32;

private:
    OpenGL4ConeRenderer();
};

#endif

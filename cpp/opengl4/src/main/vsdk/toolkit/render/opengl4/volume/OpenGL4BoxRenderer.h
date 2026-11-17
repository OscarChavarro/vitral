#ifndef __OPEN_GL_4_BOX_RENDERER__
#define __OPEN_GL_4_BOX_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Box;
class Camera;
class Light;
class OpenGL4MeshBuilder;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class Vector3Dd;

/**
Renders a `Box` with the GL4 pipeline: it is tessellated by `buildMesh`
into its six faces, with the texture layout of `Jogl2BoxRenderer`, and the
mesh is cached by `OpenGL4GeometryRenderer`, identified by the size of the
box.

C++ counterpart of Java's `Jogl4BoxRenderer`.
*/
class OpenGL4BoxRenderer {
public:
    /**
    Draws the box, with the passes selected by the configuration (see
    `OpenGL4MeshRenderer`).

    @param box box to draw
    @param camera camera that views the box
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the box
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from box space to world space
    */
    static void draw(Box* box, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param box a box
    @return a string that identifies the mesh of the box
    */
    static java::String meshKey(const Box& box);

    /**
    @param box box to tessellate
    @return a new mesh (owned by the caller), or null if the box can not
    be tessellated
    */
    static OpenGL4MeshRenderer::Mesh* buildMesh(const Box& box);

private:
    OpenGL4BoxRenderer();

    /**
    Adds a face whose vertices are given in the order upper left, lower left,
    lower right, upper right as seen from outside (the Jogl2 texture layout),
    and which is wound so its front side looks outside.
    */
    static void addFace(OpenGL4MeshBuilder& builder, const Vector3Dd& n,
        const Vector3Dd& a, const Vector3Dd& b, const Vector3Dd& c,
        const Vector3Dd& d);
};

#endif

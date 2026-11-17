#ifndef __OPEN_GL_4_TRIANGLE_MESH_GROUP_RENDERER__
#define __OPEN_GL_4_TRIANGLE_MESH_GROUP_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Camera;
class Light;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class TriangleMeshGroup;

/**
Renders a `TriangleMeshGroup` with the GL4 pipeline. OBJ imports are
represented as a group of meshes (normally one per material). Raytracing
already traverses this geometry, so rasterizing every member here (each one
with `OpenGL4TriangleMeshRenderer`) keeps the two rendering paths equivalent.
The bounding volume and the selection corners are drawn once, around the
whole group.

C++ counterpart of Java's `Jogl4TriangleMeshGroupRenderer`.
*/
class OpenGL4TriangleMeshGroupRenderer {
public:
    /**
    Draws the group, with the passes selected by the configuration (see
    `OpenGL4MeshRenderer`).

    @param group group to draw
    @param camera camera that views the group
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the group
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from group space to world space
    */
    static void draw(TriangleMeshGroup* group, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

private:
    OpenGL4TriangleMeshGroupRenderer();
};

#endif

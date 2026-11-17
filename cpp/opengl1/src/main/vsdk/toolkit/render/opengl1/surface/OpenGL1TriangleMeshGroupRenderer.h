#ifndef __OPEN_GL_1_TRIANGLE_MESH_GROUP_RENDERER__
#define __OPEN_GL_1_TRIANGLE_MESH_GROUP_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class Light;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class TriangleMeshGroup;

/**
Renders a `TriangleMeshGroup` with the OpenGL 1.2 pipeline. OBJ imports are
represented as a group of meshes (normally one per material). Raytracing
already traverses this geometry, so rasterizing every member here (each one
with `OpenGL1TriangleMeshRenderer`) keeps the two rendering paths equivalent.
The bounding volume and the selection corners are drawn once, around the
whole group.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4TriangleMeshGroupRenderer`, mirroring
`OpenGL1TriangleMeshGroupRenderer`.
*/
class OpenGL1TriangleMeshGroupRenderer {
public:
    /**
    Draws the group, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

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
    OpenGL1TriangleMeshGroupRenderer();
};

#endif

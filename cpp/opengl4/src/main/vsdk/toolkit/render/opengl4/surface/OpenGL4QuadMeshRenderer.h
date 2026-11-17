#ifndef __OPEN_GL_4_QUAD_MESH_RENDERER__
#define __OPEN_GL_4_QUAD_MESH_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Camera;
class Light;
class QuadMesh;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;

/**
Renders a `QuadMesh` with the GL4 pipeline: its quads are copied by
`buildMesh` (as two triangles each), visible from both sides, with the
normals of its vertices or, when it has none, the normal of each quad. The
mesh is cached by `OpenGL4GeometryRenderer`, identified by the quad mesh, as
they are not edited while shown.

C++ counterpart of Java's `Jogl4QuadMeshRenderer`.
*/
class OpenGL4QuadMeshRenderer {
public:
    /**
    Draws the mesh, with the passes selected by the configuration (see
    `OpenGL4MeshRenderer`).

    @param mesh mesh to draw
    @param camera camera that views the mesh
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the mesh
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from mesh space to world space
    */
    static void draw(QuadMesh* mesh, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param mesh a mesh
    @return a string that identifies the mesh of the mesh
    */
    static java::String meshKey(QuadMesh& mesh);

    /**
    @param mesh mesh to tessellate
    @return a new mesh (owned by the caller), or null if the mesh can not
    be tessellated
    */
    static OpenGL4MeshRenderer::Mesh* buildMesh(QuadMesh& mesh);

private:
    OpenGL4QuadMeshRenderer();
};

#endif

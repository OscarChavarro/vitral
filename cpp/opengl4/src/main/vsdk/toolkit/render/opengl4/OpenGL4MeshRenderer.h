#ifndef __OPEN_GL_4_MESH_RENDERER__
#define __OPEN_GL_4_MESH_RENDERER__

#include <vector>

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Camera;
class Geometry;
class Light;
class RendererConfiguration;
class RGBImageUncompressed;
class SimpleMaterial;

/**
Renders any triangle mesh (given as non indexed arrays of positions, normals,
texture coordinates, tangents and binormals) honoring every bit of a
`RendererConfiguration` (surfaces, wires, points, normals, bounding volume,
texture, bump map and shading type), with the GLSL programs selected by
`OpenGL4RendererConfigurationShaderSelector`. It is the common flow shared by
the renderers of each geometry (see `OpenGL4GeometryRenderer`).

The OpenGL resources of a mesh are created the first time it is drawn, and
must be released with `release` while the context that created them is
current.

C++ counterpart of Java's `Jogl4MeshRenderer`.
*/
class OpenGL4MeshRenderer {
public:
    /// Size of the light arrays of the GLSL programs
    static const int MAX_LIGHTS = 8;

    /**
    A triangle mesh and the OpenGL objects that hold it in the GPU.
    */
    class Mesh {
    public:
        /// x,y,z of each vertex; three consecutive vertices make a triangle
        std::vector<float> positions;
        std::vector<float> normals;
        std::vector<float> uvs;
        std::vector<float> tangents;
        std::vector<float> biNormals;
        int vertexCount;
        /// Vertices of the sides the geometry defines; the rest (if any) are
        /// back sides added to see open surfaces from both sides
        int frontVertexCount;
        /// Approximate size of the mesh, used to scale the normal overlays
        double characteristicSize;

        unsigned int vaoId;
        unsigned int vboIds[5];
        bool uploaded;
        java::ArrayList<float> vertexNormalLinePositions;
        java::ArrayList<float> vertexNormalLineColors;
        java::ArrayList<float> triangleNormalLinePositions;
        java::ArrayList<float> triangleNormalLineColors;

        explicit Mesh(double characteristicSize);

        /**
        Marks the vertices after the first `count` ones as back sides of open
        surfaces: they are drawn as surfaces and wires, but their points and
        normals are not shown again.
        */
        void setFrontVertexCount(int count);
    };

    /**
    Draws a mesh, with the passes selected by the configuration.
    @param geometry geometry the mesh comes from, used for the bounding volume
    @param lights lights of the scene (at most `MAX_LIGHTS` are used), or null
    or empty to use a light at the camera
    @param material material of the surfaces, or null for the default one
    @param textureMap texture of the surfaces, or null
    @param normalMap normal (bump) map of the surfaces, or null
    */
    static void draw(Mesh* mesh, Geometry* geometry, Camera* camera,
                     const java::ArrayList<Light*>* lights,
                     const SimpleMaterial* material,
                     const RendererConfiguration* quality,
                     RGBImageUncompressed* textureMap,
                     RGBImageUncompressed* normalMap,
                     const Matrix4x4d& localTransform);

    /**
    Releases the OpenGL objects of a mesh. PRE: the context that created them
    is current.
    */
    static void release(Mesh* mesh);

    /**
    Releases the resources shared by all the meshes.
    */
    static void dispose();

private:
    static unsigned int dummyTextureId;
    static bool tooManyLightsReported;

    static unsigned int ensureDummyTexture();
    static void upload(Mesh* mesh);
    static void renderMesh(Mesh* mesh);
    static void configureProgram(unsigned int programId,
                                 const Matrix4x4d& modelViewProjection,
                                 const Matrix4x4d& modelViewLocal,
                                 const Matrix4x4d& modelViewITLocal,
                                 Camera* camera,
                                 const java::ArrayList<Light*>& lights,
                                 const SimpleMaterial& material,
                                 const RendererConfiguration* quality,
                                 int textureId, int normalMapId);
    static void drawNormalOverlays(Mesh* mesh,
                                   const RendererConfiguration* quality,
                                   const Matrix4x4d& modelViewProjection);

    OpenGL4MeshRenderer() {}
};

#endif

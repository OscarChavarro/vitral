#ifndef __OPEN_GL_1_MESH_RENDERER__
#define __OPEN_GL_1_MESH_RENDERER__

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
Renders any triangle mesh (given as non indexed arrays of positions, normals
and texture coordinates) honoring the bits of a `RendererConfiguration`
(surfaces, wires, points, normals, bounding volume, texture and shading
type), with the fixed function state selected by
`OpenGL1RendererConfigurationStateSelector`. It is the common flow shared by
the renderers of each geometry (see `OpenGL1GeometryRenderer`). Bump maps,
Phong and Cook-Torrance shading are not available in OpenGL 1.2.

The vertices go to OpenGL as client side vertex arrays, compiled in a
display list the first time a mesh is drawn; the list must be released with
`release` while the context that created it is current.

OpenGL 1.2 counterpart of `OpenGL4MeshRenderer`.
*/
class OpenGL1MeshRenderer {
public:
    /// Number of lights of the fixed function pipeline
    static const int MAX_LIGHTS = 8;

    /**
    A triangle mesh and the display list that draws it.
    */
    class Mesh {
    public:
        /// x,y,z of each vertex; three consecutive vertices make a triangle
        std::vector<float> positions;
        std::vector<float> normals;
        std::vector<float> uvs;
        int vertexCount;
        /// Vertices of the sides the geometry defines; the rest (if any) are
        /// back sides added to see open surfaces from both sides
        int frontVertexCount;
        /// Approximate size of the mesh, used to scale the normal overlays
        double characteristicSize;

        unsigned int displayListId;
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
    @param normalMap normal (bump) map of the surfaces, ignored: bump mapping
    is not available in OpenGL 1.2
    */
    static void draw(Mesh* mesh, Geometry* geometry, Camera* camera,
                     const java::ArrayList<Light*>* lights,
                     const SimpleMaterial* material,
                     const RendererConfiguration* quality,
                     RGBImageUncompressed* textureMap,
                     RGBImageUncompressed* normalMap,
                     const Matrix4x4d& localTransform);

    /**
    Releases the display list of a mesh. PRE: the context that created it
    is current.
    */
    static void release(Mesh* mesh);

    /**
    Releases the resources shared by all the meshes.
    */
    static void dispose();

private:
    static void upload(Mesh* mesh);
    static void renderMesh(Mesh* mesh);
    static void enableArrays(Mesh* mesh);
    static void disableArrays();
    static void drawNormalOverlays(Mesh* mesh,
                                   const RendererConfiguration* quality,
                                   const Matrix4x4d& modelViewProjection);

    OpenGL1MeshRenderer() {}
};

#endif

#ifndef __OPEN_GL_1_TUBE_RENDERER__
#define __OPEN_GL_1_TUBE_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class Light;
class OpenGL1MeshBuilder;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class Tube;

/**
Renders a `Tube` with the OpenGL 1.2 pipeline: it is tessellated by `buildMesh` into
its external wall, its internal wall (with normals towards the axis) and its
annular caps, and the mesh is cached by `OpenGL1GeometryRenderer`, identified by
the parameters of the tube. A washer (height 0) is a double sided annulus.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4TubeRenderer`, mirroring
`OpenGL4TubeRenderer`.
*/
class OpenGL1TubeRenderer {
public:
    /// Divisions around the axis
    static const int SLICES = 32;

    /**
    Draws the tube, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param tube tube to draw
    @param camera camera that views the tube
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the tube
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from tube space to world space
    */
    static void draw(Tube* tube, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param tube a tube
    @return a string that identifies the mesh of the tube
    */
    static java::String meshKey(const Tube& tube);

    /**
    @param tube tube to tessellate
    @return a new mesh (owned by the caller) of the walls and the caps of the
    tube
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(const Tube& tube);

private:
    OpenGL1TubeRenderer();

    /**
    The internal wall faces the axis: its quads go clockwise seen from
    outside the hole, with normals pointing to the axis.
    */
    static void addInternalWall(OpenGL1MeshBuilder& builder, double r,
        double h);
};

#endif

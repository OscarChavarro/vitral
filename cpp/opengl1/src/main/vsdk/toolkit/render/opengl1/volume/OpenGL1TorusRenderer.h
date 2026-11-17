#ifndef __OPEN_GL_1_TORUS_RENDERER__
#define __OPEN_GL_1_TORUS_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class Light;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class Torus;
class Vector3Dd;

/**
Renders a `Torus` with the OpenGL 1.2 pipeline: it is tessellated by `buildMesh`
into a grid of quads along its two angles, and the mesh is cached by
`OpenGL1GeometryRenderer`, identified by the radii of the torus.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4TorusRenderer`, mirroring
`OpenGL1TorusRenderer`.
*/
class OpenGL1TorusRenderer {
public:
    /**
    Draws the torus, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param torus torus to draw
    @param camera camera that views the torus
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the torus
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from torus space to world space
    */
    static void draw(Torus* torus, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param torus a torus
    @return a string that identifies the mesh of the torus
    */
    static java::String meshKey(const Torus& torus);

    /**
    @param torus torus to tessellate
    @return a new mesh (owned by the caller), or null if the torus can not
    be tessellated
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(const Torus& torus);

    /// Divisions along the major circle
    static const int MAJOR_SEGMENTS = 48;
    /// Divisions along the minor circle
    static const int MINOR_SEGMENTS = 24;

private:
    OpenGL1TorusRenderer();
    static Vector3Dd point(double bigR, double smallR, double w, double v);
    static Vector3Dd normal(double w, double v);
};

#endif

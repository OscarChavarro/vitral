#ifndef __OPEN_GL_4_PLANAR_HALF_SPACE_RENDERER__
#define __OPEN_GL_4_PLANAR_HALF_SPACE_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Camera;
class InfinitePlane;
class Light;
class PlanarHalfSpace;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;

/**
Renders a `PlanarHalfSpace` with the GL4 pipeline. The half space is
unbounded: it is shown by its boundary plane, as the square that
`OpenGL4InfinitePlaneRenderer` shows for it.

C++ counterpart of Java's `Jogl4PlanarHalfSpaceRenderer`.
*/
class OpenGL4PlanarHalfSpaceRenderer {
public:
    /**
    Draws the half space, with the passes selected by the configuration (see
    `OpenGL4MeshRenderer`).

    @param halfSpace half space to draw
    @param camera camera that views the half space
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the half space
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from half space space to world space
    */
    static void draw(PlanarHalfSpace* halfSpace, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param halfSpace a half space
    @return a new plane (owned by the caller) with the equation of its
    boundary
    */
    static InfinitePlane* boundary(const PlanarHalfSpace& halfSpace);

private:
    OpenGL4PlanarHalfSpaceRenderer();
};

#endif

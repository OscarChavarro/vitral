#ifndef __OPEN_GL_1_PLANAR_HALF_SPACE_RENDERER__
#define __OPEN_GL_1_PLANAR_HALF_SPACE_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class InfinitePlane;
class Light;
class PlanarHalfSpace;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;

/**
Renders a `PlanarHalfSpace` with the OpenGL 1.2 pipeline. The half space is
unbounded: it is shown by its boundary plane, as the square that
`OpenGL1InfinitePlaneRenderer` shows for it.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4PlanarHalfSpaceRenderer`, mirroring
`OpenGL1PlanarHalfSpaceRenderer`.
*/
class OpenGL1PlanarHalfSpaceRenderer {
public:
    /**
    Draws the half space, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

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
    OpenGL1PlanarHalfSpaceRenderer();
};

#endif

#ifndef __OPEN_GL_1_INFINITE_PLANE_RENDERER__
#define __OPEN_GL_1_INFINITE_PLANE_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class InfinitePlane;
class Light;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class Vector3Dd;

/**
Renders an `InfinitePlane` with the OpenGL 1.2 pipeline. As in
`Jogl2InfinitePlaneRenderer`, a finite part of the plane is shown: a square
of `SIZE` units centered at the point of the plane closest to the origin,
divided into `TILES` x `TILES` cells. The square is visible from both sides.
The bounding volume of an infinite plane is infinite, so the bounding volume
and the selection corners are drawn around the square shown.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4InfinitePlaneRenderer`, mirroring
`OpenGL1InfinitePlaneRenderer`.
*/
class OpenGL1InfinitePlaneRenderer {
public:
    /**
    Draws the plane, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param plane plane to draw
    @param camera camera that views the plane
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the plane
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from plane space to world space
    */
    static void draw(InfinitePlane* plane, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param plane a plane
    @return a string that identifies the mesh of the plane
    */
    static java::String meshKey(const InfinitePlane& plane);

    /**
    @param plane plane to tessellate
    @return a new mesh (owned by the caller), or null if the plane can not
    be tessellated
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(const InfinitePlane& plane);

    /// Side of the square of the plane that is shown
    static constexpr double SIZE = 20.0;
    /// Number of cells along each side of the square
    static const int TILES = 10;

    /**
    Draws the mesh of the square shown for a plane. Its bounding volume is
    infinite, so the bounding volume and the selection corners are drawn
    around the square instead.

    @param mesh mesh of the plane, built by `buildMesh`
    @param plane plane to draw
    @param camera camera that views the plane
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the plane
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from plane space to world space
    */
    static void drawMesh(OpenGL1MeshRenderer::Mesh* mesh, InfinitePlane* plane,
        Camera* camera, const java::ArrayList<Light*>* lights,
        const SimpleMaterial* material, const RendererConfiguration* quality,
        RGBImageUncompressed* textureMap, RGBImageUncompressed* normalMap,
        const Matrix4x4d& localTransform);

    /**
    @param plane a plane
    @param outMinMax the bounding box of the square shown for the plane, as
    given by `Geometry::getMinMax()`
    */
    static void calculateShownMinMax(const InfinitePlane& plane,
                                     double outMinMax[6]);

private:
    OpenGL1InfinitePlaneRenderer();

    /**
    @param outFrame the point of the plane closest to the origin, two
    orthonormal directions (u, v) on it and its normal n, with (u, v, n)
    right handed
    */
    static void calculateFrame(const InfinitePlane& plane, Vector3Dd outFrame[4]);
};

#endif

#ifndef __OGL1_PLANAR_HALF_SPACE_RNDR__
#define __OGL1_PLANAR_HALF_SPACE_RNDR__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
class Camera;
class Light;
class PlanarHalfSpace;
class RendererConfiguration;
class RGBImageUncompressed;
class SimpleMaterial;

/**
Draws planar half spaces with OpenGL 1.2 (as `Jogl4PlanarHalfSpaceRenderer`
of the Java OpenGL 4 port): as their boundary plane, a finite square of the
infinite plane.
*/
class OpenGL1PlanarHalfSpaceRenderer {
private:
    OpenGL1PlanarHalfSpaceRenderer();

public:
    static void draw(PlanarHalfSpace* halfSpace, Camera* camera,
        const ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @return the boundary plane of the half space, with the same equation
    */
    static InfinitePlane boundary(const PlanarHalfSpace& halfSpace);
};

#endif

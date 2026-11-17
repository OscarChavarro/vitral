#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1PlanarHalfSpaceRenderer.h"

void OpenGL1PlanarHalfSpaceRenderer::draw(PlanarHalfSpace* halfSpace,
    Camera* camera, const ArrayList<Light*>* lights,
    const SimpleMaterial* material, const RendererConfiguration* quality,
    RGBImageUncompressed* textureMap, RGBImageUncompressed* normalMap,
    const Matrix4x4d& localTransform)
{
    InfinitePlane plane = boundary(*halfSpace);
    OpenGL1GeometryRenderer::draw(&plane, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

InfinitePlane OpenGL1PlanarHalfSpaceRenderer::boundary(
    const PlanarHalfSpace& halfSpace)
{
    return InfinitePlane(halfSpace.getA(), halfSpace.getB(),
        halfSpace.getC(), halfSpace.getD());
}

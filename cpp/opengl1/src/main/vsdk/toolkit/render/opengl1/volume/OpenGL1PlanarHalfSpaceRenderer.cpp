#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1InfinitePlaneRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1PlanarHalfSpaceRenderer.h"

void OpenGL1PlanarHalfSpaceRenderer::draw(PlanarHalfSpace* halfSpace,
    Camera* camera, const java::ArrayList<Light*>* lights,
    const SimpleMaterial* material, const RendererConfiguration* quality,
    RGBImageUncompressed* textureMap, RGBImageUncompressed* normalMap,
    const Matrix4x4d& localTransform)
{
    InfinitePlane* plane = boundary(*halfSpace);
    OpenGL1InfinitePlaneRenderer::draw(plane, camera, lights, material,
        quality, textureMap, normalMap, localTransform);
    delete plane;
}

InfinitePlane* OpenGL1PlanarHalfSpaceRenderer::boundary(
    const PlanarHalfSpace& halfSpace)
{
    return new InfinitePlane(halfSpace.getA(), halfSpace.getB(),
        halfSpace.getC(), halfSpace.getD());
}

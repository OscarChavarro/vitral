#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4InfinitePlaneRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4PlanarHalfSpaceRenderer.h"

void OpenGL4PlanarHalfSpaceRenderer::draw(PlanarHalfSpace* halfSpace,
    Camera* camera, const java::ArrayList<Light*>* lights,
    const SimpleMaterial* material, const RendererConfiguration* quality,
    RGBImageUncompressed* textureMap, RGBImageUncompressed* normalMap,
    const Matrix4x4d& localTransform)
{
    InfinitePlane* plane = boundary(*halfSpace);
    OpenGL4InfinitePlaneRenderer::draw(plane, camera, lights, material,
        quality, textureMap, normalMap, localTransform);
    delete plane;
}

InfinitePlane* OpenGL4PlanarHalfSpaceRenderer::boundary(
    const PlanarHalfSpace& halfSpace)
{
    return new InfinitePlane(halfSpace.getA(), halfSpace.getB(),
        halfSpace.getC(), halfSpace.getD());
}

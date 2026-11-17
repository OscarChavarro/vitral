#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshGroupRenderer.h"

void OpenGL1TriangleMeshGroupRenderer::draw(TriangleMeshGroup* group,
    Camera* camera, const java::ArrayList<Light*>* lights,
    const SimpleMaterial* material, const RendererConfiguration* quality,
    RGBImageUncompressed* textureMap, RGBImageUncompressed* normalMap,
    const Matrix4x4d& localTransform)
{
    if ( quality == nullptr ) {
        return;
    }
    RendererConfiguration meshQuality = quality->clone();
    meshQuality.setBoundingVolume(false);
    meshQuality.setSelectionCorners(false);
    for ( long i = 0; i < group->getMeshes().size(); i++ ) {
        OpenGL1GeometryRenderer::draw(&group->getMeshes()[i], camera, lights,
            material, &meshQuality, textureMap, normalMap, localTransform);
    }
    if ( quality->isBoundingVolumeSet() ) {
        OpenGL1MinMaxRenderer::draw(group, camera, localTransform);
    }
    if ( quality->isSelectionCornersSet() ) {
        OpenGL1SelectionCornersRenderer::draw(group, camera, localTransform);
    }
}

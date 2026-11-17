#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4TriangleMeshGroupRenderer.h"

void OpenGL4TriangleMeshGroupRenderer::draw(TriangleMeshGroup* group,
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
        OpenGL4GeometryRenderer::draw(&group->getMeshes()[i], camera, lights,
            material, &meshQuality, textureMap, normalMap, localTransform);
    }
    if ( quality->isBoundingVolumeSet() ) {
        OpenGL4MinMaxRenderer::draw(group, camera, localTransform);
    }
    if ( quality->isSelectionCornersSet() ) {
        OpenGL4SelectionCornersRenderer::draw(group, camera, localTransform);
    }
}

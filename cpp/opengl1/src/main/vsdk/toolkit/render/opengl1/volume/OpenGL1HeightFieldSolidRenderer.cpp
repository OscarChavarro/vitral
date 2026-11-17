#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/render/opengl1/volume/_OpenGL1SolidBoundaryMeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1HeightFieldSolidRenderer.h"

void OpenGL1HeightFieldSolidRenderer::draw(HeightFieldSolid* solid, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(solid, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

java::String OpenGL1HeightFieldSolidRenderer::meshKey(HeightFieldSolid& solid)
{
    char key[96];
    snprintf(key, sizeof(key), "tessellatedsolid/%p/%d", (void*)&solid,
             _OpenGL1SolidBoundaryMeshBuilder::RESOLUTION);
    return key;
}

OpenGL1MeshRenderer::Mesh* OpenGL1HeightFieldSolidRenderer::buildMesh(
    HeightFieldSolid& solid)
{
    return _OpenGL1SolidBoundaryMeshBuilder::buildMesh(&solid);
}

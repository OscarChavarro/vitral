#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/render/opengl4/volume/_OpenGL4SolidBoundaryMeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4HeightFieldSolidRenderer.h"

void OpenGL4HeightFieldSolidRenderer::draw(HeightFieldSolid* solid, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL4GeometryRenderer::draw(solid, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

java::String OpenGL4HeightFieldSolidRenderer::meshKey(HeightFieldSolid& solid)
{
    char key[96];
    snprintf(key, sizeof(key), "tessellatedsolid/%p/%d", (void*)&solid,
             _OpenGL4SolidBoundaryMeshBuilder::RESOLUTION);
    return key;
}

OpenGL4MeshRenderer::Mesh* OpenGL4HeightFieldSolidRenderer::buildMesh(
    HeightFieldSolid& solid)
{
    return _OpenGL4SolidBoundaryMeshBuilder::buildMesh(&solid);
}

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/processing/BezierPatchTessellator.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1SubdividedBezierPatchRenderer.h"

void OpenGL1SubdividedBezierPatchRenderer::draw(SubdividedBezierPatch* patch, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL1GeometryRenderer::draw(patch, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

java::String OpenGL1SubdividedBezierPatchRenderer::meshKey(
    const SubdividedBezierPatch& patch)
{
    char key[96];
    snprintf(key, sizeof(key), "subdividedbezierpatch/%p/%d",
             (const void*)&patch, TESSELLATION_STEPS);
    return key;
}

OpenGL1MeshRenderer::Mesh* OpenGL1SubdividedBezierPatchRenderer::buildMesh(
    const SubdividedBezierPatch& patch)
{
    TriangleMesh* mesh = BezierPatchTessellator::tessellate(patch,
        TESSELLATION_STEPS, TESSELLATION_STEPS);
    if ( mesh == nullptr ) {
        return nullptr;
    }
    OpenGL1MeshRenderer::Mesh* result =
        OpenGL1TriangleMeshRenderer::buildMesh(*mesh, true);
    delete mesh;
    return result;
}

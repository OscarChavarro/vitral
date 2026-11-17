#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/processing/BezierPatchTessellator.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4TriangleMeshRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/surface/OpenGL4SubdividedBezierPatchRenderer.h"

void OpenGL4SubdividedBezierPatchRenderer::draw(SubdividedBezierPatch* patch, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform)
{
    OpenGL4GeometryRenderer::draw(patch, camera, lights, material, quality,
        textureMap, normalMap, localTransform);
}

java::String OpenGL4SubdividedBezierPatchRenderer::meshKey(
    const SubdividedBezierPatch& patch)
{
    char key[96];
    snprintf(key, sizeof(key), "subdividedbezierpatch/%p/%d",
             (const void*)&patch, TESSELLATION_STEPS);
    return key;
}

OpenGL4MeshRenderer::Mesh* OpenGL4SubdividedBezierPatchRenderer::buildMesh(
    const SubdividedBezierPatch& patch)
{
    TriangleMesh* mesh = BezierPatchTessellator::tessellate(patch,
        TESSELLATION_STEPS, TESSELLATION_STEPS);
    if ( mesh == nullptr ) {
        return nullptr;
    }
    OpenGL4MeshRenderer::Mesh* result =
        OpenGL4TriangleMeshRenderer::buildMesh(*mesh, true);
    delete mesh;
    return result;
}

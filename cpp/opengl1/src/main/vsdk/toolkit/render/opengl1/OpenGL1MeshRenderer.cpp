#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LineRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1SelectionCornersRenderer.h"

namespace {

const float SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
const float SURFACE_POLYGON_OFFSET_UNITS = 1.0f;
const float LINE_POLYGON_OFFSET_FACTOR = -1.0f;
const float LINE_POLYGON_OFFSET_UNITS = -1.0f;
const float VERTEX_NORMAL_SCALE = 0.10f;
const float TRIANGLE_NORMAL_SCALE = 0.12f;
const float NORMAL_START_EPSILON = 0.002f;
const float NORMAL_LINE_DEPTH_BIAS_NDC = -1.0e-4f;
const float VERTEX_NORMAL_COLOR[3] = { 1.0f, 1.0f, 0.0f };
const float TRIANGLE_NORMAL_COLOR[3] = { 0.0f, 1.0f, 1.0f };

void addUniformColors(java::ArrayList<float>& colors, long vertexCount,
                      const float color[3])
{
    colors.reserve(vertexCount * 3);
    for ( long i = 0; i < vertexCount; i++ ) {
        colors.add(color[0]);
        colors.add(color[1]);
        colors.add(color[2]);
    }
}

void addLine(java::ArrayList<float>& lines, float sx, float sy, float sz,
             float nx, float ny, float nz, float length)
{
    lines.add(sx);
    lines.add(sy);
    lines.add(sz);
    lines.add(sx + nx * length);
    lines.add(sy + ny * length);
    lines.add(sz + nz * length);
}

}

OpenGL1MeshRenderer::Mesh::Mesh(double characteristicSize)
    : vertexCount(0), frontVertexCount(0),
      characteristicSize(characteristicSize), displayListId(0), uploaded(false)
{
}

void OpenGL1MeshRenderer::Mesh::setFrontVertexCount(int count)
{
    frontVertexCount = count < 0 ? 0 : (count > vertexCount ? vertexCount : count);
}

void OpenGL1MeshRenderer::draw(Mesh* mesh, Geometry* geometry, Camera* camera,
                               const java::ArrayList<Light*>* lights,
                               const SimpleMaterial* material,
                               const RendererConfiguration* quality,
                               RGBImageUncompressed* textureMap,
                               RGBImageUncompressed* normalMap,
                               const Matrix4x4d& localTransform)
{
    if ( mesh == nullptr || camera == nullptr || quality == nullptr ) {
        return;
    }
    const SimpleMaterial defaultMaterial;
    const SimpleMaterial& surfaceMaterial =
        material != nullptr ? *material : defaultMaterial;

    // Without lights, a light at the camera
    java::ArrayList<Light*> activeLights;
    PointLight cameraLight(camera->getPosition(), ColorRgb(1, 1, 1));
    if ( lights != nullptr ) {
        for ( long i = 0; i < lights->size(); i++ ) {
            if ( (*lights)[i] != nullptr ) {
                activeLights.add((*lights)[i]);
            }
        }
    }
    if ( activeLights.size() == 0 ) {
        activeLights.add(&cameraLight);
    }

    upload(mesh);
    bool hasTexture = textureMap != nullptr;
    int textureId = hasTexture ? OpenGL1ImageRenderer::activate(textureMap) : 0;
    // Bump mapping is not available in OpenGL 1.2 (reported by the selector)
    (void)normalMap;

    Matrix4x4d viewProjection = camera->calculateProjectionMatrix();
    Matrix4x4d modelViewProjection = viewProjection.multiply(localTransform);

    if ( quality->isSurfacesSet() ) {
        OpenGL1RendererConfigurationStateSelector::activateState(
            camera, viewProjection, localTransform, activeLights,
            surfaceMaterial, quality, textureId);
        // Pass 1: surfaces, pushed slightly backwards to avoid z-fighting
        // with the overlay passes (wires / points)
        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_TRUE);
        glDepthFunc(GL_LESS);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR, SURFACE_POLYGON_OFFSET_UNITS);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        renderMesh(mesh);
        glDisable(GL_POLYGON_OFFSET_FILL);
        OpenGL1RendererConfigurationStateSelector::deactivateState();
    }

    if ( quality->isWiresSet() ) {
        OpenGL1RendererConfigurationStateSelector::activateConstantState(
            modelViewProjection, 1.0f, 1.0f, 1.0f);
        // Pass 2: wires, with depth test but biased in front of surfaces
        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_POLYGON_OFFSET_LINE);
        glPolygonOffset(LINE_POLYGON_OFFSET_FACTOR, LINE_POLYGON_OFFSET_UNITS);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(1.0f);
        renderMesh(mesh);
        glDisable(GL_POLYGON_OFFSET_LINE);
        OpenGL1RendererConfigurationStateSelector::deactivateState();
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    if ( quality->isPointsSet() ) {
        OpenGL1RendererConfigurationStateSelector::activateConstantState(
            modelViewProjection, 1.0f, 0.0f, 0.0f);
        // Pass 3: points, last, tested against surfaces (and not writing
        // depth), so they appear above the wires
        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        glDepthFunc(GL_LEQUAL);
        glDisable(GL_CULL_FACE);
        glPointSize(4.0f);
        enableArrays(mesh);
        glDrawArrays(GL_POINTS, 0, mesh->frontVertexCount);
        disableArrays();
        OpenGL1RendererConfigurationStateSelector::deactivateState();
    }

    if ( quality->isNormalsSet() || quality->isTrianglesNormalsSet() ) {
        drawNormalOverlays(mesh, quality, modelViewProjection);
    }
    if ( geometry != nullptr && quality->isBoundingVolumeSet() ) {
        OpenGL1MinMaxRenderer::draw(geometry, camera, localTransform);
    }
    if ( geometry != nullptr && quality->isSelectionCornersSet() ) {
        OpenGL1SelectionCornersRenderer::draw(geometry, camera, localTransform);
    }
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glDisable(GL_CULL_FACE);
    glBindTexture(GL_TEXTURE_2D, 0);
}

void OpenGL1MeshRenderer::release(Mesh* mesh)
{
    if ( mesh == nullptr || !mesh->uploaded ) {
        return;
    }
    if ( mesh->displayListId != 0 ) {
        glDeleteLists(mesh->displayListId, 1);
    }
    mesh->uploaded = false;
    mesh->displayListId = 0;
}

void OpenGL1MeshRenderer::dispose()
{
    OpenGL1RendererConfigurationStateSelector::dispose();
}

void OpenGL1MeshRenderer::enableArrays(Mesh* mesh)
{
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, mesh->positions.data());
    if ( mesh->normals.size() == mesh->positions.size() ) {
        glEnableClientState(GL_NORMAL_ARRAY);
        glNormalPointer(GL_FLOAT, 0, mesh->normals.data());
    }
    if ( mesh->uvs.size() / 2 == mesh->positions.size() / 3 ) {
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(2, GL_FLOAT, 0, mesh->uvs.data());
    }
}

void OpenGL1MeshRenderer::disableArrays()
{
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_NORMAL_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
}

void OpenGL1MeshRenderer::renderMesh(Mesh* mesh)
{
    if ( mesh->displayListId != 0 ) {
        glCallList(mesh->displayListId);
        return;
    }
    enableArrays(mesh);
    glDrawArrays(GL_TRIANGLES, 0, mesh->vertexCount);
    disableArrays();
}

/**
Compiles the triangles of the mesh in a display list: the vertex arrays are
copied into it, so later frames do not send them again.
*/
void OpenGL1MeshRenderer::upload(Mesh* mesh)
{
    if ( mesh->uploaded ) {
        return;
    }
    mesh->uploaded = true;
    if ( mesh->vertexCount <= 0 ) {
        return;
    }
    mesh->displayListId = glGenLists(1);
    if ( mesh->displayListId == 0 ) {
        return;
    }
    glNewList(mesh->displayListId, GL_COMPILE);
    enableArrays(mesh);
    glDrawArrays(GL_TRIANGLES, 0, mesh->vertexCount);
    disableArrays();
    glEndList();
}

void OpenGL1MeshRenderer::drawNormalOverlays(
    Mesh* mesh, const RendererConfiguration* quality,
    const Matrix4x4d& modelViewProjection)
{
    if ( mesh->positions.empty() || mesh->normals.empty() ) {
        return;
    }
    double size = mesh->characteristicSize > 1e-6 ? mesh->characteristicSize : 1e-6;
    float epsilon = (float)size * NORMAL_START_EPSILON;

    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_FALSE);
    glDepthFunc(GL_LEQUAL);
    glDisable(GL_CULL_FACE);

    if ( quality->isNormalsSet() ) {
        if ( mesh->vertexNormalLinePositions.size() == 0 ) {
            float length = (float)size * VERTEX_NORMAL_SCALE;
            java::ArrayList<float>& lines = mesh->vertexNormalLinePositions;
            lines.reserve((long)mesh->frontVertexCount * 6);
            for ( int i = 0; i < mesh->frontVertexCount; i++ ) {
                const float* p = &mesh->positions[i * 3];
                const float* n = &mesh->normals[i * 3];
                addLine(lines, p[0] + n[0] * epsilon, p[1] + n[1] * epsilon,
                        p[2] + n[2] * epsilon, n[0], n[1], n[2], length);
            }
            addUniformColors(mesh->vertexNormalLineColors,
                             lines.size() / 3, VERTEX_NORMAL_COLOR);
        }
        OpenGL1LineRenderer::drawLines(modelViewProjection,
            mesh->vertexNormalLinePositions, mesh->vertexNormalLineColors,
            1.0f, NORMAL_LINE_DEPTH_BIAS_NDC);
    }

    if ( quality->isTrianglesNormalsSet() ) {
        if ( mesh->triangleNormalLinePositions.size() == 0 ) {
            float length = (float)size * TRIANGLE_NORMAL_SCALE;
            java::ArrayList<float>& lines = mesh->triangleNormalLinePositions;
            int triangleCount = mesh->frontVertexCount / 3;
            lines.reserve((long)triangleCount * 6);
            for ( int t = 0; t < triangleCount; t++ ) {
                const float* p = &mesh->positions[t * 9];
                float cx = (p[0] + p[3] + p[6]) / 3.0f;
                float cy = (p[1] + p[4] + p[7]) / 3.0f;
                float cz = (p[2] + p[5] + p[8]) / 3.0f;
                float ux = p[3] - p[0], uy = p[4] - p[1], uz = p[5] - p[2];
                float vx = p[6] - p[0], vy = p[7] - p[1], vz = p[8] - p[2];
                float nx = uy * vz - uz * vy;
                float ny = uz * vx - ux * vz;
                float nz = ux * vy - uy * vx;
                float norm = std::sqrt(nx * nx + ny * ny + nz * nz);
                if ( norm <= 1e-12f ) {
                    nx = 0.0f; ny = 0.0f; nz = 1.0f;
                }
                else {
                    nx /= norm; ny /= norm; nz /= norm;
                }
                if ( nx * cx + ny * cy + nz * cz < 0.0f ) {
                    nx = -nx; ny = -ny; nz = -nz;
                }
                addLine(lines, cx + nx * epsilon, cy + ny * epsilon,
                        cz + nz * epsilon, nx, ny, nz, length);
            }
            addUniformColors(mesh->triangleNormalLineColors,
                             lines.size() / 3, TRIANGLE_NORMAL_COLOR);
        }
        OpenGL1LineRenderer::drawLines(modelViewProjection,
            mesh->triangleNormalLinePositions, mesh->triangleNormalLineColors,
            1.0f, NORMAL_LINE_DEPTH_BIAS_NDC);
    }
}

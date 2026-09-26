#include <cmath>
#include <cstdio>

#include <glad/gl.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/material/MicroFacetedMaterial.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RendererConfigurationShaderSelector.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.h"

unsigned int OpenGL4MeshRenderer::dummyTextureId = 0;
bool OpenGL4MeshRenderer::tooManyLightsReported = false;

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

void setMatrix(unsigned int programId, const char* name, const Matrix4x4d& matrix)
{
    GLint location = glGetUniformLocation(programId, name);
    if ( location >= 0 ) {
        float* values = matrix.exportToFloatArrayColumnOrder();
        glUniformMatrix4fv(location, 1, GL_FALSE, values);
        delete[] values;
    }
}

void setVector3(unsigned int programId, const char* name, const Vector3Dd& value)
{
    GLint location = glGetUniformLocation(programId, name);
    if ( location >= 0 ) {
        glUniform3f(location, (float)value.x(), (float)value.y(), (float)value.z());
    }
}

void setVector3(unsigned int programId, const char* name, const ColorRgb& value)
{
    GLint location = glGetUniformLocation(programId, name);
    if ( location >= 0 ) {
        glUniform3f(location, (float)value.r(), (float)value.g(), (float)value.b());
    }
}

void setInt(unsigned int programId, const char* name, int value)
{
    GLint location = glGetUniformLocation(programId, name);
    if ( location >= 0 ) {
        glUniform1i(location, value);
    }
}

void setFloat(unsigned int programId, const char* name, float value)
{
    GLint location = glGetUniformLocation(programId, name);
    if ( location >= 0 ) {
        glUniform1f(location, value);
    }
}

void configureMicroFacetUniforms(unsigned int programId,
                                 const SimpleMaterial& material)
{
    float roughness = 0.35f;
    float alpha = roughness * roughness;
    ColorRgb fresnelF0 = material.getSpecular();
    float kd = 1.0f;
    float ks = 1.0f;
    int fresnelModel = MicroFacetedMaterial::FRESNEL_MODEL_SCHLICK;
    int ndfModel = MicroFacetedMaterial::NDF_MODEL_BECKMANN;
    int geometryModel = MicroFacetedMaterial::GEOMETRY_MODEL_SMITH;
    ColorRgb eta(1.5, 1.5, 1.5);
    ColorRgb kappa(0.0, 0.0, 0.0);

    const MicroFacetedMaterial* microFaceted =
        dynamic_cast<const MicroFacetedMaterial*>(&material);
    if ( microFaceted != nullptr ) {
        roughness = (float)microFaceted->getRoughness();
        alpha = (float)microFaceted->getAlpha();
        fresnelF0 = microFaceted->getFresnelF0();
        kd = (float)microFaceted->getKd();
        ks = (float)microFaceted->getKs();
        fresnelModel = microFaceted->getFresnelModel();
        ndfModel = microFaceted->getNdfModel();
        geometryModel = microFaceted->getGeometryModel();
        eta = microFaceted->getEta();
        kappa = microFaceted->getKappa();
    }
    setFloat(programId, "cookRoughness", roughness);
    setFloat(programId, "cookAlpha", alpha);
    setFloat(programId, "cookKd", kd);
    setFloat(programId, "cookKs", ks);
    setVector3(programId, "cookF0", fresnelF0);
    setVector3(programId, "cookEta", eta);
    setVector3(programId, "cookKappa", kappa);
    setInt(programId, "cookFresnelModel", fresnelModel);
    setInt(programId, "cookNdfModel", ndfModel);
    setInt(programId, "cookGeometryModel", geometryModel);
}

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

OpenGL4MeshRenderer::Mesh::Mesh(double characteristicSize)
    : vertexCount(0), frontVertexCount(0),
      characteristicSize(characteristicSize), vaoId(0), uploaded(false)
{
    for ( int i = 0; i < 5; i++ ) {
        vboIds[i] = 0;
    }
}

void OpenGL4MeshRenderer::Mesh::setFrontVertexCount(int count)
{
    frontVertexCount = count < 0 ? 0 : (count > vertexCount ? vertexCount : count);
}

void OpenGL4MeshRenderer::draw(Mesh* mesh, Geometry* geometry, Camera* camera,
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
    bool hasNormalMap = normalMap != nullptr;
    int textureId = hasTexture ? OpenGL4ImageRenderer::activate(textureMap) : 0;
    int normalMapId = (hasNormalMap && quality->isBumpMapSet()) ?
        OpenGL4ImageRenderer::activate(normalMap) : 0;

    Matrix4x4d modelViewProjection =
        camera->calculateProjectionMatrix().multiply(localTransform);
    Matrix4x4d modelViewITLocal = localTransform.invert().transpose();

    if ( quality->isSurfacesSet() ) {
        unsigned int programId =
            OpenGL4RendererConfigurationShaderSelector::selectSurfaceShaderProgram(
                quality, hasTexture, normalMapId > 0);
        if ( programId != 0 ) {
            configureProgram(programId, modelViewProjection, localTransform,
                modelViewITLocal, camera, activeLights, surfaceMaterial,
                quality, textureId, normalMapId);
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
            OpenGL4RendererConfigurationShaderSelector::deactivateShader();
        }
    }

    if ( quality->isWiresSet() ) {
        RendererConfiguration wireQuality;
        wireQuality.setTexture(false);
        wireQuality.setUseVertexColors(false);
        wireQuality.setShadingType(RendererConfiguration::SHADING_TYPE_NOLIGHT);
        unsigned int wireProgram =
            OpenGL4RendererConfigurationShaderSelector::selectSurfaceShaderProgram(
                &wireQuality, false, false);
        if ( wireProgram != 0 ) {
            SimpleMaterial wireMaterial = surfaceMaterial
                .withDiffuse(ColorRgb(1, 1, 1))
                .withSpecular(ColorRgb(0, 0, 0))
                .withAmbient(ColorRgb(0, 0, 0));
            configureProgram(wireProgram, modelViewProjection, localTransform,
                modelViewITLocal, camera, activeLights, wireMaterial,
                &wireQuality, 0, 0);
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
            OpenGL4RendererConfigurationShaderSelector::deactivateShader();
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }

    if ( quality->isPointsSet() ) {
        RendererConfiguration pointQuality;
        pointQuality.setTexture(false);
        pointQuality.setUseVertexColors(false);
        pointQuality.setShadingType(RendererConfiguration::SHADING_TYPE_NOLIGHT);
        unsigned int pointsProgram =
            OpenGL4RendererConfigurationShaderSelector::selectSurfaceShaderProgram(
                &pointQuality, false, false);
        if ( pointsProgram != 0 ) {
            SimpleMaterial pointMaterial = surfaceMaterial
                .withAmbient(ColorRgb(0, 0, 0))
                .withDiffuse(ColorRgb(1, 0, 0))
                .withSpecular(ColorRgb(0, 0, 0));
            configureProgram(pointsProgram, modelViewProjection, localTransform,
                modelViewITLocal, camera, activeLights, pointMaterial,
                &pointQuality, 0, 0);
            // Pass 3: points, last, tested against surfaces (and not writing
            // depth), so they appear above the wires
            glEnable(GL_DEPTH_TEST);
            glDepthMask(GL_FALSE);
            glDepthFunc(GL_LEQUAL);
            glDisable(GL_CULL_FACE);
            glPointSize(4.0f);
            glBindVertexArray(mesh->vaoId);
            glDrawArrays(GL_POINTS, 0, mesh->frontVertexCount);
            glBindVertexArray(0);
            OpenGL4RendererConfigurationShaderSelector::deactivateShader();
        }
    }

    if ( quality->isNormalsSet() || quality->isTrianglesNormalsSet() ) {
        drawNormalOverlays(mesh, quality, modelViewProjection);
    }
    if ( geometry != nullptr && quality->isBoundingVolumeSet() ) {
        OpenGL4MinMaxRenderer::draw(geometry, camera, localTransform);
    }
    if ( geometry != nullptr && quality->isSelectionCornersSet() ) {
        OpenGL4SelectionCornersRenderer::draw(geometry, camera, localTransform);
    }
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glDisable(GL_CULL_FACE);
    glBindTexture(GL_TEXTURE_2D, 0);
}

void OpenGL4MeshRenderer::release(Mesh* mesh)
{
    if ( mesh == nullptr || !mesh->uploaded ) {
        return;
    }
    glDeleteBuffers(5, mesh->vboIds);
    glDeleteVertexArrays(1, &mesh->vaoId);
    mesh->uploaded = false;
    mesh->vaoId = 0;
}

void OpenGL4MeshRenderer::dispose()
{
    if ( dummyTextureId != 0 ) {
        glDeleteTextures(1, &dummyTextureId);
        dummyTextureId = 0;
    }
    OpenGL4RendererConfigurationShaderSelector::dispose();
}

/**
Every Phong / Gouraud / Cook-Torrance shader declares a `sTexture` sampler
even when the material has no texture. Some drivers require a complete
texture bound to the unit a sampler points to, so this 1x1 white texture is
bound whenever the material has none.
*/
unsigned int OpenGL4MeshRenderer::ensureDummyTexture()
{
    if ( dummyTextureId != 0 ) {
        return dummyTextureId;
    }
    const unsigned char white[3] = { 255, 255, 255 };
    glGenTextures(1, &dummyTextureId);
    glBindTexture(GL_TEXTURE_2D, dummyTextureId);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, 1, 1, 0, GL_RGB, GL_UNSIGNED_BYTE, white);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glBindTexture(GL_TEXTURE_2D, 0);
    return dummyTextureId;
}

void OpenGL4MeshRenderer::configureProgram(
    unsigned int programId, const Matrix4x4d& modelViewProjection,
    const Matrix4x4d& modelViewLocal, const Matrix4x4d& modelViewITLocal,
    Camera* camera, const java::ArrayList<Light*>& lights,
    const SimpleMaterial& material, const RendererConfiguration* quality,
    int textureId, int normalMapId)
{
    ColorRgb kd = material.getDiffuse();
    OpenGL4RendererConfigurationShaderSelector::activateShader(
        programId, modelViewProjection, quality,
        (float)kd.r(), (float)kd.g(), (float)kd.b());
    setMatrix(programId, "modelViewLocal", modelViewLocal);
    setMatrix(programId, "modelViewITLocal", modelViewITLocal);
    setVector3(programId, "cameraPositionGlobal", camera->getPosition());

    int lightCount = 0;
    char name[64];
    for ( long i = 0; i < lights.size(); i++ ) {
        if ( lightCount >= MAX_LIGHTS ) {
            if ( !tooManyLightsReported ) {
                tooManyLightsReported = true;
                fprintf(stderr, "OpenGL4MeshRenderer: scene has %ld lights, "
                        "but shaders use only the first %d\n",
                        (long)lights.size(), MAX_LIGHTS);
            }
            break;
        }
        snprintf(name, sizeof(name), "lightPositionsGlobal[%d]", lightCount);
        setVector3(programId, name, lights[i]->getPosition());
        snprintf(name, sizeof(name), "lightColorsGlobal[%d]", lightCount);
        setVector3(programId, name, lights[i]->getEmission());
        lightCount++;
    }
    setInt(programId, "numberOfLights", lightCount);
    setVector3(programId, "ambientColor", material.getAmbient());
    setVector3(programId, "diffuseColor", material.getDiffuse());
    setVector3(programId, "specularColor", material.getSpecular());
    // [BLIN1978b] bump scale used by both shader and raytracer examples
    setVector3(programId, "bumpScale", Vector3Dd(1.0, 1.0, 1.0));
    setFloat(programId, "phongExponent", (float)material.getPhongExponent());
    configureMicroFacetUniforms(programId, material);
    setInt(programId, "withTexture",
           (quality->isTextureSet() && textureId > 0) ? 1 : 0);
    setInt(programId, "withBumpMap",
           (quality->isBumpMapSet() && normalMapId > 0) ? 1 : 0);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D,
                  textureId > 0 ? (unsigned int)textureId : ensureDummyTexture());
    if ( normalMapId > 0 ) {
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, (unsigned int)normalMapId);
        glActiveTexture(GL_TEXTURE0);
    }
}

void OpenGL4MeshRenderer::renderMesh(Mesh* mesh)
{
    glBindVertexArray(mesh->vaoId);
    glDrawArrays(GL_TRIANGLES, 0, mesh->vertexCount);
    glBindVertexArray(0);
}

void OpenGL4MeshRenderer::upload(Mesh* mesh)
{
    if ( mesh->uploaded ) {
        return;
    }
    const std::vector<float>* arrays[5] = {
        &mesh->positions, &mesh->normals, &mesh->uvs, &mesh->tangents,
        &mesh->biNormals
    };
    const int sizes[5] = { 3, 3, 2, 3, 3 };

    glGenVertexArrays(1, &mesh->vaoId);
    glGenBuffers(5, mesh->vboIds);
    glBindVertexArray(mesh->vaoId);
    for ( int i = 0; i < 5; i++ ) {
        glBindBuffer(GL_ARRAY_BUFFER, mesh->vboIds[i]);
        glBufferData(GL_ARRAY_BUFFER,
                     (GLsizeiptr)(arrays[i]->size() * sizeof(float)),
                     arrays[i]->empty() ? nullptr : &(*arrays[i])[0],
                     GL_STATIC_DRAW);
        glEnableVertexAttribArray(i);
        glVertexAttribPointer(i, sizes[i], GL_FLOAT, GL_FALSE, 0, nullptr);
    }
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    mesh->uploaded = true;
}

void OpenGL4MeshRenderer::drawNormalOverlays(
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
        OpenGL4LineRenderer::drawLines(modelViewProjection,
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
        OpenGL4LineRenderer::drawLines(modelViewProjection,
            mesh->triangleNormalLinePositions, mesh->triangleNormalLineColors,
            1.0f, NORMAL_LINE_DEPTH_BIAS_NDC);
    }
}

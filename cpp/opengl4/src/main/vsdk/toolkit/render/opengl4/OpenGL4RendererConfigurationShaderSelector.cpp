#include <cstdio>
#include <string>

#include <glad/gl.h>

#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RendererConfigurationShaderSelector.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ShaderProgramUtil.h"

unsigned int OpenGL4RendererConfigurationShaderSelector::constantProgramId = 0;
unsigned int OpenGL4RendererConfigurationShaderSelector::texturedProgramId = 0;
unsigned int OpenGL4RendererConfigurationShaderSelector::flatProgramId = 0;
unsigned int OpenGL4RendererConfigurationShaderSelector::flatTexturedProgramId = 0;
unsigned int OpenGL4RendererConfigurationShaderSelector::gouraudProgramId = 0;
unsigned int OpenGL4RendererConfigurationShaderSelector::phongProgramId = 0;
unsigned int OpenGL4RendererConfigurationShaderSelector::phongBumpProgramId = 0;
unsigned int OpenGL4RendererConfigurationShaderSelector::cookProgramId = 0;
unsigned int OpenGL4RendererConfigurationShaderSelector::cookBumpProgramId = 0;

namespace {

void setInt(unsigned int programId, const char* name, int value)
{
    GLint location = glGetUniformLocation(programId, name);
    if ( location >= 0 ) {
        glUniform1i(location, value);
    }
}

}

unsigned int OpenGL4RendererConfigurationShaderSelector::createProgramFromFiles(
    const char* vertexFile, const char* pixelFile)
{
    return OpenGL4ShaderProgramUtil::createProgramFromFiles(vertexFile,
                                                            pixelFile);
}

void OpenGL4RendererConfigurationShaderSelector::ensurePrograms()
{
    if ( constantProgramId == 0 ) {
        constantProgramId = createProgramFromFiles(
            "constantVertexShader.glsl", "constantPixelShader.glsl");
    }
    if ( texturedProgramId == 0 ) {
        texturedProgramId = createProgramFromFiles(
            "constantTextureVertexShader.glsl", "constantTexturePixelShader.glsl");
    }
    if ( gouraudProgramId == 0 ) {
        gouraudProgramId = createProgramFromFiles(
            "gouraudTextureVertexShader.glsl", "gouraudTexturePixelShader.glsl");
    }
    if ( flatProgramId == 0 ) {
        flatProgramId = createProgramFromFiles(
            "flatVertexShader.glsl", "flatPixelShader.glsl");
    }
    if ( flatTexturedProgramId == 0 ) {
        flatTexturedProgramId = createProgramFromFiles(
            "flatTexturedVertexShader.glsl", "flatTexturedPixelShader.glsl");
    }
    if ( phongProgramId == 0 ) {
        phongProgramId = createProgramFromFiles(
            "phongTextureVertexShader.glsl", "phongTexturePixelShader.glsl");
    }
    if ( phongBumpProgramId == 0 ) {
        phongBumpProgramId = createProgramFromFiles(
            "phongTextureBumpVertexShader.glsl", "phongTextureBumpPixelShader.glsl");
    }
    if ( cookProgramId == 0 ) {
        cookProgramId = createProgramFromFiles(
            "phongTextureVertexShader.glsl", "cookTexturePixelShader.glsl");
    }
    if ( cookBumpProgramId == 0 ) {
        cookBumpProgramId = createProgramFromFiles(
            "phongTextureBumpVertexShader.glsl", "cookTextureBumpPixelShader.glsl");
    }
}

unsigned int OpenGL4RendererConfigurationShaderSelector::selectSurfaceShaderProgram(
    const RendererConfiguration* quality, bool hasTexture, bool hasNormalMap)
{
    ensurePrograms();
    if ( quality == nullptr ) {
        return hasTexture ? texturedProgramId : constantProgramId;
    }

    int shadingType = quality->getShadingType();
    if ( shadingType == RendererConfiguration::SHADING_TYPE_NOLIGHT ) {
        return (quality->isTextureSet() && hasTexture) ?
            texturedProgramId : constantProgramId;
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_FLAT ) {
        return (quality->isTextureSet() && hasTexture) ?
            flatTexturedProgramId : flatProgramId;
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_PHONG ) {
        if ( quality->isBumpMapSet() && hasNormalMap ) {
            return phongBumpProgramId;
        }
        return phongProgramId;
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_COOK_TERRANCE ) {
        if ( quality->isBumpMapSet() && hasNormalMap ) {
            return cookBumpProgramId;
        }
        return cookProgramId;
    }
    return gouraudProgramId;
}

void OpenGL4RendererConfigurationShaderSelector::activateShader(
    unsigned int programId, const Matrix4x4d& modelViewProjection,
    const RendererConfiguration* quality,
    float diffuseR, float diffuseG, float diffuseB)
{
    glUseProgram(programId);

    GLint location = glGetUniformLocation(programId, "modelViewProjectionLocal");
    if ( location >= 0 ) {
        float* matrix = modelViewProjection.exportToFloatArrayColumnOrder();
        glUniformMatrix4fv(location, 1, GL_FALSE, matrix);
        delete[] matrix;
    }
    location = glGetUniformLocation(programId, "diffuseColor");
    if ( location >= 0 ) {
        glUniform3f(location, diffuseR, diffuseG, diffuseB);
    }
    setInt(programId, "withTexture",
           (quality != nullptr && quality->isTextureSet()) ? 1 : 0);
    setInt(programId, "withVertexColors",
           (quality != nullptr && quality->getUseVertexColors()) ? 1 : 0);
    setInt(programId, "sTexture", 0);
    setInt(programId, "sNormalMap", 1);
}

void OpenGL4RendererConfigurationShaderSelector::deactivateShader()
{
    glUseProgram(0);
}

void OpenGL4RendererConfigurationShaderSelector::dispose()
{
    unsigned int* programs[] = {
        &constantProgramId, &texturedProgramId, &flatProgramId,
        &flatTexturedProgramId, &gouraudProgramId, &phongProgramId,
        &phongBumpProgramId, &cookProgramId, &cookBumpProgramId
    };
    for ( size_t i = 0; i < sizeof(programs) / sizeof(programs[0]); i++ ) {
        if ( *programs[i] != 0 ) {
            glDeleteProgram(*programs[i]);
            *programs[i] = 0;
        }
    }
}

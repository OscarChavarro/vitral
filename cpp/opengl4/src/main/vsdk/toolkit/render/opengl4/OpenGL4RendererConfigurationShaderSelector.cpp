#include <cstdio>
#include <string>

#include <GL/glew.h>

#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RendererConfigurationShaderSelector.h"

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

/**
@return the source of a file of `etc/glslShaders`, looked for from the usual
working directories of the examples and applications, or an empty string
*/
std::string readShaderSource(const char* fileName)
{
    const char* folders[] = {
        "../../../../etc/glslShaders/",
        "../../../etc/glslShaders/",
        "../../etc/glslShaders/",
        "../etc/glslShaders/",
        "etc/glslShaders/"
    };
    for ( size_t i = 0; i < sizeof(folders) / sizeof(folders[0]); i++ ) {
        std::string path = std::string(folders[i]) + fileName;
        FILE* file = fopen(path.c_str(), "rb");
        if ( file == nullptr ) {
            continue;
        }
        std::string source;
        char buffer[4096];
        size_t count;
        while ( (count = fread(buffer, 1, sizeof(buffer), file)) > 0 ) {
            source.append(buffer, count);
        }
        fclose(file);
        if ( !source.empty() ) {
            return source;
        }
    }
    return std::string();
}

GLuint compileShader(GLenum type, const std::string& source, const char* fileName)
{
    GLuint shader = glCreateShader(type);
    const char* text = source.c_str();
    glShaderSource(shader, 1, &text, nullptr);
    glCompileShader(shader);
    GLint ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if ( !ok ) {
        char log[4096];
        glGetShaderInfoLog(shader, sizeof(log), nullptr, log);
        fprintf(stderr, "OpenGL4RendererConfigurationShaderSelector: %s: %s\n",
                fileName, log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

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
    std::string vertexSource = readShaderSource(vertexFile);
    std::string pixelSource = readShaderSource(pixelFile);
    if ( vertexSource.empty() || pixelSource.empty() ) {
        fprintf(stderr, "OpenGL4RendererConfigurationShaderSelector: shader "
                "not found: %s / %s\n", vertexFile, pixelFile);
        return 0;
    }
    GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource, vertexFile);
    GLuint pixelShader = compileShader(GL_FRAGMENT_SHADER, pixelSource, pixelFile);
    if ( vertexShader == 0 || pixelShader == 0 ) {
        if ( vertexShader != 0 ) glDeleteShader(vertexShader);
        if ( pixelShader != 0 ) glDeleteShader(pixelShader);
        return 0;
    }

    GLuint program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, pixelShader);
    glLinkProgram(program);
    glDeleteShader(vertexShader);
    glDeleteShader(pixelShader);

    GLint ok = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &ok);
    if ( !ok ) {
        char log[4096];
        glGetProgramInfoLog(program, sizeof(log), nullptr, log);
        fprintf(stderr, "OpenGL4RendererConfigurationShaderSelector: link %s / %s: %s\n",
                vertexFile, pixelFile, log);
        glDeleteProgram(program);
        return 0;
    }
    return program;
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

#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RGBImageUncompressedRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RGBAImageUncompressedRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RGBAImageCompressedRenderer.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBAImageCompressed.h"
#include <cstdio>
#include <fstream>
#include <sstream>
#include <vector>

OpenGL4ImageRenderer::TextureFilterMode OpenGL4ImageRenderer::textureFilterMode = OpenGL4ImageRenderer::TextureFilterMode::LINEAR;
GLuint OpenGL4ImageRenderer::quadVaoId = 0;
GLuint OpenGL4ImageRenderer::quadPositionVboId = 0;
GLuint OpenGL4ImageRenderer::quadUvVboId = 0;
GLuint OpenGL4ImageRenderer::shaderProgramId = 0;
GLint OpenGL4ImageRenderer::mvpUniformLocation = -1;
std::string OpenGL4ImageRenderer::shaderBasePath = "../etc/glslShaders";

int OpenGL4ImageRenderer::activate(Image* img) {
    if (img == nullptr) {
        return -1;
    }

    RGBAImageUncompressed* rgbaUncomp = dynamic_cast<RGBAImageUncompressed*>(img);
    if (rgbaUncomp != nullptr) {
        return OpenGL4RGBAImageUncompressedRenderer::activate(rgbaUncomp);
    }

    RGBAImageCompressed* rgbaComp = dynamic_cast<RGBAImageCompressed*>(img);
    if (rgbaComp != nullptr) {
        return OpenGL4RGBAImageCompressedRenderer::activate(rgbaComp);
    }

    RGBImageUncompressed* rgbUncomp = dynamic_cast<RGBImageUncompressed*>(img);
    if (rgbUncomp != nullptr) {
        return OpenGL4RGBImageUncompressedRenderer::activate(rgbUncomp);
    }

    return -1;
}

void OpenGL4ImageRenderer::deactivate(Image* img) {
    if (img == nullptr) {
        return;
    }

    RGBAImageUncompressed* rgbaUncomp = dynamic_cast<RGBAImageUncompressed*>(img);
    if (rgbaUncomp != nullptr) {
        OpenGL4RGBAImageUncompressedRenderer::deactivate(rgbaUncomp);
        return;
    }

    RGBAImageCompressed* rgbaComp = dynamic_cast<RGBAImageCompressed*>(img);
    if (rgbaComp != nullptr) {
        OpenGL4RGBAImageCompressedRenderer::deactivate(rgbaComp);
        return;
    }

    RGBImageUncompressed* rgbUncomp = dynamic_cast<RGBImageUncompressed*>(img);
    if (rgbUncomp != nullptr) {
        OpenGL4RGBImageUncompressedRenderer::deactivate(rgbUncomp);
    }
}

void OpenGL4ImageRenderer::unload(Image* img) {
    if (img == nullptr) {
        return;
    }

    RGBAImageUncompressed* rgbaUncomp = dynamic_cast<RGBAImageUncompressed*>(img);
    if (rgbaUncomp != nullptr) {
        OpenGL4RGBAImageUncompressedRenderer::unload(rgbaUncomp);
        return;
    }

    RGBAImageCompressed* rgbaComp = dynamic_cast<RGBAImageCompressed*>(img);
    if (rgbaComp != nullptr) {
        OpenGL4RGBAImageCompressedRenderer::unload(rgbaComp);
        return;
    }

    RGBImageUncompressed* rgbUncomp = dynamic_cast<RGBImageUncompressed*>(img);
    if (rgbUncomp != nullptr) {
        OpenGL4RGBImageUncompressedRenderer::unload(rgbUncomp);
    }
}

void OpenGL4ImageRenderer::draw(Image* img) {
    if (img == nullptr) {
        return;
    }

    RGBAImageUncompressed* rgbaUncomp = dynamic_cast<RGBAImageUncompressed*>(img);
    if (rgbaUncomp != nullptr) {
        OpenGL4RGBAImageUncompressedRenderer::draw(rgbaUncomp);
        return;
    }

    RGBAImageCompressed* rgbaComp = dynamic_cast<RGBAImageCompressed*>(img);
    if (rgbaComp != nullptr) {
        OpenGL4RGBAImageCompressedRenderer::draw(rgbaComp);
        return;
    }

    RGBImageUncompressed* rgbUncomp = dynamic_cast<RGBImageUncompressed*>(img);
    if (rgbUncomp != nullptr) {
        OpenGL4RGBImageUncompressedRenderer::draw(rgbUncomp);
    }
}

void OpenGL4ImageRenderer::drawTexturedQuad(
    GLuint textureId,
    const float* positions,
    int positionCount,
    const float* uvCoordinates,
    int uvCount,
    float diffuseR,
    float diffuseG,
    float diffuseB) {

    if (textureId <= 0 || positions == nullptr || uvCoordinates == nullptr) {
        return;
    }
    if (positionCount <= 0 || uvCount <= 0) {
        return;
    }
    if (positionCount != uvCount) {
        return;
    }

    ensureBuffers();
    initializeShaderProgram();
    if (shaderProgramId == 0) {
        return;
    }

    // Identity matrix for MVP when none is provided
    static const float identity[16] = {
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    };

    glUseProgram(shaderProgramId);

    glUniformMatrix4fv(mvpUniformLocation, 1, GL_FALSE, identity);

    GLint textureLoc = glGetUniformLocation(shaderProgramId, "sTexture");
    glUniform1i(textureLoc, 0);

    GLint colorLoc = glGetUniformLocation(shaderProgramId, "diffuseColor");
    glUniform3f(colorLoc, diffuseR, diffuseG, diffuseB);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);

    glBindVertexArray(quadVaoId);

    glBindBuffer(GL_ARRAY_BUFFER, quadPositionVboId);
    glBufferData(GL_ARRAY_BUFFER, positionCount * 3 * sizeof(float), positions, GL_STREAM_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

    glBindBuffer(GL_ARRAY_BUFFER, quadUvVboId);
    glBufferData(GL_ARRAY_BUFFER, uvCount * 2 * sizeof(float), uvCoordinates, GL_STREAM_DRAW);
    glEnableVertexAttribArray(2);
    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 0, nullptr);

    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    glDrawArrays(GL_TRIANGLES, 0, positionCount);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(2);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glUseProgram(0);
}

void OpenGL4ImageRenderer::drawTexturedQuad(
    GLuint textureId,
    const float* mvpColumnMajor16,
    const float* positions,
    int positionCount,
    const float* uvCoordinates,
    int uvCount,
    float diffuseR,
    float diffuseG,
    float diffuseB) {

    if (textureId <= 0 || mvpColumnMajor16 == nullptr || positions == nullptr || uvCoordinates == nullptr) {
        return;
    }
    if (positionCount <= 0 || uvCount <= 0) {
        return;
    }
    if (positionCount != uvCount) {
        return;
    }

    ensureBuffers();
    initializeShaderProgram();
    if (shaderProgramId == 0) {
        return;
    }

    glUseProgram(shaderProgramId);

    glUniformMatrix4fv(mvpUniformLocation, 1, GL_FALSE, mvpColumnMajor16);

    GLint textureLoc = glGetUniformLocation(shaderProgramId, "sTexture");
    glUniform1i(textureLoc, 0);

    GLint colorLoc = glGetUniformLocation(shaderProgramId, "diffuseColor");
    glUniform3f(colorLoc, diffuseR, diffuseG, diffuseB);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);

    glBindVertexArray(quadVaoId);

    glBindBuffer(GL_ARRAY_BUFFER, quadPositionVboId);
    glBufferData(GL_ARRAY_BUFFER, positionCount * 3 * sizeof(float), positions, GL_STREAM_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

    glBindBuffer(GL_ARRAY_BUFFER, quadUvVboId);
    glBufferData(GL_ARRAY_BUFFER, uvCount * 2 * sizeof(float), uvCoordinates, GL_STREAM_DRAW);
    glEnableVertexAttribArray(2);
    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 0, nullptr);

    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    glDrawArrays(GL_TRIANGLES, 0, positionCount);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(2);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glUseProgram(0);
}

void OpenGL4ImageRenderer::setTextureFilterMode(TextureFilterMode mode) {
    textureFilterMode = mode;
}

OpenGL4ImageRenderer::TextureFilterMode OpenGL4ImageRenderer::getTextureFilterMode() {
    return textureFilterMode;
}

GLint OpenGL4ImageRenderer::minFilterParam() {
    if (textureFilterMode == TextureFilterMode::NEAREST) {
        return GL_NEAREST;
    }
    return GL_LINEAR_MIPMAP_LINEAR;
}

GLint OpenGL4ImageRenderer::magFilterParam() {
    if (textureFilterMode == TextureFilterMode::NEAREST) {
        return GL_NEAREST;
    }
    return GL_LINEAR;
}

void OpenGL4ImageRenderer::setShaderBasePath(const std::string& basePath) {
    shaderBasePath = basePath;
}

void OpenGL4ImageRenderer::ensureBuffers() {
    if (quadVaoId != 0) {
        return;
    }

    // Create VAO and VBOs without pre-configuring attribute pointers
    // (attribute pointers are set per-draw in drawTexturedQuad, as in Java)
    glGenVertexArrays(1, &quadVaoId);
    glGenBuffers(1, &quadPositionVboId);
    glGenBuffers(1, &quadUvVboId);
}

void OpenGL4ImageRenderer::dispose() {
    OpenGL4RGBImageUncompressedRenderer::disposeAll();
    OpenGL4RGBAImageUncompressedRenderer::disposeAll();
    OpenGL4RGBAImageCompressedRenderer::disposeAll();

    if (quadVaoId != 0) {
        glDeleteVertexArrays(1, &quadVaoId);
        quadVaoId = 0;
    }
    if (quadPositionVboId != 0) {
        glDeleteBuffers(1, &quadPositionVboId);
        quadPositionVboId = 0;
    }
    if (quadUvVboId != 0) {
        glDeleteBuffers(1, &quadUvVboId);
        quadUvVboId = 0;
    }
    if (shaderProgramId != 0) {
        glDeleteProgram(shaderProgramId);
        shaderProgramId = 0;
        mvpUniformLocation = -1;
    }
}

void OpenGL4ImageRenderer::initializeShaderProgram() {
    if (shaderProgramId != 0) {
        return;
    }

    std::string vertexPath = shaderBasePath + "/constantTextureVertexShader.glsl";
    std::string fragmentPath = shaderBasePath + "/constantTexturePixelShader.glsl";

    std::string vertexSource = readShaderFile(vertexPath);
    std::string fragmentSource = readShaderFile(fragmentPath);

    if (vertexSource.empty() || fragmentSource.empty()) {
        fprintf(stderr, "Error: Failed to read shader files\n");
        return;
    }

    GLuint vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER);
    GLuint fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER);

    if (vertexShader == 0 || fragmentShader == 0) {
        fprintf(stderr, "Error: Failed to compile shaders\n");
        return;
    }

    shaderProgramId = glCreateProgram();
    glAttachShader(shaderProgramId, vertexShader);
    glAttachShader(shaderProgramId, fragmentShader);
    glLinkProgram(shaderProgramId);

    GLint linkStatus;
    glGetProgramiv(shaderProgramId, GL_LINK_STATUS, &linkStatus);
    if (linkStatus == GL_FALSE) {
        GLint logLength;
        glGetProgramiv(shaderProgramId, GL_INFO_LOG_LENGTH, &logLength);
        char* log = new char[logLength];
        glGetProgramInfoLog(shaderProgramId, logLength, nullptr, log);
        fprintf(stderr, "Error: Shader program linking failed:\n%s\n", log);
        delete[] log;
        glDeleteProgram(shaderProgramId);
        shaderProgramId = 0;
        return;
    }

    mvpUniformLocation = glGetUniformLocation(shaderProgramId, "modelViewProjectionLocal");

    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);
}

std::string OpenGL4ImageRenderer::readShaderFile(const std::string& filename) {
    // Try primary path first
    std::ifstream file(filename);
    if (file.is_open()) {
        std::stringstream buffer;
        buffer << file.rdbuf();
        return buffer.str();
    }

    // Try alternative paths (for when running from different directories)
    std::vector<std::string> alternatePaths = {
        "../" + filename,
        "../../" + filename,
        "../../../" + filename,
        "../../../../" + filename,
    };

    for (const auto& altPath : alternatePaths) {
        file.open(altPath);
        if (file.is_open()) {
            std::stringstream buffer;
            buffer << file.rdbuf();
            return buffer.str();
        }
    }

    fprintf(stderr, "Error: Could not open shader file: %s\n", filename.c_str());
    return "";
}

GLuint OpenGL4ImageRenderer::compileShader(const std::string& source, int type) {
    GLuint shader = glCreateShader(type);
    const char* sourcePtr = source.c_str();
    glShaderSource(shader, 1, &sourcePtr, nullptr);
    glCompileShader(shader);

    GLint compileStatus;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compileStatus);
    if (compileStatus == GL_FALSE) {
        GLint logLength;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLength);
        char* log = new char[logLength];
        glGetShaderInfoLog(shader, logLength, nullptr, log);
        fprintf(stderr, "Error: Shader compilation failed:\n%s\n", log);
        delete[] log;
        glDeleteShader(shader);
        return 0;
    }

    return shader;
}

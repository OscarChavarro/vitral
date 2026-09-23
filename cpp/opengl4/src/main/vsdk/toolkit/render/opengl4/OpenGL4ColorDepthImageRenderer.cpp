#include <cstdio>

#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ColorDepthImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"

GLuint OpenGL4ColorDepthImageRenderer::programId = 0;
GLint OpenGL4ColorDepthImageRenderer::colorTextureLocation = -1;
GLint OpenGL4ColorDepthImageRenderer::depthTextureLocation = -1;
GLuint OpenGL4ColorDepthImageRenderer::vaoId = 0;
GLuint OpenGL4ColorDepthImageRenderer::positionVboId = 0;
GLuint OpenGL4ColorDepthImageRenderer::uvVboId = 0;
GLuint OpenGL4ColorDepthImageRenderer::colorTextureId = 0;
GLuint OpenGL4ColorDepthImageRenderer::depthTextureId = 0;
int OpenGL4ColorDepthImageRenderer::textureXSize = 0;
int OpenGL4ColorDepthImageRenderer::textureYSize = 0;
float* OpenGL4ColorDepthImageRenderer::depthUploadBuffer = 0;
int OpenGL4ColorDepthImageRenderer::depthUploadCapacity = 0;

void OpenGL4ColorDepthImageRenderer::draw(RGBImageUncompressed* image, ZBuffer* depth)
{
    if ( image == 0 || image->getXSize() <= 0 || image->getYSize() <= 0 ) {
        return;
    }
    if ( depth != 0 && (depth->getXSize() != image->getXSize() ||
                        depth->getYSize() != image->getYSize()) ) {
        Logger::reportMessage("OpenGL4ColorDepthImageRenderer", Logger::WARNING, "draw",
            "Depth buffer size does not match the image size, drawing color only");
        depth = 0;
    }
    if ( depth == 0 || !ensureInitialized() ) {
        OpenGL4ImageRenderer::unload(image);
        OpenGL4ImageRenderer::draw(image);
        return;
    }

    uploadTextures(image, depth);

    glDisable(GL_CULL_FACE);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    // Depth writes need the depth test enabled; ALWAYS replaces what was there
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_ALWAYS);
    glDepthMask(GL_TRUE);

    glUseProgram(programId);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, colorTextureId);
    glUniform1i(colorTextureLocation, 0);
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, depthTextureId);
    glUniform1i(depthTextureLocation, 1);

    drawLowerLeftQuad(image->getXSize(), image->getYSize());

    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, 0);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glUseProgram(0);
    glDepthFunc(GL_LESS);
}

void OpenGL4ColorDepthImageRenderer::drawLowerLeftQuad(int width, int height)
{
    GLint viewport[4];
    glGetIntegerv(GL_VIEWPORT, viewport);
    float w = 2.0f * ((float)width / (float)(viewport[2] > 1 ? viewport[2] : 1));
    float h = 2.0f * ((float)height / (float)(viewport[3] > 1 ? viewport[3] : 1));
    float x0 = -1.0f;
    float y0 = -1.0f;
    float x1 = x0 + w;
    float y1 = y0 + h;
    float positions[] = {
        x0, y0, 0.0f,
        x1, y0, 0.0f,
        x1, y1, 0.0f,
        x0, y0, 0.0f,
        x1, y1, 0.0f,
        x0, y1, 0.0f
    };
    float uvCoordinates[] = {
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f
    };

    glBindVertexArray(vaoId);
    glBindBuffer(GL_ARRAY_BUFFER, positionVboId);
    glBufferData(GL_ARRAY_BUFFER, sizeof(positions), positions, GL_STREAM_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, 0);
    glBindBuffer(GL_ARRAY_BUFFER, uvVboId);
    glBufferData(GL_ARRAY_BUFFER, sizeof(uvCoordinates), uvCoordinates, GL_STREAM_DRAW);
    glEnableVertexAttribArray(2);
    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 0, 0);

    glDrawArrays(GL_TRIANGLES, 0, 6);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(2);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
}

void OpenGL4ColorDepthImageRenderer::uploadTextures(RGBImageUncompressed* image, ZBuffer* depth)
{
    int xSize = image->getXSize();
    int ySize = image->getYSize();
    bool resized = xSize != textureXSize || ySize != textureYSize;

    // The raw image stores its bottom row first, as OpenGL textures, while
    // the depth buffer stores its top row first: depth rows are flipped
    int count = xSize * ySize;
    if ( depthUploadBuffer == 0 || depthUploadCapacity < count ) {
        delete[] depthUploadBuffer;
        depthUploadBuffer = new float[count];
        depthUploadCapacity = count;
    }
    const float* values = depth->getZBuffer();
    int write = 0;
    for ( int row = ySize - 1; row >= 0; row-- ) {
        for ( int x = 0; x < xSize; x++ ) {
            depthUploadBuffer[write++] = values[row * xSize + x];
        }
    }

    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, colorTextureId);
    if ( resized ) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, xSize, ySize, 0,
            GL_RGB, GL_UNSIGNED_BYTE, image->getRawImage());
    }
    else {
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, xSize, ySize,
            GL_RGB, GL_UNSIGNED_BYTE, image->getRawImage());
    }

    glBindTexture(GL_TEXTURE_2D, depthTextureId);
    if ( resized ) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F, xSize, ySize, 0,
            GL_RED, GL_FLOAT, depthUploadBuffer);
    }
    else {
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, xSize, ySize,
            GL_RED, GL_FLOAT, depthUploadBuffer);
    }
    glBindTexture(GL_TEXTURE_2D, 0);

    textureXSize = xSize;
    textureYSize = ySize;
}

GLuint OpenGL4ColorDepthImageRenderer::createTexture()
{
    GLuint textureId = 0;

    glGenTextures(1, &textureId);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindTexture(GL_TEXTURE_2D, 0);
    return textureId;
}

bool OpenGL4ColorDepthImageRenderer::ensureInitialized()
{
    if ( programId != 0 ) {
        return true;
    }

    java::String basePath = OpenGL4ImageRenderer::shaderBasePath;
    java::String vertexSource = OpenGL4ImageRenderer::readShaderFile(
        basePath + "/colorDepthImageVertexShader.glsl");
    java::String fragmentSource = OpenGL4ImageRenderer::readShaderFile(
        basePath + "/colorDepthImagePixelShader.glsl");
    if ( vertexSource.empty() || fragmentSource.empty() ) {
        Logger::reportMessage("OpenGL4ColorDepthImageRenderer", Logger::ERROR,
            "ensureInitialized", "Can not read colorDepthImage shaders");
        return false;
    }
    GLuint vertexShader = OpenGL4ImageRenderer::compileShader(vertexSource, GL_VERTEX_SHADER);
    GLuint fragmentShader = OpenGL4ImageRenderer::compileShader(fragmentSource, GL_FRAGMENT_SHADER);
    if ( vertexShader == 0 || fragmentShader == 0 ) {
        Logger::reportMessage("OpenGL4ColorDepthImageRenderer", Logger::ERROR,
            "ensureInitialized", "Can not compile colorDepthImage shaders");
        return false;
    }

    GLuint program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    GLint linkStatus = GL_FALSE;
    glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
    if ( linkStatus == GL_FALSE ) {
        Logger::reportMessage("OpenGL4ColorDepthImageRenderer", Logger::ERROR,
            "ensureInitialized", "Can not link colorDepthImage shaders");
        glDeleteProgram(program);
        return false;
    }

    programId = program;
    colorTextureLocation = glGetUniformLocation(programId, "colorTexture");
    depthTextureLocation = glGetUniformLocation(programId, "depthTexture");

    glGenVertexArrays(1, &vaoId);
    glGenBuffers(1, &positionVboId);
    glGenBuffers(1, &uvVboId);

    colorTextureId = createTexture();
    depthTextureId = createTexture();
    textureXSize = 0;
    textureYSize = 0;
    return true;
}

void OpenGL4ColorDepthImageRenderer::dispose()
{
    delete[] depthUploadBuffer;
    depthUploadBuffer = 0;
    depthUploadCapacity = 0;
    if ( programId == 0 ) {
        return;
    }
    glDeleteProgram(programId);
    glDeleteBuffers(1, &positionVboId);
    glDeleteBuffers(1, &uvVboId);
    glDeleteVertexArrays(1, &vaoId);
    glDeleteTextures(1, &colorTextureId);
    glDeleteTextures(1, &depthTextureId);

    programId = 0;
    vaoId = 0;
    positionVboId = 0;
    uvVboId = 0;
    colorTextureId = 0;
    depthTextureId = 0;
    textureXSize = 0;
    textureYSize = 0;
}

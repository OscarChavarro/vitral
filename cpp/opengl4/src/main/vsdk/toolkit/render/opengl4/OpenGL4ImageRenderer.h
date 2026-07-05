#ifndef __OPENGL4IMAGERENDERER__
#define __OPENGL4IMAGERENDERER__

#include <GL/glew.h>
#include "java/lang/String.h"

class Image;
class RGBImageUncompressed;
class RGBAImageUncompressed;
class RGBAImageCompressed;

class OpenGL4ImageRenderer {
public:
    enum class TextureFilterMode {
        LINEAR,
        NEAREST
    };

    static int activate(Image* img);
    static void deactivate(Image* img);
    static void unload(Image* img);
    static void draw(Image* img);

    static void drawTexturedQuad(
        GLuint textureId,
        const float* positions,
        int positionCount,
        const float* uvCoordinates,
        int uvCount,
        float diffuseR,
        float diffuseG,
        float diffuseB);

    static void drawTexturedQuad(
        GLuint textureId,
        const float* mvpColumnMajor16,
        const float* positions,
        int positionCount,
        const float* uvCoordinates,
        int uvCount,
        float diffuseR,
        float diffuseG,
        float diffuseB);

    static void setTextureFilterMode(TextureFilterMode mode);
    static TextureFilterMode getTextureFilterMode();

    static GLint minFilterParam();
    static GLint magFilterParam();

    static void setShaderBasePath(const java::String& basePath);

    static void dispose();

private:
    static TextureFilterMode textureFilterMode;
    static GLuint quadVaoId;
    static GLuint quadPositionVboId;
    static GLuint quadUvVboId;
    static GLuint shaderProgramId;
    static GLint mvpUniformLocation;
    static java::String shaderBasePath;

    static void ensureBuffers();
    static void initializeShaderProgram();
    static java::String readShaderFile(const java::String& filename);
    static GLuint compileShader(const java::String& source, int type);
};

#endif
#include "java/lang/String.h"

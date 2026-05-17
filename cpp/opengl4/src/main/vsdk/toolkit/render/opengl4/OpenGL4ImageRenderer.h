#ifndef __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4IMAGERENDERER_H__
#define __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4IMAGERENDERER_H__

#include <GL/glew.h>
#include <string>

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

    static void setShaderBasePath(const std::string& basePath);

    static void dispose();

private:
    static TextureFilterMode textureFilterMode;
    static GLuint quadVaoId;
    static GLuint quadPositionVboId;
    static GLuint quadUvVboId;
    static GLuint shaderProgramId;
    static GLint mvpUniformLocation;
    static std::string shaderBasePath;

    static void ensureBuffers();
    static void initializeShaderProgram();
    static std::string readShaderFile(const std::string& filename);
    static GLuint compileShader(const std::string& source, int type);
};

#endif // __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4IMAGERENDERER_H__

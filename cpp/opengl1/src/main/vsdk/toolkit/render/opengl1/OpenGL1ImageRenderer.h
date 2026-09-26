#ifndef __OPEN_GL_1_IMAGE_RENDERER__
#define __OPEN_GL_1_IMAGE_RENDERER__

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "java/lang/String.h"

class Image;
class RGBImageUncompressed;
class RGBAImageUncompressed;
class RGBAImageCompressed;

class OpenGL1ImageRenderer {
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

    static void dispose();

private:
    static TextureFilterMode textureFilterMode;
};

#endif

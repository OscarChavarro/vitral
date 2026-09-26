#include <cstdio>

#include "java/util/ArrayList.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RGBAImageUncompressedRenderer.h"
java::HashMap<RGBAImageUncompressed*, GLuint> OpenGL1RGBAImageUncompressedRenderer::compiledImages;
static java::ArrayList<GLuint> compiledTextureIds;

int OpenGL1RGBAImageUncompressedRenderer::activate(RGBAImageUncompressed* img) {
    if (img == nullptr) {
        return -1;
    }

    GLuint textureId = 0;
    if (!compiledImages.tryGet(img, &textureId)) {
        textureId = upload(img);
        if (textureId == 0) {
            return -1;
        }
        compiledImages.put(img, textureId);
        compiledTextureIds.add(textureId);
    }

    glBindTexture(GL_TEXTURE_2D, textureId);
    return static_cast<int>(textureId);
}

void OpenGL1RGBAImageUncompressedRenderer::deactivate(RGBAImageUncompressed* img) {
    if (img != nullptr && compiledImages.containsKey(img)) {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}

void OpenGL1RGBAImageUncompressedRenderer::unload(RGBAImageUncompressed* img) {
    if (img == nullptr) {
        return;
    }

    GLuint textureId = 0;
    if (!compiledImages.tryGet(img, &textureId)) {
        return;
    }

    glDeleteTextures(1, &textureId);
    compiledImages.remove(img);
}

void OpenGL1RGBAImageUncompressedRenderer::draw(RGBAImageUncompressed* img) {
    if (img == nullptr) {
        return;
    }

    int textureId = activate(img);
    if (textureId <= 0) {
        return;
    }

    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    float positions[] = {
        -1.0f, -1.0f, 0.0f,
         1.0f, -1.0f, 0.0f,
         1.0f,  1.0f, 0.0f,
        -1.0f, -1.0f, 0.0f,
         1.0f,  1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f
    };
    float uvCoordinates[] = {
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f
    };

    OpenGL1ImageRenderer::drawTexturedQuad(
        (GLuint)textureId,
        positions, 6,
        uvCoordinates, 6,
        1.0f, 1.0f, 1.0f);

    glDisable(GL_BLEND);
    glEnable(GL_DEPTH_TEST);
}

GLuint OpenGL1RGBAImageUncompressedRenderer::upload(RGBAImageUncompressed* img) {
    if (img == nullptr) {
        return 0;
    }

    if (img->getRawImage() == nullptr) {
        fprintf(stderr, "Error: RGBAImageUncompressed has no raw image data\n");
        return 0;
    }
    if (img->getXSize() <= 0 || img->getYSize() <= 0) {
        fprintf(stderr, "Error: RGBAImageUncompressed has invalid dimensions: %d x %d\n",
                img->getXSize(), img->getYSize());
        return 0;
    }

    GLuint textureId = 0;
    glGenTextures(1, &textureId);

    glBindTexture(GL_TEXTURE_2D, textureId);

    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    // OpenGL 1.2 textures have power of two sizes: GLU scales the image
    // and builds its mipmaps
    gluBuild2DMipmaps(
        GL_TEXTURE_2D,
        GL_RGBA8,
        img->getXSize(),
        img->getYSize(),
        GL_RGBA,
        GL_UNSIGNED_BYTE,
        img->getRawImage());

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, OpenGL1ImageRenderer::magFilterParam());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, OpenGL1ImageRenderer::minFilterParam());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

    glBindTexture(GL_TEXTURE_2D, 0);

    return textureId;
}

void OpenGL1RGBAImageUncompressedRenderer::disposeAll() {
    for (int i = 0; i < compiledTextureIds.size(); i++) {
        GLuint textureId = compiledTextureIds.get(i);
        glDeleteTextures(1, &textureId);
    }
    compiledImages.clear();
    compiledTextureIds.clear();
}

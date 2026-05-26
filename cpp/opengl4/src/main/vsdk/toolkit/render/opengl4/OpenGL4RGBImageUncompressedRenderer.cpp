#include "vsdk/toolkit/render/opengl4/OpenGL4RGBImageUncompressedRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include <cstdio>
#include "java/util/ArrayList.h"
#include "java/util/ArrayList.txx"

java::HashMap<RGBImageUncompressed*, GLuint> OpenGL4RGBImageUncompressedRenderer::compiledImages;
static java::ArrayList<GLuint> compiledTextureIds;

int OpenGL4RGBImageUncompressedRenderer::activate(RGBImageUncompressed* img) {
    if (img == nullptr) {
        return -1;
    }

    GLuint textureId = 0;
    if (!compiledImages.tryGet(img, &textureId)) {
        textureId = upload(img);
        compiledImages.put(img, textureId);
        compiledTextureIds.add(textureId);
    }

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    return static_cast<int>(textureId);
}

void OpenGL4RGBImageUncompressedRenderer::deactivate(RGBImageUncompressed* img) {
    if (img != nullptr && compiledImages.containsKey(img)) {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}

void OpenGL4RGBImageUncompressedRenderer::unload(RGBImageUncompressed* img) {
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

void OpenGL4RGBImageUncompressedRenderer::draw(RGBImageUncompressed* img) {
    if (img == nullptr) {
        return;
    }

    int textureId = activate(img);
    if (textureId <= 0) {
        return;
    }

    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);

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

    OpenGL4ImageRenderer::drawTexturedQuad(
        (GLuint)textureId,
        positions, 6,
        uvCoordinates, 6,
        1.0f, 1.0f, 1.0f);

    glEnable(GL_DEPTH_TEST);
}

GLuint OpenGL4RGBImageUncompressedRenderer::upload(RGBImageUncompressed* img) {
    if (img == nullptr) {
        return 0;
    }

    if (img->getRawImage() == nullptr) {
        fprintf(stderr, "Error: RGBImageUncompressed has no raw image data\n");
        return 0;
    }

    if (img->getXSize() <= 0 || img->getYSize() <= 0) {
        fprintf(stderr, "Error: RGBImageUncompressed has invalid dimensions: %d x %d\n",
                img->getXSize(), img->getYSize());
        return 0;
    }

    GLuint textureId = 0;
    glGenTextures(1, &textureId);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);

    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(
        GL_TEXTURE_2D,
        0,
        GL_RGB8,
        img->getXSize(),
        img->getYSize(),
        0,
        GL_RGB,
        GL_UNSIGNED_BYTE,
        img->getRawImage());

    glGenerateMipmap(GL_TEXTURE_2D);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, OpenGL4ImageRenderer::magFilterParam());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, OpenGL4ImageRenderer::minFilterParam());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glBindTexture(GL_TEXTURE_2D, 0);

    return textureId;
}

void OpenGL4RGBImageUncompressedRenderer::disposeAll() {
    for (int i = 0; i < compiledTextureIds.size(); i++) {
        GLuint textureId = compiledTextureIds.get(i);
        glDeleteTextures(1, &textureId);
    }
    compiledImages.clear();
    compiledTextureIds.clear();
}

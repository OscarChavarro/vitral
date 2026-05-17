#include "OpenGL4RGBAImageUncompressedRenderer.h"
#include "OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include <cstdio>

std::map<RGBAImageUncompressed*, GLuint> OpenGL4RGBAImageUncompressedRenderer::compiledImages;

int OpenGL4RGBAImageUncompressedRenderer::activate(RGBAImageUncompressed* img) {
    if (img == nullptr) {
        return -1;
    }

    auto it = compiledImages.find(img);
    if (it == compiledImages.end()) {
        GLuint textureId = upload(img);
        if (textureId == 0) {
            return -1;
        }
        compiledImages[img] = textureId;
    }

    GLuint textureId = compiledImages[img];
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    return static_cast<int>(textureId);
}

void OpenGL4RGBAImageUncompressedRenderer::deactivate(RGBAImageUncompressed* img) {
    if (img != nullptr && compiledImages.find(img) != compiledImages.end()) {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}

void OpenGL4RGBAImageUncompressedRenderer::unload(RGBAImageUncompressed* img) {
    if (img == nullptr) {
        return;
    }

    auto it = compiledImages.find(img);
    if (it == compiledImages.end()) {
        return;
    }

    GLuint textureId = it->second;
    glDeleteTextures(1, &textureId);
    compiledImages.erase(it);
}

void OpenGL4RGBAImageUncompressedRenderer::draw(RGBAImageUncompressed* img) {
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

    OpenGL4ImageRenderer::drawTexturedQuad(
        (GLuint)textureId,
        positions, 6,
        uvCoordinates, 6,
        1.0f, 1.0f, 1.0f);

    glDisable(GL_BLEND);
    glEnable(GL_DEPTH_TEST);
}

GLuint OpenGL4RGBAImageUncompressedRenderer::upload(RGBAImageUncompressed* img) {
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

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);

    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(
        GL_TEXTURE_2D,
        0,
        GL_RGBA8,
        img->getXSize(),
        img->getYSize(),
        0,
        GL_RGBA,
        GL_UNSIGNED_BYTE,
        img->getRawImage());

    glGenerateMipmap(GL_TEXTURE_2D);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, OpenGL4ImageRenderer::magFilterParam());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, OpenGL4ImageRenderer::minFilterParam());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

    glBindTexture(GL_TEXTURE_2D, 0);

    return textureId;
}

void OpenGL4RGBAImageUncompressedRenderer::disposeAll() {
    for (auto& pair : compiledImages) {
        glDeleteTextures(1, &pair.second);
    }
    compiledImages.clear();
}

#include "OpenGL4RGBImageUncompressedRenderer.h"
#include "OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"

std::map<RGBImageUncompressed*, GLuint> OpenGL4RGBImageUncompressedRenderer::compiledImages;

int OpenGL4RGBImageUncompressedRenderer::activate(RGBImageUncompressed* img) {
    if (img == nullptr) {
        return -1;
    }

    auto it = compiledImages.find(img);
    if (it == compiledImages.end()) {
        GLuint textureId = upload(img);
        compiledImages[img] = textureId;
    }

    GLuint textureId = compiledImages[img];
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    return static_cast<int>(textureId);
}

void OpenGL4RGBImageUncompressedRenderer::deactivate(RGBImageUncompressed* img) {
    if (img != nullptr && compiledImages.find(img) != compiledImages.end()) {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}

void OpenGL4RGBImageUncompressedRenderer::unload(RGBImageUncompressed* img) {
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
    for (auto& pair : compiledImages) {
        glDeleteTextures(1, &pair.second);
    }
    compiledImages.clear();
}

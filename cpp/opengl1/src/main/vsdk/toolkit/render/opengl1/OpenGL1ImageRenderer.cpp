#include <cstdio>

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/RGBAImageCompressed.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MatrixState.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ColorDepthImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RGBAImageCompressedRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RGBAImageUncompressedRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RGBImageUncompressedRenderer.h"
OpenGL1ImageRenderer::TextureFilterMode OpenGL1ImageRenderer::textureFilterMode = OpenGL1ImageRenderer::TextureFilterMode::LINEAR;

int OpenGL1ImageRenderer::activate(Image* img) {
    if (img == nullptr) {
        return -1;
    }

    RGBAImageUncompressed* rgbaUncomp = dynamic_cast<RGBAImageUncompressed*>(img);
    if (rgbaUncomp != nullptr) {
        return OpenGL1RGBAImageUncompressedRenderer::activate(rgbaUncomp);
    }

    RGBAImageCompressed* rgbaComp = dynamic_cast<RGBAImageCompressed*>(img);
    if (rgbaComp != nullptr) {
        return OpenGL1RGBAImageCompressedRenderer::activate(rgbaComp);
    }

    RGBImageUncompressed* rgbUncomp = dynamic_cast<RGBImageUncompressed*>(img);
    if (rgbUncomp != nullptr) {
        return OpenGL1RGBImageUncompressedRenderer::activate(rgbUncomp);
    }

    return -1;
}

void OpenGL1ImageRenderer::deactivate(Image* img) {
    if (img == nullptr) {
        return;
    }

    RGBAImageUncompressed* rgbaUncomp = dynamic_cast<RGBAImageUncompressed*>(img);
    if (rgbaUncomp != nullptr) {
        OpenGL1RGBAImageUncompressedRenderer::deactivate(rgbaUncomp);
        return;
    }

    RGBAImageCompressed* rgbaComp = dynamic_cast<RGBAImageCompressed*>(img);
    if (rgbaComp != nullptr) {
        OpenGL1RGBAImageCompressedRenderer::deactivate(rgbaComp);
        return;
    }

    RGBImageUncompressed* rgbUncomp = dynamic_cast<RGBImageUncompressed*>(img);
    if (rgbUncomp != nullptr) {
        OpenGL1RGBImageUncompressedRenderer::deactivate(rgbUncomp);
    }
}

void OpenGL1ImageRenderer::unload(Image* img) {
    if (img == nullptr) {
        return;
    }

    RGBAImageUncompressed* rgbaUncomp = dynamic_cast<RGBAImageUncompressed*>(img);
    if (rgbaUncomp != nullptr) {
        OpenGL1RGBAImageUncompressedRenderer::unload(rgbaUncomp);
        return;
    }

    RGBAImageCompressed* rgbaComp = dynamic_cast<RGBAImageCompressed*>(img);
    if (rgbaComp != nullptr) {
        OpenGL1RGBAImageCompressedRenderer::unload(rgbaComp);
        return;
    }

    RGBImageUncompressed* rgbUncomp = dynamic_cast<RGBImageUncompressed*>(img);
    if (rgbUncomp != nullptr) {
        OpenGL1RGBImageUncompressedRenderer::unload(rgbUncomp);
    }
}

void OpenGL1ImageRenderer::draw(Image* img) {
    if (img == nullptr) {
        return;
    }

    RGBAImageUncompressed* rgbaUncomp = dynamic_cast<RGBAImageUncompressed*>(img);
    if (rgbaUncomp != nullptr) {
        OpenGL1RGBAImageUncompressedRenderer::draw(rgbaUncomp);
        return;
    }

    RGBAImageCompressed* rgbaComp = dynamic_cast<RGBAImageCompressed*>(img);
    if (rgbaComp != nullptr) {
        OpenGL1RGBAImageCompressedRenderer::draw(rgbaComp);
        return;
    }

    RGBImageUncompressed* rgbUncomp = dynamic_cast<RGBImageUncompressed*>(img);
    if (rgbUncomp != nullptr) {
        OpenGL1RGBImageUncompressedRenderer::draw(rgbUncomp);
    }
}

void OpenGL1ImageRenderer::drawTexturedQuad(
    GLuint textureId,
    const float* positions,
    int positionCount,
    const float* uvCoordinates,
    int uvCount,
    float diffuseR,
    float diffuseG,
    float diffuseB) {

    // Identity matrix for MVP when none is provided
    static const float identity[16] = {
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    };

    drawTexturedQuad(textureId, identity, positions, positionCount,
        uvCoordinates, uvCount, diffuseR, diffuseG, diffuseB);
}

void OpenGL1ImageRenderer::drawTexturedQuad(
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

    glPushAttrib(GL_ENABLE_BIT | GL_POLYGON_BIT | GL_TEXTURE_BIT | GL_CURRENT_BIT);
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadMatrixf(mvpColumnMajor16);
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    // Texture color modulated by the diffuse color, as the
    // constantTexture GLSL program of the OpenGL 4 port
    glDisable(GL_LIGHTING);
    glEnable(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    glColor4f(diffuseR, diffuseG, diffuseB, 1.0f);

    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, positions);
    glTexCoordPointer(2, GL_FLOAT, 0, uvCoordinates);

    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    glDrawArrays(GL_TRIANGLES, 0, positionCount);

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glBindTexture(GL_TEXTURE_2D, 0);

    OpenGL1MatrixState::pop();
    glPopAttrib();
}

void OpenGL1ImageRenderer::setTextureFilterMode(TextureFilterMode mode) {
    textureFilterMode = mode;
}

OpenGL1ImageRenderer::TextureFilterMode OpenGL1ImageRenderer::getTextureFilterMode() {
    return textureFilterMode;
}

GLint OpenGL1ImageRenderer::minFilterParam() {
    if (textureFilterMode == TextureFilterMode::NEAREST) {
        return GL_NEAREST;
    }
    return GL_LINEAR_MIPMAP_LINEAR;
}

GLint OpenGL1ImageRenderer::magFilterParam() {
    if (textureFilterMode == TextureFilterMode::NEAREST) {
        return GL_NEAREST;
    }
    return GL_LINEAR;
}

void OpenGL1ImageRenderer::dispose() {
    OpenGL1ColorDepthImageRenderer::dispose();
    OpenGL1RGBImageUncompressedRenderer::disposeAll();
    OpenGL1RGBAImageUncompressedRenderer::disposeAll();
    OpenGL1RGBAImageCompressedRenderer::disposeAll();
}

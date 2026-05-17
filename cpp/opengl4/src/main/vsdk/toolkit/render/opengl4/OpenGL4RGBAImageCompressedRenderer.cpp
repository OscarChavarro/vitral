#include "OpenGL4RGBAImageCompressedRenderer.h"
#include "OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/media/RGBAImageCompressed.h"
#include <algorithm>
#include <cstdio>

#ifndef GL_COMPRESSED_RGBA_S3TC_DXT1_EXT
#define GL_COMPRESSED_RGBA_S3TC_DXT1_EXT 0x83F1
#endif
#ifndef GL_COMPRESSED_RGBA_S3TC_DXT3_EXT
#define GL_COMPRESSED_RGBA_S3TC_DXT3_EXT 0x83F2
#endif
#ifndef GL_COMPRESSED_RGBA_S3TC_DXT5_EXT
#define GL_COMPRESSED_RGBA_S3TC_DXT5_EXT 0x83F3
#endif

std::map<RGBAImageCompressed*, GLuint> OpenGL4RGBAImageCompressedRenderer::compiledImages;

int OpenGL4RGBAImageCompressedRenderer::activate(RGBAImageCompressed* img) {
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

void OpenGL4RGBAImageCompressedRenderer::deactivate(RGBAImageCompressed* img) {
    if (img != nullptr && compiledImages.find(img) != compiledImages.end()) {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}

void OpenGL4RGBAImageCompressedRenderer::unload(RGBAImageCompressed* img) {
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

void OpenGL4RGBAImageCompressedRenderer::draw(RGBAImageCompressed* img) {
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

GLuint OpenGL4RGBAImageCompressedRenderer::upload(RGBAImageCompressed* img) {
    if (img == nullptr) {
        return 0;
    }

    int imageSize = img->getCompressedDataSize();
    if (imageSize <= 0) {
        fprintf(stderr, "Error: invalid compressed texture size\n");
        return 0;
    }

    const bool s3tcAvailable =
        glewIsSupported("GL_EXT_texture_compression_s3tc") ||
        glewIsSupported("GL_ANGLE_texture_compression_dxt1") ||
        glewIsSupported("GL_ANGLE_texture_compression_dxt3") ||
        glewIsSupported("GL_ANGLE_texture_compression_dxt5");

    int internalFormat = toOpenGlInternalFormat(img->getCompressionFormat());

    GLuint textureId = 0;
    glGenTextures(1, &textureId);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

    if (s3tcAvailable && internalFormat != 0) {
        glCompressedTexImage2D(
            GL_TEXTURE_2D,
            0,
            static_cast<GLenum>(internalFormat),
            img->getXSize(),
            img->getYSize(),
            0,
            imageSize,
            img->getRawImageDirectBuffer());
        glGenerateMipmap(GL_TEXTURE_2D);
    }
    else {
        fprintf(stderr, "Warning: S3TC extension not available; decoding compressed texture in CPU.\n");
        std::vector<unsigned char> rgba = decompressToRGBA(img);
        if (rgba.empty()) {
            glDeleteTextures(1, &textureId);
            glBindTexture(GL_TEXTURE_2D, 0);
            return 0;
        }

        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA8,
            img->getXSize(),
            img->getYSize(),
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            rgba.data());
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, OpenGL4ImageRenderer::magFilterParam());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, OpenGL4ImageRenderer::minFilterParam());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

    glBindTexture(GL_TEXTURE_2D, 0);
    return textureId;
}

std::vector<unsigned char> OpenGL4RGBAImageCompressedRenderer::decompressToRGBA(const RGBAImageCompressed* img) {
    std::vector<unsigned char> empty;
    if (img == nullptr) {
        return empty;
    }

    int fmt = img->getCompressionFormat();
    if (fmt != RGBAImageCompressed::COMPRESSION_DXT1 &&
        fmt != RGBAImageCompressed::COMPRESSION_DXT3 &&
        fmt != RGBAImageCompressed::COMPRESSION_DXT5) {
        fprintf(stderr, "Error: cannot decompress compressed format %d\n", fmt);
        return empty;
    }

    int width = img->getXSize();
    int height = img->getYSize();
    const unsigned char* src = reinterpret_cast<const unsigned char*>(img->getRawImageDirectBuffer());
    if (src == nullptr || width <= 0 || height <= 0) {
        return empty;
    }

    std::vector<unsigned char> rgba(static_cast<size_t>(width) * static_cast<size_t>(height) * 4U, 0);

    int blockSize = (fmt == RGBAImageCompressed::COMPRESSION_DXT1) ? 8 : 16;
    int blockCountX = std::max(1, (width + 3) / 4);
    int blockCountY = std::max(1, (height + 3) / 4);
    int srcOffset = 0;

    for (int by = 0; by < blockCountY; by++) {
        for (int bx = 0; bx < blockCountX; bx++) {
            int alphaOffset = srcOffset;
            int colorOffset = (fmt == RGBAImageCompressed::COMPRESSION_DXT1) ? srcOffset : srcOffset + 8;

            int c0 = readUShort(src, colorOffset);
            int c1 = readUShort(src, colorOffset + 2);
            int lookup = readInt(src, colorOffset + 4);

            int cr[4], cg[4], cb[4], ca[4];
            decodeRgb565(c0, cr[0], cg[0], cb[0]);
            decodeRgb565(c1, cr[1], cg[1], cb[1]);
            ca[0] = 255; ca[1] = 255; ca[2] = 255; ca[3] = 255;

            if (fmt == RGBAImageCompressed::COMPRESSION_DXT1) {
                if (c0 > c1) {
                    cr[2] = (2 * cr[0] + cr[1]) / 3;
                    cg[2] = (2 * cg[0] + cg[1]) / 3;
                    cb[2] = (2 * cb[0] + cb[1]) / 3;
                    cr[3] = (cr[0] + 2 * cr[1]) / 3;
                    cg[3] = (cg[0] + 2 * cg[1]) / 3;
                    cb[3] = (cb[0] + 2 * cb[1]) / 3;
                }
                else {
                    cr[2] = (cr[0] + cr[1]) / 2;
                    cg[2] = (cg[0] + cg[1]) / 2;
                    cb[2] = (cb[0] + cb[1]) / 2;
                    cr[3] = 0; cg[3] = 0; cb[3] = 0; ca[3] = 0;
                }
            }
            else {
                cr[2] = (2 * cr[0] + cr[1]) / 3;
                cg[2] = (2 * cg[0] + cg[1]) / 3;
                cb[2] = (2 * cb[0] + cb[1]) / 3;
                cr[3] = (cr[0] + 2 * cr[1]) / 3;
                cg[3] = (cg[0] + 2 * cg[1]) / 3;
                cb[3] = (cb[0] + 2 * cb[1]) / 3;
            }

            for (int py = 0; py < 4; py++) {
                for (int px = 0; px < 4; px++) {
                    int pixX = bx * 4 + px;
                    int pixY = by * 4 + py;
                    if (pixX >= width || pixY >= height) {
                        continue;
                    }

                    int idx = (lookup >> (2 * (py * 4 + px))) & 0x3;
                    int dstBase = (pixY * width + pixX) * 4;

                    rgba[dstBase + 0] = static_cast<unsigned char>(cr[idx]);
                    rgba[dstBase + 1] = static_cast<unsigned char>(cg[idx]);
                    rgba[dstBase + 2] = static_cast<unsigned char>(cb[idx]);

                    if (fmt == RGBAImageCompressed::COMPRESSION_DXT1) {
                        rgba[dstBase + 3] = static_cast<unsigned char>(ca[idx]);
                    }
                    else if (fmt == RGBAImageCompressed::COMPRESSION_DXT3) {
                        int alphaShift = (py * 4 + px) * 4;
                        int alphaVal = (src[alphaOffset + alphaShift / 8] >> (alphaShift % 8)) & 0xF;
                        rgba[dstBase + 3] = static_cast<unsigned char>((alphaVal << 4) | alphaVal);
                    }
                    else {
                        int a0 = src[alphaOffset] & 0xFF;
                        int a1 = src[alphaOffset + 1] & 0xFF;
                        int alphaTable[8];
                        buildAlphaTable(a0, a1, alphaTable);
                        unsigned long long alphaBits = readAlphaBits(src, alphaOffset + 2);
                        int aIdx = static_cast<int>((alphaBits >> (3 * (py * 4 + px))) & 0x7ULL);
                        rgba[dstBase + 3] = static_cast<unsigned char>(alphaTable[aIdx]);
                    }
                }
            }

            srcOffset += blockSize;
        }
    }

    return rgba;
}

int OpenGL4RGBAImageCompressedRenderer::toOpenGlInternalFormat(int compressionFormat) {
    if (compressionFormat == RGBAImageCompressed::COMPRESSION_DXT1) {
        return GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
    }
    if (compressionFormat == RGBAImageCompressed::COMPRESSION_DXT3) {
        return GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
    }
    if (compressionFormat == RGBAImageCompressed::COMPRESSION_DXT5) {
        return GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
    }
    return 0;
}

int OpenGL4RGBAImageCompressedRenderer::readUShort(const unsigned char* data, int offset) {
    return ((data[offset] & 0xFF)) | ((data[offset + 1] & 0xFF) << 8);
}

int OpenGL4RGBAImageCompressedRenderer::readInt(const unsigned char* data, int offset) {
    return (data[offset] & 0xFF)
        | ((data[offset + 1] & 0xFF) << 8)
        | ((data[offset + 2] & 0xFF) << 16)
        | ((data[offset + 3] & 0xFF) << 24);
}

unsigned long long OpenGL4RGBAImageCompressedRenderer::readAlphaBits(const unsigned char* data, int offset) {
    unsigned long long v = 0;
    for (int i = 0; i < 6; i++) {
        v |= (static_cast<unsigned long long>(data[offset + i] & 0xFF)) << (8 * i);
    }
    return v;
}

void OpenGL4RGBAImageCompressedRenderer::decodeRgb565(int packed, int& r, int& g, int& b) {
    r = ((packed >> 11) & 0x1F) * 255 / 31;
    g = ((packed >> 5) & 0x3F) * 255 / 63;
    b = (packed & 0x1F) * 255 / 31;
}

void OpenGL4RGBAImageCompressedRenderer::buildAlphaTable(int a0, int a1, int outTable[8]) {
    outTable[0] = a0;
    outTable[1] = a1;
    if (a0 > a1) {
        outTable[2] = (6 * a0 + 1 * a1) / 7;
        outTable[3] = (5 * a0 + 2 * a1) / 7;
        outTable[4] = (4 * a0 + 3 * a1) / 7;
        outTable[5] = (3 * a0 + 4 * a1) / 7;
        outTable[6] = (2 * a0 + 5 * a1) / 7;
        outTable[7] = (1 * a0 + 6 * a1) / 7;
    }
    else {
        outTable[2] = (4 * a0 + 1 * a1) / 5;
        outTable[3] = (3 * a0 + 2 * a1) / 5;
        outTable[4] = (2 * a0 + 3 * a1) / 5;
        outTable[5] = (1 * a0 + 4 * a1) / 5;
        outTable[6] = 0;
        outTable[7] = 255;
    }
}

void OpenGL4RGBAImageCompressedRenderer::disposeAll() {
    for (auto& pair : compiledImages) {
        glDeleteTextures(1, &pair.second);
    }
    compiledImages.clear();
}

#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
RGBImageUncompressed::RGBImageUncompressed() :
    data(NULL), xSize(0), ySize(0), rowStride(0) {
}

RGBImageUncompressed::~RGBImageUncompressed() {
    if (data != NULL) {
        delete[] data;
        data = NULL;
    }
}

void RGBImageUncompressed::detach() {
    if (data != NULL) {
        delete[] data;
        data = NULL;
    }
}

int RGBImageUncompressed::getSizeInBytes() const {
    return xSize * ySize * 3 + 2 * INT_SIZE_IN_BYTES + POINTER_SIZE_IN_BYTES;
}

BOOLEAN RGBImageUncompressed::init(int width, int height) {
    try {
        if (data != NULL) {
            delete[] data;
        }
        data = new char[width * height * BYTES_PER_PIXEL];
        for (int i = 0; i < width * height * BYTES_PER_PIXEL; i++) {
            data[i] = 0;
        }
    } catch (...) {
        data = NULL;
        return FALSE;
    }
    xSize = width;
    ySize = height;
    rowStride = width * BYTES_PER_PIXEL;
    return TRUE;
}

BOOLEAN RGBImageUncompressed::initNoFill(int width, int height) {
    if (data != NULL && width == xSize && height == ySize) {
        return TRUE;
    }

    try {
        if (data != NULL) {
            delete[] data;
        }
        data = new char[width * height * BYTES_PER_PIXEL];
    } catch (...) {
        data = NULL;
        return FALSE;
    }
    xSize = width;
    ySize = height;
    rowStride = width * BYTES_PER_PIXEL;
    return TRUE;
}

int RGBImageUncompressed::pixelBaseIndex(int x, int y) const {
    return (ySize - 1 - y) * rowStride + x * BYTES_PER_PIXEL;
}

void RGBImageUncompressed::putPixel(int x, int y, char r, char g, char b) {
    int index = pixelBaseIndex(x, y);
    data[index] = r;
    data[index + 1] = g;
    data[index + 2] = b;
}

void RGBImageUncompressed::putPixel(int x, int y, const RGBPixel* p) {
    int index = pixelBaseIndex(x, y);
    data[index] = p->r;
    data[index + 1] = p->g;
    data[index + 2] = p->b;
}

void RGBImageUncompressed::putPixelRgb(int x, int y, RGBPixel* p) {
    int index = pixelBaseIndex(x, y);
    data[index] = p->r;
    data[index + 1] = p->g;
    data[index + 2] = p->b;
}

RGBPixel* RGBImageUncompressed::getPixel(int x, int y) const {
    RGBPixel* p = new RGBPixel();
    int index = pixelBaseIndex(x, y);
    p->r = data[index];
    p->g = data[index + 1];
    p->b = data[index + 2];
    return p;
}

RGBPixel* RGBImageUncompressed::getPixelRgb(int x, int y) const {
    RGBPixel* p = new RGBPixel();
    int index = pixelBaseIndex(x, y);
    p->r = data[index];
    p->g = data[index + 1];
    p->b = data[index + 2];
    return p;
}

void RGBImageUncompressed::getPixelRgb(int x, int y, RGBPixel* p) const {
    int index = pixelBaseIndex(x, y);
    p->r = data[index];
    p->g = data[index + 1];
    p->b = data[index + 2];
}

int RGBImageUncompressed::getXSize() const {
    return xSize;
}

int RGBImageUncompressed::getYSize() const {
    return ySize;
}

char* RGBImageUncompressed::getRawImage() const {
    if (data == NULL) {
        return NULL;
    }
    char* copy = new char[xSize * ySize * BYTES_PER_PIXEL];
    for (int i = 0; i < xSize * ySize * BYTES_PER_PIXEL; i++) {
        copy[i] = data[i];
    }
    return copy;
}

void RGBImageUncompressed::setRawImage(int width, int height, char* imageData) {
    xSize = width;
    ySize = height;
    rowStride = width * BYTES_PER_PIXEL;

    if (data != NULL) {
        delete[] data;
    }
    if (imageData != NULL) {
        int size = width * height * BYTES_PER_PIXEL;
        data = new char[size];
        for (int i = 0; i < size; i++) {
            data[i] = imageData[i];
        }
    } else {
        data = NULL;
    }
}

RGBImageUncompressed* RGBImageUncompressed::clone() const {
    RGBImageUncompressed* copy = new RGBImageUncompressed();
    int xxSize = getXSize();
    int yySize = getYSize();
    copy->init(xxSize, yySize);

    for (int x = 0; x < xxSize; x++) {
        for (int y = 0; y < yySize; y++) {
            RGBPixel* pixel = getPixel(x, y);
            copy->putPixel(x, y, pixel);
            delete pixel;
        }
    }
    return copy;
}

RGBAImageUncompressed* RGBImageUncompressed::cloneToRgba() const {
    RGBAImageUncompressed* copy = new RGBAImageUncompressed();
    int xxSize = getXSize();
    int yySize = getYSize();
    copy->init(xxSize, yySize);

    for (int x = 0; x < xxSize; x++) {
        for (int y = 0; y < yySize; y++) {
            RGBPixel* source = getPixel(x, y);
            copy->putPixelA(x, y, source->r, source->g, source->b, (char)255);
            delete source;
        }
    }
    return copy;
}

void RGBImageUncompressed::dispose() {
    Entity::dispose();
}

const Class RGBImageUncompressed::CLASS = { "RGBImageUncompressed", &Image::CLASS };

const Class *
RGBImageUncompressed::getClass() const
{
    return &CLASS;
}

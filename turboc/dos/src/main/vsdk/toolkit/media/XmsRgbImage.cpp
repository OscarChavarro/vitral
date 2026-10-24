#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/media/XmsRgbImage.h"

XmsRgbImage::XmsRgbImage()
    : xSize(0), ySize(0), rowStride(0), imageBytes(0)
{
}

XmsRgbImage::~XmsRgbImage()
{
}

BOOLEAN XmsRgbImage::init(int width, int height)
{
    if ( !initNoFill(width, height) ) {
        return FALSE;
    }
    return storage.clear();
}

BOOLEAN XmsRgbImage::initNoFill(int width, int height)
{
    unsigned long bytes;
    if ( width <= 0 || height <= 0 ) {
        return FALSE;
    }

    rowStride = (unsigned long)width * BYTES_PER_PIXEL;
    bytes = rowStride * (unsigned long)height;
    // Pixel accesses transfer four bytes because XMS Move requires an even
    // length. The guard byte keeps the final RGB triplet inside the block.
    if ( !storage.allocate(bytes + 1UL) ) {
        xSize = ySize = 0;
        rowStride = imageBytes = 0;
        return FALSE;
    }
    xSize = width;
    ySize = height;
    imageBytes = bytes;
    return TRUE;
}

unsigned long XmsRgbImage::pixelOffset(int x, int y) const
{
    return ((unsigned long)(ySize - 1 - y) * rowStride) +
        ((unsigned long)x * BYTES_PER_PIXEL);
}

void XmsRgbImage::putPixelRgb(int x, int y, RGBPixel* pixel)
{
    char bytes[4];
    if ( pixel == 0 || x < 0 || x >= xSize || y < 0 || y >= ySize ) {
        return;
    }
    // XMS Move has an even byte-count requirement. Read the fourth byte
    // first, so a three-byte RGB update never clears the following pixel's
    // red component (the guard byte covers the last pixel of the image).
    if ( !storage.copyFrom(pixelOffset(x, y), bytes, sizeof(bytes)) ) {
        return;
    }
    bytes[0] = pixel->r;
    bytes[1] = pixel->g;
    bytes[2] = pixel->b;
    storage.copyTo(pixelOffset(x, y), bytes, sizeof(bytes));
}

void XmsRgbImage::putPixel(int x, int y, char r, char g, char b)
{
    RGBPixel pixel(r, g, b);
    putPixelRgb(x, y, &pixel);
}

void XmsRgbImage::getPixelRgb(int x, int y, RGBPixel* pixel) const
{
    char bytes[4];
    if ( pixel == 0 || x < 0 || x >= xSize || y < 0 || y >= ySize ||
         !storage.copyFrom(pixelOffset(x, y), bytes, sizeof(bytes)) ) {
        return;
    }
    pixel->r = bytes[0];
    pixel->g = bytes[1];
    pixel->b = bytes[2];
}

RGBPixel* XmsRgbImage::getPixelRgb(int x, int y) const
{
    RGBPixel* pixel = new RGBPixel();
    getPixelRgb(x, y, pixel);
    return pixel;
}

int XmsRgbImage::getXSize() const
{
    return xSize;
}

int XmsRgbImage::getYSize() const
{
    return ySize;
}

int XmsRgbImage::getSizeInBytes() const
{
    // Entity predates 32-bit framebuffers and exposes an int-sized result.
    // Call getByteCount when the exact XMS allocation size is needed.
    return (int)imageBytes;
}

unsigned long XmsRgbImage::getByteCount() const
{
    return imageBytes;
}

BOOLEAN XmsRgbImage::isXmsBacked() const
{
    return storage.isXmsBacked();
}

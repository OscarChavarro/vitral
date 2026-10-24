#ifndef __XMS_RGB_IMAGE__
#define __XMS_RGB_IMAGE__

#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/XmsMemory.h"

class RGBPixel;

/**
24-bit RGB image whose backing store is XMS under Turbo C++.

It is an opt-in DOS implementation of Image.  It does not alter the common
RGBImageUncompressed class used by the modern ports, and its storage is never
exposed as a raw pointer because XMS is not directly addressable in real mode.
*/
class XmsRgbImage : public Image {
private:
    enum { BYTES_PER_PIXEL = 3 };

    XmsMemory storage;
    int xSize;
    int ySize;
    unsigned long rowStride;
    unsigned long imageBytes;

    unsigned long pixelOffset(int x, int y) const;

public:
    XmsRgbImage();
    virtual ~XmsRgbImage();

    virtual BOOLEAN init(int width, int height);
    virtual BOOLEAN initNoFill(int width, int height);
    virtual int getXSize() const;
    virtual int getYSize() const;
    virtual void putPixelRgb(int x, int y, RGBPixel* pixel);
    virtual RGBPixel* getPixelRgb(int x, int y) const;
    virtual void getPixelRgb(int x, int y, RGBPixel* pixel) const;

    void putPixel(int x, int y, char r, char g, char b);
    virtual int getSizeInBytes() const;
    unsigned long getByteCount() const;
    BOOLEAN isXmsBacked() const;
};

#endif

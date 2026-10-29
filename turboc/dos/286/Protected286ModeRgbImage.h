#ifndef VITRAL_PROTECTED286_RGB_IMAGE_H
#define VITRAL_PROTECTED286_RGB_IMAGE_H

#include "vsdk/toolkit/common/VSDK.h"
#include "Protected286Memory.h"
class RGBPixel;

/* Direct selector-addressed storage. Each byte access selects its own
 * 64 KiB block so an RGB triplet may safely straddle a segment boundary. */
class Protected286ModeRgbImage {
    Protected286Word selectors[PROTECTED_286_BLOCK_COUNT];
    unsigned count;
    int width, height;
    unsigned long bytes;
    void release();
    char readByte(unsigned long offset) const;
    void writeByte(unsigned long offset, char value);
public:
    Protected286ModeRgbImage();
    ~Protected286ModeRgbImage();
    BOOLEAN init(int x, int y);
    BOOLEAN initNoFill(int x, int y);
    void putPixel(int x, int y, char r, char g, char b);
    void putPixelRgb(int x, int y, RGBPixel* pixel);
    void getPixelRgb(int x, int y, RGBPixel* pixel) const;
    RGBPixel* getPixelRgb(int x, int y) const;
private:
    Protected286ModeRgbImage(const Protected286ModeRgbImage&);
    Protected286ModeRgbImage& operator=(const Protected286ModeRgbImage&);
};
#endif

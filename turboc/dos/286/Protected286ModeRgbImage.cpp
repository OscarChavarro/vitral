#include <dos.h>
#include "Protected286ModeRgbImage.h"
#include "vsdk/toolkit/media/RGBPixel.h"

Protected286ModeRgbImage::Protected286ModeRgbImage()
    : count(0), width(0), height(0), bytes(0) {}
Protected286ModeRgbImage::~Protected286ModeRgbImage() { release(); }
void Protected286ModeRgbImage::release()
{
    while (count) Protected286Memory::releaseSegment(selectors[--count]);
    width = height = 0; bytes = 0;
}
BOOLEAN Protected286ModeRgbImage::initNoFill(int x, int y)
{
    unsigned needed;
    unsigned long pixels;
    release();
    if (x <= 0 || y <= 0) return FALSE;
    pixels = (unsigned long)x * (unsigned long)y;
    if (pixels > (240UL * 65536UL) / 3UL) return FALSE;
    bytes = pixels * 3UL;
    needed = (unsigned)((bytes + 65535UL) / 65536UL);
    while (count < needed) {
        Protected286Word selector = Protected286Memory::allocateSegment();
        if (!selector) { release(); return FALSE; }
        selectors[count++] = selector;
    }
    width = x; height = y;
    return TRUE;
}
BOOLEAN Protected286ModeRgbImage::init(int x, int y)
{
    unsigned long i;
    if (!initNoFill(x, y)) return FALSE;
    for (i = 0; i < bytes; ++i) writeByte(i, 0);
    return TRUE;
}
char Protected286ModeRgbImage::readByte(unsigned long offset) const
{
    return *(const char far*)MK_FP(selectors[(unsigned)(offset >> 16)], (unsigned)offset);
}
void Protected286ModeRgbImage::writeByte(unsigned long offset, char value)
{
    *(char far*)MK_FP(selectors[(unsigned)(offset >> 16)], (unsigned)offset) = value;
}
void Protected286ModeRgbImage::putPixel(int x, int y, char r, char g, char b)
{
    unsigned long offset;
    if (x < 0 || y < 0 || x >= width || y >= height) return;
    offset = ((unsigned long)(height - 1 - y) * width + x) * 3UL;
    writeByte(offset, r); writeByte(offset + 1, g); writeByte(offset + 2, b);
}
void Protected286ModeRgbImage::putPixelRgb(int x, int y, RGBPixel* p)
{
    if (p) putPixel(x, y, p->r, p->g, p->b);
}
void Protected286ModeRgbImage::getPixelRgb(int x, int y, RGBPixel* p) const
{
    unsigned long offset;
    if (!p || x < 0 || y < 0 || x >= width || y >= height) return;
    offset = ((unsigned long)(height - 1 - y) * width + x) * 3UL;
    p->r = readByte(offset); p->g = readByte(offset + 1); p->b = readByte(offset + 2);
}
RGBPixel* Protected286ModeRgbImage::getPixelRgb(int x, int y) const
{
    RGBPixel* p = new RGBPixel();
    getPixelRgb(x, y, p);
    return p;
}

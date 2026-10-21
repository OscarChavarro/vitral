#ifndef __POLYGON_CLIPPING_HUD_RENDERER__
#define __POLYGON_CLIPPING_HUD_RENDERER__

#include "java/lang/String.h"
class RGBImageUncompressed;

class PolygonClippingHudRenderer {
public:
    PolygonClippingHudRenderer();
    ~PolygonClippingHudRenderer();
    void draw(
        int viewportX, int viewportY, int viewportWidth, int viewportHeight,
        const String& line1, const String& line2, const String& line3,
        const String& line4, const String& line5,
        const String& right1, const String& right2, const String& right3);
    void dispose();
private:
    RGBImageUncompressed* hudImage;
    int hudWidth;
    int hudHeight;
    void ensureHudBuffers(int width, int height);
    void putPixelSafe(int x, int y, unsigned char r, unsigned char g, unsigned char b);
    void clearBlack();
    void drawChar5x7(int x, int y, char c);
    void drawText(int x, int y, const String& text);
};

#endif

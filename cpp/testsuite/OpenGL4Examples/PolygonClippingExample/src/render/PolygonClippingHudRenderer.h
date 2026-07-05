#ifndef __POLYGONCLIPPINGHUDRENDERER__
#define __POLYGONCLIPPINGHUDRENDERER__

#include "java/lang/String.h"
class RGBImageUncompressed;

class PolygonClippingHudRenderer {
public:
    PolygonClippingHudRenderer();
    ~PolygonClippingHudRenderer();
    void draw(
        int viewportX, int viewportY, int viewportWidth, int viewportHeight,
        const java::String& line1, const java::String& line2, const java::String& line3,
        const java::String& line4, const java::String& line5,
        const java::String& right1, const java::String& right2, const java::String& right3);
    void dispose();
private:
    RGBImageUncompressed* hudImage;
    int hudWidth;
    int hudHeight;
    void ensureHudBuffers(int width, int height);
    void putPixelSafe(int x, int y, unsigned char r, unsigned char g, unsigned char b);
    void clearBlack();
    void drawChar5x7(int x, int y, char c);
    void drawText(int x, int y, const java::String& text);
};

#endif

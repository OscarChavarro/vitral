#ifndef __PICTURE__
#define __PICTURE__

#include "vsdk/toolkit/common/VSDK.h"
#include "lib/vgaModes/VgaModes.h"

class HudPatch;

class Picture {
  public:
    explicit Picture(const VgaModes::Screen &screen);

    void draw() const;
    void circle(const HudPatch *patch) const;
    BOOLEAN covers(int x, int y) const;
    BOOLEAN isGraphics() const { return graphics; }
    int getWidth() const { return width; }
    int getHeight() const { return height; }
    int getColors() const { return colors; }
    int getBitsPerPixel() const { return bitsPerPixel; }
    int getWhite() const { return white; }

  private:
    int graphics;
    int width;
    int height;
    int colors;
    int bitsPerPixel;
    int white;
    int square;
    int centerX;
    int centerY;
    int radius;

    void plot(int x, int y, const HudPatch *patch) const;
    void plot8(int x, int y, const HudPatch *patch) const;
    BOOLEAN hits8(int x, int y, int px, int py) const;
    int rows(int v) const;
    int legendHeight() const;
    void drawLegend() const;
    static void fillRect(int x, int y, int w, int h, int color);
};

#endif

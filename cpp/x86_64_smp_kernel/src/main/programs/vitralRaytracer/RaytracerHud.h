#ifndef __RAYTRACER_HUD__
#define __RAYTRACER_HUD__
#include "lib/dithering/Dithering.h"
#include "programs/vitralRaytracer/RaytracerColorMode.h"
#include "programs/vitralRaytracer/RaytracerConstant.h"
#include "programs/vitralRaytracer/Shown.h"

class RaytracerHud {
public:
    RaytracerHud();
    void loadFont();
    bool hasFont() const;
    void toggle();
    void draw(const Shown *shown, RaytracerColorMode colorMode, int width, int height, int text, double seconds) const;

private:
    unsigned char font[FONT_GLYPHS * FONT_SLOT];
    int glyphHeight;
    int visible;

    static void selectColor(const Dithering::Palette *palette, int red, int green, int blue);
    static const char *ditheringName(const Shown *shown, RaytracerColorMode colorMode);
    void drawText(int x, int y, const char *text) const;
};

#endif

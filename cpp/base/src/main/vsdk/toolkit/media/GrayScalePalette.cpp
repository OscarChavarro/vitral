#include "GrayScalePalette.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"

GrayScalePalette::GrayScalePalette() : RGBProceduralColorPalette() {
}

int GrayScalePalette::selectNearestIndexToRgb(const ColorRgb& c) const {
    if (!pure) {
        return RGBProceduralColorPalette::selectNearestIndexToRgb(c);
    }

    double gray = (c.r() + c.g() + c.b()) / 3.0;

    if (gray < 0.0) gray = 0.0;
    if (gray > 1.0) gray = 1.0;

    return (int)(gray * ((double)(colors.size() - 1)));
}

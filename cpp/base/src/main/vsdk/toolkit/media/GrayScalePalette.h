#ifndef __GRAYSCALEPALETTE__
#define __GRAYSCALEPALETTE__

#include "vsdk/toolkit/media/RGBProceduralColorPalette.h"
class ColorRgb;

/**
Represents a linear scale gray palette.
*/
class GrayScalePalette : public RGBProceduralColorPalette {

public:
    GrayScalePalette();
    virtual ~GrayScalePalette() = default;

    int selectNearestIndexToRgb(const ColorRgb& c) const;
};

#endif

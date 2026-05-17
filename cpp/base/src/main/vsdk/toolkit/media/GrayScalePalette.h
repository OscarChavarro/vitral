#ifndef __VSDK_TOOLKIT_MEDIA_GRAYSCALEPALETTE_H__
#define __VSDK_TOOLKIT_MEDIA_GRAYSCALEPALETTE_H__

#include "RGBProceduralColorPalette.h"

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

#endif // __VSDK_TOOLKIT_MEDIA_GRAYSCALEPALETTE_H__

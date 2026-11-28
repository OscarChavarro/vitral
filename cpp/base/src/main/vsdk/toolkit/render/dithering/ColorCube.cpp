#include "vsdk/toolkit/render/dithering/ColorCube.h"
#include "vsdk/toolkit/render/dithering/Dithering.h"

namespace {

int clampChannel(int value)
{
    return value < 0 ? 0 : value > 255 ? 255 : value;
}

}

ColorCube::ColorCube() : redLevels(5), greenLevels(9), blueLevels(5)
{
}

ColorCube::ColorCube(int r, int g, int b)
    : redLevels(clampLevels(r)), greenLevels(clampLevels(g)),
      blueLevels(clampLevels(b))
{
}

int ColorCube::clampLevels(int levels)
{
    return levels < 2 ? 2 : levels > MAX_LEVELS ? MAX_LEVELS : levels;
}

int ColorCube::getRedLevels() const
{
    return redLevels;
}

int ColorCube::getGreenLevels() const
{
    return greenLevels;
}

int ColorCube::getBlueLevels() const
{
    return blueLevels;
}

int ColorCube::getSize() const
{
    return redLevels * greenLevels * blueLevels;
}

int ColorCube::nearestLevel(int value, int levels)
{
    return (clampChannel(value) * (levels - 1) + 127) / 255;
}

/**
The level below the value, or the next one when the fraction of the way to
it is above the threshold (in [0, 63], 64 steps).
*/
int ColorCube::ditheredLevel(int value, int levels, int threshold)
{
    int scaled = clampChannel(value) * (levels - 1);
    int level = scaled / 255;
    if ( level < levels - 1 && (scaled % 255) * 64 > threshold * 255 ) {
        level++;
    }
    return level;
}

int ColorCube::levelValue(int level, int levels)
{
    return level * 255 / (levels - 1);
}

int ColorCube::indexOf(int r, int g, int b) const
{
    return (nearestLevel(r, redLevels) * greenLevels +
            nearestLevel(g, greenLevels)) * blueLevels +
        nearestLevel(b, blueLevels);
}

int ColorCube::indexOfDithered(int r, int g, int b, unsigned x,
                               unsigned y) const
{
    int threshold = Dithering::orderedThreshold(x, y);
    return (ditheredLevel(r, redLevels, threshold) * greenLevels +
            ditheredLevel(g, greenLevels, threshold)) * blueLevels +
        ditheredLevel(b, blueLevels, threshold);
}

int ColorCube::getRedLevel(int index) const
{
    return index / (greenLevels * blueLevels);
}

int ColorCube::getGreenLevel(int index) const
{
    return (index / blueLevels) % greenLevels;
}

int ColorCube::getBlueLevel(int index) const
{
    return index % blueLevels;
}

int ColorCube::getRed(int index) const
{
    return levelValue(getRedLevel(index), redLevels);
}

int ColorCube::getGreen(int index) const
{
    return levelValue(getGreenLevel(index), greenLevels);
}

int ColorCube::getBlue(int index) const
{
    return levelValue(getBlueLevel(index), blueLevels);
}

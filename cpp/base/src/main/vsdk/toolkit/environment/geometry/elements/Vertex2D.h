#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_ELEMENTS_VERTEX2D_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_ELEMENTS_VERTEX2D_H__

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include <string>

class Vertex2D
{
public:
    double x;
    double y;
    ColorRgb color;

    Vertex2D(double x, double y);
    Vertex2D(double x, double y, double r, double g, double b);

    std::string toString() const;
};

#endif

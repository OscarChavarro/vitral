#include "vsdk/toolkit/environment/background/SimpleBackground.h"

SimpleBackground::SimpleBackground() : color_(0, 0, 0)
{
}

ColorRgb SimpleBackground::colorInDireccion(const Vector3Dd&)
{
    return ColorRgb(color_);
}

void SimpleBackground::setColor(double r, double g, double b)
{
    color_ = ColorRgb(r, g, b);
}

#ifndef __SIMPLEBACKGROUND__
#define __SIMPLEBACKGROUND__

#include "vsdk/toolkit/environment/background/Background.h"
class SimpleBackground : public Background {
private:
    ColorRgb color_;

public:
    SimpleBackground();
    virtual ~SimpleBackground() {}

    virtual ColorRgb colorInDireccion(const Vector3Dd& d) override;
    void setColor(double r, double g, double b);
};

#endif

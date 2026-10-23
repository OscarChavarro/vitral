#ifndef __AMBIENT_LIGHT__
#define __AMBIENT_LIGHT__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/light/Light.h"

class AmbientLight : public Light {
  public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit AmbientLight(const ColorRgb &emission);
    AmbientLight(const AmbientLight &other);

    BOOLEAN isAmbient() const;
    void getDirectionAndDistance(const Vector3Dd &surfacePoint,
        Vector3Dd *directionOut, double *maxShadowDistanceOut) const;
    double evaluateLightResponseFactor(const Ray *lightSourceRay) const;
    Light *copy() const;
};

#endif

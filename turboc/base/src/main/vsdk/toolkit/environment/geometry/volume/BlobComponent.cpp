#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobComponent.h"

BlobComponent::BlobComponent() :
    center(Vector3Dd(0, 0, 0)), radius(1.0), strength(1.0), radiusSquared(1.0)
{
    coefficients[2] = 1.0;
    coefficients[1] = -(2.0 * 1.0) / radiusSquared;
    coefficients[0] = 1.0 / (radiusSquared * radiusSquared);
}

BlobComponent::BlobComponent(const Vector3Dd& c, double r, double s) :
    center(c), radius(r), strength(s), radiusSquared(r*r)
{
    coefficients[2] = s;
    coefficients[1] = -(2.0 * s) / radiusSquared;
    coefficients[0] = s / (radiusSquared * radiusSquared);
}

BOOLEAN BlobComponent::isDegenerate() const
{
    return !Double::isFinite(radius) || !Double::isFinite(strength) ||
        !Double::isFinite(center.x()) || !Double::isFinite(center.y()) ||
        !Double::isFinite(center.z()) ||
        Math::abs(strength) < 1.0e-5 || radius < 1.0e-5;
}

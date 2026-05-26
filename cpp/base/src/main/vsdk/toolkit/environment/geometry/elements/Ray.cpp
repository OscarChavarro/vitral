#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "java/lang/String.h"
#include <cmath>
#include "java/lang/String.h"
#include <functional>
#include "java/lang/String.h"

const double Ray::UNIT_DIRECTION_TOLERANCE = 1e-12;

Ray::Ray() : Ray(Vector3Dd(0, 0, 0), Vector3Dd(1, 0, 0), 0.0) {}

Ray::Ray(const Vector3Dd& origin, const Vector3Dd& direction)
    : Ray(origin, direction, 0.0) {}

Ray::Ray(const Vector3Dd& origin, const Vector3Dd& direction, double t)
    : origin_(origin), direction_(normalizeDirection(direction)), t_(t) {}

Ray::Ray(const Ray& b) : origin_(b.origin_), direction_(b.direction_), t_(b.t_) {}

Ray Ray::copyOf(const Ray& other)
{
    return other;
}

Ray Ray::withOrigin(const Vector3Dd& newOrigin) const
{
    return Ray(newOrigin, direction_, t_);
}

Ray Ray::withDirection(const Vector3Dd& newDirection) const
{
    return Ray(origin_, newDirection, t_);
}

Ray Ray::withT(double newT) const
{
    RaytraceStatistics::recordRayWithT();
    if ( newT == t_ ) {
        return *this;
    }
    return Ray(origin_, direction_, newT);
}

const Vector3Dd& Ray::origin() const
{
    return origin_;
}

const Vector3Dd& Ray::direction() const
{
    return direction_;
}

double Ray::t() const
{
    return t_;
}

Vector3Dd Ray::normalizeDirection(const Vector3Dd& direction)
{
    double lengthSquared = direction.dotProduct(direction);
    if ( lengthSquared <= VSDK::EPSILON ) {
        return direction;
    }
    if ( std::abs(lengthSquared - 1.0) <= UNIT_DIRECTION_TOLERANCE ) {
        return direction;
    }
    return direction.multiply(1.0 / std::sqrt(lengthSquared));
}

bool Ray::equals(const Ray& other) const
{
    return t_ == other.t_ && origin_ == other.origin_ && direction_ == other.direction_;
}

int Ray::hashCode() const
{
    std::size_t result = std::hash<double>()(t_);
    result = 31u * result + static_cast<std::size_t>(origin_.hashCode());
    result = 31u * result + static_cast<std::size_t>(direction_.hashCode());
    return static_cast<int>(result);
}

/**
Provides an object to text report conversion, optimized for human
readability and debugging. Do not use for serialization or persistence
purposes.
@return human-readable representation of current Ray
*/
java::String Ray::toString() const
{
    return "Ray Origin: <" + VSDK::formatDouble(origin_.x()) + ", " +
        VSDK::formatDouble(origin_.y()) + ", " +
        VSDK::formatDouble(origin_.z()) + ">; Direction: <" +
        VSDK::formatDouble(direction_.x()) + ", " +
        VSDK::formatDouble(direction_.y()) + ", " +
        VSDK::formatDouble(direction_.z()) + "> T: " +
        VSDK::formatDouble(t_);
}

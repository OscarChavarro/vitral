#ifndef __VSDK_TOOLKIT_COMMON_LINEALALGEBRA_VECTOR2DD_H__
#define __VSDK_TOOLKIT_COMMON_LINEALALGEBRA_VECTOR2DD_H__


#include <cmath>
#include <functional>
#include <stdexcept>
#include "java/lang/String.h"

class Vector2Dd {
    double x_;
    double y_;

public:
    Vector2Dd() : x_(0.0), y_(0.0) {}
    Vector2Dd(double x, double y) : x_(x), y_(y) {}
    Vector2Dd(const Vector2Dd& other) : x_(other.x_), y_(other.y_) {}

    static Vector2Dd copyOf(const Vector2Dd& other) { return Vector2Dd(other); }

    Vector2Dd multiply(double a) const { return Vector2Dd(a * x_, a * y_); }
    double length() const { return std::sqrt(x_ * x_ + y_ * y_); }
    Vector2Dd add(const Vector2Dd& b) const { return Vector2Dd(x_ + b.x_, y_ + b.y_); }

    Vector2Dd withX(double nx) const { return Vector2Dd(nx, y_); }
    Vector2Dd withY(double ny) const { return Vector2Dd(x_, ny); }
    double x() const { return x_; }
    double y() const { return y_; }

    bool epsilonEquals(const Vector2Dd& other) const { return epsilonEquals(other, 1e-6); }
    bool epsilonEquals(const Vector2Dd& other, double epsilon) const {
        if ( epsilon < 0.0 ) throw std::invalid_argument("epsilon must be >= 0");
        return std::abs(x_ - other.x_) <= epsilon && std::abs(y_ - other.y_) <= epsilon;
    }

    bool operator==(const Vector2Dd& other) const { return x_ == other.x_ && y_ == other.y_; }
    bool equals(const Vector2Dd& other) const { return (*this) == other; }
    int hashCode() const {
        std::size_t result = std::hash<double>()(x_);
        result = 31u * result + std::hash<double>()(y_);
        return (int)result;
    }
    java::String* toString() const;
};


#endif // __VSDK_TOOLKIT_COMMON_LINEALALGEBRA_VECTOR2DD_H__

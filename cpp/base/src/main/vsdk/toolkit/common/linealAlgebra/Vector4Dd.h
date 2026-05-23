#ifndef __VSDK_TOOLKIT_COMMON_LINEALALGEBRA_VECTOR4DD_H__
#define __VSDK_TOOLKIT_COMMON_LINEALALGEBRA_VECTOR4DD_H__


#include <cmath>
#include <functional>
#include <stdexcept>
#include "java/lang/String.h"

class Vector3Dd;

class Vector4Dd {
    double x_;
    double y_;
    double z_;
    double w_;

public:
    Vector4Dd(double x, double y, double z, double w) : x_(x), y_(y), z_(z), w_(w) {}
    Vector4Dd(const Vector4Dd& other) : x_(other.x_), y_(other.y_), z_(other.z_), w_(other.w_) {}
    explicit Vector4Dd(const Vector3Dd& other);

    Vector4Dd multiply(double a) const;
    Vector4Dd dividedByW() const;
    double length() const;
    Vector4Dd add(const Vector4Dd& b) const;

    Vector4Dd withX(double nx) const { return Vector4Dd(nx, y_, z_, w_); }
    Vector4Dd withY(double ny) const { return Vector4Dd(x_, ny, z_, w_); }
    Vector4Dd withZ(double nz) const { return Vector4Dd(x_, y_, nz, w_); }
    Vector4Dd withW(double nw) const { return Vector4Dd(x_, y_, z_, nw); }
    double x() const { return x_; }
    double y() const { return y_; }
    double z() const { return z_; }
    double w() const { return w_; }

    bool epsilonEquals(const Vector4Dd& other) const { return epsilonEquals(other, 1e-6); }
    bool epsilonEquals(const Vector4Dd& other, double epsilon) const;

    bool operator==(const Vector4Dd& other) const { return x_ == other.x_ && y_ == other.y_ && z_ == other.z_ && w_ == other.w_; }
    bool equals(const Vector4Dd& other) const { return (*this) == other; }
    int hashCode() const {
        std::size_t result = std::hash<double>()(x_);
        result = 31u * result + std::hash<double>()(y_);
        result = 31u * result + std::hash<double>()(z_);
        result = 31u * result + std::hash<double>()(w_);
        return (int)result;
    }
    java::String* toString() const;
};


#endif // __VSDK_TOOLKIT_COMMON_LINEALALGEBRA_VECTOR4DD_H__

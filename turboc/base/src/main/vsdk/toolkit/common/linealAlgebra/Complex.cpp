#include <math.h>
#include <stdio.h>

#include "java/lang/String.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Complex.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
double Complex::abs() const { return Cpp98Support::hypot(r, i); }
double Complex::phase() const { return atan2(i, r); }
Complex Complex::plus(const Complex& b) const { return Complex(r + b.r, i + b.i); }
Complex Complex::minus(const Complex& b) const { return Complex(r - b.r, i - b.i); }
Complex Complex::times(const Complex& b) const { return Complex(r * b.r - i * b.i, r * b.i + i * b.r); }
Complex Complex::times(double alpha) const { return Complex(alpha * r, alpha * i); }
Complex Complex::conjugate() const { return Complex(r, -i); }
Complex Complex::reciprocal() const { double s = r * r + i * i; return Complex(r / s, -i / s); }
Complex Complex::divides(const Complex& b) const { return times(b.reciprocal()); }
Complex Complex::exp() const { return Complex(::exp(r) * ::cos(i), ::exp(r) * ::sin(i)); }
Complex Complex::sin() const { return Complex(::sin(r) * cosh(i), ::cos(r) * sinh(i)); }
Complex Complex::cos() const { return Complex(::cos(r) * cosh(i), -::sin(r) * sinh(i)); }
Complex Complex::tan() const { return sin().divides(cos()); }
Complex Complex::plus(const Complex& a, const Complex& b) { return Complex(a.r + b.r, a.i + b.i); }

String* Complex::toString() const
{
    char buf[256];
    if ( i < 0 ) {
        snprintf(buf, sizeof(buf), "%s - %si", VSDK::formatDouble(r).c_str(), VSDK::formatDouble(-i).c_str());
        return new String(buf);
    }
    snprintf(buf, sizeof(buf), "%s + %si", VSDK::formatDouble(r).c_str(), VSDK::formatDouble(i).c_str());
    return new String(buf);
}

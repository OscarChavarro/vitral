#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_CURVE_PARAMETRICCURVE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_CURVE_PARAMETRICCURVE_H__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class ParametricCurve {
public:
    static const int HERMITE = 2;
    static const int BEZIER = 4;

    static const Matrix4x4d HERMITE_MATRIX;
    static const Matrix4x4d BEZIER_MATRIX;

    virtual ~ParametricCurve() {}

    virtual const Vector3Dd* getPoint(int idx) const = 0;
    virtual double* getMinMax() const = 0;
};

#endif

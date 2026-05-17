#include "ParametricCurve.h"

static Matrix4x4d buildHermiteMatrix()
{
    const double m[4][4] = {
        { 2, -2,  1,  1},
        {-3,  3, -2, -1},
        { 0,  0,  1,  0},
        { 1,  0,  0,  0}
    };
    return Matrix4x4d::copyOf(m);
}

static Matrix4x4d buildBezierMatrix()
{
    const double m[4][4] = {
        {-1,  3, -3, 1},
        { 3, -6,  3, 0},
        {-3,  3,  0, 0},
        { 1,  0,  0, 0}
    };
    return Matrix4x4d::copyOf(m);
}

const Matrix4x4d ParametricCurve::HERMITE_MATRIX = buildHermiteMatrix();
const Matrix4x4d ParametricCurve::BEZIER_MATRIX = buildBezierMatrix();

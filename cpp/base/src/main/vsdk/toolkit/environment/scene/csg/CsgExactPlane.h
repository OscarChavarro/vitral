#ifndef __CSG_EXACT_PLANE__
#define __CSG_EXACT_PLANE__
class Matrix4x4d;
// Exact sufficient conversion of a local oriented plane to world coefficients.
// Returns false when any binary64 intermediate is not exactly representable.
class CsgExactPlane {
public:
    static bool world(const double* local, const Matrix4x4d& forward, double* result);
};
#endif

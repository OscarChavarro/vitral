#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_PARAMETRICBICUBICPATCH_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_PARAMETRICBICUBICPATCH_H__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
class ParametricCurve;
class Ray;
class RayHit;

class ParametricBiCubicPatch : public Surface {
public:
    Matrix4x4d Gx_MATRIX;
    Matrix4x4d Gy_MATRIX;
    Matrix4x4d Gz_MATRIX;

    static const int FERGUSON = 7;

    /// Note that the contourCurve must have 4 points with its respective
    /// control parameters.
    ParametricCurve* contourCurve;

private:
    Matrix4x4d S_MATRIX;
    Matrix4x4d Tt_MATRIX;
    Matrix4x4d S_MATRIX_DS;
    Matrix4x4d Tt_MATRIX_DT;
    Matrix4x4d M_MATRIX;
    Matrix4x4d Mt_MATRIX;
    Matrix4x4d M_Gx_Mt_MATRIX;
    Matrix4x4d M_Gy_Mt_MATRIX;
    Matrix4x4d M_Gz_Mt_MATRIX;

    Vector3Dd controlMeshPoints[4][4];
    bool hasControlMeshPoints;

    int approximationSteps;

public:
    int type;

    ParametricBiCubicPatch();

    void buildFergusonPatch(ParametricCurve* curve);
    void buildBezierPatch(const Vector3Dd controlMeshPoints[4][4]);

    int getApproximationSteps() const;
    void setApproximationSteps(int n);

    int getType() const;
    void setType(int type);

    void printGeometryMatrices() const;

    void evaluate(Vector3Dd& p, double s, double t);
    Vector3Dd evaluateTangent(double s, double t);
    Vector3Dd evaluateBinormal(double s, double t);
    Vector3Dd evaluateNormal(double s, double t);

    Ray* doIntersection(const Ray& r);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double intT, RayHit* outData);
    virtual double* getMinMax();

private:
    static const int INITIAL_APPROXIMATION_STEPS = 12;

    void calculateMatrices();
    void buildGeometryMatricesXYZ_Bezier();
    void buildGeometryMatricesXYZ_Hermite();
    void buildGeometryMatricesXYZ_Ferguson();
};

#endif

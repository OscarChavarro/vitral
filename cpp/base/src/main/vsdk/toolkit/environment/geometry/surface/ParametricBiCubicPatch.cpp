//= References:                                                             =
//= [WAYN1990] Knapp Wayne. "Ray with Bicubic Patch Intersection Problem",  =
//=            Ray Tracing News, volume 3, number 3, july 13 1990.          =
//=            available at                                                 =
//=         http://jedi.ks.uiuc.edu/~johns/raytracer/rtn/rtnv3n3.html#art19 =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =

#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "vsdk/toolkit/common/VSDK.h"
#include <cstdio>

ParametricBiCubicPatch::ParametricBiCubicPatch()
    : contourCurve(nullptr), hasControlMeshPoints(false),
      approximationSteps(INITIAL_APPROXIMATION_STEPS), type(ParametricCurve::HERMITE)
{
}

void ParametricBiCubicPatch::buildFergusonPatch(ParametricCurve* curve)
{
    contourCurve = curve;
    approximationSteps = INITIAL_APPROXIMATION_STEPS;
    type = FERGUSON;
    calculateMatrices();
}

void ParametricBiCubicPatch::buildBezierPatch(const Vector3Dd inControlMeshPoints[4][4])
{
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            controlMeshPoints[i][j] = inControlMeshPoints[i][j];
        }
    }
    hasControlMeshPoints = true;
    approximationSteps = INITIAL_APPROXIMATION_STEPS;
    type = ParametricCurve::BEZIER;
    calculateMatrices();
}

void ParametricBiCubicPatch::calculateMatrices()
{
    if (type == ParametricCurve::BEZIER) {
        buildGeometryMatricesXYZ_Bezier();
        M_MATRIX = ParametricCurve::BEZIER_MATRIX;
    }
    else if (type == ParametricCurve::HERMITE) {
        buildGeometryMatricesXYZ_Hermite();
        M_MATRIX = ParametricCurve::HERMITE_MATRIX;
    }
    else if (type == FERGUSON) {
        buildGeometryMatricesXYZ_Ferguson();
        M_MATRIX = ParametricCurve::HERMITE_MATRIX;
    }

    Mt_MATRIX = Matrix4x4d(M_MATRIX).transpose();
    M_Gx_Mt_MATRIX = M_MATRIX.multiply(Gx_MATRIX).multiply(Mt_MATRIX);
    M_Gy_Mt_MATRIX = M_MATRIX.multiply(Gy_MATRIX).multiply(Mt_MATRIX);
    M_Gz_Mt_MATRIX = M_MATRIX.multiply(Gz_MATRIX).multiply(Mt_MATRIX);
    S_MATRIX = Matrix4x4d();
    Tt_MATRIX = Matrix4x4d();
    S_MATRIX_DS = Matrix4x4d();
    Tt_MATRIX_DT = Matrix4x4d();
}

int ParametricBiCubicPatch::getApproximationSteps() const { return approximationSteps; }
void ParametricBiCubicPatch::setApproximationSteps(int n) { approximationSteps = n; }
int ParametricBiCubicPatch::getType() const { return type; }
void ParametricBiCubicPatch::setType(int t) { type = t; }

void ParametricBiCubicPatch::buildGeometryMatricesXYZ_Bezier()
{
    double mx[4][4], my[4][4], mz[4][4];
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            const Vector3Dd& vp = controlMeshPoints[i][j];
            mx[i][j] = vp.x();
            my[i][j] = vp.y();
            mz[i][j] = vp.z();
        }
    }
    Gx_MATRIX = Matrix4x4d::copyOf(mx);
    Gy_MATRIX = Matrix4x4d::copyOf(my);
    Gz_MATRIX = Matrix4x4d::copyOf(mz);
}

void ParametricBiCubicPatch::buildGeometryMatricesXYZ_Hermite()
{
    if (contourCurve == nullptr) return;

    double mx[4][4], my[4][4], mz[4][4];

    int p = 0;
    int i = 0;
    for (int j = 0; j < 2; j++) {
        const Vector3Dd* vp = contourCurve->getPoint(p);
        mx[i][j] = vp[0].x(); my[i][j] = vp[0].y(); mz[i][j] = vp[0].z();
        mx[i][j + 2] = vp[2 - j].x(); my[i][j + 2] = vp[2 - j].y(); mz[i][j + 2] = vp[2 - j].z();
        mx[i + 2][j] = vp[1 + j].x(); my[i + 2][j] = vp[1 + j].y(); mz[i + 2][j] = vp[1 + j].z();
        mx[i + 2][j + 2] = 0; my[i + 2][j + 2] = 0; mz[i + 2][j + 2] = 0;
        p++;
    }

    p = 2;
    i = 1;
    for (int j = 0; j < 2; j++) {
        const Vector3Dd* vp = contourCurve->getPoint(p);
        mx[i][j] = vp[0].x(); my[i][j] = vp[0].y(); mz[i][j] = vp[0].z();
        mx[i][j + 2] = vp[j + 1].x(); my[i][j + 2] = vp[j + 1].y(); mz[i][j + 2] = vp[j + 1].z();
        mx[i + 2][j] = vp[2 - j].x(); my[i + 2][j] = vp[2 - j].y(); mz[i + 2][j] = vp[2 - j].z();
        mx[i + 2][j + 2] = 0; my[i + 2][j + 2] = 0; mz[i + 2][j + 2] = 0;
        p++;
    }

    Gx_MATRIX = Matrix4x4d::copyOf(mx);
    Gy_MATRIX = Matrix4x4d::copyOf(my);
    Gz_MATRIX = Matrix4x4d::copyOf(mz);
}

void ParametricBiCubicPatch::printGeometryMatrices() const
{
    double** mx = Gx_MATRIX.toArrayCopy();
    double** my = Gy_MATRIX.toArrayCopy();
    double** mz = Gz_MATRIX.toArrayCopy();

    for (int r = 0; r < 4; r++) {
        std::printf("[ <%.2f, %.2f, %.2f> | <%.2f, %.2f, %.2f> | <%.2f, %.2f, %.2f> | <%.2f, %.2f, %.2f> ]\n",
            mx[r][0], my[r][0], mz[r][0], mx[r][1], my[r][1], mz[r][1],
            mx[r][2], my[r][2], mz[r][2], mx[r][3], my[r][3], mz[r][3]);
    }

    for (int i = 0; i < 4; i++) {
        delete[] mx[i];
        delete[] my[i];
        delete[] mz[i];
    }
    delete[] mx;
    delete[] my;
    delete[] mz;
}

void ParametricBiCubicPatch::buildGeometryMatricesXYZ_Ferguson()
{
    if (contourCurve == nullptr) return;

    double mx[4][4], my[4][4], mz[4][4];
    const Vector3Dd* vp00 = contourCurve->getPoint(0);
    const Vector3Dd* vp10 = contourCurve->getPoint(1);
    const Vector3Dd* vp11 = contourCurve->getPoint(2);
    const Vector3Dd* vp01 = contourCurve->getPoint(3);

    mx[0][0] = vp00[0].x(); my[0][0] = vp00[0].y(); mz[0][0] = vp00[0].z();
    mx[0][1] = vp01[0].x(); my[0][1] = vp01[0].y(); mz[0][1] = vp01[0].z();
    mx[1][0] = vp10[0].x(); my[1][0] = vp10[0].y(); mz[1][0] = vp10[0].z();
    mx[1][1] = vp11[0].x(); my[1][1] = vp11[0].y(); mz[1][1] = vp11[0].z();

    mx[2][0] = (vp00[2].x()); my[2][0] = (vp00[2].y()); mz[2][0] = (vp00[2].z());
    mx[2][1] = -(vp01[1].x()); my[2][1] = -(vp01[1].y()); mz[2][1] = -(vp01[1].z());
    mx[3][0] = (vp10[1].x()); my[3][0] = (vp10[1].y()); mz[3][0] = (vp10[1].z());
    mx[3][1] = -(vp11[2].x()); my[3][1] = -(vp11[2].y()); mz[3][1] = -(vp11[2].z());

    mx[0][2] = -(vp00[1].x()); my[0][2] = -(vp00[1].y()); mz[0][2] = -(vp00[1].z());
    mx[0][3] = -(vp01[2].x()); my[0][3] = -(vp01[2].y()); mz[0][3] = -(vp01[2].z());
    mx[1][2] = (vp10[2].x()); my[1][2] = (vp10[2].y()); mz[1][2] = (vp10[2].z());
    mx[1][3] = (vp11[1].x()); my[1][3] = (vp11[1].y()); mz[1][3] = (vp11[1].z());

    mx[2][2] = 0; my[2][2] = 0; mz[2][2] = 0;
    mx[2][3] = 0; my[2][3] = 0; mz[2][3] = 0;
    mx[3][2] = 0; my[3][2] = 0; mz[3][2] = 0;
    mx[3][3] = 0; my[3][3] = 0; mz[3][3] = 0;

    Gx_MATRIX = Matrix4x4d::copyOf(mx);
    Gy_MATRIX = Matrix4x4d::copyOf(my);
    Gz_MATRIX = Matrix4x4d::copyOf(mz);
}

void ParametricBiCubicPatch::evaluate(Vector3Dd& p, double s, double t)
{
    S_MATRIX = S_MATRIX.withVal(0, 0, s*s*s).withVal(0, 1, s*s).withVal(0, 2, s).withVal(0, 3, 1);
    Tt_MATRIX = Tt_MATRIX.withVal(0, 0, t*t*t).withVal(1, 0, t*t).withVal(2, 0, t).withVal(3, 0, 1);

    Matrix4x4d Qx_MATRIX = S_MATRIX.multiply(M_Gx_Mt_MATRIX).multiply(Tt_MATRIX);
    Matrix4x4d Qy_MATRIX = S_MATRIX.multiply(M_Gy_Mt_MATRIX).multiply(Tt_MATRIX);
    Matrix4x4d Qz_MATRIX = S_MATRIX.multiply(M_Gz_Mt_MATRIX).multiply(Tt_MATRIX);

    p = Vector3Dd(Qx_MATRIX.get(0, 0), Qy_MATRIX.get(0, 0), Qz_MATRIX.get(0, 0));
}

Vector3Dd ParametricBiCubicPatch::evaluateTangent(double s, double t)
{
    S_MATRIX_DS = S_MATRIX_DS.withVal(0, 0, 3*s*s).withVal(0, 1, 2*s).withVal(0, 2, 1).withVal(0, 3, 0);
    Tt_MATRIX = Tt_MATRIX.withVal(0, 0, t*t*t).withVal(1, 0, t*t).withVal(2, 0, t).withVal(3, 0, 1);

    Matrix4x4d Qx_MATRIX = S_MATRIX_DS.multiply(M_Gx_Mt_MATRIX).multiply(Tt_MATRIX);
    Matrix4x4d Qy_MATRIX = S_MATRIX_DS.multiply(M_Gy_Mt_MATRIX).multiply(Tt_MATRIX);
    Matrix4x4d Qz_MATRIX = S_MATRIX_DS.multiply(M_Gz_Mt_MATRIX).multiply(Tt_MATRIX);

    return Vector3Dd(Qx_MATRIX.get(0, 0), Qy_MATRIX.get(0, 0), Qz_MATRIX.get(0, 0)).normalized();
}

Vector3Dd ParametricBiCubicPatch::evaluateBinormal(double s, double t)
{
    S_MATRIX = S_MATRIX.withVal(0, 0, s*s*s).withVal(0, 1, s*s).withVal(0, 2, s).withVal(0, 3, 1);
    Tt_MATRIX_DT = Tt_MATRIX_DT.withVal(0, 0, 3*t*t).withVal(1, 0, 2*t).withVal(2, 0, 1).withVal(3, 0, 0);

    Matrix4x4d Qx_MATRIX = S_MATRIX.multiply(M_Gx_Mt_MATRIX).multiply(Tt_MATRIX_DT);
    Matrix4x4d Qy_MATRIX = S_MATRIX.multiply(M_Gy_Mt_MATRIX).multiply(Tt_MATRIX_DT);
    Matrix4x4d Qz_MATRIX = S_MATRIX.multiply(M_Gz_Mt_MATRIX).multiply(Tt_MATRIX_DT);

    return Vector3Dd(Qx_MATRIX.get(0, 0), Qy_MATRIX.get(0, 0), Qz_MATRIX.get(0, 0)).normalized();
}

Vector3Dd ParametricBiCubicPatch::evaluateNormal(double s, double t)
{
    Vector3Dd dQds = evaluateTangent(s, t);
    Vector3Dd dQdt = evaluateBinormal(s, t);
    return dQds.crossProduct(dQdt).normalized();
}

Ray* ParametricBiCubicPatch::doIntersection(const Ray&)
{
    return nullptr;
}

bool ParametricBiCubicPatch::doIntersection(const Ray&, RayHit*)
{
    return false;
}

void ParametricBiCubicPatch::doExtraInformation(const Ray&, double, RayHit*)
{
}

double* ParametricBiCubicPatch::getMinMax()
{
    if (contourCurve != nullptr) {
        return contourCurve->getMinMax();
    }

    double minX = 1e308, minY = 1e308, minZ = 1e308;
    double maxX = -1e308, maxY = -1e308, maxZ = -1e308;
    double* minMax = new double[6];

    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            const Vector3Dd& p = controlMeshPoints[i][j];
            if (p.x() < minX) minX = p.x();
            if (p.y() < minY) minY = p.y();
            if (p.z() < minZ) minZ = p.z();
            if (p.x() > maxX) maxX = p.x();
            if (p.y() > maxY) maxY = p.y();
            if (p.z() > maxZ) maxZ = p.z();
        }
    }

    minMax[0] = minX; minMax[1] = minY; minMax[2] = minZ;
    minMax[3] = maxX; minMax[4] = maxY; minMax[5] = maxZ;
    return minMax;
}

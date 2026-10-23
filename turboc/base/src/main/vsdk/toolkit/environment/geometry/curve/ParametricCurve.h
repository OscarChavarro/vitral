#ifndef __PARAMETRIC_CURVE__
#define __PARAMETRIC_CURVE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/curve/Curve.h"
class ParametricCurve : public Curve {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    enum {
        BREAK = 1,
        CORNER = 2,
        QUAD = 3,
        HERMITE = 4,
        BEZIER = 5,
        UNRBSPLINE = 6,
        NUNRBSPLINE = 7,
        CATMULLROM = 8
    };

    static const Matrix4x4d LINEAR_MATRIX;
    static const Matrix4x4d HERMITE_MATRIX;
    static const Matrix4x4d BEZIER_MATRIX;
    static const Matrix4x4d UNRBSPLINE_MATRIX;
    static const Matrix4x4d CATMULL_ROM_MATRIX;

    ParametricCurve();
    virtual ~ParametricCurve() {}

    int getApproximationSteps() const;
    void setApproximationSteps(int n);

    void addPoint(const ArrayList<Vector3Dd>& point, int type);
    void addPointAt(const ArrayList<Vector3Dd>& point, int type, int position);
    const ArrayList<Vector3Dd>& getPointVector(int pos) const;
    virtual const Vector3Dd* getPoint(int idx) const;
    int getPointSize() const;
    /**
    Returns the segment type (`BREAK`, `CORNER`, ...) of a control point
    (Java code accesses the public `types` list directly).
    @param pos index of the control point
    @return type associated with the control point
    */
    int getPointType(int pos) const;
    void removePoint(int pos);
    void setPointAt(const ArrayList<Vector3Dd>& p, int pos);

    int getNumPieces() const;

    BOOLEAN evaluate(int endingSegment, double t, Vector3Dd& outPoint) const;
    ArrayList<Vector3Dd> calculatePoints(int endingPointForSegment, BOOLEAN withBrokenRects) const;

    virtual double* getMinMax();
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);

private:
    enum { INITIAL_APPROXIMATION_STEPS = 50 };

    ArrayList< ArrayList<Vector3Dd> > points;
    ArrayList<int> types;
    int approximationSteps;

    BOOLEAN evaluateLinear(int nseg, double t, Vector3Dd& outPoint) const;
    BOOLEAN evaluateQuadratic(int nseg, double t, Vector3Dd& outPoint) const;
    BOOLEAN evaluateHermite(int nseg, double t, Vector3Dd& outPoint) const;
    BOOLEAN evaluateBezier(int nseg, double t, Vector3Dd& outPoint) const;
    BOOLEAN evaluateBspline(int nseg, double t, Vector3Dd& outPoint) const;

    int calculatePointPosition(int pin) const;
    static double matrixTerm(const Matrix4x4d& m, int col, double t);
};

#endif

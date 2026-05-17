#include "ParametricCurve.h"

#include <cmath>
#include <limits>

static Matrix4x4d buildLinearMatrix()
{
    const double m[4][4] = {
        { 0.0,  0.0,  0.0,  1.0},
        { 0.0,  0.0,  1.0,  1.0},
        {-1.0,  1.0,  0.0,  0.0},
        { 1.0,  0.0,  0.0,  0.0}
    };
    return Matrix4x4d::copyOf(m);
}

static Matrix4x4d buildHermiteMatrix()
{
    const double m[4][4] = {
        { 2.0, -2.0,  1.0,  1.0},
        {-3.0,  3.0, -2.0, -1.0},
        { 0.0,  0.0,  1.0,  0.0},
        { 1.0,  0.0,  0.0,  0.0}
    };
    return Matrix4x4d::copyOf(m);
}

static Matrix4x4d buildBezierMatrix()
{
    const double m[4][4] = {
        {-1.0,  3.0, -3.0, 1.0},
        { 3.0, -6.0,  3.0, 0.0},
        {-3.0,  3.0,  0.0, 0.0},
        { 1.0,  0.0,  0.0, 0.0}
    };
    return Matrix4x4d::copyOf(m);
}

static Matrix4x4d buildUnrbSplineMatrix()
{
    const double m[4][4] = {
        {-1.0,  3.0, -3.0, 1.0},
        { 3.0, -6.0,  3.0, 0.0},
        {-3.0,  0.0,  3.0, 0.0},
        { 1.0,  4.0,  1.0, 0.0}
    };
    return Matrix4x4d::copyOf(m);
}

static Matrix4x4d buildCatmullRomMatrix()
{
    const double m[4][4] = {
        {-0.5,  1.5, -1.5,  0.5},
        { 1.0, -2.5,  2.0, -0.5},
        {-0.5,  0.0,  0.5,  0.0},
        { 0.0,  1.0,  0.0,  0.0}
    };
    return Matrix4x4d::copyOf(m);
}

const Matrix4x4d ParametricCurve::LINEAR_MATRIX = buildLinearMatrix();
const Matrix4x4d ParametricCurve::HERMITE_MATRIX = buildHermiteMatrix();
const Matrix4x4d ParametricCurve::BEZIER_MATRIX = buildBezierMatrix();
const Matrix4x4d ParametricCurve::UNRBSPLINE_MATRIX = buildUnrbSplineMatrix();
const Matrix4x4d ParametricCurve::CATMULL_ROM_MATRIX = buildCatmullRomMatrix();

ParametricCurve::ParametricCurve()
    : approximationSteps(INITIAL_APPROXIMATION_STEPS)
{
}

int ParametricCurve::getApproximationSteps() const
{
    return approximationSteps;
}

void ParametricCurve::setApproximationSteps(int n)
{
    approximationSteps = n;
}

void ParametricCurve::addPoint(const std::vector<Vector3Dd>& point, int type)
{
    if ( type == BREAK && points.empty() ) {
        return;
    }
    points.push_back(point);
    types.push_back(type);
}

void ParametricCurve::addPointAt(const std::vector<Vector3Dd>& point, int type, int position)
{
    if ( type == BREAK && position < 1 ) {
        return;
    }
    points.insert(points.begin() + position, point);
    types.insert(types.begin() + position, type);
}

const std::vector<Vector3Dd>& ParametricCurve::getPointVector(int pos) const
{
    return points[pos];
}

const Vector3Dd* ParametricCurve::getPoint(int idx) const
{
    if ( idx < 0 || idx >= static_cast<int>(points.size()) || points[idx].empty() ) {
        return nullptr;
    }
    return &points[idx][0];
}

int ParametricCurve::getPointSize() const
{
    return static_cast<int>(points.size());
}

void ParametricCurve::removePoint(int pos)
{
    points.erase(points.begin() + pos);
    types.erase(types.begin() + pos);
}

void ParametricCurve::setPointAt(const std::vector<Vector3Dd>& p, int pos)
{
    points[pos] = p;
}

int ParametricCurve::getNumPieces() const
{
    int sum = 1;
    for (size_t i = 1; i < types.size(); ++i) {
        if ( types[i] == BREAK ) {
            sum++;
        }
    }
    return sum;
}

double ParametricCurve::matrixTerm(const Matrix4x4d& m, int col, double t)
{
    double vt = 0.0;
    for ( int i = 0; i < 4; ++i ) {
        vt += m.get(i, col) * std::pow(t, 3 - i);
    }
    return vt;
}

bool ParametricCurve::evaluate(int endingSegment, double t, Vector3Dd& outPoint) const
{
    if ( endingSegment < 0 || endingSegment >= static_cast<int>(types.size()) ) {
        return false;
    }

    if ( types[endingSegment] == CORNER ) return evaluateLinear(endingSegment, t, outPoint);
    if ( types[endingSegment] == QUAD ) return evaluateQuadratic(endingSegment, t, outPoint);
    if ( types[endingSegment] == HERMITE ) return evaluateHermite(endingSegment, t, outPoint);
    if ( types[endingSegment] == BEZIER ) return evaluateBezier(endingSegment, t, outPoint);
    if ( types[endingSegment] == UNRBSPLINE ) return evaluateBspline(endingSegment, t, outPoint);

    return false;
}

bool ParametricCurve::evaluateLinear(int nseg, double t, Vector3Dd& outPoint) const
{
    if ( nseg <= 0 || nseg >= static_cast<int>(points.size()) || points[nseg - 1].empty() || points[nseg].empty() ) {
        return false;
    }

    const Vector3Dd& p1 = points[nseg - 1][0];
    const Vector3Dd& p2 = points[nseg][0];

    Vector3Dd result = p1.multiply(matrixTerm(LINEAR_MATRIX, 0, t));
    result = result.add(p2.multiply(matrixTerm(LINEAR_MATRIX, 1, t)));
    outPoint = result;
    return true;
}

bool ParametricCurve::evaluateQuadratic(int nseg, double t, Vector3Dd& outPoint) const
{
    if ( nseg <= 0 || nseg >= static_cast<int>(points.size()) || points[nseg - 1].empty() || points[nseg].size() < 2 ) {
        return false;
    }

    const Vector3Dd& qp0 = points[nseg - 1][0];
    const Vector3Dd& qp1 = points[nseg][1];
    const Vector3Dd& qp2 = points[nseg][0];

    Vector3Dd result = qp0.multiply(matrixTerm(BEZIER_MATRIX, 0, t));
    Vector3Dd p1 = qp0.add(qp1.subtract(qp0).multiply(2.0 / 3.0));
    result = result.add(p1.multiply(matrixTerm(BEZIER_MATRIX, 1, t)));

    Vector3Dd p2 = qp1.add(qp2.subtract(qp0).multiply(1.0 / 3.0));
    result = result.add(p2.multiply(matrixTerm(BEZIER_MATRIX, 2, t)));
    result = result.add(qp2.multiply(matrixTerm(BEZIER_MATRIX, 3, t)));

    outPoint = result;
    return true;
}

bool ParametricCurve::evaluateHermite(int nseg, double t, Vector3Dd& outPoint) const
{
    if ( nseg <= 0 || nseg >= static_cast<int>(points.size()) || points[nseg - 1].size() < 3 || points[nseg].size() < 2 ) {
        return false;
    }

    const std::vector<Vector3Dd>& start = points[nseg - 1];
    const std::vector<Vector3Dd>& end = points[nseg];

    Vector3Dd result = start[0].multiply(matrixTerm(HERMITE_MATRIX, 0, t));
    result = result.add(end[0].multiply(matrixTerm(HERMITE_MATRIX, 1, t)));
    result = result.add(start[2].multiply(matrixTerm(HERMITE_MATRIX, 2, t)));
    result = result.add(end[1].multiply(matrixTerm(HERMITE_MATRIX, 3, t)));

    outPoint = result;
    return true;
}

bool ParametricCurve::evaluateBezier(int nseg, double t, Vector3Dd& outPoint) const
{
    if ( nseg <= 0 || nseg >= static_cast<int>(points.size()) || points[nseg - 1].size() < 3 || points[nseg].size() < 2 ) {
        return false;
    }

    const std::vector<Vector3Dd>& start = points[nseg - 1];
    const std::vector<Vector3Dd>& end = points[nseg];

    Vector3Dd result = start[0].multiply(matrixTerm(BEZIER_MATRIX, 0, t));
    result = result.add(start[2].multiply(matrixTerm(BEZIER_MATRIX, 1, t)));
    result = result.add(end[1].multiply(matrixTerm(BEZIER_MATRIX, 2, t)));
    result = result.add(end[0].multiply(matrixTerm(BEZIER_MATRIX, 3, t)));

    outPoint = result;
    return true;
}

bool ParametricCurve::evaluateBspline(int nseg, double t, Vector3Dd& outPoint) const
{
    if ( points.size() < 4 ) {
        return false;
    }

    Vector3Dd result(0.0, 0.0, 0.0);
    for ( int np = 0; np < 4; ++np ) {
        int idx = nseg - np;
        if ( idx < 0 || idx >= static_cast<int>(points.size()) || points[idx].empty() ) {
            return false;
        }
        double vt = matrixTerm(UNRBSPLINE_MATRIX, np, t);
        result = result.add(points[idx][0].multiply(vt / 6.0));
    }

    outPoint = result;
    return true;
}

int ParametricCurve::calculatePointPosition(int pin) const
{
    int pout = 0;
    for ( int i = 0; i < static_cast<int>(types.size()) && i < pin; ++i ) {
        if ( types[i] == BREAK ) {
            pout = -1;
        }
        else {
            pout++;
        }
    }
    return pout;
}

std::vector<Vector3Dd> ParametricCurve::calculatePoints(int endingPointForSegment, bool withBrokenRects) const
{
    std::vector<Vector3Dd> pol;

    if ( endingPointForSegment <= 0 || endingPointForSegment >= static_cast<int>(types.size()) ) {
        return pol;
    }

    int relativePoint = calculatePointPosition(endingPointForSegment);
    int t = types[endingPointForSegment];

    if ( ((relativePoint <= 2) && t == UNRBSPLINE) || relativePoint < 0 ) {
        return pol;
    }

    if ( ((t == CORNER) && !withBrokenRects) ||
         relativePoint <= 0 ||
         (t == UNRBSPLINE && relativePoint < 3) ) {
        if ( !points[endingPointForSegment - 1].empty() ) pol.push_back(points[endingPointForSegment - 1][0]);
        if ( !points[endingPointForSegment].empty() ) pol.push_back(points[endingPointForSegment][0]);
        return pol;
    }

    Vector3Dd q;
    if ( !evaluate(endingPointForSegment, 0.0, q) ) {
        return pol;
    }
    pol.push_back(q);

    for ( int i = 1; i <= approximationSteps; ++i ) {
        if ( evaluate(endingPointForSegment, static_cast<double>(i) / static_cast<double>(approximationSteps), q) ) {
            pol.push_back(q);
        }
    }

    return pol;
}

double* ParametricCurve::getMinMax()
{
    double* minmax = new double[6];
    minmax[0] = minmax[1] = minmax[2] = std::numeric_limits<double>::max();
    minmax[3] = minmax[4] = minmax[5] = std::numeric_limits<double>::lowest();

    for ( int i = 1; i < static_cast<int>(types.size()); ++i ) {
        std::vector<Vector3Dd> polyline = calculatePoints(i, false);
        for ( size_t j = 0; j < polyline.size(); ++j ) {
            const Vector3Dd& p = polyline[j];
            if ( p.x() < minmax[0] ) minmax[0] = p.x();
            if ( p.y() < minmax[1] ) minmax[1] = p.y();
            if ( p.z() < minmax[2] ) minmax[2] = p.z();
            if ( p.x() > minmax[3] ) minmax[3] = p.x();
            if ( p.y() > minmax[4] ) minmax[4] = p.y();
            if ( p.z() > minmax[5] ) minmax[5] = p.z();
        }
    }

    return minmax;
}

int ParametricCurve::doContainmentTest(const Vector3Dd& p, double distanceTolerance)
{
    for ( int i = 1; i < static_cast<int>(types.size()); ++i ) {
        if ( types[i] == BREAK ) {
            i++;
            continue;
        }

        std::vector<Vector3Dd> polyline = calculatePoints(i, false);
        for ( size_t j = 0; j < polyline.size(); ++j ) {
            if ( polyline[j].subtract(p).length() < distanceTolerance ) {
                return LIMIT;
            }
        }
    }

    return OUTSIDE;
}

#include <stddef.h>
#include "vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.h"
#include <math.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/slicing/_PolyhedralBoundedSolidSplitter.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "java/util/concurrent/locks/ReentrantLock.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SolidOperationFailedException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperandAligner.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"


// Weld tolerance for glyph/poly-line simplification, expressed as a
// fraction of the contour bounding-box diagonal.
static const double GLYPH_WELD_RELATIVE_FACTOR = 1.0e-2;

class BoundaryRepresentationFromCurveBuildState {
public:
    PolyhedralBoundedSolid* solid;
    BOOLEAN firstLoop;
    BOOLEAN beginningOfLoop;
    int nextVertexId;
    int lastLoopStartVertexId;
    int nextFaceId;
    Vector3Dd firstPointInLoop;
    Vector3Dd lastAcceptedPoint;
    BOOLEAN hasLastAcceptedPoint;
    double weldEpsilon;
    int verticesInCurrentLoop;

    BoundaryRepresentationFromCurveBuildState()
        : solid(new PolyhedralBoundedSolid()), firstLoop(TRUE),
          beginningOfLoop(TRUE), nextVertexId(1), lastLoopStartVertexId(1),
          nextFaceId(1), hasLastAcceptedPoint(FALSE),
          weldEpsilon(PolyhedralBoundedSolidNumericPolicy::BREP_BIG_EPSILON),
          verticesInCurrentLoop(0)
    {
    }
};

static double distance(const Vector3Dd& a, const Vector3Dd& b)
{
    return a.subtract(b).length();
}

static void findWireSweepEnds(PolyhedralBoundedSolid* solid,
                       _PolyhedralBoundedSolidHalfEdge** outFirst,
                       _PolyhedralBoundedSolidHalfEdge** outLast)
{
    _PolyhedralBoundedSolidHalfEdge* first;
    _PolyhedralBoundedSolidHalfEdge* last;

    first = solid->getPolygonsList().get(0)->boundariesList.get(0)
        ->boundaryStartHalfEdge;
    while ( first->parentEdge != first->next()->parentEdge ) {
        first = first->next();
    }
    last = first->next();
    while ( last->parentEdge != last->next()->parentEdge ) {
        last = last->next();
    }
    *outFirst = first;
    *outLast = last;
}

static BOOLEAN isOnXAxis(const Vector3Dd& p, double tolerance)
{
    return fabs(p.y()) <= tolerance && fabs(p.z()) <= tolerance;
}

static void collapseFaceToAxisVertex(_PolyhedralBoundedSolidFace* face, double x)
{
    if ( face == NULL ) {
        return;
    }

    int i;
    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        if ( he == NULL ) {
            continue;
        }
        do {
            he->startingVertex->position = Vector3Dd(x, 0.0, 0.0);
            he = he->next();
        } while ( he != NULL && he != start );
    }
}

static BOOLEAN isBreakMarker(ParametricCurve* curve, int segmentIndex)
{
    return curve->getPointType(segmentIndex) == ParametricCurve::BREAK;
}

static ArrayList<Vector3Dd> sampleCurveSegment(ParametricCurve* curve,
                                              int segmentIndex)
{
    // Approximate one parametric segment as a polyLine.
    return curve->calculatePoints(segmentIndex, FALSE);
}

static void startLoopWithSeedPoint(BoundaryRepresentationFromCurveBuildState& state,
                            const Vector3Dd& point)
{
    state.beginningOfLoop = FALSE;
    if ( state.firstLoop ) {
        // [MANT1988] 12.2: first contour starts with MVFS.
        PolyhedralBoundedSolidEulerOperators::mvfs(state.solid, point,
            state.nextVertexId, state.nextFaceId);
        state.nextVertexId++;
        state.nextFaceId++;
    }
    else {
        // Additional contours are connected and converted into rings.
        PolyhedralBoundedSolidEulerOperators::smev(state.solid, 1,
            state.nextVertexId-1, state.nextVertexId, point);
        state.nextVertexId++;
        PolyhedralBoundedSolidEulerOperators::kemr(state.solid, 1, 1,
            state.nextVertexId-2, state.nextVertexId-1,
            state.nextVertexId-1, state.nextVertexId-2);
        state.lastLoopStartVertexId = state.nextVertexId-1;
    }

    state.firstPointInLoop = point;
    state.lastAcceptedPoint = point;
    state.hasLastAcceptedPoint = TRUE;
    state.verticesInCurrentLoop = 1;
}

static BOOLEAN shouldAcceptPolyLinePoint(
    const BoundaryRepresentationFromCurveBuildState& state,
    const Vector3Dd& point)
{
    return distance(point, state.lastAcceptedPoint) > state.weldEpsilon &&
        distance(point, state.firstPointInLoop) > state.weldEpsilon;
}

static void appendPointToCurrentLoop(BoundaryRepresentationFromCurveBuildState& state,
                              const Vector3Dd& point)
{
    PolyhedralBoundedSolidEulerOperators::smev(state.solid, 1,
        state.nextVertexId-1, state.nextVertexId, point);
    state.nextVertexId++;
    state.lastAcceptedPoint = point;
    state.hasLastAcceptedPoint = TRUE;
    state.verticesInCurrentLoop++;
}

static void processSampledSegment(BoundaryRepresentationFromCurveBuildState& state,
                           const ArrayList<Vector3Dd>& polyline)
{
    int j;
    for ( j = 0; j < polyline.size(); j++ ) {
        Vector3Dd point = polyline.get(j);
        if ( state.beginningOfLoop ) {
            startLoopWithSeedPoint(state, point);
        }
        else if ( shouldAcceptPolyLinePoint(state, point) ) {
            appendPointToCurrentLoop(state, point);
        }
    }
}

static void closeLoopWithMef(BoundaryRepresentationFromCurveBuildState& state)
{
    if ( state.verticesInCurrentLoop < 3 ) {
        Logger::reportMessage(String(), Logger::WARNING,
            "closeLoopWithMef",
            String("Degenerate glyph loop with ") +
            String::valueOf(state.verticesInCurrentLoop) +
            " distinct vertices after welding; result may be invalid.");
    }

    // [MANT1988] 12.2: close current wire by creating the face boundary.
    PolyhedralBoundedSolidEulerOperators::mef(state.solid, 1, 1,
        state.lastLoopStartVertexId, state.lastLoopStartVertexId+1,
        state.nextVertexId-1, state.nextVertexId-2, state.nextFaceId);
    state.nextFaceId++;

    if ( !state.firstLoop ) {
        // For inner contours, merge ring into the first face.
        PolyhedralBoundedSolidEulerOperators::kfmrh(state.solid, 2,
            state.nextFaceId-1);
    }
    state.firstLoop = FALSE;
    state.beginningOfLoop = TRUE;
    state.hasLastAcceptedPoint = FALSE;
    state.verticesInCurrentLoop = 0;
}


void PolyhedralBoundedSolidModeler::applyTransformation(
    PolyhedralBoundedSolid* solid, const Matrix4x4d& transformation)
{
    if ( solid == NULL ) {
        Logger::reportMessage("PolyhedralBoundedSolidModeler",
            Logger::WARNING, "applyTransformation",
            "Null solid given, transformation ignored.");
        return;
    }
    int i;
    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* vertex =
            solid->getVerticesList().get(i);
        vertex->position = transformation.multiply(vertex->position);
    }
}

void PolyhedralBoundedSolidModeler::rotationalSweepExtrudeWireAroundXAxis(
    PolyhedralBoundedSolid* solid, int numberOfFaces)
{
    if ( solid == NULL || solid->getPolygonsList().size() < 1 ||
         numberOfFaces < 3 ) {
        return;
    }

    _PolyhedralBoundedSolidHalfEdge* first;
    _PolyhedralBoundedSolidHalfEdge* last;
    findWireSweepEnds(solid, &first, &last);
    _PolyhedralBoundedSolidFace* headf = solid->getPolygonsList().get(0);

    double axisTolerance = VSDK::EPSILON * 100.0;
    Vector3Dd firstEndpointPosition(first->next()->startingVertex->position);
    Vector3Dd lastEndpointPosition(last->startingVertex->position);
    BOOLEAN firstEndpointOnAxis = isOnXAxis(firstEndpointPosition,
        axisTolerance);
    BOOLEAN lastEndpointOnAxis = isOnXAxis(lastEndpointPosition,
        axisTolerance);

    _PolyhedralBoundedSolidHalfEdge* cfirst;
    _PolyhedralBoundedSolidHalfEdge* scan = NULL;
    _PolyhedralBoundedSolidFace* tailf;
    Vector3Dd v;
    Matrix4x4d rotation;

    cfirst = first;
    rotation = rotation.axisRotation((2*M_PI) / numberOfFaces, 1, 0, 0);

    int i;
    for ( i = 0; i < numberOfFaces-1; i++ ) {
        v = rotation.multiply(cfirst->next()->startingVertex->position);
        PolyhedralBoundedSolidEulerOperators::lmev(solid, cfirst->next(),
            cfirst->next(), solid->getMaxVertexId()+1, v);
        scan = cfirst->next();

        while ( scan != last->next() ) {
            v = rotation.multiply(scan->previous()->startingVertex->position);
            PolyhedralBoundedSolidEulerOperators::lmev(solid,
                scan->previous(), scan->previous(),
                solid->getMaxVertexId()+1, v);
            PolyhedralBoundedSolidEulerOperators::lmef(solid,
                scan->previous()->previous(), scan->next(),
                solid->getMaxFaceId()+1);
            scan = (scan->next()->next())->mirrorHalfEdge();
        }
        last = scan;
        cfirst = (cfirst->next()->next())->mirrorHalfEdge();
    }

    tailf = PolyhedralBoundedSolidEulerOperators::lmef(solid, cfirst->next(),
        first->mirrorHalfEdge(), solid->getMaxFaceId()+1);
    while ( cfirst != scan ) {
        PolyhedralBoundedSolidEulerOperators::lmef(solid, cfirst,
            cfirst->next()->next()->next(), solid->getMaxFaceId()+1);
        cfirst = (cfirst->previous())->mirrorHalfEdge()->previous();
    }

    // [MANT1988] 12.2: if a profile endpoint lies on the rotation axis,
    // cap vertices collapse to a single pole instead of leaving a
    // degenerate ring of coincident points.
    if ( firstEndpointOnAxis ) {
        collapseFaceToAxisVertex(headf, firstEndpointPosition.x());
    }
    if ( lastEndpointOnAxis ) {
        collapseFaceToAxisVertex(tailf, lastEndpointPosition.x());
    }
    if ( firstEndpointOnAxis || lastEndpointOnAxis ) {
        PolyhedralBoundedSolidTopologyEditing::maximizeFaces(solid);
    }

    PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
}

PolyhedralBoundedSolid*
PolyhedralBoundedSolidModeler::createBrepFromParametricCurve(
    ParametricCurve* curve)
{
    int i;
    BoundaryRepresentationFromCurveBuildState state;
    double* minMax = curve->getMinMax();
    double dx;
    double dy;
    double dz;
    double bboxDiagonal;

    if ( minMax == NULL ) {
        Logger::reportMessage(String(), Logger::WARNING,
            "createBrepFromParametricCurve",
            "Glyph bbox unavailable, falling back to BREP_BIG_EPSILON weld.");
    }
    else {
        dx = minMax[3] - minMax[0];
        dy = minMax[4] - minMax[1];
        dz = minMax[5] - minMax[2];
        bboxDiagonal = sqrt(dx * dx + dy * dy + dz * dz);
        if ( Cpp98Support::isFinite(bboxDiagonal) && bboxDiagonal > 0.0 ) {
            state.weldEpsilon = Cpp98Support::fmax(
                PolyhedralBoundedSolidNumericPolicy::BREP_BIG_EPSILON,
                GLYPH_WELD_RELATIVE_FACTOR * bboxDiagonal);
        }
        else {
            Logger::reportMessage(String(), Logger::WARNING,
                "createBrepFromParametricCurve",
                "Glyph bbox degenerate, falling back to BREP_BIG_EPSILON weld.");
        }
        delete[] minMax;
    }

    for ( i = 1; i < curve->getPointSize(); i++ ) {
        if ( isBreakMarker(curve, i) ) {
            i++;
            closeLoopWithMef(state);
            continue;
        }
        processSampledSegment(state, sampleCurveSegment(curve, i));
    }

    closeLoopWithMef(state);
    PolyhedralBoundedSolidValidationEngine::validateIntermediate(
        state.solid);
    return state.solid;
}

/**
Serializes the public set operations and splits: the kernel keeps
per-call state in static fields (Fable5.2Analysis.md Stage F step 3).
C++ port note: Java synchronizes on a lock object; here a recursive lock
held by a guard object.
*/
static ReentrantLock& kernelLock()
{
    static ReentrantLock lock;
    return lock;
}

void PolyhedralBoundedSolidModeler::split(
    PolyhedralBoundedSolid* inSolid,
    const InfinitePlane& inSplittingPlane,
    ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
    ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow)
{
    ReentrantLockGuard guard(kernelLock());
#ifndef VSDK_WITHOUT_EXCEPTIONS
    try {
#endif
        _PolyhedralBoundedSolidSplitter::split(inSolid,
            inSplittingPlane, outSolidsAbove, outSolidsBelow);
#ifndef VSDK_WITHOUT_EXCEPTIONS
    }
    catch ( const VSDKFatalException& e ) {
        String message = String("Split failed: ") + e.getMessage();
        throw SolidOperationFailedException(message, e);
    }
#endif
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModeler::setOp(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    int op,
    BOOLEAN withDebug,
    BOOLEAN maximizeResultFaces,
    BOOLEAN doStrictValidation)
{
    ReentrantLockGuard guard(kernelLock());
    PolyhedralBoundedSolid* result;
    PolyhedralBoundedSolid* copyA;
    PolyhedralBoundedSolid* copyB;

    PolyhedralBoundedSolidStatistics::recordSetOpCall(op);
    copyA = PolyhedralBoundedSolidStructuralCopy::copy(inSolidA);
    copyB = PolyhedralBoundedSolidStructuralCopy::copy(inSolidB);
    // C++ port note: the copies are released here once the result is
    // built, or when the operation throws; the result owns its own nodes
    // (see the set operator).
#ifndef VSDK_WITHOUT_EXCEPTIONS
    try {
#endif
        result = runSetOp(copyA, copyB, op, withDebug, maximizeResultFaces,
            doStrictValidation);
#ifndef VSDK_WITHOUT_EXCEPTIONS
    }
    catch ( ... ) {
        delete copyA;
        delete copyB;
        throw;
    }
#endif
    delete copyA;
    delete copyB;
    if ( isEmptyResult(result) &&
         resultCannotBeEmpty(inSolidA, inSolidB, op) ) {
        String path = "null";
        const String* lastPath = getLastSetOpResultPath();
        if ( lastPath != NULL ) {
            path = *lastPath;
        }
        String message = "Boolean operation lost the solid: op=" +
            operationName(op) + " returned an empty solid but " +
            "the operands prove the result is not empty (path=" +
            path + ")";
        delete result;
        throw SolidOperationFailedException(message);
    }
    return result;
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModeler::setOpDestructive(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    int op,
    BOOLEAN withDebug,
    BOOLEAN maximizeResultFaces,
    BOOLEAN doStrictValidation)
{
    ReentrantLockGuard guard(kernelLock());

    PolyhedralBoundedSolidStatistics::recordSetOpCall(op);
    return runSetOp(inSolidA, inSolidB, op, withDebug,
        maximizeResultFaces, doStrictValidation);
}

BOOLEAN PolyhedralBoundedSolidModeler::isEmptyResult(
    PolyhedralBoundedSolid* result)
{
    return result != NULL && result->getPolygonsList().size() == 0;
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidModeler::runSetOp(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    int op,
    BOOLEAN withDebug,
    BOOLEAN maximizeResultFaces,
    BOOLEAN doStrictValidation)
{
#ifndef VSDK_WITHOUT_EXCEPTIONS
    try {
#endif
        return _PolyhedralBoundedSolidSetOperator::setOp(inSolidA,
            inSolidB, op, withDebug, maximizeResultFaces,
            doStrictValidation);
#ifndef VSDK_WITHOUT_EXCEPTIONS
    }
    catch ( const VSDKFatalException& e ) {
        String path = "null";
        const String* lastPath = getLastSetOpResultPath();
        if ( lastPath != NULL ) {
            path = *lastPath;
        }
        String message = "Boolean operation failed: op=" +
            operationName(op) + " (path=" + path + "): " + e.getMessage();
        throw SolidOperationFailedException(message, e);
    }
#endif
}

/**
Witnesses that the regularized result of `op` on the intact operands
is not empty. Only sufficient conditions are used, so false means
"could be empty", never "is empty".
*/
BOOLEAN PolyhedralBoundedSolidModeler::resultCannotBeEmpty(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    int op)
{
    BOOLEAN emptyA = inSolidA == NULL || isEmptyResult(inSolidA);
    BOOLEAN emptyB = inSolidB == NULL || isEmptyResult(inSolidB);
    PolyhedralBoundedSolid* copyA = NULL;
    PolyhedralBoundedSolid* copyB = NULL;
    BOOLEAN answer = FALSE;

    if ( op == UNION ) {
        return !emptyA || !emptyB;
    }
    if ( emptyA || (emptyB && op == INTERSECTION) ) {
        return FALSE;
    }
    if ( !emptyB ) {
        // The operation aligned B with A inside the tolerance band
        // before deciding anything; the witnesses are looked for in
        // the same operands, on copies. Judged on the operands as
        // given, a solid that sticks out of the other by one tolerance
        // "proved" that an empty difference was wrong.
        Vector3Dd ignored;
        copyA = PolyhedralBoundedSolidStructuralCopy::copy(inSolidA);
        copyB = PolyhedralBoundedSolidStructuralCopy::copy(inSolidB);
        _PolyhedralBoundedSolidSetOperandAligner::align(copyA, copyB,
            ignored);
        inSolidA = copyA;
        inSolidB = copyB;
    }
    if ( op == INTERSECTION ) {
        answer = _PolyhedralBoundedSolidSetNonIntersectingClassifier
            ::interiorsOverlap(inSolidA, inSolidB);
    }
    else if ( op == SUBTRACT ) {
        answer = emptyB ||
            _PolyhedralBoundedSolidSetNonIntersectingClassifier
                ::hasBoundaryPointOutside(inSolidA, inSolidB);
    }
    delete copyA;
    delete copyB;
    return answer;
}

String PolyhedralBoundedSolidModeler::operationName(int op)
{
    switch ( op ) {
      case UNION: return "UNION";
      case INTERSECTION: return "INTERSECTION";
      case SUBTRACT: return "SUBTRACT";
      default: break;
    }
    return String("op#") + Cpp98Support::toString(op);
}

const String* PolyhedralBoundedSolidModeler::getLastSetOpResultPath()
{
    return _PolyhedralBoundedSolidSetOperator::getLastResultPath();
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98

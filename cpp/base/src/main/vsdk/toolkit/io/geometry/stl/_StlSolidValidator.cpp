#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/io/geometry/stl/_StlSolidValidator.h"

#include <cmath>

#include "java/lang/Double.h"
#include "java/lang/IllegalStateException.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef PolyhedralBoundedSolidNumericPolicy::ToleranceContext ToleranceContext;

static java::String d2s(double value)
{
    return java::Double::toString(value);
}

static java::String i2s(int value)
{
    return java::String::valueOf(value);
}

void _StlSolidValidator::validate(PolyhedralBoundedSolid* solid)
{
    ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid);

    validateNoDegenerateEdges(solid, numericContext);

    java::String msg;
    if ( !PolyhedralBoundedSolidGeometricValidator
        ::validateAllFacesPlanarityAndPlanes(solid, numericContext, &msg) ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: non-planar face geometry detected:\n")
            + msg);
    }

    msg = "";
    if ( !PolyhedralBoundedSolidGeometricValidator
        ::validateLoopsStrict(solid, numericContext, &msg) ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: invalid face loop geometry detected:\n")
            + msg);
    }

    msg = "";
    if ( !PolyhedralBoundedSolidGeometricValidator
        ::validateNoCoincidentVertices(solid, numericContext, &msg) ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: degenerate vertices detected:\n") + msg);
    }

    validateFacesHaveTriangulableLoops(solid, numericContext);

    if ( !PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid) ) {
        throw java::IllegalStateException(
            "STL export rejected: solid failed validateIntermediate; "
            "export requires a manifold planar intermediate solid");
    }
}

void _StlSolidValidator::validateNoDegenerateEdges(
    PolyhedralBoundedSolid* solid,
    const ToleranceContext& numericContext)
{
    int i;
    for ( i = 0; i < (int)solid->getEdgesList().size(); i++ ) {
        _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(i);
        if ( edge == nullptr || edge->rightHalf == nullptr ||
             edge->leftHalf == nullptr ) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* rightHe = edge->rightHalf;
        _PolyhedralBoundedSolidHalfEdge* leftHe = edge->leftHalf;
        double length = Vector3Dd::distance(
            rightHe->startingVertex->position, leftHe->startingVertex->position);
        if ( length <= numericContext.bigEpsilon() ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: edge ") + i2s(edge->id) +
                " is degenerate between vertices " +
                i2s(rightHe->startingVertex->id) + " and " +
                i2s(leftHe->startingVertex->id) + " (length " + d2s(length) + ")");
        }
    }
}

void _StlSolidValidator::validateFacesHaveTriangulableLoops(
    PolyhedralBoundedSolid* solid,
    const ToleranceContext& numericContext)
{
    int i;
    for ( i = 0; i < (int)solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        validateFaceLoops(face, numericContext);
    }
}

void _StlSolidValidator::validateFaceLoops(
    _PolyhedralBoundedSolidFace* face,
    const ToleranceContext& /*numericContext*/)
{
    if ( face->getContainingPlane() == nullptr ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: face ") + i2s(face->id) +
            " has no containing plane");
    }
    int i;
    for ( i = 0; i < (int)face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(i) + " has no traversable boundary");
        }
        java::ArrayList<Vector3Dd> points = collectLoopPoints(face, loop, i);
        if ( points.size() < 3 ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(i) + " has fewer than 3 vertices");
        }
        double areaTolerance = PolyhedralBoundedSolidNumericPolicy
            ::areaTolerance2D(PolyhedralBoundedSolidNumericPolicy::forFace(face));
        double projectedArea = projectedLoopAreaMagnitude(points, face);
        if ( projectedArea <= areaTolerance ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(i) + " has near-zero area" +
                buildLoopDiagnostics(face, loop, i, projectedArea,
                    areaTolerance));
        }
    }
}

java::ArrayList<Vector3Dd> _StlSolidValidator::collectLoopPoints(
    _PolyhedralBoundedSolidFace* face,
    _PolyhedralBoundedSolidLoop* loop,
    int loopIndex)
{
    java::ArrayList<Vector3Dd> points;
    _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* current = start;
    do {
        if ( current == nullptr || current->startingVertex == nullptr ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(loopIndex) + " is not traversable");
        }
        points.add(current->startingVertex->position);
        current = current->next();
        if ( current == nullptr ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(loopIndex) + " is not closed");
        }
    } while ( current != start );
    return points;
}

double _StlSolidValidator::projectedLoopAreaMagnitude(
    const java::ArrayList<Vector3Dd>& points,
    _PolyhedralBoundedSolidFace* face)
{
    Vector3Dd anchor = points.get(0);
    Vector3Dd normal = face->getContainingPlane()->getNormal().normalized();
    Vector3Dd axis = chooseReferenceAxis(normal);
    Vector3Dd u = axis.crossProduct(normal).normalized();
    Vector3Dd v = normal.crossProduct(u).normalized();

    double areaTwice = 0.0;
    int i;
    int n = (int)points.size();
    for ( i = 0; i < n; i++ ) {
        Vector3Dd current = points.get(i).subtract(anchor);
        Vector3Dd next = points.get((i + 1) % n).subtract(anchor);
        double x0 = current.dotProduct(u);
        double y0 = current.dotProduct(v);
        double x1 = next.dotProduct(u);
        double y1 = next.dotProduct(v);
        areaTwice += x0 * y1 - x1 * y0;
    }
    return std::fabs(areaTwice * 0.5);
}

Vector3Dd _StlSolidValidator::chooseReferenceAxis(const Vector3Dd& normal)
{
    double ax = std::fabs(normal.x());
    double ay = std::fabs(normal.y());
    double az = std::fabs(normal.z());
    if ( ax <= ay && ax <= az ) {
        return Vector3Dd(1.0, 0.0, 0.0);
    }
    if ( ay <= az ) {
        return Vector3Dd(0.0, 1.0, 0.0);
    }
    return Vector3Dd(0.0, 0.0, 1.0);
}

java::String _StlSolidValidator::buildLoopDiagnostics(
    _PolyhedralBoundedSolidFace* face,
    _PolyhedralBoundedSolidLoop* loop,
    int loopIndex,
    double projectedArea,
    double areaTolerance)
{
    java::String msg;
    msg += java::String("\n  projectedArea=") + d2s(projectedArea);
    msg += java::String(" areaTolerance=") + d2s(areaTolerance);
    msg += java::String(" faceScale=") +
        d2s(PolyhedralBoundedSolidNumericPolicy::forFace(face).modelScale());
    msg += java::String(" solidScale=") +
        d2s(PolyhedralBoundedSolidNumericPolicy::forSolid(face->parentSolid)
            .modelScale());
    if ( face->getContainingPlane() != nullptr ) {
        Vector3Dd normal = face->getContainingPlane()->getNormal();
        msg += java::String(" normal=(") + d2s(normal.x()) + ", " +
            d2s(normal.y()) + ", " + d2s(normal.z()) + ")";
    }
    msg += java::String("\n  loop ") + i2s(loopIndex) + " vertices:";

    _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* current = start;
    do {
        msg += java::String("\n    v") + i2s(current->startingVertex->id) + "=(" +
            d2s(current->startingVertex->position.x()) + ", " +
            d2s(current->startingVertex->position.y()) + ", " +
            d2s(current->startingVertex->position.z()) + ")";
        current = current->next();
    } while ( current != nullptr && current != start );
    return msg;
}

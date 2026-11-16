#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/io/geometry/stl/_StlFaceTriangulator.h"

#include <cmath>

#include "java/lang/IllegalStateException.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef PolyhedralBoundedSolidNumericPolicy::ToleranceContext ToleranceContext;

static java::String i2s(int value)
{
    return java::String::valueOf(value);
}

java::ArrayList<_StlFacetEmitter::Facet> _StlFaceTriangulator::triangulateSolid(
    PolyhedralBoundedSolid* solid)
{
    java::ArrayList<_StlFacetEmitter::Facet> facets;
    int i;
    for ( i = 0; i < (int)solid->getPolygonsList().size(); i++ ) {
        facets.addAll(triangulateFace(solid->getPolygonsList().get(i)));
    }
    return facets;
}

java::ArrayList<_StlFacetEmitter::Facet> _StlFaceTriangulator::triangulateFace(
    _PolyhedralBoundedSolidFace* face)
{
    ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::forFace(face);
    InfinitePlane* plane = face->getContainingPlane();
    if ( plane == nullptr ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: face ") + i2s(face->id) +
            " has no containing plane");
    }

    FaceBasis basis = buildBasis(face, plane);
    java::ArrayList<ProjectedVertex> flattenedVertices;
    java::ArrayList<Vector3Dd> originalVertices;
    Polygon2D polygon;
    buildProjectedPolygon(face, basis, numericContext,
        flattenedVertices, originalVertices, polygon);

    java::ArrayList<MonotoneDecompositionTriangulator::Triangle> triangles;
    MonotoneDecompositionTriangulator triangulator;
    int triangleCount = 0;
    triangulator.triangulate(polygon, triangles, triangleCount);
    if ( triangleCount <= 0 || triangles.isEmpty() ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: face ") + i2s(face->id) +
            " triangulation produced no triangles");
    }

    java::ArrayList<_StlFacetEmitter::Facet> facets;
    int i;
    for ( i = 0; i < (int)triangles.size(); i++ ) {
        facets.add(buildFacet(face, triangles[i], originalVertices,
            basis.normal, numericContext, i));
    }
    return facets;
}

_StlFaceTriangulator::FaceBasis _StlFaceTriangulator::buildBasis(
    _PolyhedralBoundedSolidFace* face,
    InfinitePlane* plane)
{
    Vector3Dd normal = plane->getNormal().normalized();
    Vector3Dd origin = chooseFaceAnchor(face);
    Vector3Dd referenceAxis = chooseReferenceAxis(normal);
    Vector3Dd u = referenceAxis.crossProduct(normal).normalized();
    Vector3Dd v = normal.crossProduct(u).normalized();
    return FaceBasis(origin, u, v, normal);
}

void _StlFaceTriangulator::buildProjectedPolygon(
    _PolyhedralBoundedSolidFace* face,
    const FaceBasis& basis,
    const ToleranceContext& numericContext,
    java::ArrayList<ProjectedVertex>& flattenedVertices,
    java::ArrayList<Vector3Dd>& originalVertices,
    Polygon2D& polygon)
{
    // Java: `polygon.loops.clear()` (C++ port: the contour the constructor
    // created is deleted)
    long int k;
    for ( k = 0; k < polygon.loops.size(); k++ ) {
        delete polygon.loops[k];
    }
    polygon.loops.clear();

    int i;
    for ( i = 0; i < (int)face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(i) + " has no traversable half-edge");
        }

        java::ArrayList<ProjectedVertex> loopVertices =
            collectLoopVertices(face, i, loop, basis);
        if ( loopVertices.size() < 3 ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(i) + " has fewer than 3 vertices");
        }

        double area = signedArea(loopVertices);
        if ( std::fabs(area) <=
             PolyhedralBoundedSolidNumericPolicy::areaTolerance2D(
                 numericContext) ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(i) + " has near-zero projected area");
        }

        polygon.nextLoop();
        int j;
        for ( j = 0; j < (int)loopVertices.size(); j++ ) {
            const ProjectedVertex& vertex = loopVertices[j];
            polygon.addVertex(vertex.x, vertex.y);
            flattenedVertices.add(vertex);
            originalVertices.add(vertex.original);
        }
    }

    if ( !polygon.loops.isEmpty() && polygon.loops.get(0)->vertices.isEmpty() ) {
        polygon.eraseLastLoop();
    }
}

java::ArrayList<_StlFaceTriangulator::ProjectedVertex>
_StlFaceTriangulator::collectLoopVertices(
    _PolyhedralBoundedSolidFace* face,
    int loopIndex,
    _PolyhedralBoundedSolidLoop* loop,
    const FaceBasis& basis)
{
    java::ArrayList<ProjectedVertex> vertices;
    _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* current = start;
    do {
        if ( current == nullptr || current->startingVertex == nullptr ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(loopIndex) + " is not traversable");
        }
        Vector3Dd point = current->startingVertex->position;
        Vector3Dd delta = point.subtract(basis.origin);
        vertices.add(ProjectedVertex(point,
            delta.dotProduct(basis.u), delta.dotProduct(basis.v)));
        current = current->next();
        if ( current == nullptr ) {
            throw java::IllegalStateException(
                java::String("STL export rejected: face ") + i2s(face->id) +
                " loop " + i2s(loopIndex) + " is not closed");
        }
    } while ( current != start );
    return vertices;
}

_StlFacetEmitter::Facet _StlFaceTriangulator::buildFacet(
    _PolyhedralBoundedSolidFace* face,
    const MonotoneDecompositionTriangulator::Triangle& triangle,
    const java::ArrayList<Vector3Dd>& originalVertices,
    const Vector3Dd& faceNormal,
    const ToleranceContext& numericContext,
    int triangleIndex)
{
    validateTriangleIndices(face, triangle, (int)originalVertices.size());

    Vector3Dd a = originalVertices.get(triangle.get(0));
    Vector3Dd b = originalVertices.get(triangle.get(1));
    Vector3Dd c = originalVertices.get(triangle.get(2));

    if ( !PolyhedralBoundedSolidNumericPolicy::pointsSeparated(a, b, numericContext) ||
         !PolyhedralBoundedSolidNumericPolicy::pointsSeparated(b, c, numericContext) ||
         !PolyhedralBoundedSolidNumericPolicy::pointsSeparated(a, c, numericContext) ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: face ") + i2s(face->id) +
            " triangulation produced " +
            "a repeated-vertex triangle at index " + i2s(triangleIndex));
    }

    Vector3Dd normal = b.subtract(a).crossProduct(c.subtract(a));
    double normalLength = normal.length();
    if ( normalLength <=
         PolyhedralBoundedSolidNumericPolicy::areaTolerance2D(
             numericContext) ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: face ") + i2s(face->id) +
            " triangulation produced " +
            "a zero-area triangle at index " + i2s(triangleIndex));
    }
    normal = normal.multiply(1.0 / normalLength);

    if ( normal.dotProduct(faceNormal) < 0.0 ) {
        Vector3Dd temp = b;
        b = c;
        c = temp;
        normal = b.subtract(a).crossProduct(c.subtract(a)).normalized();
        if ( normal.dotProduct(faceNormal) < 0.0 ) {
            normal = faceNormal;
        }
    }

    return _StlFacetEmitter::Facet(normal, a, b, c);
}

void _StlFaceTriangulator::validateTriangleIndices(
    _PolyhedralBoundedSolidFace* face,
    const MonotoneDecompositionTriangulator::Triangle& triangle,
    int vertexCount)
{
    int p0 = triangle.get(0);
    int p1 = triangle.get(1);
    int p2 = triangle.get(2);
    if ( p0 < 0 || p0 >= vertexCount ||
         p1 < 0 || p1 >= vertexCount ||
         p2 < 0 || p2 >= vertexCount ) {
        throw java::IllegalStateException(
            java::String("STL export rejected: face ") + i2s(face->id) +
            " triangulation returned an out-of-range index");
    }
}

double _StlFaceTriangulator::signedArea(
    const java::ArrayList<ProjectedVertex>& vertices)
{
    double areaTwice = 0.0;
    int i;
    int n = (int)vertices.size();
    for ( i = 0; i < n; i++ ) {
        const ProjectedVertex& current = vertices[i];
        const ProjectedVertex& next = vertices[(i + 1) % n];
        areaTwice += current.x * next.y - next.x * current.y;
    }
    return areaTwice * 0.5;
}

Vector3Dd _StlFaceTriangulator::chooseFaceAnchor(_PolyhedralBoundedSolidFace* face)
{
    int i;
    for ( i = 0; i < (int)face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if ( loop != nullptr && loop->boundaryStartHalfEdge != nullptr &&
             loop->boundaryStartHalfEdge->startingVertex != nullptr ) {
            return loop->boundaryStartHalfEdge->startingVertex->position;
        }
    }
    throw java::IllegalStateException(
        java::String("STL export rejected: face ") + i2s(face->id) +
        " has no anchor vertex");
}

Vector3Dd _StlFaceTriangulator::chooseReferenceAxis(const Vector3Dd& normal)
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

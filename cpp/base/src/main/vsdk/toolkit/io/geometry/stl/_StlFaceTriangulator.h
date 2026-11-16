#ifndef __STL_FACE_TRIANGULATOR__
#define __STL_FACE_TRIANGULATOR__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/io/geometry/stl/_StlFacetEmitter.h"

class InfinitePlane;
class Polygon2D;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidLoop;

/**
Triangulates the faces of a solid into STL facets. Internal collaborator
of `StlWriter`.

C++ port note: the triangles of `MonotoneDecompositionTriangulator` are
lists of three indices (Java: `point0`, `point1`, `point2`).
*/
class _StlFaceTriangulator {
public:
    static java::ArrayList<_StlFacetEmitter::Facet> triangulateSolid(
        PolyhedralBoundedSolid* solid);

private:
    class ProjectedVertex {
    public:
        Vector3Dd original;
        double x;
        double y;

        ProjectedVertex() : x(0), y(0) {}
        ProjectedVertex(const Vector3Dd& original, double x, double y)
            : original(original), x(x), y(y)
        {
        }
    };

    class FaceBasis {
    public:
        Vector3Dd origin;
        Vector3Dd u;
        Vector3Dd v;
        Vector3Dd normal;

        FaceBasis(const Vector3Dd& origin, const Vector3Dd& u,
                  const Vector3Dd& v, const Vector3Dd& normal)
            : origin(origin), u(u), v(v), normal(normal)
        {
        }
    };

    _StlFaceTriangulator() {}

    static java::ArrayList<_StlFacetEmitter::Facet> triangulateFace(
        _PolyhedralBoundedSolidFace* face);
    static FaceBasis buildBasis(_PolyhedralBoundedSolidFace* face,
                                InfinitePlane* plane);
    static void buildProjectedPolygon(
        _PolyhedralBoundedSolidFace* face,
        const FaceBasis& basis,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext,
        java::ArrayList<ProjectedVertex>& flattenedVertices,
        java::ArrayList<Vector3Dd>& originalVertices,
        Polygon2D& polygon);
    static java::ArrayList<ProjectedVertex> collectLoopVertices(
        _PolyhedralBoundedSolidFace* face,
        int loopIndex,
        _PolyhedralBoundedSolidLoop* loop,
        const FaceBasis& basis);
    static _StlFacetEmitter::Facet buildFacet(
        _PolyhedralBoundedSolidFace* face,
        const MonotoneDecompositionTriangulator::Triangle& triangle,
        const java::ArrayList<Vector3Dd>& originalVertices,
        const Vector3Dd& faceNormal,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext,
        int triangleIndex);
    static void validateTriangleIndices(
        _PolyhedralBoundedSolidFace* face,
        const MonotoneDecompositionTriangulator::Triangle& triangle,
        int vertexCount);
    static double signedArea(const java::ArrayList<ProjectedVertex>& vertices);
    static Vector3Dd chooseFaceAnchor(_PolyhedralBoundedSolidFace* face);
    static Vector3Dd chooseReferenceAxis(const Vector3Dd& normal);
};

#endif

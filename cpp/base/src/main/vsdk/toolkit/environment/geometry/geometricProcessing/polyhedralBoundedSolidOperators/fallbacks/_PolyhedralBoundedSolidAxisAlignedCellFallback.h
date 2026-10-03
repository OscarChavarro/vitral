#ifndef ___POLYHEDRAL_BOUNDED_SOLID_AXIS_ALIGNED_CELL_FALLBACK__
#define ___POLYHEDRAL_BOUNDED_SOLID_AXIS_ALIGNED_CELL_FALLBACK__

#include <map>
#include <string>
#include <vector>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidEdge;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidVertex;

/**
Structural-shape boolean fallback for axis-aligned cell decompositions: when
both operands are axis-aligned, classifies the cell grid against each operand
and rebuilds the boolean result cell-by-cell.

C++ counterpart of Java's `_PolyhedralBoundedSolidAxisAlignedCellFallback`.
*/
class _PolyhedralBoundedSolidAxisAlignedCellFallback
    : public _PolyhedralBoundedSolidOperator {
public:
    static bool axisAlignedCellSelected(bool insideA, bool insideB, int op);

    /**
    @return a new solid owned by the caller, or null when the operands are
    not axis-aligned or the grid is too large
    */
    static PolyhedralBoundedSolid* buildAxisAlignedCellBooleanFallback(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        int op);

private:
    class AxisAlignedCellBooleanBuilder {
    public:
        AxisAlignedCellBooleanBuilder(const std::vector<double>& xCoordinates,
            const std::vector<double>& yCoordinates,
            const std::vector<double>& zCoordinates);
        void addQuad(const int corners[4][3]);
        PolyhedralBoundedSolid* result();
    private:
        PolyhedralBoundedSolid* solid;
        std::vector<double> xCoordinates;
        std::vector<double> yCoordinates;
        std::vector<double> zCoordinates;
        std::map<std::string, _PolyhedralBoundedSolidVertex*> vertices;
        std::map<std::string, _PolyhedralBoundedSolidEdge*> edges;
        int nextVertexId;
        int nextFaceId;

        static std::string vertexKey(int ix, int iy, int iz);
        _PolyhedralBoundedSolidVertex* vertexAt(int ix, int iy, int iz);
        static std::string edgeKey(_PolyhedralBoundedSolidVertex* a,
            _PolyhedralBoundedSolidVertex* b);
        void attachEdge(_PolyhedralBoundedSolidHalfEdge* he,
            _PolyhedralBoundedSolidVertex* a,
            _PolyhedralBoundedSolidVertex* b);
    };

    _PolyhedralBoundedSolidAxisAlignedCellFallback();
    static bool isAxisAlignedEdge(_PolyhedralBoundedSolidEdge* edge);
    static bool isAxisAlignedSolid(PolyhedralBoundedSolid* solid);
    static int classifyPointForAxisAlignedFallback(PolyhedralBoundedSolid* solid,
        const Vector3Dd& point);
    static void addAxisAlignedBoundaryQuad(AxisAlignedCellBooleanBuilder& builder,
        int axis, bool positiveSide, int ix, int iy, int iz);
    static std::vector<double> uniformCoordinates(double min, double max,
        int divisions);
    static PolyhedralBoundedSolid* buildUniformSampledCellBooleanFallback(
        PolyhedralBoundedSolid* inSolidA,
        PolyhedralBoundedSolid* inSolidB,
        int op);
    static void addOccupiedCellQuads(AxisAlignedCellBooleanBuilder& builder,
        const std::vector<bool>& occupied, int nx, int ny, int nz);
};

#endif

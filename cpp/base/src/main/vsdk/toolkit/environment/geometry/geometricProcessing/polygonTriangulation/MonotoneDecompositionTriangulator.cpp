#include <stdexcept>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_RandomSegmentOrder.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"

// References: [SEID1991] Seidel, R. "A simple and Fast Randomized Algorithm
// for Computing Trapezoidal Decompositions and for Triangulating Polygons".

void MonotoneDecompositionTriangulator::stage1PrepareAndOrder(
    const Polygon2D &input, int &numVertices) {
    if (input.loops.size() <= 0) {
        throw std::runtime_error(
            "Polygon input must contain at least one contour");
    }
    java::ArrayList<int> contourSizes;
    java::ArrayList<double> vertices;
    Polygon2D &mutableInput = const_cast<Polygon2D &>(input);
    for (int contourIndex = 0; contourIndex < mutableInput.loops.size();
         contourIndex++) {
        _Polygon2DContour *contour = mutableInput.loops[contourIndex];
        const int pointCount = static_cast<int>(contour->vertices.size());
        if (pointCount <= 0) {
            continue;
        }
        contourSizes.add(pointCount);
        for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
            vertices.add(contour->vertices[pointIndex].x);
            vertices.add(contour->vertices[pointIndex].y);
        }
    }

    if (contourSizes.size() <= 0 || vertices.size() <= 0) {
        throw std::runtime_error(
            "Polygon input must contain at least one vertex");
    }

    numVertices = _SegmentTableBuilder::prepareSegments(
        vertices.data(), static_cast<int>(vertices.size() / 2),
        contourSizes.data(), static_cast<int>(contourSizes.size()));
}

void MonotoneDecompositionTriangulator::stage2BootStrap(int numVertices) {
    // [SEID1991].3 Initialize randomized incremental insertion status.
    // Stage-2 in Seidel (1991): initialize randomized insertion status.
    // Reset insertion flags and generate the randomized segment ordering used
    // by Stage-3 insertion.
    for (int i = 1; i <= numVertices; ++i) {
        _Construct::segmentAt(i).hasBeenInserted = false;
    }
    _RandomSegmentOrder::generateRandomOrdering(numVertices);
}

void MonotoneDecompositionTriangulator::stage3IncrementalBatchedInsertion(
    int numVertices) {
    // [SEID1991].3 Execute batched randomized segment insertion into T(S).
    buildTrapezoids(numVertices);
}

void MonotoneDecompositionTriangulator::buildTrapezoids(int numVertices) {
    _Construct::constructTrapezoids(numVertices);
}

int MonotoneDecompositionTriangulator::stage4FinalizeAndExtractTriangles(
    int numVertices, java::ArrayList<Triangle> &out) {
    // [SEID1991].2/.3 Convert trapezoid decomposition into monotone triangles.
    int op[SEGMENT_SIZE][3];
    const int nmonpoly = _Monotone::monotonateTrapezoids(numVertices);
    const int ntriangles =
        _Monotone::triangulateMonotonePolygons(numVertices, nmonpoly, op);

    out.clear();
    for (int i = 0; i < ntriangles; ++i) {
        Triangle triangle;
        triangle.add(op[i][0]);
        triangle.add(op[i][1]);
        triangle.add(op[i][2]);
        out.add(triangle);
    }

    return ntriangles;
}

void MonotoneDecompositionTriangulator::triangulate(
    const Polygon2D &input, java::ArrayList<Triangle> &triangles,
    int &triangleCount) {
    int numVertices = 0;

    stage1PrepareAndOrder(input, numVertices);
    stage2BootStrap(numVertices);
    stage3IncrementalBatchedInsertion(numVertices);
    triangleCount = stage4FinalizeAndExtractTriangles(numVertices, triangles);
}

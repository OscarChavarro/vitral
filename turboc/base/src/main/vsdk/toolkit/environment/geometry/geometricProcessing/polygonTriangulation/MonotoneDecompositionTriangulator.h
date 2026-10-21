#ifndef __MONOTONE_DECOMPOSITION_TRIANGULATOR__
#define __MONOTONE_DECOMPOSITION_TRIANGULATOR__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_Construct.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_Monotone.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_SegmentTableBuilder.h"
class MonotoneDecompositionTriangulator {
  public:
    typedef ArrayList<int> Triangle;

    void triangulate(const Polygon2D &input,
                     ArrayList<Triangle> &triangles, int &triangleCount);

  private:
    void stage1PrepareAndOrder(const Polygon2D &input, int &numVertices);
    void stage2BootStrap(int numVertices);
    void stage3IncrementalBatchedInsertion(int numVertices);
    void buildTrapezoids(int numVertices);
    int stage4FinalizeAndExtractTriangles(int numVertices,
                                          ArrayList<Triangle> &out);
};

#endif

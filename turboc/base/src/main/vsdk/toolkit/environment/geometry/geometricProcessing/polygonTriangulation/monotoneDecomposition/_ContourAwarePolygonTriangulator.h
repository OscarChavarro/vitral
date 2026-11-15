#ifndef __CNTR_AWR_PLYGN_TRNGLTR__
#define __CNTR_AWR_PLYGN_TRNGLTR__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.h"
class _ContourAwarePolygonTriangulator {
  public:
    static int triangulate(
        const Polygon2D &input,
        ArrayList<MonotoneDecompositionTriangulator::Triangle> &output);
};

#endif

#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_POLYGONTRIANGULATION_MONOTONEDECOMPOSITION__CONTOURAWAREPOLYGONTRIANGULATOR_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_POLYGONTRIANGULATION_MONOTONEDECOMPOSITION__CONTOURAWAREPOLYGONTRIANGULATOR_H__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.h"
class _ContourAwarePolygonTriangulator {
  public:
    static int triangulate(
        const Polygon2D &input,
        java::ArrayList<MonotoneDecompositionTriangulator::Triangle> &output);
};

#endif

#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_POLYGONCLIPPER_POLYGONTOPOLOGICALMERGER_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_POLYGONCLIPPER_POLYGONTOPOLOGICALMERGER_H__

#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"

class PolygonTopologicalMerger {
public:
    void mergeInPlace(Polygon2D* polygon);
    void mergeInPlace(Polygon2D* polygon, double epsilon);
};

#endif

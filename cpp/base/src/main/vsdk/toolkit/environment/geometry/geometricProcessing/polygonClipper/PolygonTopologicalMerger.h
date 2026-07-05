#ifndef __POLYGONTOPOLOGICALMERGER__
#define __POLYGONTOPOLOGICALMERGER__

#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
class PolygonTopologicalMerger {
public:
    void mergeInPlace(Polygon2D* polygon);
    void mergeInPlace(Polygon2D* polygon, double epsilon);
};

#endif

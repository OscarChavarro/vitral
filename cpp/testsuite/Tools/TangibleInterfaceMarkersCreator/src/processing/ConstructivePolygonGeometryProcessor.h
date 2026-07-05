#ifndef __CONSTRUCTIVEPOLYGONGEOMETRYPROCESSOR__
#define __CONSTRUCTIVEPOLYGONGEOMETRYPROCESSOR__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
class ConstructivePolygonGeometryProcessor {
public:
    ConstructivePolygonGeometryProcessor();
    ~ConstructivePolygonGeometryProcessor();

    Polygon2D execute(java::ArrayList<Polygon2D*>* polygons) const;
};

#endif

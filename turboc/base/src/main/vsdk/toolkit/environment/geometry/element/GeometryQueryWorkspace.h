#ifndef __GEOMETRY_QUERY_WORKSPACE__
#define __GEOMETRY_QUERY_WORKSPACE__

#include "vsdk/toolkit/common/VSDK.h"

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"

// Caller-owned, reusable per worker/query; never share during concurrent queries.
class GeometryQueryWorkspace {
private:
    RaySolidClassification solid;
    GeometryBounds bounds;
    RayHit hit;
    ArrayList<RaySolidClassification*> scratch;
    long scratchDepth;
    GeometryQueryWorkspace(const GeometryQueryWorkspace&);
    GeometryQueryWorkspace& operator=(const GeometryQueryWorkspace&);
public:
    GeometryQueryWorkspace();
    ~GeometryQueryWorkspace();
    RaySolidClassification& solidResult();
    GeometryBounds& boundsResult();
    RayHit& hitResult();
    // Acquired pointers remain stable across pool growth; release in reverse order.
    RaySolidClassification* acquireSolidScratch();
    BOOLEAN releaseSolidScratch(RaySolidClassification* value);
    void clear();
};
#endif

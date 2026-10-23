#ifndef __GEOMETRY_TRIANGULATOR__
#define __GEOMETRY_TRIANGULATOR__

#include "vsdk/toolkit/common/VSDK.h"

class Geometry;
class TriangleMeshGroup;

class GeometryTriangulator {
public:
    static BOOLEAN exportToTriangleMeshGroup(Geometry* geometry, TriangleMeshGroup& outGroup);
};

#endif

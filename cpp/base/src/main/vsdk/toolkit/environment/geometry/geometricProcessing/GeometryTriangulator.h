#ifndef __GEOMETRYTRIANGULATOR__
#define __GEOMETRYTRIANGULATOR__

class Geometry;
class TriangleMeshGroup;

class GeometryTriangulator {
public:
    static bool exportToTriangleMeshGroup(Geometry* geometry, TriangleMeshGroup& outGroup);
};

#endif

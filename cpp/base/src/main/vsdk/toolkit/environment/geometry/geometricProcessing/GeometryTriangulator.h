#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_GEOMETRYTRIANGULATOR_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_GEOMETRYTRIANGULATOR_H__

class Geometry;
class TriangleMeshGroup;

class GeometryTriangulator {
public:
    static bool exportToTriangleMeshGroup(Geometry* geometry, TriangleMeshGroup& outGroup);
};

#endif

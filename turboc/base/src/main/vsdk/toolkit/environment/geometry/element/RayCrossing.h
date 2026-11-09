#ifndef VSDK_RAYCROSSING_H
#define VSDK_RAYCROSSING_H
#include "vsdk/toolkit/environment/geometry/element/RayCrossingKind.h"
class Geometry; class SimpleBody; class Material;
class RayCrossing { public:
    typedef RayCrossingKind::Value Kind;
    double t; const Geometry* geometry; Kind kind; const SimpleBody* body; Material* material;
    RayCrossing(double t, const Geometry* geometry, Kind kind) : t(t), geometry(geometry), kind(kind), body(0), material(0) {}
};
#endif

#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_VOLUME_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_VOLUME_H__

#include "vsdk/toolkit/environment/geometry/Geometry.h"

class PolyhedralBoundedSolid;

class Volume : public Geometry {
public:
    virtual ~Volume() {}
    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();
};

#endif

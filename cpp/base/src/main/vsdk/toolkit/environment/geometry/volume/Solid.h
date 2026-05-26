#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_SOLID_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_SOLID_H__

#include "vsdk/toolkit/environment/geometry/volume/Volume.h"

class Solid : public Volume {
public:
    virtual ~Solid() {}
    virtual Vector3Dd doCenterOfMass();
};

#endif

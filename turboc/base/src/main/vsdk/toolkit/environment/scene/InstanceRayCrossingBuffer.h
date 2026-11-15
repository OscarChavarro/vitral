#ifndef __INSTNC_RY_CRSSNG_BFFR__
#define __INSTNC_RY_CRSSNG_BFFR__

#include "vsdk/toolkit/common/VSDK.h"

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingSink.h"
#include "vsdk/toolkit/environment/geometry/element/RayInstanceStep.h"
class SceneSolid;
class Material;

// Buffers events so a failed source query never emits a partial result.
class InstanceRayCrossingBuffer : public RayCrossingSink {
private:
    ArrayList<RayBoundary> boundaries;
    ArrayList<RayCrossingKind::Value> kinds;
    ArrayList<const SceneSolid*> bodies;
    ArrayList<Material*> materials;
    RayInstanceStep instance;
    const SceneSolid* body;
    Material* material;
    BOOLEAN failed;
public:
    InstanceRayCrossingBuffer(const RayInstanceStep& instance, const SceneSolid* body, Material* material);
    virtual void crossing(const RayCrossing& event);
    BOOLEAN isValid() const;
    void deliver(RayCrossingSink& sink) const;
};
#endif

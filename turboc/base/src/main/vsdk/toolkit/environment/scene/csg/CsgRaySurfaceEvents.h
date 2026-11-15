#ifndef __CSG_RAY_SURFACE_EVENTS__
#define __CSG_RAY_SURFACE_EVENTS__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayLeafBatch.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayEventReference.h"
#include "vsdk/toolkit/environment/scene/csg/CsgVersionStamp.h"
// Selected leaf boundary events ordered by ray parameter, then leaf order.
// This is a list of visible surface events, not a solid classification.
class CsgRaySurfaceEvents {
private:
    CsgRayLeafBatch leaves;
    ArrayList<CsgRayEventReference> events;
    CsgVersionStamp versions;
public:
    CsgRaySurfaceEvents();
    CsgRaySurfaceEvents(const CsgRaySurfaceEvents& other);
    CsgRaySurfaceEvents& operator=(const CsgRaySurfaceEvents& other);
    ~CsgRaySurfaceEvents();
    void clear();
    CsgRayLeafBatch& getLeaves();
    const CsgRayLeafBatch& getLeaves() const;
    // Inserts keeping the (t, leaf, boundary) order.
    BOOLEAN add(const CsgRayEventReference& reference);
    long int size() const;
    const CsgRayEventReference* get(long int index) const;
    // Parameter of event index, or NaN when out of range.
    double getT(long int index) const;
    CsgVersionStamp& getVersions();
    const CsgVersionStamp& getVersions() const;
};
#endif

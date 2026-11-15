#ifndef __CSG_RAY_EVENT_BATCH__
#define __CSG_RAY_EVENT_BATCH__
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayEventGroup.h"
class CsgRayEventBatch {
private:
    java::ArrayList<CsgRayEventGroup> groups;
public:
    CsgRayEventBatch();
    CsgRayEventBatch(const CsgRayEventBatch& other);
    CsgRayEventBatch& operator=(const CsgRayEventBatch& other);
    ~CsgRayEventBatch();
    void clear();
    bool add(const CsgRayEventGroup& group);
    long int size() const;
    const CsgRayEventGroup* get(long int index) const;
};
#endif

#ifndef __CSG_RAY_EVENT_BATCH__
#define __CSG_RAY_EVENT_BATCH__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayEventGroup.h"
class CsgRayEventBatch {
private:
    ArrayList<CsgRayEventGroup> groups;
public:
    CsgRayEventBatch();
    CsgRayEventBatch(const CsgRayEventBatch& other);
    CsgRayEventBatch& operator=(const CsgRayEventBatch& other);
    ~CsgRayEventBatch();
    void clear();
    BOOLEAN add(const CsgRayEventGroup& group);
    long int size() const;
    const CsgRayEventGroup* get(long int index) const;
};
#endif

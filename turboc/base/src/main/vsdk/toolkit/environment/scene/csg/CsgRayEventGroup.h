#ifndef __CSG_RAY_EVENT_GROUP__
#define __CSG_RAY_EVENT_GROUP__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayEventReference.h"
// Events with exactly equal binary64 ray parameters, with stable leaf order.
class CsgRayEventGroup {
private:
    double t;
    ArrayList<CsgRayEventReference> references;
public:
    CsgRayEventGroup();
    explicit CsgRayEventGroup(double t);
    CsgRayEventGroup(const CsgRayEventGroup& other);
    CsgRayEventGroup& operator=(const CsgRayEventGroup& other);
    ~CsgRayEventGroup();
    double getT() const;
    BOOLEAN add(const CsgRayEventReference& reference);
    long int size() const;
    const CsgRayEventReference* get(long int index) const;
};
#endif

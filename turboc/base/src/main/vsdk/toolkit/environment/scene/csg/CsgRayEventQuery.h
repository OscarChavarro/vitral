#ifndef __CSG_RAY_EVENT_QUERY__
#define __CSG_RAY_EVENT_QUERY__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayEventReference.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayEventCursor.h"
class CsgRayLeafBatch;
class CsgRayEventBatch;
// Stable exact-t grouping; nearby values are not asserted coincident. References
// are valid only with the unchanged leaf batch used for this query.
class CsgRayEventQuery {
public:
    static RaySolidQueryStatus::Value group(const CsgRayLeafBatch& leaves, CsgRayEventBatch& result);
    // Streaming form over caller storage. validate checks every leaf and
    // resets one cursor per leaf; nextGroup then fills group with the next
    // exactly equal parameter's references in leaf order, or returns false
    // when every boundary has been consumed.
    static RaySolidQueryStatus::Value validate(const CsgRayLeafBatch& leaves, ArrayList<CsgRayEventCursor>& cursors);
    static BOOLEAN nextGroup(const CsgRayLeafBatch& leaves, ArrayList<CsgRayEventCursor>& cursors,
        double& t, ArrayList<CsgRayEventReference>& group);
};
#endif

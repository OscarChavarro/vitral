#ifndef __CSG_RAY_WORKSPACE__
#define __CSG_RAY_WORKSPACE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayEventReference.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayEventCursor.h"
class CsgExpression;
// Caller-owned scratch storage for CSG ray queries. It is reused across
// queries by one worker and must not be shared between concurrent queries.
// Its contents are meaningful only during a query.
class CsgRayWorkspace {
public:
    ArrayList<const CsgExpression*> path;
    ArrayList<CsgRayEventCursor> cursors;
    ArrayList<CsgRayEventReference> group;
    ArrayList<BOOLEAN> states;
    ArrayList<BOOLEAN> previous;
    ArrayList<BOOLEAN> trial;
    CsgRayWorkspace();
    ~CsgRayWorkspace();
};
#endif

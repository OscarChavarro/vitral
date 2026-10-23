#ifndef __TRACE_WORKSPACE__
#define __TRACE_WORKSPACE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
class TraceWorkspace {
public:
    enum { DEFAULT_MAX_RECURSION_LEVEL = 8 };

    RayHit traversalCandidateHit;
    RayHit nearestHit;
    RayHit shadowCandidateHitObj;
    ArrayList<RayHit> reflectionHits;
    ArrayList<RayHit> shadingHits;
    ArrayList<ColorRgb> reflectionColors;

    TraceWorkspace();
    explicit TraceWorkspace(int maxRecursionLevel);

    RayHit* shadowCandidateHit();

private:
    void initialize(int maxRecursionLevel);
};

#endif

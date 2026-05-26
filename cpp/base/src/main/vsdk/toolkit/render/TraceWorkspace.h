#ifndef __VSDK_TOOLKIT_RENDER_TRACEWORKSPACE_H__
#define __VSDK_TOOLKIT_RENDER_TRACEWORKSPACE_H__

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "java/util/ArrayList.h"

class TraceWorkspace {
public:
    static const int DEFAULT_MAX_RECURSION_LEVEL = 8;

    RayHit traversalCandidateHit;
    RayHit nearestHit;
    RayHit shadowCandidateHitObj;
    java::ArrayList<RayHit> reflectionHits;
    java::ArrayList<RayHit> shadingHits;
    java::ArrayList<ColorRgb> reflectionColors;

    TraceWorkspace();
    explicit TraceWorkspace(int maxRecursionLevel);

    RayHit* shadowCandidateHit();
};

#endif

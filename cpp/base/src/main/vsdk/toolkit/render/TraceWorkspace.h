#ifndef __VSDK_TOOLKIT_RENDER_TRACEWORKSPACE_H__
#define __VSDK_TOOLKIT_RENDER_TRACEWORKSPACE_H__

#include <vector>
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"

class TraceWorkspace {
public:
    static const int DEFAULT_MAX_RECURSION_LEVEL = 8;

    RayHit traversalCandidateHit;
    RayHit nearestHit;
    RayHit shadowCandidateHitObj;
    std::vector<RayHit> reflectionHits;
    std::vector<RayHit> shadingHits;
    std::vector<ColorRgb> reflectionColors;

    TraceWorkspace();
    explicit TraceWorkspace(int maxRecursionLevel);

    RayHit* shadowCandidateHit();
};

#endif

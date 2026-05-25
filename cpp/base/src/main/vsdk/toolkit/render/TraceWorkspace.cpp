#include "vsdk/toolkit/render/TraceWorkspace.h"

TraceWorkspace::TraceWorkspace() : TraceWorkspace(DEFAULT_MAX_RECURSION_LEVEL)
{
}

TraceWorkspace::TraceWorkspace(int maxRecursionLevel)
    : traversalCandidateHit(RayHit::DETAIL_NONE, false),
      nearestHit(RayHit::DETAIL_NONE, false),
      shadowCandidateHitObj(RayHit::DETAIL_NONE, false)
{
    int levels = maxRecursionLevel + 1;
    reflectionHits.reserve(levels);
    shadingHits.reserve(levels);
    reflectionColors.reserve(levels);
    for ( int i = 0; i < levels; i++ ) {
        reflectionHits.push_back(RayHit(RayHit::DETAIL_NONE, false));
        shadingHits.push_back(RayHit());
        reflectionColors.push_back(ColorRgb());
    }
}

RayHit* TraceWorkspace::shadowCandidateHit()
{
    return &shadowCandidateHitObj;
}

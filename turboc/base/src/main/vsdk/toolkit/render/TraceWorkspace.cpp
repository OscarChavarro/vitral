#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/TraceWorkspace.h"
TraceWorkspace::TraceWorkspace()
    : traversalCandidateHit(RayHit::DETAIL_NONE, false),
      nearestHit(RayHit::DETAIL_NONE, false),
      shadowCandidateHitObj(RayHit::DETAIL_NONE, false)
{
    initialize(DEFAULT_MAX_RECURSION_LEVEL);
}

TraceWorkspace::TraceWorkspace(int maxRecursionLevel)
    : traversalCandidateHit(RayHit::DETAIL_NONE, false),
      nearestHit(RayHit::DETAIL_NONE, false),
      shadowCandidateHitObj(RayHit::DETAIL_NONE, false)
{
    initialize(maxRecursionLevel);
}

void TraceWorkspace::initialize(int maxRecursionLevel)
{
    int levels = maxRecursionLevel + 1;
    reflectionHits.reserve((long int)levels);
    shadingHits.reserve((long int)levels);
    reflectionColors.reserve((long int)levels);
    for ( int i = 0; i < levels; i++ ) {
        reflectionHits.add(RayHit(RayHit::DETAIL_NONE, false));
        shadingHits.add(RayHit());
        reflectionColors.add(ColorRgb());
    }
}

RayHit* TraceWorkspace::shadowCandidateHit()
{
    return &shadowCandidateHitObj;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int TraceWorkspace::DEFAULT_MAX_RECURSION_LEVEL;

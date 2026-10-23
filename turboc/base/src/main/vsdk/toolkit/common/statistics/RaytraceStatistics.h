#ifndef __RAYTRACE_STATISTICS__
#define __RAYTRACE_STATISTICS__

#include "vsdk/toolkit/common/VSDK.h"

class RaytraceStatistics {
public:
    static BOOLEAN isEnabled();

    static void recordPrimaryRay();
    static void recordShadowRay();
    static void recordReflectionRay();
    static void recordSceneTraversal();
    static void recordObjectIntersectionTest();
    static void recordRayWithT();
    static void recordRayHitInstance();
    static void recordHitInfoClone();
    static void recordGeometryDetailComputation();
    static void printSummary();
};

#endif

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
BOOLEAN RaytraceStatistics::isEnabled() { return FALSE; }
void RaytraceStatistics::recordPrimaryRay() {}
void RaytraceStatistics::recordShadowRay() {}
void RaytraceStatistics::recordReflectionRay() {}
void RaytraceStatistics::recordSceneTraversal() {}
void RaytraceStatistics::recordObjectIntersectionTest() {}
void RaytraceStatistics::recordRayWithT() {}
void RaytraceStatistics::recordRayHitInstance() {}
void RaytraceStatistics::recordHitInfoClone() {}
void RaytraceStatistics::recordGeometryDetailComputation() {}
void RaytraceStatistics::printSummary() {}

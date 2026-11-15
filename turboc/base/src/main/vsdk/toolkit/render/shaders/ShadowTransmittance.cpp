#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/render/TraceWorkspace.h"
#include "vsdk/toolkit/render/shaders/ShadowTransmittance.h"

double ShadowTransmittance::evaluate(
    const Ray& shadowRay,
    double maxShadowDistance,
    ArrayList<SimpleBody*>& objects,
    TraceWorkspace* workspace,
    BOOLEAN recordStatistics)
{
    if ( workspace == 0 ) return 1.0;

    RayHit* shadowCandidateHit = workspace->shadowCandidateHit();
    shadowCandidateHit->setStoreRay(FALSE);

    double factor = 1.0;
    for ( long int i = 0; i < objects.size(); i++ ) {
        SimpleBody* candidateObject = objects.get(i);
        shadowCandidateHit->resetForDistanceOnly();
        if ( recordStatistics ) {
            RaytraceStatistics::recordShadowRay();
        }
        if ( candidateObject == 0 ||
             !candidateObject->doIntersectionFirstHit(shadowRay, shadowCandidateHit) ) {
            continue;
        }
        double hitDistance = shadowCandidateHit->getHitDistance();
        if ( hitDistance <= VSDK::EPSILON || hitDistance >= maxShadowDistance ) {
            continue;
        }
        SimpleMaterial* material = candidateObject->getMaterial();
        double kt = (material != 0) ? material->getRefractionCoefficient() : 0.0;
        if ( kt <= 0.0 ) {
            return 0.0;
        }
        factor *= kt;
    }
    return factor;
}

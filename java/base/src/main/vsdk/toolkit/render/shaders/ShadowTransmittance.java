package vsdk.toolkit.render.shaders;

import java.util.List;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.statistics.RaytraceStatistics;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.TraceWorkspace;

public class ShadowTransmittance {
    public static double evaluate(Ray shadowRay, double maxShadowDistance, List<SimpleBody> objects, TraceWorkspace workspace, boolean recordStatistics) {
    if ( workspace == null ) return 1.0;

    RayHit shadowCandidateHit = workspace.shadowCandidateHit();
    shadowCandidateHit.setStoreRay(false);

    double factor = 1.0;
    for ( long i = 0; i < objects.size(); i++ ) {
        SimpleBody candidateObject = objects.get((int)i);
        shadowCandidateHit.resetForDistanceOnly();
        if ( recordStatistics ) {
            RaytraceStatistics.recordShadowRay();
        }
        if ( candidateObject == null ||
             !candidateObject.doIntersectionFirstHit(shadowRay, shadowCandidateHit) ) {
            continue;
        }
        double hitDistance = shadowCandidateHit.getHitDistance();
        if ( hitDistance <= VSDK.EPSILON || hitDistance >= maxShadowDistance ) {
            continue;
        }
        SimpleMaterial material = candidateObject.getMaterial();
        double kt = (material != null) ? material.getRefractionCoefficient() : 0.0;
        if ( kt <= 0.0 ) {
            return 0.0;
        }
        factor *= kt;
    }
    return factor;
}
}


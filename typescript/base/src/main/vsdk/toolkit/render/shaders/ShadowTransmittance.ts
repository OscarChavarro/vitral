import type { List } from "../../../../java/util/List.js";
import { VSDK } from "../../common/VSDK.js";
import { RaytraceStatistics } from "../../common/statistics/RaytraceStatistics.js";
import type { Ray } from "../../environment/geometry/element/Ray.js";
import type { RayHit } from "../../environment/geometry/element/RayHit.js";
import type { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import type { TraceWorkspace } from "../TraceWorkspace.js";

/**
Fraction of a light's energy that reaches a shaded point along a shadow
ray.  An opaque blocker between the point and the light stops the light
(factor 0).  A transmissive blocker (refraction coefficient kt > 0) lets
the fraction kt pass, so a glass body casts a tinted shadow instead of a
black one, as in Whitted's model [WHIT1980], where shadow rays crossing
transparent objects are attenuated by their transmission coefficient.
The shadow ray is not bent at the interfaces (no caustics).
*/
export class ShadowTransmittance {
    public static evaluate(
        shadowRay: Ray,
        maxShadowDistance: number,
        objects: List<SimpleBody | null>,
        workspace: TraceWorkspace | null,
        recordStatistics: boolean,
    ): number {
        if (workspace === null) return 1.0;

        const shadowCandidateHit: RayHit = workspace.shadowCandidateHit();
        shadowCandidateHit.setStoreRay(false);

        let factor: number = 1.0;
        for (let i = 0; i < objects.size(); i++) {
            const candidateObject: SimpleBody | null = objects.get(i);
            shadowCandidateHit.resetForDistanceOnly();
            if (recordStatistics) {
                RaytraceStatistics.recordShadowRay();
            }
            if (candidateObject === null || !candidateObject.doIntersectionFirstHit(shadowRay, shadowCandidateHit)) {
                continue;
            }
            const hitDistance: number = shadowCandidateHit.getHitDistance();
            if (hitDistance <= VSDK.EPSILON || hitDistance >= maxShadowDistance) {
                continue;
            }
            const material: SimpleMaterial | null = candidateObject.getMaterial();
            const kt: number = material !== null ? material.getRefractionCoefficient() : 0.0;
            if (kt <= 0.0) {
                return 0.0;
            }
            factor *= kt;
        }
        return factor;
    }
}

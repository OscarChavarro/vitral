#ifndef __SHADOW_TRANSMITTANCE__
#define __SHADOW_TRANSMITTANCE__

#include "java/util/ArrayList.h"

class Ray;
class SimpleBody;
class TraceWorkspace;

/**
Fraction of a light's energy that reaches a shaded point along a shadow
ray.  An opaque blocker between the point and the light stops the light
(factor 0).  A transmissive blocker (refraction coefficient kt > 0) lets
the fraction kt pass, so a glass body casts a tinted shadow instead of a
black one, as in Whitted's model [WHIT1980], where shadow rays crossing
transparent objects are attenuated by their transmission coefficient.
The shadow ray is not bent at the interfaces (no caustics).
*/
class ShadowTransmittance {
public:
    static double evaluate(
        const Ray& shadowRay,
        double maxShadowDistance,
        java::ArrayList<SimpleBody*>& objects,
        TraceWorkspace* workspace,
        bool recordStatistics);
};

#endif

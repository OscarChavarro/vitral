#include "SimpleBody.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"

SimpleBody::SimpleBody() : position(0,0,0), geometry(0) {}
void SimpleBody::setPosition(const Vector3Dd& p) { position = p; }
void SimpleBody::setGeometry(Geometry* g) { geometry = g; }

Ray* SimpleBody::doIntersection(const Ray& inRay) const
{
    if (geometry == 0) return 0;
    Ray local(inRay.origin().subtract(position), inRay.direction());
    RayHit hit(RayHit::DETAIL_NONE, true);
    if (!geometry->doIntersection(local, &hit) || hit.ray() == 0) return 0;
    Ray out(hit.ray()->origin().add(position), hit.ray()->direction());
    out = out.withT(hit.ray()->t());
    return new Ray(out);
}

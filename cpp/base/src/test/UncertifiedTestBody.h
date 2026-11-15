#ifndef __UNCERTIFIED_TEST_BODY__
#define __UNCERTIFIED_TEST_BODY__
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
// An application body that preserves its core solid for equivalence but
// cannot certify point containment through its own policies.
class UncertifiedTestBody : public SimpleBody {
public:
    GeometryContainment classifyPointCertified(const Vector3Dd&, double) const override;
    const Geometry* getSolidEquivalenceGeometry() const override;
};
#endif

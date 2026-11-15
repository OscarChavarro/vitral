#ifndef __UNCERTIFIED_TEST_BODY__
#define __UNCERTIFIED_TEST_BODY__
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
// An application body that preserves its core solid for equivalence but
// cannot certify point containment through its own policies.
class UncertifiedTestBody : public SimpleBody {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd&, double) const;
    virtual const Geometry* getSolidEquivalenceGeometry() const;
};
#endif

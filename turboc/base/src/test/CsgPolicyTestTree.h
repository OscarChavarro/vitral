#ifndef __CSG_POLICY_TEST_TREE__
#define __CSG_POLICY_TEST_TREE__
#include "vsdk/toolkit/environment/scene/csg/CsgTree.h"
class CsgPolicyTestTree : public CsgTree {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    CsgPolicyTestTree();
};
#endif

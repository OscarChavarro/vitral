#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExpression.h"
CsgExpression::~CsgExpression() {}

BOOLEAN CsgExpression::isStructuralTraversalSafe() const { return FALSE; }

BOOLEAN CsgExpression::queryStructureVersion(long long&) const { return FALSE; }

const Class CsgExpression::CLASS = { "CsgExpression", NULL };

const Class *
CsgExpression::getClass() const
{
    return &CLASS;
}

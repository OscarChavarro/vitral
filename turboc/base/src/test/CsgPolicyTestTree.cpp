#include "CsgPolicyTestTree.h"
CsgPolicyTestTree::CsgPolicyTestTree() : CsgTree(CsgOperation::UNION) {}

const Class CsgPolicyTestTree::CLASS = { "CsgPolicyTestTree", &CsgTree::CLASS };

const Class *
CsgPolicyTestTree::getClass() const
{
    return &CLASS;
}

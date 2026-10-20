#include "vsdk/toolkit/gui/dialog/components/DialogTreeNode.h"

DialogTreeNode::DialogTreeNode(const java::String& label, const void* identity)
    : label(label), identity(identity)
{
}

const java::String& DialogTreeNode::getLabel() const
{
    return label;
}

const void* DialogTreeNode::getIdentity() const
{
    return identity;
}

const std::vector<DialogTreeNode>& DialogTreeNode::getChildren() const
{
    return children;
}

DialogTreeNode& DialogTreeNode::add(const DialogTreeNode& child)
{
    children.push_back(child);
    return children.back();
}

java::String DialogTreeNode::toString() const
{
    return label;
}

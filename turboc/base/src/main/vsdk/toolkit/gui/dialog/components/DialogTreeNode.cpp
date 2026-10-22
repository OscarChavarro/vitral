#include "vsdk/toolkit/gui/dialog/components/DialogTreeNode.h"
#include "java/util/ArrayList.txx"

DialogTreeNode::DialogTreeNode()
    : label(""), identity(NULL)
{
}

DialogTreeNode::DialogTreeNode(const String& label, const void* identity)
    : label(label), identity(identity)
{
}

const String& DialogTreeNode::getLabel() const
{
    return label;
}

const void* DialogTreeNode::getIdentity() const
{
    return identity;
}

const ArrayList<DialogTreeNode>& DialogTreeNode::getChildren() const
{
    return children;
}

DialogTreeNode& DialogTreeNode::add(const DialogTreeNode& child)
{
    children.add(child);
    return children[children.size() - 1];
}

String DialogTreeNode::toString() const
{
    return label;
}

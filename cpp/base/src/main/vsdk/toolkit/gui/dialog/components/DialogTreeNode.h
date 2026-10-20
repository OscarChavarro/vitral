#ifndef __DIALOG_TREE_NODE__
#define __DIALOG_TREE_NODE__

#include <vector>

#include "java/lang/String.h"

/**
A hierarchy row with identity separate from its display label: the
content of the `tree` elements of the dialog definitions, built by the
application and shown by each GUI technology.

C++ port note: the identity is an untyped pointer (an `Object` in Java),
not owned by the node, and the children are kept by value.
*/
class DialogTreeNode {
private:
    java::String label;
    const void* identity;
    std::vector<DialogTreeNode> children;

public:
    /**
    @param label text of the row
    @param identity what the row stands for, for the application (not
    owned), or null for rows that only group others
    */
    DialogTreeNode(const java::String& label, const void* identity);

    const java::String& getLabel() const;
    const void* getIdentity() const;
    const std::vector<DialogTreeNode>& getChildren() const;

    /**
    @return the child added (a copy of the given node)
    */
    DialogTreeNode& add(const DialogTreeNode& child);
    java::String toString() const;
};

#endif

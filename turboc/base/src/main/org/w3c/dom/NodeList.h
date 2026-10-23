#ifndef __ORG_W3C_DOM_NODE_LIST__
#define __ORG_W3C_DOM_NODE_LIST__

#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
// Inline methods below instantiate the list
#include "java/util/ArrayList.txx"


class Node;

/**
Minimal emulation of `org.w3c.dom.NodeList`: a non-owning view of nodes.
*/
class NodeList {
private:
    ArrayList<Node*> nodes;

public:
    NodeList() {}
    explicit NodeList(const ArrayList<Node*>& nodes) : nodes(nodes) {}

    int getLength() const
    {
        return (int)nodes.size();
    }

    /**
    @param index position of the node
    @return node at given position, or null if out of range
    */
    Node* item(int index) const
    {
        if ( index < 0 || index >= (int)nodes.size() ) {
            return NULL;
        }
        return nodes.get(index);
    }
};


#endif

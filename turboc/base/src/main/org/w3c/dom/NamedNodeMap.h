#ifndef __ORG_W3C_DOM_NAMED_NODE_MAP__
#define __ORG_W3C_DOM_NAMED_NODE_MAP__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"


class Node;

/**
Minimal emulation of `org.w3c.dom.NamedNodeMap`: a non-owning view of the
attribute nodes of an element.
*/
class NamedNodeMap {
private:
    const ArrayList<Node*>* nodes;

public:
    explicit NamedNodeMap(const ArrayList<Node*>* nodes) : nodes(nodes) {}

    int getLength() const;
    Node* item(int index) const;

    /**
    @param name attribute name
    @return attribute node with given name, or null if not present
    */
    Node* getNamedItem(const String& name) const;
};


#endif

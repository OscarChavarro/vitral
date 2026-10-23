#ifndef __ORG_W3C_DOM_NODE__
#define __ORG_W3C_DOM_NODE__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"


class NodeList;
class NamedNodeMap;

/**
Minimal emulation of the `org.w3c.dom.Node` interface: element, attribute,
text and document nodes, as used by Vitral XML persistence classes. A node
owns its children and its attributes.
*/
class Node {
public:
    enum {
        ELEMENT_NODE = 1,
        ATTRIBUTE_NODE = 2,
        TEXT_NODE = 3,
        DOCUMENT_NODE = 9
    };

    Node(short nodeType, const String& nodeName,
         const String& nodeValue);
    virtual ~Node();

    short getNodeType() const;
    const String& getNodeName() const;

    /**
    @return the local name for element and attribute nodes, null otherwise
    */
    const String* getLocalName() const;

    /**
    @return node value for attribute and text nodes, empty otherwise
    */
    const String& getNodeValue() const;
    void setNodeValue(const String& nodeValue);

    Node* getParentNode() const;
    Node* getFirstChild() const;
    NodeList getChildNodes() const;

    /**
    Appends a child, taking its ownership.
    @param child node to append; it must not have a parent
    @return the appended child
    */
    Node* appendChild(Node* child);

    /**
    @return attributes of element nodes, null for other node types
    */
    const NamedNodeMap* getAttributes() const;

    /**
    @return concatenation of the values of all descendant text nodes
    */
    String getTextContent() const;

    /**
    Replaces all children with a single text node.
    @param textContent new text
    */
    void setTextContent(const String& textContent);

protected:
    ArrayList<Node*> attributes;

private:
    short nodeType;
    String nodeName;
    String nodeValue;
    Node* parentNode;
    ArrayList<Node*> children;
    NamedNodeMap* attributeMap;

    Node(const Node& other);
    Node& operator=(const Node& other);

    friend class Element;
};


#endif

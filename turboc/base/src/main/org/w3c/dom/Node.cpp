#include <stddef.h>
#include "java/util/ArrayList.txx"
#include "org/w3c/dom/Document.h"
#include "org/w3c/dom/Element.h"
#include "org/w3c/dom/NamedNodeMap.h"
#include "org/w3c/dom/Node.h"
#include "org/w3c/dom/NodeList.h"


//= Node ======================================================================

Node::Node(short nodeType, const String& nodeName,
           const String& nodeValue)
    : nodeType(nodeType), nodeName(nodeName), nodeValue(nodeValue),
      parentNode(NULL), attributeMap(NULL)
{
    if ( nodeType == ELEMENT_NODE ) {
        attributeMap = new NamedNodeMap(&attributes);
    }
}

Node::~Node()
{
    long i;
    for ( i = 0; i < children.size(); i++ ) {
        delete children.get(i);
    }
    for ( i = 0; i < attributes.size(); i++ ) {
        delete attributes.get(i);
    }
    delete attributeMap;
}

short Node::getNodeType() const
{
    return nodeType;
}

const String& Node::getNodeName() const
{
    return nodeName;
}

const String* Node::getLocalName() const
{
    if ( nodeType != ELEMENT_NODE && nodeType != ATTRIBUTE_NODE ) {
        return NULL;
    }
    return &nodeName;
}

const String& Node::getNodeValue() const
{
    return nodeValue;
}

void Node::setNodeValue(const String& value)
{
    nodeValue = value;
}

Node* Node::getParentNode() const
{
    return parentNode;
}

Node* Node::getFirstChild() const
{
    if ( children.size() == 0 ) {
        return NULL;
    }
    return children.get(0);
}

NodeList Node::getChildNodes() const
{
    return NodeList(children);
}

Node* Node::appendChild(Node* child)
{
    if ( child == NULL ) {
        return NULL;
    }
    child->parentNode = this;
    children.add(child);
    return child;
}

const NamedNodeMap* Node::getAttributes() const
{
    return attributeMap;
}

String Node::getTextContent() const
{
    if ( nodeType == TEXT_NODE || nodeType == ATTRIBUTE_NODE ) {
        return nodeValue;
    }
    String text;
    long i;
    for ( i = 0; i < children.size(); i++ ) {
        text += children.get(i)->getTextContent();
    }
    return text;
}

void Node::setTextContent(const String& textContent)
{
    if ( nodeType == TEXT_NODE || nodeType == ATTRIBUTE_NODE ) {
        nodeValue = textContent;
        return;
    }
    long i;
    for ( i = 0; i < children.size(); i++ ) {
        delete children.get(i);
    }
    children.clear();
    appendChild(new Node(TEXT_NODE, "#text", textContent));
}

//= NamedNodeMap ==============================================================

int NamedNodeMap::getLength() const
{
    return (int)nodes->size();
}

Node* NamedNodeMap::item(int index) const
{
    if ( index < 0 || index >= (int)nodes->size() ) {
        return NULL;
    }
    return nodes->get(index);
}

Node* NamedNodeMap::getNamedItem(const String& name) const
{
    long i;
    for ( i = 0; i < nodes->size(); i++ ) {
        if ( nodes->get(i)->getNodeName().equals(name) ) {
            return nodes->get(i);
        }
    }
    return NULL;
}

//= Element ===================================================================

Element::Element(const String& tagName)
    : Node(ELEMENT_NODE, tagName, "")
{
}

const String& Element::getTagName() const
{
    return getNodeName();
}

String Element::getAttribute(const String& name) const
{
    Node* attribute = getAttributes()->getNamedItem(name);
    if ( attribute == NULL ) {
        return String("");
    }
    return attribute->getNodeValue();
}

bool Element::hasAttribute(const String& name) const
{
    return getAttributes()->getNamedItem(name) != NULL;
}

void Element::setAttribute(const String& name,
                           const String& value)
{
    Node* attribute = getAttributes()->getNamedItem(name);
    if ( attribute != NULL ) {
        attribute->setNodeValue(value);
        return;
    }
    attribute = new Node(ATTRIBUTE_NODE, name, value);
    attribute->parentNode = this;
    attributes.add(attribute);
}

//= Document ==================================================================

Document::Document() : Node(DOCUMENT_NODE, "#document", "")
{
}

Element* Document::createElement(const String& tagName)
{
    return new Element(tagName);
}

Node* Document::createTextNode(const String& data)
{
    return new Node(TEXT_NODE, "#text", data);
}

Element* Document::getDocumentElement() const
{
    NodeList list = getChildNodes();
    int i;
    for ( i = 0; i < list.getLength(); i++ ) {
        if ( list.item(i)->getNodeType() == ELEMENT_NODE ) {
            return ((Element*)(list.item(i)));
        }
    }
    return NULL;
}

static void collectElementsByTagName(const Node* node,
                                     const String& tagName,
                                     ArrayList<Node*>& out)
{
    NodeList list = node->getChildNodes();
    int i;
    for ( i = 0; i < list.getLength(); i++ ) {
        Node* child = list.item(i);
        if ( child->getNodeType() == Node::ELEMENT_NODE ) {
            if ( child->getNodeName().equals(tagName) ) {
                out.add(child);
            }
            collectElementsByTagName(child, tagName, out);
        }
    }
}

NodeList Document::getElementsByTagName(const String& tagName) const
{
    ArrayList<Node*> out;
    collectElementsByTagName(this, tagName, out);
    return NodeList(out);
}

void Document::normalizeDocument()
{
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const short Node::ELEMENT_NODE;
const short Node::ATTRIBUTE_NODE;
const short Node::TEXT_NODE;
const short Node::DOCUMENT_NODE;

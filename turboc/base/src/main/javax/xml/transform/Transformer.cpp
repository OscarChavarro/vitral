#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "javax/xml/transform/Transformer.h"
#include "org/w3c/dom/Document.h"
#include "org/w3c/dom/Element.h"
#include "org/w3c/dom/NamedNodeMap.h"
#include "org/w3c/dom/NodeList.h"


const char* const Transformer::DOCTYPE_SYSTEM = "doctype-system";
const char* const Transformer::INDENT = "indent";
const char* const Transformer::ENCODING = "encoding";


static String escape(const String& in, BOOLEAN inAttribute)
{
String out;
char buffer[2] = {'\0', '\0'};
int i;
for ( i = 0; i < in.length(); i++ ) {
    char c = in.charAt(i);
    if ( c == '<' ) {
        out += "&lt;";
    }
    else if ( c == '>' ) {
        out += "&gt;";
    }
    else if ( c == '&' ) {
        out += "&amp;";
    }
    else if ( c == '"' && inAttribute ) {
        out += "&quot;";
    }
    else {
        buffer[0] = c;
        out += buffer;
    }
}
return out;
}

static void writeIndent(String& out, BOOLEAN indent, int level)
{
if ( !indent ) {
    return;
}
int i;
for ( i = 0; i < level; i++ ) {
    out += "    ";
}
}

static void writeNode(String& out, const Node* node,
           BOOLEAN indent, int level)
{
if ( node->getNodeType() == Node::TEXT_NODE ) {
    out += escape(node->getNodeValue(), FALSE);
    return;
}
if ( node->getNodeType() != Node::ELEMENT_NODE ) {
    return;
}

writeIndent(out, indent, level);
out += "<";
out += node->getNodeName();
const NamedNodeMap* attributes = node->getAttributes();
int i;
for ( i = 0; attributes != NULL && i < attributes->getLength(); i++ ) {
    out += " ";
    out += attributes->item(i)->getNodeName();
    out += "=\"";
    out += escape(attributes->item(i)->getNodeValue(), TRUE);
    out += "\"";
}

NodeList children = node->getChildNodes();
if ( children.getLength() == 0 ) {
    out += "/>";
    if ( indent ) {
        out += "\n";
    }
    return;
}

BOOLEAN onlyText = TRUE;
for ( i = 0; i < children.getLength(); i++ ) {
    if ( children.item(i)->getNodeType() !=
         Node::TEXT_NODE ) {
        onlyText = FALSE;
    }
}

out += ">";
if ( onlyText ) {
    for ( i = 0; i < children.getLength(); i++ ) {
        writeNode(out, children.item(i), indent, level + 1);
    }
}
else {
    if ( indent ) {
        out += "\n";
    }
    for ( i = 0; i < children.getLength(); i++ ) {
        writeNode(out, children.item(i), indent, level + 1);
    }
    writeIndent(out, indent, level);
}
out += "</";
out += node->getNodeName();
out += ">";
if ( indent ) {
    out += "\n";
}
}


Transformer::Transformer() : doctypeSystem(""), indent(FALSE),
    encoding("UTF-8")
{
}

void Transformer::setOutputProperty(const String& name,
                                    const String& value)
{
    if ( name.equals(DOCTYPE_SYSTEM) ) {
        doctypeSystem = value;
    }
    else if ( name.equals(INDENT) ) {
        indent = value.equals("yes");
    }
    else if ( name.equals(ENCODING) ) {
        encoding = value;
    }
}

String Transformer::toXmlText(
    const Document* document) const
{
    String out;
    out += "<?xml version=\"1.0\" encoding=\"";
    out += encoding;
    out += "\" standalone=\"no\"?>";
    const Element* root = NULL;
    if ( document != NULL ) {
        root = document->getDocumentElement();
    }
    if ( root != NULL && !doctypeSystem.isEmpty() ) {
        if ( indent ) {
            out += "\n";
        }
        out += "<!DOCTYPE ";
        out += root->getNodeName();
        out += " SYSTEM \"";
        out += doctypeSystem;
        out += "\">";
    }
    if ( indent ) {
        out += "\n";
    }
    if ( root != NULL ) {
        writeNode(out, root, indent, 0);
    }
    return out;
}

BOOLEAN Transformer::transform(const Document* document,
                            const File& file) const
{
    String xmlText = toXmlText(document);
    FILE* fd = fopen(file.getPath().c_str(), "wb");
    if ( fd == NULL ) {
        return FALSE;
    }
    size_t n = (size_t)xmlText.length();
    BOOLEAN ok = fwrite(xmlText.c_str(), 1, n, fd) == n;
    fclose(fd);
    return ok;
}


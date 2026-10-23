#ifndef __ORG_W3C_DOM_ELEMENT__
#define __ORG_W3C_DOM_ELEMENT__

#include "vsdk/toolkit/common/VSDK.h"
#include "org/w3c/dom/Node.h"


/**
Minimal emulation of `org.w3c.dom.Element`.
*/
class Element : public Node {
public:
    explicit Element(const String& tagName);
    virtual ~Element() {}

    const String& getTagName() const;

    /**
    @param name attribute name
    @return attribute value, or empty string if not present
    */
    String getAttribute(const String& name) const;
    BOOLEAN hasAttribute(const String& name) const;
    void setAttribute(const String& name, const String& value);
};


#endif

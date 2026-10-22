#ifndef __PRESENTATION_ELEMENT__
#define __PRESENTATION_ELEMENT__

#include "java/lang/Class.h"

class PresentationElement {
public:
    virtual ~PresentationElement() {}

    /// Run time class descriptor (see `Class`)
    static const Class CLASS;

    /**
    @return the descriptor of the class of this object (Java `getClass()`)
    */
    virtual const Class *getClass() const;

    /**
    @return true if this object is an instance of the class or of one of
    its subclasses (Java `instanceof`)
    */
    bool instanceOf(const Class &type) const;
};


#endif

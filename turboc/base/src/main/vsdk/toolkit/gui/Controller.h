#ifndef __CONTROLLER__
#define __CONTROLLER__

#include "vsdk/toolkit/gui/PresentationElement.h"
class Controller : public PresentationElement {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    virtual ~Controller() {}
};


#endif

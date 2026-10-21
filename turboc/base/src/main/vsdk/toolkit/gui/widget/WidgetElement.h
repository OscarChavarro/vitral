#ifndef __WIDGET_ELEMENT__
#define __WIDGET_ELEMENT__

#include <stddef.h>
#include "java/lang/String.h"
#include "vsdk/toolkit/gui/PresentationElement.h"

class Widget;

class WidgetElement : public PresentationElement {
protected:
    Widget* context;

public:
    WidgetElement() : context(NULL) {}
    virtual ~WidgetElement() {}

    virtual String toString() const = 0;
};

#endif

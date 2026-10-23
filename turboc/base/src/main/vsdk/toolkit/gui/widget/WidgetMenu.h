#ifndef __WIDGET_MENU__
#define __WIDGET_MENU__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuElement.h"

/**
Menu (internal node of a menu tree). Children are owned by the `Widget`
context, not by the menu, as a submenu can also be registered as a popup.
*/
class WidgetMenu : public WidgetMenuElement {
private:
    ArrayList<WidgetMenuElement*> children;
    String name;
    char mnemonic;
    String accelerator;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit WidgetMenu(Widget* c);
    virtual ~WidgetMenu() {}

    ArrayList<WidgetMenuElement*>& getChildren();
    void setName(const String& n);
    void addChild(WidgetMenuElement* i);
    virtual String toString(int level) const;
    virtual String toString() const;
    const String& getName() const;
    char getMnemonic() const;
    const String& getAccelerator() const;
};

#endif

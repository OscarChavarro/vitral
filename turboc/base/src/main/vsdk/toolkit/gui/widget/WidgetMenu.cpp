#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"

WidgetMenu::WidgetMenu(Widget* c) : name(""), mnemonic('\0'), accelerator("")
{
    context = c;
}

ArrayList<WidgetMenuElement*>& WidgetMenu::getChildren()
{
    return children;
}

void WidgetMenu::setName(const String& n)
{
    name = processSimplifiedName(n);
    mnemonic = processMnemonic(n);
    accelerator = processAccelerator(n);
}

void WidgetMenu::addChild(WidgetMenuElement* i)
{
    children.add(i);
}

String WidgetMenu::toString(int level) const
{
    String leadingSpace = "";
    int j;

    for ( j = 0; j < level; j++ ) {
        leadingSpace = leadingSpace + "  ";
    }

    String msg = leadingSpace + "Menu \"" + name + "\"\n";

    int i;

    for ( i = 0; i < children.size(); i++ ) {
        msg = msg + (children.get(i))->toString(level+1);
    }

    return msg;
}

String WidgetMenu::toString() const
{
    return toString(0);
}

const String& WidgetMenu::getName() const
{
    return name;
}

char WidgetMenu::getMnemonic() const
{
    return mnemonic;
}

const String& WidgetMenu::getAccelerator() const
{
    return accelerator;
}

const Class WidgetMenu::CLASS = { "WidgetMenu", &WidgetMenuElement::CLASS };

const Class *
WidgetMenu::getClass() const
{
    return &CLASS;
}

#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"

WidgetMenuItem::WidgetMenuItem(Widget* c)
    : name(""), hasName(false), commandName(""), hasCommandName(false),
      isSeparatorFlag(false), mnemonic(0), accelerator("")
{
    context = c;
}

bool WidgetMenuItem::isSeparator() const
{
    return isSeparatorFlag;
}

void WidgetMenuItem::setName(const String& n)
{
    name = processSimplifiedName(n);
    hasName = true;
    mnemonic = processMnemonic(n);
    accelerator = processAccelerator(n);
}

String WidgetMenuItem::getName() const
{
    if ( !hasName ) return "No Name";
    return name;
}

String WidgetMenuItem::getCommandName() const
{
    if ( !hasCommandName ) return "IDC_NO_COMMAND";
    return commandName;
}

void WidgetMenuItem::setCommandName(const String& a)
{
    commandName = a;
    hasCommandName = true;
}

void WidgetMenuItem::addModifier(const String& m)
{
    if ( m.equals("CHECKED") ) {
    }
    else if ( m.equals("GRAYED") ) {
    }
    else if ( m.equals("UNCHEKED") ) {
    }
    else if ( m.equals("SEPARATOR") ) {
        isSeparatorFlag = true;
    }
    else {
        setCommandName(m);
    }
}

String WidgetMenuItem::toString(int level) const
{
    String leadingSpace = "";
    int j;

    for ( j = 0; j < level; j++ ) {
        leadingSpace = leadingSpace + "  ";
    }

    String msg = leadingSpace + " - MenuItem ";

    if ( isSeparatorFlag ) {
        msg = msg + "--- SEPARATOR ---";
    }
    else {
        msg = msg + "\"" + name + "\"";
    }

    msg = msg + "\n";

    return msg;
}

String WidgetMenuItem::toString() const
{
    return toString(0);
}

char WidgetMenuItem::getMnemonic() const
{
    return mnemonic;
}

const String& WidgetMenuItem::getAccelerator() const
{
    return accelerator;
}

const Class WidgetMenuItem::CLASS = { "WidgetMenuItem", &WidgetMenuElement::CLASS };

const Class *
WidgetMenuItem::getClass() const
{
    return &CLASS;
}

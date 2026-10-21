#include <stddef.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetButtonGroup.h"

WidgetButtonGroup::WidgetButtonGroup(Widget* parent)
    : name(""), showText(false), showIcons(false), showTitle(false),
      direction(0)
{
    context = parent;
}

void WidgetButtonGroup::setShowText(bool f)
{
    showText = f;
}

void WidgetButtonGroup::setShowIcons(bool f)
{
    showIcons = f;
}

void WidgetButtonGroup::setTitle(bool f)
{
    showTitle = f;
}

void WidgetButtonGroup::setDirection(int d)
{
    direction = d;
}

int WidgetButtonGroup::getDirection() const
{
    return direction;
}

bool WidgetButtonGroup::isShowTextSet() const
{
    return showText;
}

bool WidgetButtonGroup::isShowIconsSet() const
{
    return showIcons;
}

bool WidgetButtonGroup::isShowTitleSet() const
{
    return showTitle;
}

ArrayList<WidgetCommand*>& WidgetButtonGroup::getCommands()
{
    return commands;
}

void WidgetButtonGroup::setName(const String& n)
{
    name = n;
}

const String& WidgetButtonGroup::getName() const
{
    return name;
}

void WidgetButtonGroup::addCommandByName(const String& commandName)
{
    if ( context == NULL ) {
        return;
    }
    WidgetCommand* command = context->getCommandByName(commandName);

    if ( command != NULL ) {
        commands.add(command);
    }
}

String WidgetButtonGroup::toString() const
{
    return String("ButtonGroup \"") + name + "\" with " +
        String::valueOf((long)commands.size()) + " commands\n";
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int WidgetButtonGroup::VERTICAL;

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int WidgetButtonGroup::HORIZONTAL;

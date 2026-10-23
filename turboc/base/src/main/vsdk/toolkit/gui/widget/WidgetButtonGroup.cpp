#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetButtonGroup.h"

WidgetButtonGroup::WidgetButtonGroup(Widget* parent)
    : name(""), showText(FALSE), showIcons(FALSE), showTitle(FALSE),
      direction(0)
{
    context = parent;
}

void WidgetButtonGroup::setShowText(BOOLEAN f)
{
    showText = f;
}

void WidgetButtonGroup::setShowIcons(BOOLEAN f)
{
    showIcons = f;
}

void WidgetButtonGroup::setTitle(BOOLEAN f)
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

BOOLEAN WidgetButtonGroup::isShowTextSet() const
{
    return showText;
}

BOOLEAN WidgetButtonGroup::isShowIconsSet() const
{
    return showIcons;
}

BOOLEAN WidgetButtonGroup::isShowTitleSet() const
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

const Class WidgetButtonGroup::CLASS = { "WidgetButtonGroup", &WidgetElement::CLASS };

const Class *
WidgetButtonGroup::getClass() const
{
    return &CLASS;
}

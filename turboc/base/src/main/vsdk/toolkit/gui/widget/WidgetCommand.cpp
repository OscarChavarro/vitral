#include <stddef.h>
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBAPixel.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"

WidgetCommand::WidgetCommand()
    : id(""), name(""), briefDescription(""), help(""), icon(NULL),
      iconTransparency(NULL), secondaryIcon(NULL),
      secondaryIconTransparency(NULL)
{
}

WidgetCommand::~WidgetCommand()
{
    delete icon;
    delete iconTransparency;
    delete secondaryIcon;
    delete secondaryIconTransparency;
}

const String& WidgetCommand::getId() const
{
    return id;
}

const String& WidgetCommand::getName() const
{
    return name;
}

const String& WidgetCommand::getBriefDescription() const
{
    return briefDescription;
}

const String& WidgetCommand::getHelp() const
{
    return help;
}

RGBAImageUncompressed* WidgetCommand::getIcon() const
{
    return icon;
}

RGBImageUncompressed* WidgetCommand::getIconTransparency() const
{
    return iconTransparency;
}

void WidgetCommand::applyTransparency(RGBAImageUncompressed* target,
                                      RGBImageUncompressed* mask)
{
    if ( target == NULL || mask == NULL ) {
        return;
    }

    int xlimit;
    int ylimit;
    int x;
    int y;

    xlimit = target->getXSize() < mask->getXSize() ?
        target->getXSize() : mask->getXSize();
    ylimit = target->getYSize() < mask->getYSize() ?
        target->getYSize() : mask->getYSize();

    RGBPixel in;
    RGBAPixel out;
    int r;
    int g;
    int b;
    int a;

    for ( y = 0; y < ylimit; y++ ) {
        for ( x = 0; x < xlimit; x++ ) {
            mask->getPixelRgb(x, y, &in);
            r = (unsigned char)in.r;
            g = (unsigned char)in.g;
            b = (unsigned char)in.b;
            a = (r + g + b) / 3;
            target->getPixelRgba(x, y, &out);
            out.a = (char)a;
            target->putPixel(x, y, &out);
        }
    }
}

void WidgetCommand::applyTransparency()
{
    applyTransparency(icon, iconTransparency);
}

void WidgetCommand::applySecondTransparency()
{
    applyTransparency(secondaryIcon, secondaryIconTransparency);
}

void WidgetCommand::setId(const String& i)
{
    id = i;
}

void WidgetCommand::setName(const String& n)
{
    name = n;
}

void WidgetCommand::setBrief(const String& b)
{
    briefDescription = b;
}

void WidgetCommand::setHelp(const String& h)
{
    help = h;
}

void WidgetCommand::appendToHelp(const String& h)
{
    help = help + h;
}

void WidgetCommand::setIcon(RGBAImageUncompressed* i)
{
    if ( icon != i ) {
        delete icon;
    }
    icon = i;
}

void WidgetCommand::setIconTransparency(RGBImageUncompressed* i)
{
    if ( iconTransparency != i ) {
        delete iconTransparency;
    }
    iconTransparency = i;
}

String WidgetCommand::toString() const
{
    String msg = String("  - Command [") + id + "]:\n";
    msg = msg + "    . Name: " + name + "\n";
    msg = msg + "    . Brief description: " + briefDescription + "\n";
    if ( icon == NULL ) {
        msg = msg + "    . No icon image\n";
    }
    else {
        msg = msg + "    . Icon image of size (" +
            String::valueOf(icon->getXSize()) + ", " +
            String::valueOf(icon->getYSize()) + ")\n";
    }
    return msg;
}

RGBAImageUncompressed* WidgetCommand::getSecondaryIcon() const
{
    return secondaryIcon;
}

void WidgetCommand::setSecondaryIcon(RGBAImageUncompressed* secondaryIcon)
{
    if ( this->secondaryIcon != secondaryIcon ) {
        delete this->secondaryIcon;
    }
    this->secondaryIcon = secondaryIcon;
}

RGBImageUncompressed* WidgetCommand::getSecondaryIconTransparency() const
{
    return secondaryIconTransparency;
}

void WidgetCommand::setSecondaryIconTransparency(
    RGBImageUncompressed* secondaryIconTransparency)
{
    if ( this->secondaryIconTransparency != secondaryIconTransparency ) {
        delete this->secondaryIconTransparency;
    }
    this->secondaryIconTransparency = secondaryIconTransparency;
}

const Class WidgetCommand::CLASS = { "WidgetCommand", &WidgetElement::CLASS };

const Class *
WidgetCommand::getClass() const
{
    return &CLASS;
}

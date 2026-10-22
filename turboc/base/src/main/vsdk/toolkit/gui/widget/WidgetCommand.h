#ifndef __WIDGET_COMMAND__
#define __WIDGET_COMMAND__

#include "vsdk/toolkit/gui/widget/WidgetElement.h"

class RGBAImageUncompressed;
class RGBImageUncompressed;

/**
This class plays a role of leaf on an n-ary tree in the composite design
pattern. A command owns its icon images.
*/
class WidgetCommand : public WidgetElement {
private:
    String id;
    String name;
    String briefDescription;
    String help; // Could contain HTML tags
    RGBAImageUncompressed* icon;
    RGBImageUncompressed* iconTransparency;
    RGBAImageUncompressed* secondaryIcon;
    RGBImageUncompressed* secondaryIconTransparency;

    static void applyTransparency(RGBAImageUncompressed* target,
                                  RGBImageUncompressed* mask);

    WidgetCommand(const WidgetCommand& other);
    WidgetCommand& operator=(const WidgetCommand& other);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    WidgetCommand();
    virtual ~WidgetCommand();

    const String& getId() const;
    const String& getName() const;
    const String& getBriefDescription() const;
    const String& getHelp() const;
    RGBAImageUncompressed* getIcon() const;
    RGBImageUncompressed* getIconTransparency() const;
    void applyTransparency();
    void applySecondTransparency();
    void setId(const String& i);
    void setName(const String& n);
    void setBrief(const String& b);
    void setHelp(const String& h);
    void appendToHelp(const String& h);
    void setIcon(RGBAImageUncompressed* i);
    void setIconTransparency(RGBImageUncompressed* i);
    virtual String toString() const;
    RGBAImageUncompressed* getSecondaryIcon() const;
    void setSecondaryIcon(RGBAImageUncompressed* secondaryIcon);
    RGBImageUncompressed* getSecondaryIconTransparency() const;
    void setSecondaryIconTransparency(
        RGBImageUncompressed* secondaryIconTransparency);
};

#endif

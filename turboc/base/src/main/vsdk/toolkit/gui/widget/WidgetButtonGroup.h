#ifndef __WIDGET_BUTTON_GROUP__
#define __WIDGET_BUTTON_GROUP__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/gui/widget/WidgetElement.h"

class WidgetCommand;

/**
Group of buttons, referencing commands owned by the `Widget` context.
*/
class WidgetButtonGroup : public WidgetElement {
private:
    ArrayList<WidgetCommand*> commands;
    String name;

    BOOLEAN showText;
    BOOLEAN showIcons;
    BOOLEAN showTitle;
    int direction;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    enum {
        HORIZONTAL = 1,
        VERTICAL = 2
    };

    explicit WidgetButtonGroup(Widget* parent);
    virtual ~WidgetButtonGroup() {}

    void setShowText(BOOLEAN f);
    void setShowIcons(BOOLEAN f);
    void setTitle(BOOLEAN f);
    void setDirection(int d);
    int getDirection() const;
    BOOLEAN isShowTextSet() const;
    BOOLEAN isShowIconsSet() const;
    BOOLEAN isShowTitleSet() const;
    ArrayList<WidgetCommand*>& getCommands();
    void setName(const String& n);
    const String& getName() const;
    void addCommandByName(const String& commandName);
    virtual String toString() const;
};

#endif

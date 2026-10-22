#ifndef __WIDGET_BUTTON_GROUP__
#define __WIDGET_BUTTON_GROUP__

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

    bool showText;
    bool showIcons;
    bool showTitle;
    int direction;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    static const int HORIZONTAL = 1;
    static const int VERTICAL = 2;

    explicit WidgetButtonGroup(Widget* parent);
    virtual ~WidgetButtonGroup() {}

    void setShowText(bool f);
    void setShowIcons(bool f);
    void setTitle(bool f);
    void setDirection(int d);
    int getDirection() const;
    bool isShowTextSet() const;
    bool isShowIconsSet() const;
    bool isShowTitleSet() const;
    ArrayList<WidgetCommand*>& getCommands();
    void setName(const String& n);
    const String& getName() const;
    void addCommandByName(const String& commandName);
    virtual String toString() const;
};

#endif

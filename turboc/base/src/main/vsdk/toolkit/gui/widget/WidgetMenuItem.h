#ifndef __WIDGET_MENU_ITEM__
#define __WIDGET_MENU_ITEM__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuElement.h"

class WidgetMenuItem : public WidgetMenuElement {
private:
    String name;
    BOOLEAN hasName;
    String commandName;
    BOOLEAN hasCommandName;
    BOOLEAN isSeparatorFlag;
    char mnemonic;
    String accelerator;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit WidgetMenuItem(Widget* c);
    virtual ~WidgetMenuItem() {}

    BOOLEAN isSeparator() const;
    void setName(const String& n);
    String getName() const;
    String getCommandName() const;
    void setCommandName(const String& a);
    void addModifier(const String& m);
    virtual String toString(int level) const;
    virtual String toString() const;
    char getMnemonic() const;
    const String& getAccelerator() const;
};

#endif

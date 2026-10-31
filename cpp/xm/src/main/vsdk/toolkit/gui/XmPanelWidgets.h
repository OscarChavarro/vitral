#ifndef __XM_PANEL_WIDGETS__
#define __XM_PANEL_WIDGETS__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"

/**
The panel building blocks with Motif widgets: panels are BulletinBoards
without margins (children at explicit positions), labels are Labels that
keep their size, text fields are TextFields (their activate callback is
Return) and buttons are PushButtons. Option buttons are PushButtons showing
the chosen option, which post a popup menu of the options (a Motif option
menu would size itself to its longest option).

Buttons do not take part in keyboard traversal: clicking them leaves the
keyboard focus in the drawing area of the application.
*/
class XmPanelWidgets : public XtPanelWidgets {
public:
    virtual Widget createPanel(Widget parent, const char* name, int x, int y,
                               int width, int height, bool managed) override;
    virtual Widget createLabel(Widget parent, const java::String& text,
                               XFontSet fontSet, Justify justify,
                               int x, int y, int width, int height) override;
    virtual void setLabel(Widget label, const java::String& text) override;
    virtual Widget createTextField(Widget parent, const java::String& text,
                                   XFontSet fontSet,
                                   int x, int y, int width, int height,
                                   ActivateProc activate,
                                   void* clientData) override;
    virtual java::String getText(Widget field) override;
    virtual void setText(Widget field, const java::String& text) override;
    virtual void setInvalid(Widget field, bool invalid) override;
    virtual void setForeground(Widget widget, const char* colorName) override;
    virtual Widget createPushButton(Widget parent, const java::String& label,
                                    XFontSet fontSet,
                                    int x, int y, int width, int height,
                                    ActivateProc activate,
                                    void* clientData) override;
    virtual Widget createOptionButton(Widget parent,
                                      const java::String& placeholder,
                                      const java::ArrayList<java::String>& options,
                                      XFontSet fontSet,
                                      int x, int y, int width, int height,
                                      OptionProc choose,
                                      void* clientData) override;
};

#endif

#ifndef __XM_PANEL_WIDGETS__
#define __XM_PANEL_WIDGETS__

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
    virtual Widget createLabel(Widget parent, const std::string& text,
                               XFontSet fontSet, Justify justify,
                               int x, int y, int width, int height) override;
    virtual void setLabel(Widget label, const std::string& text) override;
    virtual Widget createTextField(Widget parent, const std::string& text,
                                   XFontSet fontSet,
                                   int x, int y, int width, int height,
                                   ActivateProc activate,
                                   void* clientData) override;
    virtual std::string getText(Widget field) override;
    virtual void setText(Widget field, const std::string& text) override;
    virtual void setInvalid(Widget field, bool invalid) override;
    virtual void setForeground(Widget widget, const char* colorName) override;
    virtual Widget createPushButton(Widget parent, const std::string& label,
                                    XFontSet fontSet,
                                    int x, int y, int width, int height,
                                    ActivateProc activate,
                                    void* clientData) override;
    virtual Widget createOptionButton(Widget parent,
                                      const std::string& placeholder,
                                      const std::vector<std::string>& options,
                                      XFontSet fontSet,
                                      int x, int y, int width, int height,
                                      OptionProc choose,
                                      void* clientData) override;
};

#endif

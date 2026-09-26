#ifndef __XM_WIDGET_SET__
#define __XM_WIDGET_SET__

#include "vsdk/toolkit/gui/XmPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "vsdk/toolkit/render/xm/XmGuiRenderer.h"

/**
The Motif widget set of the Xt applications of vitral:
- the drawing area is a DrawingArea that takes part in the keyboard
  traversal of Motif, which gives it the focus (Motif manages the focus of
  its shells: `XtSetKeyboardFocus` alone does not hold),
- the close requests of the window manager are a protocol callback of the
  shell, whose own response (destroying it) is disabled,
- context menus are popup menus of toggle buttons, checked ones set.
*/
class XmWidgetSet : public XtWidgetSet {
private:
    XmPanelWidgets panelWidgets;
    XmGuiRenderer guiRenderer;

public:
    virtual const char* getName() const override;
    virtual XtPanelWidgets* getPanelWidgets() override;
    virtual XtGuiRenderer* getGuiRenderer() override;
    virtual Widget createDrawingArea(Widget parent, const char* name,
                                     int x, int y, int width,
                                     int height) override;
    virtual void setKeyboardFocus(Widget shell, Widget target) override;
    virtual void setWindowCloseHandler(Widget shell, NotifyProc close,
                                       void* clientData) override;

    /**
    @return the MenuShell of the popup menu (destroying it destroys the
    menu)
    */
    virtual Widget createChoiceMenu(Widget owner,
                                    const std::vector<MenuChoice>& items,
                                    XFontSet fontSet, ChoiceProc choose,
                                    void* clientData) override;
    virtual void popupChoiceMenu(Widget menu, int rootX, int rootY,
                                 NotifyProc poppedDown,
                                 void* clientData) override;
};

#endif

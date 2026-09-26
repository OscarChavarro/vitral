#ifndef __XAW_WIDGET_SET__
#define __XAW_WIDGET_SET__

#include "vsdk/toolkit/gui/XawPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "vsdk/toolkit/render/xaw/XawGuiRenderer.h"

/**
The Athena (Xaw) widget set of the Xt applications of vitral:
- the drawing area is a plain Core widget, which receives the keys typed
  anywhere in its shell through `XtSetKeyboardFocus`,
- the close requests of the window manager arrive as `WM_PROTOCOLS`
  client messages, once the shell declares it takes `WM_DELETE_WINDOW`,
- context menus are SimpleMenus, whose checked items show a check mark
  bitmap. Athena menus do not grab the pointer by themselves when popped up
  from code: they grab it so an outside click only closes them.
*/
class XawWidgetSet : public XtWidgetSet {
private:
    XawPanelWidgets panelWidgets;
    XawGuiRenderer guiRenderer;

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
    virtual Widget createChoiceMenu(Widget owner,
                                    const std::vector<MenuChoice>& items,
                                    XFontSet fontSet, ChoiceProc choose,
                                    void* clientData) override;
    virtual void popupChoiceMenu(Widget menu, int rootX, int rootY,
                                 NotifyProc poppedDown,
                                 void* clientData) override;
};

#endif

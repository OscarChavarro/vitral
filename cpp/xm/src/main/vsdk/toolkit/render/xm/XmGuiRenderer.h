#ifndef __XM_GUI_RENDERER__
#define __XM_GUI_RENDERER__

#include "vsdk/toolkit/render/xt/XtGuiRenderer.h"

/**
Builds Motif widgets from the vitral GUI definition, as `SwingGuiRenderer`
builds the Swing ones: a menubar is a Motif MenuBar with a pulldown menu
per menu (submenus cascade natively), popup menus are Motif popup menus
and button groups are PushButtons in a BulletinBoard.

The width asked for the menu titles is not used: Motif menubars size their
titles to their texts.
*/
class XmGuiRenderer : public XtGuiRenderer {
public:
    virtual XtWidget buildMenubar(XtWidget parent, WidgetMenu* menubar,
                                  CommandListener* executor, XFontSet fontSet,
                                  int x, int y, int width, int height,
                                  int buttonWidth) override;

    /**
    Builds a Motif popup menu (its MenuShell is a popup child of the owner),
    posted by the application only.
    */
    virtual XtWidget buildPopupMenu(XtWidget owner, WidgetMenu* menu,
                                    CommandListener* executor,
                                    XFontSet fontSet) override;

    virtual XtWidget buildButtonGroup(XtWidget parent,
                                      WidgetButtonGroup* group,
                                      CommandListener* executor,
                                      XFontSet fontSet, int x, int y,
                                      int width) override;
};

#endif

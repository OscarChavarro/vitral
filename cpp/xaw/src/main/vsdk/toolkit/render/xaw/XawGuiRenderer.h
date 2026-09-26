#ifndef __XAW_GUI_RENDERER__
#define __XAW_GUI_RENDERER__

#include "vsdk/toolkit/render/xt/XtGuiRenderer.h"

/**
Builds Athena (Xaw) widgets from the vitral GUI definition, as
`SwingGuiRenderer` builds the Swing ones: a menubar is a row of
MenuButtons with SimpleMenu popups, and button groups are Commands.

Xaw has no layout managers for these: the widgets are placed at explicit
positions inside plain Composites.
*/
class XawGuiRenderer : public XtGuiRenderer {
public:
    virtual XtWidget buildMenubar(XtWidget parent, WidgetMenu* menubar,
                                  CommandListener* executor, XFontSet fontSet,
                                  int x, int y, int width, int height,
                                  int buttonWidth) override;

    /**
    Builds a SimpleMenu popup shell, whose submenus pop up to the right of
    their entries when the pointer enters them. Its name is the one to give
    as `XtNmenuName` to a MenuButton.
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

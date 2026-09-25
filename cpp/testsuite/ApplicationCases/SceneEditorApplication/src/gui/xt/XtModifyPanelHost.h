#ifndef __XT_MODIFY_PANEL_HOST__
#define __XT_MODIFY_PANEL_HOST__

#include <X11/Intrinsic.h>

/**
Services of the Xt application that the modify panel and its editors need,
as `AwtApplicationHost` provides them to the Swing ones.
*/
class XtModifyPanelHost {
public:
    virtual ~XtModifyPanelHost() {}

    /**
    Asks for a new frame of the drawing area, after an editor changed the
    body under edition.
    */
    virtual void repaintDrawingArea() = 0;

    /**
    @return the font set of the texts of the panel
    */
    virtual XFontSet getPanelFontSet() = 0;

    /**
    Creates a SimpleMenu popup shell with the visual of the application
    (its GLX visual is not the default one of the screen).
    @param parent widget owning the popup (i.e. its MenuButton)
    @param name name of the popup shell, used as the menu name
    */
    virtual Widget createPopupMenu(Widget parent, const char* name) = 0;
};

#endif

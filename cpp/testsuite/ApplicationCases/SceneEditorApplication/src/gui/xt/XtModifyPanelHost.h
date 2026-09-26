#ifndef __XT_MODIFY_PANEL_HOST__
#define __XT_MODIFY_PANEL_HOST__

#include <X11/Intrinsic.h>

class XtPanelWidgets;

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
    @return the builder of the widgets of the panel, of the widget set the
    application is built with
    */
    virtual XtPanelWidgets* getPanelWidgets() = 0;
};

#endif

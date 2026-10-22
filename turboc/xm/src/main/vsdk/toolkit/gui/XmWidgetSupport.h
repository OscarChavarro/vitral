#ifndef __XM_WIDGET_SUPPORT__
#define __XM_WIDGET_SUPPORT__

#include "java/lang/String.h"

// Xt names its strings and objects as the vitral String and Object
// classes: they are renamed while its headers are read (see
// XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <Xm/Xm.h>
#undef Object
#undef String

#include "vsdk/toolkit/gui/XtWidget.h"

/**
Services shared by the Motif widgets of vitral: texts and fonts in the
Motif way (compound strings and render tables), menu panes with the visual
of their shell, and posting of popup menus.

The application gives the font of the texts as an `XFontSet` (the Xlib way,
shared with Athena); Motif wants render tables, built here from it. Motif
widgets copy the compound strings and render tables they are given, so the
ones created here are freed by the caller right after creating or changing
the widget.

It only takes `XtWidget`s, so it can be used from the sources that also use
the vitral `Widget` class (after "vsdk/toolkit/gui/XmIntrinsics.h").
*/
class XmWidgetSupport {
public:
    /**
    Checks that a shell has the VendorShell of Motif. The dynamic linker
    takes the VendorShell of the first library defining it: if libXt comes
    before libXm, shells get the one of Xt and Motif widgets fail with
    obscure X errors. In that case this ends the program, explaining it.
    */
    static void requireMotifShell(XtWidget shell);

    /**
    @param text UTF-8 text (the locale of the application is UTF-8)
    @return a new compound string (free it with `XmStringFree`)
    */
    static XmString createString(const String& text);

    /**
    @return the text of a compound string
    */
    static String toText(XmString string);

    /**
    @param widget any widget of the display
    @return a new render table with the font set as its default rendition
    (free it with `XmRenderTableFree`), or null if the font set is null
    */
    static XmRenderTable createRenderTable(XtWidget widget, XFontSet fontSet);

    /**
    Changes the text of a label, button or toggle.
    */
    static void setLabelString(XtWidget widget, const String& text);

    /**
    Changes the font of a widget with texts (its `XmNrenderTable`).
    */
    static void setFontSet(XtWidget widget, XFontSet fontSet);

    /**
    Creates a pulldown menu pane for a menubar, an option menu or a cascade
    of a menu, with the visual of the shell of the parent (the GLX visual of
    OpenGL applications is not the default one).
    */
    static XtWidget createPulldownMenu(XtWidget parent, const char* name);

    /**
    Creates a popup menu pane, with the visual of the shell of the owner,
    which Motif does not post by itself on a click of the owner.
    */
    static XtWidget createPopupMenu(XtWidget owner, const char* name);

    /**
    Creates a push button of a menu pane (its `XmNactivateCallback` is
    called when it is chosen).
    */
    static XtWidget createMenuItem(XtWidget menu, const String& text,
                                   XFontSet fontSet);

private:
    XmWidgetSupport();
};

#endif

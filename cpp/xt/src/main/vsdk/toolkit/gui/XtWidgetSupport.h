#ifndef __XT_WIDGET_SUPPORT__
#define __XT_WIDGET_SUPPORT__

#include <string>

#include <X11/Xlib.h>

#include "java/lang/String.h"
#include "vsdk/toolkit/gui/XtWidget.h"

class CommandListener;
class RGBAImageUncompressed;

/**
Services of the Xt Intrinsics shared by the widget sets (Athena, Motif)
that build the GUIs of vitral applications: the visual of shells, the
binding of callbacks to vitral commands, icon pixmaps and the management
of the children of containers. Every resource they allocate for a widget
is freed when the widget is destroyed.

It only uses Xt and Xlib, taking `XtWidget`s, so it can be used from the
sources that also use the vitral `Widget` class.
*/
class XtWidgetSupport {
public:
    /**
    Called by a callback bound with `bindProc`.
    @param widget the widget whose callback list was called
    @param clientData the data given when it was bound
    */
    typedef void (*WidgetProc)(XtWidget widget, void* clientData);

    /**
    Visual, depth and colormap of the shell of a widget: popups must use
    them (the GLX visual of OpenGL applications is not the default one).
    */
    struct ShellVisual {
        Visual* visual;
        int depth;
        Colormap colormap;
    };

    /**
    @return the shell of a widget, or null
    */
    static XtWidget shellOf(XtWidget widget);

    /**
    @return the visual of the shell of the widget (the default visual of the
    screen and a zero depth if the shell does not set it)
    */
    static ShellVisual shellVisualOf(XtWidget widget);

    /**
    Executes a vitral command, with its listener, when a callback list of
    the widget is called (as `SwingEventListener` does for Swing).
    @param callbackName name of the callback list (i.e. "callback" in Athena
    widgets, "activateCallback" in Motif ones)
    @param executor listener executing the command (referenced)
    @param command identifier of the command
    */
    static void bindCommand(XtWidget widget, const char* callbackName,
                            CommandListener* executor,
                            const java::String& command);

    /**
    Calls a procedure when a callback list of the widget is called.
    @param callbackName name of the callback list
    @param proc procedure to call, with the widget and the client data
    */
    static void bindProc(XtWidget widget, const char* callbackName,
                         WidgetProc proc, void* clientData);

    /**
    Creates the pixmap of an icon for a button, blending its transparent
    parts over the background of the button. The pixmap is freed when the
    button is destroyed.
    @return the pixmap, or `None` if the visual is not TrueColor
    */
    static Pixmap createIconPixmap(XtWidget button,
                                   const RGBAImageUncompressed& icon);

    /**
    Destroys every normal child of a container.
    */
    static void removeAll(XtWidget container);

    /**
    @param resource name of a color resource (i.e. "background")
    @param colorName any X color name or "#rrggbb" specification
    */
    static void setColor(XtWidget widget, const char* resource,
                         const char* colorName);

    /**
    @return the prefix followed by a number never returned before (i.e. for
    menus found by their name)
    */
    static std::string uniqueName(const char* prefix);

private:
    XtWidgetSupport();
};

#endif

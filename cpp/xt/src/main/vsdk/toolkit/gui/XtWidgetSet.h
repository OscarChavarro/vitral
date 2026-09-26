#ifndef __XT_WIDGET_SET__
#define __XT_WIDGET_SET__

#include <string>
#include <vector>

#include <X11/Intrinsic.h>

class XtGuiRenderer;
class XtPanelWidgets;

/**
A widget set over the Xt Intrinsics (Athena, Motif), as seen by the Xt
applications of vitral: its panel building blocks, its builder of GUIs
from the vitral GUI definition, and the services of windows whose
behavior differs between widget sets (the drawing canvas and its keyboard
focus, the close requests of the window manager, context menus).

Each widget set module (`vitral_xaw`, `vitral_xm`) implements it, and an
application is built with exactly one of them: the choice is made when
building, never at run time, so Athena and Motif never share a process.
*/
class XtWidgetSet {
public:
    /**
    An item of a context menu: an option, checked or not, or a separator.
    */
    struct MenuChoice {
        std::string label;
        bool checked;
        bool separator;
    };

    /**
    Called with the index of the chosen item of a context menu.
    */
    typedef void (*ChoiceProc)(int index, void* clientData);

    /**
    Called when something happens to a window or a menu.
    */
    typedef void (*NotifyProc)(void* clientData);

    virtual ~XtWidgetSet() {}

    /**
    @return the name of the widget set, for the user (i.e. "Motif")
    */
    virtual const char* getName() const = 0;

    virtual XtPanelWidgets* getPanelWidgets() = 0;

    virtual XtGuiRenderer* getGuiRenderer() = 0;

    /**
    Creates the widget where an application draws (i.e. with OpenGL),
    which receives the keys typed in its shell (see `setKeyboardFocus`).
    */
    virtual Widget createDrawingArea(Widget parent, const char* name,
                                     int x, int y, int width,
                                     int height) = 0;

    /**
    Sends the keys typed in a shell (anywhere over it) to one of its
    widgets, i.e. its drawing area.
    */
    virtual void setKeyboardFocus(Widget shell, Widget target) = 0;

    /**
    Calls a procedure, instead of destroying the shell, when the window
    manager asks to close its window (`WM_DELETE_WINDOW`).
    */
    virtual void setWindowCloseHandler(Widget shell, NotifyProc close,
                                       void* clientData) = 0;

    /**
    Creates a context menu, to show with `popupChoiceMenu`. It is destroyed
    with its owner, or with `XtDestroyWidget`.
    @param owner widget the menu belongs to
    @param items items of the menu, checked ones marked
    @param fontSet font set of the texts
    @param choose called with the index of the chosen item
    */
    virtual Widget createChoiceMenu(Widget owner,
                                    const std::vector<MenuChoice>& items,
                                    XFontSet fontSet, ChoiceProc choose,
                                    void* clientData) = 0;

    /**
    Shows a context menu at a position of the screen. While it is shown it
    takes the pointer: a click outside only closes it.
    @param poppedDown called when the menu is closed, after choosing an
    item or not
    */
    virtual void popupChoiceMenu(Widget menu, int rootX, int rootY,
                                 NotifyProc poppedDown,
                                 void* clientData) = 0;
};

#endif

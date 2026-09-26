#ifndef __XAW_GUI_RENDERER__
#define __XAW_GUI_RENDERER__

#include <X11/Xlib.h>

#include "vsdk/toolkit/gui/XtWidget.h"

class CommandListener;
class WidgetButtonGroup;
class WidgetMenu;

/**
Builds Xaw widgets from the vitral GUI definition (the `Widget` context
imported from the I18N files), as `SwingGuiRenderer` builds the Swing ones:
menubars, popup menus and groups of command buttons, whose commands are
executed by a `CommandListener` (the `ActionListener` of Swing).

Xaw has no layout managers for these: the widgets are placed at explicit
positions inside plain Composites. The resources of the widgets built here
(command bindings, icon pixmaps) are freed when they are destroyed.

C++ port note: the vitral `Widget` class and the Xt `Widget` type collide,
so the builders take the elements of the context (`WidgetMenu`,
`WidgetButtonGroup`) instead of the context and a name, and Xt widgets are
`XtWidget`s (the same type).
*/
class XawGuiRenderer {
public:
    /**
    Builds a menubar: a row of menu buttons, one per menu of the given one,
    each with its popup menu (see `buildPopupMenu`).
    @param parent container of the menubar
    @param menubar the menubar of the GUI context, or null (a menubar with
    an error message and an "Exit" item is built, as in Swing)
    @param executor executes the commands of the items (referenced)
    @param fontSet font set of the texts
    @param x position of the menubar in its parent
    @param y position of the menubar in its parent
    @param width width of the menubar
    @param height height of the menubar
    @param buttonWidth width of each menu button
    @return the Composite of the menubar
    */
    static XtWidget buildMenubar(XtWidget parent, WidgetMenu* menubar,
                                 CommandListener* executor, XFontSet fontSet,
                                 int x, int y, int width, int height,
                                 int buttonWidth);

    /**
    Builds the popup menu (a SimpleMenu shell) of a menu of the GUI: its
    items execute their commands, its separators are lines and its submenus
    cascade to the right of their entries. It uses the visual of the shell
    of the owner, as the GLX one of OpenGL applications.
    @param owner widget owning the popup (i.e. its menu button)
    @param menu menu of the GUI
    @param executor executes the commands of the items (referenced)
    @param fontSet font set of the texts
    @return the popup shell (its name is the one to give as `XtNmenuName`)
    */
    static XtWidget buildPopupMenu(XtWidget owner, WidgetMenu* menu,
                                   CommandListener* executor,
                                   XFontSet fontSet);

    /**
    Builds a group of command buttons: vertical groups of text buttons, and
    horizontal groups of icon buttons when the group shows icons and the
    commands have them (blended over the background of the button).
    @param parent container of the group
    @param group button group of the GUI, or null (a label reporting it
    is built, as in Swing)
    @param executor executes the commands of the buttons (referenced)
    @param fontSet font set of the texts
    @param x position of the group in its parent
    @param y position of the group in its parent
    @param width width of the group (its height follows its buttons)
    @return the Composite of the group
    */
    static XtWidget buildButtonGroup(XtWidget parent, WidgetButtonGroup* group,
                                     CommandListener* executor,
                                     XFontSet fontSet, int x, int y,
                                     int width);

private:
    XawGuiRenderer();
};

#endif

#ifndef __XT_BUTTONS_PANEL__
#define __XT_BUTTONS_PANEL__

#include <string>
#include <vector>

#include <X11/Intrinsic.h>

class XtCommandExecutor;

/**
A group of command buttons of the GUI definition, as `AwtButtonsPanel`
builds it for Swing (with `SwingGuiRenderer.buildButtonGroup`): vertical
groups of text buttons for the tabs of the side panel, and horizontal ones
of icon buttons for the global bar. Each button executes its command with
an `XtCommandExecutor`.

The panel is a plain Composite whose buttons are placed at explicit
positions. It must be deleted after its widget is destroyed (it owns the
pixmaps of the icons).
*/
class XtButtonsPanel {
public:
    /**
    @param parent container of the panel
    @param guiDefinition JSON GUI definition (the I18N file)
    @param group name of the button group (i.e. `CREATION`)
    @param executor executes the commands of the buttons (referenced)
    @param fontSet font set of the texts
    @param x position of the panel in its parent
    @param y position of the panel in its parent
    @param width width of the panel
    */
    XtButtonsPanel(Widget parent, const std::string& guiDefinition,
                   const std::string& group, XtCommandExecutor* executor,
                   XFontSet fontSet, int x, int y, int width);
    ~XtButtonsPanel();

    Widget getWidget() const { return container; }

    /**
    @return the height the panel takes
    */
    int getHeight() const { return height; }

private:
    struct Binding {
        XtButtonsPanel* panel;
        std::string command;
    };

    Widget container;
    Display* display;
    XtCommandExecutor* executor;
    int height;
    std::vector<Binding*> bindings;
    std::vector<Pixmap> pixmaps;

    Pixmap loadIcon(Widget button, const std::string& path,
                    const std::string& maskPath);

    static void activate(Widget button, XtPointer clientData, XtPointer);

    XtButtonsPanel(const XtButtonsPanel& other);
    XtButtonsPanel& operator=(const XtButtonsPanel& other);
};

#endif

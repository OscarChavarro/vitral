#ifndef __XT_PANEL_WIDGETS__
#define __XT_PANEL_WIDGETS__

#include <string>

#include <X11/Intrinsic.h>

/**
Small Xaw building blocks for the editors of the side panel, which is a
plain Composite whose children are placed at explicit positions: labels,
one line text fields (confirmed with Return, as Swing `JTextField` action
events) and the removal of every child of a container.

Text fields take the keyboard focus of their application shell when
clicked (the shell otherwise redirects the keys to the drawing canvas).
*/
class XtPanelWidgets {
public:
    /**
    Called when the user presses Return in a text field.
    @param field the text field
    @param clientData the data given when the field was created
    */
    typedef void (*ActivateProc)(Widget field, void* clientData);

    enum Justify { LEFT, CENTER, RIGHT };

    /**
    Destroys every normal child of a container.
    */
    static void removeAll(Widget container);

    static Widget createLabel(Widget parent, const std::string& text,
                              XFontSet fontSet, Justify justify,
                              int x, int y, int width, int height);

    static void setLabel(Widget label, const std::string& text);

    static Widget createTextField(Widget parent, const std::string& text,
                                  XFontSet fontSet,
                                  int x, int y, int width, int height,
                                  ActivateProc activate, void* clientData);

    static std::string getText(Widget field);

    /**
    Replaces the text of a field, also restoring its normal background.
    */
    static void setText(Widget field, const std::string& text);

    /**
    @param field text field to mark
    @param invalid true to show the field with the background used for
    invalid values, false for the normal one
    */
    static void setInvalid(Widget field, bool invalid);

    /**
    @param widget widget whose foreground changes
    @param colorName any X color name or "#rrggbb" specification
    */
    static void setForeground(Widget widget, const char* colorName);
};

#endif

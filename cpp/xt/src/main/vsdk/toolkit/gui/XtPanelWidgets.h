#ifndef __XT_PANEL_WIDGETS__
#define __XT_PANEL_WIDGETS__

#include <string>
#include <vector>

#include <X11/Intrinsic.h>

/**
Small building blocks for the panels of Xt applications (the side panel and
its editors, dialogs), whose children are placed at explicit positions:
containers, labels, one line text fields (confirmed with Return, as Swing
`JTextField` action events), push buttons and option buttons.

It is an interface over the Xt Intrinsics only: each widget set provides
its implementation (`XawPanelWidgets` with Athena widgets, `XmPanelWidgets`
with Motif ones), chosen when the application is built. Text fields take
the keyboard focus of their shell when clicked (the shell otherwise
redirects the keys to the drawing canvas of the application).
*/
class XtPanelWidgets {
public:
    /**
    Called when the user activates a widget: presses Return in a text field
    or clicks a push button.
    @param widget the text field or button
    @param clientData the data given when the widget was created
    */
    typedef void (*ActivateProc)(Widget widget, void* clientData);

    /**
    Called when the user chooses an option of an option button.
    @param button the option button
    @param index index of the chosen option
    @param clientData the data given when the button was created
    */
    typedef void (*OptionProc)(Widget button, int index, void* clientData);

    enum Justify { LEFT, CENTER, RIGHT };

    virtual ~XtPanelWidgets() {}

    /**
    Creates a container whose children are placed at explicit positions.
    @param managed false to create it hidden (i.e. the pages of a tabbed
    panel)
    */
    virtual Widget createPanel(Widget parent, const char* name, int x, int y,
                               int width, int height, bool managed) = 0;

    virtual Widget createLabel(Widget parent, const std::string& text,
                               XFontSet fontSet, Justify justify,
                               int x, int y, int width, int height) = 0;

    /**
    Replaces the text of a label, keeping its geometry.
    */
    virtual void setLabel(Widget label, const std::string& text) = 0;

    virtual Widget createTextField(Widget parent, const std::string& text,
                                   XFontSet fontSet,
                                   int x, int y, int width, int height,
                                   ActivateProc activate,
                                   void* clientData) = 0;

    virtual std::string getText(Widget field) = 0;

    /**
    Replaces the text of a field, also restoring its normal background.
    */
    virtual void setText(Widget field, const std::string& text) = 0;

    /**
    @param field text field to mark
    @param invalid true to show the field with the background used for
    invalid values, false for the normal one
    */
    virtual void setInvalid(Widget field, bool invalid) = 0;

    /**
    @param widget widget whose foreground changes
    @param colorName any X color name or "#rrggbb" specification
    */
    virtual void setForeground(Widget widget, const char* colorName) = 0;

    /**
    Creates a push button. Clicking it does not take the keyboard focus
    from the drawing canvas.
    */
    virtual Widget createPushButton(Widget parent, const std::string& label,
                                    XFontSet fontSet,
                                    int x, int y, int width, int height,
                                    ActivateProc activate,
                                    void* clientData) = 0;

    /**
    Creates a button that shows a menu of options (the combo box of Swing),
    showing the last chosen one.
    @param placeholder text shown while no option was chosen
    @param options texts of the options
    @param choose called when an option is chosen
    */
    virtual Widget createOptionButton(Widget parent,
                                      const std::string& placeholder,
                                      const std::vector<std::string>& options,
                                      XFontSet fontSet,
                                      int x, int y, int width, int height,
                                      OptionProc choose,
                                      void* clientData) = 0;
};

#endif

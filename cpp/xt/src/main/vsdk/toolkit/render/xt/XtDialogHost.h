#ifndef __XT_DIALOG_HOST__
#define __XT_DIALOG_HOST__

#include <functional>
#include <map>
#include <memory>
#include <string>
#include <vector>

#include <X11/Xlib.h>

#include "vsdk/toolkit/gui/XtWidget.h"
#include "vsdk/toolkit/gui/dialog/runtime/DialogSession.h"

class WidgetDialog;
class XtTreeView;
class XtWidgetSet;

/**
Xt presentation of the reusable dialog elements of a `WidgetDialog` (as
read from `etc/gui/dialogs.json` by `DialogPersistence`), as
`SwingDialogHost` is for Swing: forms whose text inputs (with the message
of their label above them) and trees (`XtTreeView`) fill the dialog, and
whose buttons are a centered row at its bottom. Each button reports its
action to the application, which owns the behavior of the dialog. The
`action` elements have no widget: they are the actions the application
offers by other means (keys, context menus), as the scene selector of the
Java application does.

It only uses the `XtPanelWidgets` of the widget set, so it serves both
Athena and Motif applications.

C++ port note: the vitral `Widget` class and the Xt `Widget` type collide,
so the messages of the GUI context are given as a function, and Xt widgets
are `XtWidget`s (the same type).
*/
class XtDialogHost {
public:
    /**
    @return the text of a message of the GUI context, from its key
    */
    typedef std::function<std::string(const std::string& key)> MessageSource;

    /**
    Called with the action of the button clicked.
    */
    typedef std::function<void(const std::string& action)> ActionListener;

    /**
    A dialog built from its definition: its shell, its widgets (fields by
    binding, buttons by action) and its session.
    */
    class Form {
    public:
        ~Form();

        XtWidget dialog() const;

        /**
        @return the text field of a binding, or null
        */
        XtWidget field(const std::string& binding) const;

        /**
        @return the button of an action, or null
        */
        XtWidget action(const std::string& action) const;

        /**
        @return the tree of a binding, or null
        */
        XtTreeView* tree(const std::string& binding) const;

        /**
        @return true if the definition has an element (a button or an
        `action` element) with the action
        */
        bool hasAction(const std::string& action) const;
        DialogSession* session();

        std::string getText(const std::string& binding) const;
        void setText(const std::string& binding, const std::string& text);

        /**
        @return the text shown by the button of an action
        */
        std::string getActionLabel(const std::string& action) const;

        /**
        Replaces the text of the button of an action, resizing the buttons
        row to fit it (as a Swing `FlowLayout` does).
        */
        void setActionLabel(const std::string& action,
                            const std::string& label);

        /**
        Shows the dialog over the application: modal dialogs take its input
        (`XtGrabExclusive`). The keys typed go to its first text field.
        */
        void show();

        /**
        Hides the dialog and closes its session, as the window manager
        close requests do (the `dispose` of Swing).
        */
        void close();
        bool isVisible() const;

    private:
        friend class XtDialogHost;
        struct Button;

        XtWidgetSet* widgetSet;
        const WidgetDialog* definition;
        XFontSet fontSet;
        XtWidget shell;
        XtWidget firstField;
        bool modal;
        bool visible;
        int width;
        int buttonsY;
        DialogSession dialogSession;
        ActionListener onAction;
        std::map<std::string, XtWidget> fields;
        std::map<std::string, XtTreeView*> trees;
        std::vector<std::unique_ptr<Button> > buttons;

        Form(XtWidgetSet* widgetSet, XFontSet fontSet, XtWidget shell,
             const WidgetDialog* definition, const ActionListener& onAction);
        void layoutButtons();
        Button* findButton(const std::string& action) const;

        static void buttonPressed(XtWidget, void* clientData);
        static void closeRequested(void* clientData);

        Form(const Form& other);
        Form& operator=(const Form& other);
    };

    /**
    Builds the form of a dialog definition, hidden.
    @param shell popup shell of the dialog (i.e. a `transientShell` with the
    visual of the application), owned by the form; its title is set from
    the `titleKey` of the definition
    @param widgetSet widget set of the application
    @param fontSet font set of the texts
    @param messages texts of the message keys of the definition
    @param definition dialog definition (referenced: it must outlive the
    form)
    @param onAction called with the action of each button clicked
    @return the form, owned by the caller
    @throws std::invalid_argument for elements that are not text inputs,
    trees, buttons, actions, rows or columns
    */
    static Form* form(XtWidget shell, XtWidgetSet* widgetSet,
                      XFontSet fontSet, const MessageSource& messages,
                      const WidgetDialog* definition,
                      const ActionListener& onAction);

private:
    XtDialogHost();
};

#endif

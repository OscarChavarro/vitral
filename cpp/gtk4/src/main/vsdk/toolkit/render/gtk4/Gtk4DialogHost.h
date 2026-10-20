#ifndef __GTK4_DIALOG_HOST__
#define __GTK4_DIALOG_HOST__

#include <functional>
#include <map>
#include <string>

#include <gtk/gtk.h>

#include "vsdk/toolkit/gui/dialog/runtime/DialogSession.h"

class Gtk4TreeView;
class Widget;
class WidgetDialog;

/**
GTK 4 presentation of the reusable dialog elements of a `WidgetDialog` (as
read from `etc/gui/dialogs.json` by `DialogPersistence`), as
`SwingDialogHost` is for Swing: forms whose text inputs (with the message
of their label above them) and trees (`Gtk4TreeView`) fill the window, and
whose buttons are a centered row at its bottom. Each button reports its
action to the application, which owns the behavior of the dialog. The
`action` elements have no widget: they are the actions the application
offers by other means (keys, context menus), as the scene selector of the
Java application does.
*/
class Gtk4DialogHost {
public:
    /**
    Called with the action of the button clicked.
    */
    typedef std::function<void(const std::string& action)> ActionListener;

    /**
    A dialog built from its definition: its window, its widgets (entries
    by binding, buttons by action) and its session. It belongs to its
    window: it is deleted when the window is finalized, and its session is
    closed when the window is destroyed (i.e. by `close`).
    */
    class Form {
    public:
        GtkWidget* dialog() const;

        /**
        @return the entry of a binding, or null
        */
        GtkWidget* field(const std::string& binding) const;

        /**
        @return the button of an action, or null
        */
        GtkWidget* action(const std::string& action) const;

        /**
        @return the tree of a binding, or null
        */
        Gtk4TreeView* tree(const std::string& binding) const;

        /**
        @return true if the definition has an element (a button or an
        `action` element) with the action
        */
        bool hasAction(const std::string& action) const;
        DialogSession* session();

        /**
        Shows the dialog over its owner, with the keyboard focus in its
        first text field (trees are not focused: a focused `GtkListBox`
        selects its first row).
        */
        void present();

        /**
        Destroys the window of the dialog (the `dispose` of Swing).
        */
        void close();

    private:
        friend class Gtk4DialogHost;

        GtkWidget* window;
        GtkWidget* firstField;
        const WidgetDialog* definition;
        DialogSession dialogSession;
        ActionListener onAction;
        std::map<std::string, GtkWidget*> fields;
        std::map<std::string, GtkWidget*> actions;
        std::map<std::string, Gtk4TreeView*> trees;

        Form(const WidgetDialog* definition, const ActionListener& onAction);
        ~Form();

        static void buttonClicked(GtkButton* button, gpointer data);
        static void destroyed(GtkWidget* window, gpointer data);
        static void deleteForm(gpointer data);

        Form(const Form& other);
        Form& operator=(const Form& other);
    };

    /**
    Builds the form of a dialog definition, hidden.
    @param owner window the dialog is transient for
    @param messages GUI context with the messages of the definition keys
    @param definition dialog definition (referenced: it must outlive the
    form)
    @param onAction called with the action of each button clicked
    @return the form, owned by its window
    @throws std::invalid_argument for elements that are not text inputs,
    trees, buttons, actions, rows or columns
    */
    static Form* form(GtkWindow* owner, Widget* messages,
                      const WidgetDialog* definition,
                      const ActionListener& onAction);

private:
    Gtk4DialogHost();
};

#endif

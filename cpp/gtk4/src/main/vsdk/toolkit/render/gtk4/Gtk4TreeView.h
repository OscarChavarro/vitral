#ifndef __GTK4_TREE_VIEW__
#define __GTK4_TREE_VIEW__

#include "java/lang/Runnable.h"
#include "java/util/function/_LambdaHolder.h"
#include "java/util/ArrayList.h"
#include "java/util/TreeSet.h"

#include <gtk/gtk.h>

class DialogTreeNode;

/**
Tree of `DialogTreeNode` rows for GTK 4 applications (the `JTree` of
Swing), for the `tree` elements of the dialog definitions: the root is not
shown and its children start expanded; rows with children can be collapsed
and expanded with their triangle.

Several rows can be selected, as a discontiguous `JTree` selection: a click
selects only its row, Control (or Command) + click toggles a row and
Shift+click selects the rows between the last clicked one and the clicked
one; Up and Down move the selection. The rows are a `GtkListBox`, whose own
click selection is not used (it differs between GTK versions). The Delete
(and BackSpace, the delete key of Mac keyboards) key and the right button
(after selecting the row under it if it was not selected) are reported to
the application.
*/
class Gtk4TreeView {
public:
    typedef java::Runnable Listener;

    /**
    Called for the right button, with the list widget and the position of
    the pointer in it.
    */
    class ContextMenuListener :
        public java::_Lambda<void, GtkWidget*, double, double> {
      public:
        ContextMenuListener() {}
        ContextMenuListener(decltype(nullptr)) {}
        ContextMenuListener(const ContextMenuListener& other) :
            java::_Lambda<void, GtkWidget*, double, double>(
                static_cast<const java::_Lambda<void, GtkWidget*, double,
                    double>&>(other)) {}

        template <class F>
        ContextMenuListener(const F& function) :
            java::_Lambda<void, GtkWidget*, double, double>(function) {}

        ContextMenuListener& operator=(const ContextMenuListener& other)
        {
            java::_Lambda<void, GtkWidget*, double, double>::operator=(other);
            return *this;
        }

        void accept(GtkWidget* list, double x, double y) const
        {
            this->invoke(list, x, y);
        }
    };

    Gtk4TreeView();

    /**
    @return the scrolled window of the tree, to add to a container
    */
    GtkWidget* getWidget() const;

    /**
    @return the list of rows (i.e. to anchor popovers)
    */
    GtkWidget* getList() const;

    /**
    Shows a tree, without selected rows.
    @param root root of the tree, referenced: it must live while it is shown
    */
    void setRoot(const DialogTreeNode* root);

    /**
    Selects the rows, without notifying the selection listener.
    */
    void setSelection(const java::ArrayList<const DialogTreeNode*>& nodes);

    /**
    @return the selected rows, in display order
    */
    java::ArrayList<const DialogTreeNode*> getSelection() const;

    /**
    Selects only a row (or none, for null), as a click does: the selection
    listener is notified.
    */
    void selectOnly(const DialogTreeNode* node);

    void setSelectionListener(const Listener& listener);
    void setDeleteListener(const Listener& listener);
    void setContextMenuListener(const ContextMenuListener& listener);

private:
    GtkWidget* widget;
    GtkWidget* list;
    const DialogTreeNode* root;
    java::TreeSet<const DialogTreeNode*> expanded;
    java::ArrayList<GtkWidget*> rows;
    int anchor;
    bool updating;
    Listener selectionListener;
    Listener deleteListener;
    ContextMenuListener contextMenuListener;

    void buildRows();
    void addRows(const DialogTreeNode* node, int depth);
    static const DialogTreeNode* nodeOf(GtkListBoxRow* row);
    void notifySelection();

    static void selectionChanged(GtkListBox*, gpointer data);
    void selectRows(int from, int to, bool keep);
    void toggleExpansion(const DialogTreeNode* node);
    static void primaryPressed(GtkGestureClick* gesture, gint, gdouble x,
                               gdouble y, gpointer data);
    static gboolean keyPressed(GtkEventControllerKey*, guint keyval, guint,
                               GdkModifierType, gpointer data);
    static void secondaryPressed(GtkGestureClick* gesture, gint, gdouble x,
                                 gdouble y, gpointer data);

    Gtk4TreeView(const Gtk4TreeView& other);
    Gtk4TreeView& operator=(const Gtk4TreeView& other);
};

#endif

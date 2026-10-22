#ifndef __XT_TREE_VIEW__
#define __XT_TREE_VIEW__

#include "java/util/TreeSet.h"
#include "java/util/ArrayList.h"

#include <X11/Xlib.h>

#include "java/lang/Runnable.h"
#include "vsdk/toolkit/gui/XtWidget.h"

class DialogTreeNode;

/**
Tree of `DialogTreeNode` rows for Xt applications (the `JTree` of Swing),
for the `tree` elements of the dialog definitions: the root is not shown
and its children start expanded; rows with children can be collapsed and
expanded by clicking their triangle.

Several rows can be selected, as a discontiguous `JTree` selection: a click
selects only its row, Control+click toggles a row and Shift+click selects
the rows between the last clicked one and the clicked one. Up and Down move
the selection, the wheel scrolls. The Delete (and BackSpace, the delete key
of Mac keyboards) key and the right button (released, after selecting on
press the row under it if it was not selected) are reported to the
application.

It is drawn with Xlib over a Core widget, so it works with every widget
set.
*/
class XtTreeView {
public:
    /// Called on selection changes and on delete requests.
    typedef Runnable Listener;

    /**
    Called for the right button, with the position of the pointer in the
    screen.
    */
    class ContextMenuListener {
    public:
        virtual ~ContextMenuListener() {}
        virtual void contextMenuRequested(int rootX, int rootY) = 0;
    };

    /**
    Creates the tree inside a container whose children are placed at
    explicit positions.
    */
    XtTreeView(XtWidget parent, XFontSet fontSet, int x, int y, int width,
               int height);
    ~XtTreeView();

    XtWidget getWidget() const;

    /**
    Shows a tree, without selected rows.
    @param root root of the tree, referenced: it must live while it is
    shown
    */
    void setRoot(const DialogTreeNode* root);

    /**
    Selects the rows, without notifying the selection listener (as the
    model updates of a `JTree`).
    */
    void setSelection(const ArrayList<const DialogTreeNode*>& nodes);

    /**
    @return the selected rows, in display order
    */
    ArrayList<const DialogTreeNode*> getSelection() const;

    /**
    Selects only a row (or none, for null), as a click does: the selection
    listener is notified.
    */
    void selectOnly(const DialogTreeNode* node);

    /// The listeners are not owned (NULL for none)
    void setSelectionListener(Listener* listener);
    void setDeleteListener(Listener* listener);
    void setContextMenuListener(ContextMenuListener* listener);

private:
    struct Row {
        const DialogTreeNode* node;
        int depth;
    };

    XtWidget widget;
    XFontSet fontSet;
    GC gc;
    unsigned long colors[5];
    bool colorsAllocated;
    int rowHeight;
    int ascent;
    int scrollTop;
    int anchor;
    const DialogTreeNode* root;
    TreeSet<const DialogTreeNode*> expanded;
    TreeSet<const DialogTreeNode*> selected;
    ArrayList<Row> rows;
    Listener* selectionListener;
    Listener* deleteListener;
    ContextMenuListener* contextMenuListener;

    void buildRows();
    void addRows(const DialogTreeNode* node, int depth);
    int visibleRows() const;
    void clampScroll();
    void ensureVisible(int row);
    void redraw();
    void allocateColors();
    void notifySelection();
    void click(int x, int y, unsigned int button, unsigned int state);
    void key(XKeyEvent* event);

    /// The Xt callbacks, which use the Xt types
    friend struct XtTreeViewCallbacks;

    XtTreeView(const XtTreeView& other);
    XtTreeView& operator=(const XtTreeView& other);
};

#endif

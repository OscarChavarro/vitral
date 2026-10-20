#ifndef __XT_TREE_VIEW__
#define __XT_TREE_VIEW__

#include <functional>
#include <set>
#include <vector>

#include <X11/Xlib.h>

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
    typedef std::function<void()> Listener;

    /**
    Called for the right button, with the position of the pointer in the
    screen.
    */
    typedef std::function<void(int rootX, int rootY)> ContextMenuListener;

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
    void setSelection(const std::vector<const DialogTreeNode*>& nodes);

    /**
    @return the selected rows, in display order
    */
    std::vector<const DialogTreeNode*> getSelection() const;

    /**
    Selects only a row (or none, for null), as a click does: the selection
    listener is notified.
    */
    void selectOnly(const DialogTreeNode* node);

    void setSelectionListener(const Listener& listener);
    void setDeleteListener(const Listener& listener);
    void setContextMenuListener(const ContextMenuListener& listener);

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
    std::set<const DialogTreeNode*> expanded;
    std::set<const DialogTreeNode*> selected;
    std::vector<Row> rows;
    Listener selectionListener;
    Listener deleteListener;
    ContextMenuListener contextMenuListener;

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

// Before any other header that could include the Xt ones
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/XtIntrinsics.h"
#include "vsdk/toolkit/gui/XtTextEncoding.h"

#include "java/lang/String.h"

#include <X11/keysym.h>
#include <X11/Xutil.h>

#include "vsdk/toolkit/gui/dialog/components/DialogTreeNode.h"
#include "vsdk/toolkit/render/xt/XtTreeView.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/TreeSet.h"

enum XtTreeViewColor { BACKGROUND, TEXT, SELECTION, SELECTED_TEXT, LINE };
static const char* COLOR_NAMES[] = { "white", "black", "#3875d7", "white", "gray55" };
static const int INDENT = 18;
static const int TOGGLE_SIZE = 9;
static const int MARGIN = 4;
static const int SCROLLBAR_WIDTH = 8;

struct XtTreeViewCallbacks {
    static void exposed(XtWidget, XtPointer clientData, XEvent* event, Boolean*)
    {
        if ( event->type == Expose && event->xexpose.count == 0 ) {
            ((XtTreeView*)(clientData))->redraw();
        }
    }

    static void pressed(XtWidget, XtPointer clientData, XEvent* event, Boolean*)
    {
        XtTreeView* self = ((XtTreeView*)(clientData));
        XButtonEvent* button = &event->xbutton;
        if ( event->type == ButtonPress ) {
            self->click(button->x, button->y, button->button, button->state);
        }
        // The menu is shown on release: shown on press, the release of the
        // same click would choose or close it
        else if ( event->type == ButtonRelease && button->button == Button3 &&
                  self->contextMenuListener ) {
            self->contextMenuListener->contextMenuRequested(
                button->x_root, button->y_root);
        }
    }

    static void keyPressed(XtWidget, XtPointer clientData, XEvent* event,
                           Boolean*)
    {
        if ( event->type == KeyPress ) {
            ((XtTreeView*)(clientData))->key(&event->xkey);
        }
    }

    static void destroyed(XtWidget, XtPointer clientData, XtPointer)
    {
        XtTreeView* self = ((XtTreeView*)(clientData));
        if ( self->gc != NULL ) {
            XFreeGC(XtDisplay(self->widget), self->gc);
            self->gc = NULL;
        }
        self->widget = NULL;
    }
};

XtTreeView::XtTreeView(XtWidget parent, XFontSet fontSet, int x, int y,
                       int width, int height)
    : widget(NULL), fontSet(fontSet), gc(NULL), colorsAllocated(FALSE),
      rowHeight(20), ascent(14), scrollTop(0), anchor(-1), root(NULL),
      selectionListener(NULL), deleteListener(NULL), contextMenuListener(NULL)
{
    if ( fontSet != NULL ) {
        XFontSetExtents* extents = XExtentsOfFontSet(fontSet);
        rowHeight = extents->max_logical_extent.height + 4;
        ascent = -extents->max_logical_extent.y + 2;
    }
    widget = XtVaCreateManagedWidget("treeView", widgetClass, parent,
        XtNx, x, XtNy, y, XtNwidth, width, XtNheight, height,
        XtNborderWidth, 1, NULL);
    XtAddEventHandler(widget, ExposureMask, False,
                      &XtTreeViewCallbacks::exposed, this);
    XtAddEventHandler(widget, ButtonPressMask | ButtonReleaseMask, False,
                      &XtTreeViewCallbacks::pressed, this);
    XtAddEventHandler(widget, KeyPressMask, False,
                      &XtTreeViewCallbacks::keyPressed, this);
    XtAddCallback(widget, XtNdestroyCallback, &XtTreeViewCallbacks::destroyed,
                  this);
}

XtTreeView::~XtTreeView()
{
    if ( widget == NULL ) {
        return;
    }
    XtRemoveEventHandler(widget, ExposureMask, False,
                         &XtTreeViewCallbacks::exposed, this);
    XtRemoveEventHandler(widget, ButtonPressMask | ButtonReleaseMask, False,
                         &XtTreeViewCallbacks::pressed, this);
    XtRemoveEventHandler(widget, KeyPressMask, False,
                         &XtTreeViewCallbacks::keyPressed, this);
    XtRemoveCallback(widget, XtNdestroyCallback,
                     &XtTreeViewCallbacks::destroyed, this);
    if ( gc != NULL ) {
        XFreeGC(XtDisplay(widget), gc);
    }
}

XtWidget XtTreeView::getWidget() const
{
    return widget;
}

void XtTreeView::setRoot(const DialogTreeNode* root)
{
    this->root = root;
    expanded.clear();
    selected.clear();
    anchor = -1;
    if ( root != NULL ) {
        // As the Java selector, the children of the root start expanded
        expanded.add(root);
        for ( long i = 0; i < root->getChildren().size(); i++ ) {
            expanded.add(&root->getChildren()[i]);
        }
    }
    buildRows();
    clampScroll();
    redraw();
}

void XtTreeView::addRows(const DialogTreeNode* node, int depth)
{
    const ArrayList<DialogTreeNode>& children = node->getChildren();
    for ( long i = 0; i < children.size(); i++ ) {
        Row row;
        row.node = &children[i];
        row.depth = depth;
        rows.add(row);
        if ( expanded.contains(&children[i]) ) {
            addRows(&children[i], depth + 1);
        }
    }
}

void XtTreeView::buildRows()
{
    rows.clear();
    if ( root != NULL ) {
        addRows(root, 0);
    }
    // Rows that are not shown are not selected (as collapsing in a JTree)
    TreeSet<const DialogTreeNode*> shown;
    long i;
    for ( i = 0; i < rows.size(); i++ ) shown.add(rows[i].node);
    for ( i = 0; i < selected.size(); ) {
        if ( !shown.contains(selected.get(i)) ) selected.remove(selected.get(i));
        else ++i;
    }
    if ( anchor >= ((int)(rows.size())) ) anchor = -1;
}

void XtTreeView::setSelection(const ArrayList<const DialogTreeNode*>& nodes)
{
    selected.clear();
    for ( long i = 0; i < nodes.size(); i++ ) selected.add(nodes[i]);
    redraw();
}

ArrayList<const DialogTreeNode*> XtTreeView::getSelection() const
{
    ArrayList<const DialogTreeNode*> result;
    for ( long i = 0; i < rows.size(); i++ ) {
        if ( selected.contains(rows[i].node) ) result.add(rows[i].node);
    }
    return result;
}

void XtTreeView::selectOnly(const DialogTreeNode* node)
{
    selected.clear();
    anchor = -1;
    for ( long i = 0; node != NULL && i < rows.size(); i++ ) {
        if ( rows[i].node == node ) {
            selected.add(node);
            anchor = (int)i;
            ensureVisible(anchor);
        }
    }
    redraw();
    notifySelection();
}

void XtTreeView::setSelectionListener(Listener* listener)
{
    selectionListener = listener;
}

void XtTreeView::setDeleteListener(Listener* listener)
{
    deleteListener = listener;
}

void XtTreeView::setContextMenuListener(ContextMenuListener* listener)
{
    contextMenuListener = listener;
}

void XtTreeView::notifySelection()
{
    if ( selectionListener != NULL ) {
        selectionListener->run();
    }
}

int XtTreeView::visibleRows() const
{
    Dimension height = 0;
    if ( widget != NULL ) XtVaGetValues(widget, XtNheight, &height, NULL);
    return Math::max(1, (((int)(height)) - MARGIN) / rowHeight);
}

void XtTreeView::clampScroll()
{
    int maximum = Math::max(0, ((int)(rows.size())) - visibleRows());
    scrollTop = Math::max(0, Math::min(scrollTop, maximum));
}

void XtTreeView::ensureVisible(int row)
{
    if ( row < scrollTop ) scrollTop = row;
    else if ( row >= scrollTop + visibleRows() ) scrollTop = row - visibleRows() + 1;
    clampScroll();
}

void XtTreeView::allocateColors()
{
    Display* display = XtDisplay(widget);
    Colormap colormap = 0;
    XtVaGetValues(widget, XtNcolormap, &colormap, NULL);
    for ( int i = 0; i < 5; i++ ) {
        XColor color, exact;
        colors[i] = i == BACKGROUND || i == SELECTED_TEXT ?
            WhitePixelOfScreen(XtScreen(widget)) :
            BlackPixelOfScreen(XtScreen(widget));
        if ( XAllocNamedColor(display, colormap, COLOR_NAMES[i], &color, &exact) ) {
            colors[i] = color.pixel;
        }
    }
    colorsAllocated = TRUE;
}

void XtTreeView::redraw()
{
    if ( widget == NULL || !XtIsRealized(widget) ) return;
    Display* display = XtDisplay(widget);
    Window window = XtWindow(widget);
    if ( gc == NULL ) gc = XCreateGC(display, window, 0, NULL);
    if ( !colorsAllocated ) allocateColors();

    Dimension width = 0, height = 0;
    XtVaGetValues(widget, XtNwidth, &width, XtNheight, &height, NULL);
    XSetForeground(display, gc, colors[BACKGROUND]);
    XFillRectangle(display, window, gc, 0, 0, width, height);

    int count = visibleRows();
    for ( int i = 0; i < count && scrollTop + i < ((int)(rows.size())); i++ ) {
        const Row& row = rows[scrollTop + i];
        int y = MARGIN / 2 + i * rowHeight;
        int x = MARGIN + row.depth * INDENT;
        BOOLEAN isSelected = selected.contains(row.node);
        String label = XtTextEncoding::toLocale(row.node->getLabel());
        int labelX = x + INDENT;
        if ( isSelected ) {
            int labelWidth = fontSet != NULL ?
                XmbTextEscapement(fontSet, label.c_str(),
                                  ((int)(label.size()))) :
                ((int)(label.size())) * 8;
            XSetForeground(display, gc, colors[SELECTION]);
            XFillRectangle(display, window, gc, labelX - 2, y,
                           labelWidth + 4, rowHeight);
        }
        if ( !row.node->getChildren().isEmpty() ) {
            // Expansion triangle: down when expanded, right when collapsed
            XPoint points[3];
            int cx = x + 4, cy = y + rowHeight / 2;
            if ( expanded.contains(row.node) ) {
                points[0].x = cx; points[0].y = cy - 3;
                points[1].x = cx + TOGGLE_SIZE; points[1].y = cy - 3;
                points[2].x = cx + TOGGLE_SIZE / 2; points[2].y = cy + 4;
            }
            else {
                points[0].x = cx + 2; points[0].y = cy - TOGGLE_SIZE / 2;
                points[1].x = cx + 2; points[1].y = cy + TOGGLE_SIZE / 2;
                points[2].x = cx + 7; points[2].y = cy;
            }
            XSetForeground(display, gc, colors[LINE]);
            XFillPolygon(display, window, gc, points, 3, Convex, CoordModeOrigin);
        }
        XSetForeground(display, gc, colors[isSelected ? SELECTED_TEXT : TEXT]);
        if ( fontSet != NULL ) {
            XmbDrawString(display, window, fontSet, gc, labelX, y + ascent,
                          label.c_str(), ((int)(label.size())));
        }
    }

    // Scroll indicator, when the rows do not fit
    if ( ((int)(rows.size())) > count && !rows.isEmpty() ) {
        int barHeight = Math::max(12, ((int)(height)) * count /
                                     ((int)(rows.size())));
        int barY = (((int)(height)) - barHeight) * scrollTop /
            Math::max(1, ((int)(rows.size())) - count);
        XSetForeground(display, gc, colors[LINE]);
        XFillRectangle(display, window, gc, width - SCROLLBAR_WIDTH - 2, barY,
                       SCROLLBAR_WIDTH, barHeight);
    }
}

void XtTreeView::click(int x, int y, unsigned int button, unsigned int state)
{
    if ( button == Button4 || button == Button5 ) {
        scrollTop += button == Button4 ? -3 : 3;
        clampScroll();
        redraw();
        return;
    }
    int index = scrollTop + (y - MARGIN / 2) / rowHeight;
    BOOLEAN onRow = y >= MARGIN / 2 && index >= 0 &&
        index < ((int)(rows.size()));

    if ( button == Button1 ) {
        if ( !onRow ) return;
        const Row& row = rows[index];
        int toggleX = MARGIN + row.depth * INDENT;
        if ( !row.node->getChildren().isEmpty() && x >= toggleX &&
             x < toggleX + INDENT ) {
            if ( expanded.contains(row.node) ) expanded.remove(row.node);
            else expanded.add(row.node);
            TreeSet<const DialogTreeNode*> before = selected;
            buildRows();
            clampScroll();
            redraw();
            if ( !before.equals(selected) ) notifySelection();
            return;
        }
        if ( (state & ShiftMask) != 0 && anchor >= 0 ) {
            selected.clear();
            for ( int i = Math::min(anchor, index); i <= Math::max(anchor, index); i++ ) {
                selected.add(rows[i].node);
            }
        }
        else if ( (state & ControlMask) != 0 ) {
            if ( selected.contains(row.node) ) selected.remove(row.node);
            else selected.add(row.node);
            anchor = index;
        }
        else {
            selected.clear();
            selected.add(row.node);
            anchor = index;
        }
        redraw();
        notifySelection();
    }
    else if ( button == Button3 ) {
        if ( onRow && !selected.contains(rows[index].node) ) {
            selected.clear();
            selected.add(rows[index].node);
            anchor = index;
            redraw();
            notifySelection();
        }
    }
}

void XtTreeView::key(XKeyEvent* event)
{
    char buffer[8];
    KeySym keysym = NoSymbol;
    XLookupString(event, buffer, sizeof(buffer), &keysym, NULL);
    if ( keysym == XK_Delete || keysym == XK_KP_Delete ||
         keysym == XK_BackSpace ) {
        if ( deleteListener != NULL ) {
            deleteListener->run();
        }
        return;
    }
    if ( (keysym == XK_Up || keysym == XK_Down) && !rows.isEmpty() ) {
        int current = anchor >= 0 ? anchor : -1;
        int next = keysym == XK_Up ? Math::max(0, current - 1) :
            Math::min(((int)(rows.size())) - 1, current + 1);
        selected.clear();
        selected.add(rows[next].node);
        anchor = next;
        ensureVisible(next);
        redraw();
        notifySelection();
    }
    else if ( keysym == XK_Page_Up || keysym == XK_Page_Down ) {
        scrollTop += (keysym == XK_Page_Up ? -1 : 1) * visibleRows();
        clampScroll();
        redraw();
    }
}

#include <algorithm>

#include "java/lang/String.h"
#include "vsdk/toolkit/gui/dialog/components/DialogTreeNode.h"
#include "vsdk/toolkit/render/gtk4/Gtk4TreeView.h"

namespace {
const int INDENT = 18;
const char* NODE_KEY = "vitral-tree-node";
}

Gtk4TreeView::Gtk4TreeView()
    : scrolled(gtk_scrolled_window_new()), list(gtk_list_box_new()),
      root(nullptr), anchor(-1), updating(false)
{
    gtk_list_box_set_selection_mode(GTK_LIST_BOX(list), GTK_SELECTION_MULTIPLE);
    gtk_widget_set_focusable(list, TRUE);
    gtk_scrolled_window_set_child(GTK_SCROLLED_WINDOW(scrolled), list);
    gtk_widget_set_vexpand(scrolled, TRUE);
    gtk_widget_set_hexpand(scrolled, TRUE);
    g_signal_connect(list, "selected-rows-changed",
                     G_CALLBACK(&Gtk4TreeView::selectionChanged), this);

    GtkEventController* keys = gtk_event_controller_key_new();
    g_signal_connect(keys, "key-pressed", G_CALLBACK(&Gtk4TreeView::keyPressed),
                     this);
    gtk_widget_add_controller(list, keys);

    // Clicks are taken before the rows (capture phase), so the list does
    // not apply its own selection
    GtkGesture* primary = gtk_gesture_click_new();
    gtk_gesture_single_set_button(GTK_GESTURE_SINGLE(primary),
                                  GDK_BUTTON_PRIMARY);
    gtk_event_controller_set_propagation_phase(GTK_EVENT_CONTROLLER(primary),
                                               GTK_PHASE_CAPTURE);
    g_signal_connect(primary, "pressed",
                     G_CALLBACK(&Gtk4TreeView::primaryPressed), this);
    gtk_widget_add_controller(list, GTK_EVENT_CONTROLLER(primary));

    GtkGesture* secondary = gtk_gesture_click_new();
    gtk_gesture_single_set_button(GTK_GESTURE_SINGLE(secondary),
                                  GDK_BUTTON_SECONDARY);
    gtk_event_controller_set_propagation_phase(GTK_EVENT_CONTROLLER(secondary),
                                               GTK_PHASE_CAPTURE);
    g_signal_connect(secondary, "pressed",
                     G_CALLBACK(&Gtk4TreeView::secondaryPressed), this);
    gtk_widget_add_controller(list, GTK_EVENT_CONTROLLER(secondary));
}

GtkWidget* Gtk4TreeView::getWidget() const
{
    return scrolled;
}

GtkWidget* Gtk4TreeView::getList() const
{
    return list;
}

void Gtk4TreeView::setRoot(const DialogTreeNode* root)
{
    this->root = root;
    expanded.clear();
    anchor = -1;
    if ( root != nullptr ) {
        // As the Java selector, the children of the root start expanded
        expanded.insert(root);
        for ( size_t i = 0; i < root->getChildren().size(); i++ ) {
            expanded.insert(&root->getChildren()[i]);
        }
    }
    buildRows();
}

void Gtk4TreeView::buildRows()
{
    updating = true;
    std::vector<const DialogTreeNode*> selected = getSelection();
    GtkWidget* child = gtk_widget_get_first_child(list);
    while ( child != nullptr ) {
        GtkWidget* next = gtk_widget_get_next_sibling(child);
        gtk_list_box_remove(GTK_LIST_BOX(list), child);
        child = next;
    }
    rows.clear();
    if ( root != nullptr ) {
        addRows(root, 0);
    }
    if ( anchor >= static_cast<int>(rows.size()) ) {
        anchor = -1;
    }
    // The rows still shown keep their selection
    for ( size_t i = 0; i < rows.size(); i++ ) {
        const DialogTreeNode* node =
            nodeOf(GTK_LIST_BOX_ROW(rows[i]));
        for ( size_t j = 0; j < selected.size(); j++ ) {
            if ( selected[j] == node ) {
                gtk_list_box_select_row(GTK_LIST_BOX(list),
                                        GTK_LIST_BOX_ROW(rows[i]));
            }
        }
    }
    updating = false;
}

void Gtk4TreeView::addRows(const DialogTreeNode* node, int depth)
{
    const std::vector<DialogTreeNode>& children = node->getChildren();
    for ( size_t i = 0; i < children.size(); i++ ) {
        const DialogTreeNode* childNode = &children[i];
        GtkWidget* box = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 2);
        gtk_widget_set_margin_start(box, depth * INDENT);
        // Expansion triangle (down when expanded), or a space of its size
        GtkWidget* toggle = gtk_label_new(
            childNode->getChildren().empty() ? "" :
            expanded.count(childNode) > 0 ? "\xe2\x96\xbc" : "\xe2\x96\xb6");
        gtk_widget_set_size_request(toggle, INDENT, -1);
        gtk_widget_add_css_class(toggle, "dim-label");
        if ( !childNode->getChildren().empty() ) {
            // A click over it expands or collapses (see primaryPressed)
            g_object_set_data(G_OBJECT(toggle), NODE_KEY,
                              const_cast<DialogTreeNode*>(childNode));
        }
        gtk_box_append(GTK_BOX(box), toggle);
        GtkWidget* label = gtk_label_new(childNode->getLabel().c_str());
        gtk_label_set_xalign(GTK_LABEL(label), 0.0f);
        gtk_box_append(GTK_BOX(box), label);

        GtkWidget* row = gtk_list_box_row_new();
        gtk_list_box_row_set_child(GTK_LIST_BOX_ROW(row), box);
        // Focusable rows make the list select the first one when the
        // window gives it the focus; the list itself takes the keys
        gtk_widget_set_focusable(row, FALSE);
        g_object_set_data(G_OBJECT(row), NODE_KEY,
                          const_cast<DialogTreeNode*>(childNode));
        gtk_list_box_append(GTK_LIST_BOX(list), row);
        rows.push_back(row);
        if ( expanded.count(childNode) > 0 ) {
            addRows(childNode, depth + 1);
        }
    }
}

const DialogTreeNode* Gtk4TreeView::nodeOf(GtkListBoxRow* row)
{
    return row != nullptr ? static_cast<const DialogTreeNode*>(
        g_object_get_data(G_OBJECT(row), NODE_KEY)) : nullptr;
}

void Gtk4TreeView::setSelection(const std::vector<const DialogTreeNode*>& nodes)
{
    updating = true;
    gtk_list_box_unselect_all(GTK_LIST_BOX(list));
    for ( size_t i = 0; i < rows.size(); i++ ) {
        const DialogTreeNode* node = nodeOf(GTK_LIST_BOX_ROW(rows[i]));
        for ( size_t j = 0; j < nodes.size(); j++ ) {
            if ( nodes[j] == node ) {
                gtk_list_box_select_row(GTK_LIST_BOX(list),
                                        GTK_LIST_BOX_ROW(rows[i]));
            }
        }
    }
    updating = false;
}

std::vector<const DialogTreeNode*> Gtk4TreeView::getSelection() const
{
    std::vector<const DialogTreeNode*> result;
    for ( size_t i = 0; i < rows.size(); i++ ) {
        if ( gtk_list_box_row_is_selected(GTK_LIST_BOX_ROW(rows[i])) ) {
            result.push_back(nodeOf(GTK_LIST_BOX_ROW(rows[i])));
        }
    }
    return result;
}

void Gtk4TreeView::selectOnly(const DialogTreeNode* node)
{
    std::vector<const DialogTreeNode*> nodes;
    if ( node != nullptr ) {
        nodes.push_back(node);
    }
    setSelection(nodes);
    anchor = -1;
    for ( size_t i = 0; i < rows.size(); i++ ) {
        if ( node != nullptr && nodeOf(GTK_LIST_BOX_ROW(rows[i])) == node ) {
            anchor = static_cast<int>(i);
        }
    }
    notifySelection();
}

/**
Selects the rows from one index to other (both included), keeping the
other selected rows or not, and notifies the change.
*/
void Gtk4TreeView::selectRows(int from, int to, bool keep)
{
    updating = true;
    if ( !keep ) {
        gtk_list_box_unselect_all(GTK_LIST_BOX(list));
    }
    for ( int i = std::min(from, to); i <= std::max(from, to); i++ ) {
        gtk_list_box_select_row(GTK_LIST_BOX(list), GTK_LIST_BOX_ROW(rows[i]));
    }
    updating = false;
    notifySelection();
}

void Gtk4TreeView::toggleExpansion(const DialogTreeNode* node)
{
    if ( expanded.count(node) > 0 ) expanded.erase(node);
    else expanded.insert(node);
    std::vector<const DialogTreeNode*> before = getSelection();
    buildRows();
    if ( before != getSelection() ) {
        notifySelection();
    }
}

void Gtk4TreeView::setSelectionListener(const Listener& listener)
{
    selectionListener = listener;
}

void Gtk4TreeView::setDeleteListener(const Listener& listener)
{
    deleteListener = listener;
}

void Gtk4TreeView::setContextMenuListener(const ContextMenuListener& listener)
{
    contextMenuListener = listener;
}

void Gtk4TreeView::notifySelection()
{
    if ( selectionListener ) {
        Listener listener = selectionListener;
        listener();
    }
}

void Gtk4TreeView::selectionChanged(GtkListBox*, gpointer data)
{
    Gtk4TreeView* self = static_cast<Gtk4TreeView*>(data);
    if ( !self->updating ) {
        self->notifySelection();
    }
}

void Gtk4TreeView::primaryPressed(GtkGestureClick* gesture, gint, gdouble x,
                                  gdouble y, gpointer data)
{
    Gtk4TreeView* self = static_cast<Gtk4TreeView*>(data);
    gtk_gesture_set_state(GTK_GESTURE(gesture), GTK_EVENT_SEQUENCE_CLAIMED);
    gtk_widget_grab_focus(self->list);

    // The triangle of a row expands or collapses it, without selecting
    GtkWidget* picked = gtk_widget_pick(self->list, x, y, GTK_PICK_DEFAULT);
    if ( picked != nullptr && GTK_IS_LABEL(picked) &&
         g_object_get_data(G_OBJECT(picked), NODE_KEY) != nullptr ) {
        self->toggleExpansion(static_cast<const DialogTreeNode*>(
            g_object_get_data(G_OBJECT(picked), NODE_KEY)));
        return;
    }
    GtkListBoxRow* row = gtk_list_box_get_row_at_y(GTK_LIST_BOX(self->list),
                                                   static_cast<int>(y));
    if ( row == nullptr ) {
        return;
    }
    int index = gtk_list_box_row_get_index(row);
    GdkModifierType state = gtk_event_controller_get_current_event_state(
        GTK_EVENT_CONTROLLER(gesture));
    if ( (state & GDK_SHIFT_MASK) != 0 && self->anchor >= 0 ) {
        self->selectRows(self->anchor, index, false);
    }
    else if ( (state & (GDK_CONTROL_MASK | GDK_META_MASK)) != 0 ) {
        self->anchor = index;
        if ( gtk_list_box_row_is_selected(row) ) {
            self->updating = true;
            gtk_list_box_unselect_row(GTK_LIST_BOX(self->list), row);
            self->updating = false;
            self->notifySelection();
        }
        else {
            self->selectRows(index, index, true);
        }
    }
    else {
        self->anchor = index;
        self->selectRows(index, index, false);
    }
}

gboolean Gtk4TreeView::keyPressed(GtkEventControllerKey*, guint keyval, guint,
                                  GdkModifierType, gpointer data)
{
    Gtk4TreeView* self = static_cast<Gtk4TreeView*>(data);
    if ( keyval == GDK_KEY_Delete || keyval == GDK_KEY_KP_Delete ||
         keyval == GDK_KEY_BackSpace ) {
        if ( self->deleteListener ) {
            Listener listener = self->deleteListener;
            listener();
        }
        return TRUE;
    }
    if ( (keyval == GDK_KEY_Up || keyval == GDK_KEY_Down) &&
         !self->rows.empty() ) {
        int last = static_cast<int>(self->rows.size()) - 1;
        int next = keyval == GDK_KEY_Up ? std::max(0, self->anchor - 1) :
            std::min(last, self->anchor + 1);
        self->anchor = next;
        self->selectRows(next, next, false);
        return TRUE;
    }
    return FALSE;
}

void Gtk4TreeView::secondaryPressed(GtkGestureClick* gesture, gint, gdouble x,
                                    gdouble y, gpointer data)
{
    Gtk4TreeView* self = static_cast<Gtk4TreeView*>(data);
    gtk_gesture_set_state(GTK_GESTURE(gesture), GTK_EVENT_SEQUENCE_CLAIMED);
    gtk_widget_grab_focus(self->list);
    GtkListBoxRow* row = gtk_list_box_get_row_at_y(GTK_LIST_BOX(self->list),
                                                   static_cast<int>(y));
    if ( row != nullptr && !gtk_list_box_row_is_selected(row) ) {
        self->anchor = gtk_list_box_row_get_index(row);
        self->selectRows(self->anchor, self->anchor, false);
    }
    if ( self->contextMenuListener ) {
        ContextMenuListener listener = self->contextMenuListener;
        listener(self->list, x, y);
    }
}

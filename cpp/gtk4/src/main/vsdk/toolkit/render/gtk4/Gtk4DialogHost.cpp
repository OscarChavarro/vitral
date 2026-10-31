#include "java/lang/IllegalArgumentException.h"
#include "java/lang/String.h"
#include "java/util/TreeMap.txx"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetDialogElement.h"
#include "vsdk/toolkit/render/gtk4/Gtk4DialogHost.h"
#include "vsdk/toolkit/render/gtk4/Gtk4TreeView.h"

namespace {

java::String message(Widget* messages, const java::String& key)
{
    return messages != nullptr ? messages->getMessage(key).c_str() :
        key.c_str();
}

/**
Adds the widgets of an element (rows and columns only group them, as in
the Swing host): text inputs to the content, buttons to the bottom row.
*/
void addElement(const WidgetDialogElement& element, GtkWidget* content,
                GtkWidget* buttons, Widget* messages,
                java::TreeMap<java::String, GtkWidget*>& fields,
                java::TreeMap<java::String, GtkWidget*>& actions,
                java::TreeMap<java::String, Gtk4TreeView*>& trees,
                GtkWidget** firstField)
{
    java::String kind = element.getKind().c_str();
    if ( kind == "textInput" ) {
        GtkWidget* label = gtk_label_new(
            message(messages, element.getLabelKey()).c_str());
        gtk_label_set_wrap(GTK_LABEL(label), TRUE);
        gtk_label_set_max_width_chars(GTK_LABEL(label), 60);
        gtk_label_set_xalign(GTK_LABEL(label), 0.0f);
        gtk_box_append(GTK_BOX(content), label);
        GtkWidget* field = gtk_entry_new();
        // As the 36 columns of the Swing text field
        gtk_editable_set_width_chars(GTK_EDITABLE(field), 36);
        gtk_widget_set_name(field, element.getBinding().c_str());
        gtk_box_append(GTK_BOX(content), field);
        fields.put(element.getBinding().c_str(), field);
        if ( *firstField == nullptr ) {
            *firstField = field;
        }
    }
    else if ( kind == "tree" ) {
        Gtk4TreeView* tree = new Gtk4TreeView();
        gtk_box_append(GTK_BOX(content), tree->getWidget());
        // Not focused when shown: a focused list selects its first row
        trees.put(element.getBinding().c_str(), tree);
    }
    else if ( kind == "action" ) {
        // Offered by the application with keys or menus
    }
    else if ( kind == "button" ) {
        GtkWidget* button = gtk_button_new_with_label(
            message(messages, element.getLabelKey()).c_str());
        gtk_widget_set_name(button, element.getAction().c_str());
        gtk_box_append(GTK_BOX(buttons), button);
        actions.put(element.getAction().c_str(), button);
    }
    else if ( kind == "row" || kind == "column" ) {
        const java::ArrayList<WidgetDialogElement>& children = element.getChildren();
        for ( size_t i = 0; i < children.size(); i++ ) {
            addElement(children[i], content, buttons, messages, fields,
                       actions, trees, firstField);
        }
    }
    else {
        throw java::IllegalArgumentException("Unsupported form element: " + kind);
    }
}

}

Gtk4DialogHost::Form::Form(const WidgetDialog* definition,
                           const ActionListener& onAction)
    : window(nullptr), firstField(nullptr), definition(definition),
      dialogSession(definition), onAction(onAction)
{
}

Gtk4DialogHost::Form::~Form()
{
    for (long tree = 0; tree < trees.size(); tree++) {
        delete trees.getValue(tree);
    }
}

Gtk4DialogHost::Form* Gtk4DialogHost::form(GtkWindow* owner, Widget* messages,
                                           const WidgetDialog* definition,
                                           const ActionListener& onAction)
{
    Form* form = new Form(definition, onAction);
    GtkWidget* content = gtk_box_new(GTK_ORIENTATION_VERTICAL, 8);
    GtkWidget* buttons = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
    gtk_widget_set_halign(buttons, GTK_ALIGN_CENTER);
    try {
        for ( size_t i = 0; i < definition->getElements().size(); i++ ) {
            addElement(definition->getElements()[i], content, buttons,
                       messages, form->fields, form->actions, form->trees,
                       &form->firstField);
        }
    }
    catch ( ... ) {
        g_object_ref_sink(content);
        g_object_unref(content);
        g_object_ref_sink(buttons);
        g_object_unref(buttons);
        delete form;
        throw;
    }

    form->window = gtk_window_new();
    gtk_widget_set_name(form->window, definition->getId().c_str());
    gtk_window_set_title(GTK_WINDOW(form->window),
        message(messages, definition->getTitleKey()).c_str());
    gtk_window_set_transient_for(GTK_WINDOW(form->window), owner);
    gtk_window_set_modal(GTK_WINDOW(form->window), definition->isModal());
    if ( form->trees.isEmpty() ) {
        gtk_window_set_resizable(GTK_WINDOW(form->window), FALSE);
    }
    else {
        // As the 450x560 Swing scene selector
        gtk_window_set_default_size(GTK_WINDOW(form->window), 450, 560);
        gtk_widget_set_vexpand(content, TRUE);
    }
    gtk_window_set_destroy_with_parent(GTK_WINDOW(form->window), TRUE);

    GtkWidget* root = gtk_box_new(GTK_ORIENTATION_VERTICAL, 10);
    gtk_widget_set_margin_start(root, 12);
    gtk_widget_set_margin_end(root, 12);
    gtk_widget_set_margin_top(root, 12);
    gtk_widget_set_margin_bottom(root, 12);
    gtk_box_append(GTK_BOX(root), content);
    gtk_box_append(GTK_BOX(root), buttons);
    gtk_window_set_child(GTK_WINDOW(form->window), root);

    for ( long action = 0; action < form->actions.size(); action++ ) {
        g_signal_connect(form->actions.getValue(action), "clicked",
                         G_CALLBACK(&Form::buttonClicked), form);
    }
    g_signal_connect(form->window, "destroy", G_CALLBACK(&Form::destroyed),
                     form);
    g_object_set_data_full(G_OBJECT(form->window), "vitral-dialog-form", form,
                           &Form::deleteForm);
    return form;
}

GtkWidget* Gtk4DialogHost::Form::dialog() const
{
    return window;
}

GtkWidget* Gtk4DialogHost::Form::field(const java::String& binding) const
{
    GtkWidget* const* found = fields.get(binding);
    return found != nullptr ? (*found) : nullptr;
}

GtkWidget* Gtk4DialogHost::Form::action(const java::String& action) const
{
    GtkWidget* const* found = actions.get(action);
    return found != nullptr ? (*found) : nullptr;
}

Gtk4TreeView* Gtk4DialogHost::Form::tree(const java::String& binding) const
{
    Gtk4TreeView* const* found = trees.get(binding);
    return found != nullptr ? (*found) : nullptr;
}

bool Gtk4DialogHost::Form::hasAction(const java::String& action) const
{
    return definition->getElementByAction(action.c_str()) != nullptr;
}

DialogSession* Gtk4DialogHost::Form::session()
{
    return &dialogSession;
}

void Gtk4DialogHost::Form::present()
{
    gtk_window_present(GTK_WINDOW(window));
    if ( firstField != nullptr ) {
        gtk_widget_grab_focus(firstField);
    }
}

void Gtk4DialogHost::Form::close()
{
    if ( window != nullptr ) {
        gtk_window_destroy(GTK_WINDOW(window));
    }
}

void Gtk4DialogHost::Form::buttonClicked(GtkButton* button, gpointer data)
{
    Form* form = static_cast<Form*>(data);
    // The listener may close the form: nothing of it is used afterwards
    ActionListener listener = form->onAction;
    java::String action = gtk_widget_get_name(GTK_WIDGET(button));
    if ( listener ) {
        listener.accept(action);
    }
}

void Gtk4DialogHost::Form::destroyed(GtkWidget*, gpointer data)
{
    Form* form = static_cast<Form*>(data);
    form->dialogSession.close();
    form->window = nullptr;
}

void Gtk4DialogHost::Form::deleteForm(gpointer data)
{
    delete static_cast<Form*>(data);
}

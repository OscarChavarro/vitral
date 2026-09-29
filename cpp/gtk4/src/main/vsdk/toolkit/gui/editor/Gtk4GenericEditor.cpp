#include <algorithm>
#include <cctype>
#include <string>

#include "vsdk/toolkit/gui/editor/ControlSpecification.h"
#include "vsdk/toolkit/gui/editor/Gtk4GenericEditor.h"

Gtk4GenericEditor::Gtk4GenericEditor(GtkWidget* container)
    : container(container), messageLabel(nullptr)
{
}

Gtk4GenericEditor::~Gtk4GenericEditor()
{
    detach();
}

void
Gtk4GenericEditor::clearBox(GtkWidget* box)
{
    if ( box == nullptr ) return;
    GtkWidget* child = gtk_widget_get_first_child(box);
    while ( child != nullptr ) {
        gtk_box_remove(GTK_BOX(box), child);
        child = gtk_widget_get_first_child(box);
    }
}

void
Gtk4GenericEditor::beginBuild(const java::String& title)
{
    fields.clear();
    messageLabel = nullptr;
    clearBox(container);

    std::string text = title.c_str();
    std::transform(text.begin(), text.end(), text.begin(),
        [](unsigned char c) { return static_cast<char>(std::toupper(c)); });
    GtkWidget* titleLabel = gtk_label_new(text.c_str());
    gtk_label_set_xalign(GTK_LABEL(titleLabel), 0.5f);
    gtk_widget_set_margin_bottom(titleLabel, 8);
    gtk_box_append(GTK_BOX(container), titleLabel);
}

void
Gtk4GenericEditor::addControl(const ControlSpecification* specification,
                              const java::String& value)
{
    if ( specification == nullptr || container == nullptr ) return;

    GtkWidget* row = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 6);
    gtk_widget_set_hexpand(row, TRUE);

    std::string labelText = specification->getLabel().c_str();
    if ( specification->getIntervalText().length() > 0 ) {
        labelText += "\n";
        labelText += specification->getIntervalText().c_str();
    }

    GtkWidget* label = gtk_label_new(labelText.c_str());
    gtk_label_set_xalign(GTK_LABEL(label), 1.0f);
    gtk_label_set_wrap(GTK_LABEL(label), TRUE);
    gtk_widget_set_size_request(label, 86, -1);
    gtk_box_append(GTK_BOX(row), label);

    GtkWidget* field = gtk_entry_new();
    gtk_editable_set_text(GTK_EDITABLE(field), value.c_str());
    gtk_widget_set_hexpand(field, TRUE);
    fields[field] = specification;
    g_signal_connect(field, "activate", G_CALLBACK(fieldActivatedThunk), this);
    g_signal_connect(field, "changed", G_CALLBACK(fieldChangedThunk), this);
    gtk_box_append(GTK_BOX(row), field);

    gtk_box_append(GTK_BOX(container), row);
}

void
Gtk4GenericEditor::endBuild()
{
    if ( messageLabel == nullptr ) {
        messageLabel = gtk_label_new("");
        gtk_label_set_wrap(GTK_LABEL(messageLabel), TRUE);
        gtk_label_set_xalign(GTK_LABEL(messageLabel), 0.0f);
        gtk_widget_add_css_class(messageLabel, "error");
        gtk_widget_set_margin_top(messageLabel, 8);
    }
    if ( gtk_widget_get_parent(messageLabel) == nullptr ) {
        gtk_box_append(GTK_BOX(container), messageLabel);
    }
}

void
Gtk4GenericEditor::showValidationMessage(const char* message)
{
    if ( messageLabel == nullptr ) {
        messageLabel = gtk_label_new("");
        gtk_label_set_wrap(GTK_LABEL(messageLabel), TRUE);
        gtk_label_set_xalign(GTK_LABEL(messageLabel), 0.0f);
        gtk_widget_add_css_class(messageLabel, "error");
    }
    gtk_label_set_text(GTK_LABEL(messageLabel), message != nullptr ? message : "");
}

void
Gtk4GenericEditor::setControlValue(
    const ControlSpecification* specification, const java::String& value)
{
    for ( std::map<GtkWidget*, const ControlSpecification*>::iterator it =
              fields.begin(); it != fields.end(); ++it ) {
        if ( it->second != specification ) continue;
        gtk_editable_set_text(GTK_EDITABLE(it->first), value.c_str());
        gtk_widget_remove_css_class(it->first, "error");
        return;
    }
}

void
Gtk4GenericEditor::clearControls(const java::String& message)
{
    fields.clear();
    messageLabel = nullptr;
    clearBox(container);
    GtkWidget* label = gtk_label_new(message.c_str());
    gtk_label_set_wrap(GTK_LABEL(label), TRUE);
    gtk_label_set_xalign(GTK_LABEL(label), 0.0f);
    gtk_box_append(GTK_BOX(container), label);
}

void
Gtk4GenericEditor::fieldActivated(GtkWidget* field)
{
    std::map<GtkWidget*, const ControlSpecification*>::iterator it =
        fields.find(field);
    if ( it == fields.end() ) return;

    const char* text = gtk_editable_get_text(GTK_EDITABLE(field));
    if ( updateValue(it->second, text != nullptr ? text : "") ) {
        gtk_widget_remove_css_class(field, "error");
    }
    else {
        gtk_widget_add_css_class(field, "error");
    }
}

void
Gtk4GenericEditor::fieldActivatedThunk(GtkEntry* entry, gpointer data)
{
    static_cast<Gtk4GenericEditor*>(data)->fieldActivated(GTK_WIDGET(entry));
}

void
Gtk4GenericEditor::fieldChangedThunk(GtkEditable* editable, gpointer)
{
    gtk_widget_remove_css_class(GTK_WIDGET(editable), "error");
}

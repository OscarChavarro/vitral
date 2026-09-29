#ifndef __GTK4_GENERIC_EDITOR__
#define __GTK4_GENERIC_EDITOR__

#include <map>

#include <gtk/gtk.h>

#include "vsdk/toolkit/gui/editor/GenericEditor.h"

class ControlSpecification;

/**
GTK4 presentation for the GUI-independent GenericEditor.

The base class owns the editing logic: supported controls, validation,
entity accessors and change notifications. This class only builds GTK widgets
and forwards confirmed values back to GenericEditor::updateValue.
*/
class Gtk4GenericEditor : public GenericEditor {
private:
    GtkWidget* container;
    GtkWidget* messageLabel;
    std::map<GtkWidget*, const ControlSpecification*> fields;

    Gtk4GenericEditor(const Gtk4GenericEditor& other);
    Gtk4GenericEditor& operator=(const Gtk4GenericEditor& other);

    static void clearBox(GtkWidget* box);
    static void fieldActivatedThunk(GtkEntry* entry, gpointer data);
    static void fieldChangedThunk(GtkEditable* editable, gpointer data);

    void fieldActivated(GtkWidget* field);

protected:
    virtual void beginBuild(const java::String& title) override;
    virtual void addControl(const ControlSpecification* specification,
                            const java::String& value) override;
    virtual void endBuild() override;
    virtual void showValidationMessage(const char* message) override;
    virtual void setControlValue(const ControlSpecification* specification,
                                 const java::String& value) override;
    virtual void clearControls(const java::String& message) override;

public:
    explicit Gtk4GenericEditor(GtkWidget* container);
    virtual ~Gtk4GenericEditor();
};

#endif

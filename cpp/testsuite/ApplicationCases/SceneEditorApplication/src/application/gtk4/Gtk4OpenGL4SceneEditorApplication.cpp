#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <algorithm>
#include <cctype>
#include <dlfcn.h>
#include <exception>
#include <fstream>
#include <string>
#include <vector>

#include <gtk/gtk.h>

#include <glad/gl.h>

#include "application/TangibleInterfaceConnection.h"
#include "application/commands/GuiEventExecutor.h"
#include "java/io/File.h"
#include "java/lang/NumberFormatException.h"
#include "java/util/ArrayList.txx"
#include "model/ApplicationModel.h"
#include "model/GuiState.h"
#include "model/dialog/SceneHierarchyPresenter.h"
#include "model/editor/FunctionalExplicitSurfaceEditor.h"
#include "render/BodyEditFeedbackProvider.h"
#include "render/opengl4/XtOpenGL4SceneBridge.h"
#include "vsdk/toolkit/common/Entity.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/editor/GenericEditorListener.h"
#include "vsdk/toolkit/gui/editor/Gtk4GenericEditor.h"
#include "vsdk/toolkit/gui/Gtk4LabelImageProvider.h"
#include "vsdk/toolkit/gui/Gtk4System.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/dialog/components/DialogFileFilter.h"
#include "vsdk/toolkit/gui/widget/WidgetButtonGroup.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/render/gtk4/Gtk4DialogHost.h"
#include "vsdk/toolkit/render/gtk4/Gtk4TreeView.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4Loader.h"

namespace {

static void* openglHandle()
{
    static void* handle = nullptr;
    if ( handle == nullptr ) {
#ifdef __APPLE__
        handle = dlopen(
            "/System/Library/Frameworks/OpenGL.framework/OpenGL",
            RTLD_LAZY | RTLD_LOCAL);
#else
        handle = dlopen("libGL.so.1", RTLD_LAZY | RTLD_LOCAL);
#endif
    }
    return handle;
}

static GLADapiproc gtk4GetProcAddress(const char* name)
{
    void* symbol = dlsym(RTLD_DEFAULT, name);
    if ( symbol == nullptr ) {
        void* handle = openglHandle();
        if ( handle != nullptr ) symbol = dlsym(handle, name);
    }
    return reinterpret_cast<GLADapiproc>(symbol);
}

/// Arguments of the editor that GApplication does not know
/// (`-tangibleServer <url>`), taken out of its command line by `main`
static std::vector<char*> editorArguments;

static const int INITIAL_SIDE_PANEL_WIDTH = 260;
static const int MIN_SIDE_PANEL_WIDTH = 220;

static void clearGtkBox(GtkWidget* box)
{
    if ( box == nullptr ) return;
    GtkWidget* child = gtk_widget_get_first_child(box);
    while ( child != nullptr ) {
        gtk_box_remove(GTK_BOX(box), child);
        child = gtk_widget_get_first_child(box);
    }
}

class Gtk4ModifyPanelHost {
public:
    virtual ~Gtk4ModifyPanelHost() {}
    virtual void repaintRequestedFromModifyPanel() = 0;
};

class Gtk4FunctionalExplicitSurfaceEditor {
private:
    Gtk4ModifyPanelHost* host;
    FunctionalExplicitSurfaceEditor* editor;
    GtkWidget* fields[FunctionalExplicitSurfaceEditor::PARAMETER_COUNT];
    GtkWidget* messageLabel;
    std::vector<std::string> presets;

    Gtk4FunctionalExplicitSurfaceEditor(
        const Gtk4FunctionalExplicitSurfaceEditor& other);
    Gtk4FunctionalExplicitSurfaceEditor& operator=(
        const Gtk4FunctionalExplicitSurfaceEditor& other);

    void refreshFields()
    {
        if ( editor == nullptr ) return;
        for ( int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; i++ ) {
            if ( fields[i] == nullptr ) continue;
            FunctionalExplicitSurfaceEditor::Parameter parameter =
                static_cast<FunctionalExplicitSurfaceEditor::Parameter>(i);
            gtk_editable_set_text(GTK_EDITABLE(fields[i]),
                                  editor->getValue(parameter).c_str());
            gtk_widget_remove_css_class(fields[i], "error");
        }
    }

    void setMessage(const char* message)
    {
        if ( messageLabel != nullptr ) {
            gtk_label_set_text(GTK_LABEL(messageLabel),
                               message != nullptr ? message : "");
        }
    }

    void presetSelected(int index)
    {
        if ( editor == nullptr ) return;
        if ( index < 0 || index >= static_cast<int>(presets.size()) ) return;
        try {
            if ( editor->applyPreset(presets[index].c_str()) ) {
                refreshFields();
                setMessage(nullptr);
                host->repaintRequestedFromModifyPanel();
            }
        }
        catch ( const java::NumberFormatException& e ) {
            setMessage(e.getMessage().c_str());
        }
    }

    void fieldActivated(GtkWidget* field)
    {
        if ( editor == nullptr ) return;
        int index = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(field), "parameter"));
        if ( index < 0 ||
             index >= FunctionalExplicitSurfaceEditor::PARAMETER_COUNT ) {
            return;
        }
        const char* text = gtk_editable_get_text(GTK_EDITABLE(field));
        try {
            editor->setValue(
                static_cast<FunctionalExplicitSurfaceEditor::Parameter>(index),
                text != nullptr ? text : "");
            gtk_widget_remove_css_class(field, "error");
            setMessage(nullptr);
            host->repaintRequestedFromModifyPanel();
        }
        catch ( const java::NumberFormatException& e ) {
            gtk_widget_add_css_class(field, "error");
            setMessage(e.getMessage().c_str());
        }
    }

    static void presetRowActivatedThunk(GtkListBox*, GtkListBoxRow* row,
                                        gpointer data)
    {
        int index = gtk_list_box_row_get_index(row);
        static_cast<Gtk4FunctionalExplicitSurfaceEditor*>(data)
            ->presetSelected(index);
    }

    static void fieldActivatedThunk(GtkEntry* entry, gpointer data)
    {
        static_cast<Gtk4FunctionalExplicitSurfaceEditor*>(data)
            ->fieldActivated(GTK_WIDGET(entry));
    }

    static void fieldChangedThunk(GtkEditable* editable, gpointer)
    {
        gtk_widget_remove_css_class(GTK_WIDGET(editable), "error");
    }

public:
    explicit Gtk4FunctionalExplicitSurfaceEditor(Gtk4ModifyPanelHost* host)
        : host(host), editor(nullptr), messageLabel(nullptr)
    {
        for ( int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; i++ ) {
            fields[i] = nullptr;
        }
    }

    ~Gtk4FunctionalExplicitSurfaceEditor()
    {
        delete editor;
    }

    void build(SimpleBody* target, GtkWidget* container)
    {
        delete editor;
        editor = new FunctionalExplicitSurfaceEditor(target);
        messageLabel = nullptr;
        presets.clear();
        for ( int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; i++ ) {
            fields[i] = nullptr;
        }

        GtkWidget* title = gtk_label_new(FunctionalExplicitSurfaceEditor::TITLE);
        gtk_label_set_xalign(GTK_LABEL(title), 0.5f);
        gtk_widget_set_margin_bottom(title, 8);
        gtk_box_append(GTK_BOX(container), title);

        GtkWidget* presetLabel =
            gtk_label_new(FunctionalExplicitSurfaceEditor::PRESETS_LABEL);
        gtk_label_set_xalign(GTK_LABEL(presetLabel), 0.0f);
        gtk_box_append(GTK_BOX(container), presetLabel);

        GtkWidget* presetFrame = gtk_frame_new(nullptr);
        gtk_widget_set_margin_bottom(presetFrame, 8);
        GtkWidget* presetBox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
        gtk_frame_set_child(GTK_FRAME(presetFrame), presetBox);

        GtkWidget* presetTitle =
            gtk_label_new(FunctionalExplicitSurfaceEditor::PRESETS_LABEL);
        gtk_label_set_xalign(GTK_LABEL(presetTitle), 0.0f);
        gtk_widget_set_margin_top(presetTitle, 4);
        gtk_widget_set_margin_bottom(presetTitle, 4);
        gtk_widget_set_margin_start(presetTitle, 6);
        gtk_widget_set_margin_end(presetTitle, 6);
        gtk_box_append(GTK_BOX(presetBox), presetTitle);

        GtkWidget* presetList = gtk_list_box_new();
        gtk_list_box_set_selection_mode(GTK_LIST_BOX(presetList),
                                        GTK_SELECTION_SINGLE);
        g_signal_connect(presetList, "row-activated",
                         G_CALLBACK(presetRowActivatedThunk), this);
        java::ArrayList<java::String> presetNames =
            FunctionalExplicitSurfaceEditor::getPresets();
        for ( long i = 0; i < presetNames.size(); i++ ) {
            std::string preset = presetNames.get(i).c_str();
            presets.push_back(preset);
            GtkWidget* row = gtk_list_box_row_new();
            GtkWidget* label = gtk_label_new(preset.c_str());
            gtk_label_set_xalign(GTK_LABEL(label), 0.0f);
            gtk_widget_set_margin_top(label, 4);
            gtk_widget_set_margin_bottom(label, 4);
            gtk_widget_set_margin_start(label, 6);
            gtk_widget_set_margin_end(label, 6);
            gtk_list_box_row_set_child(GTK_LIST_BOX_ROW(row), label);
            gtk_list_box_append(GTK_LIST_BOX(presetList), row);
        }
        gtk_list_box_select_row(GTK_LIST_BOX(presetList),
            gtk_list_box_get_row_at_index(GTK_LIST_BOX(presetList), 0));
        gtk_box_append(GTK_BOX(presetBox), presetList);
        gtk_box_append(GTK_BOX(container), presetFrame);

        for ( int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; i++ ) {
            FunctionalExplicitSurfaceEditor::Parameter parameter =
                static_cast<FunctionalExplicitSurfaceEditor::Parameter>(i);
            std::string labelText =
                FunctionalExplicitSurfaceEditor::getLabel(parameter).c_str();
            std::string value = editor->getValue(parameter).c_str();

            if ( parameter == FunctionalExplicitSurfaceEditor::FUNCTION ) {
                GtkWidget* label = gtk_label_new(labelText.c_str());
                gtk_label_set_xalign(GTK_LABEL(label), 0.0f);
                gtk_box_append(GTK_BOX(container), label);

                GtkWidget* field = gtk_entry_new();
                gtk_editable_set_text(GTK_EDITABLE(field), value.c_str());
                gtk_widget_set_hexpand(field, TRUE);
                g_object_set_data(G_OBJECT(field), "parameter", GINT_TO_POINTER(i));
                g_signal_connect(field, "activate",
                                 G_CALLBACK(fieldActivatedThunk), this);
                g_signal_connect(field, "changed",
                                 G_CALLBACK(fieldChangedThunk), this);
                gtk_box_append(GTK_BOX(container), field);
                fields[i] = field;
                continue;
            }

            GtkWidget* row = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 6);
            GtkWidget* label = gtk_label_new(labelText.c_str());
            gtk_label_set_xalign(GTK_LABEL(label), 1.0f);
            gtk_widget_set_size_request(label, 86, -1);
            gtk_box_append(GTK_BOX(row), label);

            GtkWidget* field = gtk_entry_new();
            gtk_editable_set_text(GTK_EDITABLE(field), value.c_str());
            gtk_widget_set_hexpand(field, TRUE);
            g_object_set_data(G_OBJECT(field), "parameter", GINT_TO_POINTER(i));
            g_signal_connect(field, "activate", G_CALLBACK(fieldActivatedThunk), this);
            g_signal_connect(field, "changed", G_CALLBACK(fieldChangedThunk), this);
            gtk_box_append(GTK_BOX(row), field);
            gtk_box_append(GTK_BOX(container), row);
            fields[i] = field;
        }

        messageLabel = gtk_label_new("");
        gtk_label_set_xalign(GTK_LABEL(messageLabel), 0.0f);
        gtk_label_set_wrap(GTK_LABEL(messageLabel), TRUE);
        gtk_widget_add_css_class(messageLabel, "error");
        gtk_widget_set_margin_top(messageLabel, 8);
        gtk_box_append(GTK_BOX(container), messageLabel);
    }
};

class Gtk4ModifyPanel :
    public BodyEditFeedbackProvider,
    private GenericEditorListener {
private:
    Gtk4ModifyPanelHost* host;
    GtkWidget* container;
    SimpleBody* target;
    Gtk4FunctionalExplicitSurfaceEditor* functionalExplicitSurfaceEditor;
    Gtk4GenericEditor* genericEditor;

    Gtk4ModifyPanel(const Gtk4ModifyPanel& other);
    Gtk4ModifyPanel& operator=(const Gtk4ModifyPanel& other);

    void showMessage(const char* message)
    {
        clearGtkBox(container);
        GtkWidget* label = gtk_label_new(message);
        gtk_label_set_wrap(GTK_LABEL(label), TRUE);
        gtk_label_set_xalign(GTK_LABEL(label), 0.0f);
        gtk_box_append(GTK_BOX(container), label);
    }

    virtual void notifyEntityChanged(Entity*) override
    {
        host->repaintRequestedFromModifyPanel();
    }

public:
    Gtk4ModifyPanel(Gtk4ModifyPanelHost* host, GtkWidget* container)
        : host(host),
          container(container),
          target(nullptr),
          functionalExplicitSurfaceEditor(nullptr),
          genericEditor(nullptr)
    {
        showMessage("No selected object for modifying.");
    }

    virtual ~Gtk4ModifyPanel()
    {
        delete functionalExplicitSurfaceEditor;
        delete genericEditor;
    }

    virtual SimpleBody* getTarget() override
    {
        return target;
    }

    void notifyTargetBeginEdit(SimpleBody* target)
    {
        if ( target == nullptr ) {
            notifyTargetEndEdit();
            return;
        }
        if ( this->target == target ) return;

        this->target = target;
        clearGtkBox(container);

        if ( dynamic_cast<FunctionalExplicitSurface*>(
                 target->getGeometry()) != nullptr ) {
            if ( genericEditor != nullptr ) genericEditor->detach();
            if ( functionalExplicitSurfaceEditor == nullptr ) {
                functionalExplicitSurfaceEditor =
                    new Gtk4FunctionalExplicitSurfaceEditor(host);
            }
            functionalExplicitSurfaceEditor->build(target, container);
        }
        else {
            if ( genericEditor == nullptr ) {
                genericEditor = new Gtk4GenericEditor(container);
                genericEditor->setListener(this);
            }
            genericEditor->build(target->getGeometry());
        }
    }

    void notifyTargetEndEdit()
    {
        if ( target == nullptr ) return;
        target = nullptr;
        if ( genericEditor != nullptr && genericEditor->isEntityDeleted() ) {
            return;
        }
        if ( genericEditor != nullptr ) genericEditor->detach();
        showMessage("No selected object for modifying.");
    }

    virtual java::ArrayList<RenderPrimitive> buildEditFeedback() override
    {
        return java::ArrayList<RenderPrimitive>();
    }
};

/**
Dialog to select objects of the scene (`h` key), as `AwtSelectorDialog`:
a GTK 4 adapter of the `scene.selector` definition of
`etc/gui/dialogs.json` (built by `Gtk4DialogHost`) for the
`SceneHierarchyPresenter`. Its tree shows the lights, cameras and geometry
objects of the scene; selecting rows selects their elements (undoable),
the Delete key runs the `deleteSelected` action and the context menu of the
selected bodies runs `toggleVisibility`. It is not modal, and closing it
only hides it.
*/
class Gtk4SelectorDialog {
private:
    static constexpr const char* TREE_BINDING = "sceneHierarchy";

    SceneHierarchyPresenter presenter;
    Gtk4DialogHost::Form* form;
    DialogTreeNode hierarchy;
    bool updating;
    GtkWidget* popover;
    GtkWidget* popoverButton;

    Gtk4SelectorDialog(const Gtk4SelectorDialog& other);
    Gtk4SelectorDialog& operator=(const Gtk4SelectorDialog& other);

    Gtk4TreeView* tree() const
    {
        return form != nullptr ? form->tree(TREE_BINDING) : nullptr;
    }

    void collectSelected(const DialogTreeNode* node,
                         std::vector<const DialogTreeNode*>& selected)
    {
        if ( presenter.isSelected(node) ) selected.push_back(node);
        for ( size_t i = 0; i < node->getChildren().size(); i++ ) {
            collectSelected(&node->getChildren()[i], selected);
        }
    }

    void applyTreeSelection()
    {
        if ( !updating && tree() != nullptr ) {
            presenter.select(tree()->getSelection());
        }
    }

    void showPopup(GtkWidget* list, double x, double y)
    {
        std::vector<SimpleBody*> bodies =
            presenter.selectedBodies(tree()->getSelection());
        if ( bodies.empty() ) return;
        bool anyHidden = presenter.anyHidden(bodies);
        if ( popover == nullptr ) {
            popover = gtk_popover_new();
            popoverButton = gtk_button_new_with_label("");
            gtk_button_set_has_frame(GTK_BUTTON(popoverButton), FALSE);
            g_signal_connect(popoverButton, "clicked",
                             G_CALLBACK(popupChosenThunk), this);
            gtk_popover_set_child(GTK_POPOVER(popover), popoverButton);
            gtk_widget_set_parent(popover, list);
            // A popover must be unparented before its parent goes
            g_signal_connect(list, "destroy", G_CALLBACK(listDestroyedThunk), this);
        }
        gtk_button_set_label(GTK_BUTTON(popoverButton),
            presenter.text(anyHidden ? "IDM_SELECTOR_SHOW" : "IDM_SELECTOR_HIDE",
                           anyHidden ? "Show objects" : "Hide objects").c_str());
        GdkRectangle pointer = { static_cast<int>(x), static_cast<int>(y), 1, 1 };
        gtk_popover_set_pointing_to(GTK_POPOVER(popover), &pointer);
        gtk_popover_popup(GTK_POPOVER(popover));
    }

    static void popupChosenThunk(GtkButton*, gpointer data)
    {
        Gtk4SelectorDialog* self = static_cast<Gtk4SelectorDialog*>(data);
        gtk_popover_popdown(GTK_POPOVER(self->popover));
        self->performAction("toggleVisibility");
    }

    static void listDestroyedThunk(GtkWidget*, gpointer data)
    {
        Gtk4SelectorDialog* self = static_cast<Gtk4SelectorDialog*>(data);
        if ( self->popover != nullptr ) {
            gtk_widget_unparent(self->popover);
            self->popover = nullptr;
            self->popoverButton = nullptr;
        }
    }

    static void windowDestroyedThunk(GtkWidget*, gpointer data)
    {
        static_cast<Gtk4SelectorDialog*>(data)->form = nullptr;
    }

public:
    Gtk4SelectorDialog(GtkWindow* owner, ApplicationModel* model,
                       const std::function<void()>& repaint)
        : presenter(model, repaint),
          form(nullptr),
          hierarchy("Scene", nullptr),
          updating(false),
          popover(nullptr),
          popoverButton(nullptr)
    {
        Widget* messages = model->getI18nContext();
        const WidgetDialog* definition = messages != nullptr ?
            messages->getDialogById("scene.selector") : nullptr;
        if ( definition == nullptr ) {
            throw std::logic_error("Missing scene.selector dialog");
        }
        form = Gtk4DialogHost::form(owner, messages, definition,
                                    [](const std::string&) {});
        if ( tree() == nullptr ) {
            form->close();
            form = nullptr;
            throw std::logic_error("The scene.selector dialog has no tree");
        }
        gtk_window_set_hide_on_close(GTK_WINDOW(form->dialog()), TRUE);
        g_signal_connect(form->dialog(), "destroy",
                         G_CALLBACK(windowDestroyedThunk), this);
        tree()->setSelectionListener([this]() { applyTreeSelection(); });
        tree()->setDeleteListener([this]() { performAction("deleteSelected"); });
        tree()->setContextMenuListener([this](GtkWidget* list, double x, double y) {
            showPopup(list, x, y);
        });
        rebuild();
    }

    ~Gtk4SelectorDialog()
    {
        if ( form != nullptr ) {
            g_signal_handlers_disconnect_by_data(form->dialog(), this);
            form->close();
        }
    }

    void rebuild()
    {
        if ( tree() == nullptr ) return;
        updating = true;
        tree()->setRoot(nullptr);
        hierarchy = presenter.hierarchy();
        tree()->setRoot(&hierarchy);
        std::vector<const DialogTreeNode*> selected;
        collectSelected(&hierarchy, selected);
        tree()->setSelection(selected);
        updating = false;
    }

    void setVisible(bool visible)
    {
        if ( form == nullptr ) return;
        if ( visible ) form->present();
        else gtk_widget_set_visible(form->dialog(), FALSE);
    }

    bool isVisible() const
    {
        return form != nullptr && gtk_widget_get_visible(form->dialog());
    }

    void selectBody(int index)
    {
        if ( tree() == nullptr ) return;
        if ( index < 0 ) {
            tree()->selectOnly(nullptr);
            return;
        }
        // The geometry objects are the third group, as in the Java selector
        tree()->selectOnly(&hierarchy.getChildren().at(2).getChildren().at(index));
    }

    void performAction(const std::string& action)
    {
        if ( form == nullptr || !form->hasAction(action) ) {
            throw std::invalid_argument("Unknown selector action: " + action);
        }
        if ( action == "deleteSelected" ) {
            presenter.deleteSelected();
            rebuild();
        }
        else if ( action == "toggleVisibility" ) {
            std::vector<SimpleBody*> bodies =
                presenter.selectedBodies(tree()->getSelection());
            if ( !bodies.empty() ) {
                presenter.setHidden(bodies, !presenter.anyHidden(bodies));
                rebuild();
            }
        }
    }
};

class Gtk4SceneEditor :
    private XtOpenGL4SceneBridge::Listener,
    private Gtk4ModifyPanelHost {
private:
    GtkApplication* application;
    GtkWidget* window;
    GtkWidget* root;
    GtkWidget* menuBar;
    GtkWidget* glArea;
    GtkWidget* status;
    GtkWidget* viewportMenu;
    GtkWidget* chromeBox;
    GtkWidget* globalBar;
    GtkWidget* sidePanel;
    Gtk4ModifyPanel* modifyPanel;
    guint modifyUpdateSource;
    GMenu* applicationMenu;
    std::string guiLanguage;
    Gtk4LabelImageProvider labels;
    XtOpenGL4SceneBridge sceneBridge;
    bool glLoaded;
    bool bridgeInitialized;
    bool buttonDown;
    // As in AWT, a press followed by any motion is not a click
    bool pressMoved;
    double pressX;
    double pressY;
    gint pressCount;
    TangibleInterfaceConnection tangibleConnection;
    // Form of the tangible interface connection dialog, while it exists,
    // and the serial identifying it (answers for older forms are ignored)
    Gtk4DialogHost::Form* tangibleForm;
    long tangibleFormSerial;
    Gtk4SelectorDialog* selectorDialog;

public:
    explicit Gtk4SceneEditor(GtkApplication* app)
        : application(app),
          window(nullptr),
          root(nullptr),
          menuBar(nullptr),
          glArea(nullptr),
          status(nullptr),
          viewportMenu(nullptr),
          chromeBox(nullptr),
          globalBar(nullptr),
          sidePanel(nullptr),
          modifyPanel(nullptr),
          modifyUpdateSource(0),
          applicationMenu(nullptr),
          guiLanguage(selectedGuiLanguage()),
          sceneBridge(&labels, this),
          glLoaded(false),
          bridgeInitialized(false),
          buttonDown(false),
          pressMoved(false),
          pressX(0.0),
          pressY(0.0),
          pressCount(0),
          tangibleConnection([this]() {
              // From the thread of the network client
              g_idle_add(tangibleInterfaceEventThunk, this);
          }),
          tangibleForm(nullptr),
          tangibleFormSerial(0),
          selectorDialog(nullptr)
    {
        tangibleConnection.processArguments(
            static_cast<int>(editorArguments.size()), editorArguments.data());
    }

    ~Gtk4SceneEditor()
    {
        delete selectorDialog;
        selectorDialog = nullptr;
        if ( modifyUpdateSource != 0 ) {
            g_source_remove(modifyUpdateSource);
            modifyUpdateSource = 0;
        }
        sceneBridge.setBodyEditFeedbackProvider(nullptr);
        delete modifyPanel;
        modifyPanel = nullptr;
        if ( bridgeInitialized && glArea != nullptr ) {
            gtk_gl_area_make_current(GTK_GL_AREA(glArea));
            sceneBridge.dispose();
        }
        if ( applicationMenu != nullptr ) {
            g_object_unref(applicationMenu);
            applicationMenu = nullptr;
        }
    }

    void show()
    {
        window = gtk_application_window_new(application);
        gtk_window_set_title(GTK_WINDOW(window),
                             "Vitral Scene Editor - GTK4 OpenGL4");
        gtk_window_set_default_size(GTK_WINDOW(window), 1200, 820);
        gtk_window_maximize(GTK_WINDOW(window));

        loadGuiDefinition();
        setupApplicationActions();
        applicationMenu = createApplicationMenu();

        root = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
        gtk_window_set_child(GTK_WINDOW(window), root);

        menuBar = createMenuBar();
        if ( menuBar != nullptr ) gtk_box_append(GTK_BOX(root), menuBar);
        globalBar = createButtonGroup(sceneBridge.getButtonGroup("GLOBAL"), true);
        gtk_box_append(GTK_BOX(root), globalBar);

        chromeBox = gtk_paned_new(GTK_ORIENTATION_HORIZONTAL);
        gtk_widget_set_hexpand(chromeBox, TRUE);
        gtk_widget_set_vexpand(chromeBox, TRUE);
        gtk_box_append(GTK_BOX(root), chromeBox);

        glArea = gtk_gl_area_new();
        // On macOS, forcing the GL version/API here makes GDK create an
        // incompatible shared CGL context. Let GTK choose the context, then
        // OpenGL4Loader verifies that it provides OpenGL 4.1.
        gtk_gl_area_set_has_depth_buffer(GTK_GL_AREA(glArea), TRUE);
        gtk_gl_area_set_has_stencil_buffer(GTK_GL_AREA(glArea), TRUE);
        gtk_widget_set_focusable(glArea, TRUE);
        gtk_widget_set_hexpand(glArea, TRUE);
        gtk_widget_set_vexpand(glArea, TRUE);
        gtk_paned_set_start_child(GTK_PANED(chromeBox), glArea);
        gtk_paned_set_resize_start_child(GTK_PANED(chromeBox), TRUE);
        gtk_paned_set_shrink_start_child(GTK_PANED(chromeBox), FALSE);

        sidePanel = createSidePanel();
        gtk_paned_set_end_child(GTK_PANED(chromeBox), sidePanel);
        gtk_paned_set_resize_end_child(GTK_PANED(chromeBox), FALSE);
        gtk_paned_set_shrink_end_child(GTK_PANED(chromeBox), TRUE);

        status = gtk_label_new(message("IDM_INTRO_MESSAGE").c_str());
        gtk_widget_set_halign(status, GTK_ALIGN_START);
        gtk_widget_set_margin_start(status, 6);
        gtk_widget_set_margin_end(status, 6);
        gtk_box_append(GTK_BOX(root), status);

        g_signal_connect(glArea, "realize", G_CALLBACK(realizeThunk), this);
        g_signal_connect(glArea, "unrealize", G_CALLBACK(unrealizeThunk), this);
        g_signal_connect(glArea, "render", G_CALLBACK(renderThunk), this);
        g_signal_connect(glArea, "resize", G_CALLBACK(resizeThunk), this);

        GtkGesture* click = gtk_gesture_click_new();
        gtk_gesture_single_set_button(GTK_GESTURE_SINGLE(click), 0);
        g_signal_connect(click, "pressed", G_CALLBACK(pressedThunk), this);
        g_signal_connect(click, "released", G_CALLBACK(releasedThunk), this);
        gtk_widget_add_controller(glArea, GTK_EVENT_CONTROLLER(click));

        GtkEventController* motion = gtk_event_controller_motion_new();
        g_signal_connect(motion, "motion", G_CALLBACK(motionThunk), this);
        g_signal_connect(motion, "enter", G_CALLBACK(enterThunk), this);
        gtk_widget_add_controller(glArea, motion);

        GtkEventController* scroll = gtk_event_controller_scroll_new(
            GTK_EVENT_CONTROLLER_SCROLL_VERTICAL);
        g_signal_connect(scroll, "scroll", G_CALLBACK(scrollThunk), this);
        gtk_widget_add_controller(glArea, scroll);

        GtkEventController* keys = gtk_event_controller_key_new();
        g_signal_connect(keys, "key-pressed", G_CALLBACK(keyPressedThunk), this);
        g_signal_connect(keys, "key-released", G_CALLBACK(keyReleasedThunk), this);
        gtk_widget_add_controller(glArea, keys);

        gtk_window_present(GTK_WINDOW(window));
        g_idle_add(initialSplitPositionThunk, this);
    }

private:
    void loadGuiDefinition()
    {
        std::string path = GuiState::languageFile(guiLanguage.c_str()).c_str();
        std::ifstream input(path.c_str());
        if ( !input ) {
            throw VSDKFatalException(
                java::String(("Could not open Java GUI definition: " + path).c_str()));
        }
        std::string json((std::istreambuf_iterator<char>(input)),
                         std::istreambuf_iterator<char>());
        sceneBridge.getGuiState()->setLanguageGuiFile(path.c_str());
        sceneBridge.setGuiDefinition(json);
    }

    static std::string selectedGuiLanguage()
    {
        const char* selected = std::getenv("SCENE_EDITOR_GUI_LANGUAGE");
        std::string requested =
            selected != nullptr && selected[0] != '\0' ? selected : "english";
        if ( isKnownLanguageId(requested) ) return requested;
        if ( isKnownLanguageId("english") ) return "english";

        java::ArrayList<java::String> languages = GuiState::listLanguages();
        return languages.size() > 0 ? languages.get(0).c_str() : "english";
    }

    static bool isKnownLanguageId(const std::string& language)
    {
        java::ArrayList<java::String> languages = GuiState::listLanguages();
        for ( long i = 0; i < languages.size(); i++ ) {
            if ( languages.get(i).equals(language.c_str()) ) return true;
        }
        return false;
    }

    std::string message(const char* id)
    {
        ApplicationModel* model = sceneBridge.getApplicationModel();
        Widget* context = model != nullptr ? model->getI18nContext() : nullptr;
        return context != nullptr ? context->getMessage(id).c_str() : id;
    }

    static std::string labelOf(const java::String& value, const char* fallback)
    {
        const char* text = value.c_str();
        return text != nullptr && text[0] != '\0' ? text : fallback;
    }

    static GtkWidget* createEllipsizedLabel(const char* text)
    {
        GtkWidget* label = gtk_label_new(text);
        gtk_label_set_ellipsize(GTK_LABEL(label), PANGO_ELLIPSIZE_END);
        gtk_label_set_xalign(GTK_LABEL(label), 0.5f);
        return label;
    }

    static GtkWidget* createTextButton(const char* text)
    {
        GtkWidget* button = gtk_button_new();
        gtk_button_set_child(GTK_BUTTON(button), createEllipsizedLabel(text));
        return button;
    }

    static bool isLiveWidget(GtkWidget* widget)
    {
        return widget != nullptr && GTK_IS_WIDGET(widget) &&
            gtk_widget_get_root(widget) != nullptr;
    }

    static GdkTexture* createTexture(RGBAImageUncompressed* image)
    {
        if ( image == nullptr ) return nullptr;
        int width = image->getXSize();
        int height = image->getYSize();
        char* pixels = image->getRawImage();
        if ( width <= 0 || height <= 0 || pixels == nullptr ) {
            delete[] pixels;
            return nullptr;
        }
        const int rowStride = width * 4;
        char* flipped = new char[rowStride * height];
        for ( int y = 0; y < height; y++ ) {
            std::memcpy(flipped + y * rowStride,
                        pixels + (height - 1 - y) * rowStride,
                        rowStride);
        }
        delete[] pixels;

        GBytes* bytes = g_bytes_new_take(flipped, rowStride * height);
        GdkTexture* texture = gdk_memory_texture_new(
            width, height, GDK_MEMORY_R8G8B8A8, bytes, rowStride);
        g_bytes_unref(bytes);
        return texture;
    }

    static GtkWidget* createIconButton(WidgetCommand* command,
                                       const std::string& fallbackLabel)
    {
        GdkTexture* texture = createTexture(command->getIcon());
        if ( texture == nullptr ) return createTextButton(fallbackLabel.c_str());

        GtkWidget* button = gtk_button_new();
        GtkWidget* picture = gtk_picture_new_for_paintable(GDK_PAINTABLE(texture));
        gtk_picture_set_can_shrink(GTK_PICTURE(picture), TRUE);
#if GTK_CHECK_VERSION(4, 8, 0)
        gtk_picture_set_content_fit(GTK_PICTURE(picture), GTK_CONTENT_FIT_CONTAIN);
#endif
        gtk_widget_set_size_request(picture, 24, 24);
        gtk_button_set_child(GTK_BUTTON(button), picture);
        gtk_widget_set_size_request(button, 42, 36);
        gtk_accessible_update_property(
            GTK_ACCESSIBLE(button),
            GTK_ACCESSIBLE_PROPERTY_LABEL, fallbackLabel.c_str(),
            -1);
        g_object_unref(texture);
        return button;
    }

    GtkWidget* createMenuBar()
    {
#ifdef __APPLE__
        if ( applicationMenu != nullptr ) {
            gtk_application_set_menubar(application,
                                       G_MENU_MODEL(applicationMenu));
            return nullptr;
        }
#endif
        if ( applicationMenu != nullptr ) {
            GtkWidget* bar =
                gtk_popover_menu_bar_new_from_model(G_MENU_MODEL(applicationMenu));
            gtk_widget_add_css_class(bar, "toolbar");
            return bar;
        }
        return createLegacyMenuBar();
    }

    void rebuildLocalizedGui()
    {
        // Its definition and messages belong to the replaced GUI context
        delete selectorDialog;
        selectorDialog = nullptr;
        if ( modifyUpdateSource != 0 ) {
            g_source_remove(modifyUpdateSource);
            modifyUpdateSource = 0;
        }
        sceneBridge.setBodyEditFeedbackProvider(nullptr);
        delete modifyPanel;
        modifyPanel = nullptr;

        if ( applicationMenu != nullptr ) {
            g_object_unref(applicationMenu);
            applicationMenu = nullptr;
        }
        applicationMenu = createApplicationMenu();

        if ( menuBar != nullptr ) {
            gtk_box_remove(GTK_BOX(root), menuBar);
            menuBar = nullptr;
        }
        menuBar = createMenuBar();
        if ( menuBar != nullptr ) {
            gtk_box_insert_child_after(GTK_BOX(root), menuBar, nullptr);
        }

        if ( globalBar != nullptr ) {
            gtk_box_remove(GTK_BOX(root), globalBar);
            globalBar = nullptr;
        }
        globalBar = createButtonGroup(sceneBridge.getButtonGroup("GLOBAL"), true);
        gtk_box_insert_child_after(GTK_BOX(root), globalBar, menuBar);

        sidePanel = createSidePanel();
        gtk_paned_set_end_child(GTK_PANED(chromeBox), sidePanel);
        gtk_paned_set_resize_end_child(GTK_PANED(chromeBox), FALSE);
        gtk_paned_set_shrink_end_child(GTK_PANED(chromeBox), TRUE);
        setInitialSplitPosition();

        if ( status != nullptr ) {
            gtk_label_set_text(GTK_LABEL(status), message("IDM_INTRO_MESSAGE").c_str());
        }
        repaintRequested();
    }

    void setupApplicationActions()
    {
        g_action_map_remove_action(G_ACTION_MAP(application), "command");
        GSimpleAction* action = g_simple_action_new(
            "command", G_VARIANT_TYPE_STRING);
        g_signal_connect(action, "activate",
                         G_CALLBACK(applicationCommandThunk), this);
        g_action_map_add_action(G_ACTION_MAP(application), G_ACTION(action));
        g_object_unref(action);
    }

    GMenu* createApplicationMenu()
    {
        WidgetMenu* menubar = sceneBridge.getMenubar();
        if ( menubar == nullptr ) return nullptr;

        GMenu* rootMenu = g_menu_new();
        bool hasItems = false;
        java::ArrayList<WidgetMenuElement*>& children = menubar->getChildren();
        for ( long i = 0; i < children.size(); i++ ) {
            WidgetMenu* menu = dynamic_cast<WidgetMenu*>(children.get(i));
            WidgetMenuItem* item = dynamic_cast<WidgetMenuItem*>(children.get(i));
            if ( menu != nullptr ) {
                GMenu* submenu = createGMenuForMenu(menu);
                if ( submenu != nullptr ) {
                    g_menu_append_submenu(rootMenu,
                        labelOf(menu->getName(), "Menu").c_str(),
                        G_MENU_MODEL(submenu));
                    g_object_unref(submenu);
                    hasItems = true;
                }
            }
            else if ( item != nullptr && !item->isSeparator() ) {
                appendGMenuItem(rootMenu, item);
                hasItems = true;
            }
        }
        if ( !hasItems ) {
            g_object_unref(rootMenu);
            return nullptr;
        }
        return rootMenu;
    }

    GMenu* createGMenuForMenu(WidgetMenu* menu)
    {
        GMenu* result = g_menu_new();
        GMenu* section = g_menu_new();
        bool hasAnyItem = false;
        bool sectionHasItems = false;

        java::ArrayList<WidgetMenuElement*>& children = menu->getChildren();
        for ( long i = 0; i < children.size(); i++ ) {
            WidgetMenuItem* item = dynamic_cast<WidgetMenuItem*>(children.get(i));
            WidgetMenu* submenu = dynamic_cast<WidgetMenu*>(children.get(i));
            if ( item != nullptr && item->isSeparator() ) {
                if ( sectionHasItems ) {
                    g_menu_append_section(result, nullptr, G_MENU_MODEL(section));
                    g_object_unref(section);
                    section = g_menu_new();
                    sectionHasItems = false;
                }
                continue;
            }
            if ( item != nullptr ) {
                appendGMenuItem(section, item);
                sectionHasItems = true;
                hasAnyItem = true;
            }
            else if ( submenu != nullptr ) {
                GMenu* child = createGMenuForMenu(submenu);
                if ( child != nullptr ) {
                    g_menu_append_submenu(section,
                        labelOf(submenu->getName(), "Menu").c_str(),
                        G_MENU_MODEL(child));
                    g_object_unref(child);
                    sectionHasItems = true;
                    hasAnyItem = true;
                }
            }
        }
        if ( sectionHasItems ) {
            g_menu_append_section(result, nullptr, G_MENU_MODEL(section));
        }
        g_object_unref(section);

        if ( !hasAnyItem ) {
            g_object_unref(result);
            return nullptr;
        }
        return result;
    }

    void appendGMenuItem(GMenu* menu, WidgetMenuItem* item)
    {
        std::string label = labelOf(item->getName(), "Command");
        std::string command = item->getCommandName().c_str();
        if ( command.empty() ) return;
        GMenuItem* menuItem = g_menu_item_new(label.c_str(), nullptr);
        g_menu_item_set_action_and_target(menuItem, "app.command", "s",
                                          command.c_str());
        g_menu_append_item(menu, menuItem);
        g_object_unref(menuItem);
    }

    GtkWidget* createLegacyMenuBar()
    {
        GtkWidget* bar = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 0);
        gtk_widget_add_css_class(bar, "toolbar");

        WidgetMenu* menubar = sceneBridge.getMenubar();
        if ( menubar == nullptr ) return bar;

        java::ArrayList<WidgetMenuElement*>& children = menubar->getChildren();
        for ( long i = 0; i < children.size(); i++ ) {
            WidgetMenu* menu = dynamic_cast<WidgetMenu*>(children.get(i));
            WidgetMenuItem* item = dynamic_cast<WidgetMenuItem*>(children.get(i));
            if ( menu != nullptr ) {
                GtkWidget* button = gtk_menu_button_new();
                gtk_menu_button_set_label(GTK_MENU_BUTTON(button),
                                          labelOf(menu->getName(), "Menu").c_str());
                GtkWidget* popover = gtk_popover_new();
                GtkWidget* box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
                gtk_popover_set_child(GTK_POPOVER(popover), box);
                appendMenuElements(box, menu);
                gtk_menu_button_set_popover(GTK_MENU_BUTTON(button), popover);
                gtk_box_append(GTK_BOX(bar), button);
            }
            else if ( item != nullptr && !item->isSeparator() ) {
                GtkWidget* button =
                    createTextButton(labelOf(item->getName(), "Command").c_str());
                bindCommand(button, item->getCommandName().c_str());
                gtk_box_append(GTK_BOX(bar), button);
            }
        }
        return bar;
    }

    void appendMenuElements(GtkWidget* box, WidgetMenu* menu)
    {
        java::ArrayList<WidgetMenuElement*>& children = menu->getChildren();
        for ( long i = 0; i < children.size(); i++ ) {
            WidgetMenuItem* item = dynamic_cast<WidgetMenuItem*>(children.get(i));
            WidgetMenu* submenu = dynamic_cast<WidgetMenu*>(children.get(i));
            if ( item != nullptr ) {
                if ( item->isSeparator() ) {
                    gtk_box_append(GTK_BOX(box),
                                   gtk_separator_new(GTK_ORIENTATION_HORIZONTAL));
                    continue;
                }
                GtkWidget* button =
                    createTextButton(labelOf(item->getName(), "Command").c_str());
                gtk_widget_set_halign(button, GTK_ALIGN_FILL);
                bindCommand(button, item->getCommandName().c_str());
                gtk_box_append(GTK_BOX(box), button);
            }
            else if ( submenu != nullptr ) {
                GtkWidget* expander =
                    gtk_expander_new(labelOf(submenu->getName(), "Menu").c_str());
                GtkWidget* childBox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
                gtk_expander_set_child(GTK_EXPANDER(expander), childBox);
                appendMenuElements(childBox, submenu);
                gtk_box_append(GTK_BOX(box), expander);
            }
        }
    }

    GtkWidget* createButtonGroup(WidgetButtonGroup* group, bool compact)
    {
        GtkOrientation orientation = GTK_ORIENTATION_VERTICAL;
        if ( group != nullptr && group->getDirection() == WidgetButtonGroup::HORIZONTAL ) {
            orientation = GTK_ORIENTATION_HORIZONTAL;
        }
        GtkWidget* box = gtk_box_new(orientation, compact ? 2 : 4);
        gtk_widget_set_margin_top(box, compact ? 2 : 6);
        gtk_widget_set_margin_bottom(box, compact ? 2 : 6);
        gtk_widget_set_margin_start(box, compact ? 2 : 6);
        gtk_widget_set_margin_end(box, compact ? 2 : 6);

        if ( group == nullptr ) {
            gtk_box_append(GTK_BOX(box), gtk_label_new("No commands"));
            return box;
        }

        java::ArrayList<WidgetCommand*>& commands = group->getCommands();
        for ( long i = 0; i < commands.size(); i++ ) {
            WidgetCommand* command = commands.get(i);
            if ( command == nullptr ) continue;
            std::string label = labelOf(command->getName(), command->getId().c_str());
            GtkWidget* button = compact ?
                createIconButton(command, label) : createTextButton(label.c_str());
            gtk_widget_set_hexpand(button, !compact);
            gtk_widget_set_halign(button, GTK_ALIGN_FILL);
            std::string tooltip = command->getBriefDescription().length() > 0 ?
                command->getBriefDescription().c_str() : label;
            gtk_widget_set_tooltip_text(button, tooltip.c_str());
            bindCommand(button, command->getId().c_str());
            gtk_box_append(GTK_BOX(box), button);
        }
        return box;
    }

    GtkWidget* createScrolledGroup(const char* groupName)
    {
        GtkWidget* scrolled = gtk_scrolled_window_new();
        gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scrolled),
                                       GTK_POLICY_NEVER, GTK_POLICY_AUTOMATIC);
        gtk_widget_set_vexpand(scrolled, TRUE);
        gtk_scrolled_window_set_child(
            GTK_SCROLLED_WINDOW(scrolled),
            createButtonGroup(sceneBridge.getButtonGroup(groupName), false));
        return scrolled;
    }

    /**
    Creates a section with a bold title that shows or hides its content when
    clicked, as `AwtCollapsibleSection`.
    @param titleId I18N message of the title of the section
    @param content widget shown or hidden by the section
    @return the section, starting expanded
    */
    GtkWidget* createCollapsibleSection(const char* titleId, GtkWidget* content)
    {
        gchar* escaped = g_markup_escape_text(message(titleId).c_str(), -1);
        gchar* markup = g_strdup_printf("<b>%s</b>", escaped);
        GtkWidget* section = gtk_expander_new(markup);
        g_free(markup);
        g_free(escaped);
        gtk_expander_set_use_markup(GTK_EXPANDER(section), TRUE);
        gtk_expander_set_expanded(GTK_EXPANDER(section), TRUE);
        gtk_widget_set_margin_top(section, 4);
        gtk_widget_set_margin_bottom(section, 4);
        gtk_expander_set_child(GTK_EXPANDER(section), content);
        return section;
    }

    /**
    Creates the creation page as `AwtButtonsPanel.addCreationSections`: two
    collapsible sections, the creation of geometries and the other creation
    and exchange operations.
    */
    GtkWidget* createScrolledCreationGroups()
    {
        GtkWidget* scrolled = gtk_scrolled_window_new();
        gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scrolled),
                                       GTK_POLICY_NEVER, GTK_POLICY_AUTOMATIC);
        gtk_widget_set_vexpand(scrolled, TRUE);

        GtkWidget* box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
        gtk_box_append(GTK_BOX(box), createCollapsibleSection(
            "IDM_CREATION_GEOMETRY_SECTION",
            createButtonGroup(sceneBridge.getButtonGroup("CREATION_GEOMETRY"), false)));
        gtk_box_append(GTK_BOX(box), createCollapsibleSection(
            "IDM_CREATION_OTHER_SECTION",
            createButtonGroup(sceneBridge.getButtonGroup("CREATION_OTHER"), false)));
        gtk_scrolled_window_set_child(GTK_SCROLLED_WINDOW(scrolled), box);
        return scrolled;
    }

    GtkWidget* createModifyPanel()
    {
        GtkWidget* scrolled = gtk_scrolled_window_new();
        gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scrolled),
                                       GTK_POLICY_NEVER, GTK_POLICY_AUTOMATIC);
        gtk_widget_set_vexpand(scrolled, TRUE);

        GtkWidget* box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 6);
        gtk_widget_set_margin_top(box, 8);
        gtk_widget_set_margin_bottom(box, 8);
        gtk_widget_set_margin_start(box, 8);
        gtk_widget_set_margin_end(box, 8);
        gtk_scrolled_window_set_child(GTK_SCROLLED_WINDOW(scrolled), box);

        modifyPanel = new Gtk4ModifyPanel(this, box);
        sceneBridge.setBodyEditFeedbackProvider(modifyPanel);
        sceneBridge.setModifyPanelSelected(false);
        return scrolled;
    }

    GtkWidget* createSidePanel()
    {
        GtkWidget* notebook = gtk_notebook_new();
        gtk_widget_set_size_request(notebook, MIN_SIDE_PANEL_WIDTH, -1);
        gtk_widget_set_hexpand(notebook, FALSE);
        gtk_widget_set_vexpand(notebook, TRUE);
        gtk_notebook_set_scrollable(GTK_NOTEBOOK(notebook), TRUE);

        const char* tabIds[] = {
            "IDM_CREATION_TAB", "IDM_MODIFY_TAB", "IDM_GUI_TAB",
            "IDM_OTHERS_TAB", "IDM_RENDER_TAB"
        };
        GtkWidget* pages[] = {
            createScrolledCreationGroups(),
            createModifyPanel(),
            createScrolledGroup("GUI"),
            createScrolledGroup("OTHER"),
            createScrolledGroup("RENDER")
        };
        for ( int i = 0; i < 5; i++ ) {
            gtk_notebook_append_page(GTK_NOTEBOOK(notebook), pages[i],
                                     createEllipsizedLabel(message(tabIds[i]).c_str()));
        }
        g_signal_connect(notebook, "switch-page",
                         G_CALLBACK(sidePanelPageChangedThunk), this);
        return notebook;
    }

    void updateModifyPanelTarget()
    {
        modifyUpdateSource = 0;
        if ( modifyPanel == nullptr ) return;
        SimpleBody* target = sceneBridge.getModifyPanelTarget();
        if ( target != nullptr ) {
            modifyPanel->notifyTargetBeginEdit(target);
        }
        else {
            modifyPanel->notifyTargetEndEdit();
        }
    }

    void scheduleModifyPanelUpdate()
    {
        if ( modifyUpdateSource != 0 ) return;
        modifyUpdateSource = g_idle_add(modifyPanelUpdateThunk, this);
    }

    void bindCommand(GtkWidget* widget, const char* command)
    {
        g_object_set_data_full(G_OBJECT(widget), "command",
                               g_strdup(command != nullptr ? command : ""), g_free);
        g_signal_connect(widget, "clicked", G_CALLBACK(guiCommandThunk), this);
    }

    void ensureReady()
    {
        if ( !glLoaded ) {
            glLoaded = OpenGL4Loader::load(&gtk4GetProcAddress);
            if ( !glLoaded ) {
                gtk_gl_area_set_error(GTK_GL_AREA(glArea),
                    g_error_new_literal(g_quark_from_static_string("vitral"),
                                        1, "OpenGL 4.1 core is required"));
                return;
            }
        }
        if ( !bridgeInitialized ) {
            sceneBridge.init();
            bridgeInitialized = true;
        }
    }

    void updateScreenResolutionForGtkArea()
    {
        if ( glArea == nullptr ) return;

        GtkNative* native = gtk_widget_get_native(glArea);
        GdkSurface* surface = native != nullptr ?
            gtk_native_get_surface(native) : nullptr;
        GdkDisplay* display = gtk_widget_get_display(glArea);
        GdkMonitor* monitor =
            surface != nullptr ? gdk_display_get_monitor_at_surface(display, surface)
                               : nullptr;
        if ( monitor != nullptr ) {
            GdkRectangle geometry;
            gdk_monitor_get_geometry(monitor, &geometry);
            int scale = gdk_monitor_get_scale_factor(monitor);
            sceneBridge.setScreenResolution(geometry.width * scale,
                                            geometry.height * scale);
            return;
        }

        int width = gtk_widget_get_width(glArea);
        int height = gtk_widget_get_height(glArea);
        int scale = gtk_widget_get_scale_factor(glArea);
        if ( width > 0 && height > 0 ) {
            sceneBridge.setScreenResolution(width * scale, height * scale);
        }
    }

    static GdkModifierType controllerState(GtkEventController* controller)
    {
        return gtk_event_controller_get_current_event_state(controller);
    }

    void setStatus(const std::string& message)
    {
        if ( status != nullptr ) gtk_label_set_text(GTK_LABEL(status), message.c_str());
    }

    void showViewportMenu(double x, double y)
    {
        if ( viewportMenu != nullptr ) {
            gtk_popover_popdown(GTK_POPOVER(viewportMenu));
            gtk_widget_unparent(viewportMenu);
            viewportMenu = nullptr;
        }
        viewportMenu = gtk_popover_new();
        gtk_widget_set_parent(viewportMenu, glArea);
        GtkWidget* box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
        gtk_popover_set_child(GTK_POPOVER(viewportMenu), box);

        std::vector<XtOpenGL4SceneBridge::ViewportMenuItem> items =
            sceneBridge.getViewportMenuItems();
        for ( size_t i = 0; i < items.size(); i++ ) {
            if ( items[i].separator ) {
                gtk_box_append(GTK_BOX(box), gtk_separator_new(GTK_ORIENTATION_HORIZONTAL));
                continue;
            }
            // As the menus of the other ports: flat entries, the projection
            // location and render modes in use checked
            GtkWidget* entry = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 6);
            GtkWidget* mark = gtk_label_new(items[i].current ? "\u2713" : "");
            gtk_widget_set_size_request(mark, 16, -1);
            gtk_box_append(GTK_BOX(entry), mark);
            GtkWidget* label = gtk_label_new(items[i].label.c_str());
            gtk_label_set_xalign(GTK_LABEL(label), 0.0f);
            gtk_box_append(GTK_BOX(entry), label);
            GtkWidget* button = gtk_button_new();
            gtk_button_set_child(GTK_BUTTON(button), entry);
            gtk_widget_add_css_class(button, "flat");
            g_object_set_data_full(G_OBJECT(button), "command",
                g_strdup(items[i].command.c_str()), g_free);
            g_signal_connect(button, "clicked", G_CALLBACK(menuCommandThunk), this);
            gtk_box_append(GTK_BOX(box), button);
        }

        GdkRectangle rect;
        rect.x = static_cast<int>(x);
        rect.y = static_cast<int>(y);
        rect.width = 1;
        rect.height = 1;
        gtk_popover_set_pointing_to(GTK_POPOVER(viewportMenu), &rect);
        gtk_popover_popup(GTK_POPOVER(viewportMenu));
    }

    void menuCommand(GtkWidget* widget)
    {
        const char* command =
            static_cast<const char*>(g_object_get_data(G_OBJECT(widget), "command"));
        if ( command != nullptr ) {
            executeCommand(command, true);
            gtk_widget_queue_draw(glArea);
        }
        if ( viewportMenu != nullptr ) gtk_popover_popdown(GTK_POPOVER(viewportMenu));
    }

    void executeCommand(const char* rawCommand, bool viewportCommand)
    {
        if ( rawCommand == nullptr || rawCommand[0] == '\0' ) return;
        std::string command(rawCommand);
        if ( viewportCommand ) {
            sceneBridge.executeViewportCommand(command.c_str());
        }
        else if ( isLanguageCommand(command) ) {
            if ( !setGuiLanguageByCommand(command) ) {
                setStatus(command + " is not an available GUI language");
            }
        }
        else if ( command == "IDC_FILE_QUIT" ) {
            closeRequested();
        }
        else if ( command == "IDC_IMPORT_OBJECTS_FROM_FILE" ) {
            showImportDialog();
            return;
        }
        else if ( command == "IDC_TOOLS_TANGIBLE_INTERFACE_CLIENT" ) {
            showTangibleInterfaceDialog();
            return;
        }
        else if ( command == "IDC_RENDERING_RAYTRACING" ) {
            raytracingRequested();
        }
        else {
            CommandResult result =
                sceneBridge.executeCommand(command);
            if ( result == CommandResult::NOT_HANDLED ) {
                setStatus(command + " is not implemented in the GTK4 example yet");
            }
        }
        if ( isLiveWidget(glArea) ) {
            gtk_widget_grab_focus(glArea);
            gtk_widget_queue_draw(glArea);
        }
    }

    void showImportDialog()
    {
        // GtkFileChooserNative gives GTK's platform-native dialog an actual
        // editor-window parent, equivalent to the JFrame parent supplied by
        // AWT/Swing.  This matters on compositing window managers too.
        GtkFileChooserNative* chooser = gtk_file_chooser_native_new(
            "Open", GTK_WINDOW(window), GTK_FILE_CHOOSER_ACTION_OPEN,
            "Open", "Cancel");
        // The filters of the scene.import dialog of etc/gui/dialogs.json
        Widget* context = i18nContext();
        const WidgetDialog* definition = context != nullptr ?
            context->getDialogById("scene.import") : nullptr;
        for ( size_t i = 0; definition != nullptr &&
                            i < definition->getElements().size(); i++ ) {
            const WidgetDialogElement& chooserElement = definition->getElements()[i];
            if ( !chooserElement.getKind().equals("fileChooser") ) continue;
            for ( size_t j = 0; j < chooserElement.getChildren().size(); j++ ) {
                const WidgetDialogElement& element = chooserElement.getChildren()[j];
                if ( !element.getKind().equals("fileFilter") ) continue;
                DialogFileFilter policy(element.getBinding(), element.getLabelKey());
                GtkFileFilter* filter = gtk_file_filter_new();
                gtk_file_filter_set_name(filter, policy.description().c_str());
                gtk_file_filter_add_suffix(filter, policy.getSuffix().c_str());
                gtk_file_chooser_add_filter(GTK_FILE_CHOOSER(chooser), filter);
            }
        }
        g_signal_connect(chooser, "response", G_CALLBACK(importDialogResponseThunk), this);
        gtk_native_dialog_show(GTK_NATIVE_DIALOG(chooser));
    }

    void importDialogResponse(GtkNativeDialog* dialog, int response)
    {
        if ( response == GTK_RESPONSE_ACCEPT ) {
            GFile* selected = gtk_file_chooser_get_file(GTK_FILE_CHOOSER(dialog));
            char* path = selected != nullptr ? g_file_get_path(selected) : nullptr;
            if ( path != nullptr ) {
                try {
                    if ( !sceneBridge.getCommands()->importObjects(java::File(path)) ) {
                        setStatus("Failed to read file");
                    }
                    else {
                        size_t slash = std::string(path).rfind('/');
                        sceneBridge.getGuiState()->setReadFolder(
                            (slash == std::string::npos ? "." :
                             std::string(path).substr(0, slash)).c_str());
                        repaintRequested();
                    }
                }
                catch ( const std::exception& e ) { setStatus(e.what()); }
                g_free(path);
            }
            if ( selected != nullptr ) g_object_unref(selected);
        }
        g_object_unref(dialog);
    }

    //= Tangible interface ================================================

    Widget* i18nContext()
    {
        ApplicationModel* model = sceneBridge.getApplicationModel();
        return model != nullptr ? model->getI18nContext() : nullptr;
    }

    /**
    Shows the `tangible.connect` dialog of `etc/gui/dialogs.json`, built by
    `Gtk4DialogHost`, as `AwtJogl4GuiEventExecutor` does: it closes when
    the connection succeeds; otherwise its connect button turns red and
    asks to retry.
    */
    void showTangibleInterfaceDialog()
    {
        Widget* context = i18nContext();
        const WidgetDialog* definition = context != nullptr ?
            context->getDialogById("tangible.connect") : nullptr;
        if ( definition == nullptr ) {
            setStatus("Dialog not defined: tangible.connect");
            return;
        }
        if ( tangibleForm != nullptr ) tangibleForm->close();
        installTangibleDialogStyle();
        tangibleForm = Gtk4DialogHost::form(GTK_WINDOW(window), context,
            definition, [this](const std::string& action) {
                tangibleAction(action);
            });
        ++tangibleFormSerial;
        g_signal_connect(tangibleForm->dialog(), "destroy",
                         G_CALLBACK(tangibleDialogDestroyedThunk), this);
        gtk_editable_set_text(GTK_EDITABLE(tangibleForm->field("serviceUrl")),
                              tangibleConnection.getServiceUrl().c_str());
        tangibleForm->present();
    }

    static void installTangibleDialogStyle()
    {
        static bool installed = false;
        if ( installed ) return;
        installed = true;
        GtkCssProvider* provider = gtk_css_provider_new();
        gtk_css_provider_load_from_string(provider,
            "button.tangible-retry { background: #ff0000; color: #ffffff; }");
        gtk_style_context_add_provider_for_display(gdk_display_get_default(),
            GTK_STYLE_PROVIDER(provider),
            GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
        g_object_unref(provider);
    }

    void tangibleAction(const std::string& action)
    {
        Gtk4DialogHost::Form* form = tangibleForm;
        if ( form == nullptr ) return;
        if ( action == "cancel" ) {
            form->close();
            return;
        }
        if ( action != "connect" ) return;
        std::string serviceUrl = gtk_editable_get_text(
            GTK_EDITABLE(form->field("serviceUrl")));
        size_t start = serviceUrl.find_first_not_of(" \t");
        serviceUrl = start == std::string::npos ? std::string() :
            serviceUrl.substr(start, serviceUrl.find_last_not_of(" \t") - start + 1);
        form->session()->setDraft("serviceUrl", serviceUrl.c_str());
        if ( serviceUrl.empty() ) return;
        long request = form->session()->beginAction();
        if ( request < 0 ) return;

        GtkWidget* button = form->action("connect");
        gtk_widget_remove_css_class(button, "tangible-retry");
        gtk_button_set_label(GTK_BUTTON(button),
                             message("IDM_TANGIBLE_INTERFACE_CONNECT").c_str());
        long serial = tangibleFormSerial;
        tangibleConnection.connect(serviceUrl, [this, serial, request](bool connected) {
            // From the thread of the network client
            TangibleConnectionResult* result = new TangibleConnectionResult;
            result->editor = this;
            result->serial = serial;
            result->request = request;
            result->connected = connected;
            g_idle_add(tangibleConnectionFinishedThunk, result);
        });
    }

    void tangibleConnectionFinished(long serial, long request, bool connected)
    {
        if ( tangibleForm == nullptr || serial != tangibleFormSerial ||
             !tangibleForm->session()->completeAction(request) ) {
            return;
        }
        if ( connected ) {
            tangibleForm->close();
            return;
        }
        GtkWidget* button = tangibleForm->action("connect");
        gtk_widget_add_css_class(button, "tangible-retry");
        gtk_button_set_label(GTK_BUTTON(button),
            message("IDM_TANGIBLE_INTERFACE_COULD_NOT_CONNECT_RETRY").c_str());
    }

    struct TangibleConnectionResult {
        Gtk4SceneEditor* editor;
        long serial;
        long request;
        bool connected;
    };

    static void tangibleDialogDestroyedThunk(GtkWidget* dialog, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        if ( g_object_get_data(G_OBJECT(dialog), "vitral-dialog-form") ==
             self->tangibleForm ) {
            self->tangibleForm = nullptr;
        }
    }

    static gboolean tangibleConnectionFinishedThunk(gpointer data)
    {
        TangibleConnectionResult* result =
            static_cast<TangibleConnectionResult*>(data);
        result->editor->tangibleConnectionFinished(
            result->serial, result->request, result->connected);
        delete result;
        return G_SOURCE_REMOVE;
    }

    static gboolean tangibleInterfaceEventThunk(gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        if ( self->tangibleConnection.applyPendingEvent(
                 self->sceneBridge.getApplicationModel()) ) {
            self->repaintRequested();
        }
        return G_SOURCE_REMOVE;
    }

    static bool isLanguageCommand(const std::string& command)
    {
        return command.find("IDC_CUSTOMIZE_LANGUAGE_") == 0;
    }

    static std::string languageFromCommand(const std::string& command)
    {
        std::string language = command.substr(strlen("IDC_CUSTOMIZE_LANGUAGE_"));
        std::transform(language.begin(), language.end(), language.begin(),
            [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
        return language;
    }

    bool isKnownLanguage(const std::string& language)
    {
        return isKnownLanguageId(language);
    }

    bool setGuiLanguageByCommand(const std::string& command)
    {
        return setGuiLanguage(languageFromCommand(command));
    }

    bool setGuiLanguage(const std::string& language)
    {
        if ( language == guiLanguage ) return true;
        if ( !isKnownLanguage(language) ) return false;

        guiLanguage = language;
        loadGuiDefinition();
        rebuildLocalizedGui();
        setStatus(message("IDM_INTRO_MESSAGE"));
        return true;
    }

    void guiCommand(GtkWidget* widget)
    {
        const char* command =
            static_cast<const char*>(g_object_get_data(G_OBJECT(widget), "command"));
        executeCommand(command, false);
    }

    void setInitialSplitPosition()
    {
        if ( chromeBox == nullptr ) return;
        int totalWidth = gtk_widget_get_width(chromeBox);
        if ( totalWidth <= 0 && window != nullptr ) {
            totalWidth = gtk_widget_get_width(window);
        }
        if ( totalWidth <= INITIAL_SIDE_PANEL_WIDTH ) return;
        gtk_paned_set_position(GTK_PANED(chromeBox),
                               totalWidth - INITIAL_SIDE_PANEL_WIDTH);
    }

    static gboolean initialSplitPositionThunk(gpointer data)
    {
        static_cast<Gtk4SceneEditor*>(data)->setInitialSplitPosition();
        return G_SOURCE_REMOVE;
    }

    static gboolean modifyPanelUpdateThunk(gpointer data)
    {
        static_cast<Gtk4SceneEditor*>(data)->updateModifyPanelTarget();
        return G_SOURCE_REMOVE;
    }

    static void sidePanelPageChangedThunk(GtkNotebook*, GtkWidget*, guint page,
                                          gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        bool selected = page == 1;
        self->sceneBridge.setModifyPanelSelected(selected);
        if ( selected ) self->scheduleModifyPanelUpdate();
        else if ( self->modifyPanel != nullptr ) self->modifyPanel->notifyTargetEndEdit();
    }

    void applicationCommand(GVariant* parameter)
    {
        const char* command = nullptr;
        if ( parameter != nullptr ) command = g_variant_get_string(parameter, nullptr);
        executeCommand(command, false);
    }

    static void realizeThunk(GtkGLArea* area, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        gtk_gl_area_make_current(area);
        self->ensureReady();
        self->updateScreenResolutionForGtkArea();
    }

    static void unrealizeThunk(GtkGLArea* area, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        gtk_gl_area_make_current(area);
        if ( self->bridgeInitialized ) {
            self->sceneBridge.dispose();
            self->bridgeInitialized = false;
        }
    }

    static gboolean renderThunk(GtkGLArea* area, GdkGLContext*, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        self->ensureReady();
        if ( self->bridgeInitialized ) {
            int canvasWidth = gtk_widget_get_width(GTK_WIDGET(area));
            int canvasHeight = gtk_widget_get_height(GTK_WIDGET(area));
            int scale = gtk_widget_get_scale_factor(GTK_WIDGET(area));
            self->updateScreenResolutionForGtkArea();
            self->sceneBridge.display(canvasWidth * scale,
                                      canvasHeight * scale,
                                      canvasWidth, canvasHeight);
        }
        return TRUE;
    }

    static void resizeThunk(GtkGLArea*, gint width, gint height, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        int scale = gtk_widget_get_scale_factor(self->glArea);
        self->updateScreenResolutionForGtkArea();
        self->sceneBridge.setCanvasSize(width, height);
        self->sceneBridge.reshape(width * scale, height * scale);
    }

    static void pressedThunk(GtkGestureClick* gesture, gint nPress,
                             gdouble x, gdouble y, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        gtk_widget_grab_focus(self->glArea);
        guint button = gtk_gesture_single_get_current_button(
            GTK_GESTURE_SINGLE(gesture));
        MouseEvent event = Gtk4System::mouseEvent(
            x, y, button,
            controllerState(GTK_EVENT_CONTROLLER(gesture)));
        event.setClicks(nPress);
        self->buttonDown = true;
        self->pressMoved = false;
        self->pressX = x;
        self->pressY = y;
        self->pressCount = nPress;
        self->sceneBridge.mousePressed(event);
        gtk_widget_queue_draw(self->glArea);
    }

    static void releasedThunk(GtkGestureClick* gesture, gint, gdouble x,
                              gdouble y, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        guint button = gtk_gesture_single_get_current_button(
            GTK_GESTURE_SINGLE(gesture));
        MouseEvent event = Gtk4System::mouseEvent(
            x, y, button,
            controllerState(GTK_EVENT_CONTROLLER(gesture)));
        self->buttonDown = false;
        // The release may request the viewport menu. The click comes after
        // it, as in AWT and the Xt ports: a click before the release would
        // disarm the press on the title of the viewport
        self->sceneBridge.mouseReleased(event);
        if ( !self->pressMoved ) {
            event.setClicks(self->pressCount);
            self->sceneBridge.mouseClicked(event);
        }
        gtk_widget_queue_draw(self->glArea);
    }

    static void motionThunk(GtkEventControllerMotion* motion, gdouble x, gdouble y,
                            gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        MouseEvent event = Gtk4System::mouseEvent(
            x, y, 0, controllerState(GTK_EVENT_CONTROLLER(motion)));
        if ( self->buttonDown ) {
            if ( x != self->pressX || y != self->pressY ) self->pressMoved = true;
            self->sceneBridge.mouseDragged(event);
        }
        else self->sceneBridge.mouseMoved(event);
        gtk_widget_queue_draw(self->glArea);
    }

    static void enterThunk(GtkEventControllerMotion* motion, gdouble x, gdouble y,
                           gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        self->sceneBridge.mouseEntered(
            Gtk4System::mouseEvent(
                x, y, 0, controllerState(GTK_EVENT_CONTROLLER(motion))));
    }

    static gboolean scrollThunk(GtkEventControllerScroll* scroll, gdouble,
                                gdouble dy, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        MouseEvent event = Gtk4System::mouseEvent(0, 0, dy < 0 ? 4 : 5,
            controllerState(GTK_EVENT_CONTROLLER(scroll)));
        self->sceneBridge.mouseWheel(event);
        gtk_widget_queue_draw(self->glArea);
        return TRUE;
    }

    static gboolean keyPressedThunk(GtkEventControllerKey*, guint keyval,
                                    guint, GdkModifierType state, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        self->sceneBridge.keyPressed(Gtk4System::keyEvent(keyval, state));
        gtk_widget_queue_draw(self->glArea);
        return TRUE;
    }

    static void keyReleasedThunk(GtkEventControllerKey*, guint keyval,
                                 guint, GdkModifierType state, gpointer data)
    {
        Gtk4SceneEditor* self = static_cast<Gtk4SceneEditor*>(data);
        self->sceneBridge.keyReleased(Gtk4System::keyEvent(keyval, state));
    }

    static void menuCommandThunk(GtkWidget* widget, gpointer data)
    {
        static_cast<Gtk4SceneEditor*>(data)->menuCommand(widget);
    }

    static void guiCommandThunk(GtkWidget* widget, gpointer data)
    {
        static_cast<Gtk4SceneEditor*>(data)->guiCommand(widget);
    }

    static void applicationCommandThunk(GSimpleAction*, GVariant* parameter,
                                        gpointer data)
    {
        static_cast<Gtk4SceneEditor*>(data)->applicationCommand(parameter);
    }

    static void importDialogResponseThunk(GtkNativeDialog* dialog, int response,
                                          gpointer data)
    {
        static_cast<Gtk4SceneEditor*>(data)->importDialogResponse(dialog, response);
    }

    void repaintRequested() override
    {
        if ( isLiveWidget(glArea) ) gtk_widget_queue_draw(glArea);
    }

    void cursorRequested(PointerCursor::Value) override
    {
    }

    void cursorWarpRequested(int, int) override
    {
    }

    void statusMessageRequested(const std::string& message) override
    {
        setStatus(message);
    }

    void closeRequested() override
    {
        if ( window != nullptr ) gtk_window_close(GTK_WINDOW(window));
    }

    void viewportMenuRequested(int canvasX, int canvasY) override
    {
        showViewportMenu(canvasX, canvasY);
    }

    void selectionChanged() override
    {
        scheduleModifyPanelUpdate();
    }

    virtual void repaintRequestedFromModifyPanel() override
    {
        repaintRequested();
    }

    void fullScreenGuiToggleRequested() override
    {
        sceneBridge.getGuiState()->setFullScreenGuiMode(
            !sceneBridge.getGuiState()->isFullScreenGuiMode());
        repaintRequested();
    }

    void raytracingRequested() override
    {
        sceneBridge.doRaytracingImage();
        setStatus("Raytracing finished");
        repaintRequested();
    }

    void selectorDialogRequested() override
    {
        try {
            if ( selectorDialog == nullptr ) {
                selectorDialog = new Gtk4SelectorDialog(GTK_WINDOW(window),
                    sceneBridge.getApplicationModel(),
                    [this]() { repaintRequested(); });
            }
            selectorDialog->rebuild();
            selectorDialog->setVisible(true);
        }
        catch ( const std::exception& e ) {
            setStatus(e.what());
        }
    }

    void imageRequested(RGBImageUncompressed*) override
    {
        setStatus("Image preview is not implemented in the GTK4 example yet");
    }
};

static void activate(GtkApplication* app, gpointer)
{
    try {
        Gtk4SceneEditor* editor = new Gtk4SceneEditor(app);
        g_object_set_data_full(G_OBJECT(app), "scene-editor", editor,
                               [](gpointer data) {
                                   delete static_cast<Gtk4SceneEditor*>(data);
                               });
        editor->show();
    }
    catch ( const std::exception& e ) {
        std::fprintf(stderr, "SceneEditorApplication GTK4: %s\n", e.what());
        g_application_quit(G_APPLICATION(app));
    }
}

}

int main(int argc, char** argv)
{
    // GApplication rejects the options it does not know
    std::vector<char*> gtkArguments;
    editorArguments.push_back(argv[0]);
    gtkArguments.push_back(argv[0]);
    for ( int i = 1; i < argc; i++ ) {
        if ( std::strcmp(argv[i], "-tangibleServer") == 0 && i + 1 < argc ) {
            editorArguments.push_back(argv[i]);
            editorArguments.push_back(argv[++i]);
        }
        else {
            gtkArguments.push_back(argv[i]);
        }
    }
    argc = static_cast<int>(gtkArguments.size());
    gtkArguments.push_back(nullptr);
    argv = gtkArguments.data();

    GtkApplication* app = gtk_application_new(
        "org.vitral.SceneEditorApplication.Gtk4", G_APPLICATION_FLAGS_NONE);
    g_signal_connect(app, "activate", G_CALLBACK(activate), nullptr);
    int status = g_application_run(G_APPLICATION(app), argc, argv);
    g_object_unref(app);
    return status;
}

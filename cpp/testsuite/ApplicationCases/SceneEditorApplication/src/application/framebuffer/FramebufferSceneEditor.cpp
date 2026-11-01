#include "java/util/TreeMap.txx"
#include "java/lang/String.h"
#include "application/framebuffer/FramebufferSceneEditor.h"

#include <cstdio>
#include "java/nio/file/Files.h"
#include <unistd.h>

#include "application/commands/GuiEventExecutor.h"
#include "gui/DrawingAreaInteractionTechniques.h"
#include "io/GuiI18nContextBuilder.h"
#include "java/io/File.h"
#include "java/io/FileReader.h"
#include "java/util/ArrayList.txx"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/GuiState.h"
#include "model/Scene.h"
#include "model/dialog/SceneHierarchyPresenter.h"
#include "model/selection/SceneSelectionEditor.h"
#include "render/framebuffer/FramebufferDrawingAreaRenderer.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/environment/background/FixedBackground.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/gui/dialog/components/DialogFileFilter.h"
#include "vsdk/toolkit/gui/dialog/components/DialogTreeNode.h"
#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferButton.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferDialogForm.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferEvent.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferFileDialog.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferImageView.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferInput.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferList.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferPainter.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetCommands.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionTechniques.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetButtonGroup.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetDialogElement.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"
#include "vsdk/toolkit/io/image/RGBColorPalettePersistence.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBAPixel.h"
#include "vsdk/toolkit/media/RGBColorPalette.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/processing/ImageProcessing.h"

namespace {

const char* const PALETTE_FILE = "../../../../etc/palettes/Cranes.gpl";
const char* const PALETTES_FOLDER = "../../../../etc/palettes";
const char* const SELECTOR_DIALOG = "scene.selector";
const char* const SELECTOR_TREE = "sceneHierarchy";
const char* const IMPORT_DIALOG = "scene.import";
// Characters in a line of the side panel.
const int SIDE_PANEL_COLUMNS = 32;

java::String parentFolderOf(const java::String& path)
{
    int separator = path.rfind('/');
    if ( separator < 0 ) {
        return ".";
    }
    return separator == 0 ? java::String("/") : path.substring(0, separator);
}

java::String currentFolder()
{
    char buffer[4096];
    return getcwd(buffer, sizeof(buffer)) != nullptr ? buffer : ".";
}

java::ArrayList<uint32_t> packedPixels(RGBImageUncompressed* image)
{
    int width = image->getXSize();
    int height = image->getYSize();
    java::ArrayList<uint32_t> pixels;
    pixels.reserve(static_cast<long>(width) * height);
    RGBPixel p;
    for ( int row = 0; row < height; row++ ) {
        for ( int column = 0; column < width; column++ ) {
            image->getPixelRgb(column, row, &p);
            pixels.add(
                static_cast<uint32_t>(static_cast<unsigned char>(p.r)) << 16 |
                static_cast<uint32_t>(static_cast<unsigned char>(p.g)) << 8 |
                static_cast<unsigned char>(p.b));
        }
    }
    return pixels;
}

/**
The scene selector: the tree of the scene, whose selected rows are selected
in the scene (shown behind the dialog as they change), and buttons for the
delete and hide / show actions.
*/
class SelectorListener : public FramebufferDialogListener {
    FramebufferDialogListener& application;
    FramebufferDialogForm& form;
    SceneHierarchyPresenter& presenter;
    DialogTreeNode hierarchy;

public:
    SelectorListener(FramebufferDialogListener& owner,
                     FramebufferDialogForm& f, SceneHierarchyPresenter& p)
        : application(owner), form(f), presenter(p), hierarchy("Scene", nullptr)
    {
        rebuild();
    }

    void rebuild()
    {
        hierarchy = presenter.hierarchy();
        form.setTree(SELECTOR_TREE, hierarchy);
        FramebufferList* list = form.tree(SELECTOR_TREE);
        for ( int row = 0; row < list->count(); row++ ) {
            list->setSelected(row, presenter.isSelected(form.node(SELECTOR_TREE, row)));
        }
    }

    java::ArrayList<const DialogTreeNode*> selection() const
    {
        java::ArrayList<const DialogTreeNode*> nodes;
        FramebufferList* list = form.tree(SELECTOR_TREE);
        for ( int row = 0; row < list->count(); row++ ) {
            if ( list->isSelected(row) ) {
                nodes.add(form.node(SELECTOR_TREE, row));
            }
        }
        return nodes;
    }

    void redrawBehind() override { application.redrawBehind(); }

    bool controlResponded(FramebufferDialog& dialog,
                          FramebufferControl* control,
                          FramebufferResponse response) override
    {
        FramebufferList* list = form.tree(SELECTOR_TREE);
        if ( control == list ) {
            if ( response == FramebufferResponse::Changed ) {
                presenter.select(selection());
                dialog.refreshBehind();
            }
            return true;
        }
        java::String action = form.actionOf(control);
        if ( response != FramebufferResponse::Activated ) {
            return false;
        }
        if ( action.equals("deleteSelected") ) {
            presenter.deleteSelected();
        }
        else if ( action.equals("toggleVisibility") ) {
            java::ArrayList<SimpleBody*> bodies = presenter.selectedBodies(selection());
            presenter.setHidden(bodies, !presenter.anyHidden(bodies));
        }
        else {
            return false;
        }
        rebuild();
        dialog.refreshBehind();
        return true;
    }
};

}

FramebufferSceneEditor::FramebufferSceneEditor(Framebuffer& target,
    FramebufferInput& events, const java::String& language)
    : surface(target), input(events), theme(FramebufferTheme::forSurface(target)),
      model(new ApplicationModel()), techniques(nullptr), renderer(nullptr),
      executor(nullptr), modifyTab(-1), modifyTarget(nullptr),
      modifyPageBuilt(false), viewportMenu(nullptr), menuViewport(nullptr),
      viewportMenuPosted(false), popupPending(false), popupX(0), popupY(0),
      running(false), repaintPending(false), buttonDown(false),
      pressMoved(false), areaX(0), areaY(0), areaWidth(0), areaHeight(0),
      panelX(0), panelWidth(0),
      statusY(0), statusHeight(0)
{
    createModel();
    loadGuiDefinition(language);
    techniques = new DrawingAreaInteractionTechniques(model, this);
    techniques->setCursorWrapEnabled(true);
    techniques->getViewportSetTechniques()->setListener(this);
    DrawingAreaInteractionTechniques* t = techniques;
    renderer = new FramebufferDrawingAreaRenderer(model, this,
        [t](Viewport* viewport) { t->activateViewport(viewport); });
    executor = new GuiEventExecutor(model, this);

    // No GPU: the views are hidden line drawings or raytraced images.
    java::ArrayList<int> modes;
    modes.add(Viewport::RENDER_MODE_HIDDEN_LINES);
    modes.add(Viewport::RENDER_MODE_RAYTRACING);
    model->getDrawingArea()->getViewportSet()->setAvailableRenderModes(modes);
    status = message("IDM_INTRO_MESSAGE");
}

FramebufferSceneEditor::~FramebufferSceneEditor()
{
    deleteViewportMenu();
    delete executor;
    delete renderer;
    delete techniques;
    delete model;
}

//= Model and GUI definition ==============================================

/**
Initializes the model as `AwtJogl4SceneEditorApplication.createModel` does:
the scene, the raytraced image, the palette of depth maps and the visual
debug ray.
*/
void FramebufferSceneEditor::createModel()
{
    model->setScene(new Scene());
    model->setRaytracedImage(new RGBImageUncompressed());
    model->setRaytracedImageWidth(320);
    model->setRaytracedImageHeight(240);
    model->setPalette(nullptr);
    if ( !java::File(PALETTE_FILE).canRead() ) {
        throw VSDKFatalException(
            java::String("Can not read palette file ") + PALETTE_FILE);
    }
    java::FileReader paletteStream(PALETTE_FILE);
    model->setPalette(RGBColorPalettePersistence::importGimpPalette(paletteStream));
    Ray ray(Vector3Dd(0, -3, 0), Vector3Dd(0, 1, 0));
    model->setVisualDebugRay(&ray);
    model->setVisualDebugRayLevels(2);
    model->setWithVisualDebugRay(false);
}

bool FramebufferSceneEditor::loadGuiDefinition(const java::String& language)
{
    java::String path = GuiState::languageFile(language);
    java::String json;
    if ( !java::Files::readString(path, json) ) {
        throw VSDKFatalException(
            java::String("Could not open GUI definition: ") + path);
    }
    deleteViewportMenu();
    menuBar.setMenu(nullptr);
    // The model deletes the previous context, with its menus and commands.
    model->setI18nContext(GuiI18nContextBuilder::build(
        json, GuiState::APPLICATION_DATA_FOLDER));
    model->getGuiState()->setLanguageGuiFile(path);
    menuBar.setMenu(model->getI18nContext()->getMenubar());
    buildToolBar();
    buildSidePanel();
    return true;
}

void FramebufferSceneEditor::setGuiLanguage(const java::String& language)
{
    loadGuiDefinition(language);
    status = message("IDM_INTRO_MESSAGE");
    layout();
    redrawAll();
}

/**
The buttons of the GLOBAL group, with their icons.
*/
void FramebufferSceneEditor::buildToolBar()
{
    toolBar.clear();
    toolCommands.clear();
    WidgetButtonGroup* group = model->getI18nContext()->getButtonGroup("GLOBAL");
    if ( group == nullptr ) {
        return;
    }
    java::ArrayList<WidgetCommand*>& commands = group->getCommands();
    for ( long i = 0; i < commands.size(); i++ ) {
        WidgetCommand* command = commands.get(i);
        if ( command == nullptr ) {
            continue;
        }
        RGBAImageUncompressed* icon = command->getIcon();
        java::ArrayList<uint32_t> argb;
        int width = icon != nullptr ? icon->getXSize() : 0;
        int height = icon != nullptr ? icon->getYSize() : 0;
        if ( width > 0 && height > 0 ) {
            argb.reserve(static_cast<long>(width) * height);
            RGBAPixel p;
            for ( int y = 0; y < height; y++ ) {
                for ( int x = 0; x < width; x++ ) {
                    icon->getPixelRgba(x, y, &p);
                    argb.add(
                        static_cast<uint32_t>(static_cast<unsigned char>(p.a)) << 24 |
                        static_cast<uint32_t>(static_cast<unsigned char>(p.r)) << 16 |
                        static_cast<uint32_t>(static_cast<unsigned char>(p.g)) << 8 |
                        static_cast<unsigned char>(p.b));
                }
            }
        }
        toolBar.add(command->getName(), width, height,
                    argb.size() == 0 ? nullptr : argb.data());
        toolCommands.add(command);
    }
}

/**
The tabbed side panel, as `createRightPanel` of the Xt variants: the
creation page with its two sections (geometries; import, export, lights and
other elements), the modify page, and the pages of the GUI, OTHER and RENDER
button groups.
*/
void FramebufferSceneEditor::buildSidePanel()
{
    int keep = sidePanel.getSelectedTab();
    sidePanel.clear();
    panelCommands.clear();
    int creation = sidePanel.addTab(message("IDM_CREATION_TAB"));
    modifyTab = sidePanel.addTab(message("IDM_MODIFY_TAB"));
    int gui = sidePanel.addTab(message("IDM_GUI_TAB"));
    int others = sidePanel.addTab(message("IDM_OTHERS_TAB"));
    int render = sidePanel.addTab(message("IDM_RENDER_TAB"));

    sidePanel.addSection(creation, message("IDM_CREATION_GEOMETRY_SECTION"));
    addButtonGroup(creation, "CREATION_GEOMETRY");
    sidePanel.addSection(creation, message("IDM_CREATION_OTHER_SECTION"));
    addButtonGroup(creation, "CREATION_OTHER");
    addButtonGroup(gui, "GUI");
    addButtonGroup(others, "OTHER");
    addButtonGroup(render, "RENDER");
    sidePanel.selectTab(keep >= 0 ? keep : creation);
    modifyPageBuilt = false;
    updateModifyPage(true);
}

/**
Adds the buttons of a group of the GUI definition to a page of the side
panel; their tags are indexes in `panelCommands`.
*/
void FramebufferSceneEditor::addButtonGroup(int tab, const char* name)
{
    WidgetButtonGroup* group = model->getI18nContext()->getButtonGroup(name);
    if ( group == nullptr ) {
        sidePanel.addText(tab, "No ButtonGroup found in GUI");
        return;
    }
    java::ArrayList<WidgetCommand*>& commands = group->getCommands();
    for ( long i = 0; i < commands.size(); i++ ) {
        WidgetCommand* command = commands.get(i);
        if ( command == nullptr ) {
            continue;
        }
        sidePanel.addButton(tab, command->getName(), panelCommands.size());
        panelCommands.add(command);
    }
}

/**
The modify page shows the body to edit: the first selected one. It is
built again only when that body changes, or when forced.
*/
void FramebufferSceneEditor::updateModifyPage(bool force)
{
    if ( modifyTab < 0 ) {
        return;
    }
    SceneSelectionEditor selection(model->getScene());
    SimpleBody* target = selection.getFirstSelectedBody();
    java::String name = target != nullptr ? target->getName() : java::String("");
    if ( !force && modifyPageBuilt && target == modifyTarget &&
         name.equals(modifyTargetName) ) {
        return;
    }
    modifyTarget = target;
    modifyTargetName = name;
    modifyPageBuilt = true;
    sidePanel.clearPage(modifyTab);
    if ( target == nullptr ) {
        sidePanel.addText(modifyTab, "No body selected.\n\n"
                          "Select a body with the select\n"
                          "tool or the scene selector (h).");
    }
    else {
        sidePanel.addSection(modifyTab, name.isEmpty() ?
                             java::String("(unnamed body)") : name);
        sidePanel.addText(modifyTab, "Move, rotate and scale it with\n"
                          "the tools of the tool bar.");
        sidePanel.addButton(modifyTab, message("IDC_TOOLS_TRANSLATE"),
                            panelCommandIndex("IDC_TOOLS_TRANSLATE"));
        sidePanel.addButton(modifyTab, message("IDC_TOOLS_ROTATE"),
                            panelCommandIndex("IDC_TOOLS_ROTATE"));
        sidePanel.addButton(modifyTab, message("IDC_TOOLS_SCALE"),
                            panelCommandIndex("IDC_TOOLS_SCALE"));
    }
    if ( sidePanel.getSelectedTab() == modifyTab &&
         !model->getGuiState()->isFullScreenGuiMode() && areaHeight > 0 ) {
        sidePanel.layout(surface, theme, panelX, areaY, panelWidth, areaHeight);
        sidePanel.drawPage();
    }
}

/**
@return the tag of a command for a side panel button, adding it to
`panelCommands` when it is not there yet
*/
long FramebufferSceneEditor::panelCommandIndex(const char* id)
{
    for ( long i = 0; i < panelCommands.size(); i++ ) {
        if ( panelCommands.get(i)->getId().equals(id) ) {
            return i;
        }
    }
    WidgetCommand* command = model->getI18nContext()->getCommandByName(id);
    if ( command == nullptr ) {
        return -1;
    }
    panelCommands.add(command);
    return panelCommands.size() - 1;
}

java::String FramebufferSceneEditor::message(const char* id) const
{
    Widget* context = model->getI18nContext();
    if ( context == nullptr ) {
        return java::String(id);
    }
    java::String text = context->getMessage(id);
    if ( text.equals(id) ) {
        // Not a message: the name of a command (as in its menu item)
        WidgetCommand* command = context->getCommandByName(id);
        if ( command != nullptr ) {
            text = command->getName();
        }
    }
    return text;
}

//= Presentation ==========================================================

void FramebufferSceneEditor::layout()
{
    theme = FramebufferTheme::forSurface(surface);
    int width = surface.width();
    int top = 0;
    statusHeight = 0;
    if ( !model->getGuiState()->isFullScreenGuiMode() ) {
        top = menuBar.layout(surface, theme, 0, 0, width);
        top += toolBar.layout(surface, theme, 0, top, width);
        statusHeight = surface.fontHeight() + 2 * theme.shadowThickness + 8;
    }
    else {
        menuBar.deactivate();
    }
    viewportMenuPosted = false;
    areaX = 0;
    areaY = top;
    areaHeight = surface.height() - top - statusHeight;
    areaHeight = areaHeight < 1 ? 1 : areaHeight;
    panelWidth = 0;
    if ( !model->getGuiState()->isFullScreenGuiMode() ) {
        panelWidth = FramebufferSidePanel::preferredWidth(surface, theme,
                                                          SIDE_PANEL_COLUMNS);
        panelWidth = panelWidth > width / 2 ? width / 2 : panelWidth;
    }
    panelX = width - panelWidth;
    areaWidth = width - panelWidth;
    areaWidth = areaWidth < 1 ? 1 : areaWidth;
    if ( panelWidth > 0 ) {
        sidePanel.layout(surface, theme, panelX, areaY, panelWidth, areaHeight);
    }
    statusY = surface.height() - statusHeight;
    model->getDrawingArea()->updateCanvasSize(areaWidth, areaHeight);
}

void FramebufferSceneEditor::redrawAll()
{
    surface.beginDrawing();
    if ( !model->getGuiState()->isFullScreenGuiMode() ) {
        menuBar.draw();
        toolBar.draw();
        sidePanel.draw();
        drawStatus();
    }
    repaintPending = true;
    drawViewports();
    surface.endDrawing();
}

void FramebufferSceneEditor::drawStatus()
{
    if ( statusHeight <= 0 ) {
        return;
    }
    FramebufferPainter painter(surface, theme);
    int t = theme.shadowThickness;
    int width = surface.width();
    surface.beginDrawing();
    painter.panel(0, statusY, width, statusHeight, true, false);
    painter.bevel(t + 1, statusY + t + 1, width - 2 * t - 2,
                  statusHeight - 2 * t - 2, false, false);
    surface.setClip(2 * t + 2, statusY, width - 4 * t - 4, statusHeight);
    surface.text(2 * t + 2 + surface.fontWidth() / 2,
                 statusY + (statusHeight - surface.fontHeight()) / 2,
                 status.c_str(), theme.text);
    surface.resetClip();
    surface.endDrawing();
}

/**
Draws the viewports if a repaint is pending, unless a menu is posted over
them (it would be overwritten): then the repaint waits until it closes.
*/
void FramebufferSceneEditor::drawViewports()
{
    if ( !repaintPending || menuBar.isActive() ) {
        return;
    }
    repaintPending = false;
    renderer->display(surface, areaX, areaY, areaWidth, areaHeight);
    if ( popupPending ) {
        popupPending = false;
        postViewportMenu();
    }
}

void FramebufferSceneEditor::showStatus(const java::String& text)
{
    status = text;
    drawStatus();
}

void FramebufferSceneEditor::deleteViewportMenu()
{
    if ( viewportMenu == nullptr ) {
        return;
    }
    menuBar.deactivate();
    java::ArrayList<WidgetMenuElement*>& children = viewportMenu->getChildren();
    for ( long i = 0; i < children.size(); i++ ) {
        delete children.get(i);
    }
    delete viewportMenu;
    viewportMenu = nullptr;
}

/**
Posts the menu of the viewport whose title was clicked: its projection
locations and render modes, the current ones checked and the render modes
this GUI cannot draw grayed.
*/
void FramebufferSceneEditor::postViewportMenu()
{
    if ( menuViewport == nullptr || !isKnownViewport(menuViewport) ) {
        return;
    }
    deleteViewportMenu();
    Widget* context = model->getI18nContext();
    ViewportSet* set = model->getDrawingArea()->getViewportSet();
    viewportMenu = new WidgetMenu(context);
    const char* popups[2] = {ViewportSetCommands::POPUP_PROJECTION_LOCATION,
                             ViewportSetCommands::POPUP_RENDER_MODE};
    java::String current[2] = {menuViewport->getProjectionLocationCommand(),
                               menuViewport->getRenderModeCommand()};
    for ( int p = 0; p < 2; p++ ) {
        WidgetMenu* popup = context->getPopup(popups[p]);
        if ( popup == nullptr ) {
            continue;
        }
        if ( p > 0 && viewportMenu->getChildren().size() > 0 ) {
            WidgetMenuItem* separator = new WidgetMenuItem(context);
            separator->addModifier("SEPARATOR");
            viewportMenu->addChild(separator);
        }
        java::ArrayList<WidgetMenuElement*>& children = popup->getChildren();
        for ( long i = 0; i < children.size(); i++ ) {
            if ( children.get(i)->isMenu() ) {
                continue;
            }
            WidgetMenuItem* definition = static_cast<WidgetMenuItem*>(children.get(i));
            WidgetMenuItem* item = new WidgetMenuItem(context);
            if ( definition->isSeparator() ) {
                item->addModifier("SEPARATOR");
                viewportMenu->addChild(item);
                continue;
            }
            java::String command = definition->getCommandName();
            item->setName(definition->getName());
            item->setCommandName(command);
            item->addModifier(command.equals(current[p]) ? "CHECKED" : "UNCHEKED");
            if ( p == 1 ) {
                bool available = false;
                java::ArrayList<int> modes = set->getAvailableRenderModes();
                for ( long m = 0; m < modes.size(); m++ ) {
                    available = available ||
                        Viewport::commandForRenderMode(modes.get(m)).equals(command);
                }
                if ( !available ) {
                    item->addModifier("GRAYED");
                }
            }
            viewportMenu->addChild(item);
        }
    }
    menuBar.popup(viewportMenu, popupX, popupY);
    viewportMenuPosted = true;
}

//= Events ================================================================

int FramebufferSceneEditor::run()
{
    running = true;
    layout();
    redrawAll();
    FramebufferEvent event;
    while ( running && input.waitEvent(event) ) {
        handle(event);
        if ( !menuBar.isActive() ) {
            viewportMenuPosted = false;
            updateModifyPage(false);
        }
        drawViewports();
    }
    return 0;
}

void FramebufferSceneEditor::handle(const FramebufferEvent& event)
{
    bool fullScreen = model->getGuiState()->isFullScreenGuiMode();
    switch ( event.type ) {
      case FramebufferEvent::Type::Resized:
        layout();
        redrawAll();
        return;
      case FramebufferEvent::Type::KeyPressed:
        if ( !fullScreen && menuBar.keyPressed(event.key) ) {
            chosen(menuBar.takeChosen());
            return;
        }
        techniques->processKeyPressedEvent(event.key);
        return;
      case FramebufferEvent::Type::KeyReleased:
        if ( !menuBar.isActive() ) {
            techniques->processKeyReleasedEvent(event.key);
        }
        return;
      default:
        handleMouse(event);
        return;
    }
}

void FramebufferSceneEditor::handleMouse(const FramebufferEvent& event)
{
    bool fullScreen = model->getGuiState()->isFullScreenGuiMode();
    if ( !buttonDown && (menuBar.isActive() || !fullScreen) &&
         menuBar.mouseEvent(event) ) {
        chosen(menuBar.takeChosen());
        return;
    }
    if ( !buttonDown && !fullScreen && sidePanel.mouseEvent(event) ) {
        long tag = sidePanel.takeChosen();
        if ( sidePanel.takeTabChanged() ) {
            updateModifyPage(true);
        }
        if ( tag >= 0 && tag < panelCommands.size() ) {
            executeCommand(panelCommands.get(tag)->getId());
        }
        else if ( event.type == FramebufferEvent::Type::MouseMoved &&
                  sidePanel.getHovered() >= 0 ) {
            WidgetCommand* command = panelCommands.get(sidePanel.getHovered());
            java::String text = command->getBriefDescription();
            showStatus(text.isEmpty() ? command->getName() : text);
        }
        return;
    }
    if ( !buttonDown && !fullScreen && toolBar.mouseEvent(event) ) {
        int index = toolBar.takeChosen();
        if ( index >= 0 && index < static_cast<int>(toolCommands.size()) ) {
            executeCommand(toolCommands[index]->getId());
        }
        else if ( event.type == FramebufferEvent::Type::MouseMoved &&
                  toolBar.getHovered() >= 0 ) {
            WidgetCommand* command = toolCommands[toolBar.getHovered()];
            java::String text = command->getBriefDescription();
            showStatus(text.isEmpty() ? command->getName() : text);
        }
        return;
    }
    int x = event.mouse.getX();
    int y = event.mouse.getY();
    bool inside = x >= areaX && y >= areaY && x < areaX + areaWidth &&
        y < areaY + areaHeight;
    if ( !inside && !buttonDown ) {
        return;
    }
    // The drawing area takes canvas coordinates.
    MouseEvent canvas = event.mouse;
    canvas.setX(x - areaX);
    canvas.setY(y - areaY);
    int buttons = MouseEvent::BUTTON1_DOWN_MASK | MouseEvent::BUTTON2_DOWN_MASK |
        MouseEvent::BUTTON3_DOWN_MASK;
    switch ( event.type ) {
      case FramebufferEvent::Type::MousePressed:
        buttonDown = true;
        pressMoved = false;
        techniques->processMousePressedEvent(canvas);
        return;
      case FramebufferEvent::Type::MouseMoved:
        if ( event.mouse.getModifiers() & buttons ) {
            pressMoved = true;
            techniques->processMouseDraggedEvent(canvas);
        }
        else {
            techniques->updateModeCursor(canvas);
            techniques->processMouseMovedEvent(canvas);
        }
        return;
      case FramebufferEvent::Type::MouseReleased:
        techniques->processMouseReleasedEvent(canvas);
        // As in AWT, a press followed by any motion is not a click.
        if ( !pressMoved ) {
            techniques->processMouseClickedEvent(canvas);
        }
        buttonDown = (event.mouse.getModifiers() & buttons) != 0;
        return;
      case FramebufferEvent::Type::MouseWheel:
        techniques->processMouseWheelEvent(canvas);
        return;
      default:
        return;
    }
}

void FramebufferSceneEditor::chosen(WidgetMenuItem* item)
{
    if ( item == nullptr ) {
        return;
    }
    java::String command = item->getCommandName();
    if ( viewportMenuPosted ) {
        viewportMenuPosted = false;
        executeViewportCommand(command);
    }
    else {
        executeCommand(command);
    }
}

void FramebufferSceneEditor::executeCommand(const java::String& command)
{
    CommandResult result = executor->execute(command);
    if ( result == CommandResult::NOT_HANDLED && !executeGuiCommand(command) ) {
        showStatus(java::String("Not available in the framebuffer GUI: ") + command);
    }
    repaintPending = true;
}

/**
Executes the commands that need the GUI (as `XtOpenGL1GuiEventExecutor`).
@return false if the command is not one of them
*/
bool FramebufferSceneEditor::executeGuiCommand(const java::String& command)
{
    GuiState* guiState = model->getGuiState();
    java::String path;
    if ( command.equals("IDC_FILE_QUIT") ) {
        running = false;
    }
    else if ( command.equals("IDC_IMPORT_OBJECTS_FROM_FILE") ||
              command.equals("IDC_FILE_IMPORT") ) {
        if ( chooseFile("Open", false,
                        guiState->getReadFolder(), IMPORT_DIALOG, nullptr, path) ) {
            showStatus(java::String("Reading ") + path);
            if ( !executor->importObjects(java::File(path)) ) {
                showMessage("Import", java::String("Failed to read file\n") + path);
            }
            showStatus(message("IDM_INTRO_MESSAGE"));
            guiState->setReadFolder(parentFolderOf(path));
        }
    }
    else if ( command.equals("IDC_EXPORT_OBJECTS_TO_OBJ") ||
              command.equals("IDC_EXPORT_OBJECTS_TO_GTS") ||
              command.equals("IDC_EXPORT_OBJECTS_TO_VTK") ) {
        ExportFormat format = command.equals("IDC_EXPORT_OBJECTS_TO_OBJ") ?
            ExportFormat::OBJ : command.equals("IDC_EXPORT_OBJECTS_TO_GTS") ?
            ExportFormat::GTS : ExportFormat::VTK;
        if ( chooseFile("Save", true,
                        guiState->getWriteFolder(), nullptr, nullptr, path) ) {
            if ( !executor->exportObjects(java::File(path), format) ) {
                showMessage("Export", java::String("Failed to write file\n") + path);
            }
            guiState->setWriteFolder(parentFolderOf(path));
        }
    }
    else if ( command.equals("IDC_RENDERING_SELECTPALETTEDEPTH") ) {
        if ( chooseFile("Open", false,
                        currentFolder() + "/" + PALETTES_FOLDER, nullptr, "gpl",
                        path) &&
             !executor->loadPalette(java::File(path)) ) {
            showMessage("Palette", java::String("Failed to read file\n") + path);
        }
    }
    else if ( command.equals("IDC_RENDERING_RAYTRACING") ) {
        raytracingRequested();
    }
    else if ( command.equals("IDC_CUSTOMIZE_LAF_MOTIF") ||
              command.equals("IDC_CUSTOMIZE_LAF_JAVA") ||
              command.equals("IDC_CUSTOMIZE_LAF_GTK") ||
              command.equals("IDC_CUSTOMIZE_LAF_WINDOWS") ) {
        showMessage("Look and feel",
                    "Look and feel changes are not available: the framebuffer\n"
                    "GUI draws its own DECwindows style controls.");
    }
    else if ( command.equals("IDC_CUSTOMIZE_LANGUAGE_ENGLISH") ) {
        setGuiLanguage("english");
    }
    else if ( command.equals("IDC_CUSTOMIZE_LANGUAGE_SPANISH") ) {
        setGuiLanguage("spanish");
    }
    else if ( command.equals("IDC_TOOLS_TANGIBLE_INTERFACE_CLIENT") ) {
        showMessage(message("IDM_TANGIBLE_INTERFACE_TITLE"),
                    "The tangible interface client is not available yet\n"
                    "in the framebuffer GUI.");
    }
    else {
        return false;
    }
    return true;
}

bool FramebufferSceneEditor::isKnownViewport(Viewport* viewport) const
{
    const java::ArrayList<Viewport*>& viewports =
        model->getDrawingArea()->getViewportSet()->getViewports();
    for ( long i = 0; i < viewports.size(); i++ ) {
        if ( viewports[i] == viewport ) {
            return true;
        }
    }
    return false;
}

void FramebufferSceneEditor::executeViewportCommand(const java::String& command)
{
    if ( menuViewport == nullptr || !isKnownViewport(menuViewport) ) {
        return;
    }
    techniques->processViewportCommand(command, menuViewport);
    repaintPending = true;
}

//= Dialogs ===============================================================

/**
@param dialogId dialog definition whose file filters are offered, or null
@param suffix the only suffix offered, or null
*/
bool FramebufferSceneEditor::chooseFile(const java::String& title, bool save,
    const java::String& folder, const char* dialogId, const char* suffix,
    java::String& path)
{
    FramebufferFileDialog dialog(title, save);
    const WidgetDialog* definition = dialogId != nullptr ?
        model->getI18nContext()->getDialogById(dialogId) : nullptr;
    if ( definition != nullptr ) {
        for ( const WidgetDialogElement& element : definition->getElements() ) {
            if ( !element.getKind().equals("fileChooser") ) {
                continue;
            }
            // The label of a filter is its description, as in Java.
            for ( const WidgetDialogElement& filter : element.getChildren() ) {
                if ( filter.getKind().equals("fileFilter") ) {
                    dialog.addFilter(DialogFileFilter(filter.getBinding(),
                                                      filter.getLabelKey()));
                }
            }
        }
    }
    if ( suffix != nullptr ) {
        dialog.addFilter(DialogFileFilter(suffix, java::String(suffix) + " files"));
    }
    dialog.setFolder(folder.isEmpty() ? currentFolder() : folder);
    dialog.setLabels(save ? "Save" : "Open", "Cancel", "Folder:", "File name:",
                     "Filter:");
    bool accepted = dialog.run(surface, input, theme, this, path);
    // The menus and viewports behind the dialog may have changed.
    repaintPending = true;
    return accepted;
}

void FramebufferSceneEditor::showMessage(const java::String& title,
                                         const java::String& text)
{
    // The button is also the cancel one: only the end of the input returns
    // false.
    if ( !FramebufferDialog::message(surface, input, theme, this, title, text,
                                     "OK") ) {
        running = false;
    }
}

void FramebufferSceneEditor::showImage(const java::String& title,
                                       RGBImageUncompressed* image)
{
    if ( image == nullptr || image->getXSize() <= 0 || image->getYSize() <= 0 ) {
        return;
    }
    java::ArrayList<uint32_t> pixels = packedPixels(image);
    FramebufferDialog dialog(title);
    dialog.add(new FramebufferImageView(image->getXSize(), image->getYSize(),
                                        pixels.data()));
    dialog.addButton(new FramebufferButton("OK", "ok"), true, true);
    dialog.run(surface, input, theme, this);
}

void FramebufferSceneEditor::showSelectorDialog()
{
    const WidgetDialog* definition =
        model->getI18nContext()->getDialogById(SELECTOR_DIALOG);
    if ( definition == nullptr ) {
        showStatus("The scene selector dialog is not defined");
        return;
    }
    FramebufferDialogForm form(*model->getI18nContext(), *definition);
    if ( form.tree(SELECTOR_TREE) == nullptr ) {
        showStatus("The scene selector dialog has no tree");
        return;
    }
    form.getDialog().addButton(new FramebufferButton("OK", "close"), false, true);
    FramebufferSceneEditor* self = this;
    SceneHierarchyPresenter presenter(model, [self]() { self->repaintRequested(); });
    SelectorListener listener(*this, form, presenter);
    form.run(surface, input, theme, &listener);
    repaintPending = true;
}

/**
Sizes the raytraced image as the model requests, over the fixed background
if it is the selected one (as
`AwtJogl4SceneEditorApplication.prepareRaytracedImage`).
@return false if the requested size is empty
*/
bool FramebufferSceneEditor::prepareRaytracedImage()
{
    int width = model->getRaytracedImageWidth();
    int height = model->getRaytracedImageHeight();
    if ( width <= 0 || height <= 0 ) {
        return false;
    }
    RGBImageUncompressed* image = model->getRaytracedImage();
    if ( image == nullptr ) {
        image = new RGBImageUncompressed();
        model->setRaytracedImage(image);
    }
    if ( image->getXSize() != width || image->getYSize() != height ) {
        image->init(width, height);
    }
    Scene* scene = model->getScene();
    if ( scene->selectedBackground == 1 && scene->fixedBackground != nullptr ) {
        ImageProcessing::resize(scene->fixedBackground->getImage(), image);
    }
    return true;
}

void FramebufferSceneEditor::computeRaytracedImage()
{
    if ( prepareRaytracedImage() ) {
        model->getScene()->raytrace(model->getRaytracedImage());
    }
}

//= DrawingAreaHost =======================================================

bool FramebufferSceneEditor::isFullScreenGuiMode()
{
    return model->getGuiState()->isFullScreenGuiMode();
}

BodyEditFeedbackProvider* FramebufferSceneEditor::getBodyEditFeedbackProvider()
{
    return nullptr;
}

/**
Raytraces the view of a viewport in CPU render mode with the size the
renderer requested (see `Scene::raytraceViewport`).
*/
void FramebufferSceneEditor::raytraceImage()
{
    if ( !prepareRaytracedImage() ) {
        return;
    }
    int width = model->getRaytracedImageWidth();
    int height = model->getRaytracedImageHeight();
    ZBuffer* depth = model->getRaytracedDepth();
    if ( depth == nullptr || depth->getXSize() != width ||
         depth->getYSize() != height ) {
        depth = new ZBuffer(width, height);
        model->setRaytracedDepth(depth);
    }
    model->getScene()->raytraceViewport(model->getRaytracedImage(), depth);
}

void FramebufferSceneEditor::showImage(RGBImageUncompressed* image)
{
    showImage(java::String("Image"), image);
}

void FramebufferSceneEditor::showStatusMessage(const java::String& text)
{
    showStatus(text);
}

//= DrawingAreaInteractionListener ========================================

void FramebufferSceneEditor::cursorWarpRequested(int surfaceX, int surfaceY)
{
    DrawingArea* drawingArea = model->getDrawingArea();
    input.warpPointer(areaX + drawingArea->scaleXToCanvas(surfaceX),
                      areaY + drawingArea->scaleYToCanvas(surfaceY));
}

void FramebufferSceneEditor::repaintRequested()
{
    repaintPending = true;
}

void FramebufferSceneEditor::statusMessageRequested(const java::String& text)
{
    showStatus(text);
}

void FramebufferSceneEditor::selectionChanged()
{
    repaintPending = true;
}

void FramebufferSceneEditor::raytracingRequested()
{
    showStatus(message("IDM_COMPUTING_RAYTRACING"));
    computeRaytracedImage();
    showStatus(message("IDM_INTRO_MESSAGE"));
    showImage(message("IDC_RENDERING_RAYTRACING"), model->getRaytracedImage());
}

void FramebufferSceneEditor::selectorDialogRequested()
{
    showSelectorDialog();
}

void FramebufferSceneEditor::closeRequested()
{
    running = false;
}

void FramebufferSceneEditor::fullScreenGuiToggleRequested()
{
    model->getGuiState()->toggleFullScreenGuiMode();
    layout();
    redrawAll();
}

//= ViewportSetInteractionListener ========================================

/**
The title of a viewport was clicked: its menu is posted under the pointer
once the viewports are drawn again.
*/
void FramebufferSceneEditor::projectionLocationMenuRequested(
    Viewport* viewport, int x, int y)
{
    DrawingArea* drawingArea = model->getDrawingArea();
    menuViewport = viewport;
    popupX = areaX + drawingArea->scaleXToCanvas(x);
    popupY = areaY + drawingArea->scaleYToCanvas(y);
    popupPending = true;
    repaintPending = true;
}

//= FramebufferDialogListener =============================================

void FramebufferSceneEditor::redrawBehind()
{
    layout();
    redrawAll();
}

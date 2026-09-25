#include "java/util/ArrayList.txx"
#include "application/GuiEventExecutor.h"
#include "gui/DrawingAreaInteractionListener.h"
#include "gui/DrawingAreaInteractionTechniques.h"
#include "io/GuiI18nContextBuilder.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/GuiState.h"
#include "model/selection/SceneSelectionEditor.h"
#include "model/Scene.h"
#include "render/DrawingAreaHost.h"
#include "render/opengl4/OpenGL4DrawingAreaRenderer.h"
#include "render/opengl4/XtOpenGL4SceneBridge.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetCommands.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionListener.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionTechniques.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"
#include "vsdk/toolkit/environment/background/FixedBackground.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/processing/ImageProcessing.h"

class XtOpenGL4SceneBridge::Impl :
    public DrawingAreaHost,
    public DrawingAreaInteractionListener,
    public ViewportSetInteractionListener,
    public GuiEventExecutor::Presenter {
public:
    XtOpenGL4SceneBridge::Listener* listener;
    ApplicationModel* model;
    DrawingAreaInteractionTechniques* techniques;
    OpenGL4DrawingAreaRenderer* renderer;
    GuiEventExecutor* executor;
    /// Viewport whose title was clicked, target of the viewport menu
    Viewport* menuViewport;
    /// Finds the target of the modify panel
    SceneSelectionEditor* selectionEditor;
    /// Editor of the modify panel, or null
    BodyEditFeedbackProvider* editFeedbackProvider;

    Impl(OpenGL4LabelImageProvider* labels,
         XtOpenGL4SceneBridge::Listener* listener)
        : listener(listener), model(new ApplicationModel()),
          techniques(nullptr), renderer(nullptr), executor(nullptr),
          menuViewport(nullptr), selectionEditor(nullptr),
          editFeedbackProvider(nullptr)
    {
        model->setScene(new Scene());
        selectionEditor = new SceneSelectionEditor(model->getScene());
        techniques = new DrawingAreaInteractionTechniques(model, this);
        // While dragging a gizmo, the pointer wraps around its viewport
        techniques->setCursorWrapEnabled(true);
        techniques->getViewportSetTechniques()->setListener(this);

        // The renderer presents the same gizmos the techniques manipulate
        DrawingAreaInteractionTechniques* t = techniques;
        renderer = new OpenGL4DrawingAreaRenderer(
            model, this, labels,
            [t](Viewport* viewport) { t->activateViewport(viewport); },
            techniques->getTranslationGizmo(),
            techniques->getRotateGizmo(),
            techniques->getScaleGizmo());
        executor = new GuiEventExecutor(model, this);
    }

    ~Impl()
    {
        delete selectionEditor;
        delete executor;
        delete renderer;
        delete techniques;
        delete model;
    }

    bool isKnownViewport(Viewport* viewport) const
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

    void addMenuItems(std::vector<ViewportMenuItem>& items,
                      const char* popupName,
                      const java::String& currentCommand) const
    {
        Widget* context = model->getI18nContext();
        WidgetMenu* popup =
            context != nullptr ? context->getPopup(popupName) : nullptr;
        if ( popup == nullptr ) {
            return;
        }
        for ( long i = 0; i < popup->getChildren().size(); i++ ) {
            WidgetMenuItem* definition =
                dynamic_cast<WidgetMenuItem*>(popup->getChildren().get(i));
            if ( definition == nullptr ) {
                continue;
            }
            ViewportMenuItem item;
            item.separator = definition->isSeparator();
            item.current = false;
            if ( !item.separator ) {
                item.label = definition->getName().c_str();
                item.command = definition->getCommandName().c_str();
                item.current = definition->getCommandName().equals(
                    currentCommand);
            }
            items.push_back(item);
        }
    }

    //= DrawingAreaHost ===================================================
    void beforeFrame() override {}
    bool isFullScreenGuiMode() override { return false; }
    BodyEditFeedbackProvider* getBodyEditFeedbackProvider() override
    {
        return editFeedbackProvider;
    }
    /**
    Raytraces the view of a viewport in CPU render mode (see
    `Scene::raytraceViewport`, done by its `ParallelRaytracer`) with the
    size the renderer requested, exporting its depth too, so the grid and
    the gizmos are composited over it.
    */
    void raytraceImage() override
    {
        int width = model->getRaytracedImageWidth();
        int height = model->getRaytracedImageHeight();
        if ( width <= 0 || height <= 0 ) {
            return;
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
        if ( scene->selectedBackground == 1 &&
             scene->fixedBackground != nullptr ) {
            ImageProcessing::resize(scene->fixedBackground->getImage(), image);
        }

        ZBuffer* depth = model->getRaytracedDepth();
        if ( depth == nullptr || depth->getXSize() != width ||
             depth->getYSize() != height ) {
            depth = new ZBuffer(width, height);
            model->setRaytracedDepth(depth);
        }
        scene->raytraceViewport(image, depth);
    }
    void showImage(RGBImageUncompressed* image) override { delete image; }
    void showStatusMessage(const java::String& message) override
    {
        listener->statusMessageRequested(message.c_str());
    }

    //= DrawingAreaInteractionListener ====================================
    void cursorRequested(PointerCursor::Value cursor) override
    {
        listener->cursorRequested(cursor);
    }

    void cursorWarpRequested(int surfaceX, int surfaceY) override
    {
        DrawingArea* drawingArea = model->getDrawingArea();
        listener->cursorWarpRequested(drawingArea->scaleXToCanvas(surfaceX),
                                      drawingArea->scaleYToCanvas(surfaceY));
    }

    void repaintRequested() override { listener->repaintRequested(); }

    void statusMessageRequested(const java::String& message) override
    {
        listener->statusMessageRequested(message.c_str());
    }

    void selectionChanged() override { listener->selectionChanged(); }
    void raytracingRequested() override {}
    void selectorDialogRequested() override {}
    void closeRequested() override { listener->closeRequested(); }
    void fullScreenGuiToggleRequested() override {}

    //= ViewportSetInteractionListener ====================================
    void projectionLocationMenuRequested(Viewport* viewport,
                                         int x, int y) override
    {
        DrawingArea* drawingArea = model->getDrawingArea();
        menuViewport = viewport;
        listener->viewportMenuRequested(drawingArea->scaleXToCanvas(x),
                                        drawingArea->scaleYToCanvas(y));
    }
};

XtOpenGL4SceneBridge::XtOpenGL4SceneBridge(
    OpenGL4LabelImageProvider* labels, Listener* listener)
    : impl(new Impl(labels, listener))
{
}

XtOpenGL4SceneBridge::~XtOpenGL4SceneBridge() { delete impl; }

void XtOpenGL4SceneBridge::setGuiDefinition(const std::string& json)
{
    impl->model->setI18nContext(GuiI18nContextBuilder::build(json));
}

void XtOpenGL4SceneBridge::init() { impl->renderer->init(); }
void XtOpenGL4SceneBridge::dispose() { impl->renderer->dispose(); }

void XtOpenGL4SceneBridge::display(int w, int h)
{
    setCanvasSize(w, h);
    impl->renderer->display(w, h);
}

void XtOpenGL4SceneBridge::reshape(int w, int h)
{
    setCanvasSize(w, h);
    impl->renderer->reshape(w, h);
}

bool XtOpenGL4SceneBridge::executeCommand(const std::string& command)
{
    return impl->executor->execute(command.c_str()) ==
        GuiEventExecutor::CommandResult::DONE;
}

//= Interaction ===========================================================

void XtOpenGL4SceneBridge::setCanvasSize(int width, int height)
{
    impl->model->getDrawingArea()->updateCanvasSize(width, height);
}

void XtOpenGL4SceneBridge::mouseEntered(const MouseEvent& event)
{
    impl->techniques->processMouseEnteredEvent(event);
}

void XtOpenGL4SceneBridge::mousePressed(const MouseEvent& event)
{
    impl->techniques->processMousePressedEvent(event);
}

void XtOpenGL4SceneBridge::mouseReleased(const MouseEvent& event)
{
    impl->techniques->processMouseReleasedEvent(event);
}

void XtOpenGL4SceneBridge::mouseClicked(const MouseEvent& event)
{
    impl->techniques->processMouseClickedEvent(event);
}

void XtOpenGL4SceneBridge::mouseMoved(const MouseEvent& event)
{
    impl->techniques->updateModeCursor(event);
    impl->techniques->processMouseMovedEvent(event);
}

void XtOpenGL4SceneBridge::mouseDragged(const MouseEvent& event)
{
    impl->techniques->processMouseDraggedEvent(event);
}

void XtOpenGL4SceneBridge::mouseWheel(const MouseEvent& event)
{
    impl->techniques->processMouseWheelEvent(event);
}

void XtOpenGL4SceneBridge::keyPressed(const KeyEvent& event)
{
    impl->techniques->processKeyPressedEvent(event);
}

void XtOpenGL4SceneBridge::keyReleased(const KeyEvent& event)
{
    impl->techniques->processKeyReleasedEvent(event);
}

std::vector<XtOpenGL4SceneBridge::ViewportMenuItem>
XtOpenGL4SceneBridge::getViewportMenuItems() const
{
    std::vector<ViewportMenuItem> items;
    Viewport* viewport = impl->menuViewport;
    if ( viewport == nullptr || !impl->isKnownViewport(viewport) ) {
        return items;
    }
    impl->addMenuItems(items, ViewportSetCommands::POPUP_PROJECTION_LOCATION,
                       viewport->getProjectionLocationCommand());
    std::vector<ViewportMenuItem> renderModes;
    impl->addMenuItems(renderModes, ViewportSetCommands::POPUP_RENDER_MODE,
                       viewport->getRenderModeCommand());
    if ( !items.empty() && !renderModes.empty() ) {
        ViewportMenuItem separator;
        separator.separator = true;
        separator.current = false;
        items.push_back(separator);
    }
    items.insert(items.end(), renderModes.begin(), renderModes.end());
    return items;
}

void XtOpenGL4SceneBridge::executeViewportCommand(const std::string& command)
{
    Viewport* viewport = impl->menuViewport;
    if ( viewport == nullptr || !impl->isKnownViewport(viewport) ) {
        return;
    }
    impl->techniques->processViewportCommand(command.c_str(), viewport);
}

//= Modify panel ==========================================================

void XtOpenGL4SceneBridge::setModifyPanelSelected(bool selected)
{
    impl->model->getGuiState()->setModifyPanelSelected(selected);
}

SimpleBody* XtOpenGL4SceneBridge::getModifyPanelTarget()
{
    if ( !impl->model->getGuiState()->isModifyPanelSelected() ) {
        return nullptr;
    }
    return impl->selectionEditor->getFirstSelectedBody();
}

void XtOpenGL4SceneBridge::setBodyEditFeedbackProvider(
    BodyEditFeedbackProvider* provider)
{
    impl->editFeedbackProvider = provider;
}

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "application/commands/GuiEventExecutor.h"
#include "gui/DrawingAreaInteractionListener.h"
#include "gui/DrawingAreaInteractionTechniques.h"
#include "io/GuiI18nContextBuilder.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/GuiState.h"
#include "model/selection/SceneSelectionEditor.h"
#include "model/Scene.h"
#include "render/DrawingAreaHost.h"
#include "render/opengl1/OpenGL1DrawingAreaRenderer.h"
#include "render/opengl1/XtOpenGL1SceneBridge.h"
#include "render/xlib/XlibDrawingAreaRenderer.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetCommands.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionListener.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionTechniques.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetDialog.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"
#include "vsdk/toolkit/environment/background/FixedBackground.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/processing/ImageProcessing.h"
#include "java/io/File.h"
#include "java/io/FileReader.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/io/image/RGBColorPalettePersistence.h"
#include "vsdk/toolkit/media/RGBColorPalette.h"

static const char* const PALETTE_FILE = "../../../../etc/palettes/Cranes.gpl";

class XtOpenGL1SceneBridge::Impl :
    public DrawingAreaHost,
    public DrawingAreaInteractionListener,
    public ViewportSetInteractionListener,
    public Presenter {
public:
    /**
    Activates in the interaction techniques the viewport the renderers
    present as selected.
    */
    class ViewportActivator : public Consumer<Viewport*> {
    public:
        ViewportActivator() : techniques(NULL) {}
        virtual void accept(Viewport* viewport)
        {
            if ( techniques != NULL ) {
                techniques->activateViewport(viewport);
            }
        }
        DrawingAreaInteractionTechniques* techniques;
    };

    XtOpenGL1SceneBridge::Listener* listener;
    ApplicationModel* model;
    DrawingAreaInteractionTechniques* techniques;
    OpenGL1DrawingAreaRenderer* renderer;
    XlibDrawingAreaRenderer* xlibRenderer;
    GuiEventExecutor* executor;
    /// Viewport whose title was clicked, target of the viewport menu
    Viewport* menuViewport;
    /// Finds the target of the modify panel
    SceneSelectionEditor* selectionEditor;
    /// Editor of the modify panel, or null
    BodyEditFeedbackProvider* editFeedbackProvider;
    ViewportActivator viewportActivator;

    Impl(OpenGL1LabelImageProvider* labels,
         XtOpenGL1SceneBridge::Listener* listener)
        : listener(listener), model(new ApplicationModel()),
          techniques(NULL), renderer(NULL), xlibRenderer(NULL),
          executor(NULL),
          menuViewport(NULL), selectionEditor(NULL),
          editFeedbackProvider(NULL)
    {
        createModel();
        selectionEditor = new SceneSelectionEditor(model->getScene());
        techniques = new DrawingAreaInteractionTechniques(model, this);
        // While dragging a gizmo, the pointer wraps around its viewport
        techniques->setCursorWrapEnabled(TRUE);
        techniques->getViewportSetTechniques()->setListener(this);

        // The renderer presents the same gizmos the techniques manipulate
        viewportActivator.techniques = techniques;
        renderer = new OpenGL1DrawingAreaRenderer(
            model, this, labels, &viewportActivator,
            techniques->getTranslationGizmo(),
            techniques->getRotateGizmo(),
            techniques->getScaleGizmo());
        xlibRenderer = new XlibDrawingAreaRenderer(model, this,
            &viewportActivator);
        executor = new GuiEventExecutor(model, this);
    }

    ~Impl()
    {
        delete selectionEditor;
        delete executor;
        delete renderer;
        delete xlibRenderer;
        delete techniques;
        delete model;
    }

    void useXlibRendering(BOOLEAN useXlib)
    {
        ViewportSet* set = model->getDrawingArea()->getViewportSet();
        if (set->isRenderModeAvailable(Viewport::RENDER_MODE_HIDDEN_LINES)
            == useXlib) return;
        ArrayList<int> modes;
        modes.add(useXlib ? Viewport::RENDER_MODE_HIDDEN_LINES :
                            Viewport::RENDER_MODE_Z_BUFFER);
        modes.add(Viewport::RENDER_MODE_RAYTRACING);
        set->setAvailableRenderModes(modes);
    }

    /**
    Initializes the model as `AwtJogl4SceneEditorApplication.createModel`
    does: the scene, the raytraced image, the palette of depth maps and the
    visual debug ray.
    */
    void createModel()
    {
        model->setScene(new Scene());

        model->setRaytracedImage(new RGBImageUncompressed());
        model->setRaytracedImageWidth(320);
        model->setRaytracedImageHeight(240);

        model->setPalette(NULL);
        if ( !File(PALETTE_FILE).canRead() ) {
            throw VSDKFatalException(
                String("Can not read palette file ") + PALETTE_FILE);
        }
        FileReader paletteStream(PALETTE_FILE);
        model->setPalette(
            RGBColorPalettePersistence::importGimpPalette(paletteStream));

        Ray ray(Vector3Dd(0, -3, 0), Vector3Dd(0, 1, 0));
        model->setVisualDebugRay(&ray);
        model->setVisualDebugRayLevels(2);
        model->setWithVisualDebugRay(FALSE);
    }

    /**
    Sizes the raytraced image as the model requests, over the fixed
    background if it is the selected one (as
    `AwtJogl4SceneEditorApplication.prepareRaytracedImage`).
    @return false if the requested size is empty
    */
    BOOLEAN prepareRaytracedImage()
    {
        int width = model->getRaytracedImageWidth();
        int height = model->getRaytracedImageHeight();
        if ( width <= 0 || height <= 0 ) {
            return FALSE;
        }

        RGBImageUncompressed* image = model->getRaytracedImage();
        if ( image == NULL ) {
            image = new RGBImageUncompressed();
            model->setRaytracedImage(image);
        }
        if ( image->getXSize() != width || image->getYSize() != height ) {
            image->init(width, height);
        }
        Scene* scene = model->getScene();
        if ( scene->selectedBackground == 1 &&
             scene->fixedBackground != NULL ) {
            ImageProcessing::resize(scene->fixedBackground->getImage(), image);
        }
        return TRUE;
    }

    BOOLEAN isKnownViewport(Viewport* viewport) const
    {
        const ArrayList<Viewport*>& viewports =
            model->getDrawingArea()->getViewportSet()->getViewports();
        for ( long i = 0; i < viewports.size(); i++ ) {
            if ( viewports[i] == viewport ) {
                return TRUE;
            }
        }
        return FALSE;
    }

    void addMenuItems(ArrayList<ViewportMenuItem>& items,
                      const char* popupName,
                      const String& currentCommand) const
    {
        Widget* context = model->getI18nContext();
        WidgetMenu* popup =
            context != NULL ? context->getPopup(popupName) : NULL;
        if ( popup == NULL ) {
            return;
        }
        for ( long i = 0; i < popup->getChildren().size(); i++ ) {
            WidgetMenuItem* definition =
                popup->getChildren().get(i) != NULL &&
                popup->getChildren().get(i)->instanceOf(WidgetMenuItem::CLASS) ?
                (WidgetMenuItem*)popup->getChildren().get(i) : NULL;
            if ( definition == NULL ) {
                continue;
            }
            ViewportMenuItem item;
            item.separator = definition->isSeparator();
            item.current = FALSE;
            if ( !item.separator ) {
                item.label = definition->getName().c_str();
                item.command = definition->getCommandName().c_str();
                item.current = definition->getCommandName().equals(
                    currentCommand);
            }
            items.add(item);
        }
    }

    //= DrawingAreaHost ===================================================
    void beforeFrame() {}
    BOOLEAN isFullScreenGuiMode()
    {
        return model->getGuiState()->isFullScreenGuiMode();
    }
    BodyEditFeedbackProvider* getBodyEditFeedbackProvider()
    {
        return editFeedbackProvider;
    }
    /**
    Raytraces the view of a viewport in CPU render mode (see
    `Scene::raytraceViewport`, done by its `ParallelRaytracer`) with the
    size the renderer requested, exporting its depth too, so the grid and
    the gizmos are composited over it.
    */
    void raytraceImage()
    {
        if ( !prepareRaytracedImage() ) {
            return;
        }
        int width = model->getRaytracedImageWidth();
        int height = model->getRaytracedImageHeight();
        RGBImageUncompressed* image = model->getRaytracedImage();
        Scene* scene = model->getScene();

        ZBuffer* depth = model->getRaytracedDepth();
        if ( depth == NULL || depth->getXSize() != width ||
             depth->getYSize() != height ) {
            depth = new ZBuffer(width, height);
            model->setRaytracedDepth(depth);
        }
        scene->raytraceViewport(image, depth);
    }
    void showImage(RGBImageUncompressed* image)
    {
        listener->imageRequested(image);
    }
    void showStatusMessage(const String& message)
    {
        listener->statusMessageRequested(message.c_str());
    }

    //= DrawingAreaInteractionListener ====================================
    void cursorRequested(PointerCursor::Value cursor)
    {
        listener->cursorRequested(cursor);
    }

    void cursorWarpRequested(int surfaceX, int surfaceY)
    {
        DrawingArea* drawingArea = model->getDrawingArea();
        listener->cursorWarpRequested(drawingArea->scaleXToCanvas(surfaceX),
                                      drawingArea->scaleYToCanvas(surfaceY));
    }

    void repaintRequested() { listener->repaintRequested(); }

    void statusMessageRequested(const String& message)
    {
        listener->statusMessageRequested(message.c_str());
    }

    void selectionChanged() { listener->selectionChanged(); }
    void raytracingRequested() { listener->raytracingRequested(); }
    void selectorDialogRequested()
    {
        listener->selectorDialogRequested();
    }
    void closeRequested() { listener->closeRequested(); }
    void fullScreenGuiToggleRequested()
    {
        listener->fullScreenGuiToggleRequested();
    }

    //= ViewportSetInteractionListener ====================================
    void projectionLocationMenuRequested(Viewport* viewport,
                                         int x, int y)
    {
        DrawingArea* drawingArea = model->getDrawingArea();
        menuViewport = viewport;
        listener->viewportMenuRequested(drawingArea->scaleXToCanvas(x),
                                        drawingArea->scaleYToCanvas(y));
    }
};

XtOpenGL1SceneBridge::XtOpenGL1SceneBridge(
    OpenGL1LabelImageProvider* labels, Listener* listener)
    : impl(new Impl(labels, listener))
{
}

XtOpenGL1SceneBridge::~XtOpenGL1SceneBridge() { delete impl; }

void XtOpenGL1SceneBridge::setGuiDefinition(const String& json)
{
    impl->model->setI18nContext(GuiI18nContextBuilder::build(
        json, GuiState::APPLICATION_DATA_FOLDER));
}

WidgetMenu* XtOpenGL1SceneBridge::getMenubar()
{
    Widget* context = impl->model->getI18nContext();
    return context != NULL ? context->getMenubar() : NULL;
}

WidgetButtonGroup* XtOpenGL1SceneBridge::getButtonGroup(const char* name)
{
    Widget* context = impl->model->getI18nContext();
    return context != NULL ? context->getButtonGroup(name) : NULL;
}

WidgetDialog* XtOpenGL1SceneBridge::getDialogById(const char* id)
{
    Widget* context = impl->model->getI18nContext();
    return context != NULL ? context->getDialogById(id) : NULL;
}

void XtOpenGL1SceneBridge::init() { impl->renderer->init(); }
void XtOpenGL1SceneBridge::dispose() { impl->renderer->dispose(); }

void XtOpenGL1SceneBridge::display(int w, int h)
{
    setCanvasSize(w, h);
    impl->renderer->display(w, h);
}

void XtOpenGL1SceneBridge::displayXlib(Display* display, Window window,
    Visual* visual, int depth, int width, int height)
{
    setCanvasSize(width, height);
    impl->xlibRenderer->display(display, window, visual, depth, width, height);
}

BOOLEAN XtOpenGL1SceneBridge::usesXlibRendering() const
{
    return impl->model->getDrawingArea()->getViewportSet()->isRenderModeAvailable(
        Viewport::RENDER_MODE_HIDDEN_LINES);
}

void XtOpenGL1SceneBridge::reshape(int w, int h)
{
    setCanvasSize(w, h);
    impl->renderer->reshape(w, h);
}

ApplicationModel* XtOpenGL1SceneBridge::getApplicationModel()
{
    return impl->model;
}

GuiState* XtOpenGL1SceneBridge::getGuiState()
{
    return impl->model->getGuiState();
}

GuiEventExecutor* XtOpenGL1SceneBridge::getCommands()
{
    return impl->executor;
}

CommandResult XtOpenGL1SceneBridge::executeCommand(
    const String& command)
{
    return impl->executor->execute(command.c_str());
}

void XtOpenGL1SceneBridge::doRaytracingImage()
{
    if ( impl->prepareRaytracedImage() ) {
        impl->model->getScene()->raytrace(impl->model->getRaytracedImage());
    }
}

RGBImageUncompressed* XtOpenGL1SceneBridge::getRaytracedImage()
{
    return impl->model->getRaytracedImage();
}

void XtOpenGL1SceneBridge::requestViewportExport(const File& file,
                                                 BOOLEAN jpg)
{
    impl->model->getDrawingArea()->requestViewportExport(file, jpg);
}

void XtOpenGL1SceneBridge::requestWorkspaceExport(const File& file)
{
    impl->model->getDrawingArea()->requestWorkspaceExport(file);
}

BOOLEAN XtOpenGL1SceneBridge::projectToCanvas(Viewport* viewport,
                                           const Vector3Dd& point,
                                           double outCanvas[2])
{
    return impl->model->getDrawingArea()->projectToCanvas(viewport, point,
                                                          outCanvas);
}

void XtOpenGL1SceneBridge::setScreenResolution(int width, int height)
{
    ViewportElementScaler* scaler =
        impl->model->getDrawingArea()->getViewportSet()->getElementScaler();
    if ( scaler != NULL && (scaler->getScreenWidthInPixels() != width ||
                               scaler->getScreenHeightInPixels() != height) ) {
        scaler->setScreenResolution(width, height);
    }
}

//= Interaction ===========================================================

void XtOpenGL1SceneBridge::setCanvasSize(int width, int height)
{
    impl->model->getDrawingArea()->updateCanvasSize(width, height);
}

void XtOpenGL1SceneBridge::mouseEntered(const MouseEvent& event)
{
    impl->techniques->processMouseEnteredEvent(event);
}

void XtOpenGL1SceneBridge::mousePressed(const MouseEvent& event)
{
    impl->techniques->processMousePressedEvent(event);
}

void XtOpenGL1SceneBridge::mouseReleased(const MouseEvent& event)
{
    impl->techniques->processMouseReleasedEvent(event);
}

void XtOpenGL1SceneBridge::mouseClicked(const MouseEvent& event)
{
    impl->techniques->processMouseClickedEvent(event);
}

void XtOpenGL1SceneBridge::mouseMoved(const MouseEvent& event)
{
    impl->techniques->updateModeCursor(event);
    impl->techniques->processMouseMovedEvent(event);
}

void XtOpenGL1SceneBridge::mouseDragged(const MouseEvent& event)
{
    impl->techniques->processMouseDraggedEvent(event);
}

void XtOpenGL1SceneBridge::mouseWheel(const MouseEvent& event)
{
    impl->techniques->processMouseWheelEvent(event);
}

void XtOpenGL1SceneBridge::keyPressed(const KeyEvent& event)
{
    impl->techniques->processKeyPressedEvent(event);
}

void XtOpenGL1SceneBridge::keyReleased(const KeyEvent& event)
{
    impl->techniques->processKeyReleasedEvent(event);
}

ArrayList<XtOpenGL1SceneBridge::ViewportMenuItem>
XtOpenGL1SceneBridge::getViewportMenuItems() const
{
    ArrayList<ViewportMenuItem> items;
    Viewport* viewport = impl->menuViewport;
    if ( viewport == NULL || !impl->isKnownViewport(viewport) ) {
        return items;
    }
    impl->addMenuItems(items, ViewportSetCommands::POPUP_PROJECTION_LOCATION,
                       viewport->getProjectionLocationCommand());
    ArrayList<ViewportMenuItem> renderModes;
    impl->addMenuItems(renderModes, ViewportSetCommands::POPUP_RENDER_MODE,
                       viewport->getRenderModeCommand());
    if ( !items.isEmpty() && !renderModes.isEmpty() ) {
        ViewportMenuItem separator;
        separator.separator = TRUE;
        separator.current = FALSE;
        items.add(separator);
    }
    items.addAll(renderModes);
    return items;
}

void XtOpenGL1SceneBridge::executeViewportCommand(const String& command)
{
    Viewport* viewport = impl->menuViewport;
    if ( viewport == NULL || !impl->isKnownViewport(viewport) ) {
        return;
    }
    if (command == ViewportSetCommands::IDV_RENDER_MODE_CPU_HIDDEN_LINES)
        impl->useXlibRendering(TRUE);
    else if (command == ViewportSetCommands::IDV_RENDER_MODE_GPU)
        impl->useXlibRendering(FALSE);
    impl->techniques->processViewportCommand(command.c_str(), viewport);
}

//= Modify panel ==========================================================

void XtOpenGL1SceneBridge::setModifyPanelSelected(BOOLEAN selected)
{
    impl->model->getGuiState()->setModifyPanelSelected(selected);
}

SimpleBody* XtOpenGL1SceneBridge::getModifyPanelTarget()
{
    if ( !impl->model->getGuiState()->isModifyPanelSelected() ) {
        return NULL;
    }
    return impl->selectionEditor->getFirstSelectedBody();
}

void XtOpenGL1SceneBridge::setBodyEditFeedbackProvider(
    BodyEditFeedbackProvider* provider)
{
    impl->editFeedbackProvider = provider;
}

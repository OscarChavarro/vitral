#ifndef __FRAMEBUFFER_SCENE_EDITOR__
#define __FRAMEBUFFER_SCENE_EDITOR__

#include "application/commands/Presenter.h"
#include "gui/DrawingAreaInteractionListener.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "render/DrawingAreaHost.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferDialog.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferMenuBar.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferSidePanel.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferTheme.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferToolBar.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionListener.h"

class ApplicationModel;
class DrawingAreaInteractionTechniques;
class FramebufferDrawingAreaRenderer;
class FramebufferEvent;
class FramebufferInput;
class Framebuffer;
class GuiEventExecutor;
class RGBImageUncompressed;
class SimpleBody;
class Viewport;
class WidgetCommand;
class WidgetMenu;
class WidgetMenuItem;

/**
The scene editor on the framebuffer GUI of the toolkit
(`vsdk/toolkit/gui/framebuffer`): everything is drawn with the operations of
a `Framebuffer` (pixels, lines, rectangles, polygons, images and fixed width
text) and driven by the events of a `FramebufferInput`, so the same class
runs over any framebuffer backend: Xlib, svgalib, or a future BGI one. Only
its `main` knows the backend.

The surface shows the menu bar, the buttons of the `GLOBAL` group of the GUI
definition, the viewport set (drawn in CPU by
`FramebufferDrawingAreaRenderer`: hidden lines or raytracing, no OpenGL),
the tabbed side panel with the other button groups (creation of geometries
and lights, import and export, GUI, scene and render commands) and a status
line. Dialogs are modal `FramebufferDialog`s drawn on the same
surface: the file dialogs of import, export and palettes, the raytraced
image, messages and the scene selector (`h`), built from its definition in
`etc/gui/dialogs.json`. Clicking the title of a viewport posts its menu of
projections and render modes. The drawing area takes the keys and the mouse
as in every other port (`DrawingAreaInteractionTechniques`), except F10,
which posts the menus as in Motif.
*/
class FramebufferSceneEditor :
    private DrawingAreaHost,
    private DrawingAreaInteractionListener,
    private ViewportSetInteractionListener,
    private Presenter,
    private FramebufferDialogListener {
public:
    /**
    @param surface where the application is drawn
    @param input its events
    @param language name of the GUI definition in `etc/gui` (i.e. english)
    */
    FramebufferSceneEditor(Framebuffer& surface, FramebufferInput& input,
                           const java::String& language);
    ~FramebufferSceneEditor();

    /**
    Shows the application and handles the events until the user quits or
    the input ends.
    @return the exit status of the application
    */
    int run();

private:
    Framebuffer& surface;
    FramebufferInput& input;
    FramebufferTheme theme;
    ApplicationModel* model;
    DrawingAreaInteractionTechniques* techniques;
    FramebufferDrawingAreaRenderer* renderer;
    GuiEventExecutor* executor;
    FramebufferMenuBar menuBar;
    FramebufferToolBar toolBar;
    java::ArrayList<WidgetCommand*> toolCommands;
    FramebufferSidePanel sidePanel;
    // The commands of the side panel buttons, by their tags.
    java::ArrayList<WidgetCommand*> panelCommands;
    int modifyTab;
    // What the modify page shows, to draw it again only when it changes.
    SimpleBody* modifyTarget;
    java::String modifyTargetName;
    bool modifyPageBuilt;
    WidgetMenu* viewportMenu;
    Viewport* menuViewport;
    bool viewportMenuPosted;
    bool popupPending;
    int popupX;
    int popupY;
    java::String status;
    bool running;
    bool repaintPending;
    bool buttonDown;
    bool pressMoved;
    int areaX;
    int areaY;
    int areaWidth;
    int areaHeight;
    int panelX;
    int panelWidth;
    int statusY;
    int statusHeight;

    FramebufferSceneEditor(const FramebufferSceneEditor&);
    FramebufferSceneEditor& operator=(const FramebufferSceneEditor&);

    //= Model and GUI definition ==========================================
    void createModel();
    bool loadGuiDefinition(const java::String& language);
    void setGuiLanguage(const java::String& language);
    void buildToolBar();
    void buildSidePanel();
    void addButtonGroup(int tab, const char* group);
    void updateModifyPage(bool force);
    long panelCommandIndex(const char* id);
    java::String message(const char* id) const;

    //= Presentation ======================================================
    void layout();
    void redrawAll();
    void drawStatus();
    void drawViewports();
    void showStatus(const java::String& text);
    void postViewportMenu();
    void deleteViewportMenu();

    //= Events ============================================================
    void handle(const FramebufferEvent& event);
    void handleMouse(const FramebufferEvent& event);
    void chosen(WidgetMenuItem* item);
    void executeCommand(const java::String& command);
    bool executeGuiCommand(const java::String& command);
    void executeViewportCommand(const java::String& command);
    bool isKnownViewport(Viewport* viewport) const;

    //= Dialogs ===========================================================
    bool chooseFile(const java::String& title, bool save,
                    const java::String& folder, const char* dialogId,
                    const char* suffix, java::String& path);
    void showMessage(const java::String& title, const java::String& text);
    void showImage(const java::String& title, RGBImageUncompressed* image);
    void showSelectorDialog();
    bool prepareRaytracedImage();
    void computeRaytracedImage();

    //= DrawingAreaHost ===================================================
    void beforeFrame() override {}
    bool isFullScreenGuiMode() override;
    BodyEditFeedbackProvider* getBodyEditFeedbackProvider() override;
    void raytraceImage() override;
    void showImage(RGBImageUncompressed* image) override;
    void showStatusMessage(const java::String& message) override;

    //= DrawingAreaInteractionListener ====================================
    void cursorRequested(PointerCursor::Value) override {}
    void cursorWarpRequested(int surfaceX, int surfaceY) override;
    void repaintRequested() override;
    void statusMessageRequested(const java::String& message) override;
    void selectionChanged() override;
    void raytracingRequested() override;
    void selectorDialogRequested() override;
    void closeRequested() override;
    void fullScreenGuiToggleRequested() override;

    //= ViewportSetInteractionListener ====================================
    void projectionLocationMenuRequested(Viewport* viewport, int x,
                                         int y) override;

    //= FramebufferDialogListener =========================================
    void redrawBehind() override;
};

#endif

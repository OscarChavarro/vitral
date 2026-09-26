#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <exception>
#include <clocale>
#include <fstream>
#include <map>
#include <stdexcept>
#include <string>
#include <vector>
#include <sys/time.h>

#include <glad/gl.h>
#include "vsdk/toolkit/render/opengl4/OpenGL4Loader.h"
#include <GL/glx.h>
#include <X11/Intrinsic.h>
#include <X11/IntrinsicP.h>
#include <X11/Shell.h>
#include <X11/StringDefs.h>
#include <X11/Xresource.h>
#include <X11/cursorfont.h>
#include <X11/keysym.h>

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "application/XtOpenGL4ApplicationController.h"
#include "application/XtOpenGL4GuiEventExecutor.h"
#include "application/XtOpenGL4SceneEditorApplication.h"
#include "application/mcp/XtOpenGL4VitralEditorMCP.h"
#include "gui/PopupDismissClickFilter.h"
#include "gui/xt/XtApplicationHost.h"
#include "vsdk/toolkit/gui/XtEventQueue.h"
#include "gui/xt/XtImageControlWindow.h"
#include "gui/xt/XtModifyPanel.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "gui/xt/XtSelectorDialog.h"
#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/render/xt/XtGuiRenderer.h"
#include "model/GuiState.h"
#include "gui/xt/XlibLabelImageProvider.h"
#include "vsdk/toolkit/gui/XtSystem.h"
#include "io/GuiJsonReader.h"
#include "render/opengl4/XtOpenGL4SceneBridge.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"

#ifdef __APPLE__
#error "SceneEditorApplication Xt + GLX + OpenGL 4.1 is supported only on Linux/X11, not macOS/XQuartz."
#endif

#ifndef GLX_CONTEXT_MAJOR_VERSION_ARB
#define GLX_CONTEXT_MAJOR_VERSION_ARB 0x2091
#endif
#ifndef GLX_CONTEXT_MINOR_VERSION_ARB
#define GLX_CONTEXT_MINOR_VERSION_ARB 0x2092
#endif
#ifndef GLX_CONTEXT_PROFILE_MASK_ARB
#define GLX_CONTEXT_PROFILE_MASK_ARB 0x9126
#endif
#ifndef GLX_CONTEXT_CORE_PROFILE_BIT_ARB
#define GLX_CONTEXT_CORE_PROFILE_BIT_ARB 0x00000001
#endif
#ifndef GLX_CONTEXT_FLAGS_ARB
#define GLX_CONTEXT_FLAGS_ARB 0x2094
#endif
#ifndef GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB
#define GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB 0x00000002
#endif

typedef GLXContext (*CreateContextAttribsARBProc)(
    Display*,
    GLXFBConfig,
    GLXContext,
    Bool,
    const int*);
typedef void (*SwapIntervalEXTProc)(Display*, GLXDrawable, int);

/**
Lookup of the OpenGL functions of GLX, as `OpenGL4Loader` expects it.
*/
static GLADapiproc glxGetProcAddress(const char* name)
{
    return reinterpret_cast<GLADapiproc>(
        glXGetProcAddressARB(reinterpret_cast<const GLubyte*>(name)));
}

class SceneEditorApplication;

struct TabBinding {
    SceneEditorApplication* application;
    int page;
};

/**
The scene editor with an Xt GUI and an OpenGL 4 drawing area: the
composition root of `AwtJogl4SceneEditorApplication`,
`AwtJogl4GuiController` and `AwtJogl4ApplicationController` of the Java
application. The model, its interaction techniques and its renderer are
behind the `XtOpenGL4SceneBridge`, which has no Xt dependency.

The GUI is built through the `XtWidgetSet` of the `XtUiFactory` it is
given: the widget set (Athena or Motif) is chosen when building the
application, and this class only uses the Xt Intrinsics.
*/
class SceneEditorApplication :
    public XtOpenGL4SceneEditorApplication,
    public XtOpenGL4ApplicationController,
    private XtOpenGL4SceneBridge::Listener,
    private XtApplicationHost
{
public:
    explicit SceneEditorApplication(XtUiFactory* uiFactory)
        : uiFactory(uiFactory)
        , widgetSet(uiFactory->getWidgetSet())
        , panelWidgets(widgetSet->getPanelWidgets())
        , guiRenderer(widgetSet->getGuiRenderer())
        , appContext(nullptr)
        , display(nullptr)
        , shell(nullptr)
        , workspace(nullptr)
        , menuBar(nullptr)
        , drawingCanvas(nullptr)
        , visual(nullptr)
        , visualDepth(0)
        , colormap(0)
        , menuFontSet(nullptr)
        , rightPanel(nullptr)
        , creationPage(nullptr)
        , modifyPage(nullptr)
        , guiPage(nullptr)
        , othersPage(nullptr)
        , renderPage(nullptr)
        , modifyPanel(nullptr)
        , globalBar(nullptr)
        , statusBar(nullptr)
        , executor(nullptr)
        , imageControlWindow(nullptr)
        , selectorDialog(nullptr)
        , automationService(nullptr)
        , viewportMenu(nullptr)
        , labelProvider(nullptr)
        , window(0)
        , fbConfig(nullptr)
        , context(nullptr)
        , shaderProgramId(0)
        , vertexArrayId(0)
        , vertexBufferId(0)
        , width(640)
        , height(480)
        , canvasWidth(320)
        , canvasHeight(452)
        , guiLanguage(selectedGuiLanguage())
        , pendingGuiLanguage()
        , guiRebuildQueued(false)
        , sceneBridge(nullptr)
        , ready(false)
        , closing(false)
        , repaintQueued(false)
        , currentCursor(PointerCursor::SELECT)
        , pressButton(0)
        , pressX(0)
        , pressY(0)
        , pressMoved(false)
        , lastClickButton(0)
        , lastClickTime(0)
        , clickCount(0)
        , viewportMenuX(0)
        , viewportMenuY(0)
    {
    }

    ~SceneEditorApplication()
    {
        // The automation service lives as long as the process (as in Java)
        delete imageControlWindow;
        imageControlWindow = nullptr;
        delete selectorDialog;
        selectorDialog = nullptr;
        cleanupOpenGL();
        // The editors stop listening to the entities before the scene goes
        if (sceneBridge != nullptr) sceneBridge->setBodyEditFeedbackProvider(nullptr);
        delete modifyPanel;
        modifyPanel = nullptr;
        if (sceneBridge != nullptr) {
            if (display != nullptr && context != nullptr)
                glXMakeCurrent(display, window, context);
            sceneBridge->dispose();
            delete sceneBridge;
            sceneBridge = nullptr;
        }
        delete labelProvider;
        labelProvider = nullptr;
        if (context != nullptr) {
            glXMakeCurrent(display, None, nullptr);
            glXDestroyContext(display, context);
            context = nullptr;
        }
        if (shell != nullptr) {
            XtDestroyWidget(shell);
            shell = nullptr;
        }
        globalBar = nullptr;
        delete executor;
        executor = nullptr;
        clearSideBindings();
        if (display != nullptr) {
            for (std::map<int, Cursor>::iterator it = cursors.begin(); it != cursors.end(); ++it)
                XFreeCursor(display, it->second);
            cursors.clear();
        }
        if (display != nullptr && menuFontSet != nullptr) {
            XFreeFontSet(display, menuFontSet);
            menuFontSet = nullptr;
        }
        if (display != nullptr) {
            // Frees the colors cached by the Xt converters in the colormap:
            // it goes with the connection, afterwards
            XtCloseDisplay(display);
            display = nullptr;
            colormap = 0;
        }
    }

    int run(int argc, char** argv)
    {
        // Before Xt takes its own options from the command line
        bool withAutomationService = false;
        for (int i = 1; i < argc; ++i)
            if (std::string(argv[i]) == "-s") withAutomationService = true;
        try {
            createWindow(argc, argv);
            createContext();
            initOpenGL();
            redraw();
            if (withAutomationService) {
                // Starts its own listener thread, which keeps it alive
                automationService = new XtOpenGL4VitralEditorMCP(this);
            }
            XtAppMainLoop(appContext);
        }
        catch (const VSDKFatalException& ex) {
            fprintf(stderr, "SceneEditorApplication fatal error: %s\n", ex.what());
            return 1;
        }
        catch (const std::exception& ex) {
            fprintf(stderr, "SceneEditorApplication error: %s\n", ex.what());
            return 1;
        }
        return 0;
    }

private:
    XtUiFactory* uiFactory;
    XtWidgetSet* widgetSet;
    XtPanelWidgets* panelWidgets;
    XtGuiRenderer* guiRenderer;
    XtAppContext appContext;
    Display* display;
    Widget shell;
    Widget workspace;
    Widget menuBar;
    Widget drawingCanvas;
    Visual* visual;
    int visualDepth;
    Colormap colormap;
    XFontSet menuFontSet;
    Widget rightPanel;
    Widget creationPage;
    Widget modifyPage;
    Widget guiPage;
    Widget othersPage;
    Widget renderPage;
    XtModifyPanel* modifyPanel;
    Widget globalBar;
    Widget statusBar;
    XtOpenGL4GuiEventExecutor* executor;
    XtImageControlWindow* imageControlWindow;
    XtSelectorDialog* selectorDialog;
    XtOpenGL4VitralEditorMCP* automationService;
    Widget viewportMenu;
    /// Commands of the items of the viewport menu shown
    std::vector<std::string> viewportMenuCommands;
    XlibLabelImageProvider* labelProvider;
    Window window;
    GLXFBConfig fbConfig;
    GLXContext context;
    GLuint shaderProgramId;
    GLuint vertexArrayId;
    GLuint vertexBufferId;
    int width;
    int height;
    int canvasWidth;
    int canvasHeight;
    std::string guiLanguage;
    std::string guiDefinition;
    std::string pendingGuiLanguage;
    bool guiRebuildQueued;
    std::vector<TabBinding*> tabBindings;
    std::map<std::string, std::string> messages;
    XtOpenGL4SceneBridge* sceneBridge;
    bool ready;
    bool closing;
    bool repaintQueued;
    std::map<int, Cursor> cursors;
    PointerCursor::Value currentCursor;
    PopupDismissClickFilter popupDismissFilter;
    int viewportMenuX;
    int viewportMenuY;
    // Clicks are synthesized from press / release pairs, as AWT does
    unsigned int pressButton;
    int pressX;
    int pressY;
    bool pressMoved;
    unsigned int lastClickButton;
    long long lastClickTime;
    int clickCount;

    static std::string selectedGuiLanguage()
    {
        const char* language = std::getenv("SCENE_EDITOR_GUI_LANGUAGE");
        return language != nullptr ? language : "english";
    }

    static void selectGuiLocale()
    {
        const char* overrideLocale = std::getenv("SCENE_EDITOR_GUI_LOCALE");
        // UI text is UTF-8 regardless of the selected JSON language.  A
        // neutral Unicode locale lets English labels such as "Español" and
        // future translations share one FontSet encoding.
        if (overrideLocale != nullptr &&
            setlocale(LC_CTYPE, overrideLocale) != nullptr) return;
        if (setlocale(LC_CTYPE, "C.UTF-8") != nullptr) return;
        setlocale(LC_CTYPE, "");
    }

    static void selectSideTab(Widget, void* clientData)
    {
        TabBinding* binding = reinterpret_cast<TabBinding*>(clientData);
        if (binding != nullptr && binding->application != nullptr)
            binding->application->showSidePage(binding->page);
    }

    /**
    Reads the I18N file of the language of the GUI, giving it to the model
    as its I18N context (as `AwtJogl4GuiController.loadGuiDefinition`).
    */
    void loadGuiDefinition()
    {
        const std::string path = GuiState::languageFile(guiLanguage.c_str()).c_str();
        std::ifstream input(path.c_str());
        if (!input) {
            throw VSDKFatalException(
                java::String(("Could not open Java GUI definition: " + path).c_str()));
        }
        guiDefinition.assign((std::istreambuf_iterator<char>(input)), std::istreambuf_iterator<char>());
        messages = GuiJsonReader(guiDefinition).readMessages();
        sceneBridge->getGuiState()->setLanguageGuiFile(
            GuiState::languageFile(guiLanguage.c_str()));
        // Viewport titles, their menus and the HUDs follow the language
        sceneBridge->setGuiDefinition(guiDefinition);
    }

    void clearSideBindings()
    {
        for (size_t i = 0; i < tabBindings.size(); ++i)
            delete tabBindings[i];
        tabBindings.clear();
    }

    std::string text(const char* id) const
    {
        std::map<std::string, std::string>::const_iterator it = messages.find(id);
        return it != messages.end() ? it->second : id;
    }

    static const int CREATION_PAGE = 0;
    static const int MODIFY_PAGE = 1;
    static const int GUI_PAGE = 2;
    static const int OTHERS_PAGE = 3;
    static const int RENDER_PAGE = 4;
    static const int MENU_BAR_HEIGHT = 28;
    static const int SIDE_TAB_HEIGHT = 28;
    static const int SIDE_TAB_WIDTH = 64;
    static const int SIDE_PANEL_WIDTH = 320;
    static const int STATUS_BAR_HEIGHT = 24;

    /**
    @return the vertical position where the canvas and the side panel start:
    below the menubar and the global bar
    */
    int contentTop() const
    {
        Dimension globalBarHeight = 0;
        if (globalBar != nullptr)
            XtVaGetValues(globalBar, XtNheight, &globalBarHeight, nullptr);
        return MENU_BAR_HEIGHT + globalBarHeight;
    }

    /**
    Shows the page of a tab of the side panel. As
    `AwtModifyTabChangeListener`, showing the modify page tells it the body
    it must edit.
    */
    void showSidePage(int page)
    {
        Widget pages[] = { creationPage, modifyPage, guiPage, othersPage, renderPage };
        for (int i = 0; i < 5; ++i)
            if (pages[i] == nullptr) return;
        for (int i = 0; i < 5; ++i)
            if (i != page) XtUnmanageChild(pages[i]);
        XtManageChild(pages[page]);
        setModifyPanelSelected(page == MODIFY_PAGE);
    }

    void setModifyPanelSelected(bool selected)
    {
        if (sceneBridge == nullptr) return;
        sceneBridge->setModifyPanelSelected(selected);
        if (selected) reportTargetToModifyPanel();
    }

    /**
    Notifies the modify panel of the target it must edit: the first selected
    body while the modify page is shown, or none.
    */
    void reportTargetToModifyPanel() override
    {
        if (sceneBridge == nullptr || modifyPanel == nullptr) return;
        SimpleBody* target = sceneBridge->getModifyPanelTarget();
        if (target != nullptr)
            modifyPanel->notifyTargetBeginEdit(target);
        else
            modifyPanel->notifyTargetEndEdit();
        requestRedraw();
    }

    Widget createSidePage(const char* name, bool managed)
    {
        return panelWidgets->createPanel(
            rightPanel, name, 0, SIDE_TAB_HEIGHT, SIDE_PANEL_WIDTH,
            height - contentTop() - STATUS_BAR_HEIGHT - SIDE_TAB_HEIGHT,
            managed);
    }

    /**
    Creates the tabbed side panel, as `AwtJogl4GuiController.createPanel`:
    the button groups of the GUI definition and the modify panel.
    */
    void createRightPanel()
    {
        const int top = contentTop();
        rightPanel = panelWidgets->createPanel(
            workspace, "rightPanel", width - SIDE_PANEL_WIDTH, top,
            SIDE_PANEL_WIDTH, height - top - STATUS_BAR_HEIGHT, true);

        const char* tabs[] = {
            "IDM_CREATION_TAB", "IDM_MODIFY_TAB", "IDM_GUI_TAB",
            "IDM_OTHERS_TAB", "IDM_RENDER_TAB"
        };
        for (int i = 0; i < 5; ++i) {
            TabBinding* binding = new TabBinding{this, i};
            tabBindings.push_back(binding);
            panelWidgets->createPushButton(
                rightPanel, text(tabs[i]), menuFontSet,
                i * SIDE_TAB_WIDTH, 0, SIDE_TAB_WIDTH, SIDE_TAB_HEIGHT - 4,
                &SceneEditorApplication::selectSideTab, binding);
        }

        creationPage = createSidePage("creationPage", true);
        modifyPage = createSidePage("modifyPage", false);
        guiPage = createSidePage("guiPage", false);
        othersPage = createSidePage("othersPage", false);
        renderPage = createSidePage("renderPage", false);
        modifyPanel = new XtModifyPanel(this, modifyPage, SIDE_PANEL_WIDTH);
        if (sceneBridge != nullptr) {
            sceneBridge->setBodyEditFeedbackProvider(modifyPanel);
            sceneBridge->setModifyPanelSelected(false);
        }

        // As the `AwtButtonsPanel`s of the tabs
        const char* groups[] = { "CREATION", "GUI", "OTHER", "RENDER" };
        Widget pages[] = { creationPage, guiPage, othersPage, renderPage };
        for (int i = 0; i < 4; ++i) {
            guiRenderer->buildButtonGroup(
                pages[i], sceneBridge->getButtonGroup(groups[i]), executor,
                menuFontSet, 0, 0, SIDE_PANEL_WIDTH);
        }
    }

    void rebuildRightPanel()
    {
        if (sceneBridge != nullptr) sceneBridge->setBodyEditFeedbackProvider(nullptr);
        delete modifyPanel;
        modifyPanel = nullptr;
        if (rightPanel != nullptr) XtDestroyWidget(rightPanel);
        rightPanel = nullptr;
        creationPage = nullptr;
        modifyPage = nullptr;
        guiPage = nullptr;
        othersPage = nullptr;
        renderPage = nullptr;
        clearSideBindings();
        createRightPanel();
    }

    /**
    Creates the global bar of icon buttons below the menubar, as the
    executor panel of `AwtJogl4GuiController`.
    */
    void createGlobalBar()
    {
        globalBar = guiRenderer->buildButtonGroup(
            workspace, sceneBridge->getButtonGroup("GLOBAL"), executor,
            menuFontSet, 0, MENU_BAR_HEIGHT, width);
    }

    void rebuildGlobalBar()
    {
        if (globalBar != nullptr) XtDestroyWidget(globalBar);
        globalBar = nullptr;
        createGlobalBar();
    }

    void createStatusBar()
    {
        statusBar = panelWidgets->createLabel(
            workspace, text("IDM_INTRO_MESSAGE"), menuFontSet,
            XtPanelWidgets::LEFT, 3, height - STATUS_BAR_HEIGHT + 1,
            width - 6, STATUS_BAR_HEIGHT - 2);
    }

    /**
    Creates the menubar from the I18N context, as
    `SwingGuiRenderer.buildMenubar` in `AwtJogl4GuiController`.
    */
    void createMenuBar()
    {
        menuBar = guiRenderer->buildMenubar(
            workspace, sceneBridge->getMenubar(), executor, menuFontSet,
            0, 0, width, MENU_BAR_HEIGHT, 112);
    }

    static void rebuildMenusAfterCallback(XtPointer clientData, XtIntervalId*)
    {
        SceneEditorApplication* self = reinterpret_cast<SceneEditorApplication*>(clientData);
        if (self == nullptr) return;
        self->guiRebuildQueued = false;
        self->rebuildMenus(self->pendingGuiLanguage);
    }

    void scheduleMenuRebuild(const std::string& language)
    {
        if (language == guiLanguage || guiRebuildQueued) return;
        pendingGuiLanguage = language;
        guiRebuildQueued = true;
        XtAppAddTimeOut(
            appContext, 0, &SceneEditorApplication::rebuildMenusAfterCallback,
            reinterpret_cast<XtPointer>(this));
    }

    void rebuildMenus(const std::string& language)
    {
        if (language == guiLanguage || workspace == nullptr) return;
        guiLanguage = language;
        if (menuBar != nullptr) {
            XtDestroyWidget(menuBar);
            menuBar = nullptr;
        }
        if (menuFontSet != nullptr) {
            XFreeFontSet(display, menuFontSet);
            menuFontSet = nullptr;
        }
        selectGuiLocale();
        createMenuFontSet();
        loadGuiDefinition();
        createMenuBar();
        rebuildGlobalBar();
        rebuildRightPanel();
        // Widgets with texts use the font set: all of them are created again,
        // as the Java application rebuilds its whole GUI
        if (statusBar != nullptr) XtDestroyWidget(statusBar);
        createStatusBar();
        delete imageControlWindow;
        imageControlWindow = nullptr;
        delete selectorDialog;
        selectorDialog = nullptr;
        applyFullScreenGuiMode();
        requestRedraw();
    }

    void createMenuFontSet()
    {
        char** missingCharsets = nullptr;
        int missingCharsetCount = 0;
        char* defaultString = nullptr;
        menuFontSet = XCreateFontSet(
            display,
            "-adobe-helvetica-medium-r-normal--14-*-*-*-*-*-iso8859-1",
            &missingCharsets,
            &missingCharsetCount,
            &defaultString);
        if (missingCharsets != nullptr) XFreeStringList(missingCharsets);
        if (menuFontSet == nullptr) {
            throw VSDKFatalException(
                "Could not create the UTF-8 menu font set");
        }
    }

    /**
    Merges the resources of the look of the GUI (the black and white one of
    the Athena GUI) over the ones of the display, so they win over the
    user's defaults. Widget sets keeping their own look (Motif) have none.
    */
    void loadResourceFile()
    {
        const char* path = uiFactory->getResourceFile();
        if (path == nullptr) return;
        XrmDatabase database = XtDatabase(display);
        if (!XrmCombineFileDatabase(path, &database, True)) {
            fprintf(stderr, "SceneEditorApplication: could not read X resources file %s\n", path);
            return;
        }
        XrmSetDatabase(display, database);
    }

    void createWindow(int argc, char** argv)
    {
        selectGuiLocale();
        XtToolkitInitialize();
        appContext = XtCreateApplicationContext();
        // Tasks of other threads (the automation service) run in its loop
        XtEventQueue::install(appContext);
        display = XtOpenDisplay(
            appContext,
            nullptr,
            "scene-editor",
            "SceneEditorApplication",
            nullptr,
            0,
            &argc,
            argv);
        if (display == nullptr) {
            throw VSDKFatalException("Could not open X display. Is DISPLAY set?");
        }
        loadResourceFile();
        const int screen = DefaultScreen(display);
        // Request a screen-sized client area at the upper-left corner.  This
        // keeps the window manager's title bar and borders, unlike fullscreen.
        width = DisplayWidth(display, screen);
        height = DisplayHeight(display, screen);
        createMenuFontSet();
        XVisualInfo* visualInfo = chooseVisual();
        visual = visualInfo->visual;
        visualDepth = visualInfo->depth;
        colormap = XCreateColormap(
            display,
            RootWindow(display, visualInfo->screen),
            visual,
            AllocNone);

        Arg args[9];
        Cardinal n = 0;
        // The shell accepts the keyboard focus from the window manager, so
        // it can redirect it to the drawing canvas (see
        // XtWidgetSet::setKeyboardFocus)
        XtSetArg(args[n], XtNinput, True); n++;
        XtSetArg(args[n], XtNvisual, visual); n++;
        XtSetArg(args[n], XtNcolormap, colormap); n++;
        XtSetArg(args[n], XtNdepth, visualDepth); n++;
        XtSetArg(args[n], XtNwidth, width); n++;
        XtSetArg(args[n], XtNheight, height); n++;
        const std::string title = std::string("VITRAL Scene Editor - ") +
            widgetSet->getName() + " GLX OpenGL 4";
        XtSetArg(args[n], XtNtitle, title.c_str()); n++;
        XtSetArg(args[n], XtNx, 0); n++;
        XtSetArg(args[n], XtNy, 0); n++;

        // An application shell: only for it Xt starts the resource path of
        // the widgets with the application class, so the entries of the
        // XResources file (`SceneEditorApplication*...`) apply
        shell = XtAppCreateShell(
            "sceneEditor",
            "SceneEditorApplication",
            applicationShellWidgetClass,
            display,
            args,
            n);
        if (shell == nullptr) {
            XFree(visualInfo);
            throw VSDKFatalException("Could not create Xt shell");
        }

        workspace = panelWidgets->createPanel(shell, "workspace", 0, 0,
                                              width, height, true);
        widgetSet->setWindowCloseHandler(
            shell, &SceneEditorApplication::closeRequestedByWindowManager, this);

        // The model is created first: the GUI is built from its I18N context
        labelProvider = new XlibLabelImageProvider(display);
        sceneBridge = new XtOpenGL4SceneBridge(labelProvider, this);
        loadGuiDefinition();
        executor = new XtOpenGL4GuiEventExecutor(this);
        createMenuBar();
        createGlobalBar();
        createRightPanel();
        createStatusBar();
        canvasWidth = width - SIDE_PANEL_WIDTH;
        canvasHeight = height - contentTop() - STATUS_BAR_HEIGHT;
        drawingCanvas = widgetSet->createDrawingArea(
            workspace, "drawingCanvas", 0, contentTop(), canvasWidth,
            canvasHeight);

        // Every mouse and keyboard event of the canvas is captured and
        // converted to a vitral event for the interaction techniques
        XtAddEventHandler(
            drawingCanvas,
            ExposureMask | StructureNotifyMask |
            KeyPressMask | KeyReleaseMask |
            ButtonPressMask | ButtonReleaseMask | PointerMotionMask |
            EnterWindowMask,
            False,
            &SceneEditorApplication::eventHandler,
            reinterpret_cast<XtPointer>(this));

        // Keys typed anywhere in the window (i.e. with the pointer over the
        // side panel) are redirected to the canvas
        widgetSet->setKeyboardFocus(shell, drawingCanvas);

        XtRealizeWidget(shell);
        window = XtWindow(drawingCanvas);
        widgetSet->setKeyboardFocus(shell, drawingCanvas);

        XFree(visualInfo);
    }

    XVisualInfo* chooseVisual()
    {
        const int fbAttributes[] = {
            GLX_X_RENDERABLE, True,
            GLX_DRAWABLE_TYPE, GLX_WINDOW_BIT,
            GLX_RENDER_TYPE, GLX_RGBA_BIT,
            GLX_X_VISUAL_TYPE, GLX_TRUE_COLOR,
            GLX_RED_SIZE, 8,
            GLX_GREEN_SIZE, 8,
            GLX_BLUE_SIZE, 8,
            GLX_ALPHA_SIZE, 8,
            GLX_DEPTH_SIZE, 24,
            GLX_DOUBLEBUFFER, True,
            None
        };

        int count = 0;
        GLXFBConfig* configs = glXChooseFBConfig(
            display,
            DefaultScreen(display),
            fbAttributes,
            &count);
        if (configs == nullptr || count == 0) {
            throw VSDKFatalException("No suitable GLX framebuffer config found");
        }

        XVisualInfo* visualInfo = nullptr;
        for (int i = 0; i < count; ++i) {
            visualInfo = glXGetVisualFromFBConfig(display, configs[i]);
            if (visualInfo != nullptr) {
                fbConfig = configs[i];
                break;
            }
        }

        XFree(configs);
        if (visualInfo == nullptr) {
            throw VSDKFatalException("No X visual found for GLX framebuffer config");
        }
        return visualInfo;
    }

    void createContext()
    {
        CreateContextAttribsARBProc createContextAttribs =
            reinterpret_cast<CreateContextAttribsARBProc>(
                glXGetProcAddressARB(
                    reinterpret_cast<const GLubyte*>("glXCreateContextAttribsARB")));

        if (createContextAttribs == nullptr) {
            throw VSDKFatalException("GLX_ARB_create_context is not available");
        }

        const int contextAttributes[] = {
            GLX_CONTEXT_MAJOR_VERSION_ARB, 4,
            GLX_CONTEXT_MINOR_VERSION_ARB, 1,
            GLX_CONTEXT_PROFILE_MASK_ARB, GLX_CONTEXT_CORE_PROFILE_BIT_ARB,
            GLX_CONTEXT_FLAGS_ARB, GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB,
            None
        };

        context = createContextAttribs(
            display,
            fbConfig,
            nullptr,
            True,
            contextAttributes);
        if (context == nullptr) {
            throw VSDKFatalException("Could not create an OpenGL 4.1 core GLX context");
        }

        if (!glXMakeCurrent(display, window, context)) {
            throw VSDKFatalException("Could not make GLX context current");
        }

        SwapIntervalEXTProc swapInterval =
            reinterpret_cast<SwapIntervalEXTProc>(
                glXGetProcAddressARB(
                    reinterpret_cast<const GLubyte*>("glXSwapIntervalEXT")));
        if (swapInterval != nullptr) {
            swapInterval(display, window, 1);
        }
    }

    void initOpenGL()
    {
        // The functions of the GLX context, as glad loads them
        if (!OpenGL4Loader::load(&glxGetProcAddress)) {
            throw VSDKFatalException("The OpenGL 4.1 functions could not be loaded");
        }

        checkOpenGLVersion();
        sceneBridge->setCanvasSize(canvasWidth, canvasHeight);
        sceneBridge->init();
        shaderProgramId = createShaderProgram();

        glGenVertexArrays(1, &vertexArrayId);
        glBindVertexArray(vertexArrayId);

        glGenBuffers(1, &vertexBufferId);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId);

        const float vertexData[] = {
            -0.8f, -0.8f, 0.0f,
             0.8f,  0.8f, 0.0f
        };

        glBufferData(GL_ARRAY_BUFFER, sizeof(vertexData), vertexData, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 3 * sizeof(float), nullptr);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        glUseProgram(shaderProgramId);
        setShaderUniforms();
        glUseProgram(0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glViewport(0, 0, canvasWidth, canvasHeight);
        ready = true;
    }

    void checkOpenGLVersion()
    {
        const char* versionStr = reinterpret_cast<const char*>(glGetString(GL_VERSION));
        const char* rendererStr = reinterpret_cast<const char*>(glGetString(GL_RENDERER));
        const char* vendorStr = reinterpret_cast<const char*>(glGetString(GL_VENDOR));
        printf("OpenGL vendor: %s\n", vendorStr != nullptr ? vendorStr : "(unknown)");
        printf("OpenGL renderer: %s\n", rendererStr != nullptr ? rendererStr : "(unknown)");
        printf("OpenGL version string: %s\n", versionStr != nullptr ? versionStr : "(unknown)");

        GLint major = 0;
        GLint minor = 0;
        glGetIntegerv(GL_MAJOR_VERSION, &major);
        glGetIntegerv(GL_MINOR_VERSION, &minor);
        if ((major == 0 && minor == 0) && versionStr != nullptr) {
            sscanf(versionStr, "%d.%d", &major, &minor);
        }
        printf("OpenGL parsed version: %d.%d\n", major, minor);

        if (major < 4 || (major == 4 && minor < 1)) {
            Logger::reportMessage(
                "SceneEditorApplication",
                Logger::ERROR,
                "checkOpenGLVersion",
                "OpenGL version too old");
            throw VSDKFatalException("This example requires OpenGL 4.1+");
        }
    }

    GLuint createShaderProgram()
    {
        const java::String vertexSource =
            "#version 410 core\n"
            "layout(location = 0) in vec3 vertexPosition;\n"
            "uniform mat4 modelViewProjectionLocal;\n"
            "void main() {\n"
            "    gl_Position = modelViewProjectionLocal * vec4(vertexPosition, 1.0);\n"
            "}\n";
        const java::String fragmentSource =
            "#version 410 core\n"
            "uniform vec3 diffuseColor;\n"
            "out vec4 fragColor;\n"
            "void main() {\n"
            "    fragColor = vec4(diffuseColor, 1.0);\n"
            "}\n";

        GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        GLuint program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glBindFragDataLocation(program, 0, "fragColor");
        glLinkProgram(program);

        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        if (linkStatus == GL_FALSE) {
            java::String log = getProgramInfoLog(program);
            fprintf(stderr, "Program link error: %s\n", log.c_str());
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(program);
            throw VSDKFatalException("Shader program link failed");
        }

        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    GLuint compileShader(GLenum shaderType, const java::String& source)
    {
        GLuint shader = glCreateShader(shaderType);
        const char* src = source.c_str();
        GLint length = source.length();

        glShaderSource(shader, 1, &src, &length);
        glCompileShader(shader);

        GLint compileStatus = GL_FALSE;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compileStatus);
        if (compileStatus == GL_FALSE) {
            java::String log = getShaderInfoLog(shader);
            fprintf(stderr, "Shader compile error: %s\n", log.c_str());
            glDeleteShader(shader);
            throw VSDKFatalException("Shader compile failed");
        }
        return shader;
    }

    void setShaderUniforms()
    {
        const float identity[] = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };

        GLint mvpLoc = glGetUniformLocation(shaderProgramId, "modelViewProjectionLocal");
        GLint diffuseColorLoc = glGetUniformLocation(shaderProgramId, "diffuseColor");

        if (mvpLoc >= 0) {
            glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, identity);
        }
        if (diffuseColorLoc >= 0) {
            glUniform3f(diffuseColorLoc, 1.0f, 1.0f, 1.0f);
        }
    }

    void redraw()
    {
        if (!ready || display == nullptr || context == nullptr) {
            return;
        }
        glXMakeCurrent(display, window, context);
        glViewport(0, 0, canvasWidth, canvasHeight);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        if (sceneBridge != nullptr) {
            // X11 reports the screen in the pixels text is drawn with
            const int screen = DefaultScreen(display);
            sceneBridge->setScreenResolution(DisplayWidth(display, screen),
                                             DisplayHeight(display, screen));
            sceneBridge->display(canvasWidth, canvasHeight);
        }
        else {
            glUseProgram(shaderProgramId);
            glBindVertexArray(vertexArrayId);
            glLineWidth(1.0f);
            glDrawArrays(GL_LINES, 0, 2);
            glBindVertexArray(0);
            glUseProgram(0);
        }

        glXSwapBuffers(display, window);
    }

    void requestClose()
    {
        if (closing) {
            return;
        }
        closing = true;
        XtAppSetExitFlag(appContext);
    }

    //= Events ============================================================

    static long long currentTimeMillis()
    {
        struct timeval now;
        gettimeofday(&now, nullptr);
        return static_cast<long long>(now.tv_sec) * 1000 + now.tv_usec / 1000;
    }

    static void closeRequestedByWindowManager(void* clientData)
    {
        static_cast<SceneEditorApplication*>(clientData)->requestClose();
    }

    static void eventHandler(
        Widget,
        XtPointer clientData,
        XEvent* event,
        Boolean*)
    {
        SceneEditorApplication* self =
            reinterpret_cast<SceneEditorApplication*>(clientData);
        if (self == nullptr) {
            return;
        }
        try {
            self->processEvent(event);
        }
        catch (const std::exception& ex) {
            fprintf(stderr, "SceneEditorApplication: event processing failed: %s\n", ex.what());
        }
    }

    void processEvent(XEvent* event)
    {
        switch (event->type) {
        case Expose:
            if (event->xexpose.count == 0) {
                redraw();
            }
            break;
        case ConfigureNotify:
            canvasWidth = event->xconfigure.width;
            canvasHeight = event->xconfigure.height;
            if (sceneBridge != nullptr) {
                sceneBridge->setCanvasSize(canvasWidth, canvasHeight);
            }
            redraw();
            break;
        case EnterNotify:
            if (sceneBridge != nullptr) {
                sceneBridge->mouseEntered(XtSystem::xt2vsdkMouseEvent(*event));
            }
            break;
        case ButtonPress:
            processButtonPress(*event);
            break;
        case ButtonRelease:
            processButtonRelease(*event);
            break;
        case MotionNotify:
            processMotion(event);
            break;
        case KeyPress:
            if (sceneBridge != nullptr) {
                keyPressed(XtSystem::xt2vsdkKeyEvent(event->xkey),
                           XLookupKeysym(&event->xkey, 0), event->xkey.state);
            }
            break;
        case KeyRelease:
            if (sceneBridge != nullptr && !isAutoRepeatRelease(event->xkey)) {
                sceneBridge->keyReleased(XtSystem::xt2vsdkKeyEvent(event->xkey));
                requestRedraw();
            }
            break;
        default:
            break;
        }
    }

    /**
    Delivers a key press to the interaction techniques, as
    `AwtDrawingAreaController.keyPressed`.
    @param event the vitral event
    @param baseKeysym symbol of the key without modifiers
    @param state X modifiers mask
    */
    void keyPressed(const KeyEvent& event, KeySym baseKeysym, unsigned int state)
    {
        sceneBridge->keyPressed(event);

        // Ctrl+Shift+F: the key identity is lost in the vitral event for
        // control characters, so the chord is detected here
        if ((state & ShiftMask) != 0 && (state & ControlMask) != 0 &&
            baseKeysym == XK_f) {
            fullScreenGuiToggleRequested();
        }
        requestRedraw();
    }

    /**
    X repeats a held key as release / press pairs: the release of such a
    pair is not reported, so the key is seen as held (as with AWT).
    */
    bool isAutoRepeatRelease(const XKeyEvent& release)
    {
        if (XEventsQueued(display, QueuedAfterReading) == 0) return false;
        XEvent next;
        XPeekEvent(display, &next);
        return next.type == KeyPress && next.xkey.window == release.window &&
            next.xkey.keycode == release.keycode && next.xkey.time == release.time;
    }

    bool consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind kind)
    {
        return popupDismissFilter.consumes(kind, currentTimeMillis());
    }

    void processButtonPress(const XEvent& event)
    {
        if (sceneBridge == nullptr) return;
        if (XtSystem::isWheelButton(event)) {
            MouseEvent wheel = XtSystem::xt2vsdkWheelEvent(event);
            if (wheel.getClicks() != 0) {
                sceneBridge->mouseWheel(wheel);
                requestRedraw();
            }
            return;
        }
        if (consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::PRESS)) return;
        // Keys go back to the canvas after typing in a field of the panel
        widgetSet->setKeyboardFocus(shell, drawingCanvas);
        pressButton = event.xbutton.button;
        pressX = event.xbutton.x;
        pressY = event.xbutton.y;
        pressMoved = false;
        sceneBridge->mousePressed(XtSystem::xt2vsdkMouseEvent(event));
        requestRedraw();
    }

    void processButtonRelease(const XEvent& event)
    {
        if (sceneBridge == nullptr || XtSystem::isWheelButton(event)) return;
        if (consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::RELEASE)) return;
        const bool clicked = event.xbutton.button == pressButton && !pressMoved;
        pressButton = 0;
        // The release may request the viewport menu, that grabs the pointer
        sceneBridge->mouseReleased(XtSystem::xt2vsdkMouseEvent(event));
        if (clicked &&
            !consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::CLICK)) {
            const long long now = currentTimeMillis();
            const long long multiClickTime = XtGetMultiClickTime(display);
            if (event.xbutton.button == lastClickButton &&
                now - lastClickTime <= multiClickTime) {
                ++clickCount;
            }
            else {
                clickCount = 1;
            }
            lastClickButton = event.xbutton.button;
            lastClickTime = now;
            MouseEvent click = XtSystem::xt2vsdkMouseEvent(event);
            click.setClicks(clickCount);
            sceneBridge->mouseClicked(click);
        }
        requestRedraw();
    }

    void processMotion(XEvent* event)
    {
        if (sceneBridge == nullptr) return;
        // Only the last position of consecutive motions matters
        XEvent next;
        while (XEventsQueued(display, QueuedAfterReading) > 0) {
            XPeekEvent(display, &next);
            if (next.type != MotionNotify || next.xmotion.window != event->xmotion.window) break;
            XNextEvent(display, event);
        }
        const unsigned int buttons = Button1Mask | Button2Mask | Button3Mask;
        if ((event->xmotion.state & buttons) == 0) {
            sceneBridge->mouseMoved(XtSystem::xt2vsdkMouseEvent(*event));
            return;
        }
        if (consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::DRAG)) return;
        // As in AWT, a press followed by any motion is not a click
        if (event->xmotion.x != pressX || event->xmotion.y != pressY) pressMoved = true;
        sceneBridge->mouseDragged(XtSystem::xt2vsdkMouseEvent(*event));
        requestRedraw();
    }

    static void redrawAfterEvents(XtPointer clientData, XtIntervalId*)
    {
        SceneEditorApplication* self = reinterpret_cast<SceneEditorApplication*>(clientData);
        if (self == nullptr) return;
        self->repaintQueued = false;
        self->redraw();
    }

    /**
    Coalesces the repaints requested while processing a burst of events in
    one redraw, done once the pending events are processed.
    */
    void requestRedraw()
    {
        if (repaintQueued || appContext == nullptr) return;
        repaintQueued = true;
        XtAppAddTimeOut(appContext, 0, &SceneEditorApplication::redrawAfterEvents,
                        reinterpret_cast<XtPointer>(this));
    }

    //= Viewport menu =====================================================

    static void executeViewportMenuCommand(int index, void* clientData)
    {
        SceneEditorApplication* self = static_cast<SceneEditorApplication*>(clientData);
        if (self->sceneBridge == nullptr || index < 0 ||
            index >= static_cast<int>(self->viewportMenuCommands.size())) return;
        self->sceneBridge->executeViewportCommand(self->viewportMenuCommands[index]);
        self->requestRedraw();
    }

    static void viewportMenuPoppedDown(void* clientData)
    {
        SceneEditorApplication* self = static_cast<SceneEditorApplication*>(clientData);
        self->popupDismissFilter.popupClosed(currentTimeMillis());
        self->requestRedraw();
    }

    /**
    Pops up the menu of the viewport whose title was clicked: its
    projection locations and render modes, the ones in use checked. It
    takes the pointer, so a click outside only closes it.
    */
    void showViewportMenu(int canvasX, int canvasY)
    {
        if (viewportMenu != nullptr) {
            XtDestroyWidget(viewportMenu);
            viewportMenu = nullptr;
        }
        const std::vector<XtOpenGL4SceneBridge::ViewportMenuItem> items =
            sceneBridge->getViewportMenuItems();
        if (items.empty()) return;

        std::vector<XtWidgetSet::MenuChoice> choices;
        viewportMenuCommands.clear();
        for (size_t i = 0; i < items.size(); ++i) {
            XtWidgetSet::MenuChoice choice;
            choice.label = items[i].label;
            choice.checked = items[i].current;
            choice.separator = items[i].separator;
            choices.push_back(choice);
            viewportMenuCommands.push_back(items[i].command);
        }
        viewportMenu = widgetSet->createChoiceMenu(
            drawingCanvas, choices, menuFontSet,
            &SceneEditorApplication::executeViewportMenuCommand, this);

        Position rootX = 0;
        Position rootY = 0;
        XtTranslateCoords(drawingCanvas, static_cast<Position>(canvasX),
                          static_cast<Position>(canvasY), &rootX, &rootY);
        widgetSet->popupChoiceMenu(viewportMenu, rootX, rootY,
                                   &SceneEditorApplication::viewportMenuPoppedDown,
                                   this);
        popupDismissFilter.popupShown();
    }

    //= XtOpenGL4SceneBridge::Listener ====================================

    void repaintRequested() override
    {
        requestRedraw();
    }

    Cursor getCursor(PointerCursor::Value cursor)
    {
        unsigned int shape = XC_left_ptr;
        switch (cursor) {
        case PointerCursor::VIEWPORT_TITLE: shape = XC_hand2; break;
        case PointerCursor::CAMERA_ROTATE: shape = XC_exchange; break;
        case PointerCursor::CAMERA_TRANSLATE: shape = XC_fleur; break;
        case PointerCursor::CAMERA_ADVANCE: shape = XC_sb_v_double_arrow; break;
        case PointerCursor::TRANSLATE: shape = XC_fleur; break;
        case PointerCursor::ROTATE: shape = XC_exchange; break;
        case PointerCursor::SCALE: shape = XC_sizing; break;
        default: shape = XC_left_ptr; break;
        }
        std::map<int, Cursor>::iterator known = cursors.find(static_cast<int>(shape));
        if (known != cursors.end()) return known->second;
        Cursor created = XCreateFontCursor(display, shape);
        cursors[static_cast<int>(shape)] = created;
        return created;
    }

    void cursorRequested(PointerCursor::Value cursor) override
    {
        if (cursor == currentCursor || window == 0) return;
        currentCursor = cursor;
        XDefineCursor(display, window, getCursor(cursor));
    }

    void cursorWarpRequested(int canvasX, int canvasY) override
    {
        if (window == 0) return;
        XWarpPointer(display, None, window, 0, 0, 0, 0, canvasX, canvasY);
    }

    void statusMessageRequested(const std::string& message) override
    {
        showStatusMessage(message);
    }

    void closeRequested() override
    {
        requestClose();
    }

    void selectionChanged() override
    {
        reportTargetToModifyPanel();
    }

    /**
    Switches between showing only the drawing area and showing all the
    GUI, as `AwtDrawingAreaFeedback.fullScreenGuiToggleRequested` (which
    rebuilds the Swing GUI).
    */
    void fullScreenGuiToggleRequested() override
    {
        if (sceneBridge == nullptr) return;
        sceneBridge->getGuiState()->toggleFullScreenGuiMode();
        applyFullScreenGuiMode();
        requestRedraw();
    }

    /**
    Lays out the window for the full screen mode of the GUI state: only the
    drawing area, over the whole window, or all the GUI.
    */
    void applyFullScreenGuiMode()
    {
        if (sceneBridge == nullptr || drawingCanvas == nullptr) return;
        const bool fullScreen = sceneBridge->getGuiState()->isFullScreenGuiMode();
        Widget others[] = {
            menuBar, globalBar,
            rightPanel, statusBar
        };
        for (int i = 0; i < 4; ++i) {
            if (others[i] == nullptr) continue;
            if (fullScreen) XtUnmanageChild(others[i]);
            else XtManageChild(others[i]);
        }
        if (fullScreen) {
            XtConfigureWidget(drawingCanvas, 0, 0, width, height, 0);
        }
        else {
            XtConfigureWidget(drawingCanvas, 0, contentTop(),
                              width - SIDE_PANEL_WIDTH,
                              height - contentTop() - STATUS_BAR_HEIGHT, 0);
        }
        setWindowManagerFullScreen(fullScreen);
        widgetSet->setKeyboardFocus(shell, drawingCanvas);
    }

    /**
    Asks the window manager (EWMH) to show the window without decorations
    over the whole screen, as the undecorated frame of the Java application.
    */
    void setWindowManagerFullScreen(bool fullScreen)
    {
        if (shell == nullptr || !XtIsRealized(shell)) return;
        XEvent event;
        memset(&event, 0, sizeof(event));
        event.xclient.type = ClientMessage;
        event.xclient.window = XtWindow(shell);
        event.xclient.message_type = XInternAtom(display, "_NET_WM_STATE", False);
        event.xclient.format = 32;
        event.xclient.data.l[0] = fullScreen ? 1 : 0;
        event.xclient.data.l[1] = static_cast<long>(
            XInternAtom(display, "_NET_WM_STATE_FULLSCREEN", False));
        event.xclient.data.l[3] = 1;
        XSendEvent(display, DefaultRootWindow(display), False,
                   SubstructureRedirectMask | SubstructureNotifyMask, &event);
    }

    void raytracingRequested() override
    {
        showStatusMessage(getMessage("IDM_COMPUTING_RAYTRACING"));
        doRaytracingImage();
        showImage(sceneBridge->getRaytracedImage());
    }

    void selectorDialogRequested() override
    {
        if (selectorDialog == nullptr) selectorDialog = new XtSelectorDialog(this);
        selectorDialog->setVisible(true);
    }

    void imageRequested(RGBImageUncompressed* image) override
    {
        showImage(image);
    }

    //= XtApplicationHost =================================================

    XtOpenGL4SceneBridge* getSceneBridge() override
    {
        return sceneBridge;
    }

    XtUiFactory* getUiFactory() override
    {
        return uiFactory;
    }

    std::string getMessage(const char* id) override
    {
        return text(id);
    }

    void showStatusMessage(const std::string& message) override
    {
        if (statusBar != nullptr) panelWidgets->setLabel(statusBar, message);
    }

    void showImage(RGBImageUncompressed* image) override
    {
        if (imageControlWindow == nullptr)
            imageControlWindow = new XtImageControlWindow(this, image);
        else
            imageControlWindow->setImage(image);
        imageControlWindow->redrawImage();
    }

    Widget createDialogShell(const char* name, WidgetClass shellClass,
                             const char* title) override
    {
        Arg args[4]; Cardinal n = 0;
        XtSetArg(args[n], XtNvisual, visual); ++n;
        XtSetArg(args[n], XtNdepth, visualDepth); ++n;
        XtSetArg(args[n], XtNcolormap, colormap); ++n;
        XtSetArg(args[n], XtNtitle, title); ++n;
        return XtCreatePopupShell(name, shellClass, shell, args, n);
    }

    void setGuiLanguage(const std::string& language) override
    {
        scheduleMenuRebuild(language);
    }

    //= XtOpenGL4SceneEditorApplication ===================================

    ApplicationModel* getApplicationModel() override
    {
        return sceneBridge != nullptr ? sceneBridge->getApplicationModel() : nullptr;
    }

    XtOpenGL4ApplicationController* getOpenGL4Controller() override
    {
        return this;
    }

    java::ArrayList<java::String> getGuiLanguages() override
    {
        return GuiState::listLanguages();
    }

    java::String getCurrentGuiLanguage() override
    {
        return guiLanguage.c_str();
    }

    bool setGuiLanguageById(const java::String& language) override
    {
        java::ArrayList<java::String> languages = getGuiLanguages();
        bool known = false;
        for (long i = 0; i < languages.size(); ++i)
            if (languages.get(i).equals(language)) known = true;
        if (!known) return false;
        // Not called from a widget callback: the GUI is rebuilt now
        rebuildMenus(language.c_str());
        return true;
    }

    void doRaytracingImage() override
    {
        if (sceneBridge != nullptr) sceneBridge->doRaytracingImage();
    }

    void closeApplication() override
    {
        requestClose();
    }

    //= XtOpenGL4ApplicationController ====================================

    bool isDrawingAreaCreated() override
    {
        return ready && sceneBridge != nullptr;
    }

    void repaint() override
    {
        requestRedraw();
    }

    void exportViewportPng(const java::File& file) override
    {
        sceneBridge->requestViewportExport(file, false);
        redraw();
    }

    void exportViewportJpg(const java::File& file) override
    {
        sceneBridge->requestViewportExport(file, true);
        redraw();
    }

    void exportWorkspaceJpg(const java::File& file) override
    {
        sceneBridge->requestWorkspaceExport(file);
        redraw();
    }

    static int buttonMaskFor(int button)
    {
        switch (button) {
        case 1: return MouseEvent::BUTTON1_DOWN_MASK;
        case 2: return MouseEvent::BUTTON2_DOWN_MASK;
        case 3: return MouseEvent::BUTTON3_DOWN_MASK;
        default: return 0;
        }
    }

    /**
    As `AwtDrawingAreaController.injectMouseEvent` and the listener methods
    the event is dispatched to.
    */
    void injectMouseEvent(const java::String& type, int x, int y,
                          int button) override
    {
        MouseEvent event;
        event.setX(x);
        event.setY(y);
        event.setClicks(1);
        sceneBridge->setCanvasSize(canvasWidth, canvasHeight);

        if (type.equals("move")) {
            sceneBridge->mouseMoved(event);
        }
        else if (type.equals("press")) {
            event.setButton(button);
            event.setModifiers(buttonMaskFor(button));
            if (consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::PRESS)) return;
            sceneBridge->mousePressed(event);
        }
        else if (type.equals("drag")) {
            event.setButton(button);
            event.setModifiers(buttonMaskFor(button));
            if (consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::DRAG)) return;
            sceneBridge->mouseDragged(event);
        }
        else if (type.equals("release")) {
            event.setButton(button);
            if (consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::RELEASE)) return;
            sceneBridge->mouseReleased(event);
        }
        else {
            throw std::invalid_argument((java::String("Unknown mouse event type \"") +
                type + "\". Use move, press, drag or release").c_str());
        }
        requestRedraw();
    }

    /**
    As `AwtDrawingAreaController.injectKeyEvent`: the key is mapped as the
    ones of the keyboard (see `XtSystem`), from its symbol.
    */
    void injectKeyEvent(const java::String& key, bool shift, bool ctrl) override
    {
        KeySym keysym;
        char keyChar = 0;

        if (key.equals("tab")) { keysym = XK_Tab; keyChar = '\t'; }
        else if (key.equals("enter")) { keysym = XK_Return; keyChar = '\n'; }
        else if (key.equals("backspace")) { keysym = XK_BackSpace; keyChar = '\b'; }
        else if (key.equals("delete")) { keysym = XK_Delete; keyChar = 127; }
        else if (key.equals("escape")) keysym = XK_Escape;
        else if (key.equals("left")) keysym = XK_Left;
        else if (key.equals("right")) keysym = XK_Right;
        else if (key.equals("up")) keysym = XK_Up;
        else if (key.equals("down")) keysym = XK_Down;
        else if (key.equals("pageup")) keysym = XK_Page_Up;
        else if (key.equals("pagedown")) keysym = XK_Page_Down;
        else {
            if (key.length() != 1) {
                throw std::invalid_argument((java::String("Unknown key \"") + key +
                    "\". Use a single character, tab, enter, backspace, delete, escape, left, right, up, down, pageup or pagedown").c_str());
            }
            keyChar = key.charAt(0);
            // Latin 1 symbols are their characters
            keysym = static_cast<unsigned char>(keyChar);
        }

        // The symbol of the key without modifiers: the lower case letter
        KeySym baseKeysym = keysym;
        if (keysym >= XK_A && keysym <= XK_Z) baseKeysym = keysym - XK_A + XK_a;

        unsigned int state = 0;
        if (shift) state |= ShiftMask;
        if (ctrl) {
            state |= ControlMask;
            if ((keyChar >= 'a' && keyChar <= 'z') || (keyChar >= 'A' && keyChar <= 'Z')) {
                char upper = keyChar >= 'a' ? static_cast<char>(keyChar - 'a' + 'A') : keyChar;
                keyChar = static_cast<char>(upper - 'A' + 1);
            }
        }
        char text[2] = { keyChar, 0 };
        // Delivered directly: it does not depend on the focus of the window
        keyPressed(XtSystem::xt2vsdkKeyEvent(keysym, baseKeysym, text,
                                             keyChar != 0 ? 1 : 0, state),
                   baseKeysym, state);
    }

    bool projectToCanvas(Viewport* viewport, const Vector3Dd& point,
                         double outCanvas[2]) override
    {
        sceneBridge->setCanvasSize(canvasWidth, canvasHeight);
        return sceneBridge->projectToCanvas(viewport, point, outCanvas);
    }

    //= XtModifyPanelHost =================================================

    void repaintDrawingArea() override
    {
        requestRedraw();
    }

    XFontSet getPanelFontSet() override
    {
        return menuFontSet;
    }

    XtPanelWidgets* getPanelWidgets() override
    {
        return panelWidgets;
    }

    static void showViewportMenuAfterEvent(XtPointer clientData, XtIntervalId*)
    {
        SceneEditorApplication* self = reinterpret_cast<SceneEditorApplication*>(clientData);
        if (self != nullptr && self->sceneBridge != nullptr)
            self->showViewportMenu(self->viewportMenuX, self->viewportMenuY);
    }

    void viewportMenuRequested(int canvasX, int canvasY) override
    {
        // Requested while dispatching the release over the title: popped
        // up once it is dispatched, or Xt would also deliver that release
        // to the new menu, closing it
        viewportMenuX = canvasX;
        viewportMenuY = canvasY;
        XtAppAddTimeOut(appContext, 0, &SceneEditorApplication::showViewportMenuAfterEvent,
                        reinterpret_cast<XtPointer>(this));
    }

    java::String getShaderInfoLog(GLuint shader)
    {
        GLint length = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &length);
        if (length <= 1) {
            return "(no log)";
        }

        char* log = new char[length]();
        glGetShaderInfoLog(shader, length, &length, log);
        java::String result(log);
        delete[] log;
        return result;
    }

    java::String getProgramInfoLog(GLuint program)
    {
        GLint length = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &length);
        if (length <= 1) {
            return "(no log)";
        }

        char* log = new char[length]();
        glGetProgramInfoLog(program, length, &length, log);
        java::String result(log);
        delete[] log;
        return result;
    }

    void cleanupOpenGL()
    {
        if (display != nullptr && context != nullptr) {
            glXMakeCurrent(display, window, context);
        }
        if (vertexBufferId != 0) {
            glDeleteBuffers(1, &vertexBufferId);
            vertexBufferId = 0;
        }
        if (vertexArrayId != 0) {
            glDeleteVertexArrays(1, &vertexArrayId);
            vertexArrayId = 0;
        }
        if (shaderProgramId != 0) {
            glDeleteProgram(shaderProgramId);
            shaderProgramId = 0;
        }
        ready = false;
    }
};

int main(int argc, char** argv)
{
    // The widget set the application is built with
    XtUiFactory* uiFactory = createUiFactory();
    int result = 0;
    {
        SceneEditorApplication app(uiFactory);
        result = app.run(argc, argv);
    }
    delete uiFactory;
    return result;
}

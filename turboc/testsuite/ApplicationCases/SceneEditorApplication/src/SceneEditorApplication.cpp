#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "java/lang/Runnable.h"
#include "java/util/function/Consumer.h"
#include "java/util/function/Function.h"
#include "java/util/function/Supplier.h"
#include <locale.h>
#include "java/nio/file/Files.h"
#include "java/util/TreeMap.h"
#include <sys/time.h>

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Loader.h"
// Mesa 6.5 declares glXGetProcAddressARB (GLX_ARB_get_proc_address) only
// with the prototypes of glxext.h
#ifndef GLX_GLXEXT_PROTOTYPES
#define GLX_GLXEXT_PROTOTYPES
#endif
#include <GL/glx.h>
// Xt names its strings and objects as the vitral String and Object
// classes: renamed while its headers are read (see XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <X11/Intrinsic.h>
#include <X11/IntrinsicP.h>
#include <X11/Shell.h>
#include <X11/StringDefs.h>
#undef Object
#undef String
#include <X11/Xresource.h>
#include <X11/cursorfont.h>
#include <X11/keysym.h>

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "application/XtOpenGL1ApplicationController.h"
#include "application/XtOpenGL1GuiEventExecutor.h"
#include "application/XtOpenGL1SceneEditorApplication.h"
#include "application/mcp/XtOpenGL1VitralEditorMCP.h"
#include "render/opengl1/XtOpenGL1SceneBridge.h"
#include "application/TangibleInterfaceConnection.h"
#include "application/XtSceneEditorOpenGLVariant.h"
#include "gui/PopupDismissClickFilter.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtCollapsibleSection.h"
#include "vsdk/toolkit/gui/XtEventQueue.h"
#include "gui/xt/XtImageControlWindow.h"
#include "gui/xt/XtModifyPanel.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "gui/xt/XtSelectorDialog.h"
#include "gui/xt/XtTangibleInterfaceDialog.h"
#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/render/xt/XtGuiRenderer.h"
#include "model/GuiState.h"
#include "gui/xt/XlibLabelImageProvider.h"
#include "vsdk/toolkit/gui/XtSystem.h"
#include "io/GuiJsonReader.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "java/lang/Exception.h"
#include "java/lang/IllegalArgumentException.h"


typedef void (*SwapIntervalEXTProc)(Display*, GLXDrawable, int);

class SceneEditorApplication;

/**
Applies the pending tangible interface event, as a task of `XtEventQueue`
(lambdas in the C++11 port).
*/
class ApplyTangibleEventTask : public Runnable {
public:
    explicit ApplyTangibleEventTask(SceneEditorApplication* application)
        : application(application) {}
    virtual void run();
private:
    SceneEditorApplication* application;
};

/**
Called from the thread of the network client when a tangible interface
event arrives: applies it in the thread of the Xt event loop.
*/
class TangibleEventArrived : public Runnable {
public:
    explicit TangibleEventArrived(SceneEditorApplication* application)
        : application(application) {}
    virtual void run()
    {
        XtEventQueue::invokeLater(new ApplyTangibleEventTask(application));
    }
private:
    SceneEditorApplication* application;
};

/**
Gives the result of a connection to its listener, as a task of
`XtEventQueue`.
*/
class TangibleConnectionResultTask : public Runnable {
public:
    TangibleConnectionResultTask(Consumer<bool>* listener, bool connected)
        : listener(listener), connected(connected) {}
    virtual void run()
    {
        listener->accept(connected);
    }
private:
    Consumer<bool>* listener;
    bool connected;
};

/**
Receives the result of a connection in the thread of the network client,
and gives it to the listener of the GUI in the thread of the Xt event loop.
*/
class TangibleConnectionForwarder : public Consumer<bool> {
public:
    explicit TangibleConnectionForwarder(Consumer<bool>* listener)
        : listener(listener) {}
    virtual void accept(bool connected)
    {
        XtEventQueue::invokeLater(
            new TangibleConnectionResultTask(listener, connected));
    }
private:
    Consumer<bool>* listener;
};

/**
Owns the forwarder of the current connection. Declared before the
connection in the application, so it is destroyed after it (and its
network thread).
*/
class TangibleConnectionForwarderSlot {
public:
    TangibleConnectionForwarderSlot() : forwarder(NULL) {}
    ~TangibleConnectionForwarderSlot() { delete forwarder; }
    TangibleConnectionForwarder* forwarder;
private:
    TangibleConnectionForwarderSlot(const TangibleConnectionForwarderSlot&);
    TangibleConnectionForwarderSlot& operator=(const TangibleConnectionForwarderSlot&);
};

struct TabBinding {
    SceneEditorApplication* application;
    int page;
};

/**
The scene editor with an Xt GUI and an OpenGL 1.2 fixed function drawing
area (the turboc port has no OpenGL 4.1 variant): the composition root of
`AwtJogl4SceneEditorApplication`, `AwtJogl4GuiController` and
`AwtJogl4ApplicationController` of the Java application. The model, its
interaction techniques and its renderer are behind the
`XtOpenGL1SceneBridge`, which has no Xt dependency.

The GUI is built through the `XtWidgetSet` of the `XtUiFactory` it is
given: the widget set (Athena or Motif) is chosen when building the
application, and this class only uses the Xt Intrinsics.
*/
class SceneEditorApplication :
    public XtOpenGLSceneEditorApplication,
    public XtOpenGLApplicationController,
    private XtOpenGLSceneBridge::Listener,
    private XtApplicationHost
{
public:
    explicit SceneEditorApplication(XtUiFactory* uiFactory)
        : uiFactory(uiFactory)
        , widgetSet(uiFactory->getWidgetSet())
        , panelWidgets(widgetSet->getPanelWidgets())
        , guiRenderer(widgetSet->getGuiRenderer())
        , appContext(NULL)
        , display(NULL)
        , shell(NULL)
        , workspace(NULL)
        , menuBar(NULL)
        , drawingCanvas(NULL)
        , visual(NULL)
        , visualDepth(0)
        , colormap(0)
        , menuFontSet(NULL)
        , sectionFontSet(NULL)
        , rightPanel(NULL)
        , creationPage(NULL)
        , modifyPage(NULL)
        , guiPage(NULL)
        , othersPage(NULL)
        , renderPage(NULL)
        , modifyPanel(NULL)
        , globalBar(NULL)
        , statusBar(NULL)
        , executor(NULL)
        , imageControlWindow(NULL)
        , selectorDialog(NULL)
        , tangibleDialog(NULL)
        , tangibleEventArrived(this)
        , tangibleConnection(&tangibleEventArrived)
        , automationService(NULL)
        , viewportMenu(NULL)
        , labelProvider(NULL)
        , window(0)
        , cpuShellMode(false)
        , fbConfig(NULL)
        , context(NULL)
        , width(640)
        , height(480)
        , canvasWidth(320)
        , canvasHeight(452)
        , guiLanguage(selectedGuiLanguage())
        , pendingGuiLanguage()
        , guiRebuildQueued(false)
        , sceneBridge(NULL)
        , ready(false)
        , closing(false)
        , repaintQueued(false)
        , currentCursor(PointerCursor::SELECT)
        , viewportMenuX(0)
        , viewportMenuY(0)
        , pressButton(0)
        , pressX(0)
        , pressY(0)
        , pressMoved(false)
        , lastClickButton(0)
        , lastClickTime(0)
        , clickCount(0)
    {
    }

    ~SceneEditorApplication()
    {
        // The automation service lives as long as the process (as in Java)
        delete imageControlWindow;
        imageControlWindow = NULL;
        delete selectorDialog;
        selectorDialog = NULL;
        delete tangibleDialog;
        tangibleDialog = NULL;
        cleanupOpenGL();
        // The editors stop listening to the entities before the scene goes
        if (sceneBridge != NULL) sceneBridge->setBodyEditFeedbackProvider(NULL);
        delete modifyPanel;
        modifyPanel = NULL;
        if (sceneBridge != NULL) {
            if (display != NULL && context != NULL && window != 0) {
                glXMakeCurrent(display, window, context);
                sceneBridge->dispose();
            }
            delete sceneBridge;
            sceneBridge = NULL;
        }
        delete labelProvider;
        labelProvider = NULL;
        if (context != NULL) {
            glXMakeCurrent(display, None, NULL);
            glXDestroyContext(display, context);
            context = NULL;
        }
        if (shell != NULL) {
            XtDestroyWidget(shell);
            shell = NULL;
        }
        globalBar = NULL;
        delete executor;
        executor = NULL;
        clearSideBindings();
        if (display != NULL) {
            for (long i = 0; i < cursors.size(); i++)
                XFreeCursor(display, cursors.getValue(i));
            cursors.clear();
        }
        clearCreationSections();
        if (display != NULL && sectionFontSet != NULL) {
            XFreeFontSet(display, sectionFontSet);
            sectionFontSet = NULL;
        }
        if (display != NULL && menuFontSet != NULL) {
            XFreeFontSet(display, menuFontSet);
            menuFontSet = NULL;
        }
        if (display != NULL) {
            // Frees the colors cached by the Xt converters in the colormap:
            // it goes with the connection, afterwards
            XtCloseDisplay(display);
            display = NULL;
            colormap = 0;
        }
    }

    int run(int argc, char** argv)
    {
        // Before Xt takes its own options from the command line
        bool withAutomationService = false;
        for (int i = 1; i < argc; ++i)
            if (String(argv[i]) == "-s") withAutomationService = true;
        tangibleConnection.processArguments(argc, argv);
        try {
            createWindow(argc, argv);
            createContext();
            initOpenGL();
            redraw();
            if (withAutomationService) {
                // Starts its own listener thread, which keeps it alive
                automationService = new XtOpenGLVitralEditorMCP(this);
            }
            XtAppMainLoop(appContext);
        }
        catch (const VSDKFatalException& ex) {
            fprintf(stderr, "SceneEditorApplication fatal error: %s\n", ex.getMessage().c_str());
            return 1;
        }
        catch (const Exception& ex) {
            fprintf(stderr, "SceneEditorApplication error: %s\n", ex.getMessage().c_str());
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
    // Bold font set of the headers of the collapsible sections (the menu
    // one if the display has no bold font)
    XFontSet sectionFontSet;
    ArrayList<XtCollapsibleSection*> creationSections;
    Widget rightPanel;
    Widget creationPage;
    Widget modifyPage;
    Widget guiPage;
    Widget othersPage;
    Widget renderPage;
    XtModifyPanel* modifyPanel;
    Widget globalBar;
    Widget statusBar;
    XtOpenGLGuiEventExecutor* executor;
    XtImageControlWindow* imageControlWindow;
    XtSelectorDialog* selectorDialog;
    XtTangibleInterfaceDialog* tangibleDialog;
    // Must be built before (and destroyed after) the connection
    TangibleEventArrived tangibleEventArrived;
    TangibleConnectionForwarderSlot tangibleConnectionForwarder;
    TangibleInterfaceConnection tangibleConnection;
    XtOpenGLVitralEditorMCP* automationService;
    Widget viewportMenu;
    /// Commands of the items of the viewport menu shown
    ArrayList<String> viewportMenuCommands;
    XlibLabelImageProvider* labelProvider;
    Window window;
    bool cpuShellMode;
    GLXFBConfig fbConfig;
    GLXContext context;
    int width;
    int height;
    int canvasWidth;
    int canvasHeight;
    String guiLanguage;
    String guiDefinition;
    String pendingGuiLanguage;
    bool guiRebuildQueued;
    ArrayList<TabBinding*> tabBindings;
    TreeMap<String, String> messages;
    XtOpenGLSceneBridge* sceneBridge;
    bool ready;
    bool closing;
    bool repaintQueued;
    TreeMap<int, Cursor> cursors;
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

    Widget activeCanvas() const { return drawingCanvas; }

    static String selectedGuiLanguage()
    {
        const char* language = getenv("SCENE_EDITOR_GUI_LANGUAGE");
        return language != NULL ? language : "english";
    }

    static void selectGuiLocale()
    {
        const char* overrideLocale = getenv("SCENE_EDITOR_GUI_LOCALE");
        // UI text is UTF-8 regardless of the selected JSON language.  A
        // neutral Unicode locale lets English labels such as "Español" and
        // future translations share one FontSet encoding.
        if (overrideLocale != NULL &&
            setlocale(LC_CTYPE, overrideLocale) != NULL) return;
        if (setlocale(LC_CTYPE, "C.UTF-8") != NULL) return;
        setlocale(LC_CTYPE, "");
    }

    static void selectSideTab(Widget, void* clientData)
    {
        TabBinding* binding = ((TabBinding*)(clientData));
        if (binding != NULL && binding->application != NULL)
            binding->application->showSidePage(binding->page);
    }

    /**
    Reads the I18N file of the language of the GUI, giving it to the model
    as its I18N context (as `AwtJogl4GuiController.loadGuiDefinition`).
    */
    void loadGuiDefinition()
    {
        const String path = GuiState::languageFile(guiLanguage.c_str()).c_str();
        if (!Files::readString(path, guiDefinition)) {
            throw VSDKFatalException(
                String(("Could not open Java GUI definition: " + path).c_str()));
        }
        messages = GuiJsonReader(guiDefinition).readMessages();
        sceneBridge->getGuiState()->setLanguageGuiFile(
            GuiState::languageFile(guiLanguage.c_str()));
        // Viewport titles, their menus and the HUDs follow the language
        sceneBridge->setGuiDefinition(guiDefinition);
    }

    void clearSideBindings()
    {
        for ( long i = 0; i < tabBindings.size(); ++i)
            delete tabBindings[i];
        tabBindings.clear();
    }

    String text(const char* id) const
    {
        const String* message = messages.get(id);
        return message != NULL ? (*message) : String(id);
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
        if (globalBar != NULL)
            XtVaGetValues(globalBar, XtNheight, &globalBarHeight, NULL);
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
            if (pages[i] == NULL) return;
        for (int i = 0; i < 5; ++i)
            if (i != page) XtUnmanageChild(pages[i]);
        XtManageChild(pages[page]);
        setModifyPanelSelected(page == MODIFY_PAGE);
    }

    void setModifyPanelSelected(bool selected)
    {
        if (sceneBridge == NULL) return;
        sceneBridge->setModifyPanelSelected(selected);
        if (selected) reportTargetToModifyPanel();
    }

    /**
    Notifies the modify panel of the target it must edit: the first selected
    body while the modify page is shown, or none.
    */
    void reportTargetToModifyPanel()
    {
        if (sceneBridge == NULL || modifyPanel == NULL) return;
        SimpleBody* target = sceneBridge->getModifyPanelTarget();
        if (target != NULL)
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
            TabBinding* binding = new TabBinding;
            binding->application = this;
            binding->page = i;
            tabBindings.add(binding);
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
        if (sceneBridge != NULL) {
            sceneBridge->setBodyEditFeedbackProvider(modifyPanel);
            sceneBridge->setModifyPanelSelected(false);
        }

        // As the `AwtButtonsPanel`s of the tabs
        createCreationSections();

        const char* groups[] = { "GUI", "OTHER", "RENDER" };
        Widget pages[] = { guiPage, othersPage, renderPage };
        for (int i = 0; i < 3; ++i) {
            guiRenderer->buildButtonGroup(
                pages[i], sceneBridge->getButtonGroup(groups[i]), executor,
                menuFontSet, 0, 0, SIDE_PANEL_WIDTH);
        }
    }

    /**
    Adds the creation commands as two collapsible sections, as
    `AwtButtonsPanel.addCreationSections`: the creation of geometries, and
    the other creation and exchange operations.
    */
    void createCreationSections()
    {
        const char* titles[] = {
            "IDM_CREATION_GEOMETRY_SECTION", "IDM_CREATION_OTHER_SECTION"
        };
        const char* groups[] = { "CREATION_GEOMETRY", "CREATION_OTHER" };
        int y = 0;
        for (int i = 0; i < 2; ++i) {
            XtCollapsibleSection* section = new XtCollapsibleSection(
                panelWidgets, creationPage, text(titles[i]),
                sectionFontSet != NULL ? sectionFontSet : menuFontSet,
                y, SIDE_PANEL_WIDTH, true);
            section->setContent(guiRenderer->buildButtonGroup(
                section->getContainer(), sceneBridge->getButtonGroup(groups[i]),
                executor, menuFontSet, 0, 0, SIDE_PANEL_WIDTH));
            section->setLayoutListener(
                &SceneEditorApplication::creationSectionResized, this);
            creationSections.add(section);
            y += section->getHeight();
        }
    }

    static void creationSectionResized(XtCollapsibleSection*, void* clientData)
    {
        SceneEditorApplication* self =
            ((SceneEditorApplication*)(clientData));
        if (self == NULL || self->creationSections.isEmpty()) return;
        XtCollapsibleSection::stack(
            &self->creationSections[0],
            ((int)(self->creationSections.size())), 0);
    }

    /**
    Deletes the sections of the creation page (their widgets go with the
    page).
    */
    void clearCreationSections()
    {
        for ( long i = 0; i < creationSections.size(); ++i)
            delete creationSections[i];
        creationSections.clear();
    }

    void rebuildRightPanel()
    {
        if (sceneBridge != NULL) sceneBridge->setBodyEditFeedbackProvider(NULL);
        delete modifyPanel;
        modifyPanel = NULL;
        if (rightPanel != NULL) XtDestroyWidget(rightPanel);
        rightPanel = NULL;
        clearCreationSections();
        creationPage = NULL;
        modifyPage = NULL;
        guiPage = NULL;
        othersPage = NULL;
        renderPage = NULL;
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
        if (globalBar != NULL) XtDestroyWidget(globalBar);
        globalBar = NULL;
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
        SceneEditorApplication* self = ((SceneEditorApplication*)(clientData));
        if (self == NULL) return;
        self->guiRebuildQueued = false;
        self->rebuildMenus(self->pendingGuiLanguage);
    }

    void scheduleMenuRebuild(const String& language)
    {
        if (language == guiLanguage || guiRebuildQueued) return;
        pendingGuiLanguage = language;
        guiRebuildQueued = true;
        XtAppAddTimeOut(
            appContext, 0, &SceneEditorApplication::rebuildMenusAfterCallback,
            ((XtPointer)(this)));
    }

    void rebuildMenus(const String& language)
    {
        if (language == guiLanguage || workspace == NULL) return;
        guiLanguage = language;
        if (menuBar != NULL) {
            XtDestroyWidget(menuBar);
            menuBar = NULL;
        }
        if (menuFontSet != NULL) {
            XFreeFontSet(display, menuFontSet);
            menuFontSet = NULL;
        }
        if (sectionFontSet != NULL) {
            XFreeFontSet(display, sectionFontSet);
            sectionFontSet = NULL;
        }
        selectGuiLocale();
        createMenuFontSet();
        loadGuiDefinition();
        createMenuBar();
        rebuildGlobalBar();
        rebuildRightPanel();
        // Widgets with texts use the font set: all of them are created again,
        // as the Java application rebuilds its whole GUI
        if (statusBar != NULL) XtDestroyWidget(statusBar);
        createStatusBar();
        delete imageControlWindow;
        imageControlWindow = NULL;
        delete selectorDialog;
        selectorDialog = NULL;
        // Its form uses the font set: built again when shown
        if (tangibleDialog != NULL) tangibleDialog->discard();
        applyFullScreenGuiMode();
        requestRedraw();
    }

    void createMenuFontSet()
    {
        char** missingCharsets = NULL;
        int missingCharsetCount = 0;
        char* defaultString = NULL;
        menuFontSet = XCreateFontSet(
            display,
            "-adobe-helvetica-medium-r-normal--14-*-*-*-*-*-iso8859-1",
            &missingCharsets,
            &missingCharsetCount,
            &defaultString);
        if (missingCharsets != NULL) XFreeStringList(missingCharsets);
        if (menuFontSet == NULL) {
            throw VSDKFatalException(
                "Could not create the UTF-8 menu font set");
        }

        // Headers of the sections are bold, as in `AwtCollapsibleSection`
        missingCharsets = NULL;
        missingCharsetCount = 0;
        sectionFontSet = XCreateFontSet(
            display,
            "-adobe-helvetica-bold-r-normal--14-*-*-*-*-*-iso8859-1",
            &missingCharsets,
            &missingCharsetCount,
            &defaultString);
        if (missingCharsets != NULL) XFreeStringList(missingCharsets);
    }

    /**
    Merges the resources of the look of the GUI (the black and white one of
    the Athena GUI) over the ones of the display, so they win over the
    user's defaults. Widget sets keeping their own look (Motif) have none.
    */
    void loadResourceFile()
    {
        const char* path = uiFactory->getResourceFile();
        if (path == NULL) return;
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
        XtEventQueue::install(appContext);
        display = XtOpenDisplay(appContext, NULL, "scene-editor",
            "SceneEditorApplication", NULL, 0, &argc, argv);
        if (display == NULL)
            throw VSDKFatalException("Could not open X display. Is DISPLAY set?");
        loadResourceFile();
        const int screen = DefaultScreen(display);
        width = DisplayWidth(display, screen);
        height = DisplayHeight(display, screen);
        createMenuFontSet();
        buildShell(false);
    }

    void buildShell(bool cpu)
    {
        const int screen = DefaultScreen(display);
        if (cpu) {
            visual = DefaultVisual(display, screen);
            visualDepth = DefaultDepth(display, screen);
            colormap = DefaultColormap(display, screen);
            fbConfig = NULL;
        }
        else
        {
            XVisualInfo* visualInfo = chooseVisual();
            visual = visualInfo->visual;
            visualDepth = visualInfo->depth;
            colormap = XCreateColormap(display,
                RootWindow(display, visualInfo->screen), visual, AllocNone);
            XFree(visualInfo);
        }

        Arg args[9];
        Cardinal n = 0;
        XtSetArg(args[n], XtNinput, True); ++n;
        XtSetArg(args[n], XtNvisual, visual); ++n;
        XtSetArg(args[n], XtNcolormap, colormap); ++n;
        XtSetArg(args[n], XtNdepth, visualDepth); ++n;
        XtSetArg(args[n], XtNwidth, width); ++n;
        XtSetArg(args[n], XtNheight, height); ++n;
        const String title = String("VITRAL Scene Editor - ") +
            widgetSet->getName() + (cpu ? " Xlib CPU hidden lines" :
                                        " GLX OpenGL 1.2");
        XtSetArg(args[n], XtNtitle, title.c_str()); ++n;
        XtSetArg(args[n], XtNx, 0); ++n;
        XtSetArg(args[n], XtNy, 0); ++n;
        shell = XtAppCreateShell("sceneEditor", "SceneEditorApplication",
                                 applicationShellWidgetClass, display, args, n);
        if (shell == NULL)
            throw VSDKFatalException("Could not create Xt shell");
        workspace = panelWidgets->createPanel(shell, "workspace", 0, 0,
                                              width, height, true);
        widgetSet->setWindowCloseHandler(shell,
            &SceneEditorApplication::closeRequestedByWindowManager, this);

        if (sceneBridge == NULL) {
            labelProvider = new XlibLabelImageProvider(display);
            sceneBridge = new XtOpenGLSceneBridge(labelProvider, this);
            loadGuiDefinition();
            executor = new XtOpenGLGuiEventExecutor(this);
        }
        createMenuBar();
        createGlobalBar();
        createRightPanel();
        createStatusBar();
        canvasWidth = width - SIDE_PANEL_WIDTH;
        canvasHeight = height - contentTop() - STATUS_BAR_HEIGHT;
        drawingCanvas = widgetSet->createDrawingArea(workspace,
            "drawingCanvas", 0, contentTop(), canvasWidth, canvasHeight);
        XtAddEventHandler(drawingCanvas,
            ExposureMask | StructureNotifyMask |
            KeyPressMask | KeyReleaseMask |
            ButtonPressMask | ButtonReleaseMask | PointerMotionMask |
            EnterWindowMask, False, &SceneEditorApplication::eventHandler,
            ((XtPointer)(this)));
        widgetSet->setKeyboardFocus(shell, drawingCanvas);
        XtRealizeWidget(shell);
        window = XtWindow(drawingCanvas);
        widgetSet->setKeyboardFocus(shell, drawingCanvas);
        cpuShellMode = cpu;
    }

    void rebuildShell(bool cpu)
    {
        if (cpu == cpuShellMode) return;
        ready = false;
        if (context != NULL) {
            glXMakeCurrent(display, window, context);
            sceneBridge->dispose();
            glXMakeCurrent(display, None, NULL);
            glXDestroyContext(display, context);
            context = NULL;
        }
        sceneBridge->setBodyEditFeedbackProvider(NULL);
        delete modifyPanel;
        modifyPanel = NULL;
        clearCreationSections();
        clearSideBindings();
        delete imageControlWindow;
        imageControlWindow = NULL;
        delete selectorDialog;
        selectorDialog = NULL;
        if (tangibleDialog != NULL) tangibleDialog->discard();
        if (shell != NULL) XtDestroyWidget(shell);
        XSync(display, False);
        shell = workspace = menuBar = drawingCanvas = NULL;
        rightPanel = creationPage = modifyPage = guiPage = NULL;
        othersPage = renderPage = globalBar = statusBar = NULL;
        viewportMenu = NULL;
        viewportMenuCommands.clear();
        window = 0;
        if (colormap != 0 && colormap != DefaultColormap(display, DefaultScreen(display)))
            XFreeColormap(display, colormap);
        colormap = 0;
        buildShell(cpu);
        if (!cpu) {
            XSync(display, False);
            createContext();
            initOpenGL();
        }
        else {
            ready = true;
        }
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
        if (configs == NULL || count == 0) {
            throw VSDKFatalException("No suitable GLX framebuffer config found");
        }

        XVisualInfo* visualInfo = NULL;
        for (int i = 0; i < count; ++i) {
            visualInfo = glXGetVisualFromFBConfig(display, configs[i]);
            if (visualInfo != NULL) {
                fbConfig = configs[i];
                break;
            }
        }

        XFree(configs);
        if (visualInfo == NULL) {
            throw VSDKFatalException("No X visual found for GLX framebuffer config");
        }
        return visualInfo;
    }

    void createContext()
    {
        // A legacy context: it offers the OpenGL 1.2 fixed function
        // pipeline (as XQuartz does on macOS)
        context = glXCreateNewContext(display, fbConfig, GLX_RGBA_TYPE,
                                      NULL, True);
        if (context == NULL) {
            throw VSDKFatalException("Could not create an OpenGL GLX context");
        }
        makeContextCurrentWithVSync();
    }

    void makeContextCurrentWithVSync()
    {
        if (!glXMakeCurrent(display, window, context)) {
            throw VSDKFatalException("Could not make GLX context current");
        }

        // glXGetProcAddressARB gives an address for any name (i.e. Mesa
        // 6.5, without GLX_EXT_swap_control): the extension is checked first
        if (!hasGlxExtension("GLX_EXT_swap_control")) {
            return;
        }
        SwapIntervalEXTProc swapInterval =
            ((SwapIntervalEXTProc)(
                glXGetProcAddressARB(
                    ((const GLubyte*)("glXSwapIntervalEXT")))));
        if (swapInterval != NULL) {
            swapInterval(display, window, 1);
        }
    }

    bool hasGlxExtension(const char* name)
    {
        const char* extensions =
            glXQueryExtensionsString(display, DefaultScreen(display));
        if (extensions == NULL) {
            return false;
        }
        size_t length = strlen(name);
        const char* found = extensions;
        while ((found = strstr(found, name)) != NULL) {
            if ((found == extensions || found[-1] == ' ') &&
                (found[length] == ' ' || found[length] == '\0')) {
                return true;
            }
            found += length;
        }
        return false;
    }

    void initOpenGL()
    {
        // The functions are exported by the system OpenGL library: only
        // the version of the context is checked
        if (!OpenGL1Loader::load()) {
            throw VSDKFatalException("The context does not offer OpenGL 1.2");
        }

        checkOpenGLVersion();
        sceneBridge->setCanvasSize(canvasWidth, canvasHeight);
        sceneBridge->init();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glViewport(0, 0, canvasWidth, canvasHeight);
        ready = true;
    }

    void checkOpenGLVersion()
    {
        const char* versionStr = ((const char*)(glGetString(GL_VERSION)));
        const char* rendererStr = ((const char*)(glGetString(GL_RENDERER)));
        const char* vendorStr = ((const char*)(glGetString(GL_VENDOR)));
        printf("OpenGL vendor: %s\n", vendorStr != NULL ? vendorStr : "(unknown)");
        printf("OpenGL renderer: %s\n", rendererStr != NULL ? rendererStr : "(unknown)");
        printf("OpenGL version string: %s\n", versionStr != NULL ? versionStr : "(unknown)");

        // GL_MAJOR_VERSION does not exist before OpenGL 3.0
        int major = 0;
        int minor = 0;
        if (versionStr != NULL) {
            sscanf(versionStr, "%d.%d", &major, &minor);
        }
        printf("OpenGL parsed version: %d.%d\n", major, minor);

        if (major < 1 || (major == 1 && minor < 2)) {
            Logger::reportMessage(
                "SceneEditorApplication",
                Logger::ERROR,
                "checkOpenGLVersion",
                "OpenGL version too old");
            throw VSDKFatalException("This example requires OpenGL 1.2+");
        }
    }

    void redraw()
    {
        if (!ready || display == NULL) return;
        if (sceneBridge != NULL &&
            sceneBridge->usesXlibRendering() != cpuShellMode)
            rebuildShell(sceneBridge->usesXlibRendering());
        if (cpuShellMode) {
            const int screen = DefaultScreen(display);
            sceneBridge->setScreenResolution(DisplayWidth(display, screen),
                                             DisplayHeight(display, screen));
            sceneBridge->displayXlib(display, window, visual, visualDepth,
                                     canvasWidth, canvasHeight);
            return;
        }
        if (context == NULL) return;
        glXMakeCurrent(display, window, context);
        glViewport(0, 0, canvasWidth, canvasHeight);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        if (sceneBridge != NULL) {
            // X11 reports the screen in the pixels text is drawn with
            const int screen = DefaultScreen(display);
            sceneBridge->setScreenResolution(DisplayWidth(display, screen),
                                             DisplayHeight(display, screen));
            sceneBridge->display(canvasWidth, canvasHeight);
        }
        else {
            glLineWidth(1.0f);
            glColor3f(1.0f, 1.0f, 1.0f);
            glBegin(GL_LINES);
            glVertex3f(-0.8f, -0.8f, 0.0f);
            glVertex3f(0.8f, 0.8f, 0.0f);
            glEnd();
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
        gettimeofday(&now, NULL);
        return ((long long)(now.tv_sec)) * 1000 + now.tv_usec / 1000;
    }

    static void closeRequestedByWindowManager(void* clientData)
    {
        ((SceneEditorApplication*)(clientData))->requestClose();
    }

    static void eventHandler(
        Widget,
        XtPointer clientData,
        XEvent* event,
        Boolean*)
    {
        SceneEditorApplication* self =
            ((SceneEditorApplication*)(clientData));
        if (self == NULL) {
            return;
        }
        try {
            self->processEvent(event);
        }
        catch (const Exception& ex) {
            fprintf(stderr, "SceneEditorApplication: event processing failed: %s\n", ex.getMessage().c_str());
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
            if (sceneBridge != NULL) {
                sceneBridge->setCanvasSize(canvasWidth, canvasHeight);
            }
            redraw();
            break;
        case EnterNotify:
            if (sceneBridge != NULL) {
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
            if (sceneBridge != NULL) {
                keyPressed(XtSystem::xt2vsdkKeyEvent(event->xkey),
                           XLookupKeysym(&event->xkey, 0), event->xkey.state);
            }
            break;
        case KeyRelease:
            if (sceneBridge != NULL && !isAutoRepeatRelease(event->xkey)) {
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
        if (sceneBridge == NULL) return;
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
        widgetSet->setKeyboardFocus(shell, activeCanvas());
        pressButton = event.xbutton.button;
        pressX = event.xbutton.x;
        pressY = event.xbutton.y;
        pressMoved = false;
        sceneBridge->mousePressed(XtSystem::xt2vsdkMouseEvent(event));
        requestRedraw();
    }

    void processButtonRelease(const XEvent& event)
    {
        if (sceneBridge == NULL || XtSystem::isWheelButton(event)) return;
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
        if (sceneBridge == NULL) return;
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
        SceneEditorApplication* self = ((SceneEditorApplication*)(clientData));
        if (self == NULL) return;
        self->repaintQueued = false;
        self->redraw();
    }

    /**
    Coalesces the repaints requested while processing a burst of events in
    one redraw, done once the pending events are processed.
    */
    void requestRedraw()
    {
        if (repaintQueued || appContext == NULL) return;
        repaintQueued = true;
        XtAppAddTimeOut(appContext, 0, &SceneEditorApplication::redrawAfterEvents,
                        ((XtPointer)(this)));
    }

    //= Viewport menu =====================================================

    static void executeViewportMenuCommand(int index, void* clientData)
    {
        SceneEditorApplication* self = ((SceneEditorApplication*)(clientData));
        if (self->sceneBridge == NULL || index < 0 ||
            index >= ((int)(self->viewportMenuCommands.size()))) return;
        self->sceneBridge->executeViewportCommand(self->viewportMenuCommands[index]);
        self->requestRedraw();
    }

    static void viewportMenuPoppedDown(void* clientData)
    {
        SceneEditorApplication* self = ((SceneEditorApplication*)(clientData));
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
        if (viewportMenu != NULL) {
            XtDestroyWidget(viewportMenu);
            viewportMenu = NULL;
        }
        const ArrayList<XtOpenGLSceneBridge::ViewportMenuItem> items =
            sceneBridge->getViewportMenuItems();
        if (items.isEmpty()) return;

        ArrayList<XtWidgetSet::MenuChoice> choices;
        viewportMenuCommands.clear();
        for ( long i = 0; i < items.size(); ++i) {
            XtWidgetSet::MenuChoice choice;
            choice.label = items[i].label;
            choice.checked = items[i].current;
            choice.separator = items[i].separator;
            choices.add(choice);
            viewportMenuCommands.add(items[i].command);
        }
        viewportMenu = widgetSet->createChoiceMenu(
            activeCanvas(), choices, menuFontSet,
            &SceneEditorApplication::executeViewportMenuCommand, this);

        Position rootX = 0;
        Position rootY = 0;
        XtTranslateCoords(activeCanvas(), ((Position)(canvasX)),
                          ((Position)(canvasY)), &rootX, &rootY);
        widgetSet->popupChoiceMenu(viewportMenu, rootX, rootY,
                                   &SceneEditorApplication::viewportMenuPoppedDown,
                                   this);
        popupDismissFilter.popupShown();
    }

    //= XtOpenGLSceneBridge::Listener =====================================

    void repaintRequested()
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
        Cursor* known = cursors.get(((int)(shape)));
        if (known != NULL) return (*known);
        Cursor created = XCreateFontCursor(display, shape);
        cursors.put(((int)(shape)), created);
        return created;
    }

    void cursorRequested(PointerCursor::Value cursor)
    {
        if (cursor == currentCursor || window == 0) return;
        currentCursor = cursor;
        XDefineCursor(display, window, getCursor(cursor));
    }

    void cursorWarpRequested(int canvasX, int canvasY)
    {
        if (window == 0) return;
        XWarpPointer(display, None, window, 0, 0, 0, 0, canvasX, canvasY);
    }

    void statusMessageRequested(const String& message)
    {
        showStatusMessage(message);
    }

    void closeRequested()
    {
        requestClose();
    }

    void selectionChanged()
    {
        reportTargetToModifyPanel();
    }

    /**
    Switches between showing only the drawing area and showing all the
    GUI, as `AwtDrawingAreaFeedback.fullScreenGuiToggleRequested` (which
    rebuilds the Swing GUI).
    */
    void fullScreenGuiToggleRequested()
    {
        if (sceneBridge == NULL) return;
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
        if (sceneBridge == NULL || activeCanvas() == NULL) return;
        const bool fullScreen = sceneBridge->getGuiState()->isFullScreenGuiMode();
        Widget others[] = {
            menuBar, globalBar,
            rightPanel, statusBar
        };
        for (int i = 0; i < 4; ++i) {
            if (others[i] == NULL) continue;
            if (fullScreen) XtUnmanageChild(others[i]);
            else XtManageChild(others[i]);
        }
        if (fullScreen) {
            if (drawingCanvas != NULL)
                XtConfigureWidget(drawingCanvas, 0, 0, width, height, 0);
        }
        else {
            if (drawingCanvas != NULL)
                XtConfigureWidget(drawingCanvas, 0, contentTop(),
                                  width - SIDE_PANEL_WIDTH,
                                  height - contentTop() - STATUS_BAR_HEIGHT, 0);
        }
        setWindowManagerFullScreen(fullScreen);
        widgetSet->setKeyboardFocus(shell, activeCanvas());
    }

    /**
    Asks the window manager (EWMH) to show the window without decorations
    over the whole screen, as the undecorated frame of the Java application.
    */
    void setWindowManagerFullScreen(bool fullScreen)
    {
        if (shell == NULL || !XtIsRealized(shell)) return;
        XEvent event;
        memset(&event, 0, sizeof(event));
        event.xclient.type = ClientMessage;
        event.xclient.window = XtWindow(shell);
        event.xclient.message_type = XInternAtom(display, "_NET_WM_STATE", False);
        event.xclient.format = 32;
        event.xclient.data.l[0] = fullScreen ? 1 : 0;
        event.xclient.data.l[1] = ((long)(
            XInternAtom(display, "_NET_WM_STATE_FULLSCREEN", False)));
        event.xclient.data.l[3] = 1;
        XSendEvent(display, DefaultRootWindow(display), False,
                   SubstructureRedirectMask | SubstructureNotifyMask, &event);
    }

    void raytracingRequested()
    {
        showStatusMessage(getMessage("IDM_COMPUTING_RAYTRACING"));
        doRaytracingImage();
        showImage(sceneBridge->getRaytracedImage());
    }

    void selectorDialogRequested()
    {
        if (selectorDialog == NULL) selectorDialog = new XtSelectorDialog(this);
        selectorDialog->rebuild();
        selectorDialog->setVisible(true);
    }

    void imageRequested(RGBImageUncompressed* image)
    {
        showImage(image);
    }

    //= Tangible interface ================================================

    friend class ApplyTangibleEventTask;

    void applyTangibleInterfaceEvent()
    {
        if (tangibleConnection.applyPendingEvent(getApplicationModel()))
            requestRedraw();
    }

    //= XtApplicationHost =================================================

    XtOpenGLSceneBridge* getSceneBridge()
    {
        return sceneBridge;
    }

    XtUiFactory* getUiFactory()
    {
        return uiFactory;
    }

    String getMessage(const char* id)
    {
        return text(id);
    }

    void showStatusMessage(const String& message)
    {
        if (statusBar != NULL) panelWidgets->setLabel(statusBar, message);
    }

    void showImage(RGBImageUncompressed* image)
    {
        if (imageControlWindow == NULL)
            imageControlWindow = new XtImageControlWindow(this, image);
        else
            imageControlWindow->setImage(image);
        imageControlWindow->redrawImage();
    }

    Widget createDialogShell(const char* name, WidgetClass shellClass,
                             const char* title)
    {
        Arg args[4]; Cardinal n = 0;
        XtSetArg(args[n], XtNvisual, visual); ++n;
        XtSetArg(args[n], XtNdepth, visualDepth); ++n;
        XtSetArg(args[n], XtNcolormap, colormap); ++n;
        XtSetArg(args[n], XtNtitle, title); ++n;
        return XtCreatePopupShell(name, shellClass, shell, args, n);
    }

    void setGuiLanguage(const String& language)
    {
        scheduleMenuRebuild(language);
    }

    void showTangibleInterfaceDialog()
    {
        if (tangibleDialog == NULL)
            tangibleDialog = new XtTangibleInterfaceDialog(this);
        tangibleDialog->show();
    }

    String getTangibleServiceUrl()
    {
        return tangibleConnection.getServiceUrl();
    }

    void connectTangibleInterface(const String& serviceUrl,
        Consumer<bool>* connectionListener)
    {
        TangibleConnectionForwarder* previous =
            tangibleConnectionForwarder.forwarder;
        tangibleConnectionForwarder.forwarder =
            new TangibleConnectionForwarder(connectionListener);
        tangibleConnection.connect(serviceUrl,
            tangibleConnectionForwarder.forwarder);
        // connect stops the previous client (and its thread) first
        delete previous;
    }

    const WidgetDialog* getDialogById(const char* id)
    {
        return sceneBridge != NULL ? sceneBridge->getDialogById(id) : NULL;
    }

    //= XtOpenGLSceneEditorApplication ====================================

    ApplicationModel* getApplicationModel()
    {
        return sceneBridge != NULL ? sceneBridge->getApplicationModel() : NULL;
    }

    XtOpenGLApplicationController* getOpenGL1Controller()
    {
        return this;
    }

    ArrayList<String> getGuiLanguages()
    {
        return GuiState::listLanguages();
    }

    String getCurrentGuiLanguage()
    {
        return guiLanguage.c_str();
    }

    bool setGuiLanguageById(const String& language)
    {
        ArrayList<String> languages = getGuiLanguages();
        bool known = false;
        for (long i = 0; i < languages.size(); ++i)
            if (languages.get(i).equals(language)) known = true;
        if (!known) return false;
        // Not called from a widget callback: the GUI is rebuilt now
        rebuildMenus(language.c_str());
        return true;
    }

    void doRaytracingImage()
    {
        if (sceneBridge != NULL) sceneBridge->doRaytracingImage();
    }

    bool showImportSceneDialog()
    {
        return executor != NULL &&
            executor->executeCommand("IDC_IMPORT_OBJECTS_FROM_FILE");
    }

    void openSelectorDialog()
    {
        selectorDialogRequested();
    }

    bool isSelectorDialogVisible()
    {
        return selectorDialog != NULL && selectorDialog->isVisible();
    }

    void selectorSelectBody(int index)
    {
        if (selectorDialog != NULL) selectorDialog->selectBody(index);
    }

    void selectorAction(const String& action)
    {
        if (selectorDialog != NULL) selectorDialog->performAction(action.c_str());
    }

    void openTangibleInterfaceDialog()
    {
        if (executor != NULL)
            executor->executeCommand("IDC_TOOLS_TANGIBLE_INTERFACE_CLIENT");
    }

    bool isTangibleInterfaceDialogVisible()
    {
        return tangibleDialog != NULL && tangibleDialog->isVisible();
    }

    String getTangibleInterfaceDialogServiceUrl()
    {
        return tangibleDialog != NULL ?
            tangibleDialog->getServiceUrl().c_str() : "";
    }

    String getTangibleInterfaceDialogConnectLabel()
    {
        return tangibleDialog != NULL ?
            tangibleDialog->getConnectLabel().c_str() : "";
    }

    bool tangibleInterfaceDialogAction(const String& action,
                                       const String* serviceUrl)
    {
        if (tangibleDialog == NULL) return false;
        String url = serviceUrl != NULL ? serviceUrl->c_str() : "";
        return tangibleDialog->performAction(action.c_str(),
                                             serviceUrl != NULL ? &url : NULL);
    }

    void closeApplication()
    {
        requestClose();
    }

    //= XtOpenGLApplicationController =====================================

    bool isDrawingAreaCreated()
    {
        return ready && sceneBridge != NULL;
    }

    void repaint()
    {
        requestRedraw();
    }

    void exportViewportPng(const File& file)
    {
        sceneBridge->requestViewportExport(file, false);
        redraw();
    }

    void exportViewportJpg(const File& file)
    {
        sceneBridge->requestViewportExport(file, true);
        redraw();
    }

    void exportWorkspaceJpg(const File& file)
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
    void injectMouseEvent(const String& type, int x, int y,
                          int button)
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
            throw IllegalArgumentException((String("Unknown mouse event type \"") +
                type + "\". Use move, press, drag or release").c_str());
        }
        requestRedraw();
    }

    /**
    As `AwtDrawingAreaController.injectKeyEvent`: the key is mapped as the
    ones of the keyboard (see `XtSystem`), from its symbol.
    */
    void injectKeyEvent(const String& key, bool shift, bool ctrl)
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
        else if (key.equals("numenter")) { keysym = XK_KP_Enter; keyChar = '\n'; }
        else if (key.length() == 4 && key.startsWith("num") &&
                 key.charAt(3) >= '0' && key.charAt(3) <= '9') {
            keyChar = key.charAt(3);
            keysym = XK_KP_0 + (keyChar - '0');
        }
        else if (key.equals("num/")) { keysym = XK_KP_Divide; keyChar = '/'; }
        else if (key.equals("num*")) { keysym = XK_KP_Multiply; keyChar = '*'; }
        else if (key.equals("num-")) { keysym = XK_KP_Subtract; keyChar = '-'; }
        else if (key.equals("num+")) { keysym = XK_KP_Add; keyChar = '+'; }
        else if (key.equals("num.")) { keysym = XK_KP_Decimal; keyChar = '.'; }
        else {
            if (key.length() != 1) {
                throw IllegalArgumentException((String("Unknown key \"") + key +
                    "\". Use a single character, tab, enter, backspace, delete, escape, left, right, up, down, pageup, pagedown, num0..num9, num/, num*, num-, num+, num. or numenter").c_str());
            }
            keyChar = key.charAt(0);
            // Latin 1 symbols are their characters
            keysym = ((unsigned char)(keyChar));
        }

        // The symbol of the key without modifiers: the lower case letter
        KeySym baseKeysym = keysym;
        if (keysym >= XK_A && keysym <= XK_Z) baseKeysym = keysym - XK_A + XK_a;

        unsigned int state = 0;
        if (shift) state |= ShiftMask;
        if (ctrl) {
            state |= ControlMask;
            if ((keyChar >= 'a' && keyChar <= 'z') || (keyChar >= 'A' && keyChar <= 'Z')) {
                char upper = keyChar >= 'a' ? ((char)(keyChar - 'a' + 'A')) : keyChar;
                keyChar = ((char)(upper - 'A' + 1));
            }
        }
        char text[2] = { keyChar, 0 };
        // Delivered directly: it does not depend on the focus of the window
        keyPressed(XtSystem::xt2vsdkKeyEvent(keysym, baseKeysym, text,
                                             keyChar != 0 ? 1 : 0, state),
                   baseKeysym, state);
    }

    bool projectToCanvas(Viewport* viewport, const Vector3Dd& point,
                         double outCanvas[2])
    {
        sceneBridge->setCanvasSize(canvasWidth, canvasHeight);
        return sceneBridge->projectToCanvas(viewport, point, outCanvas);
    }

    //= XtModifyPanelHost =================================================

    void repaintDrawingArea()
    {
        requestRedraw();
    }

    XFontSet getPanelFontSet()
    {
        return menuFontSet;
    }

    XtPanelWidgets* getPanelWidgets()
    {
        return panelWidgets;
    }

    static void showViewportMenuAfterEvent(XtPointer clientData, XtIntervalId*)
    {
        SceneEditorApplication* self = ((SceneEditorApplication*)(clientData));
        if (self != NULL && self->sceneBridge != NULL)
            self->showViewportMenu(self->viewportMenuX, self->viewportMenuY);
    }

    void viewportMenuRequested(int canvasX, int canvasY)
    {
        // Requested while dispatching the release over the title: popped
        // up once it is dispatched, or Xt would also deliver that release
        // to the new menu, closing it
        viewportMenuX = canvasX;
        viewportMenuY = canvasY;
        XtAppAddTimeOut(appContext, 0, &SceneEditorApplication::showViewportMenuAfterEvent,
                        ((XtPointer)(this)));
    }


    void cleanupOpenGL()
    {
        if (display != NULL && context != NULL && window != 0) {
            glXMakeCurrent(display, window, context);
        }
        ready = false;
    }
};

void ApplyTangibleEventTask::run()
{
    application->applyTangibleInterfaceEvent();
}

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

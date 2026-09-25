#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <exception>
#include <clocale>
#include <fstream>
#include <map>
#include <string>
#include <vector>
#include <sys/time.h>

#include <GL/glew.h>
#include <GL/glx.h>
#include <X11/Intrinsic.h>
#include <X11/Shell.h>
#include <X11/StringDefs.h>
#include <X11/Xresource.h>
#include <X11/cursorfont.h>
#include <X11/Xaw/MenuButton.h>
#include <X11/Xaw/Command.h>
#include <X11/Xaw/Label.h>
#include <X11/Xaw/SimpleMenu.h>
#include <X11/Xaw/SmeBSB.h>
#include <X11/Xaw/SmeLine.h>
#include <X11/Composite.h>

#include "java/lang/String.h"
#include "gui/PopupDismissClickFilter.h"
#include "gui/xt/XtModifyPanel.h"
#include "gui/xt/XtModifyPanelHost.h"
#include "gui/xt/XlibLabelImageProvider.h"
#include "gui/xt/XtEventMapper.h"
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

class SceneEditorApplication;

struct SubmenuBinding {
    SceneEditorApplication* application;
    Widget popup;
};

struct CommandBinding {
    SceneEditorApplication* application;
    std::string identifier;
};

struct TabBinding {
    SceneEditorApplication* application;
    int page;
};

class SceneEditorApplication :
    private XtOpenGL4SceneBridge::Listener,
    private XtModifyPanelHost
{
public:
    SceneEditorApplication()
        : appContext(nullptr)
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
        , pendingPage(nullptr)
        , modifyPanel(nullptr)
        , viewportMenu(nullptr)
        , checkMarkBitmap(None)
        , labelProvider(nullptr)
        , window(0)
        , fbConfig(nullptr)
        , context(nullptr)
        , wmDeleteWindow(None)
        , shaderProgramId(0)
        , vertexArrayId(0)
        , vertexBufferId(0)
        , width(640)
        , height(480)
        , canvasWidth(320)
        , canvasHeight(452)
        , guiLanguage(selectedGuiLanguage())
        , pendingGuiLanguage()
        , menuSequence(0)
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
        clearSubmenuBindings();
        clearSideBindings();
        clearViewportMenuBindings();
        if (display != nullptr) {
            for (std::map<int, Cursor>::iterator it = cursors.begin(); it != cursors.end(); ++it)
                XFreeCursor(display, it->second);
            cursors.clear();
            if (checkMarkBitmap != None) XFreePixmap(display, checkMarkBitmap);
            checkMarkBitmap = None;
        }
        if (display != nullptr && colormap != 0) {
            XFreeColormap(display, colormap);
            colormap = 0;
        }
        if (display != nullptr && menuFontSet != nullptr) {
            XFreeFontSet(display, menuFontSet);
            menuFontSet = nullptr;
        }
        if (display != nullptr) {
            XtCloseDisplay(display);
            display = nullptr;
        }
    }

    int run(int argc, char** argv)
    {
        try {
            createWindow(argc, argv);
            createContext();
            initOpenGL();
            redraw();
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
    Widget pendingPage;
    Widget pendingLabel;
    XtModifyPanel* modifyPanel;
    Widget viewportMenu;
    Pixmap checkMarkBitmap;
    XlibLabelImageProvider* labelProvider;
    Window window;
    GLXFBConfig fbConfig;
    GLXContext context;
    Atom wmDeleteWindow;
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
    unsigned int menuSequence;
    bool guiRebuildQueued;
    std::vector<SubmenuBinding*> submenuBindings;
    std::vector<CommandBinding*> commandBindings;
    std::vector<TabBinding*> tabBindings;
    std::vector<CommandBinding*> viewportMenuBindings;
    std::vector<CommandBinding*> menuCommandBindings;
    std::map<std::string, std::string> commandLabels;
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

    static std::string cleanLabel(const std::string& label)
    {
        std::string result;
        for (size_t i = 0; i < label.size(); ++i) {
            if (label[i] == '&' || label[i] == '!') continue;
            if (label[i] == '\t') { result += "    "; continue; }
            result += label[i];
        }
        return result;
    }

    static bool hasModifier(const GuiNode& node, const char* modifier)
    {
        for (size_t i = 0; i < node.modifiers.size(); ++i)
            if (node.modifiers[i] == modifier) return true;
        return false;
    }

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

    static void menuItemSelected(Widget, XtPointer clientData, XtPointer)
    {
        SceneEditorApplication* self = reinterpret_cast<SceneEditorApplication*>(clientData);
        if (self != nullptr) self->requestClose();
    }

    static void selectEnglish(Widget, XtPointer clientData, XtPointer)
    {
        SceneEditorApplication* self = reinterpret_cast<SceneEditorApplication*>(clientData);
        if (self != nullptr) self->scheduleMenuRebuild("english");
    }

    static void selectSpanish(Widget, XtPointer clientData, XtPointer)
    {
        SceneEditorApplication* self = reinterpret_cast<SceneEditorApplication*>(clientData);
        if (self != nullptr) self->scheduleMenuRebuild("spanish");
    }

    static void executePanelCommand(Widget, XtPointer clientData, XtPointer)
    {
        CommandBinding* binding = reinterpret_cast<CommandBinding*>(clientData);
        if (binding == nullptr || binding->application == nullptr) return;
        binding->application->executeCreationCommand(binding->identifier);
        binding->application->redraw();
    }

    static void selectSideTab(Widget, XtPointer clientData, XtPointer)
    {
        TabBinding* binding = reinterpret_cast<TabBinding*>(clientData);
        if (binding != nullptr && binding->application != nullptr)
            binding->application->showSidePage(binding->page);
    }

    static void popupSubmenu(Widget entry, XtPointer clientData, XtPointer)
    {
        SubmenuBinding* binding = reinterpret_cast<SubmenuBinding*>(clientData);
        if (binding == nullptr || binding->application == nullptr ||
            binding->popup == nullptr) return;
        binding->application->showSubmenu(entry, binding->popup);
    }

    GuiNode loadGuiDefinition()
    {
        const char* filename = guiLanguage == "english" ? "english.json" : "spanish.json";
        const std::string path = std::string("../../../../java/testsuite/ApplicationCases/SceneEditorApplication/etc/gui/") + filename;
        std::ifstream input(path.c_str());
        if (!input) {
            throw VSDKFatalException(
                java::String(("Could not open Java GUI definition: " + path).c_str()));
        }
        guiDefinition.assign((std::istreambuf_iterator<char>(input)), std::istreambuf_iterator<char>());
        commandLabels = GuiJsonReader(guiDefinition).readCommandLabels();
        messages = GuiJsonReader(guiDefinition).readMessages();
        return GuiJsonReader(guiDefinition).readMenuBar();
    }

    Widget createPopupMenu(Widget parent, const GuiNode& definition)
    {
        const std::string popupName = "sceneMenu" + std::to_string(menuSequence++);
        Arg popupArgs[4]; Cardinal popupArgCount = 0;
        XtSetArg(popupArgs[popupArgCount], XtNvisual, visual); ++popupArgCount;
        XtSetArg(popupArgs[popupArgCount], XtNdepth, visualDepth); ++popupArgCount;
        XtSetArg(popupArgs[popupArgCount], XtNcolormap, colormap); ++popupArgCount;
        // Xaw only follows an SmeBSB's menuName while the pointer enters the
        // entry when popupOnEntry is enabled on its containing SimpleMenu.
        XtSetArg(popupArgs[popupArgCount], XtNpopupOnEntry, True); ++popupArgCount;
        Widget popup = XtCreatePopupShell(
            popupName.c_str(), simpleMenuWidgetClass,
            menuBar != nullptr ? menuBar : parent,
            popupArgs, popupArgCount);
        addMenuEntries(popup, definition.children);
        return popup;
    }

    void addMenuEntries(Widget menu, const std::vector<GuiNode>& entries)
    {
        for (size_t i = 0; i < entries.size(); ++i) {
            const GuiNode& entry = entries[i];
            if (hasModifier(entry, "SEPARATOR")) {
                XtCreateManagedWidget("separator", smeLineObjectClass, menu, nullptr, 0);
                continue;
            }
            const std::string label = cleanLabel(entry.name);
            Arg args[5]; Cardinal n = 0;
            XtSetArg(args[n], XtNlabel, label.c_str()); ++n;
            XtSetArg(args[n], XtNinternational, True); ++n;
            XtSetArg(args[n], XtNfontSet, menuFontSet); ++n;
            Widget submenu = nullptr;
            if (entry.type == "menu") {
                submenu = createPopupMenu(menu, entry);
            }
            if (hasModifier(entry, "GRAYED")) { XtSetArg(args[n], XtNsensitive, False); ++n; }
            Widget item = XtCreateManagedWidget("menuItem", smeBSBObjectClass, menu, args, n);
            if (hasModifier(entry, "IDC_FILE_QUIT"))
                XtAddCallback(item, XtNcallback, &SceneEditorApplication::menuItemSelected, reinterpret_cast<XtPointer>(this));
            if (hasModifier(entry, "IDC_CUSTOMIZE_LANGUAGE_ENGLISH"))
                XtAddCallback(item, XtNcallback, &SceneEditorApplication::selectEnglish, reinterpret_cast<XtPointer>(this));
            if (hasModifier(entry, "IDC_CUSTOMIZE_LANGUAGE_SPANISH"))
                XtAddCallback(item, XtNcallback, &SceneEditorApplication::selectSpanish, reinterpret_cast<XtPointer>(this));
            for (size_t m = 0; m < entry.modifiers.size(); ++m) {
                if (!isCreationCommand(entry.modifiers[m])) continue;
                CommandBinding* binding = new CommandBinding{this, entry.modifiers[m]};
                menuCommandBindings.push_back(binding);
                XtAddCallback(item, XtNcallback, &SceneEditorApplication::executePanelCommand, binding);
            }
            if (entry.type == "menu") {
                SubmenuBinding* binding = new SubmenuBinding{this, submenu};
                submenuBindings.push_back(binding);
                XtAddCallback(item, XtNcallback, &SceneEditorApplication::popupSubmenu, binding);
            }
        }
    }

    void showSubmenu(Widget entry, Widget popup)
    {
        Position x = 0;
        Position y = 0;
        Position entryX = 0;
        Position entryY = 0;
        Dimension entryWidth = 0;
        XtVaGetValues(
            entry,
            XtNx, &entryX,
            XtNy, &entryY,
            XtNwidth, &entryWidth,
            nullptr);
        // SmeBSB is a windowless RectObj.  Translate through its owning
        // SimpleMenu, which owns the actual X window.
        XtTranslateCoords(
            XtParent(entry), entryX + static_cast<Position>(entryWidth),
            entryY, &x, &y);
        XtVaSetValues(popup, XtNx, x, XtNy, y, nullptr);
        XtPopup(popup, XtGrabNonexclusive);
    }

    void clearSubmenuBindings()
    {
        for (size_t i = 0; i < submenuBindings.size(); ++i)
            delete submenuBindings[i];
        submenuBindings.clear();
        for (size_t i = 0; i < menuCommandBindings.size(); ++i)
            delete menuCommandBindings[i];
        menuCommandBindings.clear();
    }

    void clearSideBindings()
    {
        for (size_t i = 0; i < commandBindings.size(); ++i)
            delete commandBindings[i];
        commandBindings.clear();
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

    /**
    Shows the page of a tab of the side panel: creation, modify or (for the
    tabs not ported yet) an empty one. As `AwtModifyTabChangeListener`,
    showing the modify page tells it the body it must edit.
    */
    void showSidePage(int page)
    {
        if (creationPage == nullptr || modifyPage == nullptr ||
            pendingPage == nullptr) return;
        Widget pages[] = { creationPage, modifyPage, pendingPage };
        Widget shown = page == CREATION_PAGE ? creationPage :
            page == MODIFY_PAGE ? modifyPage : pendingPage;
        for (int i = 0; i < 3; ++i)
            if (pages[i] != shown) XtUnmanageChild(pages[i]);
        XtManageChild(shown);
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
    void reportTargetToModifyPanel()
    {
        if (sceneBridge == nullptr || modifyPanel == nullptr) return;
        SimpleBody* target = sceneBridge->getModifyPanelTarget();
        if (target != nullptr)
            modifyPanel->notifyTargetBeginEdit(target);
        else
            modifyPanel->notifyTargetEndEdit();
        requestRedraw();
    }

    void createRightPanel()
    {
        const int sideWidth = 320;
        const int tabHeight = 28;
        const int contentWidth = width - sideWidth;
        Arg args[4]; Cardinal n = 0;
        XtSetArg(args[n], XtNx, contentWidth); ++n;
        XtSetArg(args[n], XtNy, tabHeight); ++n;
        XtSetArg(args[n], XtNwidth, sideWidth); ++n;
        XtSetArg(args[n], XtNheight, height - tabHeight); ++n;
        rightPanel = XtCreateManagedWidget(
            "rightPanel", compositeWidgetClass, workspace, args, n);

        const char* tabs[] = {
            "IDM_CREATION_TAB", "IDM_MODIFY_TAB", "IDM_GUI_TAB",
            "IDM_OTHERS_TAB", "IDM_RENDER_TAB"
        };
        for (int i = 0; i < 5; ++i) {
            const std::string label = text(tabs[i]);
            Arg tabArgs[6]; Cardinal tabN = 0;
            XtSetArg(tabArgs[tabN], XtNlabel, label.c_str()); ++tabN;
            XtSetArg(tabArgs[tabN], XtNinternational, True); ++tabN;
            XtSetArg(tabArgs[tabN], XtNfontSet, menuFontSet); ++tabN;
            XtSetArg(tabArgs[tabN], XtNx, i * 64); ++tabN;
            XtSetArg(tabArgs[tabN], XtNy, 0); ++tabN;
            XtSetArg(tabArgs[tabN], XtNwidth, 64); ++tabN;
            Widget tab = XtCreateManagedWidget(
                "sideTab", commandWidgetClass, rightPanel, tabArgs, tabN);
            TabBinding* binding = new TabBinding{this, i};
            tabBindings.push_back(binding);
            XtAddCallback(tab, XtNcallback, &SceneEditorApplication::selectSideTab, binding);
        }

        Arg pageArgs[4]; Cardinal pageN = 0;
        XtSetArg(pageArgs[pageN], XtNx, 0); ++pageN;
        XtSetArg(pageArgs[pageN], XtNy, tabHeight); ++pageN;
        XtSetArg(pageArgs[pageN], XtNwidth, sideWidth); ++pageN;
        XtSetArg(pageArgs[pageN], XtNheight, height - 2 * tabHeight); ++pageN;
        creationPage = XtCreateManagedWidget(
            "creationPage", compositeWidgetClass, rightPanel, pageArgs, pageN);
        modifyPage = XtCreateWidget(
            "modifyPage", compositeWidgetClass, rightPanel, pageArgs, pageN);
        pendingPage = XtCreateWidget(
            "pendingPage", compositeWidgetClass, rightPanel, pageArgs, pageN);
        modifyPanel = new XtModifyPanel(this, modifyPage, sideWidth);
        if (sceneBridge != nullptr) {
            sceneBridge->setBodyEditFeedbackProvider(modifyPanel);
            sceneBridge->setModifyPanelSelected(false);
        }

        // The commands of the CREATION group of the GUI definition, but the
        // import / export ones, that need file dialogs
        std::vector<std::string> commands;
        const std::vector<std::string> group =
            GuiJsonReader(guiDefinition).readButtonGroupCommands("CREATION");
        for (size_t i = 0; i < group.size(); ++i)
            if (isCreationCommand(group[i])) commands.push_back(group[i]);
        for (size_t i = 0; i < commands.size(); ++i) {
            const std::string& id = commands[i];
            std::map<std::string, std::string>::const_iterator labelIt = commandLabels.find(id);
            const std::string label = labelIt != commandLabels.end() ? labelIt->second : id;
            Arg buttonArgs[7]; Cardinal buttonN = 0;
            XtSetArg(buttonArgs[buttonN], XtNlabel, label.c_str()); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNinternational, True); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNfontSet, menuFontSet); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNx, 8); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNy, static_cast<Position>(i * 29)); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNwidth, sideWidth - 16); ++buttonN;
            XtSetArg(buttonArgs[buttonN], XtNheight, 26); ++buttonN;
            Widget button = XtCreateManagedWidget(
                "creationCommand", commandWidgetClass, creationPage, buttonArgs, buttonN);
            CommandBinding* binding = new CommandBinding{this, id};
            commandBindings.push_back(binding);
            XtAddCallback(button, XtNcallback, &SceneEditorApplication::executePanelCommand, binding);
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
        pendingPage = nullptr;
        clearSideBindings();
        createRightPanel();
    }

    static bool isCreationCommand(const std::string& id)
    {
        return id.compare(0, 11, "IDC_CREATE_") == 0;
    }

    void executeCreationCommand(const std::string& id)
    {
        if (sceneBridge == nullptr) return;
        if (!sceneBridge->executeCommand(id))
            fprintf(stderr, "SceneEditorApplication: command %s was not executed\n", id.c_str());
    }

    void createMenuBar(Widget parent)
    {
        menuSequence = 0;
        GuiNode menubar = loadGuiDefinition();
        const int buttonWidth = 112;
        for (size_t i = 0; i < menubar.children.size(); ++i) {
            const GuiNode& menu = menubar.children[i];
            Widget popup = createPopupMenu(parent, menu);
            const std::string popupName = XtName(popup);

            Arg args[8]; Cardinal n = 0;
            const std::string label = cleanLabel(menu.name);
            XtSetArg(args[n], XtNlabel, label.c_str()); ++n;
            XtSetArg(args[n], XtNinternational, True); ++n;
            XtSetArg(args[n], XtNfontSet, menuFontSet); ++n;
            XtSetArg(args[n], XtNmenuName, popupName.c_str()); ++n;
            XtSetArg(args[n], XtNx, static_cast<Position>(i * buttonWidth)); ++n;
            XtSetArg(args[n], XtNy, 0); ++n;
            XtSetArg(args[n], XtNwidth, buttonWidth); ++n;
            XtSetArg(args[n], XtNheight, 28); ++n;
            XtCreateManagedWidget("menuButton", menuButtonWidgetClass, parent, args, n);
        }
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
        clearSubmenuBindings();
        if (menuFontSet != nullptr) {
            XFreeFontSet(display, menuFontSet);
            menuFontSet = nullptr;
        }
        selectGuiLocale();
        createMenuFontSet();
        Arg args[4]; Cardinal n = 0;
        XtSetArg(args[n], XtNx, 0); ++n;
        XtSetArg(args[n], XtNy, 0); ++n;
        XtSetArg(args[n], XtNwidth, width); ++n;
        XtSetArg(args[n], XtNheight, 28); ++n;
        menuBar = XtCreateManagedWidget("menuBar", compositeWidgetClass, workspace, args, n);
        createMenuBar(menuBar);
        rebuildRightPanel();
        // Viewport titles, their menus and the HUDs follow the language
        if (sceneBridge != nullptr) sceneBridge->setGuiDefinition(guiDefinition);
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
    Merges the resources of the application (its black and white look) over
    the ones of the display, so they win over the user's defaults.
    */
    void loadResourceFile()
    {
        const char* path = std::getenv("SCENE_EDITOR_XRESOURCES");
        if (path == nullptr) path = "XResources";
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
        // it can redirect it to the drawing canvas (see XtSetKeyboardFocus)
        XtSetArg(args[n], XtNinput, True); n++;
        XtSetArg(args[n], XtNvisual, visual); n++;
        XtSetArg(args[n], XtNcolormap, colormap); n++;
        XtSetArg(args[n], XtNdepth, visualDepth); n++;
        XtSetArg(args[n], XtNwidth, width); n++;
        XtSetArg(args[n], XtNheight, height); n++;
        XtSetArg(args[n], XtNtitle, "VITRAL Scene Editor - Xt GLX OpenGL 4"); n++;
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

        Cardinal workspaceArgsCount = 0;
        XtSetArg(args[workspaceArgsCount], XtNwidth, width); ++workspaceArgsCount;
        XtSetArg(args[workspaceArgsCount], XtNheight, height); ++workspaceArgsCount;
        workspace = XtCreateManagedWidget(
            "workspace", compositeWidgetClass, shell, args, workspaceArgsCount);

        Arg menuBarArgs[4]; Cardinal menuBarArgCount = 0;
        XtSetArg(menuBarArgs[menuBarArgCount], XtNx, 0); ++menuBarArgCount;
        XtSetArg(menuBarArgs[menuBarArgCount], XtNy, 0); ++menuBarArgCount;
        XtSetArg(menuBarArgs[menuBarArgCount], XtNwidth, width); ++menuBarArgCount;
        XtSetArg(menuBarArgs[menuBarArgCount], XtNheight, 28); ++menuBarArgCount;
        menuBar = XtCreateManagedWidget(
            "menuBar", compositeWidgetClass, workspace, menuBarArgs, menuBarArgCount);
        createMenuBar(menuBar);
        createRightPanel();
        canvasWidth = width - 320;
        canvasHeight = height - 28;
        Cardinal canvasArgsCount = 0;
        XtSetArg(args[canvasArgsCount], XtNx, 0); ++canvasArgsCount;
        XtSetArg(args[canvasArgsCount], XtNy, 28); ++canvasArgsCount;
        XtSetArg(args[canvasArgsCount], XtNwidth, canvasWidth); ++canvasArgsCount;
        XtSetArg(args[canvasArgsCount], XtNheight, canvasHeight); ++canvasArgsCount;
        drawingCanvas = XtCreateManagedWidget(
            "drawingCanvas", coreWidgetClass, workspace, args, canvasArgsCount);

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
        // side panel) are redirected to the canvas: the shell selects them,
        // so they reach Xt, which delivers them to its focus widget
        XtAddEventHandler(
            shell, KeyPressMask | KeyReleaseMask, False,
            &SceneEditorApplication::ignoreEventHandler, nullptr);
        XtSetKeyboardFocus(shell, drawingCanvas);

        XtRealizeWidget(shell);
        window = XtWindow(drawingCanvas);
        wmDeleteWindow = XInternAtom(display, "WM_DELETE_WINDOW", False);
        XSetWMProtocols(display, XtWindow(shell), &wmDeleteWindow, 1);
        XtAddEventHandler(
            shell,
            NoEventMask,
            True,
            &SceneEditorApplication::shellEventHandler,
            reinterpret_cast<XtPointer>(this));

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
        glewExperimental = GL_TRUE;
        GLenum err = glewInit();
        if (err != GLEW_OK) {
            fprintf(stderr, "GLEW init failed: %s\n", glewGetErrorString(err));
            throw VSDKFatalException("GLEW init failed");
        }
        glGetError();

        checkOpenGLVersion();
        labelProvider = new XlibLabelImageProvider(display);
        sceneBridge = new XtOpenGL4SceneBridge(labelProvider, this);
        sceneBridge->setBodyEditFeedbackProvider(modifyPanel);
        sceneBridge->setGuiDefinition(guiDefinition);
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

    static void ignoreEventHandler(Widget, XtPointer, XEvent*, Boolean*)
    {
    }

    static void shellEventHandler(
        Widget,
        XtPointer clientData,
        XEvent* event,
        Boolean*)
    {
        SceneEditorApplication* self =
            reinterpret_cast<SceneEditorApplication*>(clientData);
        if (self != nullptr && event->type == ClientMessage &&
            static_cast<Atom>(event->xclient.data.l[0]) == self->wmDeleteWindow) {
            self->requestClose();
        }
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
                sceneBridge->mouseEntered(XtEventMapper::toMouseEvent(*event));
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
                sceneBridge->keyPressed(XtEventMapper::toKeyEvent(event->xkey));
                requestRedraw();
            }
            break;
        case KeyRelease:
            if (sceneBridge != nullptr && !isAutoRepeatRelease(event->xkey)) {
                sceneBridge->keyReleased(XtEventMapper::toKeyEvent(event->xkey));
                requestRedraw();
            }
            break;
        default:
            break;
        }
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
        if (XtEventMapper::isWheelButton(event)) {
            MouseEvent wheel = XtEventMapper::toMouseWheelEvent(event);
            if (wheel.getClicks() != 0) {
                sceneBridge->mouseWheel(wheel);
                requestRedraw();
            }
            return;
        }
        if (consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::PRESS)) return;
        // Keys go back to the canvas after typing in a field of the panel
        XtSetKeyboardFocus(shell, drawingCanvas);
        pressButton = event.xbutton.button;
        pressX = event.xbutton.x;
        pressY = event.xbutton.y;
        pressMoved = false;
        sceneBridge->mousePressed(XtEventMapper::toMouseEvent(event));
        requestRedraw();
    }

    void processButtonRelease(const XEvent& event)
    {
        if (sceneBridge == nullptr || XtEventMapper::isWheelButton(event)) return;
        if (consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::RELEASE)) return;
        const bool clicked = event.xbutton.button == pressButton && !pressMoved;
        pressButton = 0;
        // The release may request the viewport menu, that grabs the pointer
        sceneBridge->mouseReleased(XtEventMapper::toMouseEvent(event));
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
            MouseEvent click = XtEventMapper::toMouseEvent(event);
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
            sceneBridge->mouseMoved(XtEventMapper::toMouseEvent(*event));
            return;
        }
        if (consumedByPopupDismiss(PopupDismissClickFilter::MouseEventKind::DRAG)) return;
        // As in AWT, a press followed by any motion is not a click
        if (event->xmotion.x != pressX || event->xmotion.y != pressY) pressMoved = true;
        sceneBridge->mouseDragged(XtEventMapper::toMouseEvent(*event));
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

    static void executeViewportMenuCommand(Widget, XtPointer clientData, XtPointer)
    {
        CommandBinding* binding = reinterpret_cast<CommandBinding*>(clientData);
        if (binding == nullptr || binding->application == nullptr ||
            binding->application->sceneBridge == nullptr) return;
        binding->application->sceneBridge->executeViewportCommand(binding->identifier);
        binding->application->requestRedraw();
    }

    static void viewportMenuPoppedDown(Widget popup, XtPointer clientData, XtPointer)
    {
        SceneEditorApplication* self = reinterpret_cast<SceneEditorApplication*>(clientData);
        XtUngrabPointer(popup, CurrentTime);
        if (self != nullptr) {
            self->popupDismissFilter.popupClosed(currentTimeMillis());
            self->requestRedraw();
        }
    }

    void clearViewportMenuBindings()
    {
        for (size_t i = 0; i < viewportMenuBindings.size(); ++i)
            delete viewportMenuBindings[i];
        viewportMenuBindings.clear();
    }

    Pixmap getCheckMarkBitmap()
    {
        static const unsigned char checkMarkBits[] = {
            0x00, 0x80, 0xc0, 0x61, 0x33, 0x1e, 0x0c, 0x00
        };
        if (checkMarkBitmap == None) {
            checkMarkBitmap = XCreateBitmapFromData(
                display, RootWindow(display, DefaultScreen(display)),
                reinterpret_cast<const char*>(checkMarkBits), 8, 8);
        }
        return checkMarkBitmap;
    }

    /**
    Pops up, with Xt, the menu of the viewport whose title was clicked: its
    projection locations and render modes, the ones in use checked. It
    grabs the pointer, so a click outside only closes it.
    */
    void showViewportMenu(int canvasX, int canvasY)
    {
        if (viewportMenu != nullptr) {
            XtDestroyWidget(viewportMenu);
            viewportMenu = nullptr;
        }
        clearViewportMenuBindings();
        const std::vector<XtOpenGL4SceneBridge::ViewportMenuItem> items =
            sceneBridge->getViewportMenuItems();
        if (items.empty()) return;

        Arg popupArgs[3]; Cardinal popupArgCount = 0;
        XtSetArg(popupArgs[popupArgCount], XtNvisual, visual); ++popupArgCount;
        XtSetArg(popupArgs[popupArgCount], XtNdepth, visualDepth); ++popupArgCount;
        XtSetArg(popupArgs[popupArgCount], XtNcolormap, colormap); ++popupArgCount;
        viewportMenu = XtCreatePopupShell(
            "viewportMenu", simpleMenuWidgetClass, drawingCanvas,
            popupArgs, popupArgCount);
        // Entries follow the pointer without a button pressed, too
        XtOverrideTranslations(viewportMenu, XtParseTranslationTable("<Motion>: highlight()"));
        XtAddCallback(viewportMenu, XtNpopdownCallback,
                      &SceneEditorApplication::viewportMenuPoppedDown,
                      reinterpret_cast<XtPointer>(this));

        for (size_t i = 0; i < items.size(); ++i) {
            if (items[i].separator) {
                XtCreateManagedWidget("separator", smeLineObjectClass, viewportMenu, nullptr, 0);
                continue;
            }
            Arg args[6]; Cardinal n = 0;
            XtSetArg(args[n], XtNlabel, items[i].label.c_str()); ++n;
            XtSetArg(args[n], XtNinternational, True); ++n;
            XtSetArg(args[n], XtNfontSet, menuFontSet); ++n;
            XtSetArg(args[n], XtNleftMargin, 16); ++n;
            if (items[i].current) {
                XtSetArg(args[n], XtNleftBitmap, getCheckMarkBitmap()); ++n;
            }
            Widget item = XtCreateManagedWidget(
                "viewportMenuItem", smeBSBObjectClass, viewportMenu, args, n);
            CommandBinding* binding = new CommandBinding{this, items[i].command};
            viewportMenuBindings.push_back(binding);
            XtAddCallback(item, XtNcallback,
                          &SceneEditorApplication::executeViewportMenuCommand, binding);
        }

        Position rootX = 0;
        Position rootY = 0;
        XtTranslateCoords(drawingCanvas, static_cast<Position>(canvasX),
                          static_cast<Position>(canvasY), &rootX, &rootY);
        XtVaSetValues(viewportMenu, XtNx, rootX, XtNy, rootY, nullptr);
        XtPopup(viewportMenu, XtGrabExclusive);
        // Without owner events, every pointer event (over the menu, the
        // canvas or anywhere) goes to the menu: an outside release closes it
        XtGrabPointer(viewportMenu, False,
                      ButtonPressMask | ButtonReleaseMask | PointerMotionMask |
                      EnterWindowMask | LeaveWindowMask,
                      GrabModeAsync, GrabModeAsync, None, None, CurrentTime);
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
        printf("%s\n", message.c_str());
        fflush(stdout);
    }

    void closeRequested() override
    {
        requestClose();
    }

    void selectionChanged() override
    {
        reportTargetToModifyPanel();
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

    Widget createPopupMenu(Widget parent, const char* name) override
    {
        Arg args[3]; Cardinal n = 0;
        XtSetArg(args[n], XtNvisual, visual); ++n;
        XtSetArg(args[n], XtNdepth, visualDepth); ++n;
        XtSetArg(args[n], XtNcolormap, colormap); ++n;
        return XtCreatePopupShell(name, simpleMenuWidgetClass, parent, args, n);
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
    SceneEditorApplication app;
    return app.run(argc, argv);
}

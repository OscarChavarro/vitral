#ifndef __XT_OPENGL4_SCENE_BRIDGE__
#define __XT_OPENGL4_SCENE_BRIDGE__

#include <string>
#include <vector>

#include "gui/PointerCursor.h"

class KeyEvent;
class MouseEvent;
class OpenGL4LabelImageProvider;

/**
Xt-facing façade for the technology-independent scene model, its
interaction techniques (camera, selection and the translation, rotation and
scale gizmos) and its OpenGL4 drawing-area renderer. Its implementation
intentionally has no Xt dependency, avoiding the legacy Widget name
collision in the port: the Xt side converts its events to vitral ones (see
`XtEventMapper`) and presents what the interaction requests through the
`Listener`.

Mouse events must have coordinates in canvas pixels, with origin at the
upper left corner of the drawing canvas.
*/
class XtOpenGL4SceneBridge {
public:
    /**
    Requests from the interaction that the GUI technology must present.
    */
    class Listener {
    public:
        virtual ~Listener() {}
        virtual void repaintRequested() = 0;
        virtual void cursorRequested(PointerCursor::Value cursor) = 0;

        /**
        @param canvasX horizontal position, in canvas pixels
        @param canvasY vertical position, in canvas pixels
        */
        virtual void cursorWarpRequested(int canvasX, int canvasY) = 0;
        virtual void statusMessageRequested(const std::string& message) = 0;
        virtual void closeRequested() = 0;

        /**
        The title of the selected viewport was clicked: its menu (see
        `getViewportMenuItems`) must be presented.
        @param canvasX horizontal position of the menu, in canvas pixels
        @param canvasY vertical position of the menu, in canvas pixels
        */
        virtual void viewportMenuRequested(int canvasX, int canvasY) = 0;
    };

    /**
    Entry of the menu of a viewport: its projection location and render
    mode commands, as defined by the I18N context.
    */
    struct ViewportMenuItem {
        std::string label;
        std::string command;
        bool separator;
        /** true for the projection location / render mode in use */
        bool current;
    };

private:
    class Impl;
    Impl* impl;

    XtOpenGL4SceneBridge(const XtOpenGL4SceneBridge& other);
    XtOpenGL4SceneBridge& operator=(const XtOpenGL4SceneBridge& other);

public:
    /**
    @param labels rasterizes the texts of the HUDs of the viewports
    (referenced, not owned)
    @param listener presents the interaction requests (referenced)
    */
    XtOpenGL4SceneBridge(OpenGL4LabelImageProvider* labels,
                         Listener* listener);
    ~XtOpenGL4SceneBridge();

    /**
    Sets the I18N context from the JSON GUI definition: it names the
    viewports, the entries of their menus and the interaction modes shown
    in their HUDs.
    */
    void setGuiDefinition(const std::string& json);

    void init();
    void dispose();
    void display(int width, int height);
    void reshape(int width, int height);

    /**
    Executes a command of the GUI that only works over the model (i.e. the
    `IDC_CREATE_...` ones, that create objects and lights), recording what
    it changes in the scene history (see `GuiEventExecutor`).
    @return false if the command is unknown, needs the GUI (file dialogs)
    or failed
    */
    bool executeCommand(const std::string& command);

    //= Interaction =======================================================
    void setCanvasSize(int width, int height);
    void mouseEntered(const MouseEvent& event);
    void mousePressed(const MouseEvent& event);
    void mouseReleased(const MouseEvent& event);
    void mouseClicked(const MouseEvent& event);
    void mouseMoved(const MouseEvent& event);
    void mouseDragged(const MouseEvent& event);
    void mouseWheel(const MouseEvent& event);
    void keyPressed(const KeyEvent& event);
    void keyReleased(const KeyEvent& event);

    /**
    @return the entries of the menu of the viewport whose title was last
    clicked (see `Listener::viewportMenuRequested`)
    */
    std::vector<ViewportMenuItem> getViewportMenuItems() const;

    /**
    Executes a command of the viewport menu over the viewport whose title
    was clicked, recording the change of its view.
    */
    void executeViewportCommand(const std::string& command);
};

#endif

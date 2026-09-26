#ifndef __XT_OPENGL4_SCENE_BRIDGE__
#define __XT_OPENGL4_SCENE_BRIDGE__

#include <string>
#include <vector>

#include "application/GuiEventExecutor.h"
#include "gui/PointerCursor.h"
#include "java/io/File.h"

class ApplicationModel;
class BodyEditFeedbackProvider;
class GuiState;
class KeyEvent;
class MouseEvent;
class OpenGL4LabelImageProvider;
class RGBImageUncompressed;
class SimpleBody;
class Vector3Dd;
class Viewport;

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

        /**
        The set of selected things changed: the modify panel must be told
        its new target (see `getModifyPanelTarget`).
        */
        virtual void selectionChanged() = 0;

        /**
        The user requested (Ctrl+Shift+F) to switch between showing only
        the drawing area and showing all the GUI.
        */
        virtual void fullScreenGuiToggleRequested() = 0;

        /**
        The user requested (F10) the raytraced image of the scene, to be
        computed (see `doRaytracingImage`) and shown.
        */
        virtual void raytracingRequested() = 0;

        /**
        The user requested (h) the dialog to select objects by name.
        */
        virtual void selectorDialogRequested() = 0;

        /**
        An image obtained from the renderer (i.e. a depth map) must be shown
        to the user.
        @param image the image (not owned: it stays in the model)
        */
        virtual void imageRequested(RGBImageUncompressed* image) = 0;
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
    @return technology independent model of the application, created as
    `AwtJogl4SceneEditorApplication` does it
    */
    ApplicationModel* getApplicationModel();

    /**
    @return the executor of the commands of the GUI that only work over the
    model (see `GuiEventExecutor`); its status messages are presented
    through the `Listener`
    */
    GuiEventExecutor* getCommands();

    /**
    @return the state of the GUI kept in the model (language, folders of
    the file dialogs, full screen mode)
    */
    GuiState* getGuiState();

    /**
    Executes a command of the GUI that only works over the model (i.e. the
    `IDC_CREATE_...` ones, that create objects and lights), recording what
    it changes in the scene history (see `GuiEventExecutor`).
    @return whether the command was executed, failed, or needs the GUI
    */
    GuiEventExecutor::CommandResult executeCommand(const std::string& command);

    /**
    Ray traces the scene from its active camera into the raytraced image of
    the model, with the size set there, reporting the progress in the
    console and exporting it to `output.jpg`.
    */
    void doRaytracingImage();

    /**
    @return the raytraced image of the model (kept there)
    */
    RGBImageUncompressed* getRaytracedImage();

    /**
    Requests the export of the selected viewport, done while drawing the
    next frame.
    @param file destination file
    @param jpg true for a JPG file, false for a PNG one
    */
    void requestViewportExport(const java::File& file, bool jpg);

    /**
    Requests the export of the whole viewport set area as a JPG, done while
    drawing the next frame.
    @param file destination file
    */
    void requestWorkspaceExport(const java::File& file);

    /**
    Projects a point of the scene to canvas pixel coordinates.
    @param viewport viewport whose camera is used
    @param point point in world coordinates
    @param outCanvas {x, y} in canvas pixels
    @return false if the point is behind the camera
    */
    bool projectToCanvas(Viewport* viewport, const Vector3Dd& point,
                         double outCanvas[2]);

    /**
    Informs the scaler of the texts and marks of the viewports of the
    resolution of the screen showing the drawing area, in the pixels text is
    drawn with (as `AwtViewportElementScaler` does for AWT).
    */
    void setScreenResolution(int width, int height);

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

    //= Modify panel ======================================================

    /**
    @param selected true while the modify panel of the GUI is shown
    */
    void setModifyPanelSelected(bool selected);

    /**
    @return the body the modify panel must edit: the first selected body
    while the modify panel is shown, or null
    */
    SimpleBody* getModifyPanelTarget();

    /**
    @param provider editor of the modify panel, whose feedback geometry is
    drawn over the body under edition (referenced, not owned), or null
    */
    void setBodyEditFeedbackProvider(BodyEditFeedbackProvider* provider);
};

#endif

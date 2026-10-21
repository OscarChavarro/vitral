#ifndef __XT_OPENGL1_MCP_APPLICATION_TOOLS__
#define __XT_OPENGL1_MCP_APPLICATION_TOOLS__

#include "java/lang/String.h"

class XtOpenGL1ApplicationController;
class XtOpenGL1SceneEditorApplication;

/**
Tools of the automation service of the editor that need the running Xt/GL1
application: input events injected into the drawing area, projection of
points to its pixels, images exported from it or raytraced, the GUI language
and the end of the application.

C++ counterpart of Java's `AwtJogl4MCPApplicationTools`. Failures are C++
exceptions, with the same messages as the Java ones.
*/
class XtOpenGL1MCPApplicationTools {
private:
    XtOpenGL1SceneEditorApplication* parent;

    XtOpenGL1ApplicationController* getDrawingAreaController();

public:
    /**
    @param parent application the tools work over (referenced)
    */
    explicit XtOpenGL1MCPApplicationTools(XtOpenGL1SceneEditorApplication* parent);

    /**
    Repaints the drawing area, i.e. after a change of the model.
    */
    void repaint();

    //= Drawing area ======================================================

    /**
    Sends a mouse event to the drawing area (tool `gui.mouse`).
    @param request request with the arguments type,x,y,button
    */
    void injectMouse(const String& request);

    /**
    Sends a key press to the drawing area (tool `gui.key`).
    @param request request with the arguments key,shift,ctrl
    */
    void injectKey(const String& request);

    /** Opens the same native dialog as File > Import (gui.import_scene_dialog). */
    String showImportDialog();

    /**
    @param request request with the argument viewport (index, default 0)
    @return the drawing area pixels of the origin of the first selected body
    and of its unit axis tips, as JSON (tool `viewport.project`)
    */
    String projectSelectedBody(const String& request);

    //= Images ============================================================

    /**
    Raytraces the scene and exports it to a PNG (tool `render.raytrace_png`).
    @param request request with the arguments path,width,height
    @return the path of the written file as JSON
    */
    String raytracePng(const String& request);

    /**
    Exports the selected viewport, as drawn, to a JPG (tool
    `viewport.export_jpg`).
    @param request request with the argument path
    @return the path of the written file as JSON
    */
    String exportViewportJpg(const String& request);

    /**
    Exports the whole drawing area to a JPG (tool `workspace.export_jpg`).
    @param request request with the argument path
    @return the path of the written file as JSON
    */
    String exportWorkspaceJpg(const String& request);

    //= Dialogs ===========================================================

    /**
    Opens or refreshes the scene selector dialog (tool `gui.selector.open`).
    */
    void openSelector();

    /**
    Selects a body through the open scene selector (tool
    `gui.selector.select_body`).
    @param request request with the argument index (-1 clears)
    @return the first selected body, as JSON
    */
    String selectBodyInDialog(const String& request);

    /**
    Runs an action of the open scene selector (tool `gui.selector.action`).
    @param request request with the argument action
    */
    String selectorAction(const String& request);

    /**
    Opens the tangible interface connection dialog (tool
    `gui.tangible.open`).
    */
    void openTangibleDialog();

    /**
    @return the open dialogs and the state of the tangible connection form,
    as JSON (tool `gui.dialog.state`)
    */
    String dialogState();

    /**
    Acts on the tangible connection dialog (tool `gui.tangible.action`).
    @param request request with the arguments action (connect|cancel) and
    serviceUrl
    @return the state of the dialogs, as `dialogState`
    */
    String tangibleAction(const String& request);

    //= Application =======================================================

    /**
    @return the languages of the GUI, marking the current one, as JSON (tool
    `gui.list_languages`)
    */
    String listLanguages();

    /**
    Changes the GUI language, rebuilding the GUI (tool `gui.set_language`).
    @param request request with the argument language
    @return the language set as JSON
    */
    String setLanguage(const String& request);

    /**
    Closes the application shortly, so the answer of this tool can be sent
    before (tool `app.exit`).
    @return the answer of the tool as JSON
    */
    String exitApplication();
};

#endif

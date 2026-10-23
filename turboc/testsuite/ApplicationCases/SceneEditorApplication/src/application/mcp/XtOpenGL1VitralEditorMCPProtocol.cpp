#include <stdio.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Runnable.h"

#include "java/io/BufferedReader.h"
#include "java/io/PrintWriter.h"
#include "java/net/Socket.h"
#include "vsdk/toolkit/gui/XtEventQueue.h"
#include "application/XtOpenGL1SceneEditorApplication.h"
#include "application/mcp/MCPJson.h"
#include "application/mcp/XtOpenGL1VitralEditorMCPProtocol.h"
#include "java/lang/Exception.h"
#include "java/lang/IllegalArgumentException.h"


/**
A tool executed in the thread of the GUI.
*/
class ToolCall : public Runnable {
private:
    XtOpenGL1VitralEditorMCPProtocol* protocol;
    String tool;
    String request;

public:
    String result;
    BOOLEAN failed;
    String failure;

    ToolCall(XtOpenGL1VitralEditorMCPProtocol* protocol, const String& tool,
             const String& request)
        : protocol(protocol), tool(tool), request(request), failed(FALSE) {}

    virtual void run()
    {
        try {
            result = protocol->executeTool(tool, request);
        }
        catch ( const Exception& e ) {
            failed = TRUE;
            failure = e.getMessage().c_str();
        }
    }
};


XtOpenGL1VitralEditorMCPProtocol::XtOpenGL1VitralEditorMCPProtocol(
    XtOpenGL1SceneEditorApplication* parent, Socket* socket)
    : socket(socket),
      sceneTools(parent->getApplicationModel()),
      viewportTools(parent->getApplicationModel(), TRUE),
      applicationTools(parent)
{
}

void XtOpenGL1VitralEditorMCPProtocol::run()
{
    try {
        BufferedReader in(socket->getInputStream());
        PrintWriter out(socket->getOutputStream(), TRUE);
        String line;

        while ( in.readLine(line) ) {
            out.println(handle(line));
            if ( out.checkError() ) {
                break;
            }
        }
    }
    catch ( const Exception& e ) {
        fprintf(stderr, "Error on XtOpenGL1VitralEditorMCPProtocol!\n%s\n",
                e.getMessage().c_str());
    }
}

String XtOpenGL1VitralEditorMCPProtocol::handle(
    const String& request)
{
    String id = MCPJson::idProperty(request);
    String method = MCPJson::stringProperty(request, "method", "");

    try {
        if ( method.equals("initialize") ) {
            return MCPJson::result(id,
                "{\"protocolVersion\":\"2024-11-05\",\"serverInfo\":{\"name\":\"VitralEditorMCP\",\"version\":\"0.1\"},\"capabilities\":{\"tools\":{}}}");
        }
        if ( method.equals("tools/list") ) {
            return MCPJson::result(id, toolsJson());
        }
        if ( method.equals("tools/call") ) {
            String tool = MCPJson::stringProperty(request, "name", "");
            return MCPJson::result(id, callTool(tool, request));
        }
        return MCPJson::error(id, -32601, String("Unknown method: ") + method);
    }
    catch ( const Exception& e ) {
        return MCPJson::error(id, -32000, e.getMessage().c_str());
    }
}

String XtOpenGL1VitralEditorMCPProtocol::callTool(
    const String& tool, const String& request)
{
    ToolCall command(this, tool, request);

    XtEventQueue::invokeAndWait(&command);
    if ( command.failed ) {
        return MCPJson::content(String("{\"error\":\"") +
            MCPJson::escape(command.failure) + "\"}");
    }
    return MCPJson::content(command.result);
}

String XtOpenGL1VitralEditorMCPProtocol::executeTool(
    const String& tool, const String& request)
{
    //- Scene ---------------------------------------------------------
    if ( tool.equals("scene.describe") ) {
        return sceneTools.describeScene();
    }
    if ( tool.equals("scene.clear") ) {
        sceneTools.clearScene();
        applicationTools.repaint();
        return "{\"ok\":true}";
    }
    if ( tool.equals("scene.add_point_light") ) {
        sceneTools.addPointLight(request);
        applicationTools.repaint();
        return sceneTools.describeScene();
    }
    if ( tool.equals("scene.add_sphere") ) {
        sceneTools.addSphere(request);
        applicationTools.repaint();
        return sceneTools.describeScene();
    }
    if ( tool.equals("scene.add_cone") ) {
        sceneTools.addCone(request);
        applicationTools.repaint();
        return sceneTools.describeScene();
    }
    if ( tool.equals("scene.add_cylinder") ) {
        sceneTools.addCylinder(request);
        applicationTools.repaint();
        return sceneTools.describeScene();
    }
    if ( tool.equals("scene.move_body") ) {
        sceneTools.moveBody(request);
        applicationTools.repaint();
        return sceneTools.describeScene();
    }
    if ( tool.equals("scene.select_body") ) {
        sceneTools.selectBody(request);
        return sceneTools.describeScene();
    }
    if ( tool.equals("edit.history") ) {
        return sceneTools.describeEditHistory();
    }
    if ( tool.equals("gui.command") ) {
        String answer = sceneTools.executeGuiCommand(request);
        applicationTools.repaint();
        return answer;
    }
    //- Viewports -----------------------------------------------------
    if ( tool.equals("gui.set_mode") ) {
        return viewportTools.setInteractionMode(request);
    }
    if ( tool.equals("render.get_configuration") ) {
        return viewportTools.describeRendererConfigurations(request);
    }
    if ( tool.equals("render.set_configuration") ) {
        viewportTools.setRendererConfiguration(request);
        applicationTools.repaint();
        return viewportTools.describeRendererConfigurations(request);
    }
    //- Application ---------------------------------------------------
    if ( tool.equals("gui.mouse") ) {
        applicationTools.injectMouse(request);
        return sceneTools.describeScene();
    }
    if ( tool.equals("gui.key") ) {
        applicationTools.injectKey(request);
        return sceneTools.describeScene();
    }
    if ( tool.equals("gui.import_scene_dialog") ) {
        return applicationTools.showImportDialog();
    }
    if ( tool.equals("gui.dialog.state") ) {
        return applicationTools.dialogState();
    }
    if ( tool.equals("gui.selector.open") ) {
        applicationTools.openSelector();
        return applicationTools.dialogState();
    }
    if ( tool.equals("gui.selector.select_body") ) {
        return applicationTools.selectBodyInDialog(request);
    }
    if ( tool.equals("gui.selector.action") ) {
        return applicationTools.selectorAction(request);
    }
    if ( tool.equals("gui.tangible.open") ) {
        applicationTools.openTangibleDialog();
        return "{\"ok\":true}";
    }
    if ( tool.equals("gui.tangible.action") ) {
        return applicationTools.tangibleAction(request);
    }
    if ( tool.equals("viewport.project") ) {
        return applicationTools.projectSelectedBody(request);
    }
    if ( tool.equals("render.raytrace_png") ) {
        return applicationTools.raytracePng(request);
    }
    if ( tool.equals("viewport.export_jpg") ) {
        return applicationTools.exportViewportJpg(request);
    }
    if ( tool.equals("workspace.export_jpg") ) {
        return applicationTools.exportWorkspaceJpg(request);
    }
    if ( tool.equals("gui.list_languages") ) {
        return applicationTools.listLanguages();
    }
    if ( tool.equals("gui.set_language") ) {
        return applicationTools.setLanguage(request);
    }
    if ( tool.equals("app.exit") ) {
        return applicationTools.exitApplication();
    }
    throw IllegalArgumentException(
        (String("Unknown tool: ") + tool).c_str());
}

String XtOpenGL1VitralEditorMCPProtocol::toolsJson()
{
    return String("{\"tools\":[")
        + MCPJson::tool("scene.describe", "Return the bodies (index, name, geometry, position, scale, radius of spheres) and lights (index, type, position, emission) as JSON.")
        + "," + MCPJson::tool("scene.clear", "Remove all bodies, lights and debug groups (undoable).")
        + "," + MCPJson::tool("scene.add_point_light", "Create a point light inside the view of a viewport (first light white, the rest random light colors and positions). Optional arguments: x,y,z,r,g,b override the automatic values.")
        + "," + MCPJson::tool("scene.add_sphere", "Create a sphere. Arguments: radius,x,y,z.")
        + "," + MCPJson::tool("scene.add_cone", "Create a cone (or truncated cone). Arguments: baseRadius,topRadius,height,x,y,z.")
        + "," + MCPJson::tool("scene.add_cylinder", "Create a cylinder. Arguments: radius,height,x,y,z.")
        + "," + MCPJson::tool("scene.move_body", "Set the position of a body (default: the last one; undoable). Arguments: index,x,y,z (missing coordinates are kept).")
        + "," + MCPJson::tool("scene.select_body", "Select one body (a negative index clears the selection). Arguments: index.")
        + "," + MCPJson::tool("gui.set_mode", "Set the interaction mode. Arguments: mode (camera|select|translate|rotate|scale).")
        + "," + MCPJson::tool("gui.mouse", "Send a mouse event to the drawing area. Arguments: type (move|press|drag|release), x, y (logical pixels of the drawing area, as given by viewport.project), button (1 left, 2 middle, 3 right; default 1). Returns the scene state.")
        + "," + MCPJson::tool("gui.key", "Send a key press to the drawing area. Arguments: key (a single character, or tab|enter|backspace|delete|escape|left|right|up|down|pageup|pagedown|num0..num9|num/|num*|num-|num+|num.|numenter), shift (default false), ctrl (default false; i.e. key z with ctrl is undo, y with ctrl is redo, and with shift too they work over the view of the selected viewport). Returns the scene state.")
        + "," + MCPJson::tool("gui.import_scene_dialog", "Open the native File > Import scene chooser. The call returns after the user selects a file or cancels it.")
        + "," + MCPJson::tool("gui.dialog.state", "Describe open widget dialogs and tangible connection form state.")
        + "," + MCPJson::tool("gui.selector.open", "Open or refresh the scene hierarchy dialog.")
        + "," + MCPJson::tool("gui.selector.select_body", "Select a geometry body through the open hierarchy dialog. Arguments: index, or -1 to clear.")
        + "," + MCPJson::tool("gui.selector.action", "Run a selector dialog action. Arguments: action (deleteSelected|toggleVisibility).")
        + "," + MCPJson::tool("gui.tangible.open", "Open the tangible interface connection dialog.")
        + "," + MCPJson::tool("gui.tangible.action", "Act on the tangible connection dialog. Arguments: action (connect|cancel), serviceUrl (when connecting).")
        + "," + MCPJson::tool("gui.command", "Execute a command of the GUI that works only over the model, as its menu item or button does (i.e. IDC_CREATE_SPHERE, IDC_CREATE_FUNCTIONALEXPLICITSURFACE, IDC_CREATE_OMNILIGHT, IDC_TOOLS_RAY, IDC_OTHERS_CYCLE_BACKGROUND). Arguments: command. Returns result (DONE, FAILED or NOT_HANDLED for commands that need the GUI, i.e. file dialogs) and the status message.")
        + "," + MCPJson::tool("edit.history", "Return the undo/redo state of the scene history and of the view history of each viewport: operations to undo and redo, and the names of the next ones.")
        + "," + MCPJson::tool("viewport.project", "Drawing area pixels (as used by gui.mouse) of the first selected body origin and its x, y, z unit-axis tips in a viewport. Arguments: viewport (index, default 0).")
        + "," + MCPJson::tool("render.get_configuration", "Return the rendering configuration of the viewports. Arguments: viewport (index; default all).")
        + "," + MCPJson::tool("render.set_configuration", "Set the rendering configuration of the viewports, only in the given values. Arguments: viewport (index; default all), and any of the booleans points,wires,surfaces,texture,bumpMap,boundingVolume,normals,trianglesNormals,selectionCorners,grid, shading (nolight|flat|gouraud|phong|cook_terrance) and renderMode (gpu|cpu|hidden_lines).")
        + "," + MCPJson::tool("render.raytrace_png", "Raytrace the scene from the camera of the last drawn viewport and export a PNG (it also writes ./output.jpg). Arguments: path, width (default 640), height (default 480).")
        + "," + MCPJson::tool("viewport.export_jpg", "Export the selected viewport, as drawn, to a JPG. Arguments: path.")
        + "," + MCPJson::tool("workspace.export_jpg", "Export the whole drawing area, with all its viewports, to a JPG. Arguments: path.")
        + "," + MCPJson::tool("gui.list_languages", "List the languages available for the GUI (I18N files in etc/gui), marking the current one.")
        + "," + MCPJson::tool("gui.set_language", "Change the GUI language, rebuilding the GUI. Arguments: language (an id given by gui.list_languages).")
        + "," + MCPJson::tool("app.exit", "Close the application (after answering this call).")
        + "]}";
}

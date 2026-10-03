#include <cstdio>
#include <exception>
#include <functional>
#include <stdexcept>

#include "java/io/BufferedReader.h"
#include "java/io/PrintWriter.h"
#include "java/net/Socket.h"
#include "vsdk/toolkit/gui/XtEventQueue.h"
#include "application/XtOpenGL4SceneEditorApplication.h"
#include "application/mcp/MCPJson.h"
#include "application/mcp/XtOpenGL4VitralEditorMCPProtocol.h"

namespace {

/**
A tool executed in the thread of the GUI.
*/
class ToolCall : public java::Runnable {
private:
    std::function<java::String()> tool;

public:
    java::String result;
    bool failed;
    java::String failure;

    explicit ToolCall(const std::function<java::String()>& tool)
        : tool(tool), failed(false) {}

    virtual void run() override
    {
        try {
            result = tool();
        }
        catch ( const std::exception& e ) {
            failed = true;
            failure = e.what();
        }
    }
};

}

XtOpenGL4VitralEditorMCPProtocol::XtOpenGL4VitralEditorMCPProtocol(
    XtOpenGL4SceneEditorApplication* parent, java::net::Socket* socket)
    : socket(socket),
      sceneTools(parent->getApplicationModel()),
      viewportTools(parent->getApplicationModel()),
      applicationTools(parent)
{
}

void XtOpenGL4VitralEditorMCPProtocol::run()
{
    try {
        java::BufferedReader in(socket->getInputStream());
        java::PrintWriter out(socket->getOutputStream(), true);
        java::String line;

        while ( in.readLine(line) ) {
            out.println(handle(line));
            if ( out.checkError() ) {
                break;
            }
        }
    }
    catch ( const std::exception& e ) {
        fprintf(stderr, "Error on XtOpenGL4VitralEditorMCPProtocol!\n%s\n",
                e.what());
    }
}

java::String XtOpenGL4VitralEditorMCPProtocol::handle(
    const java::String& request)
{
    java::String id = MCPJson::idProperty(request);
    java::String method = MCPJson::stringProperty(request, "method", "");

    try {
        if ( method.equals("initialize") ) {
            return MCPJson::result(id,
                "{\"protocolVersion\":\"2024-11-05\",\"serverInfo\":{\"name\":\"VitralEditorMCP\",\"version\":\"0.1\"},\"capabilities\":{\"tools\":{}}}");
        }
        if ( method.equals("tools/list") ) {
            return MCPJson::result(id, toolsJson());
        }
        if ( method.equals("tools/call") ) {
            java::String tool = MCPJson::stringProperty(request, "name", "");
            return MCPJson::result(id, callTool(tool, request));
        }
        return MCPJson::error(id, -32601, java::String("Unknown method: ") + method);
    }
    catch ( const std::exception& e ) {
        return MCPJson::error(id, -32000, e.what());
    }
}

java::String XtOpenGL4VitralEditorMCPProtocol::callTool(
    const java::String& tool, const java::String& request)
{
    ToolCall command([this, tool, request]() {
        return executeTool(tool, request);
    });

    XtEventQueue::invokeAndWait(&command);
    if ( command.failed ) {
        return MCPJson::content(java::String("{\"error\":\"") +
            MCPJson::escape(command.failure) + "\"}");
    }
    return MCPJson::content(command.result);
}

java::String XtOpenGL4VitralEditorMCPProtocol::executeTool(
    const java::String& tool, const java::String& request)
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
        java::String answer = sceneTools.executeGuiCommand(request);
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
    throw std::invalid_argument(
        (java::String("Unknown tool: ") + tool).c_str());
}

java::String XtOpenGL4VitralEditorMCPProtocol::toolsJson()
{
    return java::String("{\"tools\":[")
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
        + "," + MCPJson::tool("gui.command", "Execute a command of the GUI that works only over the model, as its menu item or button does (i.e. IDC_CREATE_SPHERE, IDC_CREATE_FUNCTIONALEXPLICITSURFACE, IDC_CREATE_OMNILIGHT, IDC_TOOLS_RAY, IDC_OTHERS_CYCLE_BACKGROUND). Arguments: command. Returns result (DONE, FAILED or NOT_HANDLED for commands that need the GUI, i.e. file dialogs) and the status message.")
        + "," + MCPJson::tool("edit.history", "Return the undo/redo state of the scene history and of the view history of each viewport: operations to undo and redo, and the names of the next ones.")
        + "," + MCPJson::tool("viewport.project", "Drawing area pixels (as used by gui.mouse) of the first selected body origin and its x, y, z unit-axis tips in a viewport. Arguments: viewport (index, default 0).")
        + "," + MCPJson::tool("render.get_configuration", "Return the rendering configuration of the viewports. Arguments: viewport (index; default all).")
        + "," + MCPJson::tool("render.set_configuration", "Set the rendering configuration of the viewports, only in the given values. Arguments: viewport (index; default all), and any of the booleans points,wires,surfaces,texture,bumpMap,boundingVolume,normals,trianglesNormals,selectionCorners,grid, shading (nolight|flat|gouraud|phong|cook_terrance) and renderMode (gpu|cpu).")
        + "," + MCPJson::tool("render.raytrace_png", "Raytrace the scene from the camera of the last drawn viewport and export a PNG (it also writes ./output.jpg). Arguments: path, width (default 640), height (default 480).")
        + "," + MCPJson::tool("viewport.export_jpg", "Export the selected viewport, as drawn, to a JPG. Arguments: path.")
        + "," + MCPJson::tool("workspace.export_jpg", "Export the whole drawing area, with all its viewports, to a JPG. Arguments: path.")
        + "," + MCPJson::tool("gui.list_languages", "List the languages available for the GUI (I18N files in etc/gui), marking the current one.")
        + "," + MCPJson::tool("gui.set_language", "Change the GUI language, rebuilding the GUI. Arguments: language (an id given by gui.list_languages).")
        + "," + MCPJson::tool("app.exit", "Close the application (after answering this call).")
        + "]}";
}

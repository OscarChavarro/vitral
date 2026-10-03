package application.mcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.swing.SwingUtilities;

import application.AwtJogl4SceneEditorApplication;

/**
Serves one connection of the automation service of the editor: reads one
JSON-RPC request per line (`initialize`, `tools/list`, `tools/call`) and
answers it in one line. Each tool is executed in the Swing event dispatch
thread, delegated to the tool group that owns it:
- `MCPSceneTools`: the scene and the GUI commands over the model,
- `MCPViewportTools`: rendering configuration and interaction mode,
- `AwtJogl4MCPApplicationTools`: the running application (input events,
  images, language, exit).
*/
class AwtJogl4VitralEditorMCPProtocol implements Runnable
{
    private final Socket socket;
    private final MCPSceneTools sceneTools;
    private final MCPViewportTools viewportTools;
    private final AwtJogl4MCPApplicationTools applicationTools;

    /**
    @param parent application to automate
    @param socket connection to serve
    */
    public AwtJogl4VitralEditorMCPProtocol(AwtJogl4SceneEditorApplication parent, Socket socket)
    {
        this.socket = socket;
        this.sceneTools = new MCPSceneTools(parent.getApplicationModel());
        this.viewportTools = new MCPViewportTools(parent.getApplicationModel());
        this.applicationTools = new AwtJogl4MCPApplicationTools(parent);
    }

    @Override
    public void run()
    {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String line;
            while ( (line = in.readLine()) != null ) {
                out.println(handle(line));
            }
        }
        catch ( Exception e ) {
            System.err.println("Error on AwtJogl4VitralEditorMCPProtocol!");
            System.err.println(e);
        }
    }

    private String handle(String request)
    {
        String id = MCPJson.idProperty(request);
        String method = MCPJson.stringProperty(request, "method", "");

        try {
            if ( "initialize".equals(method) ) {
                return MCPJson.result(id,
                    "{\"protocolVersion\":\"2024-11-05\",\"serverInfo\":{\"name\":\"VitralEditorMCP\",\"version\":\"0.1\"},\"capabilities\":{\"tools\":{}}}");
            }
            if ( "tools/list".equals(method) ) {
                return MCPJson.result(id, toolsJson());
            }
            if ( "tools/call".equals(method) ) {
                String tool = MCPJson.stringProperty(request, "name", "");
                return MCPJson.result(id, callTool(tool, request));
            }
            return MCPJson.error(id, -32601, "Unknown method: " + method);
        }
        catch ( Exception e ) {
            return MCPJson.error(id, -32000, e.getMessage());
        }
    }

    private String callTool(String tool, String request) throws Exception
    {
        final String[] result = new String[1];
        Runnable command = () -> {
            try {
                result[0] = executeTool(tool, request);
            }
            catch ( Exception e ) {
                result[0] = "{\"error\":\"" + MCPJson.escape(e.getMessage()) + "\"}";
            }
        };

        if ( SwingUtilities.isEventDispatchThread() ) {
            command.run();
        }
        else {
            SwingUtilities.invokeAndWait(command);
        }

        return MCPJson.content(result[0]);
    }

    private String executeTool(String tool, String request) throws Exception
    {
        switch ( tool ) {
            //- Scene -----------------------------------------------------
            case "scene.describe" -> {
                return sceneTools.describeScene();
            }
            case "scene.clear" -> {
                sceneTools.clearScene();
                applicationTools.repaint();
                return "{\"ok\":true}";
            }
            case "scene.add_point_light" -> {
                sceneTools.addPointLight(request);
                applicationTools.repaint();
                return sceneTools.describeScene();
            }
            case "scene.add_sphere" -> {
                sceneTools.addSphere(request);
                applicationTools.repaint();
                return sceneTools.describeScene();
            }
            case "scene.add_cone" -> {
                sceneTools.addCone(request);
                applicationTools.repaint();
                return sceneTools.describeScene();
            }
            case "scene.add_cylinder" -> {
                sceneTools.addCylinder(request);
                applicationTools.repaint();
                return sceneTools.describeScene();
            }
            case "scene.move_body" -> {
                sceneTools.moveBody(request);
                applicationTools.repaint();
                return sceneTools.describeScene();
            }
            case "scene.select_body" -> {
                sceneTools.selectBody(request);
                return sceneTools.describeScene();
            }
            case "edit.history" -> {
                return sceneTools.describeEditHistory();
            }
            case "gui.command" -> {
                String answer = sceneTools.executeGuiCommand(request);
                applicationTools.repaint();
                return answer;
            }
            //- Viewports -------------------------------------------------
            case "gui.set_mode" -> {
                return viewportTools.setInteractionMode(request);
            }
            case "render.get_configuration" -> {
                return viewportTools.describeRendererConfigurations(request);
            }
            case "render.set_configuration" -> {
                viewportTools.setRendererConfiguration(request);
                return viewportTools.describeRendererConfigurations(request);
            }
            //- Application -----------------------------------------------
            case "gui.mouse" -> {
                applicationTools.injectMouse(request);
                return sceneTools.describeScene();
            }
            case "gui.key" -> {
                applicationTools.injectKey(request);
                return sceneTools.describeScene();
            }
            case "viewport.project" -> {
                return applicationTools.projectSelectedBody(request);
            }
            case "render.raytrace_png" -> {
                return applicationTools.raytracePng(request);
            }
            case "viewport.export_jpg" -> {
                return applicationTools.exportViewportJpg(request);
            }
            case "workspace.export_jpg" -> {
                return applicationTools.exportWorkspaceJpg(request);
            }
            case "render.set_technology" -> {
                return applicationTools.setRenderTechnology(request);
            }
            case "gui.list_languages" -> {
                return applicationTools.listLanguages();
            }
            case "gui.set_language" -> {
                return applicationTools.setLanguage(request);
            }
            case "app.exit" -> {
                return applicationTools.exitApplication();
            }
            default -> throw new IllegalArgumentException("Unknown tool: " + tool);
        }
    }

    private static String toolsJson()
    {
        return "{\"tools\":["
            + MCPJson.tool("scene.describe", "Return the bodies (index, name, geometry, position, scale, radius of spheres) and lights (index, type, position, emission) as JSON.")
            + "," + MCPJson.tool("scene.clear", "Remove all bodies, lights and debug groups (undoable).")
            + "," + MCPJson.tool("scene.add_point_light", "Create a point light inside the view of a viewport (first light white, the rest random light colors and positions). Optional arguments: x,y,z,r,g,b override the automatic values.")
            + "," + MCPJson.tool("scene.add_sphere", "Create a sphere. Arguments: radius,x,y,z.")
            + "," + MCPJson.tool("scene.add_cone", "Create a cone (or truncated cone). Arguments: baseRadius,topRadius,height,x,y,z.")
            + "," + MCPJson.tool("scene.add_cylinder", "Create a cylinder. Arguments: radius,height,x,y,z.")
            + "," + MCPJson.tool("scene.move_body", "Set the position of a body (default: the last one; undoable). Arguments: index,x,y,z (missing coordinates are kept).")
            + "," + MCPJson.tool("scene.select_body", "Select one body (a negative index clears the selection). Arguments: index.")
            + "," + MCPJson.tool("gui.set_mode", "Set the interaction mode. Arguments: mode (camera|select|translate|rotate|scale).")
            + "," + MCPJson.tool("gui.mouse", "Send a mouse event to the drawing area. Arguments: type (move|press|drag|release), x, y (logical pixels of the drawing area, as given by viewport.project), button (1 left, 2 middle, 3 right; default 1). Returns the scene state.")
            + "," + MCPJson.tool("gui.key", "Send a key press to the drawing area. Arguments: key (a single character, or tab|enter|backspace|delete|escape|left|right|up|down|pageup|pagedown|num0..num9|num/|num*|num-|num+|num.|numenter), shift (default false), ctrl (default false; i.e. key z with ctrl is undo, y with ctrl is redo, and with shift too they work over the view of the selected viewport). Returns the scene state.")
            + "," + MCPJson.tool("gui.command", "Execute a command of the GUI that works only over the model, as its menu item or button does (i.e. IDC_CREATE_SPHERE, IDC_CREATE_FUNCTIONALEXPLICITSURFACE, IDC_CREATE_OMNILIGHT, IDC_TOOLS_RAY, IDC_OTHERS_CYCLE_BACKGROUND). Arguments: command. Returns result (DONE, FAILED or NOT_HANDLED for commands that need the GUI, i.e. file dialogs) and the status message.")
            + "," + MCPJson.tool("edit.history", "Return the undo/redo state of the scene history and of the view history of each viewport: operations to undo and redo, and the names of the next ones.")
            + "," + MCPJson.tool("viewport.project", "Drawing area pixels (as used by gui.mouse) of the first selected body origin and its x, y, z unit-axis tips in a viewport. Arguments: viewport (index, default 0).")
            + "," + MCPJson.tool("render.get_configuration", "Return the rendering configuration of the viewports. Arguments: viewport (index; default all).")
            + "," + MCPJson.tool("render.set_configuration", "Set the rendering configuration of the viewports, only in the given values. Arguments: viewport (index; default all), and any of the booleans points,wires,surfaces,texture,bumpMap,boundingVolume,normals,trianglesNormals,selectionCorners,grid, shading (nolight|flat|gouraud|phong|cook_terrance) and renderMode (gpu|cpu|hidden_lines; only the ones available in the render technology: gpu|cpu with opengl4, hidden_lines|cpu with awt).")
            + "," + MCPJson.tool("render.set_technology", "Present the viewport set with other technology, rebuilding the GUI (wait about 1 s before the next call). Arguments: technology (opengl4|awt). AWT draws only with 2D operations: its viewports are hidden lines (wireframe, or Appel hidden line removal when surfaces are on) or raytraced.")
            + "," + MCPJson.tool("render.raytrace_png", "Raytrace the scene from the camera of the last drawn viewport and export a PNG (it also writes ./output.jpg). Arguments: path, width (default 640), height (default 480).")
            + "," + MCPJson.tool("viewport.export_jpg", "Export the selected viewport, as drawn, to a JPG. Arguments: path.")
            + "," + MCPJson.tool("workspace.export_jpg", "Export the whole drawing area, with all its viewports, to a JPG. Arguments: path.")
            + "," + MCPJson.tool("gui.list_languages", "List the languages available for the GUI (I18N files in etc/gui), marking the current one.")
            + "," + MCPJson.tool("gui.set_language", "Change the GUI language, rebuilding the GUI. Arguments: language (an id given by gui.list_languages).")
            + "," + MCPJson.tool("app.exit", "Close the application (after answering this call).")
            + "]}";
    }
}

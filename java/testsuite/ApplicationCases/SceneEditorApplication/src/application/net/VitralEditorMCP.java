package application.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import application.SceneEditorApplication;
import application.framework.Scene;
import application.render.jogl.Jogl4DrawingAreaRenderer;
import framework.model.Viewport;
import framework.model.ViewportSet;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.ShadingType;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.io.image.ImagePersistence;

class VitralEditorMCPProtocol implements Runnable
{
    private final SceneEditorApplication parent;
    private final Socket socket;

    public VitralEditorMCPProtocol(SceneEditorApplication parent, Socket socket)
    {
        this.parent = parent;
        this.socket = socket;
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
            System.err.println("Error on VitralEditorMCPProtocol!");
            System.err.println(e);
        }
    }

    private String handle(String request)
    {
        String id = idProperty(request);
        String method = stringProperty(request, "method", "");

        try {
            if ( "initialize".equals(method) ) {
                return result(id,
                    "{\"protocolVersion\":\"2024-11-05\",\"serverInfo\":{\"name\":\"VitralEditorMCP\",\"version\":\"0.1\"},\"capabilities\":{\"tools\":{}}}");
            }
            if ( "tools/list".equals(method) ) {
                return result(id, toolsJson());
            }
            if ( "tools/call".equals(method) ) {
                String tool = nestedStringProperty(request, "name", "");
                return result(id, callTool(tool, request));
            }
            return error(id, -32601, "Unknown method: " + method);
        }
        catch ( Exception e ) {
            return error(id, -32000, e.getMessage());
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
                result[0] = "{\"error\":\"" + escape(e.getMessage()) + "\"}";
            }
        };

        if ( SwingUtilities.isEventDispatchThread() ) {
            command.run();
        }
        else {
            SwingUtilities.invokeAndWait(command);
        }

        return content(result[0]);
    }

    private String executeTool(String tool, String request) throws Exception
    {
        if ( "scene.describe".equals(tool) ) {
            return describeScene();
        }
        if ( "scene.clear".equals(tool) ) {
            clearScene();
            return "{\"ok\":true}";
        }
        if ( "scene.add_point_light".equals(tool) ) {
            addPointLight(request);
            return describeScene();
        }
        if ( "scene.add_sphere".equals(tool) ) {
            addSphere(request);
            return describeScene();
        }
        if ( "scene.add_cone".equals(tool) ) {
            addCone(request);
            return describeScene();
        }
        if ( "scene.add_cylinder".equals(tool) ) {
            addCylinder(request);
            return describeScene();
        }
        if ( "scene.move_body".equals(tool) ) {
            moveBody(request);
            return describeScene();
        }
        if ( "scene.select_body".equals(tool) ) {
            selectBody(request);
            return describeScene();
        }
        if ( "gui.set_mode".equals(tool) ) {
            return setInteractionMode(request);
        }
        if ( "gui.mouse".equals(tool) ) {
            return injectMouse(request);
        }
        if ( "viewport.project".equals(tool) ) {
            return projectSelectedBody(request);
        }
        if ( "render.get_configuration".equals(tool) ) {
            return describeRendererConfigurations(request);
        }
        if ( "render.set_configuration".equals(tool) ) {
            setRendererConfiguration(request);
            return describeRendererConfigurations(request);
        }
        if ( "render.raytrace_png".equals(tool) ) {
            return raytracePng(request);
        }
        if ( "viewport.export_jpg".equals(tool) ) {
            return viewportJpg(request);
        }
        if ( "workspace.export_jpg".equals(tool) ) {
            return workspaceJpg(request);
        }
        if ( "gui.list_languages".equals(tool) ) {
            return listLanguages();
        }
        if ( "gui.set_language".equals(tool) ) {
            return setLanguage(request);
        }
        if ( "app.exit".equals(tool) ) {
            return exitApplication();
        }
        throw new IllegalArgumentException("Unknown tool: " + tool);
    }

    private String listLanguages()
    {
        StringBuilder sb = new StringBuilder();
        String current = parent.getCurrentGuiLanguage();
        boolean first = true;

        sb.append("{\"current\":\"").append(escape(current)).append("\",\"languages\":[");
        for ( String language : parent.getGuiLanguages() ) {
            if ( !first ) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"id\":\"").append(escape(language)).append('"')
                .append(",\"current\":").append(language.equals(current)).append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private String setLanguage(String request)
    {
        String language = stringProperty(request, "language", "");

        if ( !parent.setGuiLanguageById(language) ) {
            throw new IllegalArgumentException("Unknown language \"" + language +
                "\". Available languages: " + parent.getGuiLanguages());
        }
        return "{\"ok\":true,\"language\":\"" + escape(language) + "\"}";
    }

    /**
    The exit is deferred a short time, so the response of this call can be
    sent to the client before the process ends.
    */
    private String exitApplication()
    {
        Timer timer = new Timer(300, e -> parent.closeApplication());

        timer.setRepeats(false);
        timer.start();
        return "{\"ok\":true,\"message\":\"The application is closing\"}";
    }

    private void clearScene()
    {
        Scene scene = parent.getApplicationModel().getScene();
        scene.scene.getSimpleBodies().clear();
        scene.scene.getLights().clear();
        scene.debugThingGroups.clear();
    }

    private void addPointLight(String request)
    {
        double x = numberProperty(request, "x", -10.0);
        double y = numberProperty(request, "y", -9.0);
        double z = numberProperty(request, "z", 8.0);
        double r = numberProperty(request, "r", 1.0);
        double g = numberProperty(request, "g", 1.0);
        double b = numberProperty(request, "b", 1.0);
        Scene scene = parent.getApplicationModel().getScene();
        scene.scene.addLight(new PointLight(new Vector3Dd(x, y, z),
            new ColorRgb(r, g, b)));
    }

    private void addSphere(String request)
    {
        double radius = numberProperty(request, "radius", 1.0);
        double x = numberProperty(request, "x", 0.0);
        double y = numberProperty(request, "y", 0.0);
        double z = numberProperty(request, "z", 0.0);
        SimpleBody body = parent.getApplicationModel().getScene()
            .addThing(new Sphere(radius));
        body.setPosition(new Vector3Dd(x, y, z));
    }

    private void addCone(String request)
    {
        double baseRadius = numberProperty(request, "baseRadius", 1.0);
        double topRadius = numberProperty(request, "topRadius", 0.0);
        double height = numberProperty(request, "height", 2.0);
        placeNewBody(new Cone(baseRadius, topRadius, height), request);
    }

    private void addCylinder(String request)
    {
        double radius = numberProperty(request, "radius", 1.0);
        double height = numberProperty(request, "height", 2.0);
        placeNewBody(new Cone(radius, radius, height), request);
    }

    private void placeNewBody(Geometry geometry, String request)
    {
        SimpleBody body = parent.getApplicationModel().getScene().addThing(geometry);
        body.setPosition(new Vector3Dd(
            numberProperty(request, "x", 0.0),
            numberProperty(request, "y", 0.0),
            numberProperty(request, "z", 0.0)));
    }

    private void moveBody(String request)
    {
        ArrayList<SimpleBody> bodies =
            parent.getApplicationModel().getScene().scene.getSimpleBodies();
        int index = (int)numberProperty(request, "index", bodies.size() - 1);

        if ( index < 0 || index >= bodies.size() ) {
            throw new IllegalArgumentException("Body index out of range: " + index);
        }
        SimpleBody body = bodies.get(index);
        Vector3Dd p = body.getPosition();

        body.setPosition(new Vector3Dd(
            numberProperty(request, "x", p.x()),
            numberProperty(request, "y", p.y()),
            numberProperty(request, "z", p.z())));
    }

    private String setInteractionMode(String request)
    {
        String mode = stringProperty(request, "mode", "");
        Jogl4DrawingAreaRenderer drawingArea = parent.getJogl4Controller().getDrawingArea();
        int value;

        if ( drawingArea == null ) {
            throw new IllegalStateException("Jogl4DrawingAreaRenderer has not been created");
        }
        switch ( mode ) {
            case "camera" -> value = Jogl4DrawingAreaRenderer.CAMERA_INTERACTION_MODE;
            case "select" -> value = Jogl4DrawingAreaRenderer.SELECT_INTERACTION_MODE;
            case "translate" -> value = Jogl4DrawingAreaRenderer.TRANSLATE_INTERACTION_MODE;
            case "rotate" -> value = Jogl4DrawingAreaRenderer.ROTATE_INTERACTION_MODE;
            case "scale" -> value = Jogl4DrawingAreaRenderer.SCALE_INTERACTION_MODE;
            default -> throw new IllegalArgumentException("Unknown mode \"" + mode +
                "\". Use camera, select, translate, rotate or scale");
        }
        drawingArea.interactionMode = value;
        return "{\"ok\":true,\"mode\":\"" + mode + "\"}";
    }

    private String injectMouse(String request)
    {
        Jogl4DrawingAreaRenderer drawingArea = parent.getJogl4Controller().getDrawingArea();
        String type = stringProperty(request, "type", "move");
        int x = (int)Math.round(numberProperty(request, "x", 0));
        int y = (int)Math.round(numberProperty(request, "y", 0));
        int button = (int)numberProperty(request, "button", 1);

        if ( drawingArea == null ) {
            throw new IllegalStateException("Jogl4DrawingAreaRenderer has not been created");
        }
        drawingArea.injectMouseEvent(type, x, y, button);
        return describeScene();
    }

    /**
    Reports the canvas pixel of the first selected body and of the tips of its
    three axes (one unit long), as seen by a viewport.
    */
    private String projectSelectedBody(String request)
    {
        Jogl4DrawingAreaRenderer drawingArea = parent.getJogl4Controller().getDrawingArea();
        Scene scene = parent.getApplicationModel().getScene();
        ViewportSet set = parent.getApplicationModel().getActiveViewportSet();
        int viewportIndex = (int)numberProperty(request, "viewport", 0);
        int selected = scene.selectedThings.firstSelected();

        if ( drawingArea == null ) {
            throw new IllegalStateException("Jogl4DrawingAreaRenderer has not been created");
        }
        if ( selected < 0 ) {
            throw new IllegalStateException("No body is selected");
        }
        Viewport viewport = set.getViewport(viewportIndex);
        Vector3Dd p = scene.scene.getSimpleBodies().get(selected).getPosition();
        String[] names = {"origin", "x", "y", "z"};
        Vector3Dd[] points = {
            p,
            p.add(new Vector3Dd(1, 0, 0)),
            p.add(new Vector3Dd(0, 1, 0)),
            p.add(new Vector3Dd(0, 0, 1))
        };
        StringBuilder sb = new StringBuilder("{\"viewport\":\"" + escape(viewport.getTitle()) + "\"");

        for ( int i = 0; i < names.length; i++ ) {
            double[] pixel = drawingArea.projectToCanvas(viewport, points[i]);

            sb.append(",\"").append(names[i]).append("\":");
            sb.append(pixel == null ? "null" : "[" + pixel[0] + "," + pixel[1] + "]");
        }
        sb.append('}');
        return sb.toString();
    }

    private void selectBody(String request)
    {
        Scene scene = parent.getApplicationModel().getScene();
        int index = (int)numberProperty(request, "index", -1);

        scene.selectedThings.unselectAll();
        if ( index >= 0 ) {
            if ( index >= scene.scene.getSimpleBodies().size() ) {
                throw new IllegalArgumentException("Body index out of range: " + index);
            }
            scene.selectedThings.select(index);
        }
    }

    /**
    @return the viewports selected by the "viewport" argument: an index, or
    all of them when it is missing
    */
    private ArrayList<Viewport> selectedViewports(String request)
    {
        ViewportSet set = parent.getApplicationModel().getActiveViewportSet();
        ArrayList<Viewport> out = new ArrayList<>();
        double index = numberProperty(request, "viewport", -1);

        if ( index < 0 ) {
            out.addAll(set.getViewports());
        }
        else if ( index < set.getViewportCount() ) {
            out.add(set.getViewport((int)index));
        }
        else {
            throw new IllegalArgumentException("Viewport index out of range: " + (int)index);
        }
        return out;
    }

    private void setRendererConfiguration(String request)
    {
        for ( Viewport viewport : selectedViewports(request) ) {
            RendererConfiguration q = viewport.getRendererConfiguration();
            Boolean value;

            value = booleanProperty(request, "points");
            if ( value != null ) q.setPoints(value);
            value = booleanProperty(request, "wires");
            if ( value != null ) q.setWires(value);
            value = booleanProperty(request, "surfaces");
            if ( value != null ) q.setSurfaces(value);
            value = booleanProperty(request, "texture");
            if ( value != null ) q.setTexture(value);
            value = booleanProperty(request, "bumpMap");
            if ( value != null ) q.setBumpMap(value);
            value = booleanProperty(request, "boundingVolume");
            if ( value != null ) q.setBoundingVolume(value);
            value = booleanProperty(request, "normals");
            if ( value != null ) q.setNormals(value);
            value = booleanProperty(request, "trianglesNormals");
            if ( value != null ) q.setTrianglesNormals(value);
            value = booleanProperty(request, "selectionCorners");
            if ( value != null ) q.setSelectionCorners(value);

            String shading = stringProperty(request, "shading", "");
            if ( !shading.isEmpty() ) {
                q.setShadingType(ShadingType.valueOf(shading.toUpperCase()));
            }
        }
    }

    private String describeRendererConfigurations(String request)
    {
        StringBuilder sb = new StringBuilder("{\"viewports\":[");
        ViewportSet set = parent.getApplicationModel().getActiveViewportSet();
        boolean first = true;

        for ( Viewport viewport : selectedViewports(request) ) {
            RendererConfiguration q = viewport.getRendererConfiguration();

            if ( !first ) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"index\":").append(set.getViewports().indexOf(viewport))
                .append(",\"title\":\"").append(escape(viewport.getTitle())).append('"')
                .append(",\"points\":").append(q.isPointsSet())
                .append(",\"wires\":").append(q.isWiresSet())
                .append(",\"surfaces\":").append(q.isSurfacesSet())
                .append(",\"texture\":").append(q.isTextureSet())
                .append(",\"bumpMap\":").append(q.isBumpMapSet())
                .append(",\"boundingVolume\":").append(q.isBoundingVolumeSet())
                .append(",\"normals\":").append(q.isNormalsSet())
                .append(",\"trianglesNormals\":").append(q.isTrianglesNormalsSet())
                .append(",\"selectionCorners\":").append(q.isSelectionCornersSet())
                .append(",\"shading\":\"").append(q.getShadingTypeEnum()).append("\"}");
        }
        return sb.append("]}").toString();
    }

    private String raytracePng(String request)
    {
        String path = stringProperty(request, "path", "./mcp-raytrace.png");
        int width = (int)numberProperty(request, "width", 640);
        int height = (int)numberProperty(request, "height", 480);
        parent.getApplicationModel().setRaytracedImageWidth(width);
        parent.getApplicationModel().setRaytracedImageHeight(height);
        parent.doRaytracingImage();
        File out = new File(path);
        ImagePersistence.exportPNG(out, parent.getApplicationModel().getRaytracedImage());
        return "{\"ok\":true,\"path\":\"" + escape(out.getAbsolutePath()) + "\"}";
    }

    private String viewportJpg(String request)
    {
        String path = stringProperty(request, "path", "./outputSelectedViewport.jpg");
        Jogl4DrawingAreaRenderer drawingArea = parent.getJogl4Controller().getDrawingArea();
        if ( drawingArea == null ) {
            throw new IllegalStateException("Jogl4DrawingAreaRenderer has not been created");
        }
        File out = new File(path);
        drawingArea.exportViewportJpg(out);
        return "{\"ok\":true,\"path\":\"" + escape(out.getAbsolutePath()) + "\"}";
    }

    private String workspaceJpg(String request)
    {
        String path = stringProperty(request, "path", "./outputViewport.jpg");
        Jogl4DrawingAreaRenderer drawingArea = parent.getJogl4Controller().getDrawingArea();
        if ( drawingArea == null ) {
            throw new IllegalStateException("Jogl4DrawingAreaRenderer has not been created");
        }
        File out = new File(path);
        drawingArea.exportWorkspaceJpg(out);
        return "{\"ok\":true,\"path\":\"" + escape(out.getAbsolutePath()) + "\"}";
    }

    private String describeScene()
    {
        Scene scene = parent.getApplicationModel().getScene();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"bodies\":[");
        ArrayList<SimpleBody> bodies = scene.scene.getSimpleBodies();
        for ( int i = 0; i < bodies.size(); i++ ) {
            if ( i > 0 ) {
                sb.append(',');
            }
            SimpleBody body = bodies.get(i);
            Geometry geometry = body.getGeometry();
            Vector3Dd position = body.getPosition();
            Vector3Dd scale = body.getScale();
            sb.append("{\"index\":").append(i)
                .append(",\"name\":\"").append(escape(body.getName())).append('"')
                .append(",\"geometry\":\"").append(geometry.getClass().getSimpleName()).append('"')
                .append(",\"position\":").append(vector(position))
                .append(",\"scale\":").append(vector(scale));
            if ( geometry instanceof Sphere ) {
                sb.append(",\"radius\":").append(((Sphere)geometry).getRadius());
            }
            sb.append('}');
        }
        sb.append("],\"lights\":[");
        ArrayList<Light> lights = scene.scene.getLights();
        for ( int i = 0; i < lights.size(); i++ ) {
            if ( i > 0 ) {
                sb.append(',');
            }
            Light light = lights.get(i);
            sb.append("{\"index\":").append(i)
                .append(",\"type\":\"").append(light.getClass().getSimpleName()).append('"')
                .append(",\"position\":").append(vector(light.getPosition()))
                .append(",\"emission\":").append(color(light.getEmission()))
                .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String toolsJson()
    {
        return "{\"tools\":["
            + tool("scene.describe", "Return bodies, lights and core transforms as JSON.")
            + "," + tool("scene.clear", "Remove all bodies, lights and debug groups.")
            + "," + tool("scene.add_point_light", "Create a point light. Arguments: x,y,z,r,g,b.")
            + "," + tool("scene.add_sphere", "Create a sphere. Arguments: radius,x,y,z.")
            + "," + tool("scene.add_cone", "Create a cone (or truncated cone). Arguments: baseRadius,topRadius,height,x,y,z.")
            + "," + tool("scene.add_cylinder", "Create a cylinder. Arguments: radius,height,x,y,z.")
            + "," + tool("scene.move_body", "Set the position of a body (default: the last one). Arguments: index,x,y,z (missing coordinates are kept).")
            + "," + tool("scene.select_body", "Select one body (a negative index clears the selection). Arguments: index.")
            + "," + tool("gui.set_mode", "Set the interaction mode. Arguments: mode (camera|select|translate|rotate|scale).")
            + "," + tool("gui.mouse", "Inject a mouse event into the canvas. Arguments: type (move|press|drag|release), x, y (canvas pixels), button (default 1). Returns the scene state.")
            + "," + tool("viewport.project", "Canvas pixels of the selected body origin and its x, y, z unit-axis tips in a viewport. Arguments: viewport (index, default 0).")
            + "," + tool("render.get_configuration", "Return the RendererConfiguration flags of the viewports. Arguments: viewport (index; default all).")
            + "," + tool("render.set_configuration", "Set RendererConfiguration flags bit by bit. Arguments: viewport (index; default all), and any of the booleans points,wires,surfaces,texture,bumpMap,boundingVolume,normals,trianglesNormals,selectionCorners, and shading (nolight|flat|gouraud|phong|cook_terrance).")
            + "," + tool("render.raytrace_png", "Raytrace the scene and export PNG. Arguments: path,width,height.")
            + "," + tool("viewport.export_jpg", "Export the selected JOGL4 viewport to JPG. Arguments: path.")
            + "," + tool("workspace.export_jpg", "Export the complete JOGL4 workspace area, including all viewports, to JPG. Arguments: path.")
            + "," + tool("gui.list_languages", "List the languages available for the GUI (I18N files in etc/gui), marking the current one.")
            + "," + tool("gui.set_language", "Change the GUI language, rebuilding the GUI. Arguments: language (an id given by gui.list_languages).")
            + "," + tool("app.exit", "Close the application (after answering this call).")
            + "]}";
    }

    private static String tool(String name, String description)
    {
        return "{\"name\":\"" + name + "\",\"description\":\""
            + escape(description)
            + "\",\"inputSchema\":{\"type\":\"object\",\"additionalProperties\":true}}";
    }

    private static String content(String json)
    {
        return "{\"content\":[{\"type\":\"text\",\"text\":\""
            + escape(json) + "\"}],\"isError\":false}";
    }

    private static String result(String id, String json)
    {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":" + json + "}";
    }

    private static String error(String id, int code, String message)
    {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id
            + ",\"error\":{\"code\":" + code + ",\"message\":\""
            + escape(message) + "\"}}";
    }

    private static String vector(Vector3Dd v)
    {
        return "{\"x\":" + v.x() + ",\"y\":" + v.y() + ",\"z\":" + v.z() + "}";
    }

    private static String color(ColorRgb c)
    {
        return "{\"r\":" + c.r() + ",\"g\":" + c.g() + ",\"b\":" + c.b() + "}";
    }

    private static String stringProperty(String json, String key, String defaultValue)
    {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key)
            + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if ( !matcher.find() ) {
            return defaultValue;
        }
        return matcher.group(1);
    }

    private static String nestedStringProperty(String json, String key, String defaultValue)
    {
        return stringProperty(json, key, defaultValue);
    }

    private static String idProperty(String json)
    {
        Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|[-0-9]+|null)");
        Matcher matcher = pattern.matcher(json);
        if ( !matcher.find() ) {
            return "null";
        }
        return matcher.group(1);
    }

    private static double numberProperty(String json, String key, double defaultValue)
    {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key)
            + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
        Matcher matcher = pattern.matcher(json);
        if ( !matcher.find() ) {
            return defaultValue;
        }
        return Double.parseDouble(matcher.group(1));
    }

    private static Boolean booleanProperty(String json, String key)
    {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key)
            + "\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        if ( !matcher.find() ) {
            return null;
        }
        return Boolean.valueOf(matcher.group(1));
    }

    private static String escape(String in)
    {
        if ( in == null ) {
            return "";
        }
        return in.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

public class VitralEditorMCP implements Runnable
{
    private final SceneEditorApplication parent;
    private final int tcpPort;

    public VitralEditorMCP(SceneEditorApplication parent)
    {
        this.parent = parent;
        tcpPort = 1234;
        Thread networkThread = new Thread(this);
        networkThread.setName("VitralEditorMCP");
        networkThread.start();
    }

    @Override
    public void run()
    {
        System.out.println("Waiting for MCP connections on TCP port " + tcpPort);

        try ( ServerSocket serverSocket = new ServerSocket(tcpPort) ) {
            while ( true ) {
                Socket clientSocket = serverSocket.accept();
                VitralEditorMCPProtocol listener =
                    new VitralEditorMCPProtocol(parent, clientSocket);
                Thread listenerThread = new Thread(listener);
                listenerThread.setName("VitralEditorMCPClient");
                listenerThread.start();
            }
        }
        catch ( Exception e ) {
            System.err.println("Error in VitralEditorMCP communications!");
            System.err.println(e);
        }
    }
}

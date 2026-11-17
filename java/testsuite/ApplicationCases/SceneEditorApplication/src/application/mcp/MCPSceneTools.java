package application.mcp;

import java.util.ArrayList;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Tube;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;

import model.ApplicationModel;
import model.Scene;
import model.SceneLightFactory;
import model.history.EditHistory;
import model.history.UndoQueue;

import application.commands.CommandResult;
import application.commands.GuiEventExecutor;

/**
Tools of the automation service of the editor that query and edit the scene
of the application model: description, bodies, lights, selection, edit history
and the GUI commands that only work over the model. The changes are recorded
in the scene history (named after the tool), so they can be undone. It does not
depend on any GUI or rendering technology: repainting after a change is up to
the caller.
*/
class MCPSceneTools
{
    private final ApplicationModel model;

    /**
    @param model application model the tools work over
    */
    MCPSceneTools(ApplicationModel model)
    {
        this.model = model;
    }

    private Scene scene()
    {
        return model.getScene();
    }

    private void recordSceneChange(String tool, Runnable change)
    {
        model.getEditHistory().getSceneHistory().perform(tool, change);
    }

    /**
    @return the bodies (placement and parameters of their geometries) and
    lights of the scene as JSON (tool `scene.describe`)
    */
    String describeScene()
    {
        return new MCPSceneSerializer(scene(), false).toJson(-1);
    }

    /**
    @param request request with the optional argument index
    @return the bodies with their materials and the full data of their
    geometries, and the lights, as JSON (tool `scene.serialize`); only the
    body `index` if given
    */
    String serializeScene(String request)
    {
        int index = (int)MCPJson.numberProperty(request, "index", -1);
        return new MCPSceneSerializer(scene(), true).toJson(index);
    }

    /**
    Removes all bodies, lights and debug groups (tool `scene.clear`).
    */
    void clearScene()
    {
        recordSceneChange("scene.clear", () -> {
            Scene scene = scene();
            scene.scene.getSimpleBodies().clear();
            scene.scene.getLights().clear();
            scene.debugThingGroups.clear();
        });
    }

    /**
    Adds a point light (tool `scene.add_point_light`).
    @param request request with the optional arguments x,y,z,r,g,b
    */
    void addPointLight(String request)
    {
        recordSceneChange("scene.add_point_light", () -> createPointLight(request));
    }

    private void createPointLight(String request)
    {
        boolean explicitPosition = MCPJson.hasProperty(request, "x") ||
            MCPJson.hasProperty(request, "y") || MCPJson.hasProperty(request, "z");
        boolean explicitColor = MCPJson.hasProperty(request, "r") ||
            MCPJson.hasProperty(request, "g") || MCPJson.hasProperty(request, "b");

        if ( !explicitPosition && !explicitColor ) {
            model.addNewLight();
            return;
        }
        // Missing values follow the default policy of the first light
        PointLight defaults = new SceneLightFactory().createLight(
            new ArrayList<Light>(), model.getActiveViewportSet());
        Vector3Dd p = defaults == null ? new Vector3Dd() : defaults.getPosition();
        double x = MCPJson.numberProperty(request, "x", p.x());
        double y = MCPJson.numberProperty(request, "y", p.y());
        double z = MCPJson.numberProperty(request, "z", p.z());
        double r = MCPJson.numberProperty(request, "r", 1.0);
        double g = MCPJson.numberProperty(request, "g", 1.0);
        double b = MCPJson.numberProperty(request, "b", 1.0);
        scene().scene.addLight(new PointLight(new Vector3Dd(x, y, z),
            new ColorRgb(r, g, b)));
    }

    /**
    Adds a sphere (tool `scene.add_sphere`).
    @param request request with the arguments radius,x,y,z
    */
    void addSphere(String request)
    {
        double radius = MCPJson.numberProperty(request, "radius", 1.0);
        recordSceneChange("scene.add_sphere",
            () -> placeNewBody(new Sphere(radius), request));
    }

    /**
    Adds a cone (tool `scene.add_cone`).
    @param request request with the arguments baseRadius,topRadius,height,x,y,z
    */
    void addCone(String request)
    {
        double baseRadius = MCPJson.numberProperty(request, "baseRadius", 1.0);
        double topRadius = MCPJson.numberProperty(request, "topRadius", 0.0);
        double height = MCPJson.numberProperty(request, "height", 2.0);
        recordSceneChange("scene.add_cone",
            () -> placeNewBody(new Cone(baseRadius, topRadius, height), request));
    }

    /**
    Adds a cylinder (tool `scene.add_cylinder`).
    @param request request with the arguments radius,height,x,y,z
    */
    void addCylinder(String request)
    {
        double radius = MCPJson.numberProperty(request, "radius", 1.0);
        double height = MCPJson.numberProperty(request, "height", 2.0);
        recordSceneChange("scene.add_cylinder",
            () -> placeNewBody(new Cone(radius, radius, height), request));
    }

    /**
    Adds a tube, or a washer when its height is 0 (tool `scene.add_tube`).
    @param request request with the arguments
    internalRadius,externalRadius,height,x,y,z
    */
    void addTube(String request)
    {
        double internalRadius = MCPJson.numberProperty(request, "internalRadius", 0.5);
        double externalRadius = MCPJson.numberProperty(request, "externalRadius", 1.0);
        double height = MCPJson.numberProperty(request, "height", 2.0);
        recordSceneChange("scene.add_tube",
            () -> placeNewBody(new Tube(internalRadius, externalRadius, height),
                request));
    }

    private void placeNewBody(Geometry geometry, String request)
    {
        SimpleBody body = scene().addThing(geometry);
        body.setPosition(new Vector3Dd(
            MCPJson.numberProperty(request, "x", 0.0),
            MCPJson.numberProperty(request, "y", 0.0),
            MCPJson.numberProperty(request, "z", 0.0)));
    }

    /**
    Sets the position of a body (tool `scene.move_body`).
    @param request request with the arguments index,x,y,z
    */
    void moveBody(String request)
    {
        recordSceneChange("scene.move_body", () -> {
            ArrayList<SimpleBody> bodies = scene().scene.getSimpleBodies();
            int index = (int)MCPJson.numberProperty(request, "index", bodies.size() - 1);

            if ( index < 0 || index >= bodies.size() ) {
                throw new IllegalArgumentException("Body index out of range: " + index);
            }
            SimpleBody body = bodies.get(index);
            Vector3Dd p = body.getPosition();

            body.setPosition(new Vector3Dd(
                MCPJson.numberProperty(request, "x", p.x()),
                MCPJson.numberProperty(request, "y", p.y()),
                MCPJson.numberProperty(request, "z", p.z())));
        });
    }

    /**
    Selects one body, or none (tool `scene.select_body`).
    @param request request with the argument index
    */
    void selectBody(String request)
    {
        Scene scene = scene();
        int index = (int)MCPJson.numberProperty(request, "index", -1);

        scene.selectedThings.unselectAll();
        if ( index >= 0 ) {
            if ( index >= scene.scene.getSimpleBodies().size() ) {
                throw new IllegalArgumentException("Body index out of range: " + index);
            }
            scene.selectedThings.select(index);
        }
    }

    /**
    Executes a GUI command that only works over the model (tool
    `gui.command`).
    @param request request with the argument command
    @return the result of the command and its status message as JSON
    */
    String executeGuiCommand(String request)
    {
        String command = MCPJson.stringProperty(request, "command", "");
        final String[] message = new String[] { "" };
        GuiEventExecutor executor = new GuiEventExecutor(
            model, text -> message[0] = text);
        CommandResult result = executor.execute(command);

        return "{\"command\":\"" + MCPJson.escape(command) + "\",\"result\":\"" + result +
            "\",\"message\":\"" + MCPJson.escape(message[0]) + "\"}";
    }

    /**
    @return the undo/redo state of the scene history and of the view history
    of each viewport as JSON (tool `edit.history`)
    */
    String describeEditHistory()
    {
        EditHistory history = model.getEditHistory();
        ViewportSet viewportSet = model.getActiveViewportSet();
        StringBuilder sb = new StringBuilder();
        int i;

        sb.append("{\"scene\":")
            .append(queueJson(history.getSceneHistory().getQueue()))
            .append(",\"viewports\":[");
        for ( i = 0; i < viewportSet.getViewportCount(); i++ ) {
            Viewport viewport = viewportSet.getViewport(i);

            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append("{\"index\":").append(i)
                .append(",\"title\":\"").append(MCPJson.escape(viewport.getTitle())).append('"')
                .append(",\"selected\":").append(viewport == viewportSet.getSelectedViewport())
                .append(",\"history\":")
                .append(queueJson(history.getViewportHistory().getQueue(viewport)))
                .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String queueJson(UndoQueue queue)
    {
        String undoName = queue.getUndoName();
        String redoName = queue.getRedoName();

        return "{\"undo\":" + queue.getUndoCount() +
            ",\"redo\":" + queue.getRedoCount() +
            ",\"nextUndo\":" + (undoName == null ? "null" : "\"" + MCPJson.escape(undoName) + "\"") +
            ",\"nextRedo\":" + (redoName == null ? "null" : "\"" + MCPJson.escape(redoName) + "\"") +
            "}";
    }
}

package application.mcp;

import java.io.File;

import javax.swing.Timer;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;
import vsdk.toolkit.io.image.ImagePersistence;

import model.Scene;

import application.AwtJogl4ApplicationController;
import application.AwtJogl4SceneEditorApplication;

/**
Tools of the automation service of the editor that need the running Swing/GL4
application: input events injected into the drawing area, projection of
points to its pixels, images exported from it or raytraced, the GUI language
and the end of the application.
*/
class AwtJogl4MCPApplicationTools
{
    private final AwtJogl4SceneEditorApplication parent;

    /**
    @param parent application the tools work over
    */
    AwtJogl4MCPApplicationTools(AwtJogl4SceneEditorApplication parent)
    {
        this.parent = parent;
    }

    /**
    Repaints the drawing area, i.e. after a change of the model.
    */
    void repaint()
    {
        parent.getJogl4Controller().repaint();
    }

    private AwtJogl4ApplicationController getDrawingAreaController()
    {
        AwtJogl4ApplicationController controller = parent.getJogl4Controller();

        if ( !controller.isDrawingAreaCreated() ) {
            throw new IllegalStateException("The drawing area has not been created");
        }
        return controller;
    }

    //= Drawing area ======================================================

    /**
    Sends a mouse event to the drawing area (tool `gui.mouse`).
    @param request request with the arguments type,x,y,button
    */
    void injectMouse(String request)
    {
        AwtJogl4ApplicationController drawingArea = getDrawingAreaController();
        String type = MCPJson.stringProperty(request, "type", "move");
        int x = (int)Math.round(MCPJson.numberProperty(request, "x", 0));
        int y = (int)Math.round(MCPJson.numberProperty(request, "y", 0));
        int button = (int)MCPJson.numberProperty(request, "button", 1);

        drawingArea.injectMouseEvent(type, x, y, button);
    }

    /**
    Sends a key press to the drawing area (tool `gui.key`).
    @param request request with the arguments key,shift,ctrl
    */
    void injectKey(String request)
    {
        AwtJogl4ApplicationController drawingArea = getDrawingAreaController();
        String key = MCPJson.stringProperty(request, "key", "");
        boolean shift = Boolean.TRUE.equals(MCPJson.booleanProperty(request, "shift"));
        boolean ctrl = Boolean.TRUE.equals(MCPJson.booleanProperty(request, "ctrl"));

        drawingArea.injectKeyEvent(key, shift, ctrl);
    }

    /**
    @param request request with the argument viewport (index, default 0)
    @return the drawing area pixels of the origin of the first selected body
    and of its unit axis tips, as JSON (tool `viewport.project`)
    */
    String projectSelectedBody(String request)
    {
        AwtJogl4ApplicationController drawingArea = getDrawingAreaController();
        Scene scene = parent.getApplicationModel().getScene();
        ViewportSet set = parent.getApplicationModel().getActiveViewportSet();
        int viewportIndex = (int)MCPJson.numberProperty(request, "viewport", 0);
        int selected = scene.selectedThings.firstSelected();

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
        StringBuilder sb = new StringBuilder("{\"viewport\":\"" +
            MCPJson.escape(viewport.getTitle()) + "\"");

        for ( int i = 0; i < names.length; i++ ) {
            double[] pixel = drawingArea.projectToCanvas(viewport, points[i]);

            sb.append(",\"").append(names[i]).append("\":");
            sb.append(pixel == null ? "null" : "[" + pixel[0] + "," + pixel[1] + "]");
        }
        sb.append('}');
        return sb.toString();
    }

    //= Images ============================================================

    /**
    Raytraces the scene and exports it to a PNG (tool `render.raytrace_png`).
    @param request request with the arguments path,width,height
    @return the path of the written file as JSON
    */
    String raytracePng(String request)
    {
        String path = MCPJson.stringProperty(request, "path", "./mcp-raytrace.png");
        int width = (int)MCPJson.numberProperty(request, "width", 640);
        int height = (int)MCPJson.numberProperty(request, "height", 480);
        parent.getApplicationModel().setRaytracedImageWidth(width);
        parent.getApplicationModel().setRaytracedImageHeight(height);
        parent.doRaytracingImage();
        File out = new File(path);
        ImagePersistence.exportPNG(out, parent.getApplicationModel().getRaytracedImage());
        return MCPJson.writtenFile(out.getAbsolutePath());
    }

    /**
    Exports the selected viewport, as drawn, to a JPG (tool
    `viewport.export_jpg`).
    @param request request with the argument path
    @return the path of the written file as JSON
    */
    String exportViewportJpg(String request)
    {
        String path = MCPJson.stringProperty(request, "path", "./outputSelectedViewport.jpg");
        AwtJogl4ApplicationController drawingArea = getDrawingAreaController();
        File out = new File(path);
        drawingArea.exportViewportJpg(out);
        return MCPJson.writtenFile(out.getAbsolutePath());
    }

    /**
    Exports the whole drawing area to a JPG (tool `workspace.export_jpg`).
    @param request request with the argument path
    @return the path of the written file as JSON
    */
    String exportWorkspaceJpg(String request)
    {
        String path = MCPJson.stringProperty(request, "path", "./outputViewport.jpg");
        AwtJogl4ApplicationController drawingArea = getDrawingAreaController();
        File out = new File(path);
        drawingArea.exportWorkspaceJpg(out);
        return MCPJson.writtenFile(out.getAbsolutePath());
    }

    //= Application =======================================================

    /**
    @return the languages of the GUI, marking the current one, as JSON (tool
    `gui.list_languages`)
    */
    String listLanguages()
    {
        StringBuilder sb = new StringBuilder();
        String current = parent.getCurrentGuiLanguage();
        boolean first = true;

        sb.append("{\"current\":\"").append(MCPJson.escape(current)).append("\",\"languages\":[");
        for ( String language : parent.getGuiLanguages() ) {
            if ( !first ) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"id\":\"").append(MCPJson.escape(language)).append('"')
                .append(",\"current\":").append(language.equals(current)).append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
    Changes the GUI language, rebuilding the GUI (tool `gui.set_language`).
    @param request request with the argument language
    @return the language set as JSON
    */
    String setLanguage(String request)
    {
        String language = MCPJson.stringProperty(request, "language", "");

        if ( !parent.setGuiLanguageById(language) ) {
            throw new IllegalArgumentException("Unknown language \"" + language +
                "\". Available languages: " + parent.getGuiLanguages());
        }
        return "{\"ok\":true,\"language\":\"" + MCPJson.escape(language) + "\"}";
    }

    /**
    Closes the application shortly, so the answer of this tool can be sent
    before (tool `app.exit`).
    @return the answer of the tool as JSON
    */
    String exitApplication()
    {
        Timer timer = new Timer(300, e -> parent.closeApplication());

        timer.setRepeats(false);
        timer.start();
        return "{\"ok\":true,\"message\":\"The application is closing\"}";
    }
}

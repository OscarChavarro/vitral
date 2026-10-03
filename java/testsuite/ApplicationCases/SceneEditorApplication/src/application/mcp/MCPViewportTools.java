package application.mcp;

import java.util.ArrayList;

import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.ShadingType;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;

import model.ApplicationModel;
import model.InteractionMode;

/**
Tools of the automation service of the editor that query and change how the
viewports of the application model are shown and used: the rendering
configuration of each viewport and the interaction mode of the drawing area.
It does not depend on any GUI or rendering technology.
*/
class MCPViewportTools
{
    private final ApplicationModel model;

    /**
    @param model application model the tools work over
    */
    MCPViewportTools(ApplicationModel model)
    {
        this.model = model;
    }

    /**
    Sets the interaction mode of the drawing area (tool `gui.set_mode`).
    @param request request with the argument mode
    @return the mode set as JSON
    */
    String setInteractionMode(String request)
    {
        String mode = MCPJson.stringProperty(request, "mode", "");
        InteractionMode value;

        switch ( mode ) {
            case "camera" -> value = InteractionMode.CAMERA;
            case "select" -> value = InteractionMode.SELECT;
            case "translate" -> value = InteractionMode.TRANSLATE;
            case "rotate" -> value = InteractionMode.ROTATE;
            case "scale" -> value = InteractionMode.SCALE;
            default -> throw new IllegalArgumentException("Unknown mode \"" + mode +
                "\". Use camera, select, translate, rotate or scale");
        }
        model.getDrawingArea().setInteractionMode(value);
        return "{\"ok\":true,\"mode\":\"" + mode + "\"}";
    }

    private ArrayList<Viewport> selectedViewports(String request)
    {
        ViewportSet set = model.getActiveViewportSet();
        ArrayList<Viewport> out = new ArrayList<>();
        double index = MCPJson.numberProperty(request, "viewport", -1);

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

    /**
    Sets, only in the given values, the rendering configuration of one
    viewport or of all of them (tool `render.set_configuration`).
    @param request request with the argument viewport, and the values to set
    */
    void setRendererConfiguration(String request)
    {
        for ( Viewport viewport : selectedViewports(request) ) {
            RendererConfiguration q = viewport.getRendererConfiguration();
            Boolean value;

            value = MCPJson.booleanProperty(request, "points");
            if ( value != null ) q.setPoints(value);
            value = MCPJson.booleanProperty(request, "wires");
            if ( value != null ) q.setWires(value);
            value = MCPJson.booleanProperty(request, "surfaces");
            if ( value != null ) q.setSurfaces(value);
            value = MCPJson.booleanProperty(request, "texture");
            if ( value != null ) q.setTexture(value);
            value = MCPJson.booleanProperty(request, "bumpMap");
            if ( value != null ) q.setBumpMap(value);
            value = MCPJson.booleanProperty(request, "boundingVolume");
            if ( value != null ) q.setBoundingVolume(value);
            value = MCPJson.booleanProperty(request, "normals");
            if ( value != null ) q.setNormals(value);
            value = MCPJson.booleanProperty(request, "trianglesNormals");
            if ( value != null ) q.setTrianglesNormals(value);
            value = MCPJson.booleanProperty(request, "selectionCorners");
            if ( value != null ) q.setSelectionCorners(value);

            String shading = MCPJson.stringProperty(request, "shading", "");
            if ( !shading.isEmpty() ) {
                q.setShadingType(ShadingType.valueOf(shading.toUpperCase()));
            }

            value = MCPJson.booleanProperty(request, "grid");
            if ( value != null ) viewport.setShowGrid(value);
            String renderMode = MCPJson.stringProperty(request, "renderMode", "");
            if ( !renderMode.isEmpty() ) {
                int mode = renderModeFromName(renderMode);
                if ( !model.getActiveViewportSet().isRenderModeAvailable(mode) ) {
                    throw new IllegalArgumentException("renderMode " + renderMode +
                        " is not available with the render technology " +
                        model.getDrawingArea().getRenderTechnology());
                }
                viewport.setRenderMode(mode);
            }
        }
    }

    /**
    @param request request with the argument viewport (index; default all)
    @return the rendering configuration of the viewports as JSON (tool
    `render.get_configuration`)
    */
    String describeRendererConfigurations(String request)
    {
        StringBuilder sb = new StringBuilder("{\"viewports\":[");
        ViewportSet set = model.getActiveViewportSet();
        boolean first = true;

        for ( Viewport viewport : selectedViewports(request) ) {
            RendererConfiguration q = viewport.getRendererConfiguration();

            if ( !first ) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"index\":").append(set.getViewports().indexOf(viewport))
                .append(",\"title\":\"").append(MCPJson.escape(viewport.getTitle())).append('"')
                .append(",\"points\":").append(q.isPointsSet())
                .append(",\"wires\":").append(q.isWiresSet())
                .append(",\"surfaces\":").append(q.isSurfacesSet())
                .append(",\"texture\":").append(q.isTextureSet())
                .append(",\"bumpMap\":").append(q.isBumpMapSet())
                .append(",\"boundingVolume\":").append(q.isBoundingVolumeSet())
                .append(",\"normals\":").append(q.isNormalsSet())
                .append(",\"trianglesNormals\":").append(q.isTrianglesNormalsSet())
                .append(",\"selectionCorners\":").append(q.isSelectionCornersSet())
                .append(",\"shading\":\"").append(q.getShadingTypeEnum()).append('"')
                .append(",\"grid\":").append(viewport.isShowGrid())
                .append(",\"renderMode\":\"")
                .append(renderModeName(viewport.getRenderMode()))
                .append("\"}");
        }
        return sb.append("],\"technology\":\"")
            .append(model.getDrawingArea().getRenderTechnology().name().toLowerCase())
            .append("\"}").toString();
    }

    private static int renderModeFromName(String name)
    {
        return switch ( name.toLowerCase() ) {
            case "gpu" -> Viewport.RENDER_MODE_Z_BUFFER;
            case "cpu" -> Viewport.RENDER_MODE_RAYTRACING;
            case "hidden_lines" -> Viewport.RENDER_MODE_HIDDEN_LINES;
            default -> throw new IllegalArgumentException(
                "renderMode must be gpu, cpu or hidden_lines");
        };
    }

    private static String renderModeName(int mode)
    {
        return switch ( mode ) {
            case Viewport.RENDER_MODE_RAYTRACING -> "cpu";
            case Viewport.RENDER_MODE_HIDDEN_LINES -> "hidden_lines";
            default -> "gpu";
        };
    }
}

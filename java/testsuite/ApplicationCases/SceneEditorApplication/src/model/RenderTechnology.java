package model;

import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSetCommands;

/**
Technologies the editor can use to present the viewport set of its drawing
area. Each one offers a set of render modes to the viewports: OpenGL 4 is
used only for the GPU z-buffer, and every mode computed by the processor is
drawn over a 2D canvas without a 3D API (AWT): projected lines (wireframe or
Appel hidden line removal), projected visible polygons (Weiler/Atherton
hidden surface removal) or Whitted raytraced images. As the whole set is
presented by one technology, choosing a mode of the other one changes the
technology of the set (see `forRenderMode`).
*/
public enum RenderTechnology
{
    /** OpenGL 4 (JOGL) canvas */
    OPENGL4(ViewportSetCommands.IDV_RENDER_TECHNOLOGY_OPENGL4,
        Viewport.RENDER_MODE_Z_BUFFER),
    /** AWT canvas drawn with 2D operations only */
    AWT(ViewportSetCommands.IDV_RENDER_TECHNOLOGY_AWT,
        Viewport.RENDER_MODE_HIDDEN_LINES, Viewport.RENDER_MODE_HIDDEN_SURFACES,
        Viewport.RENDER_MODE_RAYTRACING);

    private final String command;
    private final int[] renderModes;

    RenderTechnology(String command, int... renderModes)
    {
        this.command = command;
        this.renderModes = renderModes;
    }

    /**
    @return the standard command of the viewport set that selects this
    technology (see `ViewportSetCommands`)
    */
    public String getCommand()
    {
        return command;
    }

    /**
    @return the render modes (`Viewport.RENDER_MODE_*`) available in this
    technology, the default one first
    */
    public int[] getRenderModes()
    {
        return renderModes.clone();
    }

    /**
    @param renderMode a `Viewport.RENDER_MODE_*`
    @param current the technology in use
    @return the technology offering the render mode: the current one if it
    offers it, or null if none does
    */
    public static RenderTechnology forRenderMode(int renderMode,
                                                 RenderTechnology current)
    {
        if ( current != null && current.offers(renderMode) ) {
            return current;
        }
        for ( RenderTechnology technology : values() ) {
            if ( technology.offers(renderMode) ) {
                return technology;
            }
        }
        return null;
    }

    private boolean offers(int renderMode)
    {
        for ( int mode : renderModes ) {
            if ( mode == renderMode ) {
                return true;
            }
        }
        return false;
    }

    /**
    @param command a command of the `VIEWPORT_SET_RENDER_TECHNOLOGY` popup
    @return the technology selected by the command, or null if it is not a
    render technology command
    */
    public static RenderTechnology fromCommand(String command)
    {
        for ( RenderTechnology technology : values() ) {
            if ( technology.command.equals(command) ) {
                return technology;
            }
        }
        return null;
    }
}

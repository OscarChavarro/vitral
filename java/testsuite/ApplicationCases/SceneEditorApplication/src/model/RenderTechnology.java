package model;

import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSetCommands;

/**
Technologies the editor can use to present the viewport set of its drawing
area. Each one offers a set of render modes to the viewports: a 2D canvas
without a 3D API (AWT) has no GPU z-buffer, so its viewports are drawn by the
processor, as raytraced images or as projected lines (wireframe or hidden
line removal).
*/
public enum RenderTechnology
{
    /** OpenGL 4 (JOGL) canvas */
    OPENGL4(ViewportSetCommands.IDV_RENDER_TECHNOLOGY_OPENGL4,
        Viewport.RENDER_MODE_Z_BUFFER, Viewport.RENDER_MODE_RAYTRACING),
    /** AWT canvas drawn with 2D operations only */
    AWT(ViewportSetCommands.IDV_RENDER_TECHNOLOGY_AWT,
        Viewport.RENDER_MODE_HIDDEN_LINES, Viewport.RENDER_MODE_RAYTRACING);

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

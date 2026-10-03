package vsdk.toolkit.gui.viewport;

/**
Names of the standard GUI elements the `ViewportSet` model uses from the I18N
context (the `Widget` built from the GUI definition of the application).

Popup menus are named after the element they serve, and their commands start
with `IDV_` (instead of the usual `IDC_` of application commands): they are
standard commands of the viewport set, defined by the framework, that any
application can reuse just by providing the popup and the commands, with their
texts in each language, in its GUI definition files. These popups are not part
of the main menubar of the application.

The `VIEWPORT_SET_PROJECTION_LOCATION` popup lists the places from where a
viewport projects the scene, in the same order as the cameras of `Viewport`.
Each of its items must have as modifier the corresponding command:
`IDV_VIEWPORT_SET_PROJECTION_LOCATION_PERSPECTIVE`, `..._TOP`, `..._BOTTOM`,
`..._LEFT` and `..._FRONT`.

The `VIEWPORT_SET_RENDER_MODE` popup lists the ways a viewport can be
rendered: `IDV_VIEWPORT_SET_RENDER_MODE_GPU` (the graphics API in use, i.e.
OpenGL; the default), `IDV_VIEWPORT_SET_RENDER_MODE_CPU` (raytracing in the
processor) and `IDV_VIEWPORT_SET_RENDER_MODE_CPU_HIDDEN_LINES` (lines computed
in the processor: wireframe or hidden line removal). Applications present it
after the projection location items, separated from them, showing only the
modes available in the technology presenting the set (see
`ViewportSet.getAvailableRenderModes`).

The `VIEWPORT_SET_RENDER_TECHNOLOGY` popup lists the technologies an
application can use to present the whole viewport set:
`IDV_VIEWPORT_SET_RENDER_TECHNOLOGY_OPENGL4` and
`IDV_VIEWPORT_SET_RENDER_TECHNOLOGY_AWT` (2D drawing only, without any 3D
API). The viewport set model does not process these commands: only the
application knows how to replace the drawing surface.
*/
public final class ViewportSetCommands
{
    public static final String POPUP_PROJECTION_LOCATION =
        "VIEWPORT_SET_PROJECTION_LOCATION";

    public static final String IDV_PROJECTION_LOCATION_PERSPECTIVE =
        "IDV_VIEWPORT_SET_PROJECTION_LOCATION_PERSPECTIVE";
    public static final String IDV_PROJECTION_LOCATION_TOP =
        "IDV_VIEWPORT_SET_PROJECTION_LOCATION_TOP";
    public static final String IDV_PROJECTION_LOCATION_BOTTOM =
        "IDV_VIEWPORT_SET_PROJECTION_LOCATION_BOTTOM";
    public static final String IDV_PROJECTION_LOCATION_LEFT =
        "IDV_VIEWPORT_SET_PROJECTION_LOCATION_LEFT";
    public static final String IDV_PROJECTION_LOCATION_FRONT =
        "IDV_VIEWPORT_SET_PROJECTION_LOCATION_FRONT";

    public static final String POPUP_RENDER_MODE =
        "VIEWPORT_SET_RENDER_MODE";

    public static final String IDV_RENDER_MODE_GPU =
        "IDV_VIEWPORT_SET_RENDER_MODE_GPU";
    public static final String IDV_RENDER_MODE_CPU =
        "IDV_VIEWPORT_SET_RENDER_MODE_CPU";
    public static final String IDV_RENDER_MODE_CPU_HIDDEN_LINES =
        "IDV_VIEWPORT_SET_RENDER_MODE_CPU_HIDDEN_LINES";

    public static final String POPUP_RENDER_TECHNOLOGY =
        "VIEWPORT_SET_RENDER_TECHNOLOGY";

    public static final String IDV_RENDER_TECHNOLOGY_OPENGL4 =
        "IDV_VIEWPORT_SET_RENDER_TECHNOLOGY_OPENGL4";
    public static final String IDV_RENDER_TECHNOLOGY_AWT =
        "IDV_VIEWPORT_SET_RENDER_TECHNOLOGY_AWT";

    private ViewportSetCommands()
    {
    }
}

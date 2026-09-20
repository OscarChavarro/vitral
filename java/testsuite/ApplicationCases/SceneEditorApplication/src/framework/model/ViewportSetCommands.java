package framework.model;

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

    private ViewportSetCommands()
    {
    }
}

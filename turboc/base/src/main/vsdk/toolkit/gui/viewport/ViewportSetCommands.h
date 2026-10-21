#ifndef __VIEWPORT_SET_COMMANDS__
#define __VIEWPORT_SET_COMMANDS__

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
OpenGL; the default) and `IDV_VIEWPORT_SET_RENDER_MODE_CPU` (raytracing in the
processor). Applications present it after the projection location items,
separated from them.
*/
class ViewportSetCommands {
public:
    static const char* const POPUP_PROJECTION_LOCATION;

    static const char* const IDV_PROJECTION_LOCATION_PERSPECTIVE;
    static const char* const IDV_PROJECTION_LOCATION_TOP;
    static const char* const IDV_PROJECTION_LOCATION_BOTTOM;
    static const char* const IDV_PROJECTION_LOCATION_LEFT;
    static const char* const IDV_PROJECTION_LOCATION_FRONT;

    static const char* const POPUP_RENDER_MODE;

    static const char* const IDV_RENDER_MODE_GPU;
    static const char* const IDV_RENDER_MODE_CPU;
    static const char* const IDV_RENDER_MODE_CPU_HIDDEN_LINES;

private:
    ViewportSetCommands() {}
};

#endif

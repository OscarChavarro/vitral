#ifndef __XT_OPENGL1_APPLICATION_CONTROLLER__
#define __XT_OPENGL1_APPLICATION_CONTROLLER__

#include "java/io/File.h"
#include "java/lang/String.h"

class Vector3Dd;
class Viewport;

/**
Operations over the drawing area of the editor that need its canvas, as
`AwtJogl4ApplicationController` offers them in the Java application: the
repaint and export of frames, and the delivery of synthetic events to the
interaction techniques, for automated agents (see
`XtOpenGL1VitralEditorMCP`).

It is an interface without Xt types, so it can be used from the classes
that work with the vitral `Widget` (whose name collides with the Xt one).
All the methods must be called from the thread of the Xt event loop (see
`XtEventQueue`).
*/
class XtOpenGL1ApplicationController {
public:
    virtual ~XtOpenGL1ApplicationController() {}

    /**
    @return true if the canvas of the drawing area was already created
    */
    virtual bool isDrawingAreaCreated() = 0;

    /**
    Requests a new frame of the drawing area.
    */
    virtual void repaint() = 0;

    /**
    Notifies the modify panel of the currently selected target.
    */
    virtual void reportTargetToModifyPanel() = 0;

    /**
    Exports the selected viewport to a PNG file, drawing a frame now.
    @param file destination file
    */
    virtual void exportViewportPng(const java::File& file) = 0;

    /**
    Exports the selected viewport to a JPG file, drawing a frame now.
    @param file destination file
    */
    virtual void exportViewportJpg(const java::File& file) = 0;

    /**
    Exports the whole viewport set area to a JPG file, drawing a frame now.
    @param file destination file
    */
    virtual void exportWorkspaceJpg(const java::File& file) = 0;

    /**
    Delivers a synthetic mouse event to the drawing area, as if it came from
    the user's pointer. As with the synthetic events of AWT, no click is
    derived from a press and a release.
    @param type one of "move", "press", "drag", "release"
    @param x canvas x coordinate
    @param y canvas y coordinate
    @param button button number (1 = left, 2 = middle, 3 = right)
    @throws std::invalid_argument for an unknown type (Java throws
    IllegalArgumentException)
    */
    virtual void injectMouseEvent(const java::String& type, int x, int y,
                                  int button) = 0;

    /**
    Delivers a synthetic key press to the drawing area, as if it came from
    the user's keyboard, optionally with the SHIFT and CTRL keys down. As
    AWT does, the character of a Ctrl+letter chord is the control one, and
    SHIFT does not change the character.
    @param key a single character (i.e. "5", "x") or one of the names "tab",
    "enter", "backspace", "delete", "escape", "left", "right", "up", "down",
    "pageup", "pagedown"
    @param shift true to press it with the SHIFT key down
    @param ctrl true to press it with the CTRL key down
    @throws std::invalid_argument for an unknown key (Java throws
    IllegalArgumentException)
    */
    virtual void injectKeyEvent(const java::String& key, bool shift,
                                bool ctrl) = 0;

    /**
    Projects a point of the scene to canvas pixel coordinates.
    @param viewport viewport whose camera is used
    @param point point in world coordinates
    @param outCanvas {x, y} in canvas pixels
    @return false if the point is behind the camera (Java returns null)
    */
    virtual bool projectToCanvas(Viewport* viewport, const Vector3Dd& point,
                                 double outCanvas[2]) = 0;
};

#endif

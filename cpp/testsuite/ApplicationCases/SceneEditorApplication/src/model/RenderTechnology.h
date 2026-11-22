#ifndef __RENDER_TECHNOLOGY__
#define __RENDER_TECHNOLOGY__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"

/**
Technologies the editor can use to present the viewport set of its drawing
area (port of the Java `model.RenderTechnology` enum). Each one offers a set
of render modes to the viewports: OpenGL is used only for the GPU z-buffer,
and every mode computed by the processor is drawn over a 2D canvas without a
3D API (Xlib): projected lines (wireframe or Appel hidden line removal),
projected visible polygons (Weiler/Atherton hidden surface removal) or
Whitted raytraced images. As the whole set is presented by one technology,
choosing a mode of the other one changes the technology of the set (see
`forRenderMode`).

The Java port presents with OpenGL 4 (JOGL) or with AWT 2D drawing; this
one with the OpenGL version it is built with (GLX) or with Xlib 2D drawing,
over a drawing area of the X toolkit that never has a GL context. Both
ports share the commands of the `VIEWPORT_SET_RENDER_TECHNOLOGY` popup: the
`..._AWT` one selects the 2D canvas of the GUI toolkit.
*/
class RenderTechnology {
public:
    enum Value {
        /** OpenGL canvas (GLX context over the drawing area) */
        OPENGL,
        /** Drawing area of the toolkit drawn with Xlib 2D operations only */
        XLIB
    };

    /**
    @return the standard command of the viewport set that selects the
    technology (see `ViewportSetCommands`)
    */
    static const char* getCommand(Value technology);

    /**
    @return the render modes (`Viewport::RENDER_MODE_*`) available in the
    technology, the default one first
    */
    static java::ArrayList<int> getRenderModes(Value technology);

    /**
    @return the name of the technology for the automation service: the
    OpenGL version the editor is built with (`opengl4` or `opengl1`), or
    `xlib`
    */
    static const char* getName(Value technology);

    /**
    @param command a command of the `VIEWPORT_SET_RENDER_TECHNOLOGY` popup
    @param outTechnology receives the technology selected by the command
    @return false if it is not a render technology command
    */
    static bool fromCommand(const java::String& command, Value& outTechnology);

    /**
    @param name a technology name: `opengl`, `opengl4`, `opengl1`, `gpu`,
    `xlib`, or `awt` (the name of the 2D canvas in the Java port), ignoring
    case
    @param outTechnology receives the technology
    @return false if the name is unknown
    */
    static bool fromName(const java::String& name, Value& outTechnology);

    /**
    @param renderMode a `Viewport::RENDER_MODE_*`
    @param outTechnology receives the technology offering the mode, the
    current one if it offers it
    @return false if no technology offers the mode
    */
    static bool forRenderMode(int renderMode, Value current,
                              Value& outTechnology);

private:
    RenderTechnology() {}
};

#endif

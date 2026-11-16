#ifndef __POLYHEDRAL_EXAMPLE_JOGL4_HEADLESS_RENDERER__
#define __POLYHEDRAL_EXAMPLE_JOGL4_HEADLESS_RENDERER__

#include "java/lang/String.h"
#include "vsdk/toolkit/gui/GlfwLabelImageProvider.h"
#include "render/Jogl4DebuggerRenderer.h"

class DebuggerModel;

/**
C++ port of Java's `render.Jogl4HeadlessRenderer`.

C++ port note: the JOGL offscreen drawable (a pbuffer) is a framebuffer
object in the OpenGL 4.1 core context of a hidden GLFW window; its
`GLEventListener` calls (`init`, `reshape`, `display` and, when it is
destroyed, `dispose`) are made in that order by `render`.
@throws java::IllegalStateException when no OpenGL context can be made
*/
class Jogl4HeadlessRenderer {
public:
    Jogl4HeadlessRenderer(DebuggerModel* model, const java::String& outputFile);
    Jogl4HeadlessRenderer(DebuggerModel* model, const java::String& outputFile,
        int width, int height);

    void render();

private:
    DebuggerModel* model;
    java::String outputFile;
    int width;
    int height;
    GlfwLabelImageProvider labels;
    Jogl4DebuggerRenderer delegate;
    bool done;

    void display();
};

#endif

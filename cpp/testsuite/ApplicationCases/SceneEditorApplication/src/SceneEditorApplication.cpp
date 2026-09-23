#include "java/lang/System.h"

/**
Entry point of the C++ port of the scene editor application
(`java/testsuite/ApplicationCases/SceneEditorApplication`).

The port is being done gradually: the model, input/output, neutral rendering
and interaction classes (the ones that depend neither on AWT/Swing nor on
JOGL in the Java version) are already here, but nothing uses them yet. The
GUI will be built over libXt (X11 intrinsics) + GLX + OpenGL 4, with the
support libraries of `cpp/xt` (the counterpart of `java/awt`).
*/
int main(int /*argc*/, char** /*argv*/)
{
    java::System::out.println("TO DO");
    return 0;
}

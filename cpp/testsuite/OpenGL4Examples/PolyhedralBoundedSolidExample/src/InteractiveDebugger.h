#ifndef __POLYHEDRAL_EXAMPLE_INTERACTIVE_DEBUGGER__
#define __POLYHEDRAL_EXAMPLE_INTERACTIVE_DEBUGGER__

#include "java/util/concurrent/atomic/AtomicLong.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/gui/GlfwLabelImageProvider.h"
#include "animation/SolidAnimationController.h"
#include "gui/DebuggerKeyboardInteractionTechniques.h"
#include "gui/DebuggerMouseInteractionTechniques.h"
#include "models/SolidModelNames.h"
#include "render/Jogl4DebuggerRenderer.h"

struct GLFWwindow;
class DebuggerModel;

/**
C++ port of Java's `InteractiveDebugger`.

C++ port notes:
- The Swing `JFrame` with its `GLCanvas` is one GLFW window; the AWT
  listeners are GLFW callbacks. GLFW has no "clicked" nor "entered" mouse
  events of its own: a press released without motion is a click and the
  cursor enter callback gives the entered/exited events.
- `repaintCanvas` asks for a new frame; frames are drawn by the event loop
  on the thread owning the OpenGL context (the animation thread only asks
  for them, as Java's `GLCanvas.display` from that thread is serialized by
  JOGL).
- Full screen switches the same window to the primary monitor (and back to
  the windowed bounds), instead of making a new frame; the macOS native full
  screen path does not apply.
*/
class InteractiveDebugger {
public:
    explicit InteractiveDebugger(DebuggerModel& model);
    ~InteractiveDebugger();

    static void launch(DebuggerModel& model);

private:
    DebuggerModel& model;
    DebuggerKeyboardInteractionTechniques keyboardInteractionTechniques;
    DebuggerMouseInteractionTechniques mouseInteractionTechniques;
    GlfwLabelImageProvider labels;
    Jogl4DebuggerRenderer joglDebuggerRenderer;
    SolidAnimationController solidAnimationController;
    bool shutdownRequested;
    /// Frames asked for (by any thread) and not drawn yet
    java::AtomicLong pendingRepaints;
    bool mouseMovedSincePress;

    class KeyActions;

    bool createMainWindow(bool fullScreenMode);
    void run();
    void rebuildSolid();
    void repaintCanvas();
    void displayFrame();
    void exportCurrentSolidToStl();
    Vector3Dd calculateSolidCenter();
    void recenterOrbiterAfterModelChange(SolidModelNames previousModelName,
        const Vector3Dd& previousPointOfInterest);
    void toggleFullscreenMode();
    void shutdownApplication();

    static InteractiveDebugger* of(GLFWwindow* window);
    static void framebufferSizeCallback(GLFWwindow* window, int width,
        int height);
    static void windowRefreshCallback(GLFWwindow* window);
    static void windowCloseCallback(GLFWwindow* window);
    static void keyCallback(GLFWwindow* window, int key, int scancode,
        int action, int mods);
    static void mouseButtonCallback(GLFWwindow* window, int button,
        int action, int mods);
    static void cursorPosCallback(GLFWwindow* window, double x, double y);
    static void cursorEnterCallback(GLFWwindow* window, int entered);
    static void scrollCallback(GLFWwindow* window, double xoffset,
        double yoffset);
};

#endif

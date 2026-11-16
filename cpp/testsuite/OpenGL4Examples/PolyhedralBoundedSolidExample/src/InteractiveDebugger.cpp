#include <glad/gl.h>
#include "vsdk/toolkit/render/opengl4/OpenGL4Loader.h"
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "InteractiveDebugger.h"

#include "java/io/File.h"
#include "java/io/FileOutputStream.h"
#include "java/lang/Exception.h"
#include "java/lang/System.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/gui/CameraControllerOrbiter.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
#include "vsdk/toolkit/io/geometry/stl/StlWriter.h"
#include "PolyhedralBoundedSolidExample.h"
#include "models/DebuggerModel.h"

static const char* const WINDOW_TITLE =
    "VITRAL concept test - Polyhedral bounded solid example";
static const int DEFAULT_WINDOW_WIDTH = 1024;
static const int DEFAULT_WINDOW_HEIGHT = 768;
static const double STL_EXPORT_SCALE_FACTOR = 1.0 / 100.0;

/// Java: the anonymous `DebuggerKeyboardInteractionTechniques.Actions`
class InteractiveDebugger::KeyActions
    : public DebuggerKeyboardInteractionTechniques::Actions {
public:
    explicit KeyActions(InteractiveDebugger* owner) : owner(owner) {}

    void requestExit() override
    {
        owner->shutdownApplication();
    }

    void rebuildSolid() override
    {
        owner->rebuildSolid();
    }

    void toggleFullscreen() override
    {
        owner->toggleFullscreenMode();
    }

    void toggleSolidAnimation() override
    {
        owner->solidAnimationController.toggleAnimation(owner->model);
    }

    void requestScreenshot() override
    {
        owner->joglDebuggerRenderer.requestScreenshot("screenshot.png");
        owner->repaintCanvas();
    }

    void requestStlExport() override
    {
        owner->exportCurrentSolidToStl();
    }

private:
    InteractiveDebugger* owner;
};

InteractiveDebugger::InteractiveDebugger(DebuggerModel& model)
    : model(model),
      joglDebuggerRenderer(&model, &labels),
      shutdownRequested(false),
      pendingRepaints(1),
      mouseMovedSincePress(false)
{
}

InteractiveDebugger::~InteractiveDebugger()
{
    solidAnimationController.stop();
}

void InteractiveDebugger::launch(DebuggerModel& model)
{
    InteractiveDebugger instance(model);
    if ( !instance.createMainWindow(false) ) {
        java::System::err.println(
            "[PolyhedralBoundedSolidExample] OpenGL 4 is not available");
        return;
    }
    instance.run();
}

void InteractiveDebugger::rebuildSolid()
{
    PolyhedralBoundedSolidExample::buildSolidWithRecovery(model);
}

void InteractiveDebugger::repaintCanvas()
{
    if ( model.getCanvas() != nullptr ) {
        pendingRepaints.incrementAndGet();
        glfwPostEmptyEvent();
    }
}

void InteractiveDebugger::displayFrame()
{
    GLFWwindow* window = model.getCanvas();
    int width = 0;
    int height = 0;
    glfwGetFramebufferSize(window, &width, &height);
    joglDebuggerRenderer.display(width, height);
    glfwSwapBuffers(window);
}

void InteractiveDebugger::exportCurrentSolidToStl()
{
    PolyhedralBoundedSolid* solid = model.getSolid();
    if ( solid == nullptr ) {
        java::System::err.println("[PolyhedralBoundedSolidExample] No selected solid "
            "available for STL export");
        return;
    }

    java::String outputFile = "output.stl";
    Jogl4DebuggerRenderer::ensureParentFolder(outputFile);
    try {
        java::FileOutputStream outputStream(outputFile.c_str());
        StlWriter::exportSolid(solid, &outputStream, STL_EXPORT_SCALE_FACTOR);
        outputStream.close();
        java::System::out.println((java::String(
            "[PolyhedralBoundedSolidExample] Exported ") + outputFile).c_str());
    }
    catch ( const java::Exception& e ) {
        java::System::err.println((java::String(
            "[PolyhedralBoundedSolidExample] STL export failed: ") +
            e.getMessage()).c_str());
    }
}

Vector3Dd InteractiveDebugger::calculateSolidCenter()
{
    if ( model.getSolid() == nullptr ) {
        return Vector3Dd(0, 0, 0);
    }

    double* minMax = model.getSolid()->getMinMax();
    if ( minMax == nullptr ) {
        return Vector3Dd(0, 0, 0);
    }

    Vector3Dd center(
        (minMax[0] + minMax[3]) / 2.0,
        (minMax[1] + minMax[4]) / 2.0,
        (minMax[2] + minMax[5]) / 2.0);
    delete[] minMax;
    return center;
}

void InteractiveDebugger::recenterOrbiterAfterModelChange(
    SolidModelNames previousModelName,
    const Vector3Dd& previousPointOfInterest)
{
    if ( previousModelName == model.getSolidModelName() ) {
        return;
    }
    if ( model.getSolid() == nullptr ) {
        return;
    }
    CameraControllerOrbiter* orbiterController =
        dynamic_cast<CameraControllerOrbiter*>(model.getCameraController());
    if ( orbiterController == nullptr ) {
        return;
    }

    Vector3Dd previousEye = model.getCamera()->getPosition();
    Vector3Dd relativeVector = previousEye.subtract(previousPointOfInterest);
    Vector3Dd newPointOfInterest = calculateSolidCenter();
    Vector3Dd newEye = newPointOfInterest.add(relativeVector);

    orbiterController->setPointOfInterest(newPointOfInterest);
    model.getCamera()->setPosition(newEye);
    model.getCamera()->setFocusedPositionMaintainingOrthogonality(newPointOfInterest);
}

void InteractiveDebugger::toggleFullscreenMode()
{
    GLFWwindow* window = model.getMainFrame();
    if ( window == nullptr ) {
        return;
    }

    if ( !model.isFullScreenMode() ) {
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        glfwGetWindowPos(window, &x, &y);
        glfwGetWindowSize(window, &width, &height);
        model.setWindowedBounds(DebuggerModel::WindowBounds(x, y, width, height));
    }

    model.setFullScreenMode(!model.isFullScreenMode());
    if ( model.isFullScreenMode() ) {
        GLFWmonitor* monitor = glfwGetPrimaryMonitor();
        const GLFWvidmode* mode = monitor != nullptr
            ? glfwGetVideoMode(monitor) : nullptr;
        if ( mode != nullptr ) {
            glfwSetWindowMonitor(window, monitor, 0, 0, mode->width,
                mode->height, mode->refreshRate);
        }
        else {
            glfwMaximizeWindow(window);
        }
    }
    else {
        const DebuggerModel::WindowBounds* bounds = model.getWindowedBounds();
        if ( bounds != nullptr ) {
            glfwSetWindowMonitor(window, nullptr, bounds->x, bounds->y,
                bounds->width, bounds->height, GLFW_DONT_CARE);
        }
        else {
            glfwSetWindowMonitor(window, nullptr, 0, 0, DEFAULT_WINDOW_WIDTH,
                DEFAULT_WINDOW_HEIGHT, GLFW_DONT_CARE);
        }
    }
    joglDebuggerRenderer.refreshCanvasAfterWindowModeChange();
}

bool InteractiveDebugger::createMainWindow(bool fullScreenMode)
{
    if ( !glfwInit() ) {
        return false;
    }
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    glfwWindowHint(GLFW_DEPTH_BITS, 24);

    GLFWmonitor* monitor = nullptr;
    int width = DEFAULT_WINDOW_WIDTH;
    int height = DEFAULT_WINDOW_HEIGHT;
    if ( fullScreenMode ) {
        monitor = glfwGetPrimaryMonitor();
        const GLFWvidmode* mode = monitor != nullptr
            ? glfwGetVideoMode(monitor) : nullptr;
        if ( mode != nullptr ) {
            width = mode->width;
            height = mode->height;
        }
    }
    else if ( model.getWindowedBounds() != nullptr ) {
        width = model.getWindowedBounds()->width;
        height = model.getWindowedBounds()->height;
    }

    GLFWwindow* window = glfwCreateWindow(width, height, WINDOW_TITLE,
        monitor, nullptr);
    if ( window == nullptr ) {
        glfwTerminate();
        return false;
    }
    glfwMakeContextCurrent(window);
    if ( !OpenGL4Loader::load(glfwGetProcAddress) ) {
        glfwDestroyWindow(window);
        glfwTerminate();
        return false;
    }
    glfwSwapInterval(1);

    model.setMainFrame(window);
    model.setCanvas(window);
    glfwSetWindowUserPointer(window, this);
    glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);
    glfwSetWindowRefreshCallback(window, windowRefreshCallback);
    glfwSetWindowCloseCallback(window, windowCloseCallback);
    glfwSetKeyCallback(window, keyCallback);
    glfwSetMouseButtonCallback(window, mouseButtonCallback);
    glfwSetCursorPosCallback(window, cursorPosCallback);
    glfwSetCursorEnterCallback(window, cursorEnterCallback);
    glfwSetScrollCallback(window, scrollCallback);

    // Java: `GLEventListener.init` and the first `reshape`
    int framebufferWidth = 0;
    int framebufferHeight = 0;
    glfwGetFramebufferSize(window, &framebufferWidth, &framebufferHeight);
    joglDebuggerRenderer.init(framebufferWidth, framebufferHeight);
    joglDebuggerRenderer.reshape(0, 0, framebufferWidth, framebufferHeight);

    solidAnimationController.start(&model, [this]() { repaintCanvas(); });

    // Java: `canvas.requestFocusInWindow()`
    glfwShowWindow(window);
    glfwFocusWindow(window);
    return true;
}

void InteractiveDebugger::run()
{
    GLFWwindow* window = model.getCanvas();
    while ( !shutdownRequested && !glfwWindowShouldClose(window) ) {
        if ( pendingRepaints.getAndSet(0) > 0 ) {
            displayFrame();
        }
        glfwWaitEvents();
    }
    shutdownApplication();
}

void InteractiveDebugger::shutdownApplication()
{
    if ( shutdownRequested && model.getCanvas() == nullptr ) {
        return;
    }
    shutdownRequested = true;

    solidAnimationController.stop();
    GLFWwindow* window = model.getCanvas();
    if ( window != nullptr ) {
        // Java: `GLCanvas.destroy`, which calls `GLEventListener.dispose`
        joglDebuggerRenderer.dispose();
        model.setCanvas(nullptr);
        model.setMainFrame(nullptr);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
    java::System::exit(0);
}

//= GLFW callbacks (Java: the AWT listener methods) ==================

InteractiveDebugger* InteractiveDebugger::of(GLFWwindow* window)
{
    return static_cast<InteractiveDebugger*>(glfwGetWindowUserPointer(window));
}

void InteractiveDebugger::framebufferSizeCallback(GLFWwindow* window,
    int width, int height)
{
    InteractiveDebugger* self = of(window);
    if ( self == nullptr ) {
        return;
    }
    self->joglDebuggerRenderer.reshape(0, 0, width, height);
    self->repaintCanvas();
}

void InteractiveDebugger::windowRefreshCallback(GLFWwindow* window)
{
    InteractiveDebugger* self = of(window);
    if ( self != nullptr ) {
        self->repaintCanvas();
    }
}

void InteractiveDebugger::windowCloseCallback(GLFWwindow* window)
{
    InteractiveDebugger* self = of(window);
    if ( self != nullptr ) {
        // Java: `WindowAdapter.windowClosing`
        self->shutdownApplication();
    }
}

void InteractiveDebugger::keyCallback(GLFWwindow* window, int key,
    int /*scancode*/, int action, int mods)
{
    InteractiveDebugger* self = of(window);
    if ( self == nullptr ) {
        return;
    }
    KeyEvent event = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
    DebuggerModel& model = self->model;

    if ( action == GLFW_RELEASE ) {
        if ( model.getCameraController()->processKeyReleasedEvent(event) ) {
            self->repaintCanvas();
        }
        return;
    }

    SolidModelNames previousModelName = model.getSolidModelName();
    Vector3Dd previousPointOfInterest(0, 0, 0);
    CameraControllerOrbiter* orbiterController =
        dynamic_cast<CameraControllerOrbiter*>(model.getCameraController());
    if ( orbiterController != nullptr ) {
        previousPointOfInterest = orbiterController->getPointOfInterest();
    }

    KeyActions actions(self);
    if ( self->keyboardInteractionTechniques.processPressed(model, event,
             actions) ) {
        self->recenterOrbiterAfterModelChange(previousModelName,
            previousPointOfInterest);
        self->repaintCanvas();
    }
}

void InteractiveDebugger::mouseButtonCallback(GLFWwindow* window, int button,
    int action, int /*mods*/)
{
    InteractiveDebugger* self = of(window);
    if ( self == nullptr ) {
        return;
    }
    double x;
    double y;
    glfwGetCursorPos(window, &x, &y);
    MouseEvent event = GlfwSystem::glfw2vsdkMouseEvent(button, action, x, y);
    if ( action == GLFW_PRESS ) {
        self->mouseMovedSincePress = false;
        if ( self->mouseInteractionTechniques.processMousePressed(self->model,
                 event) ) {
            self->repaintCanvas();
        }
        return;
    }
    if ( self->mouseInteractionTechniques.processMouseReleased(self->model,
             event) ) {
        self->repaintCanvas();
    }
    // AWT reports a click after a release without motion
    if ( !self->mouseMovedSincePress &&
         self->mouseInteractionTechniques.processMouseClicked(self->model,
             event) ) {
        self->repaintCanvas();
    }
}

void InteractiveDebugger::cursorPosCallback(GLFWwindow* window, double x,
    double y)
{
    InteractiveDebugger* self = of(window);
    if ( self == nullptr ) {
        return;
    }
    MouseEvent event = GlfwSystem::glfw2vsdkMotionEvent(x, y);
    int leftButton = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT);
    int middleButton = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_MIDDLE);
    int rightButton = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT);
    int modifiers = 0;
    if ( leftButton == GLFW_PRESS ) modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
    if ( middleButton == GLFW_PRESS ) modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
    if ( rightButton == GLFW_PRESS ) modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
    event.setModifiers(modifiers);
    bool dragged = modifiers != 0;
    self->mouseMovedSincePress = true;
    bool repaint = dragged
        ? self->mouseInteractionTechniques.processMouseDragged(self->model, event)
        : self->mouseInteractionTechniques.processMouseMoved(self->model, event);
    if ( repaint ) {
        self->repaintCanvas();
    }
}

void InteractiveDebugger::cursorEnterCallback(GLFWwindow* window, int entered)
{
    InteractiveDebugger* self = of(window);
    if ( self == nullptr ) {
        return;
    }
    if ( entered ) {
        self->mouseInteractionTechniques.processMouseEntered(self->model);
    }
    else {
        self->mouseInteractionTechniques.processMouseExited(self->model);
    }
}

void InteractiveDebugger::scrollCallback(GLFWwindow* window, double xoffset,
    double yoffset)
{
    InteractiveDebugger* self = of(window);
    if ( self == nullptr ) {
        return;
    }
    MouseEvent event = GlfwSystem::glfw2vsdkWheelEvent(xoffset, yoffset);
    if ( self->mouseInteractionTechniques.processMouseWheelMoved(self->model,
             event) ) {
        self->repaintCanvas();
    }
}

#include <cstdio>
#include <cstdlib>

#include <glad/gl.h>
#include "vsdk/toolkit/render/opengl4/OpenGL4Loader.h"
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/fixtures/OpenGL4SimpleCorridorSample.h"
#include "vsdk/toolkit/gui/CameraController.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4CameraRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MatrixRenderer.h"

/**
C++ counterpart of Java's `Jogl4Examples/CameraExample`. As in Java, the
scene is only redrawn when the camera controller reports a change (or the
window is resized / exposed).
*/
static GLFWwindow* window = nullptr;
static Camera* camera = nullptr;
static CameraController* cameraController = nullptr;
static OpenGL4SimpleCorridorSample* corridor = nullptr;
static bool needsRepaint = true;

static void createModel()
{
    camera = new Camera();
    cameraController = new CameraControllerAquynza(camera);
    corridor = new OpenGL4SimpleCorridorSample();
}

static void drawObjectsGL()
{
    glEnable(GL_DEPTH_TEST);

    Matrix4x4d projection = OpenGL4CameraRenderer::activate(camera);
    float* mvp = projection.exportToFloatArrayColumnOrder();
    corridor->drawGL(mvp, projection);
    OpenGL4MatrixRenderer::draw(mvp, Matrix4x4d::identityMatrix());
    delete[] mvp;
}

static void display()
{
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    drawObjectsGL();
    glfwSwapBuffers(window);
}

static void dispose()
{
    if ( corridor != nullptr ) {
        corridor->dispose();
    }
    OpenGL4CameraRenderer::dispose();
}

static void reshape(GLFWwindow* /*win*/, int xSize, int ySize)
{
    glViewport(0, 0, xSize, ySize);
    camera->updateViewportResize(xSize, ySize);
    needsRepaint = true;
}

static void refresh(GLFWwindow* /*win*/)
{
    needsRepaint = true;
}

static void requestClose()
{
    glfwSetWindowShouldClose(window, GLFW_TRUE);
}

static void keyCallback(GLFWwindow* /*win*/, int key, int /*scancode*/,
    int action, int mods)
{
    KeyEvent e = GlfwSystem::glfw2vsdkKeyEvent(key, mods);

    if ( action == GLFW_RELEASE ) {
        if ( cameraController->processKeyReleasedEvent(e) ) {
            needsRepaint = true;
        }
        return;
    }

    if ( key == GLFW_KEY_ESCAPE ) {
        requestClose();
        return;
    }

    if ( cameraController->processKeyPressedEvent(e) ) {
        needsRepaint = true;
    }
}

static void mouseButtonCallback(GLFWwindow* win, int button, int action,
    int /*mods*/)
{
    double x;
    double y;
    glfwGetCursorPos(win, &x, &y);
    MouseEvent e = GlfwSystem::glfw2vsdkMouseEvent(button, action, x, y);

    if ( action == GLFW_PRESS ) {
        if ( cameraController->processMousePressedEvent(e) ) {
            needsRepaint = true;
        }
    }
    else if ( action == GLFW_RELEASE ) {
        if ( cameraController->processMouseReleasedEvent(e) ) {
            needsRepaint = true;
        }
    }
}

static void cursorPosCallback(GLFWwindow* win, double x, double y)
{
    MouseEvent e = GlfwSystem::glfw2vsdkMotionEvent(x, y);
    int modifiers = 0;

    if ( glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS ) {
        modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
    }
    if ( glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_MIDDLE) == GLFW_PRESS ) {
        modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
    }
    if ( glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS ) {
        modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
    }
    e.setModifiers(modifiers);

    bool changed;
    if ( modifiers != 0 ) {
        changed = cameraController->processMouseDraggedEvent(e);
    }
    else {
        changed = cameraController->processMouseMovedEvent(e);
    }
    if ( changed ) {
        needsRepaint = true;
    }
}

static void scrollCallback(GLFWwindow* /*win*/, double xOffset, double yOffset)
{
    MouseEvent e = GlfwSystem::glfw2vsdkWheelEvent(xOffset, yOffset);
    if ( cameraController->processMouseWheelEvent(e) ) {
        needsRepaint = true;
    }
}

int main(int /*argc*/, char** /*argv*/)
{
    if ( !glfwInit() ) {
        printf("Can not start OpenGL/GLFW.\n");
        return 0;
    }

    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

    window = glfwCreateWindow(640, 480,
        "VITRAL concept test - OpenGL4 Camera control example",
        nullptr, nullptr);
    if ( window == nullptr ) {
        printf("Can not start OpenGL/GLFW.\n");
        glfwTerminate();
        return 0;
    }
    glfwSetWindowSizeLimits(window, 640, 480, GLFW_DONT_CARE, GLFW_DONT_CARE);
    glfwMakeContextCurrent(window);
    glfwSwapInterval(1);

    if ( !OpenGL4Loader::load(glfwGetProcAddress) ) {
        printf("Can not start OpenGL/GLFW.\n");
        glfwDestroyWindow(window);
        glfwTerminate();
        return 0;
    }

    createModel();

    glfwSetFramebufferSizeCallback(window, reshape);
    glfwSetWindowRefreshCallback(window, refresh);
    glfwSetKeyCallback(window, keyCallback);
    glfwSetMouseButtonCallback(window, mouseButtonCallback);
    glfwSetCursorPosCallback(window, cursorPosCallback);
    glfwSetScrollCallback(window, scrollCallback);

    int xSize;
    int ySize;
    glfwGetFramebufferSize(window, &xSize, &ySize);
    reshape(window, xSize, ySize);

    while ( !glfwWindowShouldClose(window) ) {
        if ( needsRepaint ) {
            needsRepaint = false;
            display();
        }
        glfwWaitEvents();
    }

    dispose();
    delete corridor;
    delete cameraController;
    delete camera;
    glfwDestroyWindow(window);
    glfwTerminate();

    return 0;
}

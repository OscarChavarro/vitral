#include <cstdio>
#include <cstdlib>

#ifdef __APPLE__
#define GLFW_INCLUDE_GLCOREARB
#include <OpenGL/gl3.h>
#else
#define GLFW_INCLUDE_NONE
#include <GL/glew.h>
#endif
#include <GLFW/glfw3.h>

#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MatrixRenderer.h"
#include "vsdk/toolkit/fixtures/OpenGL4SimpleCorridorSample.h"

GLFWwindow* window = nullptr;
Camera* camera = nullptr;
CameraControllerAquynza* controller = nullptr;
OpenGL4SimpleCorridorSample* corridor = nullptr;
int lastFramebufferWidth = 640;
int lastFramebufferHeight = 480;

void framebufferSizeCallback(GLFWwindow* win, int width, int height) {
    glViewport(0, 0, width, height);
    if (camera) {
        camera->updateViewportResize(width, height);
    }
}

void keyCallback(GLFWwindow* win, int key, int scancode, int action, int mods) {
    if (action == GLFW_PRESS || action == GLFW_REPEAT) {
        KeyEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkKeyEvent(key, mods);
        if (controller) {
            controller->processKeyPressedEvent(event);
        }
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            glfwSetWindowShouldClose(win, GLFW_TRUE);
        }
    } else if (action == GLFW_RELEASE) {
        KeyEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkKeyEvent(key, mods);
        if (controller) {
            controller->processKeyReleasedEvent(event);
        }
    }
}

void mouseButtonCallback(GLFWwindow* win, int button, int action, int mods) {
    double xpos, ypos;
    glfwGetCursorPos(win, &xpos, &ypos);
    MouseEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkMouseEvent(button, action, xpos, ypos);

    if (controller) {
        if (action == GLFW_PRESS) {
            controller->processMousePressedEvent(event);
        } else if (action == GLFW_RELEASE) {
            controller->processMouseReleasedEvent(event);
        }
    }
}

void cursorPosCallback(GLFWwindow* win, double xpos, double ypos) {
    if (controller) {
        MouseEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkMotionEvent(xpos, ypos);
        int leftButton = glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_LEFT);
        int middleButton = glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_MIDDLE);
        int rightButton = glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_RIGHT);

        int modifiers = 0;
        if (leftButton == GLFW_PRESS) {
            modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
        }
        if (middleButton == GLFW_PRESS) {
            modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
        }
        if (rightButton == GLFW_PRESS) {
            modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
        }
        event.setModifiers(modifiers);

        if (leftButton == GLFW_PRESS || middleButton == GLFW_PRESS || rightButton == GLFW_PRESS) {
            controller->processMouseDraggedEvent(event);
        } else {
            controller->processMouseMovedEvent(event);
        }
    }
}

void scrollCallback(GLFWwindow* win, double xoffset, double yoffset) {
    if (controller) {
        MouseEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkWheelEvent(xoffset, yoffset);
        controller->processMouseWheelEvent(event);
    }
}

int main(int argc, char** argv) {
    if (!glfwInit()) {
        fprintf(stderr, "Failed to initialize GLFW\n");
        return -1;
    }

    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

    window = glfwCreateWindow(640, 480, "Camera Example", nullptr, nullptr);
    if (!window) {
        fprintf(stderr, "Failed to create GLFW window\n");
        glfwTerminate();
        return -1;
    }

    glfwMakeContextCurrent(window);
    glfwSwapInterval(1);

#ifndef __APPLE__
    glewExperimental = GL_TRUE;
    GLenum glewErr = glewInit();
    if (glewErr != GLEW_OK) {
        fprintf(stderr, "Failed to initialize GLEW: %s\n", glewGetErrorString(glewErr));
        glfwDestroyWindow(window);
        glfwTerminate();
        return -1;
    }
#endif

    camera = new Camera();
    camera->updateViewportResize(640, 480);

    controller = new CameraControllerAquynza(camera);
    corridor = new OpenGL4SimpleCorridorSample();

    glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);
    glfwSetKeyCallback(window, keyCallback);
    glfwSetMouseButtonCallback(window, mouseButtonCallback);
    glfwSetCursorPosCallback(window, cursorPosCallback);
    glfwSetScrollCallback(window, scrollCallback);

    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glEnable(GL_DEPTH_TEST);

    while (!glfwWindowShouldClose(window)) {
        glfwPollEvents();

        int currentWidth, currentHeight;
        glfwGetFramebufferSize(window, &currentWidth, &currentHeight);
        if (currentWidth != lastFramebufferWidth || currentHeight != lastFramebufferHeight) {
            lastFramebufferWidth = currentWidth;
            lastFramebufferHeight = currentHeight;
            glViewport(0, 0, currentWidth, currentHeight);
            if (camera) {
                camera->updateViewportResize(currentWidth, currentHeight);
            }
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        float* mvp = camera->calculateProjectionMatrix().exportToFloatArrayColumnOrder();
        Matrix4x4d identity = Matrix4x4d::identityMatrix();

        corridor->drawGL(mvp, identity);
        OpenGL4MatrixRenderer::draw(mvp, identity);

        delete[] mvp;

        glfwSwapBuffers(window);
    }

    OpenGL4MatrixRenderer::release();

    if (corridor) {
        corridor->dispose();
        delete corridor;
        corridor = nullptr;
    }
    if (controller) {
        delete controller;
        controller = nullptr;
    }
    if (camera) {
        delete camera;
        camera = nullptr;
    }

    if (window) {
        glfwDestroyWindow(window);
    }
    glfwTerminate();

    return 0;
}

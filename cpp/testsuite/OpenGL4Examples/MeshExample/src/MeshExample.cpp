#include <cstdio>

#include <glad/gl.h>
#include "vsdk/toolkit/render/opengl4/OpenGL4Loader.h"
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "java/io/File.h"
#include "vsdk/toolkit/gui/CameraControllerOrbiter.h"
#include "vsdk/toolkit/gui/GlfwLabelImageProvider.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "animation/AnimationController.h"
#include "gui/MeshKeyboardInteractionTechniques.h"
#include "gui/MeshMouseInteractionTechniques.h"
#include "gui/TangibleInterfaceInteractionTechniques.h"
#include "model/MeshModel.h"
#include "options/CommandLineOptions.h"
#include "render/Jogl4DebuggerRenderer.h"

static const int WINDOW_WIDTH = 1024;
static const int WINDOW_HEIGHT = 768;

class MeshExampleApp {
public:
    MeshExampleApp()
        : window(0),
          commandLineOptions(&model),
          cameraController(0),
          qualityController(0),
          mouseInteractionTechniques(0),
          keyboardInteractionTechniques(0),
          tangibleInterfaceClient(0),
          tangibleInteractionTechniques(0),
          renderer(&model, &labelImageProvider),
          shouldClose(false)
    {
    }

    ~MeshExampleApp()
    {
        cleanup();
    }

    bool init(int argc, char** argv)
    {
        if ( !glfwInit() ) {
            std::fprintf(stderr, "Failed to initialize GLFW\n");
            return false;
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        // A normal window covering the whole usable area of the desktop (not
        // full screen: menu bars, docks and task bars stay visible)
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "VITRAL mesh test - OpenGL4", 0, 0);
        if ( window == 0 ) {
            std::fprintf(stderr, "Failed to create GLFW window\n");
            glfwTerminate();
            return false;
        }

        requestKeyboardFocus();
        glfwMakeContextCurrent(window);
        glfwSetWindowUserPointer(window, this);
        glfwSwapInterval(1);

        glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);
        glfwSetKeyCallback(window, keyCallback);
        glfwSetMouseButtonCallback(window, mouseButtonCallback);
        glfwSetCursorPosCallback(window, cursorPosCallback);
        glfwSetScrollCallback(window, scrollCallback);

        if ( !OpenGL4Loader::load(glfwGetProcAddress) ) {
            return false;
        }

        const char* fileName = extractFileName(argc, argv);
        if ( !loadScene(fileName) ) {
            return false;
        }

        commandLineOptions.processArguments(argc, argv);
        cameraController = new CameraControllerOrbiter(model.getCamera());
        qualityController = new RendererConfigurationController(model.getQualitySelection());
        mouseInteractionTechniques = new MeshMouseInteractionTechniques(cameraController);
        keyboardInteractionTechniques =
            new MeshKeyboardInteractionTechniques(&model, cameraController, qualityController, &shouldClose);

        if ( !renderer.init() ) {
            std::fprintf(stderr, "Failed to initialize mesh renderer\n");
            return false;
        }
        configureOverlayScreenResolution();
        // The window is maximized when created, so its framebuffer size is
        // only known now
        int framebufferWidth = 0;
        int framebufferHeight = 0;
        glfwGetFramebufferSize(window, &framebufferWidth, &framebufferHeight);
        renderer.reshape(framebufferWidth, framebufferHeight);

        animationController.start(&model, [this]() { requestRepaint(); });

        std::printf("Searching tangible interface server on %s\n", model.getTangibleServiceUrl().c_str());
        tangibleInterfaceClient = new TangibleInterfaceNetworkClient(model.getTangibleServiceUrl());
        tangibleInteractionTechniques =
            new TangibleInterfaceInteractionTechniques(&model, [this]() { requestRepaint(); });
        tangibleInterfaceClient->addListener(tangibleInteractionTechniques);
        tangibleInterfaceClient->run();

        return true;
    }

    void run()
    {
        while ( !glfwWindowShouldClose(window) && !shouldClose ) {
            glfwPollEvents();
            draw();
            glfwSwapBuffers(window);
        }
    }

private:
    GLFWwindow* window;
    MeshModel model;
    CommandLineOptions commandLineOptions;
    CameraControllerOrbiter* cameraController;
    RendererConfigurationController* qualityController;
    MeshMouseInteractionTechniques* mouseInteractionTechniques;
    MeshKeyboardInteractionTechniques* keyboardInteractionTechniques;
    TangibleInterfaceNetworkClient* tangibleInterfaceClient;
    TangibleInterfaceInteractionTechniques* tangibleInteractionTechniques;
    GlfwLabelImageProvider labelImageProvider;
    Jogl4DebuggerRenderer renderer;
    AnimationController animationController;
    bool shouldClose;

    bool loadScene(const char* fileName)
    {
        if ( fileName == 0 ) {
            std::fprintf(stderr, "File not specified\n");
            return false;
        }

        java::File file(fileName);
        if ( !file.exists() || !file.canRead() ) {
            std::fprintf(stderr, "Failed to read file: %s\n", fileName);
            return false;
        }

        try {
            EnvironmentPersistence::importEnvironment(file, model.getScene());
            model.configureInitialViewAndLightToScene();
        }
        catch ( ... ) {
            std::fprintf(stderr, "Failed to import scene\n");
            return false;
        }

        return true;
    }

    /**
    Asks the window manager to make this application the foreground one and
    gives it the keyboard focus, so key presses reach it without the user
    having to select the window first (i.e. when launched from a terminal,
    which otherwise keeps the focus).
    */
    void requestKeyboardFocus()
    {
        glfwShowWindow(window);
        glfwFocusWindow(window);
    }

    /**
    Overlay texts (input gizmo boxes) follow the screen resolution, in
    pixels (not in screen coordinates).
    */
    void configureOverlayScreenResolution()
    {
        GLFWmonitor* monitor = glfwGetPrimaryMonitor();
        const GLFWvidmode* mode = monitor != 0 ? glfwGetVideoMode(monitor) : 0;
        if ( mode == 0 ) {
            return;
        }
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        glfwGetMonitorContentScale(monitor, &scaleX, &scaleY);
        renderer.getViewportSet()->getElementScaler()->setScreenResolution(
            (int)(mode->width * scaleX + 0.5f),
            (int)(mode->height * scaleY + 0.5f));
    }

    void draw()
    {
        renderer.display();
    }

    void requestRepaint()
    {
        if ( window != 0 ) {
            glfwPostEmptyEvent();
        }
    }

    void cleanup()
    {
        animationController.stop();

        if ( tangibleInterfaceClient != 0 ) {
            tangibleInterfaceClient->disconnect();
            delete tangibleInterfaceClient;
            tangibleInterfaceClient = 0;
        }
        if ( tangibleInteractionTechniques != 0 ) {
            delete tangibleInteractionTechniques;
            tangibleInteractionTechniques = 0;
        }

        renderer.dispose();

        delete mouseInteractionTechniques;
        mouseInteractionTechniques = 0;
        delete keyboardInteractionTechniques;
        keyboardInteractionTechniques = 0;
        delete qualityController;
        qualityController = 0;
        delete cameraController;
        cameraController = 0;

        if ( window != 0 ) {
            glfwDestroyWindow(window);
            window = 0;
        }
        glfwTerminate();
    }

    static const char* extractFileName(int argc, char** argv)
    {
        if ( argv == 0 ) {
            return 0;
        }

        for ( int i = 1; i < argc; i++ ) {
            java::String arg(argv[i]);
            if ( arg == "-tangibleServer" ) {
                i++;
                continue;
            }
            if ( arg.length() > 0 && arg[0] != '-' ) {
                return argv[i];
            }
        }

        return 0;
    }

    static void framebufferSizeCallback(GLFWwindow* win, int width, int height)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if ( app == 0 ) return;
        app->renderer.reshape(width, height);
    }

    static void keyCallback(GLFWwindow* win, int key, int, int action, int mods)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if ( app == 0 || app->keyboardInteractionTechniques == 0 ) return;

        if ( action == GLFW_PRESS || action == GLFW_REPEAT ) {
            KeyEvent event = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
            if ( app->keyboardInteractionTechniques->processKeyPressedEvent(event) ) {
                app->requestRepaint();
            }
        }
        else if ( action == GLFW_RELEASE ) {
            KeyEvent event = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
            if ( app->keyboardInteractionTechniques->processKeyReleasedEvent(event) ) {
                app->requestRepaint();
            }
        }
    }

    static void mouseButtonCallback(GLFWwindow* win, int button, int action, int)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if ( app == 0 || app->mouseInteractionTechniques == 0 ) return;

        double xpos = 0.0;
        double ypos = 0.0;
        glfwGetCursorPos(win, &xpos, &ypos);

        MouseEvent event = GlfwSystem::glfw2vsdkMouseEvent(button, action, xpos, ypos);
        bool repaint = false;
        if ( action == GLFW_PRESS ) {
            repaint = app->mouseInteractionTechniques->processMousePressedEvent(event);
        }
        else if ( action == GLFW_RELEASE ) {
            repaint = app->mouseInteractionTechniques->processMouseReleasedEvent(event);
        }
        if ( repaint ) {
            app->requestRepaint();
        }
    }

    static void cursorPosCallback(GLFWwindow* win, double xpos, double ypos)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if ( app == 0 || app->mouseInteractionTechniques == 0 ) return;

        MouseEvent event = GlfwSystem::glfw2vsdkMotionEvent(xpos, ypos);

        int leftButton = glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_LEFT);
        int middleButton = glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_MIDDLE);
        int rightButton = glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_RIGHT);

        int modifiers = 0;
        if ( leftButton == GLFW_PRESS ) modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
        if ( middleButton == GLFW_PRESS ) modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
        if ( rightButton == GLFW_PRESS ) modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
        event.setModifiers(modifiers);

        bool repaint;
        if ( leftButton == GLFW_PRESS || middleButton == GLFW_PRESS || rightButton == GLFW_PRESS ) {
            repaint = app->mouseInteractionTechniques->processMouseDraggedEvent(event);
        }
        else {
            repaint = app->mouseInteractionTechniques->processMouseMovedEvent(event);
        }
        if ( repaint ) {
            app->requestRepaint();
        }
    }

    static void scrollCallback(GLFWwindow* win, double xoffset, double yoffset)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if ( app == 0 || app->mouseInteractionTechniques == 0 ) return;

        MouseEvent event = GlfwSystem::glfw2vsdkWheelEvent(xoffset, yoffset);
        if ( app->mouseInteractionTechniques->processMouseWheelEvent(event) ) {
            app->requestRepaint();
        }
    }
};

int main(int argc, char** argv)
{
    MeshExampleApp app;
    if ( !app.init(argc, argv) ) {
        return 1;
    }

    app.run();
    return 0;
}

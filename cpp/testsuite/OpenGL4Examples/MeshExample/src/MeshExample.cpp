#include <cstdio>

#include <GL/glew.h>
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "java/io/File.h"
#include "vsdk/toolkit/gui/CameraControllerOrbiter.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.h"
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
          renderer(&model),
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

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "VITRAL mesh test - OpenGL4", 0, 0);
        if ( window == 0 ) {
            std::fprintf(stderr, "Failed to create GLFW window\n");
            glfwTerminate();
            return false;
        }

        glfwMakeContextCurrent(window);
        glfwSetWindowUserPointer(window, this);
        glfwSwapInterval(1);

        glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);
        glfwSetKeyCallback(window, keyCallback);
        glfwSetMouseButtonCallback(window, mouseButtonCallback);
        glfwSetCursorPosCallback(window, cursorPosCallback);
        glfwSetScrollCallback(window, scrollCallback);

        glewExperimental = GL_TRUE;
        GLenum err = glewInit();
        if ( err != GLEW_OK ) {
            std::fprintf(stderr, "Failed to initialize GLEW: %s\n", glewGetErrorString(err));
            return false;
        }

        const char* fileName = extractFileName(argc, argv);
        if ( !loadScene(fileName) ) {
            return false;
        }

        commandLineOptions.processArguments(argc, argv);
        model.getCamera()->updateViewportResize(WINDOW_WIDTH, WINDOW_HEIGHT);
        cameraController = new CameraControllerOrbiter(model.getCamera());
        qualityController = new RendererConfigurationController(model.getQualitySelection());
        mouseInteractionTechniques = new MeshMouseInteractionTechniques(cameraController);
        keyboardInteractionTechniques =
            new MeshKeyboardInteractionTechniques(&model, cameraController, qualityController, &shouldClose);

        if ( !renderer.init() ) {
            std::fprintf(stderr, "Failed to initialize mesh renderer\n");
            return false;
        }

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

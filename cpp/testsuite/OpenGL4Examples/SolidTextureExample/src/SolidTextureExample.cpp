#include <cstdio>

#include <glad/gl.h>
#include "vsdk/toolkit/render/opengl4/OpenGL4Loader.h"
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "java/io/File.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/gui/CameraControllerOrbiter.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.h"
#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "animation/AnimationController.h"
#include "gui/SolidTextureKeyboardInteractionTechniques.h"
#include "gui/SolidTextureMouseInteractionTechniques.h"
#include "gui/TangibleInterfaceInteractionTechniques.h"
#include "model/SolidTextureModel.h"
#include "options/CommandLineOptions.h"
#include "render/Jogl4DebuggerRenderer.h"

static const int WINDOW_WIDTH = 640;
static const int WINDOW_HEIGHT = 480;

static const char* operationModeName(OperationMode mode)
{
    return mode == OperationMode::MESH_MODEL ? "MESH_MODEL" : "TEXTURE_2D_STACK";
}

class SolidTextureExampleApp {
public:
    SolidTextureExampleApp()
        : window(0), commandLineOptions(&model), cameraController(0),
          qualityController(0), mouseInteractionTechniques(0),
          keyboardInteractionTechniques(0), tangibleInterfaceClient(0),
          tangibleInteractionTechniques(0), renderer(&model), shouldClose(false)
    {
    }

    ~SolidTextureExampleApp()
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

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT,
                                  "VITRAL solid texture test - OpenGL4", 0, 0);
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

        if ( !OpenGL4Loader::load(glfwGetProcAddress) ) return false;
        const char* fileName = extractFileName(argc, argv);
        if ( !loadScene(fileName) ) return false;

        commandLineOptions.processArguments(argc, argv);
        model.getCamera()->updateViewportResize(WINDOW_WIDTH, WINDOW_HEIGHT);
        cameraController = new CameraControllerOrbiter(model.getCamera());
        qualityController = new RendererConfigurationController(model.getQualitySelection());
        mouseInteractionTechniques = new SolidTextureMouseInteractionTechniques(cameraController);
        keyboardInteractionTechniques =
            new SolidTextureKeyboardInteractionTechniques(
                &model, cameraController, qualityController, &shouldClose);

        if ( !renderer.init() ) {
            std::fprintf(stderr, "Failed to initialize solid texture renderer\n");
            return false;
        }
        animationController.start(&model, [this]() { requestRepaint(); });
        std::printf("Searching tangible interface server on %s\n",
                    model.getTangibleServiceUrl().c_str());
        tangibleInterfaceClient =
            new TangibleInterfaceNetworkClient(model.getTangibleServiceUrl());
        tangibleInteractionTechniques =
            new TangibleInterfaceInteractionTechniques(&model, [this]() { requestRepaint(); });
        tangibleInterfaceClient->addListener(tangibleInteractionTechniques);
        tangibleInterfaceClient->run();
        updateWindowTitle();
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
    SolidTextureModel model;
    CommandLineOptions commandLineOptions;
    CameraControllerOrbiter* cameraController;
    RendererConfigurationController* qualityController;
    SolidTextureMouseInteractionTechniques* mouseInteractionTechniques;
    SolidTextureKeyboardInteractionTechniques* keyboardInteractionTechniques;
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
        updateWindowTitle();
        if ( window != 0 ) glfwPostEmptyEvent();
    }

    void updateWindowTitle()
    {
        if ( window == 0 || !model.isHudVisible() ) return;
        char title[512];
        std::snprintf(title, sizeof(title),
            "VITRAL solid texture - mode[1]=%s size[2,3]=%d texture[4,5]=%s animation[a]=%s ray[r]",
            operationModeName(model.getOperationMode()),
            model.getSolidTextureSize(),
            solidTextureExampleColorName(model.getSelectedSolidTexture()),
            model.isAnimationEnabled() ? "PLAY" : "STOP");
        glfwSetWindowTitle(window, title);
    }

    void cleanup()
    {
        animationController.stop();
        if ( tangibleInterfaceClient != 0 ) {
            tangibleInterfaceClient->disconnect();
            delete tangibleInterfaceClient;
            tangibleInterfaceClient = 0;
        }
        delete tangibleInteractionTechniques;
        tangibleInteractionTechniques = 0;
        renderer.dispose();
        delete mouseInteractionTechniques;
        delete keyboardInteractionTechniques;
        delete qualityController;
        delete cameraController;
        mouseInteractionTechniques = 0;
        keyboardInteractionTechniques = 0;
        qualityController = 0;
        cameraController = 0;
        if ( window != 0 ) {
            glfwDestroyWindow(window);
            window = 0;
        }
        glfwTerminate();
    }

    static const char* extractFileName(int argc, char** argv)
    {
        if ( argv == 0 ) return 0;
        for ( int i = 1; i < argc; i++ ) {
            java::String arg(argv[i]);
            if ( arg == "-tangibleServer" ) {
                i++;
                continue;
            }
            if ( arg.length() > 0 && arg[0] != '-' ) return argv[i];
        }
        return 0;
    }

    static void framebufferSizeCallback(GLFWwindow* win, int width, int height)
    {
        SolidTextureExampleApp* app =
            static_cast<SolidTextureExampleApp*>(glfwGetWindowUserPointer(win));
        if ( app != 0 ) app->renderer.reshape(width, height);
    }

    static void keyCallback(GLFWwindow* win, int key, int, int action, int mods)
    {
        SolidTextureExampleApp* app =
            static_cast<SolidTextureExampleApp*>(glfwGetWindowUserPointer(win));
        if ( app == 0 || app->keyboardInteractionTechniques == 0 ) return;
        KeyEvent event = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
        if ( action == GLFW_PRESS || action == GLFW_REPEAT ) {
            if ( app->keyboardInteractionTechniques->processKeyPressedEvent(event) ) {
                app->requestRepaint();
            }
        }
        else if ( action == GLFW_RELEASE ) {
            if ( app->keyboardInteractionTechniques->processKeyReleasedEvent(event) ) {
                app->requestRepaint();
            }
        }
    }

    static void mouseButtonCallback(GLFWwindow* win, int button, int action, int)
    {
        SolidTextureExampleApp* app =
            static_cast<SolidTextureExampleApp*>(glfwGetWindowUserPointer(win));
        if ( app == 0 || app->mouseInteractionTechniques == 0 ) return;
        double xpos = 0.0, ypos = 0.0;
        glfwGetCursorPos(win, &xpos, &ypos);
        MouseEvent event = GlfwSystem::glfw2vsdkMouseEvent(button, action, xpos, ypos);
        bool repaint = action == GLFW_PRESS ?
            app->mouseInteractionTechniques->processMousePressedEvent(event) :
            app->mouseInteractionTechniques->processMouseReleasedEvent(event);
        if ( repaint ) app->requestRepaint();
    }

    static void cursorPosCallback(GLFWwindow* win, double xpos, double ypos)
    {
        SolidTextureExampleApp* app =
            static_cast<SolidTextureExampleApp*>(glfwGetWindowUserPointer(win));
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
        bool repaint = (leftButton == GLFW_PRESS || middleButton == GLFW_PRESS ||
                        rightButton == GLFW_PRESS) ?
            app->mouseInteractionTechniques->processMouseDraggedEvent(event) :
            app->mouseInteractionTechniques->processMouseMovedEvent(event);
        if ( repaint ) app->requestRepaint();
    }

    static void scrollCallback(GLFWwindow* win, double xoffset, double yoffset)
    {
        SolidTextureExampleApp* app =
            static_cast<SolidTextureExampleApp*>(glfwGetWindowUserPointer(win));
        if ( app == 0 || app->mouseInteractionTechniques == 0 ) return;
        MouseEvent event = GlfwSystem::glfw2vsdkWheelEvent(xoffset, yoffset);
        if ( app->mouseInteractionTechniques->processMouseWheelEvent(event) ) {
            app->requestRepaint();
        }
    }
};

int main(int argc, char** argv)
{
    SolidTextureExampleApp app;
    if ( !app.init(argc, argv) ) return 1;
    app.run();
    return 0;
}

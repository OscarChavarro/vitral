#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Loader.h"
#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif

#include "java/io/File.h"
#include "java/lang/Runnable.h"
#include "vsdk/toolkit/gui/CameraControllerOrbiter.h"
#include "vsdk/toolkit/gui/GlutLabelImageProvider.h"
#include "vsdk/toolkit/gui/GlutSystem.h"
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
#include "render/OpenGL1DebuggerRenderer.h"

static const int WINDOW_WIDTH = 1024;
static const int WINDOW_HEIGHT = 768;
/// Room (in pixels) left for the window manager decorations around the
/// window, both sides of each axis
static const int WINDOW_DECORATION_MARGIN = 32;

/// Period (in milliseconds) used to look for repaints requested by the
/// animation and tangible interface threads
static const int REPAINT_POLL_MILLISECONDS = 50;

/**
GLUT is not thread safe: the animation and tangible interface threads only
raise this flag, and the GLUT thread asks for the repaint.
*/
class RepaintRequest : public Runnable {
public:
    RepaintRequest() : requested(false) {}
    virtual void run() { requested = true; }
    bool consume()
    {
        bool r = requested;
        requested = false;
        return r;
    }
private:
    volatile bool requested;
};

class MeshExampleApp {
public:
    MeshExampleApp()
        : commandLineOptions(&model),
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

    bool init(int* argc, char** argv)
    {
        glutInit(argc, argv);
        glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA | GLUT_DEPTH);
        // A normal window covering the desktop (GLUT can not ask the window
        // manager for a maximized window): leaves room for the decorations
        // of the window manager, so the overlays at the bottom stay visible
        int screenWidth = glutGet(GLUT_SCREEN_WIDTH);
        int screenHeight = glutGet(GLUT_SCREEN_HEIGHT);
        glutInitWindowSize(
            screenWidth > WINDOW_DECORATION_MARGIN ?
                screenWidth - WINDOW_DECORATION_MARGIN : WINDOW_WIDTH,
            screenHeight > 2 * WINDOW_DECORATION_MARGIN ?
                screenHeight - 2 * WINDOW_DECORATION_MARGIN : WINDOW_HEIGHT);
        glutInitWindowPosition(0, 0);
        glutCreateWindow("VITRAL mesh test - OpenGL1");

        if ( !OpenGL1Loader::load() ) {
            return false;
        }

        const char* fileName = extractFileName(*argc, argv);
        if ( !loadScene(fileName) ) {
            return false;
        }

        commandLineOptions.processArguments(*argc, argv);
        cameraController = new CameraControllerOrbiter(model.getCamera());
        qualityController = new RendererConfigurationController(model.getQualitySelection());
        mouseInteractionTechniques = new MeshMouseInteractionTechniques(cameraController);
        keyboardInteractionTechniques =
            new MeshKeyboardInteractionTechniques(&model, cameraController, qualityController, &shouldClose);

        if ( !renderer.init() ) {
            fprintf(stderr, "Failed to initialize mesh renderer\n");
            return false;
        }
        configureOverlayScreenResolution();

        animationController.start(&model, &repaintRequest);

        printf("Searching tangible interface server on %s\n", model.getTangibleServiceUrl().c_str());
        tangibleInterfaceClient = new TangibleInterfaceNetworkClient(model.getTangibleServiceUrl());
        tangibleInteractionTechniques =
            new TangibleInterfaceInteractionTechniques(&model, &repaintRequest);
        tangibleInterfaceClient->addListener(tangibleInteractionTechniques);
        tangibleInterfaceClient->run();

        return true;
    }

    void display()
    {
        renderer.display();
        glutSwapBuffers();
    }

    void reshape(int width, int height)
    {
        renderer.reshape(width, height);
        glutPostRedisplay();
    }

    void pollRepaintRequests()
    {
        if ( repaintRequest.consume() ) {
            glutPostRedisplay();
        }
    }

    void keyPressed(const KeyEvent& event)
    {
        if ( keyboardInteractionTechniques == 0 ) return;
        if ( keyboardInteractionTechniques->processKeyPressedEvent(event) ) {
            glutPostRedisplay();
        }
        closeIfRequested();
    }

    void keyReleased(const KeyEvent& event)
    {
        if ( keyboardInteractionTechniques == 0 ) return;
        if ( keyboardInteractionTechniques->processKeyReleasedEvent(event) ) {
            glutPostRedisplay();
        }
    }

    void mouse(int button, int state, int x, int y)
    {
        if ( mouseInteractionTechniques == 0 ) return;

        int direction;
        if ( GlutSystem::isWheelButton(button, &direction) ) {
            if ( state == GLUT_DOWN ) {
                MouseEvent event = GlutSystem::glut2vsdkWheelEvent(0, direction, x, y);
                if ( mouseInteractionTechniques->processMouseWheelEvent(event) ) {
                    glutPostRedisplay();
                }
            }
            return;
        }

        GlutSystem::updateMouseButtonsState(button, state);
        MouseEvent event = GlutSystem::glut2vsdkMouseEvent(button, state, x, y);
        bool repaint;
        if ( state == GLUT_DOWN ) {
            repaint = mouseInteractionTechniques->processMousePressedEvent(event);
        }
        else {
            repaint = mouseInteractionTechniques->processMouseReleasedEvent(event);
        }
        if ( repaint ) {
            glutPostRedisplay();
        }
    }

    void motion(int x, int y, bool dragged)
    {
        if ( mouseInteractionTechniques == 0 ) return;

        MouseEvent event = GlutSystem::glut2vsdkMotionEvent(x, y);
        bool repaint;
        if ( dragged ) {
            event.setModifiers(GlutSystem::getMouseButtonsDownMask());
            repaint = mouseInteractionTechniques->processMouseDraggedEvent(event);
        }
        else {
            repaint = mouseInteractionTechniques->processMouseMovedEvent(event);
        }
        if ( repaint ) {
            glutPostRedisplay();
        }
    }

private:
    MeshModel model;
    CommandLineOptions commandLineOptions;
    CameraControllerOrbiter* cameraController;
    RendererConfigurationController* qualityController;
    MeshMouseInteractionTechniques* mouseInteractionTechniques;
    MeshKeyboardInteractionTechniques* keyboardInteractionTechniques;
    TangibleInterfaceNetworkClient* tangibleInterfaceClient;
    TangibleInterfaceInteractionTechniques* tangibleInteractionTechniques;
    GlutLabelImageProvider labelImageProvider;
    OpenGL1DebuggerRenderer renderer;
    AnimationController animationController;
    RepaintRequest repaintRequest;
    bool shouldClose;

    bool loadScene(const char* fileName)
    {
        if ( fileName == 0 ) {
            fprintf(stderr, "File not specified\n");
            return false;
        }

        File file(fileName);
        if ( !file.exists() || !file.canRead() ) {
            fprintf(stderr, "Failed to read file: %s\n", fileName);
            return false;
        }

        try {
            EnvironmentPersistence::importEnvironment(file, model.getScene());
            model.configureInitialViewAndLightToScene();
        }
        catch ( ... ) {
            fprintf(stderr, "Failed to import scene\n");
            return false;
        }

        return true;
    }

    /**
    Overlay texts (input gizmo boxes) follow the screen resolution, in
    pixels.
    */
    void configureOverlayScreenResolution()
    {
        int width = glutGet(GLUT_SCREEN_WIDTH);
        int height = glutGet(GLUT_SCREEN_HEIGHT);
        if ( width <= 0 || height <= 0 ) {
            return;
        }
        renderer.getViewportSet()->getElementScaler()->setScreenResolution(
            width, height);
    }

    void closeIfRequested()
    {
        if ( shouldClose ) {
            // Classic GLUT never returns from its main loop
            cleanup();
            exit(0);
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
        GlutSystem::releaseResources();

        delete mouseInteractionTechniques;
        mouseInteractionTechniques = 0;
        delete keyboardInteractionTechniques;
        keyboardInteractionTechniques = 0;
        delete qualityController;
        qualityController = 0;
        delete cameraController;
        cameraController = 0;
    }

    static const char* extractFileName(int argc, char** argv)
    {
        if ( argv == 0 ) {
            return 0;
        }

        for ( int i = 1; i < argc; i++ ) {
            String arg(argv[i]);
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
};

static MeshExampleApp* app = NULL;

static void displayCallback()
{
    app->display();
}

static void reshapeCallback(int width, int height)
{
    app->reshape(width, height);
}

static void timerCallback(int /*value*/)
{
    app->pollRepaintRequests();
    glutTimerFunc(REPAINT_POLL_MILLISECONDS, timerCallback, 0);
}

static void keyboardCallback(unsigned char key, int, int)
{
    app->keyPressed(GlutSystem::glut2vsdkKeyEvent(key, glutGetModifiers()));
}

static void keyboardUpCallback(unsigned char key, int, int)
{
    app->keyReleased(GlutSystem::glut2vsdkKeyEvent(key, glutGetModifiers()));
}

static void specialCallback(int key, int, int)
{
    app->keyPressed(GlutSystem::glut2vsdkSpecialKeyEvent(key, glutGetModifiers()));
}

static void specialUpCallback(int key, int, int)
{
    app->keyReleased(GlutSystem::glut2vsdkSpecialKeyEvent(key, glutGetModifiers()));
}

static void mouseCallback(int button, int state, int x, int y)
{
    app->mouse(button, state, x, y);
}

static void motionCallback(int x, int y)
{
    app->motion(x, y, true);
}

static void passiveMotionCallback(int x, int y)
{
    app->motion(x, y, false);
}

int main(int argc, char** argv)
{
    app = new MeshExampleApp();
    if ( !app->init(&argc, argv) ) {
        return 1;
    }

    glutDisplayFunc(displayCallback);
    glutReshapeFunc(reshapeCallback);
    glutKeyboardFunc(keyboardCallback);
    glutKeyboardUpFunc(keyboardUpCallback);
    glutSpecialFunc(specialCallback);
    glutSpecialUpFunc(specialUpCallback);
    glutMouseFunc(mouseCallback);
    glutMotionFunc(motionCallback);
    glutPassiveMotionFunc(passiveMotionCallback);
    glutTimerFunc(REPAINT_POLL_MILLISECONDS, timerCallback, 0);

    glutMainLoop();
    return 0;
}

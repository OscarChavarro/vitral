#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/File.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/fixtures/OpenGL1SimpleCorridorSample.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Loader.h"
#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/GlutSystem.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1CameraRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MatrixRenderer.h"
static const float IMAGE_DEPTH_BIAS_FACTOR = -1.0f;
static const float IMAGE_DEPTH_BIAS_UNITS = -8.0f;
static const int WINDOW_WIDTH = 1024;
static const int WINDOW_HEIGHT = 768;

class ImageExampleApp;

static void displayCallback();
static void reshapeCallback(int width, int height);
static void keyboardCallback(unsigned char key, int x, int y);
static void keyboardUpCallback(unsigned char key, int x, int y);
static void specialCallback(int key, int x, int y);
static void specialUpCallback(int key, int x, int y);
static void mouseCallback(int button, int state, int x, int y);
static void motionCallback(int x, int y);
static void passiveMotionCallback(int x, int y);

class ImageExampleApp {
private:
    Camera* camera;
    CameraControllerAquynza* cameraController;
    Image* renderImage;
    Image* earthImage;
    OpenGL1SimpleCorridorSample* corridor;
    int lastFramebufferWidth;
    int lastFramebufferHeight;

public:
    ImageExampleApp() : camera(NULL),
                       cameraController(NULL), renderImage(NULL),
                       earthImage(NULL), corridor(NULL),
                       lastFramebufferWidth(0), lastFramebufferHeight(0) {
    }

    ~ImageExampleApp() {
        cleanup();
    }

    BOOLEAN init(int* argc, char** argv) {
        glutInit(argc, argv);
        glutInitDisplayMode(GLUT_DOUBLE | GLUT_RGBA | GLUT_DEPTH);
        // At most the screen, leaving room for the window manager
        // decorations (i.e. on 800x600 displays)
        int width = WINDOW_WIDTH;
        int height = WINDOW_HEIGHT;
        int screenWidth = glutGet(GLUT_SCREEN_WIDTH);
        int screenHeight = glutGet(GLUT_SCREEN_HEIGHT);
        if (screenWidth > 0 && width > screenWidth - 32) width = screenWidth - 32;
        if (screenHeight > 0 && height > screenHeight - 64) height = screenHeight - 64;
        glutInitWindowSize(width, height);
        glutCreateWindow("VITRAL concept test - OpenGL1 Image use example");

        if (!OpenGL1Loader::load()) {
            return FALSE;
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

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        camera = new Camera();
        camera->setNearPlaneDistance(0.1);
        camera->setFarPlaneDistance(1000.0);
        camera->updateViewportResize(width, height);

        cameraController = new CameraControllerAquynza(camera);
        corridor = new OpenGL1SimpleCorridorSample();

        renderImage = loadImage("etc/images/render.jpg");
        if (renderImage == NULL || renderImage->getXSize() <= 0) {
            fprintf(stderr, "Warning: Could not load render.jpg\n");
            if (renderImage != NULL) {
                delete renderImage;
                renderImage = NULL;
            }
        }

        earthImage = loadImage("etc/textures/earth.dds");
        if (earthImage == NULL || earthImage->getXSize() <= 0) {
            fprintf(stderr, "Warning: Could not load earth.dds\n");
            if (earthImage != NULL) {
                delete earthImage;
                earthImage = NULL;
            }
        }

        return TRUE;
    }

    void onFramebufferSizeChanged(int width, int height) {
        lastFramebufferWidth = width;
        lastFramebufferHeight = height;
        glViewport(0, 0, width, height);
        if (camera != NULL) {
            camera->updateViewportResize(width, height);
        }
    }

    void onKeyPressed(const KeyEvent& event) {
        if (cameraController != NULL) {
            cameraController->processKeyPressedEvent(event);
        }
    }

    void onKeyReleased(const KeyEvent& event) {
        if (cameraController != NULL) {
            cameraController->processKeyReleasedEvent(event);
        }
    }

    void onMousePressed(const MouseEvent& event) {
        if (cameraController != NULL) {
            cameraController->processMousePressedEvent(event);
        }
    }

    void onMouseReleased(const MouseEvent& event) {
        if (cameraController != NULL) {
            cameraController->processMouseReleasedEvent(event);
        }
    }

    void onMouseMoved(const MouseEvent& event) {
        if (cameraController != NULL) {
            cameraController->processMouseMovedEvent(event);
        }
    }

    void onMouseDragged(const MouseEvent& event) {
        if (cameraController != NULL) {
            cameraController->processMouseDraggedEvent(event);
        }
    }

    void onMouseWheel(const MouseEvent& event) {
        if (cameraController != NULL) {
            cameraController->processMouseWheelEvent(event);
        }
    }

    Image* loadImage(const char* filename) {
        ArrayList<String> candidates;
        candidates.add(String(filename));
        candidates.add(String("../") + filename);
        candidates.add(String("../../") + filename);
        candidates.add(String("../../../") + filename);
        candidates.add(String("../../../../") + filename);

        for (long int i = 0; i < candidates.size(); i++) {
            const String& candidate = candidates[i];
            File file(candidate.c_str());
            if (!file.exists() || !file.canRead()) {
                continue;
            }

            Image* result = NULL;
            const String& path = candidate;
            if (path.size() >= 4 && path.substr(path.size() - 4) == ".dds") {
                result = ImagePersistence::importImage(file);
            }
            else {
                result = ImagePersistence::importRGB(file);
            }

            if (result != NULL && result->getXSize() > 0 && result->getYSize() > 0) {
                return result;
            }
            if (result != NULL) {
                delete result;
            }
        }

        fprintf(stderr, "Error: Could not read image file \"%s\"\n", filename);
        return NULL;
    }

    void drawTexturedPolygon(Image* image, const float* mvp, float x0, float y0, float width, float height) {
        if (image == NULL) {
            return;
        }

        int textureId = OpenGL1ImageRenderer::activate(image);
        if (textureId <= 0) {
            return;
        }

        float x1 = x0 + width;
        float y1 = y0 + height;

        float positions[] = {
            x0, y0, 0.01f,
            x1, y0, 0.01f,
            x1, y1, 0.01f,
            x0, y0, 0.01f,
            x1, y1, 0.01f,
            x0, y1, 0.01f
        };

        float uvCoordinates[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };

        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        OpenGL1ImageRenderer::drawTexturedQuad(
            textureId,
            mvp,
            positions, 6,
            uvCoordinates, 6,
            1.0f, 1.0f, 1.0f);
    }

    void drawWorldImages(const float* mvp) {
        if (renderImage == NULL || earthImage == NULL) {
            return;
        }

        float renderWidth = (float)renderImage->getXSize() / (float)renderImage->getYSize();

        drawTexturedPolygon(renderImage, mvp, 0.0f, 0.0f, renderWidth, 1.0f);
        drawTexturedPolygon(earthImage, mvp, 0.0f, -1.0f, 1.0f, 1.0f);
    }

    void drawWorldImagesDepthBiased(const float* mvp) {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(IMAGE_DEPTH_BIAS_FACTOR, IMAGE_DEPTH_BIAS_UNITS);

        drawWorldImages(mvp);

        glDisable(GL_POLYGON_OFFSET_FILL);
        glDepthFunc(GL_LESS);
    }

    void drawHudImage(Image* image, BOOLEAN upperLeft) {
        if (image == NULL) {
            return;
        }

        int textureId = OpenGL1ImageRenderer::activate(image);
        if (textureId <= 0) {
            return;
        }

        int viewport[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int viewportWidth = (viewport[2] > 0) ? viewport[2] : 1;
        int viewportHeight = (viewport[3] > 0) ? viewport[3] : 1;

        float width = 2.0f * ((float)image->getXSize() / (float)viewportWidth);
        float height = 2.0f * ((float)image->getYSize() / (float)viewportHeight);

        float x0 = -1.0f;
        float y0 = upperLeft ? 1.0f - height : -1.0f;
        float x1 = x0 + width;
        float y1 = y0 + height;

        float positions[] = {
            x0, y0, 0.0f,
            x1, y0, 0.0f,
            x1, y1, 0.0f,
            x0, y0, 0.0f,
            x1, y1, 0.0f,
            x0, y1, 0.0f
        };

        float uvCoordinates[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };

        glDisable(GL_CULL_FACE);
        RGBAImageUncompressed* rgbaUncomp = (image != NULL && image->instanceOf(RGBAImageUncompressed::CLASS) ? (RGBAImageUncompressed*)image : NULL);
        if (rgbaUncomp != NULL) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        OpenGL1ImageRenderer::drawTexturedQuad(
            textureId,
            positions, 6,
            uvCoordinates, 6,
            1.0f, 1.0f, 1.0f);

        if (rgbaUncomp != NULL) {
            glDisable(GL_BLEND);
        }
    }

    void drawHud() {
        glDisable(GL_DEPTH_TEST);
        if (renderImage != NULL) {
            drawHudImage(renderImage, FALSE);
        }
        if (earthImage != NULL) {
            drawHudImage(earthImage, TRUE);
        }
        glEnable(GL_DEPTH_TEST);
    }

    void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix4x4d projection = OpenGL1CameraRenderer::activate(camera);
        float* mvp = projection.exportToFloatArrayColumnOrder();
        Matrix4x4d identity = Matrix4x4d::identityMatrix();

        corridor->drawGL(mvp, identity);
        drawWorldImagesDepthBiased(mvp);
        drawHud();

        OpenGL1MatrixRenderer::draw(mvp, identity);

        delete[] mvp;
    }

    void display() {
        render();
        glutSwapBuffers();
    }

    void cleanup() {
        if (corridor != NULL) {
            delete corridor;
            corridor = NULL;
        }
        if (renderImage != NULL) {
            OpenGL1ImageRenderer::unload(renderImage);
            delete renderImage;
            renderImage = NULL;
        }
        if (earthImage != NULL) {
            OpenGL1ImageRenderer::unload(earthImage);
            delete earthImage;
            earthImage = NULL;
        }

        OpenGL1CameraRenderer::dispose();
        OpenGL1ImageRenderer::dispose();
        OpenGL1MatrixRenderer::release();

        if (cameraController != NULL) {
            delete cameraController;
            cameraController = NULL;
        }
        if (camera != NULL) {
            delete camera;
            camera = NULL;
        }

    }
};

static ImageExampleApp* app = NULL;

static void displayCallback() {
    app->display();
}

static void reshapeCallback(int width, int height) {
    app->onFramebufferSizeChanged(width, height);
    glutPostRedisplay();
}

static void keyPressed(const KeyEvent& event) {
    if (event.keycode == KeyEvent::KEY_ESC) {
        // Classic GLUT never returns from its main loop
        delete app;
        app = NULL;
        exit(0);
    }
    app->onKeyPressed(event);
    glutPostRedisplay();
}

static void keyReleased(const KeyEvent& event) {
    app->onKeyReleased(event);
    glutPostRedisplay();
}

static void keyboardCallback(unsigned char key, int, int) {
    keyPressed(GlutSystem::glut2vsdkKeyEvent(key, glutGetModifiers()));
}

static void keyboardUpCallback(unsigned char key, int, int) {
    keyReleased(GlutSystem::glut2vsdkKeyEvent(key, glutGetModifiers()));
}

static void specialCallback(int key, int, int) {
    keyPressed(GlutSystem::glut2vsdkSpecialKeyEvent(key, glutGetModifiers()));
}

static void specialUpCallback(int key, int, int) {
    keyReleased(GlutSystem::glut2vsdkSpecialKeyEvent(key, glutGetModifiers()));
}

static void mouseCallback(int button, int state, int x, int y) {
    int direction;
    if (GlutSystem::isWheelButton(button, &direction)) {
        if (state == GLUT_DOWN) {
            app->onMouseWheel(GlutSystem::glut2vsdkWheelEvent(0, direction, x, y));
            glutPostRedisplay();
        }
        return;
    }

    GlutSystem::updateMouseButtonsState(button, state);
    MouseEvent event = GlutSystem::glut2vsdkMouseEvent(button, state, x, y);
    if (state == GLUT_DOWN) {
        app->onMousePressed(event);
    } else {
        app->onMouseReleased(event);
    }
    glutPostRedisplay();
}

static void motionCallback(int x, int y) {
    MouseEvent event = GlutSystem::glut2vsdkMotionEvent(x, y);
    event.setModifiers(GlutSystem::getMouseButtonsDownMask());
    app->onMouseDragged(event);
    glutPostRedisplay();
}

static void passiveMotionCallback(int x, int y) {
    app->onMouseMoved(GlutSystem::glut2vsdkMotionEvent(x, y));
    glutPostRedisplay();
}

int main(int argc, char** argv) {
    app = new ImageExampleApp();

    if (!app->init(&argc, argv)) {
        fprintf(stderr, "Failed to initialize application\n");
        return 1;
    }

    glutMainLoop();
    return 0;
}

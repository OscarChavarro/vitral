#include <GL/glew.h>
#include <GLFW/glfw3.h>
#include <cstdio>
#include <cmath>

#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "java/io/File.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MatrixRenderer.h"
#include "vsdk/toolkit/fixtures/OpenGL4SimpleCorridorSample.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"

static const float IMAGE_DEPTH_BIAS_FACTOR = -1.0f;
static const float IMAGE_DEPTH_BIAS_UNITS = -8.0f;
static const int WINDOW_WIDTH = 1024;
static const int WINDOW_HEIGHT = 768;

class ImageExampleApp;

static void framebufferSizeCallback(GLFWwindow* win, int width, int height);
static void keyCallback(GLFWwindow* win, int key, int scancode, int action, int mods);
static void mouseButtonCallback(GLFWwindow* win, int button, int action, int mods);
static void cursorPosCallback(GLFWwindow* win, double xpos, double ypos);
static void scrollCallback(GLFWwindow* win, double xoffset, double yoffset);

class ImageExampleApp {
private:
    GLFWwindow* window;
    Camera* camera;
    CameraControllerAquynza* cameraController;
    Image* renderImage;
    Image* earthImage;
    OpenGL4SimpleCorridorSample* corridor;
    bool shouldClose;
    int lastFramebufferWidth;
    int lastFramebufferHeight;

public:
    ImageExampleApp() : window(nullptr), camera(nullptr),
                       cameraController(nullptr), renderImage(nullptr),
                       earthImage(nullptr), corridor(nullptr), shouldClose(false),
                       lastFramebufferWidth(WINDOW_WIDTH), lastFramebufferHeight(WINDOW_HEIGHT) {
    }

    ~ImageExampleApp() {
        cleanup();
    }

    bool init() {
        if (!glfwInit()) {
            fprintf(stderr, "Failed to initialize GLFW\n");
            return false;
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT,
                                 "VITRAL concept test - OpenGL4 Image use example",
                                 nullptr, nullptr);
        if (window == nullptr) {
            fprintf(stderr, "Failed to create GLFW window\n");
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
        if (err != GLEW_OK) {
            fprintf(stderr, "Failed to initialize GLEW: %s\n", glewGetErrorString(err));
            return false;
        }

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        camera = new Camera();
        camera->setNearPlaneDistance(0.1);
        camera->setFarPlaneDistance(1000.0);
        camera->updateViewportResize(WINDOW_WIDTH, WINDOW_HEIGHT);

        cameraController = new CameraControllerAquynza(camera);
        corridor = new OpenGL4SimpleCorridorSample();

        renderImage = loadImage("etc/images/render.jpg");
        if (renderImage == nullptr || renderImage->getXSize() <= 0) {
            fprintf(stderr, "Warning: Could not load render.jpg\n");
            if (renderImage != nullptr) {
                delete renderImage;
                renderImage = nullptr;
            }
        }

        earthImage = loadImage("etc/textures/earth.dds");
        if (earthImage == nullptr || earthImage->getXSize() <= 0) {
            fprintf(stderr, "Warning: Could not load earth.dds\n");
            if (earthImage != nullptr) {
                delete earthImage;
                earthImage = nullptr;
            }
        }

        return true;
    }

    void onFramebufferSizeChanged(int width, int height) {
        if (camera != nullptr) {
            camera->updateViewportResize(width, height);
        }
    }

    void onKeyPressed(const KeyEvent& event) {
        if (cameraController != nullptr) {
            cameraController->processKeyPressedEvent(event);
        }
    }

    void onKeyReleased(const KeyEvent& event) {
        if (cameraController != nullptr) {
            cameraController->processKeyReleasedEvent(event);
        }
    }

    void onMousePressed(const MouseEvent& event) {
        if (cameraController != nullptr) {
            cameraController->processMousePressedEvent(event);
        }
    }

    void onMouseReleased(const MouseEvent& event) {
        if (cameraController != nullptr) {
            cameraController->processMouseReleasedEvent(event);
        }
    }

    void onMouseMoved(const MouseEvent& event) {
        if (cameraController != nullptr) {
            cameraController->processMouseMovedEvent(event);
        }
    }

    void onMouseDragged(const MouseEvent& event) {
        if (cameraController != nullptr) {
            cameraController->processMouseDraggedEvent(event);
        }
    }

    void onMouseWheel(const MouseEvent& event) {
        if (cameraController != nullptr) {
            cameraController->processMouseWheelEvent(event);
        }
    }

    Image* loadImage(const char* filename) {
        java::ArrayList<java::String> candidates;
        candidates.add(java::String(filename));
        candidates.add(java::String("../") + filename);
        candidates.add(java::String("../../") + filename);
        candidates.add(java::String("../../../") + filename);
        candidates.add(java::String("../../../../") + filename);

        for (long int i = 0; i < candidates.size(); i++) {
            const java::String& candidate = candidates[i];
            java::File file(candidate.c_str());
            if (!file.exists() || !file.canRead()) {
                continue;
            }

            Image* result = nullptr;
            const java::String& path = candidate;
            if (path.size() >= 4 && path.substr(path.size() - 4) == ".dds") {
                result = ImagePersistence::importImage(file);
            }
            else {
                result = ImagePersistence::importRGB(file);
            }

            if (result != nullptr && result->getXSize() > 0 && result->getYSize() > 0) {
                return result;
            }
            if (result != nullptr) {
                delete result;
            }
        }

        fprintf(stderr, "Error: Could not read image file \"%s\"\n", filename);
        return nullptr;
    }

    void drawTexturedPolygon(Image* image, const float* mvp, float x0, float y0, float width, float height) {
        if (image == nullptr) {
            return;
        }

        int textureId = OpenGL4ImageRenderer::activate(image);
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

        OpenGL4ImageRenderer::drawTexturedQuad(
            textureId,
            mvp,
            positions, 6,
            uvCoordinates, 6,
            1.0f, 1.0f, 1.0f);
    }

    void drawWorldImages(const float* mvp) {
        if (renderImage == nullptr || earthImage == nullptr) {
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

    void drawHudImage(Image* image, bool upperLeft) {
        if (image == nullptr) {
            return;
        }

        int textureId = OpenGL4ImageRenderer::activate(image);
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
        RGBAImageUncompressed* rgbaUncomp = dynamic_cast<RGBAImageUncompressed*>(image);
        if (rgbaUncomp != nullptr) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        OpenGL4ImageRenderer::drawTexturedQuad(
            textureId,
            positions, 6,
            uvCoordinates, 6,
            1.0f, 1.0f, 1.0f);

        if (rgbaUncomp != nullptr) {
            glDisable(GL_BLEND);
        }
    }

    void drawHud() {
        glDisable(GL_DEPTH_TEST);
        if (renderImage != nullptr) {
            drawHudImage(renderImage, false);
        }
        if (earthImage != nullptr) {
            drawHudImage(earthImage, true);
        }
        glEnable(GL_DEPTH_TEST);
    }

    void render() {
        int currentWidth, currentHeight;
        glfwGetFramebufferSize(window, &currentWidth, &currentHeight);
        if (currentWidth != lastFramebufferWidth || currentHeight != lastFramebufferHeight) {
            lastFramebufferWidth = currentWidth;
            lastFramebufferHeight = currentHeight;
            glViewport(0, 0, currentWidth, currentHeight);
            if (camera != nullptr) {
                camera->updateViewportResize(currentWidth, currentHeight);
            }
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        float* mvp = camera->calculateProjectionMatrix().exportToFloatArrayColumnOrder();
        Matrix4x4d identity = Matrix4x4d::identityMatrix();

        corridor->drawGL(mvp, identity);
        drawWorldImagesDepthBiased(mvp);
        drawHud();

        OpenGL4MatrixRenderer::draw(mvp, identity);

        delete[] mvp;
    }

    void run() {
        while (!glfwWindowShouldClose(window) && !shouldClose) {
            glfwPollEvents();
            render();
            glfwSwapBuffers(window);
        }
    }

    void cleanup() {
        if (corridor != nullptr) {
            delete corridor;
            corridor = nullptr;
        }
        if (renderImage != nullptr) {
            OpenGL4ImageRenderer::unload(renderImage);
            delete renderImage;
            renderImage = nullptr;
        }
        if (earthImage != nullptr) {
            OpenGL4ImageRenderer::unload(earthImage);
            delete earthImage;
            earthImage = nullptr;
        }

        OpenGL4ImageRenderer::dispose();
        OpenGL4MatrixRenderer::release();

        if (cameraController != nullptr) {
            delete cameraController;
            cameraController = nullptr;
        }
        if (camera != nullptr) {
            delete camera;
            camera = nullptr;
        }

        if (window != nullptr) {
            glfwDestroyWindow(window);
            window = nullptr;
        }
        glfwTerminate();
    }
};

static void framebufferSizeCallback(GLFWwindow* win, int width, int height) {
    glViewport(0, 0, width, height);
    ImageExampleApp* app = (ImageExampleApp*)glfwGetWindowUserPointer(win);
    if (app) {
        app->onFramebufferSizeChanged(width, height);
    }
}

static void keyCallback(GLFWwindow* win, int key, int scancode, int action, int mods) {
    ImageExampleApp* app = (ImageExampleApp*)glfwGetWindowUserPointer(win);
    if (!app) return;

    if (action == GLFW_PRESS || action == GLFW_REPEAT) {
        KeyEvent event = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
        app->onKeyPressed(event);
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            glfwSetWindowShouldClose(win, GLFW_TRUE);
        }
    } else if (action == GLFW_RELEASE) {
        KeyEvent event = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
        app->onKeyReleased(event);
    }
}

static void mouseButtonCallback(GLFWwindow* win, int button, int action, int mods) {
    ImageExampleApp* app = (ImageExampleApp*)glfwGetWindowUserPointer(win);
    if (!app) return;

    double xpos, ypos;
    glfwGetCursorPos(win, &xpos, &ypos);
    MouseEvent event = GlfwSystem::glfw2vsdkMouseEvent(button, action, xpos, ypos);

    if (action == GLFW_PRESS) {
        app->onMousePressed(event);
    } else if (action == GLFW_RELEASE) {
        app->onMouseReleased(event);
    }
}

static void cursorPosCallback(GLFWwindow* win, double xpos, double ypos) {
    ImageExampleApp* app = (ImageExampleApp*)glfwGetWindowUserPointer(win);
    if (!app) return;

    MouseEvent event = GlfwSystem::glfw2vsdkMotionEvent(xpos, ypos);
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
        app->onMouseDragged(event);
    } else {
        app->onMouseMoved(event);
    }
}

static void scrollCallback(GLFWwindow* win, double xoffset, double yoffset) {
    ImageExampleApp* app = (ImageExampleApp*)glfwGetWindowUserPointer(win);
    if (!app) return;

    MouseEvent event = GlfwSystem::glfw2vsdkWheelEvent(xoffset, yoffset);
    app->onMouseWheel(event);
}

int main(int argc, char** argv) {
    ImageExampleApp app;

    if (!app.init()) {
        fprintf(stderr, "Failed to initialize application\n");
        return 1;
    }

    app.run();
    return 0;
}

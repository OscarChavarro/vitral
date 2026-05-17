#include <cstdio>
#include <cstdlib>
#include <cmath>
#include <string>
#include <vector>
#include <algorithm>
#include <stdexcept>

#include <GL/glew.h>
#include <GL/gl.h>
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/LightType.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/gui/CameraControllerAquynza.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/java/io/File.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/IndexedColorImageUncompressed.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/render/SimpleRaytracer.h"
#include "vsdk/toolkit/render/shaders/CpuTextureSamplingConfig.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MatrixRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SphereRenderer.h"
#include "options/CommandLineOptions.h"
#include "OfflineControl.h"
#include "gui/Animation.h"
#include "gui/ShadersKeyboardInteractionTechniques.h"
#include "gui/ShadersMouseInteractionTechniques.h"
#include "render/JogHudRenderer.h"

using namespace vsdk::toolkit::environment::camera;
using namespace vsdk::toolkit::gui;
using namespace vsdk::toolkit::render::opengl4;

static const int WINDOW_WIDTH = 1100;
static const int WINDOW_HEIGHT = 900;
static const Vector3Dd DEFAULT_BUMP_SCALE(1.0, 1.0, 1.0);

static RGBImageUncompressed* loadRgbByCandidates(const std::vector<std::string>& candidates)
{
    for (size_t i = 0; i < candidates.size(); i++) {
        java::File f(candidates[i].c_str());
        if (!f.exists() || !f.canRead()) continue;
        RGBImageUncompressed* img = ImagePersistence::importRGB(f);
        if (img != 0 && img->getXSize() > 0 && img->getYSize() > 0) return img;
        if (img != 0) delete img;
    }
    return 0;
}

static NormalMap* loadBumpNormalMapByCandidates(const std::vector<std::string>& candidates)
{
    for (size_t i = 0; i < candidates.size(); i++) {
        java::File f(candidates[i].c_str());
        if (!f.exists() || !f.canRead()) continue;
        IndexedColorImageUncompressed* bump = ImagePersistence::importIndexedColor(f);
        if (bump == 0 || bump->getXSize() <= 0 || bump->getYSize() <= 0) {
            if (bump != 0) delete bump;
            continue;
        }
        NormalMap* nm = new NormalMap();
        nm->importBumpMap(bump, DEFAULT_BUMP_SCALE);
        delete bump;
        return nm;
    }
    return 0;
}

static void applyOptionsToQuality(const CommandLineOptions& options, RendererConfiguration& quality)
{
    if (options.hasWithTexture) quality.setTexture(options.withTexture);
    if (options.hasWithBumpMap) quality.setBumpMap(options.withBumpMap);
    if (options.hasShadingType) quality.setShadingType(options.shadingType);
    if (options.hasCpuTextureOffsetU || options.hasCpuTextureOffsetV) {
        CpuTextureSamplingConfig::setTextureOffsetTexels(
            options.hasCpuTextureOffsetU ? options.cpuTextureOffsetUTexels : -0.5,
            options.hasCpuTextureOffsetV ? options.cpuTextureOffsetVTexels : -0.5);
    }
}

static void applyLightRotationIfAny(const CommandLineOptions& options, Light* light)
{
    if (!options.hasLightRotation || light == 0) return;
    const double radians = options.lightRotationDegrees * M_PI / 180.0;
    Matrix4x4d rotation = Matrix4x4d().axisRotation(radians, 0.0, -1.0, 0.0);
    Vector3Dd baseLightPosition(1.0, -3.0, 1.0);
    light->setPosition(rotation.multiply(baseLightPosition));
}

class App {
public:
    GLFWwindow* window;
    Camera* camera;
    CameraControllerAquynza* controller;
    vsdk::toolkit::gui::RendererConfigurationController* qualityController;
    Sphere* sphere;
    Light* light;
    RendererConfiguration quality;
    SimpleMaterial material;
    RGBImageUncompressed* textureMap;
    RGBImageUncompressed* bumpMap;
    NormalMap* bumpNormalMap;
    int meridians;
    int parallels;
    double angle;
    bool animationEnabled;
    bool lightAnimationEnabled;
    bool showHud;
    ShaderOperationMode renderingMode;
    double lastHudTitleTickSeconds;
    RGBImageUncompressed* softwareFrameImage;
    JogHudRenderer* hudRenderer;
    Animation animation;
    ShadersKeyboardInteractionTechniques keyboardInteractionTechniques;
    ShadersMouseInteractionTechniques mouseInteractionTechniques;

    App()
        : window(0), camera(0), controller(0), qualityController(0), sphere(0), light(0),
          textureMap(0), bumpMap(0), bumpNormalMap(0), meridians(100), parallels(50), angle(0.0),
          animationEnabled(false), lightAnimationEnabled(false), showHud(true),
          renderingMode(ShaderOperationMode::OPENGL_4_1),
          lastHudTitleTickSeconds(-1.0), softwareFrameImage(0), hudRenderer(0) {}

    void updateHudTitle()
    {
        if (!showHud || window == 0) return;
        const double now = nowSeconds();
        if (lastHudTitleTickSeconds > 0.0 && (now - lastHudTitleTickSeconds) < 0.20) return;
        lastHudTitleTickSeconds = now;

        const int triangles = std::max(0, (parallels - 1) * meridians * 2);
        const char* modeText = renderingMode == ShaderOperationMode::OPENGL_4_1 ? "GPU" : "CPU";
        char title[512];
        std::snprintf(title, sizeof(title),
            "VITRAL ShadersExample | Mode: %s [.] | Meridians: %d | Parallels: %d | Triangles: %d | HUD [h]",
            modeText, meridians, parallels, triangles);
        glfwSetWindowTitle(window, title);
    }

    bool ensureSoftwareFrameImageForViewport()
    {
        int w = 1;
        int h = 1;
        glfwGetFramebufferSize(window, &w, &h);
        w = std::max(1, w);
        h = std::max(1, h);
        if (softwareFrameImage != 0 && softwareFrameImage->getXSize() == w && softwareFrameImage->getYSize() == h) {
            return true;
        }
        if (softwareFrameImage != 0) {
            OpenGL4ImageRenderer::unload(softwareFrameImage);
            delete softwareFrameImage;
            softwareFrameImage = 0;
        }
        softwareFrameImage = new RGBImageUncompressed();
        if (!softwareFrameImage->init(w, h)) {
            delete softwareFrameImage;
            softwareFrameImage = 0;
            return false;
        }
        return true;
    }

    void renderSoftwareFrame(const Matrix4x4d& modelRotation)
    {
        if (!ensureSoftwareFrameImageForViewport()) return;

        std::vector<SimpleBody*> bodies;
        std::vector<Light*> lights;
        SimpleBackground* background = 0;
        SimpleSceneSnapshot* snapshot = 0;

        try {
            camera->updateViewportResize(softwareFrameImage->getXSize(), softwareFrameImage->getYSize());

            SimpleBody* sphereBody = new SimpleBody();
            sphereBody->setGeometry(new Sphere(sphere->getRadius()));
            sphereBody->setMaterial(new SimpleMaterial(material));
            if (textureMap != 0 && quality.isTextureSet()) {
                sphereBody->setTexture(textureMap->clone());
            }
            if (bumpNormalMap != 0 && quality.isBumpMapSet()) {
                sphereBody->setNormalMap(bumpNormalMap->clone());
            }
            sphereBody->setRotation(modelRotation);
            bodies.push_back(sphereBody);

            Light* ambientLight = new Light(LightType::AMBIENT, Vector3Dd(0, 0, 0), ColorRgb(1, 1, 1));
            ambientLight->setId(0);
            lights.push_back(ambientLight);

            Light* pointLight = new Light(light->getLightType(), light->getPosition(), light->getSpecular());
            pointLight->setId(1);
            lights.push_back(pointLight);

            background = new SimpleBackground();
            background->setColor(0, 0, 0);

            snapshot = new SimpleSceneSnapshot(
                bodies,
                lights,
                background,
                camera->exportToCameraSnapshot(softwareFrameImage->getXSize(), softwareFrameImage->getYSize()));

            SimpleRaytracer raytracer;
            raytracer.execute(softwareFrameImage, &quality, snapshot, 0);
        }
        catch (...) {
            if (snapshot != 0) delete snapshot;
            if (background != 0) delete background;
            for (size_t i = 0; i < lights.size(); i++) delete lights[i];
            for (size_t i = 0; i < bodies.size(); i++) delete bodies[i];
            throw;
        }

        delete snapshot;
        delete background;
        for (size_t i = 0; i < lights.size(); i++) delete lights[i];
        for (size_t i = 0; i < bodies.size(); i++) delete bodies[i];

        OpenGL4ImageRenderer::unload(softwareFrameImage);
        OpenGL4ImageRenderer::draw(softwareFrameImage);
    }

    static double nowSeconds()
    {
        return glfwGetTime();
    }

    ~App() {
        const bool sharedBumpTexture = (bumpMap != 0 && bumpMap == textureMap);
        if (textureMap) {
            OpenGL4ImageRenderer::unload(textureMap);
            delete textureMap;
            textureMap = 0;
        }
        if (bumpMap && !sharedBumpTexture) {
            OpenGL4ImageRenderer::unload(bumpMap);
            delete bumpMap;
            bumpMap = 0;
        }
        if (bumpNormalMap) {
            delete bumpNormalMap;
            bumpNormalMap = 0;
        }
        if (softwareFrameImage) {
            OpenGL4ImageRenderer::unload(softwareFrameImage);
            delete softwareFrameImage;
            softwareFrameImage = 0;
        }
        if (hudRenderer) {
            delete hudRenderer;
            hudRenderer = 0;
        }
        delete light;
        delete sphere;
        delete qualityController;
        delete controller;
        delete camera;
        OpenGL4SphereRenderer::dispose();
        OpenGL4ImageRenderer::dispose();
        OpenGL4MatrixRenderer::release();
        if (window) glfwDestroyWindow(window);
        glfwTerminate();
    }

    bool initFromOptions(const CommandLineOptions& options) {
        if (!glfwInit()) return false;
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "VITRAL OpenGL4 Shaders Example", nullptr, nullptr);
        if (!window) return false;
        glfwMakeContextCurrent(window);
        glfwSetWindowUserPointer(window, this);
        glfwSwapInterval(1);

        glewExperimental = GL_TRUE;
        if (glewInit() != GLEW_OK) return false;

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0, 0, 0, 1);

        camera = new Camera();
        camera->setPosition(Vector3Dd(0, -4, 0));
        camera->setRotation(Matrix4x4d().eulerAnglesRotation(M_PI / 2.0, 0.0, 0.0));
        camera->setFov(30.0);
        camera->updateViewportResize(WINDOW_WIDTH, WINDOW_HEIGHT);

        controller = new CameraControllerAquynza(camera);
        qualityController = new vsdk::toolkit::gui::RendererConfigurationController(&quality);
        hudRenderer = new JogHudRenderer();
        sphere = new Sphere(1.0);
        light = new Light(LightType::POINT, Vector3Dd(1, -3, 1), ColorRgb(1, 1, 1));
        light->setId(0);
        applyLightRotationIfAny(options, light);

        quality.setTexture(true);
        quality.setBumpMap(true);
        quality.setShadingType(RendererConfiguration::SHADING_TYPE_PHONG);
        applyOptionsToQuality(options, quality);

        material = material.withAmbient(ColorRgb(0.1, 0.1, 0.1));
        material = material.withDiffuse(ColorRgb(1.0, 1.0, 1.0));
        material = material.withSpecular(ColorRgb(1.0, 1.0, 1.0));
        material = material.withPhongExponent(40.0);

        textureMap = loadRgbByCandidates({
            "../../../../etc/textures/miniearth.png",
            "../etc/textures/miniearth.png",
            "../../etc/textures/miniearth.png"
        });
        bumpNormalMap = loadBumpNormalMapByCandidates({
            "../../../../etc/bumpmaps/earth.bw",
            "../etc/bumpmaps/earth.bw"
        });
        if (bumpNormalMap != 0) {
            bumpMap = bumpNormalMap->exportToRgbImage();
        }
        if (textureMap == 0) {
            std::fprintf(stderr, "ShadersExample: textureMap not loaded\n");
        }
        if (bumpNormalMap == 0 || bumpMap == 0) {
            std::fprintf(stderr, "ShadersExample: bumpMap not loaded (bump pass disabled)\n");
        }

        if (options.hasMeridians) meridians = options.meridians;
        if (options.hasParallels) parallels = options.parallels;
        if (options.hasRotation) angle = options.rotationDegrees * M_PI / 180.0;

        glfwSetFramebufferSizeCallback(window, [](GLFWwindow* w, int width, int height) {
            glViewport(0, 0, width, height);
            App* app = reinterpret_cast<App*>(glfwGetWindowUserPointer(w));
            if (app && app->camera) app->camera->updateViewportResize(width, height);
        });

        glfwSetKeyCallback(window, [](GLFWwindow* w, int key, int, int action, int mods) {
            App* app = reinterpret_cast<App*>(glfwGetWindowUserPointer(w));
            if (!app) return;
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                KeyEvent ev = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
                class CallbackActions : public ShadersKeyboardInteractionTechniques::Actions {
                public:
                    explicit CallbackActions(GLFWwindow* inWindow) : window(inWindow) {}
                    void requestExit() override { glfwSetWindowShouldClose(window, GLFW_TRUE); }
                    void animationStateChanged() override {}
                private:
                    GLFWwindow* window;
                } actions(w);
                app->keyboardInteractionTechniques.processPressedForApp(
                    ev,
                    app->controller,
                    app->qualityController,
                    app->light,
                    &app->quality,
                    &app->meridians,
                    &app->parallels,
                    &app->showHud,
                    &app->animationEnabled,
                    &app->lightAnimationEnabled,
                    &app->renderingMode,
                    &actions);
            } else if (action == GLFW_RELEASE) {
                KeyEvent ev = GlfwSystem::glfw2vsdkKeyEvent(key, mods);
                app->keyboardInteractionTechniques.processReleasedForApp(
                    ev,
                    app->controller);
            }
        });

        glfwSetMouseButtonCallback(window, [](GLFWwindow* w, int button, int action, int) {
            App* app = reinterpret_cast<App*>(glfwGetWindowUserPointer(w));
            if (!app) return;
            double x, y;
            glfwGetCursorPos(w, &x, &y);
            MouseEvent e = GlfwSystem::glfw2vsdkMouseEvent(button, action, x, y);
            if (action == GLFW_PRESS) app->mouseInteractionTechniques.processMousePressedForApp(app->controller, e);
            if (action == GLFW_RELEASE) app->mouseInteractionTechniques.processMouseReleasedForApp(app->controller, e);
        });

        glfwSetCursorPosCallback(window, [](GLFWwindow* w, double x, double y) {
            App* app = reinterpret_cast<App*>(glfwGetWindowUserPointer(w));
            if (!app) return;
            MouseEvent e = GlfwSystem::glfw2vsdkMotionEvent(x, y);
            int l = glfwGetMouseButton(w, GLFW_MOUSE_BUTTON_LEFT);
            int m = glfwGetMouseButton(w, GLFW_MOUSE_BUTTON_MIDDLE);
            int r = glfwGetMouseButton(w, GLFW_MOUSE_BUTTON_RIGHT);
            int evMods = 0;
            if (l == GLFW_PRESS) evMods |= MouseEvent::BUTTON1_DOWN_MASK;
            if (m == GLFW_PRESS) evMods |= MouseEvent::BUTTON2_DOWN_MASK;
            if (r == GLFW_PRESS) evMods |= MouseEvent::BUTTON3_DOWN_MASK;
            e.setModifiers(evMods);
            if (l == GLFW_PRESS || m == GLFW_PRESS || r == GLFW_PRESS) {
                app->mouseInteractionTechniques.processMouseDraggedForApp(app->controller, e);
            }
            else {
                app->mouseInteractionTechniques.processMouseMovedForApp(app->controller, e);
            }
        });

        glfwSetScrollCallback(window, [](GLFWwindow* w, double xoff, double yoff) {
            App* app = reinterpret_cast<App*>(glfwGetWindowUserPointer(w));
            if (!app) return;
            MouseEvent e = GlfwSystem::glfw2vsdkWheelEvent(xoff, yoff);
            app->mouseInteractionTechniques.processMouseWheelMovedForApp(app->controller, e);
        });

        return true;
    }

    void run() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            animation.tickForApp(
                &angle,
                animationEnabled,
                light,
                lightAnimationEnabled,
                nowSeconds());
            Matrix4x4d modelRotation = Matrix4x4d().axisRotation(angle, 0, 0, 1);
            Matrix4x4d worldTransform = Matrix4x4d::identityMatrix().multiply(modelRotation);

            if (renderingMode == ShaderOperationMode::SOFTWARE) {
                renderSoftwareFrame(worldTransform);
            }
            else {
                OpenGL4SphereRenderer::draw(
                    sphere,
                    camera,
                    light,
                    &material,
                    &quality,
                    textureMap,
                    bumpMap,
                    worldTransform,
                    meridians,
                    parallels);
            }

            if (showHud) {
                if (hudRenderer) {
                    int vx = 0;
                    int vy = 0;
                    int vw = 1;
                    int vh = 1;
                    GLint vp[4] = {0, 0, 1, 1};
                    glGetIntegerv(GL_VIEWPORT, vp);
                    vx = vp[0];
                    vy = vp[1];
                    vw = vp[2];
                    vh = vp[3];
                    hudRenderer->draw(
                        true,
                        vx,
                        vy,
                        vw,
                        vh,
                        renderingMode == ShaderOperationMode::OPENGL_4_1,
                        meridians,
                        parallels,
                        &quality,
                        std::string());
                }
            }

            updateHudTitle();

            glfwSwapBuffers(window);
        }
    }
};

int main(int argc, char** argv)
{
    try {
        CommandLineOptions options = CommandLineOptions::parse(argc, argv);
        if (options.offline) {
            return OfflineControl::run(options);
        }

        App app;
        if (!app.initFromOptions(options)) {
            fprintf(stderr, "Failed to initialize ShadersExample\n");
            return 1;
        }
        app.run();
        return 0;
    }
    catch (const std::exception& e) {
        fprintf(stderr, "%s\n", e.what());
        return 1;
    }
}

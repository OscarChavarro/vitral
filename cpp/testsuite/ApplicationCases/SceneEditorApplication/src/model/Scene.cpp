#include <cfloat>
#include <cmath>
#include <typeinfo>

#include "java/io/File.h"
#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/background/CubemapBackground.h"
#include "vsdk/toolkit/environment/background/FixedBackground.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/render/raytracing/DepthBufferMode.h"
#include "vsdk/toolkit/render/raytracing/ParallelRaytracer.h"

#include "model/Scene.h"
#include "model/selection/SelectionSet.h"

namespace {
double toRadians(double degrees)
{
    return degrees * M_PI / 180.0;
}

/// Distance of a hit along its ray (the Java version reads the ray stored
/// in the hit)
double hitDistance(const RayHit& hit)
{
    if ( hit.hasHitDistance() ) {
        return hit.getHitDistance();
    }
    if ( hit.getRay() != nullptr ) {
        return hit.getRay()->getT();
    }
    return DBL_MAX;
}

RGBAImageUncompressed* importCubemapFace(const char* fileName)
{
    return ImagePersistence::importRGBA(java::File(fileName));
}
}

ParallelRaytracer* Scene::getRaytracer()
{
    static ParallelRaytracer raytracer;
    return &raytracer;
}

Scene::Scene() : acumObject(1), lightGizmoScale(1.0)
{
    scene = new SimpleScene();

    //-----------------------------------------------------------------
    Matrix4x4d R;
    camera = new Camera();

    R = R.eulerAnglesRotation(toRadians(45), toRadians(-35), 0);
    camera->setPosition(Vector3Dd(-5, -5, 5));
    camera->setRotation(R);

    activeCamera = camera;
    selectedThings = new SelectionSet(&scene->getSimpleBodies());
    selectedLights = new SelectionSet(&scene->getLights());
    selectedDebugThingGroups = new SelectionSet(&debugThingGroups);

    //-----------------------------------------------------------------
    simpleBackground = new SimpleBackground();
    simpleBackground->setColor(0.49, 0.49, 0.49);

    cubemapBackground = nullptr;
    fixedBackground = nullptr;

    selectedBackground = 0;

    //-----------------------------------------------------------------
    showCorridor = false;

    qualityTemplate = new RendererConfiguration();
    qualityTemplate->setSurfaces(true);
    qualityTemplate->setWires(false);
}

Scene::~Scene()
{
    long i;

    delete selectedThings;
    delete selectedLights;
    delete selectedDebugThingGroups;

    // Backgrounds are owned by this scene, not by the vitral scene
    scene->getBackgrounds().clear();
    delete scene;
    delete camera;
    delete simpleBackground;
    delete cubemapBackground;
    delete fixedBackground;
    // Debug groups own their bodies (see `ProjectedViewsDebugger`)
    for ( i = 0; i < debugThingGroups.size(); i++ ) {
        java::ArrayList<SimpleBody*>& bodies =
            debugThingGroups.get(i)->getBodies();
        long j;
        for ( j = 0; j < bodies.size(); j++ ) {
            delete bodies.get(j);
        }
        delete debugThingGroups.get(i);
    }
    delete qualityTemplate;
}

double Scene::getLightGizmoScale() const
{
    return lightGizmoScale;
}

void Scene::setLightGizmoScale(double lightGizmoScale)
{
    this->lightGizmoScale = lightGizmoScale;
}

bool Scene::buildCubemap()
{
    RGBAImageUncompressed* front;
    RGBAImageUncompressed* right;
    RGBAImageUncompressed* back;
    RGBAImageUncompressed* left;
    RGBAImageUncompressed* down;
    RGBAImageUncompressed* up;

    java::System::out.print("Loading background: 1");
    front = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno0.jpg");
    java::System::out.print("2");
    right = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno1.jpg");
    java::System::out.print("3");
    back = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno2.jpg");
    java::System::out.print("4");
    left = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno3.jpg");
    java::System::out.print("5");
    down = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno4.jpg");
    java::System::out.print("6");
    up = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno5.jpg");

    if ( front == nullptr || right == nullptr || back == nullptr ||
         left == nullptr || down == nullptr || up == nullptr ) {
        java::System::err.println(" Error loading cubemap images");
        delete front;
        delete right;
        delete back;
        delete left;
        delete down;
        delete up;
        return false;
    }
    java::System::out.println(" OK!");

    // C++ port note: the images are referenced by the background, and kept
    // for the rest of the program (as done by the Java garbage collector)
    cubemapBackground =
        new CubemapBackground(camera, front, right, back, left, down, up);
    return true;
}

bool Scene::buildFixedmap()
{
    RGBAImageUncompressed* img;

    java::System::out.print("Loading background: ");
    img = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno0.jpg");
    if ( img == nullptr ) {
        java::System::err.println("Error loading background image");
        return false;
    }
    java::System::out.println("OK!");

    fixedBackground = new FixedBackground(camera, img);
    return true;
}

SimpleMaterial Scene::defaultMaterial()
{
    SimpleMaterial m;

/*
    m = m.withAmbient(ColorRgb(0.2, 0.2, 0.2));
    m = m.withDiffuse(ColorRgb(0.5, 0.9, 0.5));
    m = m.withSpecular(ColorRgb(1, 1, 1));
    m = m.withDoubleSided(false);
    m = m.withPhongExponent(100.0);
*/

    m = m.withAmbient(ColorRgb(0, 0, 0));
    m = m.withDiffuse(ColorRgb(1, 1, 1));
    m = m.withSpecular(ColorRgb(1, 1, 1));
    m = m.withDoubleSided(false);
    m = m.withPhongExponent(40.0);

    return m;
}

SimpleBody* Scene::addThing(Geometry* g)
{
    SimpleBody* thing;

    thing = new SimpleBody();
    thing->setGeometry(g);
    thing->setPosition(Vector3Dd());
    thing->setRotation(Matrix4x4d());
    thing->setRotationInverse(Matrix4x4d());
    thing->setMaterial(new SimpleMaterial(defaultMaterial()));
    thing->setName(java::String("Geometric object ") +
                   java::String::valueOf(acumObject));
    scene->getSimpleBodies().add(thing);

    acumObject++;
    selectedThings->sync();
    return thing;
}

bool Scene::doIntersectionFirstHit(const Ray& inRay, RayHit* info)
{
    int i;
    double nearestDistance = FLT_MAX;
    java::ArrayList<SimpleBody*>& things = scene->getSimpleBodies();
    bool intersected = false;
    SimpleBody* gi;
    RayHit ii;
    Ray r(inRay);

    for ( i = 0; i < things.size(); i++ ) {
        gi = things.get(i);
        RayHit hit;
        hit.setStoreRay(true);
        if ( gi->doIntersectionFirstHit(r, &hit) ) {
            double t = hitDistance(hit);
            if ( t < nearestDistance ) {
                ii.clone(hit);
                nearestDistance = t;
                r = r.withT(t);
                intersected = true;
            }
        }
    }
    if ( intersected ) {
        info->clone(ii);
        return true;
    }
    return false;
}

void Scene::rotateBackground()
{
    selectedBackground++;
    if ( selectedBackground > 2 ) {
        selectedBackground = 0;
    }
}

void Scene::activateSelectedBackground()
{
    Background* currentBackground;

    currentBackground = simpleBackground;
    switch ( selectedBackground ) {
      case 2:
        if ( cubemapBackground == nullptr ) {
            buildCubemap();
        }
        if ( cubemapBackground != nullptr ) {
            cubemapBackground->setCamera(activeCamera);
            currentBackground = cubemapBackground;
        }
        break;
      case 1:
        if ( fixedBackground == nullptr ) {
            buildFixedmap();
        }
        if ( fixedBackground != nullptr ) {
            currentBackground = fixedBackground;
        }
        break;
    }

    java::ArrayList<Background*>& list = scene->getBackgrounds();
    if ( list.size() < 1 ) {
        list.add(currentBackground);
    }
    else {
        list.remove((long)0);
        list.add((long)0, currentBackground);
    }
    scene->setActiveBackgroundIndex(0);
}

void Scene::print()
{
    java::ArrayList<SimpleBody*>& things = scene->getSimpleBodies();
    int i;

    java::System::out.println("= SCENE REPORT ============================================================");
    java::System::out.println((java::String("Current camera:\n") +
                               activeCamera->toString()).c_str());
    java::System::out.println((java::String("Things in scene: ") +
        java::String::valueOf((long)things.size())).c_str());
    for ( i = 0; i < things.size(); i++ ) {
        Geometry* g = things.get(i)->getGeometry();
        java::String msg = java::String("  - Thing[") +
            java::String::valueOf(i) + "]: " +
            (g != nullptr ? typeid(*g).name() : "null");
        java::System::out.println(msg.c_str());
    }
    java::System::out.println("= END OF REPORT ===========================================================");
}

void Scene::raytrace(RGBImageUncompressed* outViewport)
{
    raytrace(outViewport, nullptr, true);
}

void Scene::raytraceViewport(RGBImageUncompressed* outViewport)
{
    raytrace(outViewport, nullptr, false);
}

void Scene::raytraceViewport(RGBImageUncompressed* outViewport,
                             ZBuffer* outDepth)
{
    raytrace(outViewport, outDepth, false);
}

void Scene::raytrace(RGBImageUncompressed* outViewport, ZBuffer* outDepth,
                     bool interactiveReport)
{
    int originalWidth;
    int originalHeight;

    originalWidth = (int)activeCamera->getViewportXSize();
    originalHeight = (int)activeCamera->getViewportYSize();
    CameraSnapshot* cameraSnapshot = activeCamera->exportToCameraSnapshot(
        outViewport->getXSize(), outViewport->getYSize());

    //-----------------------------------------------------------------

    Background* activeBackground;
    switch ( selectedBackground ) {
      case 2:
        if ( cubemapBackground == nullptr ) {
            buildCubemap();
        }
        if ( cubemapBackground != nullptr ) {
            activeBackground = cubemapBackground;
        }
        else {
            activeBackground = simpleBackground;
        }
        break;
      case 1:
        if ( fixedBackground == nullptr ) {
            buildFixedmap();
        }
        if ( fixedBackground != nullptr ) {
            activeBackground = fixedBackground;
        }
        else {
            activeBackground = simpleBackground;
        }
        break;
      case 0: default:
        activeBackground = simpleBackground;
        break;
    }

    // The snapshot owns the camera snapshot
    SimpleSceneSnapshot* sceneSnapshot =
        scene->exportToSimpleSceneSnapshot(cameraSnapshot, activeBackground);
    long long initialTime = java::System::currentTimeMillis();
    getRaytracer()->execute(outViewport, outDepth,
        outDepth != nullptr ? DepthBufferMode::OPENGL_DEPTH :
                              DepthBufferMode::NONE,
        qualityTemplate, sceneSnapshot, interactiveReport);
    long long finalTime = java::System::currentTimeMillis();
    delete sceneSnapshot;

    if ( interactiveReport ) {
        java::String msg = java::String("Image generated in ") +
            java::String::valueOf(finalTime-initialTime) + " miliseconds.";
        java::System::out.println(msg.c_str());

        java::File fd("./output.jpg");

        java::System::out.print("Exporting result image to file: ");
        if ( !ImagePersistence::exportJPEG(fd, outViewport) ) {
            java::System::err.println("Error grabando la imagen!!");
            java::System::exit(1);
        }
        java::System::out.println(" OK!");
        java::System::out.println("An image has been created in the file output.jpg");
    }

    //-----------------------------------------------------------------
    activeCamera->updateViewportResize(originalWidth, originalHeight);
}

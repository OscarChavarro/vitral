#include <float.h>
#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
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
#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/render/raytracing/DepthBufferMode.h"
#include "vsdk/toolkit/render/raytracing/ParallelRaytracer.h"

#include "model/Scene.h"
#include "model/selection/SelectionSet.h"

static double toRadians(double degrees)
{
    return degrees * M_PI / 180.0;
}

/// Distance of a hit along its ray (the Java version reads the ray stored
/// in the hit)
static double hitDistance(const RayHit& hit)
{
    if ( hit.hasHitDistance() ) {
        return hit.getHitDistance();
    }
    if ( hit.getRay() != NULL ) {
        return hit.getRay()->getT();
    }
    return DBL_MAX;
}

static RGBAImageUncompressed* importCubemapFace(const char* fileName)
{
    return ImagePersistence::importRGBA(File(fileName));
}

ParallelRaytracer* Scene::getRaytracer()
{
    static ParallelRaytracer raytracer;
    return &raytracer;
}

Scene::Scene() : acumObject(1), importedMitScene(FALSE),
    importedSceneBackground(FALSE), lightGizmoScale(1.0)
{
    scene = new SimpleScene();

    //-----------------------------------------------------------------
    Matrix4x4d R;
    camera = new Camera();

    R = R.eulerAnglesRotation(toRadians(45), toRadians(-35), 0);
    camera->setPosition(Vector3Dd(-5, -5, 5));
    camera->setRotation(R);

    activeCamera = camera;
    selectedThings = new SelectionSet(
        EntityListView(&scene->getSimpleBodies()));
    selectedLights = new SelectionSet(EntityListView(&scene->getLights()));
    selectedDebugThingGroups = new SelectionSet(
        EntityListView(&debugThingGroups));

    //-----------------------------------------------------------------
    simpleBackground = new SimpleBackground();
    simpleBackground->setColor(0.49, 0.49, 0.49);

    cubemapBackground = NULL;
    fixedBackground = NULL;

    selectedBackground = 0;

    //-----------------------------------------------------------------
    showCorridor = FALSE;

    qualityTemplate = new RendererConfiguration();
    qualityTemplate->setSurfaces(TRUE);
    qualityTemplate->setWires(FALSE);
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
        ArrayList<SimpleBody*>& bodies =
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

BOOLEAN Scene::buildCubemap()
{
    RGBAImageUncompressed* front;
    RGBAImageUncompressed* right;
    RGBAImageUncompressed* back;
    RGBAImageUncompressed* left;
    RGBAImageUncompressed* down;
    RGBAImageUncompressed* up;

    System::out.print("Loading background: 1");
    front = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno0.jpg");
    System::out.print("2");
    right = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno1.jpg");
    System::out.print("3");
    back = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno2.jpg");
    System::out.print("4");
    left = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno3.jpg");
    System::out.print("5");
    down = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno4.jpg");
    System::out.print("6");
    up = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno5.jpg");

    if ( front == NULL || right == NULL || back == NULL ||
         left == NULL || down == NULL || up == NULL ) {
        System::err.println(" Error loading cubemap images");
        delete front;
        delete right;
        delete back;
        delete left;
        delete down;
        delete up;
        return FALSE;
    }
    System::out.println(" OK!");

    // C++ port note: the images are referenced by the background, and kept
    // for the rest of the program (as done by the Java garbage collector)
    cubemapBackground =
        new CubemapBackground(camera, front, right, back, left, down, up);
    return TRUE;
}

BOOLEAN Scene::buildFixedmap()
{
    RGBAImageUncompressed* img;

    System::out.print("Loading background: ");
    img = importCubemapFace("../../../../etc/cubemaps/dorise1/entorno0.jpg");
    if ( img == NULL ) {
        System::err.println("Error loading background image");
        return FALSE;
    }
    System::out.println("OK!");

    fixedBackground = new FixedBackground(camera, img);
    return TRUE;
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
    m = m.withDoubleSided(FALSE);
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
    thing->setName(String("Geometric object ") +
                   String::valueOf(acumObject));
    scene->getSimpleBodies().add(thing);

    acumObject++;
    selectedThings->sync();
    return thing;
}

BOOLEAN Scene::isHidden(SimpleBody* body) const
{
    return hiddenBodies.contains(body);
}

void Scene::setHidden(SimpleBody* body, BOOLEAN hidden)
{
    if ( hidden ) {
        hiddenBodies.add(body);
    }
    else {
        hiddenBodies.remove(body);
    }
}

BOOLEAN Scene::doIntersectionFirstHit(const Ray& inRay, RayHit* info)
{
    int i;
    double nearestDistance = FLT_MAX;
    ArrayList<SimpleBody*>& things = scene->getSimpleBodies();
    BOOLEAN intersected = FALSE;
    SimpleBody* gi;
    RayHit ii;
    Ray r(inRay);

    for ( i = 0; i < things.size(); i++ ) {
        gi = things.get(i);
        if ( isHidden(gi) ) continue;
        RayHit hit;
        hit.setStoreRay(TRUE);
        if ( gi->doIntersectionFirstHit(r, &hit) ) {
            double t = hitDistance(hit);
            if ( t < nearestDistance ) {
                ii.clone(hit);
                nearestDistance = t;
                r = r.withT(t);
                intersected = TRUE;
            }
        }
    }
    if ( intersected ) {
        info->clone(ii);
        return TRUE;
    }
    return FALSE;
}

void Scene::rotateBackground()
{
    // An explicit editor background choice takes precedence over `.ray`.
    importedSceneBackground = FALSE;
    selectedBackground++;
    if ( selectedBackground > 2 ) {
        selectedBackground = 0;
    }
}

void Scene::activateSelectedBackground()
{
    Background* currentBackground;

    if ( importedSceneBackground && scene->getBackgrounds().size() > 0 ) {
        return;
    }

    currentBackground = simpleBackground;
    switch ( selectedBackground ) {
      case 2:
        if ( cubemapBackground == NULL ) {
            buildCubemap();
        }
        if ( cubemapBackground != NULL ) {
            cubemapBackground->setCamera(activeCamera);
            currentBackground = cubemapBackground;
        }
        break;
      case 1:
        if ( fixedBackground == NULL ) {
            buildFixedmap();
        }
        if ( fixedBackground != NULL ) {
            currentBackground = fixedBackground;
        }
        break;
    }

    ArrayList<Background*>& list = scene->getBackgrounds();
    if ( list.size() < 1 ) {
        list.add(currentBackground);
    }
    else {
        list.remove((long)0);
        list.add((long)0, currentBackground);
    }
    scene->setActiveBackgroundIndex(0);
}

void Scene::importMitScene(const File& file)
{
    SimpleScene imported;
    ReaderMitScene reader;
    reader.importEnvironment(file.getPath().c_str(), &imported);

    // Do not replace `scene`: OpenGL renderers own a reference to this
    // instance for their complete lifetime.  Transfer the imported elements
    // instead, carefully detaching them from the temporary owner.
    scene->clearOwnedElements();
    ArrayList<SimpleBody*>& bodies = imported.getSimpleBodies();
    ArrayList<Light*>& lights = imported.getLights();
    ArrayList<Background*>& backgrounds = imported.getBackgrounds();
    ArrayList<Camera*>& cameras = imported.getCameras();
    long i;
    for ( i = 0; i < bodies.size(); i++ ) scene->getSimpleBodies().add(bodies.get(i));
    for ( i = 0; i < lights.size(); i++ ) scene->getLights().add(lights.get(i));
    for ( i = 0; i < backgrounds.size(); i++ ) scene->getBackgrounds().add(backgrounds.get(i));
    for ( i = 0; i < cameras.size(); i++ ) scene->getCameras().add(cameras.get(i));
    scene->setActiveCameraIndex(imported.getActiveCameraIndex());
    scene->setActiveBackgroundIndex(imported.getActiveBackgroundIndex());
    bodies.clear(); lights.clear(); backgrounds.clear(); cameras.clear();
    hiddenBodies.clear();

    activeCamera = scene->getActiveCamera();
    cubemapBackground = NULL;
    fixedBackground = NULL;
    importedMitScene = TRUE;
    importedSceneBackground = scene->getBackgrounds().size() > 0;
    selectedBackground = 0;
    // Debug groups are editor-owned, unlike bodies owned by SimpleScene.
    // Release them before replacing their selection domain.
    for ( i = 0; i < debugThingGroups.size(); i++ ) {
        ArrayList<SimpleBody*>& debugBodies =
            debugThingGroups.get(i)->getBodies();
        for ( long j = 0; j < debugBodies.size(); j++ ) delete debugBodies.get(j);
        delete debugThingGroups.get(i);
    }
    debugThingGroups.clear();
    selectedThings->sync(); selectedThings->unselectAll();
    selectedLights->sync(); selectedLights->unselectAll();
    selectedDebugThingGroups->sync(); selectedDebugThingGroups->unselectAll();
    acumObject = scene->getSimpleBodies().size() + 1;
    optimizeRendererConfigurationForImportedScene();
}

void Scene::optimizeRendererConfigurationForImportedScene()
{
    BOOLEAN hasTextures = FALSE;
    BOOLEAN hasNormalMaps = FALSE;
    ArrayList<SimpleBody*>& bodies = scene->getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        hasTextures = hasTextures || bodies.get(i)->getTexture() != NULL;
        hasNormalMaps = hasNormalMaps || bodies.get(i)->getNormalMap() != NULL;
        if ( hasTextures && hasNormalMaps ) break;
    }
    qualityTemplate->setTexture(hasTextures);
    qualityTemplate->setBumpMap(hasNormalMaps);
}

void Scene::print()
{
    ArrayList<SimpleBody*>& things = scene->getSimpleBodies();
    int i;

    System::out.println("= SCENE REPORT ============================================================");
    System::out.println((String("Current camera:\n") +
                               activeCamera->toString()).c_str());
    System::out.println((String("Things in scene: ") +
        String::valueOf((long)things.size())).c_str());
    for ( i = 0; i < things.size(); i++ ) {
        Geometry* g = things.get(i)->getGeometry();
        String msg = String("  - Thing[") +
            String::valueOf(i) + "]: " +
            (g != NULL ? g->getClassSimpleName() : String("null"));
        System::out.println(msg.c_str());
    }
    System::out.println("= END OF REPORT ===========================================================");
}

void Scene::raytrace(RGBImageUncompressed* outViewport)
{
    raytrace(outViewport, NULL, TRUE);
}

void Scene::raytraceViewport(RGBImageUncompressed* outViewport)
{
    raytrace(outViewport, NULL, FALSE);
}

void Scene::raytraceViewport(RGBImageUncompressed* outViewport,
                             ZBuffer* outDepth)
{
    raytrace(outViewport, outDepth, FALSE);
}

void Scene::raytrace(RGBImageUncompressed* outViewport, ZBuffer* outDepth,
                     BOOLEAN interactiveReport)
{
    int originalWidth;
    int originalHeight;

    originalWidth = (int)activeCamera->getViewportXSize();
    originalHeight = (int)activeCamera->getViewportYSize();
    if ( importedMitScene ) optimizeRendererConfigurationForImportedScene();
    CameraSnapshot* cameraSnapshot = activeCamera->exportToCameraSnapshot(
        outViewport->getXSize(), outViewport->getYSize());

    //-----------------------------------------------------------------

    Background* activeBackground;
    if ( importedSceneBackground && scene->getBackgrounds().size() > 0 ) {
        activeBackground = scene->getActiveBackground();
    }
    else switch ( selectedBackground ) {
      case 2:
        if ( cubemapBackground == NULL ) {
            buildCubemap();
        }
        if ( cubemapBackground != NULL ) {
            activeBackground = cubemapBackground;
        }
        else {
            activeBackground = simpleBackground;
        }
        break;
      case 1:
        if ( fixedBackground == NULL ) {
            buildFixedmap();
        }
        if ( fixedBackground != NULL ) {
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
    ArrayList<SimpleBody*> visibleBodies;
    ArrayList<SimpleBody*>& bodies = scene->getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        if ( !isHidden(bodies.get(i)) ) {
            visibleBodies.add(bodies.get(i));
        }
    }
    SimpleSceneSnapshot* sceneSnapshot = new SimpleSceneSnapshot(
        visibleBodies, scene->getLights(), activeBackground, cameraSnapshot);
    long long initialTime = System::currentTimeMillis();
    getRaytracer()->execute(outViewport, outDepth,
        outDepth != NULL ? DepthBufferMode::OPENGL_DEPTH :
                              DepthBufferMode::NONE,
        qualityTemplate, sceneSnapshot, interactiveReport);
    long long finalTime = System::currentTimeMillis();
    delete sceneSnapshot;

    if ( interactiveReport ) {
        String msg = String("Image generated in ") +
            String::valueOf(finalTime-initialTime) + " miliseconds.";
        System::out.println(msg.c_str());

        File fd("./output.jpg");

        System::out.print("Exporting result image to file: ");
        if ( !ImagePersistence::exportJPEG(fd, outViewport) ) {
            System::err.println("Error grabando la imagen!!");
            System::exit(1);
        }
        System::out.println(" OK!");
        System::out.println("An image has been created in the file output.jpg");
    }

    //-----------------------------------------------------------------
    activeCamera->updateViewportResize(originalWidth, originalHeight);
}

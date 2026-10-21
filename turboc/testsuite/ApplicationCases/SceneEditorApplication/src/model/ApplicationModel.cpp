#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Intersection.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/media/RGBColorPalette.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "model/ApplicationModel.h"
#include "model/Scene.h"

ApplicationModel::ApplicationModel()
    : scene(NULL), raytracedImage(NULL), raytracedDepth(NULL),
      zbufferImage(NULL), raytracedImageWidth(0), raytracedImageHeight(0),
      palette(NULL), rayGizmo(NULL),
      activeViewportSetIndex(0),
      i18nContext(NULL), drawingArea(NULL),
      sceneSource(this), rayGizmoIntersector(this),
      editHistory(&sceneSource)
{
    viewportSets.add(ViewportSet::createStandardSet("Display 1"));
    activeViewportSetIndex = 0;
    drawingArea = new DrawingArea(viewportSets.get(0));
}

Scene* ApplicationModel::SceneSource::get()
{
    return model->getScene();
}

Intersection* ApplicationModel::RayGizmoIntersector::apply(const Ray& ray)
{
    return model->makeRayGizmoIntersection(ray);
}

ApplicationModel::~ApplicationModel()
{
    long i;

    // The history may reference the scene and the viewports
    editHistory.getSceneHistory()->clear();
    editHistory.getViewportHistory()->clear();
    delete drawingArea;
    for ( i = 0; i < viewportSets.size(); i++ ) {
        delete viewportSets.get(i);
    }
    delete scene;
    delete i18nContext;
    delete palette;
    delete rayGizmo;
    delete raytracedImage;
    delete raytracedDepth;
    delete zbufferImage;
}

GuiState* ApplicationModel::getGuiState()
{
    return &guiState;
}

DrawingArea* ApplicationModel::getDrawingArea() const
{
    return drawingArea;
}

const ArrayList<ViewportSet*>& ApplicationModel::getViewportSets() const
{
    return viewportSets;
}

void ApplicationModel::addViewportSet(ViewportSet* viewportSet)
{
    if ( viewportSet != NULL ) {
        viewportSet->setI18nContext(i18nContext);
        viewportSets.add(viewportSet);
    }
}

Widget* ApplicationModel::getI18nContext() const
{
    return i18nContext;
}

void ApplicationModel::setI18nContext(Widget* i18nContext)
{
    if ( this->i18nContext != i18nContext ) {
        delete this->i18nContext;
    }
    this->i18nContext = i18nContext;
    long i;
    for ( i = 0; i < viewportSets.size(); i++ ) {
        viewportSets.get(i)->setI18nContext(i18nContext);
    }
}

int ApplicationModel::getActiveViewportSetIndex() const
{
    return activeViewportSetIndex;
}

void ApplicationModel::setActiveViewportSetIndex(int activeViewportSetIndex)
{
    if ( activeViewportSetIndex >= 0 &&
         activeViewportSetIndex < viewportSets.size() ) {
        this->activeViewportSetIndex = activeViewportSetIndex;
    }
}

ViewportSet* ApplicationModel::getActiveViewportSet() const
{
    return viewportSets.get(activeViewportSetIndex);
}

Scene* ApplicationModel::getScene() const
{
    return scene;
}

void ApplicationModel::setScene(Scene* scene)
{
    // The operations of the history reference the former scene
    editHistory.getSceneHistory()->clear();
    if ( this->scene != scene ) {
        delete this->scene;
    }
    this->scene = scene;
    delete rayGizmo;
    rayGizmo = new RayGizmo(&rayGizmoIntersector, 2);
}

void ApplicationModel::useImportedSceneCameraInViewports()
{
    Camera* imported = scene->activeCamera;
    for ( long i = 0; i < viewportSets.size(); i++ ) {
        const ArrayList<Viewport*>& viewports = viewportSets.get(i)->getViewports();
        for ( long j = 0; j < viewports.size(); j++ ) {
            Viewport* viewport = viewports.get(j);
            Camera* perspective = viewport->getPerspectiveCamera();
            *perspective = *imported;
            perspective->setName("Perspective");
            if ( viewport->getPixelSizeX() > 0 && viewport->getPixelSizeY() > 0 ) {
                perspective->updateViewportResize(viewport->getPixelSizeX(),
                                                  viewport->getPixelSizeY());
            }
        }
    }
}

EditHistory* ApplicationModel::getEditHistory()
{
    return &editHistory;
}

Camera* ApplicationModel::getCamera() const
{
    return scene->camera;
}

Camera* ApplicationModel::getActiveCamera() const
{
    return scene->activeCamera;
}

void ApplicationModel::setActiveCamera(Camera* activeCamera)
{
    scene->activeCamera = activeCamera;
}

ArrayList<Light*>& ApplicationModel::getLights()
{
    return scene->scene->getLights();
}

PointLight* ApplicationModel::addNewLight()
{
    PointLight* light = lightFactory.createLight(getLights(),
                                                 getActiveViewportSet());

    if ( light != NULL ) {
        getLights().add(light);
    }
    return light;
}

ArrayList<SimpleBody*>& ApplicationModel::getSimpleBodies()
{
    return scene->scene->getSimpleBodies();
}

ZBuffer* ApplicationModel::getRaytracedDepth() const
{
    return raytracedDepth;
}

void ApplicationModel::setRaytracedDepth(ZBuffer* raytracedDepth)
{
    if ( this->raytracedDepth != raytracedDepth ) {
        delete this->raytracedDepth;
    }
    this->raytracedDepth = raytracedDepth;
}

RGBImageUncompressed* ApplicationModel::getRaytracedImage() const
{
    return raytracedImage;
}

void ApplicationModel::setRaytracedImage(RGBImageUncompressed* raytracedImage)
{
    if ( this->raytracedImage != raytracedImage ) {
        delete this->raytracedImage;
    }
    this->raytracedImage = raytracedImage;
}

RGBImageUncompressed* ApplicationModel::getZbufferImage() const
{
    return zbufferImage;
}

void ApplicationModel::setZbufferImage(RGBImageUncompressed* zbufferImage)
{
    if ( this->zbufferImage != zbufferImage ) {
        delete this->zbufferImage;
    }
    this->zbufferImage = zbufferImage;
}

int ApplicationModel::getRaytracedImageWidth() const
{
    return raytracedImageWidth;
}

void ApplicationModel::setRaytracedImageWidth(int raytracedImageWidth)
{
    this->raytracedImageWidth = raytracedImageWidth;
}

int ApplicationModel::getRaytracedImageHeight() const
{
    return raytracedImageHeight;
}

void ApplicationModel::setRaytracedImageHeight(int raytracedImageHeight)
{
    this->raytracedImageHeight = raytracedImageHeight;
}

RGBColorPalette* ApplicationModel::getPalette() const
{
    return palette;
}

void ApplicationModel::setPalette(RGBColorPalette* palette)
{
    if ( this->palette != palette ) {
        delete this->palette;
    }
    this->palette = palette;
}

bool ApplicationModel::isWithVisualDebugRay() const
{
    return rayGizmo != NULL && rayGizmo->isVisible();
}

void ApplicationModel::setWithVisualDebugRay(bool withVisualDebugRay)
{
    if ( rayGizmo != NULL ) {
        rayGizmo->setVisible(withVisualDebugRay);
    }
}

const Ray* ApplicationModel::getVisualDebugRay() const
{
    if ( rayGizmo == NULL ) {
        return NULL;
    }
    visualDebugRay = Ray(rayGizmo->getPosition(), rayGizmo->getDirection());
    return &visualDebugRay;
}

void ApplicationModel::setVisualDebugRay(const Ray* visualDebugRay)
{
    if ( rayGizmo != NULL && visualDebugRay != NULL ) {
        rayGizmo->setRay(*visualDebugRay, 0.0);
    }
}

int ApplicationModel::getVisualDebugRayLevels() const
{
    return rayGizmo != NULL ? rayGizmo->getMaxNumOfReflections() : 0;
}

void ApplicationModel::setVisualDebugRayLevels(int visualDebugRayLevels)
{
    if ( rayGizmo != NULL ) {
        rayGizmo->setMaxNumOfReflections(visualDebugRayLevels);
    }
}

RayGizmo* ApplicationModel::getRayGizmo() const
{
    return rayGizmo;
}

Intersection* ApplicationModel::makeRayGizmoIntersection(const Ray& ray)
{
    if ( scene == NULL || scene->scene == NULL ) {
        return NULL;
    }

    Intersection* closest = NULL;
    double closestT = 1e308;
    ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();

    for ( long i = 0; i < bodies.size(); i++ ) {
        SimpleBody* body = bodies.get(i);
        RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL);

        if ( body != NULL &&
             body->doIntersectionFirstHit(ray, &hit) &&
             hit.hasHitDistance() ) {
            double t = hit.getHitDistance();

            if ( t > VSDK::EPSILON && t < closestT ) {
                closestT = t;
                delete closest;
                closest = new Intersection(t, hit.point, hit.normal);
            }
        }
    }
    return closest;
}

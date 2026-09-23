#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/media/RGBColorPalette.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "model/ApplicationModel.h"
#include "model/Scene.h"

ApplicationModel::ApplicationModel()
    : scene(nullptr), raytracedImage(nullptr), raytracedDepth(nullptr),
      zbufferImage(nullptr), raytracedImageWidth(0), raytracedImageHeight(0),
      palette(nullptr), withVisualDebugRay(false), hasVisualDebugRay(false),
      visualDebugRayLevels(0), activeViewportSetIndex(0),
      i18nContext(nullptr), drawingArea(nullptr),
      editHistory([this]() { return getScene(); })
{
    viewportSets.add(ViewportSet::createStandardSet("Display 1"));
    activeViewportSetIndex = 0;
    drawingArea = new DrawingArea(viewportSets.get(0));
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

const java::ArrayList<ViewportSet*>& ApplicationModel::getViewportSets() const
{
    return viewportSets;
}

void ApplicationModel::addViewportSet(ViewportSet* viewportSet)
{
    if ( viewportSet != nullptr ) {
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

java::ArrayList<Light*>& ApplicationModel::getLights()
{
    return scene->scene->getLights();
}

PointLight* ApplicationModel::addNewLight()
{
    PointLight* light = lightFactory.createLight(getLights(),
                                                 getActiveViewportSet());

    if ( light != nullptr ) {
        getLights().add(light);
    }
    return light;
}

java::ArrayList<SimpleBody*>& ApplicationModel::getSimpleBodies()
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
    return withVisualDebugRay;
}

void ApplicationModel::setWithVisualDebugRay(bool withVisualDebugRay)
{
    this->withVisualDebugRay = withVisualDebugRay;
}

const Ray* ApplicationModel::getVisualDebugRay() const
{
    return hasVisualDebugRay ? &visualDebugRay : nullptr;
}

void ApplicationModel::setVisualDebugRay(const Ray* visualDebugRay)
{
    hasVisualDebugRay = visualDebugRay != nullptr;
    if ( hasVisualDebugRay ) {
        this->visualDebugRay = *visualDebugRay;
    }
}

int ApplicationModel::getVisualDebugRayLevels() const
{
    return visualDebugRayLevels;
}

void ApplicationModel::setVisualDebugRayLevels(int visualDebugRayLevels)
{
    this->visualDebugRayLevels = visualDebugRayLevels;
}

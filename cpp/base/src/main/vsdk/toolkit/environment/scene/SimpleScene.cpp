#include "vsdk/toolkit/environment/scene/SimpleScene.h"

#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/environment/background/Background.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "java/util/ArrayList.txx"

SimpleScene::SimpleScene()
    : activeCameraIndex(0), activeBackgroundIndex(0)
{
}

SimpleScene::~SimpleScene()
{
    clearOwnedElements();
}

void SimpleScene::clearOwnedElements()
{
    for (long int i = 0; i < simpleBodiesArray.size(); ++i) {
        delete simpleBodiesArray[i];
    }
    for (long int i = 0; i < lightsArray.size(); ++i) {
        delete lightsArray[i];
    }
    for (long int i = 0; i < backgroundsArray.size(); ++i) {
        delete backgroundsArray[i];
    }
    for (long int i = 0; i < camerasArray.size(); ++i) {
        delete camerasArray[i];
    }

    simpleBodiesArray.clear();
    lightsArray.clear();
    backgroundsArray.clear();
    camerasArray.clear();

    activeCameraIndex = 0;
    activeBackgroundIndex = 0;
}

int SimpleScene::getActiveCameraIndex() const { return activeCameraIndex; }
int SimpleScene::getActiveBackgroundIndex() const { return activeBackgroundIndex; }
void SimpleScene::setActiveCameraIndex(int i) { activeCameraIndex = i; }
void SimpleScene::setActiveBackgroundIndex(int i) { activeBackgroundIndex = i; }

void SimpleScene::addBody(SimpleBody* b) { simpleBodiesArray.add(b); }
void SimpleScene::addCamera(Camera* c) { camerasArray.add(c); }
void SimpleScene::addBackground(Background* b) { backgroundsArray.add(b); }
void SimpleScene::addLight(Light* l) { l->setId(static_cast<int>(lightsArray.size())); lightsArray.add(l); }

java::ArrayList<SimpleBody*>& SimpleScene::getSimpleBodies() { return simpleBodiesArray; }
java::ArrayList<Light*>& SimpleScene::getLights() { return lightsArray; }
java::ArrayList<Background*>& SimpleScene::getBackgrounds() { return backgroundsArray; }
java::ArrayList<Camera*>& SimpleScene::getCameras() { return camerasArray; }

void SimpleScene::setSimpleBodies(java::ArrayList<SimpleBody*>& simpleBodies)
{
    for (long int i = 0; i < simpleBodiesArray.size(); ++i) {
        delete simpleBodiesArray[i];
    }
    simpleBodiesArray.clear();
    for (long int i = 0; i < simpleBodies.size(); i++) simpleBodiesArray.add(simpleBodies.get(i));
}
void SimpleScene::setLights(java::ArrayList<Light*>& lights)
{
    for (long int i = 0; i < lightsArray.size(); ++i) {
        delete lightsArray[i];
    }
    lightsArray.clear();
    for (long int i = 0; i < lights.size(); i++) lightsArray.add(lights.get(i));
    for (long int i = 0; i < lightsArray.size(); ++i) {
        lightsArray[i]->setId(static_cast<int>(i));
    }
}
void SimpleScene::setBackgrounds(java::ArrayList<Background*>& backgrounds)
{
    for (long int i = 0; i < backgroundsArray.size(); ++i) {
        delete backgroundsArray[i];
    }
    backgroundsArray.clear();
    for (long int i = 0; i < backgrounds.size(); i++) backgroundsArray.add(backgrounds.get(i));
}
void SimpleScene::setCameras(java::ArrayList<Camera*>& cameras)
{
    for (long int i = 0; i < camerasArray.size(); ++i) {
        delete camerasArray[i];
    }
    camerasArray.clear();
    for (long int i = 0; i < cameras.size(); i++) camerasArray.add(cameras.get(i));
}

Background* SimpleScene::getActiveBackground() const { return backgroundsArray.get(activeBackgroundIndex); }
Camera* SimpleScene::getActiveCamera() const { return camerasArray.get(activeCameraIndex); }

SimpleSceneSnapshot* SimpleScene::exportToSimpleSceneSnapshot()
{
    return exportToSimpleSceneSnapshot(getActiveCamera()->exportToCameraSnapshot(), getActiveBackground());
}

SimpleSceneSnapshot* SimpleScene::exportToSimpleSceneSnapshot(int viewportXSize, int viewportYSize)
{
    return exportToSimpleSceneSnapshot(getActiveCamera()->exportToCameraSnapshot(viewportXSize, viewportYSize), getActiveBackground());
}

SimpleSceneSnapshot* SimpleScene::exportToSimpleSceneSnapshot(CameraSnapshot* cameraSnapshot, Background* background)
{
    return new SimpleSceneSnapshot(simpleBodiesArray, lightsArray, background, cameraSnapshot);
}

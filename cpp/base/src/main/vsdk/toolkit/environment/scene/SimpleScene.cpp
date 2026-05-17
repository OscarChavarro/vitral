#include "SimpleScene.h"

#include "SimpleBody.h"
#include "SimpleSceneSnapshot.h"
#include "vsdk/toolkit/environment/background/Background.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/light/Light.h"

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
    for (size_t i = 0; i < simpleBodiesArray.size(); ++i) {
        delete simpleBodiesArray[i];
    }
    for (size_t i = 0; i < lightsArray.size(); ++i) {
        delete lightsArray[i];
    }
    for (size_t i = 0; i < backgroundsArray.size(); ++i) {
        delete backgroundsArray[i];
    }
    for (size_t i = 0; i < camerasArray.size(); ++i) {
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

void SimpleScene::addBody(SimpleBody* b) { simpleBodiesArray.push_back(b); }
void SimpleScene::addCamera(vsdk::toolkit::environment::camera::Camera* c) { camerasArray.push_back(c); }
void SimpleScene::addBackground(Background* b) { backgroundsArray.push_back(b); }
void SimpleScene::addLight(Light* l) { l->setId(static_cast<int>(lightsArray.size())); lightsArray.push_back(l); }

std::vector<SimpleBody*>& SimpleScene::getSimpleBodies() { return simpleBodiesArray; }
std::vector<Light*>& SimpleScene::getLights() { return lightsArray; }
std::vector<Background*>& SimpleScene::getBackgrounds() { return backgroundsArray; }
std::vector<vsdk::toolkit::environment::camera::Camera*>& SimpleScene::getCameras() { return camerasArray; }

void SimpleScene::setSimpleBodies(const std::vector<SimpleBody*>& simpleBodies)
{
    for (size_t i = 0; i < simpleBodiesArray.size(); ++i) {
        delete simpleBodiesArray[i];
    }
    simpleBodiesArray = simpleBodies;
}
void SimpleScene::setLights(const std::vector<Light*>& lights)
{
    for (size_t i = 0; i < lightsArray.size(); ++i) {
        delete lightsArray[i];
    }
    lightsArray = lights;
    for (size_t i = 0; i < lightsArray.size(); ++i) {
        lightsArray[i]->setId(static_cast<int>(i));
    }
}
void SimpleScene::setBackgrounds(const std::vector<Background*>& backgrounds)
{
    for (size_t i = 0; i < backgroundsArray.size(); ++i) {
        delete backgroundsArray[i];
    }
    backgroundsArray = backgrounds;
}
void SimpleScene::setCameras(const std::vector<vsdk::toolkit::environment::camera::Camera*>& cameras)
{
    for (size_t i = 0; i < camerasArray.size(); ++i) {
        delete camerasArray[i];
    }
    camerasArray = cameras;
}

Background* SimpleScene::getActiveBackground() const { return backgroundsArray[static_cast<size_t>(activeBackgroundIndex)]; }
vsdk::toolkit::environment::camera::Camera* SimpleScene::getActiveCamera() const { return camerasArray[static_cast<size_t>(activeCameraIndex)]; }

SimpleSceneSnapshot* SimpleScene::exportToSimpleSceneSnapshot()
{
    return exportToSimpleSceneSnapshot(getActiveCamera()->exportToCameraSnapshot(), getActiveBackground());
}

SimpleSceneSnapshot* SimpleScene::exportToSimpleSceneSnapshot(int viewportXSize, int viewportYSize)
{
    return exportToSimpleSceneSnapshot(getActiveCamera()->exportToCameraSnapshot(viewportXSize, viewportYSize), getActiveBackground());
}

SimpleSceneSnapshot* SimpleScene::exportToSimpleSceneSnapshot(vsdk::toolkit::environment::camera::CameraSnapshot* cameraSnapshot, Background* background)
{
    return new SimpleSceneSnapshot(simpleBodiesArray, lightsArray, background, cameraSnapshot);
}

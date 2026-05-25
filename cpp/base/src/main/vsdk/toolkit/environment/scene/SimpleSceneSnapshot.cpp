#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"

SimpleSceneSnapshot::SimpleSceneSnapshot(
    const std::vector<SimpleBody*>& inSimpleBodies,
    const std::vector<Light*>& inLights,
    Background* inBackground,
    CameraSnapshot* inCameraSnapshot)
    : simpleBodies(inSimpleBodies), lights(inLights), background(inBackground), cameraSnapshot(inCameraSnapshot)
{
}

SimpleSceneSnapshot::~SimpleSceneSnapshot()
{
    if ( cameraSnapshot != 0 ) {
        delete cameraSnapshot;
        cameraSnapshot = 0;
    }
}

const std::vector<SimpleBody*>& SimpleSceneSnapshot::getSimpleBodies() const { return simpleBodies; }
const std::vector<Light*>& SimpleSceneSnapshot::getLights() const { return lights; }
Background* SimpleSceneSnapshot::getBackground() const { return background; }
CameraSnapshot* SimpleSceneSnapshot::getCameraSnapshot() const { return cameraSnapshot; }

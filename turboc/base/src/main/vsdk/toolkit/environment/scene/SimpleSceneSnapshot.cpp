#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
SimpleSceneSnapshot::SimpleSceneSnapshot(
    ArrayList<SimpleBody*>& inSimpleBodies,
    ArrayList<Light*>& inLights,
    Background* inBackground,
    CameraSnapshot* inCameraSnapshot)
    : background(inBackground), cameraSnapshot(inCameraSnapshot)
{
    for (long int i = 0; i < inSimpleBodies.size(); i++) simpleBodies.add(inSimpleBodies.get(i));
    for (long int i = 0; i < inLights.size(); i++) lights.add(inLights.get(i));
}

SimpleSceneSnapshot::~SimpleSceneSnapshot()
{
    if ( cameraSnapshot != 0 ) {
        delete cameraSnapshot;
        cameraSnapshot = 0;
    }
}

ArrayList<SimpleBody*>& SimpleSceneSnapshot::getSimpleBodies() { return simpleBodies; }
ArrayList<Light*>& SimpleSceneSnapshot::getLights() { return lights; }
Background* SimpleSceneSnapshot::getBackground() const { return background; }
CameraSnapshot* SimpleSceneSnapshot::getCameraSnapshot() const { return cameraSnapshot; }

const Class SimpleSceneSnapshot::CLASS = { "SimpleSceneSnapshot", &Entity::CLASS };

const Class *
SimpleSceneSnapshot::getClass() const
{
    return &CLASS;
}

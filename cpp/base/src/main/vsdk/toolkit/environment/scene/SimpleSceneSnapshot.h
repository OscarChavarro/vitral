#ifndef __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENESNAPSHOT_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENESNAPSHOT_H__

#include "java/util/ArrayList.h"
class SimpleBody;
class Light;
class Background;
class CameraSnapshot;

class SimpleSceneSnapshot {
private:
    java::ArrayList<SimpleBody*> simpleBodies;
    java::ArrayList<Light*> lights;
    Background* background;
    CameraSnapshot* cameraSnapshot;

public:
    SimpleSceneSnapshot(
        java::ArrayList<SimpleBody*>& simpleBodies,
        java::ArrayList<Light*>& lights,
        Background* background,
        CameraSnapshot* cameraSnapshot);

    ~SimpleSceneSnapshot();

    java::ArrayList<SimpleBody*>& getSimpleBodies();
    java::ArrayList<Light*>& getLights();
    Background* getBackground() const;
    CameraSnapshot* getCameraSnapshot() const;
};

#endif

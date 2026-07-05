#ifndef __SIMPLESCENESNAPSHOT__
#define __SIMPLESCENESNAPSHOT__

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

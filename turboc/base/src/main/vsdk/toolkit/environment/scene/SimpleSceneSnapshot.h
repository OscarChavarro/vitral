#ifndef __SIMPLE_SCENE_SNAPSHOT__
#define __SIMPLE_SCENE_SNAPSHOT__

#include "vsdk/toolkit/common/Entity.h"
#include "java/util/ArrayList.h"
class SimpleBody;
class Light;
class Background;
class CameraSnapshot;

class SimpleSceneSnapshot : public Entity {
private:
    ArrayList<SimpleBody*> simpleBodies;
    ArrayList<Light*> lights;
    Background* background;
    CameraSnapshot* cameraSnapshot;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    SimpleSceneSnapshot(
        ArrayList<SimpleBody*>& simpleBodies,
        ArrayList<Light*>& lights,
        Background* background,
        CameraSnapshot* cameraSnapshot);

    ~SimpleSceneSnapshot();

    ArrayList<SimpleBody*>& getSimpleBodies();
    ArrayList<Light*>& getLights();
    Background* getBackground() const;
    CameraSnapshot* getCameraSnapshot() const;
};

#endif

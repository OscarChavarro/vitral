#ifndef __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENESNAPSHOT_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENESNAPSHOT_H__

#include <vector>

class SimpleBody;
class Light;
class Background;
class CameraSnapshot;

class SimpleSceneSnapshot {
private:
    std::vector<SimpleBody*> simpleBodies;
    std::vector<Light*> lights;
    Background* background;
    CameraSnapshot* cameraSnapshot;

public:
    SimpleSceneSnapshot(
        const std::vector<SimpleBody*>& simpleBodies,
        const std::vector<Light*>& lights,
        Background* background,
        CameraSnapshot* cameraSnapshot);

    ~SimpleSceneSnapshot();

    const std::vector<SimpleBody*>& getSimpleBodies() const;
    const std::vector<Light*>& getLights() const;
    Background* getBackground() const;
    CameraSnapshot* getCameraSnapshot() const;
};

#endif

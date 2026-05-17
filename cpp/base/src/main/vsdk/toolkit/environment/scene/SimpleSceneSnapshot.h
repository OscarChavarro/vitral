#ifndef __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENESNAPSHOT_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENESNAPSHOT_H__

#include <vector>

class SimpleBody;
class Light;
class Background;
namespace vsdk { namespace toolkit { namespace environment { namespace camera { class CameraSnapshot; }}}}

class SimpleSceneSnapshot {
private:
    std::vector<SimpleBody*> simpleBodies;
    std::vector<Light*> lights;
    Background* background;
    vsdk::toolkit::environment::camera::CameraSnapshot* cameraSnapshot;

public:
    SimpleSceneSnapshot(
        const std::vector<SimpleBody*>& simpleBodies,
        const std::vector<Light*>& lights,
        Background* background,
        vsdk::toolkit::environment::camera::CameraSnapshot* cameraSnapshot);

    ~SimpleSceneSnapshot();

    const std::vector<SimpleBody*>& getSimpleBodies() const;
    const std::vector<Light*>& getLights() const;
    Background* getBackground() const;
    vsdk::toolkit::environment::camera::CameraSnapshot* getCameraSnapshot() const;
};

#endif

#ifndef __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENE_H__

#include <vector>

class SimpleBody;
class Light;
class Background;
class Camera;
class CameraSnapshot;
class SimpleSceneSnapshot;

class SimpleScene {
private:
    std::vector<SimpleBody*> simpleBodiesArray;
    std::vector<Light*> lightsArray;
    std::vector<Background*> backgroundsArray;
    std::vector<Camera*> camerasArray;
    int activeCameraIndex;
    int activeBackgroundIndex;

public:
    SimpleScene();
    virtual ~SimpleScene();

    void clearOwnedElements();

    int getActiveCameraIndex() const;
    int getActiveBackgroundIndex() const;
    void setActiveCameraIndex(int i);
    void setActiveBackgroundIndex(int i);

    void addBody(SimpleBody* b);
    void addCamera(Camera* c);
    void addBackground(Background* b);
    void addLight(Light* l);

    std::vector<SimpleBody*>& getSimpleBodies();
    std::vector<Light*>& getLights();
    std::vector<Background*>& getBackgrounds();
    std::vector<Camera*>& getCameras();

    void setSimpleBodies(const std::vector<SimpleBody*>& simpleBodies);
    void setLights(const std::vector<Light*>& lights);
    void setBackgrounds(const std::vector<Background*>& backgrounds);
    void setCameras(const std::vector<Camera*>& cameras);

    Background* getActiveBackground() const;
    Camera* getActiveCamera() const;

    SimpleSceneSnapshot* exportToSimpleSceneSnapshot();
    SimpleSceneSnapshot* exportToSimpleSceneSnapshot(int viewportXSize, int viewportYSize);
    SimpleSceneSnapshot* exportToSimpleSceneSnapshot(CameraSnapshot* cameraSnapshot, Background* background);
};

#endif

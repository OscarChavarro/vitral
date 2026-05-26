#ifndef __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLESCENE_H__

#include "java/util/ArrayList.h"

class SimpleBody;
class Light;
class Background;
class Camera;
class CameraSnapshot;
class SimpleSceneSnapshot;

class SimpleScene {
private:
    java::ArrayList<SimpleBody*> simpleBodiesArray;
    java::ArrayList<Light*> lightsArray;
    java::ArrayList<Background*> backgroundsArray;
    java::ArrayList<Camera*> camerasArray;
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

    java::ArrayList<SimpleBody*>& getSimpleBodies();
    java::ArrayList<Light*>& getLights();
    java::ArrayList<Background*>& getBackgrounds();
    java::ArrayList<Camera*>& getCameras();

    void setSimpleBodies(java::ArrayList<SimpleBody*>& simpleBodies);
    void setLights(java::ArrayList<Light*>& lights);
    void setBackgrounds(java::ArrayList<Background*>& backgrounds);
    void setCameras(java::ArrayList<Camera*>& cameras);

    Background* getActiveBackground() const;
    Camera* getActiveCamera() const;

    SimpleSceneSnapshot* exportToSimpleSceneSnapshot();
    SimpleSceneSnapshot* exportToSimpleSceneSnapshot(int viewportXSize, int viewportYSize);
    SimpleSceneSnapshot* exportToSimpleSceneSnapshot(CameraSnapshot* cameraSnapshot, Background* background);
};

#endif

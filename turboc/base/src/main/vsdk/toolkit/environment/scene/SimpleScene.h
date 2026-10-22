#ifndef __SIMPLE_SCENE__
#define __SIMPLE_SCENE__

#include "vsdk/toolkit/common/Entity.h"
#include "java/util/ArrayList.h"
class SimpleBody;
class Light;
class Background;
class Camera;
class CameraSnapshot;
class SimpleSceneSnapshot;

class SimpleScene : public Entity {
private:
    ArrayList<SimpleBody*> simpleBodies;
    ArrayList<Light*> lights;
    ArrayList<Background*> backgrounds;
    ArrayList<Camera*> cameras;
    int activeCameraIndex;
    int activeBackgroundIndex;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

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

    ArrayList<SimpleBody*>& getSimpleBodies();
    ArrayList<Light*>& getLights();
    ArrayList<Background*>& getBackgrounds();
    ArrayList<Camera*>& getCameras();

    void setSimpleBodies(ArrayList<SimpleBody*>& simpleBodies);
    void setLights(ArrayList<Light*>& lights);
    void setBackgrounds(ArrayList<Background*>& backgrounds);
    void setCameras(ArrayList<Camera*>& cameras);

    Background* getActiveBackground() const;
    Camera* getActiveCamera() const;

    SimpleSceneSnapshot* exportToSimpleSceneSnapshot();
    SimpleSceneSnapshot* exportToSimpleSceneSnapshot(int viewportXSize, int viewportYSize);
    SimpleSceneSnapshot* exportToSimpleSceneSnapshot(CameraSnapshot* cameraSnapshot, Background* background);
};

#endif

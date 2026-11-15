#ifndef __SIMPLE_SCENE__
#define __SIMPLE_SCENE__

#include "vsdk/toolkit/common/Entity.h"
#include "java/util/ArrayList.h"
class SimpleBody;
class Light;
class Background;
class Camera;
class CameraSnapshot;
class Image;
class NormalMap;
class SimpleSceneSnapshot;

class SimpleScene : public Entity {
private:
    ArrayList<SimpleBody*> simpleBodies;
    ArrayList<Light*> lights;
    ArrayList<Background*> backgrounds;
    ArrayList<Camera*> cameras;
    // Resources shared by reference among bodies and backgrounds.
    ArrayList<Image*> images;
    ArrayList<NormalMap*> normalMaps;
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
    /**
    Takes ownership of an image that bodies (`SimpleBody::setTextureReference`)
    or backgrounds reference; it is deleted with the scene's other elements.
    */
    void addImage(Image* image);
    /**
    Takes ownership of a normal map that bodies reference
    (`SimpleBody::setNormalMapReference`).
    */
    void addNormalMap(NormalMap* normalMap);
    const ArrayList<Image*>& getImages() const { return images; }
    const ArrayList<NormalMap*>& getNormalMaps() const { return normalMaps; }

    ArrayList<SimpleBody*>& getSimpleBodies();
    const ArrayList<SimpleBody*>& getSimpleBodies() const { return simpleBodies; }
    ArrayList<Light*>& getLights();
    const ArrayList<Light*>& getLights() const { return lights; }
    ArrayList<Background*>& getBackgrounds();
    ArrayList<Camera*>& getCameras();

    void setSimpleBodies(ArrayList<SimpleBody*>& simpleBodies);
    void setLights(ArrayList<Light*>& lights);
    void setBackgrounds(ArrayList<Background*>& backgrounds);
    void setCameras(ArrayList<Camera*>& cameras);

    Background* getActiveBackground() const;
    Camera* getActiveCamera() const;

    virtual SimpleSceneSnapshot* exportToSimpleSceneSnapshot();
    virtual SimpleSceneSnapshot* exportToSimpleSceneSnapshot(int viewportXSize, int viewportYSize);
    SimpleSceneSnapshot* exportToSimpleSceneSnapshot(CameraSnapshot* cameraSnapshot, Background* background);
};

#endif

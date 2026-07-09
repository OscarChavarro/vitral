#ifndef __MESH_MODEL__
#define __MESH_MODEL__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"

class MeshModel {
private:
    Camera camera;
    java::ArrayList<Light*> lights;
    SimpleScene scene;
    RendererConfiguration qualitySelection;
    RayGizmo rayGizmo;
    java::String tangibleServiceUrl;

    Intersection* makeIntersectionCallback(const Ray& ray);

public:
    MeshModel();
    ~MeshModel();

    Camera* getCamera();
    java::ArrayList<Light*>& getLights();
    SimpleScene* getScene();
    RendererConfiguration* getQualitySelection();
    RayGizmo* getRayGizmo();
    const java::String& getTangibleServiceUrl() const;
    void setTangibleServiceUrl(const java::String& tangibleServiceUrl);
    void configureInitialViewAndLightToScene();
};

#endif

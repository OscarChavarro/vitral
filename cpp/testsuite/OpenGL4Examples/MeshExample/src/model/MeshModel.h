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

    static const Vector3Dd INITIAL_RAY_ORIGIN;
    static const double INITIAL_RAY_YAW_IN_DEGREES;
    static const double INITIAL_RAY_PITCH_IN_DEGREES;

    Intersection* makeIntersectionCallback(const Ray& ray);
    static Vector3Dd initialRayDirection();

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

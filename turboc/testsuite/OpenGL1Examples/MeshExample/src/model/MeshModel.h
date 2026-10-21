#ifndef __MESH_MODEL__
#define __MESH_MODEL__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "java/util/function/Function.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"

class MeshModel {
private:
    /**
    Intersects the rays of the ray gizmo with the scene (a lambda in the
    C++11 port).
    */
    class IntersectionCallback : public Function<const Ray&, Intersection*> {
    public:
        explicit IntersectionCallback(MeshModel* model) : model(model) {}
        virtual Intersection* apply(const Ray& ray)
        {
            return model->makeIntersectionCallback(ray);
        }
    private:
        MeshModel* model;
    };
    friend class IntersectionCallback;

    Camera camera;
    ArrayList<Light*> lights;
    SimpleScene scene;
    RendererConfiguration qualitySelection;
    // Must be built before the ray gizmo that uses it
    IntersectionCallback intersectionCallback;
    RayGizmo rayGizmo;
    String tangibleServiceUrl;

    static const Vector3Dd INITIAL_RAY_ORIGIN;
    static const double INITIAL_RAY_YAW_IN_DEGREES;
    static const double INITIAL_RAY_PITCH_IN_DEGREES;

    Intersection* makeIntersectionCallback(const Ray& ray);
    static Vector3Dd initialRayDirection();

public:
    MeshModel();
    ~MeshModel();

    Camera* getCamera();
    ArrayList<Light*>& getLights();
    SimpleScene* getScene();
    RendererConfiguration* getQualitySelection();
    RayGizmo* getRayGizmo();
    const String& getTangibleServiceUrl() const;
    void setTangibleServiceUrl(const String& tangibleServiceUrl);
    void configureInitialViewAndLightToScene();
};

#endif

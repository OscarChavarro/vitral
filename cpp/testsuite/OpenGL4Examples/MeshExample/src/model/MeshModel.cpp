#include <cmath>

#include <java/lang/Math.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "model/MeshModel.h"

MeshModel::MeshModel()
    : rayGizmo([this](const Ray& ray) { return this->makeIntersectionCallback(ray); }, 1),
      tangibleServiceUrl("ws://localhost:8090/v1/values")
{
    Light* light0 = new PointLight(Vector3Dd(10, -20, 50), ColorRgb(1, 1, 1));
    light0->setId(0);
    Light* light1 = new PointLight(Vector3Dd(-10, 20, 50), ColorRgb(1, 1, 1));
    light1->setId(1);
    lights.add(light0);
    lights.add(light1);
}

MeshModel::~MeshModel()
{
    for ( long i = 0; i < lights.size(); i++ ) {
        delete lights.get(i);
    }
    lights.clear();
}

Camera* MeshModel::getCamera()
{
    return &camera;
}

java::ArrayList<Light*>& MeshModel::getLights()
{
    return lights;
}

SimpleScene* MeshModel::getScene()
{
    return &scene;
}

RendererConfiguration* MeshModel::getQualitySelection()
{
    return &qualitySelection;
}

RayGizmo* MeshModel::getRayGizmo()
{
    return &rayGizmo;
}

const java::String& MeshModel::getTangibleServiceUrl() const
{
    return tangibleServiceUrl;
}

void MeshModel::setTangibleServiceUrl(const java::String& tangibleServiceUrl)
{
    if ( tangibleServiceUrl.empty() ) {
        return;
    }
    this->tangibleServiceUrl = tangibleServiceUrl;
}

Intersection* MeshModel::makeIntersectionCallback(const Ray& ray)
{
    Intersection* closest = 0;
    double closestT = 1e308;
    java::ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        SimpleBody* body = bodies.get(i);
        RayHit hit(RayHit::DETAIL_POINT | RayHit::DETAIL_NORMAL);
        if ( body != 0 && body->doIntersectionFirstHit(ray, &hit) && hit.hasHitDistance() ) {
            double t = hit.hitDistance();
            if ( t > 1e-6 && t < closestT ) {
                closestT = t;
                delete closest;
                closest = new Intersection(t, hit.p, hit.n);
            }
        }
    }
    return closest;
}

void MeshModel::configureInitialViewAndLightToScene()
{
    java::ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    if ( bodies.size() == 0 ) {
        return;
    }

    SimpleBodyGroup group;
    java::ArrayList<SimpleBody*>& groupBodies = group.getBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        groupBodies.add(bodies.get(i));
    }
    double* minmax = group.getMinMax();
    if ( minmax == 0 ) {
        return;
    }

    Vector3Dd min(minmax[0], minmax[1], minmax[2]);
    Vector3Dd max(minmax[3], minmax[4], minmax[5]);
    delete[] minmax;

    Vector3Dd center = min.add(max).multiply(0.5);
    double radius = max.subtract(min).length() * 0.5;
    if ( radius < 0.001 ) {
        radius = 1.0;
    }

    double fovRad = camera.getFov() * M_PI / 180.0;
    double viewDistance = (radius / std::tan(fovRad * 0.5)) * 1.35;
    if ( viewDistance < radius * 1.5 ) {
        viewDistance = radius * 1.5;
    }

    Vector3Dd eyeDirection = Vector3Dd(0, -1, 0.35).normalized();
    Vector3Dd eye = center.add(eyeDirection.multiply(viewDistance));
    camera.setPosition(eye);
    camera.setUpMaintainingOrthogonality(Vector3Dd(0, 0, 1));
    camera.setFocusedPositionMaintainingOrthogonality(center);

    double nearPlane = java::Math::max(0.01, viewDistance - (radius * 2.2));
    double farPlane = java::Math::max(nearPlane + 1.0, viewDistance + (radius * 4.0));
    camera.setNearPlaneDistance(nearPlane);
    camera.setFarPlaneDistance(farPlane);
    camera.updateVectors();

    Vector3Dd lightDirection = Vector3Dd(1, -1, 1).normalized();
    Vector3Dd lightPos0 = center.add(lightDirection.multiply(radius * 3.0));
    Vector3Dd lightPos1 = center.add(Vector3Dd(-lightDirection.x(), -lightDirection.y(), lightDirection.z())
        .normalized()
        .multiply(radius * 3.0));

    if ( lights.size() > 0 && lights.get(0) != 0 ) {
        lights.get(0)->setPosition(lightPos0);
    }
    if ( lights.size() > 1 && lights.get(1) != 0 ) {
        lights.get(1)->setPosition(lightPos1);
    }
}

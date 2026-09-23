#include <cmath>

#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/gui/gizmo/LightGizmoOmniBillboard.h"
#include "model/selection/LightPicker.h"

double LightPicker::calculatePickRadius(const Camera* camera,
                                        const Light* light, double gizmoScale)
{
    return LightGizmoOmniBillboard::calculateWorldHalfSize(camera,
        light->getPosition(), (int)camera->getViewportXSize(),
        (int)camera->getViewportYSize()) * gizmoScale;
}

double LightPicker::intersectSphere(const Ray& ray, const Vector3Dd& center,
                                    double radius)
{
    Vector3Dd toCenter = center.subtract(ray.getOrigin());
    double projection = toCenter.dotProduct(ray.getDirection());
    double discriminant = radius * radius
        - (toCenter.dotProduct(toCenter) - projection * projection);

    if ( discriminant < 0 ) {
        return -1;
    }
    double t = projection - std::sqrt(discriminant);

    if ( t >= 0 ) {
        return t;
    }
    // Origin inside the sphere or sphere behind the ray
    return projection + std::sqrt(discriminant) >= 0 ? 0 : -1;
}

double LightPicker::pick(const Ray& ray, const Camera* camera,
                         const Light* light, double gizmoScale)
{
    return intersectSphere(ray, light->getPosition(),
        calculatePickRadius(camera, light, gizmoScale));
}

#ifndef __LIGHT_PICKER__
#define __LIGHT_PICKER__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Camera;
class Light;
class Ray;

/**
Picks lights with a viewport ray. A light has no geometry, so it is picked
with a sphere centered at its position, whose radius is the size of its
gizmo in world units. As the gizmo keeps a constant apparent size in the
viewport, that sphere projects, at any distance and for any kind of camera,
over the area of the viewport covered by the gizmo. This is a plain model
class: it knows nothing about the GUI or rendering technology.
*/
class LightPicker {
private:
    LightPicker() {}

public:
    /**
    @param camera camera of the viewport being picked, with the size of the
    viewport already set
    @param light light to pick
    @param gizmoScale scale applied to the gizmo when drawing it
    @return the radius of the pick sphere, in world units
    */
    static double calculatePickRadius(const Camera* camera,
                                      const Light* light, double gizmoScale);

    /**
    Intersects a ray with a sphere.
    @param ray ray to test, its direction is assumed to be a unit vector
    @param center center of the sphere
    @param radius radius of the sphere
    @return the distance from the ray origin to the first point of the
    sphere in front of the origin (or to the ray origin if it is inside the
    sphere), or -1 if the sphere is not hit
    */
    static double intersectSphere(const Ray& ray, const Vector3Dd& center,
                                  double radius);

    /**
    @param ray ray fired from the camera through the picked pixel
    @param camera camera of the viewport being picked
    @param light light to pick
    @param gizmoScale scale applied to the gizmo when drawing it
    @return the distance along the ray to the light, or -1 if it is not hit
    */
    static double pick(const Ray& ray, const Camera* camera,
                       const Light* light, double gizmoScale);
};

#endif

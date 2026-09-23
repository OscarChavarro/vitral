#include <cmath>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "model/ApplicationModel.h"
#include "model/Scene.h"
#include "render/VisualRayDebugGeometry.h"

VisualRayDebugGeometry::VisualRayDebugGeometry(ApplicationModel* model)
    : model(model), scene(model->getScene())
{
    rendererConfiguration = new RendererConfiguration();
    rendererConfiguration->setShadingType(
        RendererConfiguration::SHADING_TYPE_GOURAUD);
    visualDebugMaterial = Scene::defaultMaterial();
}

VisualRayDebugGeometry::~VisualRayDebugGeometry()
{
    delete rendererConfiguration;
}

RendererConfiguration* VisualRayDebugGeometry::getRendererConfiguration() const
{
    return rendererConfiguration;
}

java::ArrayList<RenderPrimitive> VisualRayDebugGeometry::buildPrimitives() const
{
    java::ArrayList<RenderPrimitive> primitives;

    if ( model->isWithVisualDebugRay() && model->getVisualDebugRay() != nullptr ) {
        addRay(primitives, *model->getVisualDebugRay(),
               model->getVisualDebugRayLevels());
    }
    return primitives;
}

void VisualRayDebugGeometry::addSegment(
    java::ArrayList<RenderPrimitive>& primitives, const Vector3Dd& start,
    const Vector3Dd& end, bool follow, double w, double tip,
    const SimpleMaterial& segmentMaterial) const
{
    double l;
    Vector3Dd diff = end.subtract(start);
    l = diff.length();

    //-----------------------------------------------------------------
    std::shared_ptr<Geometry> a;

    if ( l > tip ) {
        a = std::shared_ptr<Geometry>(new Arrow(l - tip, tip, w/2, w));
    }
    else {
        a = std::shared_ptr<Geometry>(new Cone(w/2, w/2, l));
    }
    Matrix4x4d R;
    double yaw;
    double pitch;
    yaw = diff.obtainSphericalThetaAngle();
    pitch = diff.obtainSphericalPhiAngle();
    R = R.eulerAnglesRotation(M_PI + yaw, pitch, 0);

    primitives.add(RenderPrimitive(a,
        Matrix4x4d().translation(start).multiply(R), segmentMaterial));

    //-----------------------------------------------------------------
    if ( follow ) {
        std::shared_ptr<Geometry> s(new Sphere(0.025));
        Vector3Dd p;
        double offset = 0.1;
        int i;
        diff = diff.normalized();
        for ( i = 0; i < 3; i++, offset += 0.1 ) {
            p = end.add(diff.multiply(offset));
            primitives.add(RenderPrimitive(s,
                Matrix4x4d().translation(p), segmentMaterial));
        }
    }
}

void VisualRayDebugGeometry::addRay(java::ArrayList<RenderPrimitive>& primitives,
                                    const Ray& ray, int level) const
{
    if ( level < 0 ) {
        return;
    }

    //-----------------------------------------------------------------
    Vector3Dd p;
    Vector3Dd d = ray.getDirection();
    d = d.normalized();
    RayHit info;

    //-----------------------------------------------------------------
    SimpleMaterial rayOriginMaterial =
        visualDebugMaterial.withDiffuse(ColorRgb(0.9, 0.5, 0.0));
    primitives.add(RenderPrimitive(std::shared_ptr<Geometry>(new Sphere(0.05)),
        Matrix4x4d().translation(ray.getOrigin()), rayOriginMaterial));

    //-----------------------------------------------------------------
    if ( scene->doIntersectionFirstHit(ray, &info) ) {
        d = d.multiply(ray.getT());
        p = ray.getOrigin().add(d);

        addSegment(primitives, ray.getOrigin(), p, false, 0.07, 0.4,
                   rayOriginMaterial);
        if ( level >= 1 ) {
            // Draw normal
            SimpleMaterial normalMaterial =
                visualDebugMaterial.withDiffuse(ColorRgb(0.9, 0.9, 0.5));
            addSegment(primitives, p, p.add(info.normal.multiply(0.5)), false,
                       0.05, 0.2, normalMaterial);
        }
        // Reflection ray
        Vector3Dd dd = ray.getDirection().multiply(-1);
        dd = dd.normalized();
        Vector3Dd h = info.normal.multiply(dd.dotProduct(info.normal)).subtract(dd);
        Ray subray(p, dd.add(h.multiply(2)));
        subray = subray.withOrigin(
            subray.getOrigin().add(
                subray.getDirection().multiply(VSDK::EPSILON*10.0)));
        addRay(primitives, subray, level-1);
    }
    else {
        d = d.multiply(1.4);
        p = ray.getOrigin().add(d);
        addSegment(primitives, ray.getOrigin(), p, true, 0.07, 0.4,
                   rayOriginMaterial);
    }
}

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Intersection.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RayGizmoRenderer.h"

void OpenGL4RayGizmoRenderer::draw(RayGizmo* gizmo, Camera* camera, const java::ArrayList<Light*>&)
{
    if ( gizmo == 0 || camera == 0 || !gizmo->isVisible() ) {
        return;
    }

    RayGizmo::RaySnapshot* snapshot = gizmo->getCurrentSnapshot();
    java::ArrayList<float> positions;
    java::ArrayList<float> colors;
    double arrowTotalLength = 4.0;

    if ( snapshot == 0 || snapshot->rays.size() == 0 ) {
        Ray ray(gizmo->getPosition(), gizmo->getDirection());
        Vector3Dd end = ray.getOrigin().add(ray.getDirection().multiply(arrowTotalLength));
        addLine(positions, colors, ray.getOrigin(), end, gizmo->getSourceRayColor());
    }
    else {
        java::ArrayList<ColorRgb>& normalColors = gizmo->getNormalRayColors();
        java::ArrayList<ColorRgb>& reflectedColors = gizmo->getReflectedRayColors();

        for ( long i = 0; i < snapshot->rays.size(); i++ ) {
            Ray ray = snapshot->rays.get(i);
            Intersection* intersection = snapshot->intersections.get(i);
            ColorRgb rayColor = i == 0 ?
                gizmo->getSourceRayColor() :
                reflectedColors.get((i - 1) % reflectedColors.size());

            Vector3Dd end = ray.getOrigin().add(ray.getDirection().multiply(arrowTotalLength));
            if ( intersection != 0 && intersection->t > 1e-6 ) {
                end = ray.getOrigin().add(ray.getDirection().multiply(intersection->t));
            }
            addLine(positions, colors, ray.getOrigin(), end, rayColor);

            if ( intersection == 0 ) {
                double factors[] = { 1.25, 1.50, 1.75 };
                Vector3Dd dir = ray.getDirection();
                if ( dir.length() >= VSDK::EPSILON ) {
                    dir = dir.normalized();
                    for ( int j = 0; j < 3; j++ ) {
                        addDot(positions, colors,
                            ray.getOrigin().add(dir.multiply(arrowTotalLength * factors[j])),
                            ColorRgb(1.0, 1.0, 0.0), 0.12);
                    }
                }
            }
            else {
                ColorRgb normalColor = normalColors.get(i % normalColors.size());
                addLine(positions, colors,
                    intersection->point,
                    intersection->point.add(intersection->normal.normalized().multiply(arrowTotalLength * 0.5)),
                    normalColor);
            }
        }
    }

    OpenGL4LineRenderer::drawLines(camera->calculateProjectionMatrix(), positions, colors, 3.0f);
}

void OpenGL4RayGizmoRenderer::dispose()
{
}

void OpenGL4RayGizmoRenderer::addLine(java::ArrayList<float>& positions, java::ArrayList<float>& colors,
    const Vector3Dd& a, const Vector3Dd& b, const ColorRgb& color)
{
    positions.add((float)a.x());
    positions.add((float)a.y());
    positions.add((float)a.z());
    colors.add((float)color.r());
    colors.add((float)color.g());
    colors.add((float)color.b());

    positions.add((float)b.x());
    positions.add((float)b.y());
    positions.add((float)b.z());
    colors.add((float)color.r());
    colors.add((float)color.g());
    colors.add((float)color.b());
}

void OpenGL4RayGizmoRenderer::addDot(java::ArrayList<float>& positions, java::ArrayList<float>& colors,
    const Vector3Dd& center, const ColorRgb& color, double radius)
{
    addLine(positions, colors,
        center.add(Vector3Dd(-radius, 0, 0)),
        center.add(Vector3Dd(radius, 0, 0)), color);
    addLine(positions, colors,
        center.add(Vector3Dd(0, -radius, 0)),
        center.add(Vector3Dd(0, radius, 0)), color);
    addLine(positions, colors,
        center.add(Vector3Dd(0, 0, -radius)),
        center.add(Vector3Dd(0, 0, radius)), color);
}

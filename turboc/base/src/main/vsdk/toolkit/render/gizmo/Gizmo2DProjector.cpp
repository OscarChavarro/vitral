#include "vsdk/toolkit/common/VSDK.h"
#include <math.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DProjector.h"

enum { NUMBER_OF_CLIP_PLANES = 6 };
static const double CLIP_PLANES[NUMBER_OF_CLIP_PLANES][4] = {
    { 1.0, 0.0, 0.0, 1.0 },
    { -1.0, 0.0, 0.0, 1.0 },
    { 0.0, 1.0, 0.0, 1.0 },
    { 0.0, -1.0, 0.0, 1.0 },
    { 0.0, 0.0, 1.0, 1.0 },
    { 0.0, 0.0, -1.0, 1.0 }
};

Gizmo2DProjector::Gizmo2DProjector(const Camera* camera,
                                   Gizmo2DDrawList* gizmo2DDrawList)
    : projection(camera->calculateProjectionMatrix()),
      width(camera->getViewportXSize()),
      height(camera->getViewportYSize()),
      gizmo2DDrawList(gizmo2DDrawList)
{
}

Gizmo2DDrawList* Gizmo2DProjector::getGizmo2DDrawList() const
{
    return gizmo2DDrawList;
}

//= Polygons ==============================================================

void Gizmo2DProjector::addPolygon(const ArrayList<Vector3Dd>& vertices,
    const ColorRgb& color, double opacity)
{
    ArrayList<Vector4Dd> polygon;
    long i;
    int p;

    polygon.reserve(vertices.size() + 6);
    for ( i = 0; i < vertices.size(); i++ ) {
        polygon.add(projection.multiply(Vector4Dd(vertices[i])));
    }
    for ( p = 0; p < NUMBER_OF_CLIP_PLANES; p++ ) {
        polygon = clip(polygon, CLIP_PLANES[p]);
        if ( polygon.size() < 3 ) {
            return;
        }
    }

    long n = polygon.size();
    ArrayList<double> x;
    ArrayList<double> y;
    double depth = 0;

    x.reserve(n);
    y.reserve(n);
    for ( i = 0; i < n; i++ ) {
        Vector4Dd ndc = polygon[i].dividedByW();
        x.add(toPixelX(ndc.x()));
        y.add(toPixelY(ndc.y()));
        depth += ndc.z();
    }
    gizmo2DDrawList->addPolygon(x, y, depth / n, color, opacity);
}

void Gizmo2DProjector::addStrip(const ArrayList<Vector3Dd>& strip,
    const ColorRgb& color, double opacity)
{
    long n = strip.size();
    long i;

    for ( i = 0; i + 3 < n; i += 2 ) {
        ArrayList<Vector3Dd> quad;
        quad.add(strip[i]);
        quad.add(strip[i + 1]);
        quad.add(strip[i + 3]);
        quad.add(strip[i + 2]);
        addPolygon(quad, color, opacity);
    }
    if ( n % 2 == 1 && n >= 3 ) {
        long last = n - 1;
        ArrayList<Vector3Dd> triangle;
        triangle.add(strip[last - 2]);
        triangle.add(strip[last - 1]);
        triangle.add(strip[last]);
        addPolygon(triangle, color, opacity);
    }
}

void Gizmo2DProjector::addFanTriangles(const ArrayList<Vector3Dd>& fan,
    const ColorRgb& color, double opacity)
{
    long i;

    for ( i = 1; i + 1 < fan.size(); i++ ) {
        ArrayList<Vector3Dd> triangle;
        triangle.add(fan[0]);
        triangle.add(fan[i]);
        triangle.add(fan[i + 1]);
        addPolygon(triangle, color, opacity);
    }
}

void Gizmo2DProjector::addFanAsPolygon(const ArrayList<Vector3Dd>& fan,
    const ColorRgb& color, double opacity)
{
    if ( fan.size() >= 3 ) {
        addPolygon(fan, color, opacity);
    }
}

void Gizmo2DProjector::addTriangles(const ArrayList<Vector3Dd>& triangles,
    const ColorRgb& color, double opacity)
{
    long i;

    for ( i = 0; i + 2 < triangles.size(); i += 3 ) {
        ArrayList<Vector3Dd> triangle;
        triangle.add(triangles[i]);
        triangle.add(triangles[i + 1]);
        triangle.add(triangles[i + 2]);
        addPolygon(triangle, color, opacity);
    }
}

//= Lines and points ======================================================

void Gizmo2DProjector::addSegment(const Vector3Dd& p0, const Vector3Dd& p1,
    const ColorRgb& color, double lineWidth, double depthBias)
{
    Vector4Dd start = projection.multiply(Vector4Dd(p0));
    Vector4Dd end = projection.multiply(Vector4Dd(p1));
    int i;

    for ( i = 0; i < NUMBER_OF_CLIP_PLANES; i++ ) {
        double d0 = evaluate(CLIP_PLANES[i], start);
        double d1 = evaluate(CLIP_PLANES[i], end);

        if ( d0 < 0.0 && d1 < 0.0 ) {
            return;
        }
        if ( d0 < 0.0 || d1 < 0.0 ) {
            double denominator = d0 - d1;
            if ( fabs(denominator) < VSDK::EPSILON ) {
                return;
            }
            Vector4Dd intersection = interpolate(start, end, d0 / denominator);
            if ( d0 < 0.0 ) {
                start = intersection;
            }
            else {
                end = intersection;
            }
        }
    }
    Vector4Dd a = start.dividedByW();
    Vector4Dd b = end.dividedByW();
    gizmo2DDrawList->addLine(toPixelX(a.x()), toPixelY(a.y()), toPixelX(b.x()),
        toPixelY(b.y()), (a.z() + b.z()) / 2 + depthBias, color, lineWidth);
}

BOOLEAN Gizmo2DProjector::projectPoint(const Vector3Dd& p,
                                       double outPixel[2]) const
{
    Vector4Dd clipped = projection.multiply(Vector4Dd(p));

    if ( clipped.w() <= VSDK::EPSILON || clipped.z() < -clipped.w() ||
         clipped.z() > clipped.w() ) {
        return FALSE;
    }
    Vector4Dd ndc = clipped.dividedByW();
    outPixel[0] = toPixelX(ndc.x());
    outPixel[1] = toPixelY(ndc.y());
    return TRUE;
}

//=========================================================================

double Gizmo2DProjector::toPixelX(double ndcX) const
{
    return (ndcX + 1.0) / 2.0 * width;
}

double Gizmo2DProjector::toPixelY(double ndcY) const
{
    return (1.0 - ndcY) / 2.0 * height;
}

/**
Sutherland-Hodgman clipping of a polygon against one plane of the
homogeneous view volume.
*/
ArrayList<Vector4Dd> Gizmo2DProjector::clip(
    const ArrayList<Vector4Dd>& polygon, const double plane[4])
{
    ArrayList<Vector4Dd> result;
    long n = polygon.size();
    long i;

    result.reserve(n + 2);
    for ( i = 0; i < n; i++ ) {
        const Vector4Dd& current = polygon[i];
        const Vector4Dd& next = polygon[(i + 1) % n];
        double dc = evaluate(plane, current);
        double dn = evaluate(plane, next);

        if ( dc >= 0.0 ) {
            result.add(current);
        }
        if ( (dc >= 0.0) != (dn >= 0.0) ) {
            result.add(interpolate(current, next, dc / (dc - dn)));
        }
    }
    return result;
}

Vector4Dd Gizmo2DProjector::interpolate(const Vector4Dd& a,
                                        const Vector4Dd& b, double t)
{
    return a.multiply(1.0 - t).add(b.multiply(t));
}

double Gizmo2DProjector::evaluate(const double plane[4],
                                  const Vector4Dd& point)
{
    return plane[0] * point.x() + plane[1] * point.y() +
        plane[2] * point.z() + plane[3] * point.w();
}

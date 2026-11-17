#include <cmath>

#include "render/ClipSpaceLineProjector.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"

namespace {
const double CLIP_PLANES[6][4] = {
    { 1.0, 0.0, 0.0, 1.0 },
    { -1.0, 0.0, 0.0, 1.0 },
    { 0.0, 1.0, 0.0, 1.0 },
    { 0.0, -1.0, 0.0, 1.0 },
    { 0.0, 0.0, 1.0, 1.0 },
    { 0.0, 0.0, -1.0, 1.0 }
};

double evaluate(const double plane[4], const Vector4Dd& point)
{
    return plane[0] * point.x() + plane[1] * point.y() +
        plane[2] * point.z() + plane[3] * point.w();
}
}

ClipSpaceLineProjector::ClipSpaceLineProjector(const Camera* camera)
    : projection(camera->calculateProjectionMatrix())
{
}

bool ClipSpaceLineProjector::addSegment(Calligraphic2DBuffer* out,
    const Vector3Dd& p0, const Vector3Dd& p1) const
{
    Vector4Dd start = projection.multiply(Vector4Dd(p0));
    Vector4Dd end = projection.multiply(Vector4Dd(p1));

    for ( int i = 0; i < 6; i++ ) {
        double d0 = evaluate(CLIP_PLANES[i], start);
        double d1 = evaluate(CLIP_PLANES[i], end);

        if ( d0 < 0.0 && d1 < 0.0 ) {
            return false;
        }
        if ( d0 < 0.0 || d1 < 0.0 ) {
            double denominator = d0 - d1;
            if ( std::fabs(denominator) < VSDK::EPSILON ) {
                return false;
            }
            double t = d0 / denominator;
            Vector4Dd intersection =
                start.multiply(1.0 - t).add(end.multiply(t));
            if ( d0 < 0.0 ) {
                start = intersection;
            }
            else {
                end = intersection;
            }
        }
    }

    Vector4Dd ndc0 = start.dividedByW();
    Vector4Dd ndc1 = end.dividedByW();
    out->add2DLine(ndc0.x(), ndc0.y(), ndc1.x(), ndc1.y());
    return true;
}

bool ClipSpaceLineProjector::addSegment(Calligraphic2DBuffer* out,
    const Matrix4x4d& transform, const Vector3Dd& p0,
    const Vector3Dd& p1) const
{
    return addSegment(out, transform.multiply(p0), transform.multiply(p1));
}

bool ClipSpaceLineProjector::addPoint(Calligraphic2DBuffer* out,
                                      const Vector3Dd& p) const
{
    Vector4Dd clip = projection.multiply(Vector4Dd(p));

    for ( int i = 0; i < 6; i++ ) {
        if ( evaluate(CLIP_PLANES[i], clip) < 0.0 ) {
            return false;
        }
    }
    Vector4Dd ndc = clip.dividedByW();
    out->add2DLine(ndc.x(), ndc.y(), ndc.x(), ndc.y());
    return true;
}

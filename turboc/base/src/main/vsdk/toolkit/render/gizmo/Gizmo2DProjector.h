#ifndef __GIZMO_2D_PROJECTOR__
#define __GIZMO_2D_PROJECTOR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"

class Camera;
class Gizmo2DDrawList;

/**
Geometric stage of the 2D gizmo pipeline [FOLE1992].6: takes the world
space primitives the gizmos build for the OpenGL renderers (triangle strips,
fans, segments, see `GizmoSolidTessellator`), transforms them with the
projection matrix of a camera, clips them in homogeneous coordinates against
the six planes `-w <= x, y, z <= w` (Sutherland-Hodgman for polygons) and
maps the result to the pixels of the viewport of the camera, adding them to
a `Gizmo2DDrawList`.

Pixels have the origin at the upper left corner of the viewport and grow
downwards; the viewport size is the one of the camera (see
`Camera::updateViewportResize`).
*/
class Gizmo2DProjector {
private:
    Matrix4x4d projection;
    double width;
    double height;
    Gizmo2DDrawList* gizmo2DDrawList;

    double toPixelX(double ndcX) const;
    double toPixelY(double ndcY) const;
    static ArrayList<Vector4Dd> clip(const ArrayList<Vector4Dd>& polygon,
                                     const double plane[4]);
    static Vector4Dd interpolate(const Vector4Dd& a, const Vector4Dd& b,
                                 double t);
    static double evaluate(const double plane[4], const Vector4Dd& point);

public:
    /**
    @param camera camera viewing the gizmo, with its viewport size updated
    @param gizmo2DDrawList list receiving the projected primitives
    */
    Gizmo2DProjector(const Camera* camera, Gizmo2DDrawList* gizmo2DDrawList);

    Gizmo2DDrawList* getGizmo2DDrawList() const;

    //= Polygons ==========================================================

    /**
    Adds a simple polygon (convex or not), given by its vertices in order.
    */
    void addPolygon(const ArrayList<Vector3Dd>& vertices,
                    const ColorRgb& color, double opacity);

    /**
    Adds a triangle strip, as one quad for each pair of consecutive edges (a
    strip of 4 vertices is one quad), which are drawn with fewer seams than
    its triangles.
    */
    void addStrip(const ArrayList<Vector3Dd>& strip, const ColorRgb& color,
                  double opacity);

    /**
    Adds a triangle fan as its triangles (the first vertex is the center or
    apex of the fan).
    */
    void addFanTriangles(const ArrayList<Vector3Dd>& fan,
                         const ColorRgb& color, double opacity);

    /**
    Adds a planar triangle fan as one polygon (its center followed by its
    perimeter), so a translucent fan is not darker along its inner edges.
    */
    void addFanAsPolygon(const ArrayList<Vector3Dd>& fan,
                         const ColorRgb& color, double opacity);

    /**
    Adds a list of independent triangles (3 vertices each).
    */
    void addTriangles(const ArrayList<Vector3Dd>& triangles,
                      const ColorRgb& color, double opacity);

    //= Lines and points ==================================================

    /**
    Adds a segment.
    @param depthBias added to its depth (negative is towards the viewer)
    */
    void addSegment(const Vector3Dd& p0, const Vector3Dd& p1,
                    const ColorRgb& color, double lineWidth,
                    double depthBias);

    /**
    @param p point in world space
    @param outPixel its pixel (x, y) in the viewport (it may be outside the
    sides of the viewport)
    @return false if it is behind the camera or outside of the depth range
    of the view volume (the Java version returns null)
    */
    BOOLEAN projectPoint(const Vector3Dd& p, double outPixel[2]) const;
};

#endif

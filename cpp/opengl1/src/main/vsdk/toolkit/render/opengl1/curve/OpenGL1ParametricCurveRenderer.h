#ifndef __OPEN_GL_1_PARAMETRIC_CURVE_RENDERER__
#define __OPEN_GL_1_PARAMETRIC_CURVE_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Camera;
class ParametricCurve;
class RendererConfiguration;

/**
Renders a `ParametricCurve` with the OpenGL 1.2 pipeline. As in
`Jogl2ParametricCurveRenderer`, each segment of the curve (segments ending at
a `BREAK` point are skipped) is approximated by the polyline given by
`ParametricCurve::calculatePoints` and drawn, unlit, with the wire color of
the configuration, whatever its surface and wire bits are (a curve has no
surface). The points bit shows the vertices of the polylines, and the
bounding volume and selection corners bits are honored as for any geometry.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4ParametricCurveRenderer`, mirroring
`OpenGL1ParametricCurveRenderer`.
*/
class OpenGL1ParametricCurveRenderer {
public:
    /**
    Draws the curve with the wire color of the configuration.

    @param curve curve to draw
    @param camera camera that views the curve
    @param quality bits of rendering configuration
    @param localTransform transformation from curve space to world space
    */
    static void draw(ParametricCurve* curve, Camera* camera,
        const RendererConfiguration* quality, const Matrix4x4d& localTransform);

    /**
    Draws the curve.

    @param curve curve to draw
    @param camera camera that views the curve
    @param quality bits of rendering configuration
    @param localTransform transformation from curve space to world space
    @param color color of the lines
    */
    static void draw(ParametricCurve* curve, Camera* camera,
        const RendererConfiguration* quality, const Matrix4x4d& localTransform,
        const ColorRgb& color);

private:
    static const float POINT_SIZE;
    static const float POINT_RGB[3];

    OpenGL1ParametricCurveRenderer();

    /**
    @param outPositions x,y,z of the ends of the line segments of the
    polylines of the segments of the curve
    */
    static void buildSegmentPositions(const ParametricCurve& curve,
        java::ArrayList<Vector3Dd>& vertices,
        java::ArrayList<float>& outPositions);

    /**
    Draws the vertices of the polylines as small crosses, as the points of
    `Jogl2ParametricCurveRenderer` are, sized in world units relative to the
    curve.
    */
    static void drawPoints(const Matrix4x4d& mvp,
        const java::ArrayList<Vector3Dd>& vertices);

    static void boundsOf(const java::ArrayList<Vector3Dd>& vertices,
        double outMinMax[6]);

    static void buildUniformColors(long vertexCount, float r, float g,
        float b, java::ArrayList<float>& outColors);
};

#endif

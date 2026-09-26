#ifndef __OPEN_GL_1_LINE_RENDERER__
#define __OPEN_GL_1_LINE_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
class OpenGL1LineRenderer {
public:
    static void drawLines(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        float lineWidth);

    static void drawLines(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        float lineWidth,
        float depthBiasNdc);

    static void release();

private:
    static double evaluateClipPlane(const double plane[4], const Vector4Dd& point);
    static Vector4Dd interpolate(const Vector4Dd& start, const Vector4Dd& end, double t);
    static bool clipLineToClipVolume(
        const Vector4Dd& start,
        const Vector4Dd& end,
        Vector4Dd& outStart,
        Vector4Dd& outEnd);
    static void addVertex(
        java::ArrayList<float>& positions,
        java::ArrayList<float>& colors,
        const float point[3],
        const float color[3]);
    static void drawThinLines(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        float lineWidth,
        float depthBiasNdc);
    static void drawPrimitives(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        unsigned int primitiveType,
        float depthBiasNdc,
        float lineWidth);
    static void buildThickLineMesh(
        const Matrix4x4d& modelViewProjection,
        const java::ArrayList<float>& positions,
        const java::ArrayList<float>& colors,
        float lineWidth,
        java::ArrayList<float>& trianglePositions,
        java::ArrayList<float>& triangleColors);
};

#endif

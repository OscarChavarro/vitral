#include "java/util/ArrayList.txx"
#include <glad/gl.h>
#include <algorithm>
#include <limits>

#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl4/curve/OpenGL4ParametricCurveRenderer.h"

const float OpenGL4ParametricCurveRenderer::POINT_SIZE = 4.0f;
const float OpenGL4ParametricCurveRenderer::POINT_RGB[3] = { 1.0f, 0.0f, 0.0f };

void OpenGL4ParametricCurveRenderer::draw(ParametricCurve* curve,
    Camera* camera, const RendererConfiguration* quality,
    const Matrix4x4d& localTransform)
{
    // C++ port note: the wire color of a configuration is never null
    ColorRgb color = quality != nullptr ?
        quality->getWireColor() : ColorRgb(1, 1, 1);
    draw(curve, camera, quality, localTransform, color);
}

void OpenGL4ParametricCurveRenderer::draw(ParametricCurve* curve,
    Camera* camera, const RendererConfiguration* quality,
    const Matrix4x4d& localTransform, const ColorRgb& color)
{
    if ( curve == nullptr || camera == nullptr || quality == nullptr ||
         curve->getPointSize() < 2 ) {
        return;
    }
    const Matrix4x4d& local = localTransform;
    Matrix4x4d mvp = camera->calculateProjectionMatrix().multiply(local);
    java::ArrayList<Vector3Dd> vertices;
    java::ArrayList<float> positions;
    buildSegmentPositions(*curve, vertices, positions);

    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    if ( positions.size() > 0 ) {
        java::ArrayList<float> colors;
        buildUniformColors(positions.size() / 3, (float)color.r(),
            (float)color.g(), (float)color.b(), colors);
        OpenGL4LineRenderer::drawLines(mvp, positions, colors, 1.0f);
    }
    if ( quality->isPointsSet() && vertices.size() > 0 ) {
        drawPoints(mvp, vertices);
    }
    if ( quality->isBoundingVolumeSet() ) {
        OpenGL4MinMaxRenderer::draw(curve, camera, local);
    }
    if ( quality->isSelectionCornersSet() ) {
        OpenGL4SelectionCornersRenderer::draw(curve, camera, local);
    }
    glDepthFunc(GL_LESS);
}

void OpenGL4ParametricCurveRenderer::buildSegmentPositions(
    const ParametricCurve& curve, java::ArrayList<Vector3Dd>& vertices,
    java::ArrayList<float>& outPositions)
{
    java::ArrayList<Vector3Dd> ends;
    for ( int i = 1; i < curve.getPointSize(); i++ ) {
        if ( curve.getPointType(i) == ParametricCurve::BREAK ) {
            i++;
            continue;
        }
        java::ArrayList<Vector3Dd> polyline = curve.calculatePoints(i, false);
        for ( long j = 0; j + 1 < polyline.size(); j++ ) {
            ends.add(polyline[j]);
            ends.add(polyline[j + 1]);
        }
        for ( long j = 0; j < polyline.size(); j++ ) {
            vertices.add(polyline[j]);
        }
    }
    for ( long k = 0; k < ends.size(); k++ ) {
        outPositions.add((float)ends[k].x());
        outPositions.add((float)ends[k].y());
        outPositions.add((float)ends[k].z());
    }
}

void OpenGL4ParametricCurveRenderer::drawPoints(const Matrix4x4d& mvp,
    const java::ArrayList<Vector3Dd>& vertices)
{
    double minmax[6];
    boundsOf(vertices, minmax);
    double size = std::max(minmax[3] - minmax[0],
        std::max(minmax[4] - minmax[1], minmax[5] - minmax[2]));
    float h = (float)(std::max(size, 1e-6) * 0.01);
    java::ArrayList<float> positions;
    for ( long i = 0; i < vertices.size(); i++ ) {
        float x = (float)vertices[i].x();
        float y = (float)vertices[i].y();
        float z = (float)vertices[i].z();
        float axes[3][3] = { { h, 0, 0 }, { 0, h, 0 }, { 0, 0, h } };
        for ( int a = 0; a < 3; a++ ) {
            positions.add(x - axes[a][0]);
            positions.add(y - axes[a][1]);
            positions.add(z - axes[a][2]);
            positions.add(x + axes[a][0]);
            positions.add(y + axes[a][1]);
            positions.add(z + axes[a][2]);
        }
    }
    java::ArrayList<float> colors;
    buildUniformColors(positions.size() / 3, POINT_RGB[0], POINT_RGB[1],
        POINT_RGB[2], colors);
    OpenGL4LineRenderer::drawLines(mvp, positions, colors,
        std::max(1.0f, POINT_SIZE / 2));
}

void OpenGL4ParametricCurveRenderer::boundsOf(
    const java::ArrayList<Vector3Dd>& vertices, double outMinMax[6])
{
    for ( int k = 0; k < 3; k++ ) {
        outMinMax[k] = std::numeric_limits<double>::max();
        outMinMax[k + 3] = -std::numeric_limits<double>::max();
    }
    for ( long i = 0; i < vertices.size(); i++ ) {
        const Vector3Dd& v = vertices[i];
        outMinMax[0] = std::min(outMinMax[0], v.x());
        outMinMax[1] = std::min(outMinMax[1], v.y());
        outMinMax[2] = std::min(outMinMax[2], v.z());
        outMinMax[3] = std::max(outMinMax[3], v.x());
        outMinMax[4] = std::max(outMinMax[4], v.y());
        outMinMax[5] = std::max(outMinMax[5], v.z());
    }
}

void OpenGL4ParametricCurveRenderer::buildUniformColors(long vertexCount,
    float r, float g, float b, java::ArrayList<float>& outColors)
{
    for ( long i = 0; i < vertexCount; i++ ) {
        outColors.add(r);
        outColors.add(g);
        outColors.add(b);
    }
}

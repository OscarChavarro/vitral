#include <cmath>
#include <cstdio>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MatrixState.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LineRenderer.h"
const double CLIP_PLANES[][4] = {
    { 1.0, 0.0, 0.0, 1.0 },
    { -1.0, 0.0, 0.0, 1.0 },
    { 0.0, 1.0, 0.0, 1.0 },
    { 0.0, -1.0, 0.0, 1.0 },
    { 0.0, 0.0, 1.0, 1.0 },
    { 0.0, 0.0, -1.0, 1.0 }
};

double OpenGL1LineRenderer::evaluateClipPlane(const double plane[4], const Vector4Dd& point)
{
    return plane[0] * point.x() + plane[1] * point.y() +
        plane[2] * point.z() + plane[3] * point.w();
}

Vector4Dd OpenGL1LineRenderer::interpolate(const Vector4Dd& start, const Vector4Dd& end, double t)
{
    return start.multiply(1.0 - t).add(end.multiply(t));
}

bool OpenGL1LineRenderer::clipLineToClipVolume(
    const Vector4Dd& start,
    const Vector4Dd& end,
    Vector4Dd& outStart,
    Vector4Dd& outEnd)
{
    outStart = start;
    outEnd = end;

    for ( int i = 0; i < 6; i++ ) {
        double d0 = evaluateClipPlane(CLIP_PLANES[i], outStart);
        double d1 = evaluateClipPlane(CLIP_PLANES[i], outEnd);
        if ( d0 < 0.0 && d1 < 0.0 ) {
            return false;
        }
        if ( d0 < 0.0 || d1 < 0.0 ) {
            double denominator = d0 - d1;
            if ( std::abs(denominator) < 1.0e-12 ) {
                return false;
            }
            double t = d0 / denominator;
            Vector4Dd intersection = interpolate(outStart, outEnd, t);
            if ( d0 < 0.0 ) {
                outStart = intersection;
            }
            else {
                outEnd = intersection;
            }
        }
    }
    return true;
}

void OpenGL1LineRenderer::addVertex(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    const float point[3],
    const float color[3])
{
    positions.add(point[0]);
    positions.add(point[1]);
    positions.add(point[2]);
    colors.add(color[0]);
    colors.add(color[1]);
    colors.add(color[2]);
}

void OpenGL1LineRenderer::drawLines(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    float lineWidth)
{
    drawLines(modelViewProjection, positions, colors, lineWidth, 0.0f);
}

void OpenGL1LineRenderer::drawLines(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    float lineWidth,
    float depthBiasNdc)
{
    if ( positions.size() == 0 || colors.size() == 0 ) {
        return;
    }
    if ( positions.size() != colors.size() ) {
        fprintf(stderr, "OpenGL1LineRenderer positions/colors length mismatch\n");
        return;
    }

    if ( lineWidth <= 1.0f ) {
        drawThinLines(modelViewProjection, positions, colors, lineWidth,
            depthBiasNdc);
        return;
    }

    java::ArrayList<float> trianglePositions;
    java::ArrayList<float> triangleColors;
    buildThickLineMesh(modelViewProjection, positions, colors, lineWidth,
        trianglePositions, triangleColors);
    if ( trianglePositions.size() == 0 ) {
        return;
    }

    drawPrimitives(Matrix4x4d::identityMatrix(), trianglePositions,
        triangleColors, GL_TRIANGLES, depthBiasNdc, 1.0f);
}

void OpenGL1LineRenderer::drawThinLines(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    float lineWidth,
    float depthBiasNdc)
{
    drawPrimitives(modelViewProjection, positions, colors, GL_LINES,
        depthBiasNdc, lineWidth);
}

void OpenGL1LineRenderer::drawPrimitives(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    unsigned int primitiveType,
    float depthBiasNdc,
    float lineWidth)
{
    if ( positions.size() == 0 || colors.size() == 0 ) {
        return;
    }
    glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT);
    glDisable(GL_LIGHTING);
    glDisable(GL_TEXTURE_2D);
    OpenGL1MatrixState::push(
        OpenGL1MatrixState::depthBias(depthBiasNdc).multiply(modelViewProjection));

    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, positions.data());
    glColorPointer(3, GL_FLOAT, 0, colors.data());

    if ( primitiveType == GL_LINES ) {
        glLineWidth(lineWidth);
    }
    glDrawArrays(primitiveType, 0, (GLsizei)(positions.size() / 3));

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
    OpenGL1MatrixState::pop();
    glPopAttrib();
}

void OpenGL1LineRenderer::buildThickLineMesh(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    float lineWidth,
    java::ArrayList<float>& trianglePositions,
    java::ArrayList<float>& triangleColors)
{
    int viewport[4] = { 0, 0, 1, 1 };
    glGetIntegerv(GL_VIEWPORT, viewport);
    double viewportWidth = viewport[2] > 0 ? viewport[2] : 1.0;
    double viewportHeight = viewport[3] > 0 ? viewport[3] : 1.0;
    double halfWidth = lineWidth / 2.0;

    for ( long int i = 0; i + 5 < positions.size(); i += 6 ) {
        Vector4Dd clip0 = modelViewProjection.multiply(Vector4Dd(
            positions.get(i), positions.get(i + 1), positions.get(i + 2), 1.0));
        Vector4Dd clip1 = modelViewProjection.multiply(Vector4Dd(
            positions.get(i + 3), positions.get(i + 4), positions.get(i + 5), 1.0));
        Vector4Dd clipped0(0.0, 0.0, 0.0, 1.0);
        Vector4Dd clipped1(0.0, 0.0, 0.0, 1.0);
        if ( !clipLineToClipVolume(clip0, clip1, clipped0, clipped1) ) {
            continue;
        }

        Vector4Dd ndc0 = clipped0.dividedByW();
        Vector4Dd ndc1 = clipped1.dividedByW();
        double dxPixels = (ndc1.x() - ndc0.x()) * viewportWidth / 2.0;
        double dyPixels = (ndc1.y() - ndc0.y()) * viewportHeight / 2.0;
        double lengthPixels = std::sqrt(dxPixels * dxPixels + dyPixels * dyPixels);
        if ( lengthPixels <= 1.0e-9 ) {
            continue;
        }

        double perpX = -dyPixels / lengthPixels;
        double perpY = dxPixels / lengthPixels;
        double offsetNdcX = perpX * halfWidth * 2.0 / viewportWidth;
        double offsetNdcY = perpY * halfWidth * 2.0 / viewportHeight;

        float p0Plus[3] = {
            (float)(ndc0.x() + offsetNdcX),
            (float)(ndc0.y() + offsetNdcY),
            (float)ndc0.z()
        };
        float p0Minus[3] = {
            (float)(ndc0.x() - offsetNdcX),
            (float)(ndc0.y() - offsetNdcY),
            (float)ndc0.z()
        };
        float p1Plus[3] = {
            (float)(ndc1.x() + offsetNdcX),
            (float)(ndc1.y() + offsetNdcY),
            (float)ndc1.z()
        };
        float p1Minus[3] = {
            (float)(ndc1.x() - offsetNdcX),
            (float)(ndc1.y() - offsetNdcY),
            (float)ndc1.z()
        };

        float c0[3] = {
            colors.get(i), colors.get(i + 1), colors.get(i + 2)
        };
        float c1[3] = {
            colors.get(i + 3), colors.get(i + 4), colors.get(i + 5)
        };

        addVertex(trianglePositions, triangleColors, p0Plus, c0);
        addVertex(trianglePositions, triangleColors, p0Minus, c0);
        addVertex(trianglePositions, triangleColors, p1Plus, c1);

        addVertex(trianglePositions, triangleColors, p1Plus, c1);
        addVertex(trianglePositions, triangleColors, p0Minus, c0);
        addVertex(trianglePositions, triangleColors, p1Minus, c1);
    }
}

void OpenGL1LineRenderer::release()
{
}

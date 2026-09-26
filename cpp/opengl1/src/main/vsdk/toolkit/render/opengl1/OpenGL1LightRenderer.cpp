#include <cmath>

#include <java/lang/Math.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/gui/gizmo/LightGizmoOmniBillboard.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LightRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LineRenderer.h"
double OpenGL1LightRenderer::scale = 1.0;

double OpenGL1LightRenderer::calculateHalfAxisLength(const Light* light, Camera* camera, int viewportWidth, int viewportHeight)
{
    const double viewportFraction = 0.05;
    const double targetPixels = viewportFraction * (double)java::Math::min(viewportWidth, viewportHeight);

    if (camera == nullptr) {
        return java::Math::max(0.05, targetPixels / (double)java::Math::max(viewportHeight, 1));
    }

    if (camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL) {
        double worldViewHeight = 2.0 / camera->getOrthogonalZoom();
        double worldPerPixel = worldViewHeight / (double)java::Math::max(viewportHeight, 1);
        return java::Math::max(1e-5, 0.5 * targetPixels * worldPerPixel);
    }

    Vector3Dd toLight = light->getPosition().subtract(camera->getPosition());
    double depth = std::abs(toLight.dotProduct(camera->getFront()));
    depth = java::Math::max(depth, camera->getNearPlaneDistance());

    double fovRadians = camera->getFov() * M_PI / 180.0;
    double worldViewHeightAtDepth = 2.0 * depth * std::tan(fovRadians / 2.0);
    double worldPerPixel = worldViewHeightAtDepth / (double)java::Math::max(viewportHeight, 1);
    return java::Math::max(1e-5, 0.5 * targetPixels * worldPerPixel);
}

Vector3Dd OpenGL1LightRenderer::mapPatternPointToWorld(
    const Vector3Dd& point,
    const Vector3Dd& center,
    const Vector3Dd& right,
    const Vector3Dd& up,
    double worldSize)
{
    double localX = (point.x() - 0.5) * worldSize;
    double localY = (point.y() - 0.5) * worldSize;
    return center.add(right.multiply(localX)).add(up.multiply(localY));
}

void OpenGL1LightRenderer::drawLines(const Matrix4x4d& mvp,
                                     const java::ArrayList<float>& positions,
                                     const java::ArrayList<float>& colors,
                                     float lineWidth)
{
    glDisable(GL_CULL_FACE);
    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_FALSE);
    glDepthFunc(GL_LEQUAL);
    OpenGL1LineRenderer::drawLines(mvp, positions, colors, lineWidth);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
}

void OpenGL1LightRenderer::draw(const Light* light)
{
    draw(light, nullptr, LightGizmoStyle::CROSS);
}

void OpenGL1LightRenderer::draw(const Light* light, Camera* camera)
{
    draw(light, camera, LightGizmoStyle::CROSS);
}

void OpenGL1LightRenderer::draw(const Light* light, Camera* camera, LightGizmoStyle lightGizmoStyle)
{
    draw(light, camera, lightGizmoStyle, false);
}

void OpenGL1LightRenderer::draw(const Light* light, Camera* camera,
                                LightGizmoStyle lightGizmoStyle, bool selected)
{
    if (light == nullptr) return;

    if (lightGizmoStyle == LightGizmoStyle::OMNI_BILLBOARD) {
        drawOmniBillboard(light, camera, selected);
        return;
    }

    drawCross(light, camera, selected);
}

void OpenGL1LightRenderer::drawCross(const Light* light, Camera* camera,
                                     bool selected)
{
    int viewport[4] = {0, 0, 1, 1};
    glGetIntegerv(GL_VIEWPORT, viewport);

    int viewportWidth = java::Math::max(viewport[2], 1);
    int viewportHeight = java::Math::max(viewport[3], 1);

    Matrix4x4d mvp = Matrix4x4d::identityMatrix();
    if (camera != nullptr) {
        camera->updateViewportResize(viewportWidth, viewportHeight);
        mvp = camera->calculateProjectionMatrix();
    }

    double halfAxisLength = calculateHalfAxisLength(light, camera, viewportWidth, viewportHeight) * scale;
    Vector3Dd p = light->getPosition();
    ColorRgb c = selected ? ColorRgb(1, 1, 0) : light->getEmission();

    float px = (float)p.x();
    float py = (float)p.y();
    float pz = (float)p.z();
    float d = (float)halfAxisLength;

    java::ArrayList<float> positions;
    positions.reserve(18);
    positions.add(px - d); positions.add(py); positions.add(pz);
    positions.add(px + d); positions.add(py); positions.add(pz);
    positions.add(px); positions.add(py - d); positions.add(pz);
    positions.add(px); positions.add(py + d); positions.add(pz);
    positions.add(px); positions.add(py); positions.add(pz - d);
    positions.add(px); positions.add(py); positions.add(pz + d);

    java::ArrayList<float> colors;
    colors.reserve(18);
    for (long int i = 0; i < positions.size() / 3; i++) {
        colors.add((float)c.r());
        colors.add((float)c.g());
        colors.add((float)c.b());
    }

    drawLines(mvp, positions, colors, selected ? 4.0f : 2.0f);
}

void OpenGL1LightRenderer::drawOmniBillboard(const Light* light, Camera* camera,
                                             bool selected)
{
    int viewport[4] = {0, 0, 1, 1};
    glGetIntegerv(GL_VIEWPORT, viewport);

    int viewportWidth = java::Math::max(viewport[2], 1);
    int viewportHeight = java::Math::max(viewport[3], 1);

    if (camera == nullptr) {
        drawCross(light, nullptr, selected);
        return;
    }

    camera->updateViewportResize(viewportWidth, viewportHeight);
    Matrix4x4d mvp = camera->calculateProjectionMatrix();

    double worldHalfSize = calculateHalfAxisLength(light, camera, viewportWidth, viewportHeight) * scale;
    double worldFullSize = 2.0 * worldHalfSize;

    Vector3Dd lightPosition = light->getPosition();
    Vector3Dd right = camera->getLeft().multiply(-1.0).normalized();
    Vector3Dd up = camera->getUp().normalized();

    Calligraphic2DBuffer pattern = LightGizmoOmniBillboard::createLinePattern();
    int lineCount = pattern.getNumLines();
    if (lineCount <= 0) return;

    java::ArrayList<float> positions;
    positions.reserve((long int)lineCount * 2 * 3);

    for (int i = 0; i < lineCount; i++) {
        Vector3Dd* p0Pattern = pattern.get2DLinePoint0(i);
        Vector3Dd* p1Pattern = pattern.get2DLinePoint1(i);
        if (p0Pattern == nullptr || p1Pattern == nullptr) {
            delete p0Pattern;
            delete p1Pattern;
            continue;
        }

        Vector3Dd p0 = mapPatternPointToWorld(*p0Pattern, lightPosition, right, up, worldFullSize);
        Vector3Dd p1 = mapPatternPointToWorld(*p1Pattern, lightPosition, right, up, worldFullSize);
        delete p0Pattern;
        delete p1Pattern;

        positions.add((float)p0.x());
        positions.add((float)p0.y());
        positions.add((float)p0.z());
        positions.add((float)p1.x());
        positions.add((float)p1.y());
        positions.add((float)p1.z());
    }

    ColorRgb c = selected ? ColorRgb(1, 1, 0) : light->getEmission();
    java::ArrayList<float> colors;
    colors.reserve(positions.size());
    for (long int i = 0; i < positions.size() / 3; i++) {
        colors.add((float)c.r());
        colors.add((float)c.g());
        colors.add((float)c.b());
    }

    drawLines(mvp, positions, colors, selected ? 4.0f : 2.0f);
}

double OpenGL1LightRenderer::getScale()
{
    return scale;
}

void OpenGL1LightRenderer::setScale(double newScale)
{
    scale = newScale;
}

void OpenGL1LightRenderer::dispose()
{
    OpenGL1LineRenderer::release();
}

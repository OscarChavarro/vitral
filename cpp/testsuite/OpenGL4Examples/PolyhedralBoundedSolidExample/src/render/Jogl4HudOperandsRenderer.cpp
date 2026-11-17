#include <cmath>

#include <glad/gl.h>

#include "render/Jogl4HudOperandsRenderer.h"

#include "java/lang/Math.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SimpleMaterialRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidRenderer.h"
#include "models/DebuggerModel.h"

static const double HUD_INSET_DEPTH = 2.8;

Jogl4HudOperandsRenderer::Jogl4HudOperandsRenderer(DebuggerModel* model)
    : model(model),
      csgOperandMaterialA(createInsetMaterial(1.0, 0.502, 0.502)),
      csgOperandMaterialB(createInsetMaterial(0.502, 1.0, 0.502))
{
}

SimpleMaterial Jogl4HudOperandsRenderer::createInsetMaterial(double r,
    double g, double b)
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.2 * r, 0.2 * g, 0.2 * b));
    m = m.withDiffuse(ColorRgb(r, g, b));
    m = m.withSpecular(ColorRgb(1.0, 1.0, 1.0));
    m = m.withDoubleSided(false);
    m = m.withPhongExponent(100);
    return m;
}

Vector3Dd Jogl4HudOperandsRenderer::solidCenter(PolyhedralBoundedSolid* solid)
{
    double* minMax;

    if ( solid == nullptr ) {
        return Vector3Dd(0, 0, 0);
    }
    minMax = solid->getMinMax();
    if ( minMax == nullptr ) {
        return Vector3Dd(0, 0, 0);
    }
    Vector3Dd center(
        (minMax[0] + minMax[3]) / 2.0,
        (minMax[1] + minMax[4]) / 2.0,
        (minMax[2] + minMax[5]) / 2.0);
    delete[] minMax;
    return center;
}

double Jogl4HudOperandsRenderer::solidMaxExtent(PolyhedralBoundedSolid* solid)
{
    double* minMax;
    double ex;
    double ey;
    double ez;

    if ( solid == nullptr ) {
        return 1.0;
    }
    minMax = solid->getMinMax();
    if ( minMax == nullptr ) {
        return 1.0;
    }
    ex = std::fabs(minMax[0] - minMax[3]);
    ey = std::fabs(minMax[1] - minMax[4]);
    ez = std::fabs(minMax[2] - minMax[5]);
    delete[] minMax;
    return java::Math::max(ex, java::Math::max(ey, ez));
}

Vector3Dd Jogl4HudOperandsRenderer::cameraRelativeAnchor(Camera* camera,
    double ndcX, double ndcY, double depth)
{
    Vector3Dd eye = camera->getPosition();
    Vector3Dd front = camera->getFront().normalized();
    Vector3Dd up = camera->getUp().normalized();
    Vector3Dd right = camera->getLeft().multiply(-1).normalized();
    double viewportY = java::Math::max(camera->getViewportYSize(), 1e-9);
    double aspect = camera->getViewportXSize() / viewportY;
    double offsetX;
    double offsetY;
    double safeDepth = java::Math::max(depth, 1e-9);

    if ( camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        double zoom = java::Math::max(camera->getOrthogonalZoom(), 1e-9);
        offsetX = ndcX * (aspect / zoom);
        offsetY = ndcY * (1.0 / zoom);
    }
    else {
        double halfHeight = safeDepth *
            std::tan(java::Math::toRadians(camera->getFov() / 2.0));
        double halfWidth = halfHeight * aspect;
        offsetX = ndcX * halfWidth;
        offsetY = ndcY * halfHeight;
    }

    return eye.add(front.multiply(safeDepth))
              .add(right.multiply(offsetX))
              .add(up.multiply(offsetY));
}

void Jogl4HudOperandsRenderer::drawInsetSolid(PolyhedralBoundedSolid* solid,
    const SimpleMaterial& material, const Vector3Dd& anchorPoint,
    double mainSolidExtent)
{
    Vector3Dd center;
    double extent;
    double scale;

    if ( solid == nullptr ) {
        return;
    }
    center = solidCenter(solid);
    extent = solidMaxExtent(solid);
    if ( extent < 1e-12 ) {
        extent = 1.0;
    }
    if ( mainSolidExtent < 1e-12 ) {
        mainSolidExtent = 1.0;
    }
    scale = 0.75 * (mainSolidExtent / extent);

    Matrix4x4d modelMatrix = Matrix4x4d()
        .translation(anchorPoint)
        .multiply(Matrix4x4d().scale(scale, scale, scale)
            .multiply(Matrix4x4d().translation(-center.x(), -center.y(),
                -center.z())));
    OpenGL4SimpleMaterialRenderer::activate(&material);
    OpenGL4PolyhedralBoundedSolidRenderer::draw(solid, model->getCamera(),
        model->getQuality(), modelMatrix);
}

void Jogl4HudOperandsRenderer::draw(int viewportWidth, int viewportHeight)
{
    PolyhedralBoundedSolid* operandA = model->getCsgPreviewOperandA();
    PolyhedralBoundedSolid* operandB = model->getCsgPreviewOperandB();
    PolyhedralBoundedSolid* mainSolid = model->getSolid();
    Camera* camera = model->getCamera();
    double mainExtent;
    Vector3Dd leftAnchor;
    Vector3Dd rightAnchor;

    if ( operandA == nullptr || operandB == nullptr || mainSolid == nullptr ) {
        return;
    }
    if ( viewportWidth <= 0 || viewportHeight <= 0 ) {
        return;
    }
    mainExtent = solidMaxExtent(mainSolid);

    leftAnchor = cameraRelativeAnchor(camera, -0.76, -0.5,
        HUD_INSET_DEPTH);
    rightAnchor = cameraRelativeAnchor(camera, 0.76, -0.5,
        HUD_INSET_DEPTH);

    drawInsetSolid(operandA, csgOperandMaterialA, leftAnchor, mainExtent);
    drawInsetSolid(operandB, csgOperandMaterialB, rightAnchor, mainExtent);
    OpenGL4SimpleMaterialRenderer::activate(model->getMaterial());
}

#include <cmath>
#include <cstdio>

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ArrowRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1CameraRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.h"
#include "vsdk/toolkit/render/opengl1/gizmo/OpenGL1RayGizmoRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1SphereRenderer.h"

namespace {
const float IND_OUTER_R = 0.65f;
const float IND_INNER_R = 0.17f;
const float IND_HALF_W = 0.12f;
const float IND_TIP_Z = 0.30f;
}


void OpenGL1RayGizmoRenderer::draw(RayGizmo* gizmo, Camera* camera, const java::ArrayList<Light*>& lights)
{
    if ( gizmo == 0 || camera == 0 || lights.size() == 0 || !gizmo->isVisible() ) {
        return;
    }
    RendererConfiguration quality = buildSurfaceQuality();
    Matrix4x4d primaryModelMatrix = gizmo->getBody()->getTransformationMatrix();
    Matrix4x4d projection = OpenGL1CameraRenderer::activate(camera);
    SimpleScene* scene = gizmo->buildScene();

    java::ArrayList<SimpleBody*>& bodies = scene->getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        SimpleBody* body = bodies.get(i);
        if ( body == 0 || body->getGeometry() == 0 || body->getMaterial() == 0 ) {
            continue;
        }

        Geometry* geom = body->getGeometry();
        Matrix4x4d modelMatrix = body->getTransformationMatrix();
        SimpleMaterial* material = body->getMaterial();

        Arrow* arrow = dynamic_cast<Arrow*>(geom);
        if ( arrow != 0 ) {
            OpenGL1ArrowRenderer::draw(arrow, modelMatrix, projection, camera, lights, material, &quality);
            continue;
        }

        Sphere* sphere = dynamic_cast<Sphere*>(geom);
        if ( sphere != 0 ) {
            OpenGL1SphereRenderer::draw(
                sphere, camera, lights.get(0), material, &quality,
                0, 0, modelMatrix, 16, 12);
        }
    }

    drawIndicator(gizmo->getRotationAngleInRadians(), primaryModelMatrix, projection, camera, lights, &quality);
    delete scene;

    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
}

void OpenGL1RayGizmoRenderer::dispose()
{
    OpenGL1ArrowRenderer::dispose();
    OpenGL1SphereRenderer::dispose();

}

void OpenGL1RayGizmoRenderer::drawIndicator(
    double rollAngleRadians,
    const Matrix4x4d& arrowModelMatrix,
    const Matrix4x4d& projection,
    const Camera* camera,
    const java::ArrayList<Light*>& lights,
    const RendererConfiguration* quality)
{
    Matrix4x4d rollRotation = Matrix4x4d().axisRotation(rollAngleRadians, 0, 0, 1);
    Matrix4x4d indicatorModelMatrix = arrowModelMatrix.multiply(rollRotation);
    SimpleMaterial material = indicatorMaterial();
    float normals[9];
    computeNormals(normals);

    // Gouraud shaded, as the gouraudTexture GLSL program used by the
    // OpenGL 4 port
    RendererConfiguration gouraud(*quality);
    gouraud.setShadingType(RendererConfiguration::SHADING_TYPE_GOURAUD);
    OpenGL1RendererConfigurationStateSelector::activateState(
        camera, projection, indicatorModelMatrix, lights, material,
        &gouraud, 0);

    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glDisable(GL_CULL_FACE);

    glBegin(GL_TRIANGLES);
        glNormal3fv(&normals[0]);
        glTexCoord2f(0.5f, 1.0f);
        glVertex3f(IND_OUTER_R, 0.0f, IND_TIP_Z);
        glNormal3fv(&normals[3]);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3f(IND_INNER_R, -IND_HALF_W, 0.0f);
        glNormal3fv(&normals[6]);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3f(IND_INNER_R, IND_HALF_W, 0.0f);
    glEnd();

    OpenGL1RendererConfigurationStateSelector::deactivateState();
}

RendererConfiguration OpenGL1RayGizmoRenderer::buildSurfaceQuality()
{
    RendererConfiguration quality;
    quality.setSurfaces(true);
    quality.setWires(false);
    quality.setPoints(false);
    quality.setTexture(false);
    quality.setBumpMap(false);
    return quality;
}

SimpleMaterial OpenGL1RayGizmoRenderer::indicatorMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.3, 0.3, 0.0));
    m = m.withDiffuse(ColorRgb(1.0, 0.9, 0.0));
    m = m.withSpecular(ColorRgb(1.0, 1.0, 0.8));
    m = m.withPhongExponent(64.0);
    return m;
}

void OpenGL1RayGizmoRenderer::computeNormals(float normals[9])
{
    float ax = IND_INNER_R - IND_OUTER_R;
    float ay = -IND_HALF_W;
    float az = -IND_TIP_Z;
    float bx = IND_INNER_R - IND_OUTER_R;
    float by = IND_HALF_W;
    float bz = -IND_TIP_Z;
    float nx = ay * bz - az * by;
    float ny = az * bx - ax * bz;
    float nz = ax * by - ay * bx;
    float len = std::sqrt(nx * nx + ny * ny + nz * nz);
    nx /= len;
    ny /= len;
    nz /= len;

    for ( int i = 0; i < 3; i++ ) {
        normals[i * 3] = nx;
        normals[i * 3 + 1] = ny;
        normals[i * 3 + 2] = nz;
    }
}

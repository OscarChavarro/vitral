#include <cstdio>

#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MatrixState.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.h"

bool OpenGL1RendererConfigurationStateSelector::tooManyLightsReported = false;
bool OpenGL1RendererConfigurationStateSelector::phongReported = false;
bool OpenGL1RendererConfigurationStateSelector::cookTorranceReported = false;
bool OpenGL1RendererConfigurationStateSelector::bumpMapReported = false;

namespace {

/// OpenGL 1.2 clamps the specular exponent to this value
const float MAX_SHININESS = 128.0f;

void setColor(GLenum face, GLenum parameter, const ColorRgb& c)
{
    float value[4] = { (float)c.r(), (float)c.g(), (float)c.b(), 1.0f };
    glMaterialfv(face, parameter, value);
}

}

bool OpenGL1RendererConfigurationStateSelector::reportUnsupportedFeatures(
    const RendererConfiguration* quality)
{
    if ( quality == nullptr ) {
        return false;
    }
    bool unsupported = false;
    int shadingType = quality->getShadingType();
    if ( shadingType == RendererConfiguration::SHADING_TYPE_PHONG ) {
        unsupported = true;
        if ( !phongReported ) {
            phongReported = true;
            fprintf(stderr, "OpenGL1RendererConfigurationStateSelector: Phong "
                    "shading is not supported by OpenGL 1.2, Gouraud used\n");
        }
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_COOK_TERRANCE ) {
        unsupported = true;
        if ( !cookTorranceReported ) {
            cookTorranceReported = true;
            fprintf(stderr, "OpenGL1RendererConfigurationStateSelector: "
                    "Cook-Torrance shading is not supported by OpenGL 1.2, "
                    "Gouraud used\n");
        }
    }
    if ( quality->isBumpMapSet() &&
         shadingType != RendererConfiguration::SHADING_TYPE_NOLIGHT ) {
        unsupported = true;
        if ( !bumpMapReported ) {
            bumpMapReported = true;
            fprintf(stderr, "OpenGL1RendererConfigurationStateSelector: bump "
                    "mapping is not supported by OpenGL 1.2, ignored\n");
        }
    }
    return unsupported;
}

void OpenGL1RendererConfigurationStateSelector::pushState()
{
    glPushAttrib(GL_ENABLE_BIT | GL_LIGHTING_BIT | GL_TEXTURE_BIT |
                 GL_CURRENT_BIT);
}

void OpenGL1RendererConfigurationStateSelector::activateState(
    const Camera* camera, const Matrix4x4d& viewProjection,
    const Matrix4x4d& local, const java::ArrayList<Light*>& lights,
    const SimpleMaterial& material, const RendererConfiguration* quality,
    int textureId)
{
    pushState();

    // Eye space: the camera transformation goes to the modelview matrix, and
    // the rest of its projection to the projection matrix
    Matrix4x4d view = Matrix4x4d::identityMatrix();
    Matrix4x4d projection = viewProjection;
    if ( camera != nullptr ) {
        view = camera->calculateTransformationMatrix();
        projection = viewProjection.multiply(view.invert());
    }
    OpenGL1MatrixState::push(projection, view);

    reportUnsupportedFeatures(quality);
    int shadingType = quality != nullptr ?
        quality->getShadingType() : RendererConfiguration::SHADING_TYPE_NOLIGHT;
    bool withTexture = textureId > 0 &&
        (quality == nullptr || quality->isTextureSet());

    ColorRgb diffuse = material.getDiffuse();
    glColor4f((float)diffuse.r(), (float)diffuse.g(), (float)diffuse.b(), 1.0f);
    if ( shadingType == RendererConfiguration::SHADING_TYPE_NOLIGHT ) {
        glDisable(GL_LIGHTING);
        glShadeModel(GL_SMOOTH);
    }
    else {
        // Lights are placed in world space, before the local transformation
        activateLights(lights);
        activateMaterial(material);
        glEnable(GL_LIGHTING);
        glEnable(GL_NORMALIZE);
        glShadeModel(shadingType == RendererConfiguration::SHADING_TYPE_FLAT ?
            GL_FLAT : GL_SMOOTH);
    }

    double* m = local.exportToDoubleArrayColumnOrder();
    glMatrixMode(GL_MODELVIEW);
    glMultMatrixd(m);
    delete[] m;

    if ( withTexture ) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, (GLuint)textureId);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    }
    else {
        glDisable(GL_TEXTURE_2D);
    }
}

void OpenGL1RendererConfigurationStateSelector::activateConstantState(
    const Matrix4x4d& modelViewProjection,
    float diffuseR, float diffuseG, float diffuseB)
{
    pushState();
    OpenGL1MatrixState::push(modelViewProjection);
    glDisable(GL_LIGHTING);
    glDisable(GL_TEXTURE_2D);
    glShadeModel(GL_SMOOTH);
    glColor4f(diffuseR, diffuseG, diffuseB, 1.0f);
}

void OpenGL1RendererConfigurationStateSelector::deactivateState()
{
    OpenGL1MatrixState::pop();
    glPopAttrib();
    glBindTexture(GL_TEXTURE_2D, 0);
}

void OpenGL1RendererConfigurationStateSelector::activateLights(
    const java::ArrayList<Light*>& lights)
{
    const float black[4] = { 0.0f, 0.0f, 0.0f, 1.0f };
    const float white[4] = { 1.0f, 1.0f, 1.0f, 1.0f };

    // The ambient color of the material is added once, as in the shaders
    glLightModelfv(GL_LIGHT_MODEL_AMBIENT, white);
    glLightModeli(GL_LIGHT_MODEL_LOCAL_VIEWER, GL_TRUE);
    glLightModeli(GL_LIGHT_MODEL_TWO_SIDE, GL_FALSE);
    // Specular modulated by the texture, as in the shaders
    glLightModeli(GL_LIGHT_MODEL_COLOR_CONTROL, GL_SINGLE_COLOR);

    int lightCount = 0;
    for ( long i = 0; i < lights.size(); i++ ) {
        if ( lights[i] == nullptr ) {
            continue;
        }
        if ( lightCount >= MAX_LIGHTS ) {
            if ( !tooManyLightsReported ) {
                tooManyLightsReported = true;
                fprintf(stderr, "OpenGL1RendererConfigurationStateSelector: "
                        "scene has %ld lights, but OpenGL 1.2 uses only the "
                        "first %d\n", (long)lights.size(), MAX_LIGHTS);
            }
            break;
        }
        GLenum id = (GLenum)(GL_LIGHT0 + lightCount);
        const Vector3Dd& p = lights[i]->getPosition();
        const ColorRgb& e = lights[i]->getEmission();
        float position[4] = { (float)p.x(), (float)p.y(), (float)p.z(), 1.0f };
        float emission[4] = { (float)e.r(), (float)e.g(), (float)e.b(), 1.0f };
        glLightfv(id, GL_POSITION, position);
        glLightfv(id, GL_AMBIENT, black);
        glLightfv(id, GL_DIFFUSE, emission);
        glLightfv(id, GL_SPECULAR, emission);
        glLightf(id, GL_CONSTANT_ATTENUATION, 1.0f);
        glLightf(id, GL_LINEAR_ATTENUATION, 0.0f);
        glLightf(id, GL_QUADRATIC_ATTENUATION, 0.0f);
        glLightf(id, GL_SPOT_CUTOFF, 180.0f);
        glEnable(id);
        lightCount++;
    }
    for ( int i = lightCount; i < MAX_LIGHTS; i++ ) {
        glDisable((GLenum)(GL_LIGHT0 + i));
    }
}

void OpenGL1RendererConfigurationStateSelector::activateMaterial(
    const SimpleMaterial& material)
{
    const float black[4] = { 0.0f, 0.0f, 0.0f, 1.0f };
    glDisable(GL_COLOR_MATERIAL);
    setColor(GL_FRONT_AND_BACK, GL_AMBIENT, material.getAmbient());
    setColor(GL_FRONT_AND_BACK, GL_DIFFUSE, material.getDiffuse());
    setColor(GL_FRONT_AND_BACK, GL_SPECULAR, material.getSpecular());
    glMaterialfv(GL_FRONT_AND_BACK, GL_EMISSION, black);
    float shininess = (float)material.getPhongExponent();
    if ( shininess < 0.0f ) shininess = 0.0f;
    if ( shininess > MAX_SHININESS ) shininess = MAX_SHININESS;
    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, shininess);
}

void OpenGL1RendererConfigurationStateSelector::dispose()
{
}

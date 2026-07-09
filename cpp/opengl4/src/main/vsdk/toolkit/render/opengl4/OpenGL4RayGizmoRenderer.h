#ifndef VITRAL_OPEN_GL_4_RAY_GIZMO_RENDERER_H
#define VITRAL_OPEN_GL_4_RAY_GIZMO_RENDERER_H

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Camera;
class Light;
class RayGizmo;
class RendererConfiguration;
class SimpleMaterial;

class OpenGL4RayGizmoRenderer {
public:
    static void draw(RayGizmo* gizmo, Camera* camera, const java::ArrayList<Light*>& lights);
    static void dispose();

private:
    static unsigned int indicatorVao_;
    static unsigned int indicatorPositionVbo_;
    static unsigned int indicatorNormalVbo_;
    static unsigned int indicatorUvVbo_;
    static unsigned int indicatorProgram_;
    static bool initialized_;

    static bool ensureIndicatorMesh();
    static bool ensureIndicatorProgram();
    static void uploadIndicatorMesh();
    static void drawIndicator(
        double rollAngleRadians,
        const Matrix4x4d& arrowModelMatrix,
        const Matrix4x4d& projection,
        const Camera* camera,
        const java::ArrayList<Light*>& lights,
        const RendererConfiguration* quality);
    static RendererConfiguration buildSurfaceQuality();
    static SimpleMaterial indicatorMaterial();
    static void uploadBuffer(unsigned int bufferId, int attrib, int size, const float* data, int count);
    static void computeNormals(float normals[9]);
    static unsigned int buildProgram(const char* vsFile, const char* fsFile);
    static unsigned int compileShader(unsigned int type, const char* source);
    static java::String readTextFile(const java::String& path);
    static java::String findShaderSource(const java::String& shaderFileName);
    static void setMatrix(unsigned int programId, const char* name, const Matrix4x4d& matrix);
    static void setVector3(unsigned int programId, const char* name, const Vector3Dd& value);
    static void setVector3(unsigned int programId, const char* name, const ColorRgb& value);
    static void setInt(unsigned int programId, const char* name, int value);
    static void setFloat(unsigned int programId, const char* name, float value);
};

#endif

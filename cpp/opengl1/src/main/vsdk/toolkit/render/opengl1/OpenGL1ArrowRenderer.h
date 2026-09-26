#ifndef VITRAL_OPEN_GL_1_ARROW_RENDERER_H
#define VITRAL_OPEN_GL_1_ARROW_RENDERER_H

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Arrow;
class Camera;
class Light;
class RendererConfiguration;
class SimpleMaterial;

class OpenGL1ArrowRenderer {
public:
    static void draw(
        const Arrow* arrow,
        const Matrix4x4d& modelMatrix,
        const Matrix4x4d& projection,
        const Camera* camera,
        const java::ArrayList<Light*>& lights,
        const SimpleMaterial* material,
        const RendererConfiguration* quality);

    static void dispose();

private:
    struct ArrowMesh {
        java::ArrayList<float> positions;
        java::ArrayList<float> normals;
        java::ArrayList<float> uvs;
        int vertexCount;
    };

    static const int SLICES;
    static unsigned int displayListId;
    static int vertexCount;
    static bool initialized;

    static bool ensureMesh(const Arrow* arrow);
    static ArrowMesh buildArrowMesh(
        double baseRadius,
        double headRadius,
        double baseLength,
        double headLength,
        int slices);
    static void uploadMesh(const ArrowMesh& mesh);
    static void addPos(java::ArrayList<float>& buf, float x, float y, float z);
    static void addNorm(java::ArrayList<float>& buf, float x, float y, float z);
    static void addUv(java::ArrayList<float>& buf, float u, float v);
};

#endif

#ifndef __JOGL_4_DEBUGGER_RENDERER__
#define __JOGL_4_DEBUGGER_RENDERER__

class MeshModel;
class Camera;
class Light;
class RendererConfiguration;
class SimpleBody;
class SimpleMaterial;
class TriangleMesh;

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Jogl4DebuggerRenderer {
private:
    static const float SURFACE_POLYGON_OFFSET_FACTOR;
    static const float SURFACE_POLYGON_OFFSET_UNITS;
    static const float LINE_POLYGON_OFFSET_FACTOR;
    static const float LINE_POLYGON_OFFSET_UNITS;

    MeshModel* model;

    unsigned int vaoId;
    unsigned int positionVboId;
    unsigned int normalVboId;
    unsigned int uvVboId;
    int vertexCount;

    unsigned int constantProgram;
    unsigned int constantTexturedProgram;
    unsigned int flatProgram;
    unsigned int flatTexturedProgram;
    unsigned int gouraudProgram;
    unsigned int phongProgram;
    unsigned int phongBumpProgram;
    unsigned int cookProgram;
    unsigned int cookBumpProgram;

    void drawSimpleBody(SimpleBody* body, Camera* camera, const java::ArrayList<Light*>& lights, RendererConfiguration* quality);
    unsigned int selectProgram(RendererConfiguration* quality, bool hasTexture, bool hasNormalMap);
    bool buildFrame(TriangleMesh* mesh, java::ArrayList<float>& outPositions, java::ArrayList<float>& outNormals, java::ArrayList<float>& outUvs);
    void uploadFrame(java::ArrayList<float>& positions, java::ArrayList<float>& normals, java::ArrayList<float>& uvs);
    void configureProgram(
        unsigned int programId,
        const Matrix4x4d& modelViewProjection,
        const Matrix4x4d& model,
        const Matrix4x4d& modelIt,
        Camera* camera,
        const java::ArrayList<Light*>& lights,
        const SimpleMaterial& material,
        RendererConfiguration* quality,
        bool withTexture,
        int textureId);
    static SimpleMaterial defaultMaterial();
    static SimpleMaterial whiteWireMaterial();
    static SimpleMaterial redPointMaterial();

public:
    explicit Jogl4DebuggerRenderer(MeshModel* model);
    bool init();
    void display();
    void reshape(int width, int height);
    void dispose();
};

#endif

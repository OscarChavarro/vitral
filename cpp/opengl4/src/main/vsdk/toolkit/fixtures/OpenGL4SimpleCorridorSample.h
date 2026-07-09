#ifndef __OPEN_GL_4_SIMPLE_CORRIDOR_SAMPLE__
#define __OPEN_GL_4_SIMPLE_CORRIDOR_SAMPLE__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class OpenGL4SimpleCorridorSample {
public:
    OpenGL4SimpleCorridorSample();
    ~OpenGL4SimpleCorridorSample();

    void drawGL(const float* mvpColumnMajor16, const Matrix4x4d& modelViewProjection);
    void dispose();

private:
    double a;
    int na;
    double b;
    int nb;
    double c;
    int nc;
    double interSpace;

    bool initialized;
    unsigned int shaderProgramId;
    unsigned int vertexArrayId;
    unsigned int positionBufferId;
    unsigned int colorBufferId;
    int modelViewProjectionLocalLoc;
    int withTextureLoc;
    int withVertexColorsLoc;
    int diffuseColorLoc;
    int vertexCount;

    void initialize();
    void buildGeometry(java::ArrayList<float>& positions, java::ArrayList<float>& colors);

    void appendTilesCenter(
        java::ArrayList<float>& positions,
        java::ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg, bool flipYZ, double translateZ);

    void appendTilesLong(
        java::ArrayList<float>& positions,
        java::ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg, bool flipYZ, double translateZ);

    void appendTilesWallA(
        java::ArrayList<float>& positions,
        java::ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg);

    void appendTilesWallB(
        java::ArrayList<float>& positions,
        java::ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg);

    void appendTilesWallC(
        java::ArrayList<float>& positions,
        java::ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg);

    void addQuad(
        java::ArrayList<float>& positions,
        java::ArrayList<float>& colors,
        float r, float g, float bColor,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3,
        double x4, double y4, double z4,
        double rotZDeg, bool flipYZ, double translateZ);

    void addVertex(
        java::ArrayList<float>& positions,
        java::ArrayList<float>& colors,
        double x, double y, double z,
        float r, float g, float bColor,
        double rotZDeg, bool flipYZ, double translateZ);

    unsigned int compileShaders();
    java::String readShaderFile(const java::String& filename);
    unsigned int compileShader(const java::String& source, int type);
};

#endif

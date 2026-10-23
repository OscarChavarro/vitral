#ifndef __OPEN_GL_1_SIMPLE_CORRIDOR_SAMPLE__
#define __OPEN_GL_1_SIMPLE_CORRIDOR_SAMPLE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class OpenGL1SimpleCorridorSample {
public:
    OpenGL1SimpleCorridorSample();
    ~OpenGL1SimpleCorridorSample();

    void drawGL(const float* mvpColumnMajor16, const Matrix4x4d& modelViewProjection);
    void dispose();

private:
    double width;
    int widthTiles;
    double length;
    int lengthTiles;
    double height;
    int heightTiles;
    double interSpace;

    BOOLEAN initialized;
    unsigned int displayListId;
    int vertexCount;

    void initialize();
    void buildGeometry(ArrayList<float>& positions, ArrayList<float>& colors);

    void appendTilesCenter(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg, BOOLEAN flipYZ, double translateZ);

    void appendTilesLong(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg, BOOLEAN flipYZ, double translateZ);

    void appendTilesWallA(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg);

    void appendTilesWallB(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg);

    void appendTilesWallC(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        float r, float g, float bColor,
        double rotZDeg);

    void addQuad(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        float r, float g, float bColor,
        double x1, double y1, double z1,
        double x2, double y2, double z2,
        double x3, double y3, double z3,
        double x4, double y4, double z4,
        double rotZDeg, BOOLEAN flipYZ, double translateZ);

    void addVertex(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        double x, double y, double z,
        float r, float g, float bColor,
        double rotZDeg, BOOLEAN flipYZ, double translateZ);
};

#endif

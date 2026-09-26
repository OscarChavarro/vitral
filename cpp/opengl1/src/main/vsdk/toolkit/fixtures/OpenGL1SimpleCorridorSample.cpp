#include <cmath>
#include <cstdio>

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/fixtures/OpenGL1SimpleCorridorSample.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
OpenGL1SimpleCorridorSample::OpenGL1SimpleCorridorSample()
    : width(6), widthTiles(6), length(20), lengthTiles(20), height(4), heightTiles(4), interSpace(0.05),
      initialized(false), displayListId(0), vertexCount(0) {
}

OpenGL1SimpleCorridorSample::~OpenGL1SimpleCorridorSample() {
    if (initialized) {
        dispose();
    }
}

/**
Compiles the tiles, with their vertex colors, in a display list.
*/
void OpenGL1SimpleCorridorSample::initialize() {
    java::ArrayList<float> positions;
    java::ArrayList<float> colors;

    buildGeometry(positions, colors);

    displayListId = glGenLists(1);
    if (displayListId == 0) {
        fprintf(stderr, "Error: Failed to create display list\n");
        return;
    }

    vertexCount = positions.size() / 3;
    glNewList(displayListId, GL_COMPILE);
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, positions.data());
    glColorPointer(3, GL_FLOAT, 0, colors.data());
    glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
    glEndList();

    initialized = true;
}

void OpenGL1SimpleCorridorSample::drawGL(const float* mvpColumnMajor16, const Matrix4x4d& modelViewProjection) {
    if (!initialized) {
        initialize();
    }
    if (displayListId == 0) {
        return;
    }
    (void)modelViewProjection;

    glPushAttrib(GL_ENABLE_BIT);
    glDisable(GL_LIGHTING);
    glDisable(GL_TEXTURE_2D);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadMatrixf(mvpColumnMajor16);
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glCallList(displayListId);

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
    glPopAttrib();
    glEnable(GL_CULL_FACE);
}

void OpenGL1SimpleCorridorSample::dispose() {
    if (displayListId != 0) {
        glDeleteLists(displayListId, 1);
        displayListId = 0;
    }
    initialized = false;
    vertexCount = 0;
}

void OpenGL1SimpleCorridorSample::buildGeometry(java::ArrayList<float>& positions, java::ArrayList<float>& colors) {
    appendTilesCenter(positions, colors, 0.5f, 0.5f, 0.9f, 0, false, 0);
    for (int i = 0; i < 4; i++) {
        appendTilesLong(positions, colors, 0.5f, 0.5f, 0.9f, 90 * i, false, 0);
    }

    appendTilesCenter(positions, colors, 0.0f, 0.0f, 1.0f, 0, true, height);
    for (int i = 0; i < 4; i++) {
        appendTilesLong(positions, colors, 0.0f, 0.0f, 1.0f, 90 * i, true, height);
    }

    for (int i = 0; i < 4; i++) {
        switch (i) {
            case 0:
                appendTilesWallA(positions, colors, 0.9f, 0.5f, 0.5f, 90 * i);
                break;
            case 1:
                appendTilesWallA(positions, colors, 0.5f, 0.9f, 0.5f, 90 * i);
                break;
            case 2:
                appendTilesWallA(positions, colors, 1.0f, 0.0f, 0.0f, 90 * i);
                break;
            default:
                appendTilesWallA(positions, colors, 0.0f, 1.0f, 0.0f, 90 * i);
                break;
        }
    }

    for (int i = 0; i < 4; i++) {
        appendTilesWallB(positions, colors, 0.9f, 0.5f, 0.8f, 90 * i);
        appendTilesWallC(positions, colors, 0.9f, 0.5f, 0.8f, 90 * i);
    }
}

void OpenGL1SimpleCorridorSample::appendTilesCenter(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg, bool flipYZ, double translateZ) {

    double da = width / ((double)widthTiles);
    double epsilon = 0.005;

    for (int i = 0; i < widthTiles; i++) {
        double x = -width / 2 + i * da;
        for (int j = 0; j < widthTiles; j++) {
            double y = -width / 2 + j * da;
            addQuad(positions, colors, r, g, bColor,
                x + interSpace / 2, y + interSpace / 2, -epsilon,
                x + da - interSpace / 2, y + interSpace / 2, -epsilon,
                x + da - interSpace / 2, y + da - interSpace / 2, -epsilon,
                x + interSpace / 2, y + da - interSpace / 2, -epsilon,
                rotZDeg, flipYZ, translateZ);
        }
    }
}

void OpenGL1SimpleCorridorSample::appendTilesLong(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg, bool flipYZ, double translateZ) {

    double da = width / ((double)widthTiles);
    double db = length / ((double)lengthTiles);
    double epsilon = 0.001;

    for (int i = 0; i < lengthTiles; i++) {
        double x = -width / 2 - length + i * db;
        for (int j = 0; j < widthTiles; j++) {
            double y = -width / 2 + j * da;
            addQuad(positions, colors, r, g, bColor,
                x + interSpace / 2, y + interSpace / 2, -epsilon,
                x + da - interSpace / 2, y + interSpace / 2, -epsilon,
                x + da - interSpace / 2, y + da - interSpace / 2, -epsilon,
                x + interSpace / 2, y + da - interSpace / 2, -epsilon,
                rotZDeg, flipYZ, translateZ);
        }
    }
}

void OpenGL1SimpleCorridorSample::appendTilesWallA(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg) {

    double da = width / ((double)widthTiles);
    double dc = height / ((double)heightTiles);

    for (int i = 0; i < heightTiles; i++) {
        double z = i * dc;
        for (int j = 0; j < widthTiles; j++) {
            double y = -width / 2 + j * da;
            addQuad(positions, colors, r, g, bColor,
                -width / 2 - length, y + interSpace / 2, z + dc - interSpace / 2,
                -width / 2 - length, y + interSpace / 2, z + interSpace / 2,
                -width / 2 - length, y + da - interSpace / 2, z + interSpace / 2,
                -width / 2 - length, y + da - interSpace / 2, z + dc - interSpace / 2,
                rotZDeg, false, 0);
        }
    }
}

void OpenGL1SimpleCorridorSample::appendTilesWallB(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg) {

    double db = length / ((double)lengthTiles);
    double dc = height / ((double)heightTiles);

    for (int i = 0; i < heightTiles; i++) {
        double z = i * dc;
        for (int j = 0; j < lengthTiles; j++) {
            double y = width / 2 + j * db;
            addQuad(positions, colors, r, g, bColor,
                -width / 2, y + interSpace / 2, z + dc - interSpace / 2,
                -width / 2, y + interSpace / 2, z + interSpace / 2,
                -width / 2, y + db - interSpace / 2, z + interSpace / 2,
                -width / 2, y + db - interSpace / 2, z + dc - interSpace / 2,
                rotZDeg, false, 0);
        }
    }
}

void OpenGL1SimpleCorridorSample::appendTilesWallC(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg) {

    double db = length / ((double)lengthTiles);
    double dc = height / ((double)heightTiles);

    for (int i = 0; i < lengthTiles; i++) {
        double x = -width / 2 - length + i * db;
        for (int j = 0; j < heightTiles; j++) {
            double z = j * dc;
            addQuad(positions, colors, r, g, bColor,
                x + interSpace / 2, width / 2, z + interSpace / 2,
                x + db - interSpace / 2, width / 2, z + interSpace / 2,
                x + db - interSpace / 2, width / 2, z + dc - interSpace / 2,
                x + interSpace / 2, width / 2, z + dc - interSpace / 2,
                rotZDeg, false, 0);
        }
    }
}

void OpenGL1SimpleCorridorSample::addQuad(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double x1, double y1, double z1,
    double x2, double y2, double z2,
    double x3, double y3, double z3,
    double x4, double y4, double z4,
    double rotZDeg, bool flipYZ, double translateZ) {

    addVertex(positions, colors, x1, y1, z1, r, g, bColor, rotZDeg, flipYZ, translateZ);
    addVertex(positions, colors, x2, y2, z2, r, g, bColor, rotZDeg, flipYZ, translateZ);
    addVertex(positions, colors, x3, y3, z3, r, g, bColor, rotZDeg, flipYZ, translateZ);

    addVertex(positions, colors, x1, y1, z1, r, g, bColor, rotZDeg, flipYZ, translateZ);
    addVertex(positions, colors, x3, y3, z3, r, g, bColor, rotZDeg, flipYZ, translateZ);
    addVertex(positions, colors, x4, y4, z4, r, g, bColor, rotZDeg, flipYZ, translateZ);
}

void OpenGL1SimpleCorridorSample::addVertex(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    double x, double y, double z,
    float r, float g, float bColor,
    double rotZDeg, bool flipYZ, double translateZ) {

    double tx = x;
    double ty = y;
    double tz = z;

    if (flipYZ) {
        ty = -ty;
        tz = -tz;
    }

    double angle = rotZDeg * M_PI / 180.0;
    double cos_a = std::cos(angle);
    double sin_a = std::sin(angle);

    double rx = tx * cos_a - ty * sin_a;
    double ry = tx * sin_a + ty * cos_a;
    double rz = tz + translateZ;

    positions.add((float)rx);
    positions.add((float)ry);
    positions.add((float)rz);

    colors.add(r);
    colors.add(g);
    colors.add(bColor);
}

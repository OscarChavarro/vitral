#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/opengl1/OpenGL1LineRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MatrixRenderer.h"

void OpenGL1MatrixRenderer::draw(const float* mvpColumnMajor16, const Matrix4x4d& A) {
    Vector3Dd x(A.get(0, 0), A.get(1, 0), A.get(2, 0));
    Vector3Dd y(A.get(0, 1), A.get(1, 1), A.get(2, 1));
    Vector3Dd z(A.get(0, 2), A.get(1, 2), A.get(2, 2));
    Vector3Dd translation(A.get(0, 3), A.get(1, 3), A.get(2, 3));
    java::ArrayList<float> positions;
    positions.reserve(18);
    positions.add((float)translation.x());
    positions.add((float)translation.y());
    positions.add((float)translation.z());
    positions.add((float)(translation.x() + x.x()));
    positions.add((float)(translation.y() + x.y()));
    positions.add((float)(translation.z() + x.z()));

    positions.add((float)translation.x());
    positions.add((float)translation.y());
    positions.add((float)translation.z());
    positions.add((float)(translation.x() + y.x()));
    positions.add((float)(translation.y() + y.y()));
    positions.add((float)(translation.z() + y.z()));

    positions.add((float)translation.x());
    positions.add((float)translation.y());
    positions.add((float)translation.z());
    positions.add((float)(translation.x() + z.x()));
    positions.add((float)(translation.y() + z.y()));
    positions.add((float)(translation.z() + z.z()));

    java::ArrayList<float> colors;
    colors.reserve(18);
    colors.add(1.0f); colors.add(0.0f); colors.add(0.0f);
    colors.add(1.0f); colors.add(0.0f); colors.add(0.0f);
    colors.add(0.0f); colors.add(1.0f); colors.add(0.0f);
    colors.add(0.0f); colors.add(1.0f); colors.add(0.0f);
    colors.add(0.0f); colors.add(0.0f); colors.add(1.0f);
    colors.add(0.0f); colors.add(0.0f); colors.add(1.0f);

    double mvpValues[4][4];
    int pos = 0;
    for ( int column = 0; column < 4; column++ ) {
        for ( int row = 0; row < 4; row++, pos++ ) {
            mvpValues[row][column] = mvpColumnMajor16[pos];
        }
    }
    Matrix4x4d mvp(mvpValues);
    OpenGL1LineRenderer::drawLines(mvp, positions, colors, 1.0f);
}

void OpenGL1MatrixRenderer::release() {
    OpenGL1LineRenderer::release();
}

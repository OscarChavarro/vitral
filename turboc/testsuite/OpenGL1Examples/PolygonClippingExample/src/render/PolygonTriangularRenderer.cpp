#include "java/util/ArrayList.txx"
#include "render/PolygonTriangularRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.h"
void PolygonTriangularRenderer::drawColoredArrays(const Matrix4x4d& mvp,
    unsigned int mode, const float* positions, const float* colors,
    int vertexCount)
{
    if (vertexCount <= 0) return;
    OpenGL1RendererConfigurationStateSelector::activateConstantState(
        mvp, 1.0f, 1.0f, 1.0f);
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, positions);
    glEnableClientState(GL_COLOR_ARRAY);
    glColorPointer(3, GL_FLOAT, 0, colors);
    glDrawArrays((GLenum)mode, 0, (GLsizei)vertexCount);
    glDisableClientState(GL_COLOR_ARRAY);
    glDisableClientState(GL_VERTEX_ARRAY);
    OpenGL1RendererConfigurationStateSelector::deactivateState();
}

void PolygonTriangularRenderer::fillPolygonSurface(
    const Matrix4x4d& mvp,
    Polygon2D* polygon,
    RendererConfiguration* config,
    float fillR, float fillG, float fillB,
    float lineR, float lineG, float lineB)
{
    if (!polygon || !config) return;
    if (!config->isSurfacesSet() && !config->isWiresSet() && !config->isPointsSet()) return;
    if (polygon->loops.size() <= 0) return;

    // Flatten polygon vertices into a lookup array.
    // Seidel 1-based indices: triangle.get(j) - 1 = 0-based flat index.
    int totalVertices = 0;
    for (long int i = 0; i < polygon->loops.size(); ++i)
        totalVertices += (int)polygon->loops[i]->vertices.size();
    if (totalVertices <= 0) return;

    double* vx = new double[(size_t)totalVertices];
    double* vz = new double[(size_t)totalVertices];
    int vi = 0;
    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContour* loop = polygon->loops[i];
        for (long int j = 0; j < loop->vertices.size(); ++j) {
            vx[vi] = loop->vertices[j].x;
            vz[vi] = loop->vertices[j].y;
            vi++;
        }
    }

    MonotoneDecompositionTriangulator triangulator;
    ArrayList<MonotoneDecompositionTriangulator::Triangle> triangles;
    int triangleCount = 0;
    try {
        triangulator.triangulate(*polygon, triangles, triangleCount);
    }
    catch (...) {
        delete[] vx;
        delete[] vz;
        return;
    }

    if (triangleCount <= 0) {
        delete[] vx;
        delete[] vz;
        return;
    }

    // Draw filled triangles with the constant-color program.
    if (config->isSurfacesSet()) {
        ArrayList<float> pos;
        for (int i = 0; i < triangleCount; ++i) {
            const MonotoneDecompositionTriangulator::Triangle& t = triangles.get(i);
            // C++ Seidel indices are 1-based
            int a = t.get(0) - 1;
            int b = t.get(1) - 1;
            int c = t.get(2) - 1;
            if (a < 0 || b < 0 || c < 0
                || a >= totalVertices || b >= totalVertices || c >= totalVertices)
                continue;
            // x, y(=0), z
            pos.add((float)vx[a]); pos.add(0.0f); pos.add((float)vz[a]);
            pos.add((float)vx[b]); pos.add(0.0f); pos.add((float)vz[b]);
            pos.add((float)vx[c]); pos.add(0.0f); pos.add((float)vz[c]);
        }
        if (pos.size() > 0) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1.0f, 1.0f);

            OpenGL1RendererConfigurationStateSelector::activateConstantState(
                mvp, fillR, fillG, fillB);
            glEnableClientState(GL_VERTEX_ARRAY);
            glVertexPointer(3, GL_FLOAT, 0, pos.data());
            glDrawArrays(GL_TRIANGLES, 0, (GLsizei)(pos.size() / 3));
            glDisableClientState(GL_VERTEX_ARRAY);
            OpenGL1RendererConfigurationStateSelector::deactivateState();

            glPolygonOffset(0.0f, 0.0f);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
    }

    // Draw triangle-edge wires with the line program.
    if (config->isWiresSet()) {
        ArrayList<float> wPos, wCol;
        for (int i = 0; i < triangleCount; ++i) {
            const MonotoneDecompositionTriangulator::Triangle& t = triangles.get(i);
            int a = t.get(0) - 1;
            int b = t.get(1) - 1;
            int c = t.get(2) - 1;
            if (a < 0 || b < 0 || c < 0
                || a >= totalVertices || b >= totalVertices || c >= totalVertices)
                continue;
            float pts[3][2] = {
                {(float)vx[a], (float)vz[a]},
                {(float)vx[b], (float)vz[b]},
                {(float)vx[c], (float)vz[c]}
            };
            for (int e = 0; e < 3; ++e) {
                int e2 = (e + 1) % 3;
                wPos.add(pts[e][0]); wPos.add(0.0f); wPos.add(pts[e][1]);
                wCol.add(lineR); wCol.add(lineG); wCol.add(lineB);
                wPos.add(pts[e2][0]); wPos.add(0.0f); wPos.add(pts[e2][1]);
                wCol.add(lineR); wCol.add(lineG); wCol.add(lineB);
            }
        }
        if (wPos.size() > 0) {
            glLineWidth(1.0f);
            drawColoredArrays(mvp, GL_LINES, wPos.data(), wCol.data(),
                (int)(wPos.size() / 3));
        }
    }

    // Draw triangle vertex points with the line program.
    if (config->isPointsSet()) {
        ArrayList<float> pPos, pCol;
        for (int i = 0; i < triangleCount; ++i) {
            const MonotoneDecompositionTriangulator::Triangle& t = triangles.get(i);
            for (int j = 0; j < 3; ++j) {
                int idx = t.get(j) - 1;
                if (idx < 0 || idx >= totalVertices) continue;
                pPos.add((float)vx[idx]); pPos.add(0.0f); pPos.add((float)vz[idx]);
                pCol.add(lineR); pCol.add(lineG); pCol.add(lineB);
            }
        }
        if (pPos.size() > 0) {
            glPointSize(8.0f);
            drawColoredArrays(mvp, GL_POINTS, pPos.data(), pCol.data(),
                (int)(pPos.size() / 3));
            glPointSize(1.0f);
        }
    }

    delete[] vx;
    delete[] vz;
}

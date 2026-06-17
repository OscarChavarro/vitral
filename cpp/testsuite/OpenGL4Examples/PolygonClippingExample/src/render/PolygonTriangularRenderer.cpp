#include "java/util/ArrayList.txx"
#include "render/PolygonTriangularRenderer.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.h"
static void setMvp(GLuint prog, const Matrix4x4d& mvp)
{
    GLint loc = glGetUniformLocation(prog, "modelViewProjectionLocal");
    if (loc < 0) return;
    float mm[16];
    int k = 0;
    for (int c = 0; c < 4; ++c)
        for (int r = 0; r < 4; ++r)
            mm[k++] = (float)mvp.get(r, c);
    glUniformMatrix4fv(loc, 1, GL_FALSE, mm);
}

void PolygonTriangularRenderer::fillPolygonSurface(
    const Matrix4x4d& mvp,
    Polygon2D* polygon,
    RendererConfiguration* config,
    float fillR, float fillG, float fillB,
    float lineR, float lineG, float lineB,
    GLuint lineProg, GLuint constantProg,
    GLuint vao, GLuint vboP, GLuint vboC)
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
    java::ArrayList<MonotoneDecompositionTriangulator::Triangle> triangles;
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

    float mm[16];
    int k = 0;
    for (int c = 0; c < 4; ++c)
        for (int r = 0; r < 4; ++r)
            mm[k++] = (float)mvp.get(r, c);

    // Draw filled triangles with the constant-color program.
    if (config->isSurfacesSet()) {
        java::ArrayList<float> pos;
        for (int i = 0; i < triangleCount; ++i) {
            const MonotoneDecompositionTriangulator::Triangle& t = triangles.get(i);
            // C++ Seidel indices are 1-based
            int a = t.get(0) - 1;
            int b = t.get(1) - 1;
            int c = t.get(2) - 1;
            if (a < 0 || b < 0 || c < 0
                || a >= totalVertices || b >= totalVertices || c >= totalVertices)
                continue;
            // vec4: x, y(=0), z, w(=1)
            pos.add((float)vx[a]); pos.add(0.0f); pos.add((float)vz[a]); pos.add(1.0f);
            pos.add((float)vx[b]); pos.add(0.0f); pos.add((float)vz[b]); pos.add(1.0f);
            pos.add((float)vx[c]); pos.add(0.0f); pos.add((float)vz[c]); pos.add(1.0f);
        }
        if (pos.size() > 0) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1.0f, 1.0f);

            glUseProgram(constantProg);
            setMvp(constantProg, mvp);
            GLint withTexLoc = glGetUniformLocation(constantProg, "withTexture");
            GLint withVCLoc  = glGetUniformLocation(constantProg, "withVertexColors");
            GLint diffLoc    = glGetUniformLocation(constantProg, "diffuseColor");
            if (withTexLoc >= 0) glUniform1i(withTexLoc, 0);
            if (withVCLoc  >= 0) glUniform1i(withVCLoc, 0);
            if (diffLoc    >= 0) glUniform3f(diffLoc, fillR, fillG, fillB);

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vboP);
            glBufferData(GL_ARRAY_BUFFER, pos.size() * sizeof(float), &pos[0], GL_STREAM_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 0, (void*)0);
            glDrawArrays(GL_TRIANGLES, 0, (GLsizei)(pos.size() / 4));
            glDisableVertexAttribArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            glUseProgram(0);

            glPolygonOffset(0.0f, 0.0f);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
    }

    // Draw triangle-edge wires with the line program.
    if (config->isWiresSet()) {
        java::ArrayList<float> wPos, wCol;
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
            glUseProgram(lineProg);
            setMvp(lineProg, mvp);

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vboP);
            glBufferData(GL_ARRAY_BUFFER, wPos.size() * sizeof(float), &wPos[0], GL_STREAM_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);
            glBindBuffer(GL_ARRAY_BUFFER, vboC);
            glBufferData(GL_ARRAY_BUFFER, wCol.size() * sizeof(float), &wCol[0], GL_STREAM_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);
            glLineWidth(1.0f);
            glDrawArrays(GL_LINES, 0, (GLsizei)(wPos.size() / 3));
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            glUseProgram(0);
        }
    }

    // Draw triangle vertex points with the line program.
    if (config->isPointsSet()) {
        java::ArrayList<float> pPos, pCol;
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
            glUseProgram(lineProg);
            setMvp(lineProg, mvp);

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vboP);
            glBufferData(GL_ARRAY_BUFFER, pPos.size() * sizeof(float), &pPos[0], GL_STREAM_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);
            glBindBuffer(GL_ARRAY_BUFFER, vboC);
            glBufferData(GL_ARRAY_BUFFER, pCol.size() * sizeof(float), &pCol[0], GL_STREAM_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);
            glPointSize(8.0f);
            glDrawArrays(GL_POINTS, 0, (GLsizei)(pPos.size() / 3));
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            glUseProgram(0);
        }
    }

    delete[] vx;
    delete[] vz;
}

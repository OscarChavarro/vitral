#include "vsdk/toolkit/render/opengl4/OpenGL4Polygon2DRenderer.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "java/util/ArrayList.h"
#include "java/util/ArrayList.txx"
#include <OpenGL/glu.h>

void OpenGL4Polygon2DRenderer::push3(java::ArrayList<float>& a, float x, float y, float z) { a.add(x); a.add(y); a.add(z); }
void OpenGL4Polygon2DRenderer::push4(java::ArrayList<float>& a, float x, float y, float z, float w) { a.add(x); a.add(y); a.add(z); a.add(w); }
void OpenGL4Polygon2DRenderer::toColumnMajor(const Matrix4x4d& m, float out[16])
{
    int k = 0;
    for (int c = 0; c < 4; ++c) {
        for (int r = 0; r < 4; ++r) out[k++] = (float)m.get(r, c);
    }
}

void OpenGL4Polygon2DRenderer::tessBegin(GLenum which, void* userData)
{
    TessCollector* c = (TessCollector*)userData;
    c->mode = which;
    c->pending.clear();
}

void OpenGL4Polygon2DRenderer::tessVertex(void* vertexData, void* userData)
{
    TessCollector* c = (TessCollector*)userData;
    double* v = (double*)vertexData;
    c->pending.add((float)v[0]); c->pending.add((float)v[1]); c->pending.add((float)v[2]);
}

void OpenGL4Polygon2DRenderer::tessEnd(void* userData)
{
    TessCollector* c = (TessCollector*)userData;
    if (c->mode == GL_TRIANGLES) {
        for (long int i = 0; i + 2 < c->pending.size(); i += 3) {
            c->out->add(c->pending[i]);
            c->out->add(c->pending[i + 1]);
            c->out->add(c->pending[i + 2]);
        }
        return;
    }
    if (c->pending.size() < 9) return;
    if (c->mode == GL_TRIANGLE_FAN) {
        float x0 = c->pending[0], y0 = c->pending[1], z0 = c->pending[2];
        for (long int i = 3; i + 5 < c->pending.size(); i += 3) {
            c->out->add(x0); c->out->add(y0); c->out->add(z0);
            c->out->add(c->pending[i]); c->out->add(c->pending[i+1]); c->out->add(c->pending[i+2]);
            c->out->add(c->pending[i+3]); c->out->add(c->pending[i+4]); c->out->add(c->pending[i+5]);
        }
    }
    else if (c->mode == GL_TRIANGLE_STRIP) {
        for (long int i = 0; i + 8 < c->pending.size(); i += 3) {
            bool odd = ((i / 3) % 2) != 0;
            if (!odd) {
                c->out->add(c->pending[i]); c->out->add(c->pending[i+1]); c->out->add(c->pending[i+2]);
                c->out->add(c->pending[i+3]); c->out->add(c->pending[i+4]); c->out->add(c->pending[i+5]);
                c->out->add(c->pending[i+6]); c->out->add(c->pending[i+7]); c->out->add(c->pending[i+8]);
            }
            else {
                c->out->add(c->pending[i+3]); c->out->add(c->pending[i+4]); c->out->add(c->pending[i+5]);
                c->out->add(c->pending[i]); c->out->add(c->pending[i+1]); c->out->add(c->pending[i+2]);
                c->out->add(c->pending[i+6]); c->out->add(c->pending[i+7]); c->out->add(c->pending[i+8]);
            }
        }
    }
}

void OpenGL4Polygon2DRenderer::tessCombine(
    GLdouble coords[3],
    void* [4],
    GLfloat [4],
    void** outData,
    void* userData)
{
    TessCollector* c = (TessCollector*)userData;
    double* nv = new double[3];
    nv[0] = coords[0]; nv[1] = coords[1]; nv[2] = coords[2];
    c->allocs.add(nv);
    *outData = nv;
}

void OpenGL4Polygon2DRenderer::tessError(GLenum, void*) {}

void OpenGL4Polygon2DRenderer::tessellatePolygonToTriangles(Polygon2D* polygon, java::ArrayList<float>& out)
{
    GLUtesselator* tess = gluNewTess();
    if (!tess) return;
    TessCollector c; c.out = &out;
    gluTessCallback(tess, GLU_TESS_BEGIN_DATA, (void (*)())tessBegin);
    gluTessCallback(tess, GLU_TESS_VERTEX_DATA, (void (*)())tessVertex);
    gluTessCallback(tess, GLU_TESS_END_DATA, (void (*)())tessEnd);
    gluTessCallback(tess, GLU_TESS_COMBINE_DATA, (void (*)())tessCombine);
    gluTessCallback(tess, GLU_TESS_ERROR_DATA, (void (*)())tessError);
    gluTessBeginPolygon(tess, &c);
    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContour* contour = polygon->loops[i];
        if (!contour || contour->vertices.size() < 3) continue;
        gluTessBeginContour(tess);
        for (long int j = 0; j < contour->vertices.size(); ++j) {
            Vertex2D v = contour->vertices[j];
            double* p = new double[3];
            p[0] = v.x; p[1] = 0.0; p[2] = v.y;
            c.allocs.add(p);
            gluTessVertex(tess, p, p);
        }
        gluTessEndContour(tess);
    }
    gluTessEndPolygon(tess);
    gluDeleteTess(tess);
    for (long int i = 0; i < c.allocs.size(); ++i) delete[] c.allocs[i];
}

void OpenGL4Polygon2DRenderer::draw(const Matrix4x4d& mvp, Polygon2D* polygon, RendererConfiguration* quality,
    float fillR, float fillG, float fillB, float lineR, float lineG, float lineB, GLuint lineProgramId, GLuint constantProgramId, GLuint vaoId, GLuint positionVboId, GLuint colorVboId)
{
    if (polygon == 0 || quality == 0) return;

    if (quality->isSurfacesSet()) {
        java::ArrayList<float> tri3;
        tessellatePolygonToTriangles(polygon, tri3);
        if (tri3.size() >= 9) {
            java::ArrayList<float> tri4;
            for (long int i = 0; i + 2 < tri3.size(); i += 3) {
                push4(tri4, tri3[i], tri3[i+1], tri3[i+2], 1.0f);
            }
            glUseProgram(constantProgramId);
            GLint mvpLoc2 = glGetUniformLocation(constantProgramId, "modelViewProjectionLocal");
            if (mvpLoc2 >= 0) {
                float m[16];
                toColumnMajor(mvp, m);
                glUniformMatrix4fv(mvpLoc2, 1, GL_FALSE, m);
            }
            GLint withTextureLoc = glGetUniformLocation(constantProgramId, "withTexture");
            GLint withVertexColorsLoc = glGetUniformLocation(constantProgramId, "withVertexColors");
            GLint diffuseLoc = glGetUniformLocation(constantProgramId, "diffuseColor");
            if (withTextureLoc >= 0) glUniform1i(withTextureLoc, 0);
            if (withVertexColorsLoc >= 0) glUniform1i(withVertexColorsLoc, 0);
            if (diffuseLoc >= 0) glUniform3f(diffuseLoc, fillR, fillG, fillB);
            glBindVertexArray(vaoId);
            glBindBuffer(GL_ARRAY_BUFFER, positionVboId);
            glBufferData(GL_ARRAY_BUFFER, tri4.size() * sizeof(float), &tri4[0], GL_STREAM_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 0, (void*)0);
            glDisableVertexAttribArray(1);
            glVertexAttrib3f(1, 0.0f, 0.0f, 0.0f);
            glDisableVertexAttribArray(2);
            glVertexAttrib2f(2, 0.0f, 0.0f);
            glDrawArrays(GL_TRIANGLES, 0, tri4.size() / 4);
            glDisableVertexAttribArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            glUseProgram(0);
        }
    }

    glUseProgram(lineProgramId);
    GLint mvpLoc = glGetUniformLocation(lineProgramId, "modelViewProjectionLocal");
    if (mvpLoc >= 0) {
        float m[16];
        toColumnMajor(mvp, m);
        glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, m);
    }

    glBindVertexArray(vaoId);
    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContour* contour = polygon->loops[i];
        if (contour->vertices.size() <= 0) continue;
        java::ArrayList<float> pos;
        java::ArrayList<float> col;
        for (long int j = 0; j < contour->vertices.size(); ++j) {
            Vertex2D v = contour->vertices[j];
            push3(pos, (float)v.x, 0.0f, (float)v.y);
            push3(col, lineR, lineG, lineB);
        }

        glBindBuffer(GL_ARRAY_BUFFER, positionVboId);
        glBufferData(GL_ARRAY_BUFFER, pos.size() * sizeof(float), &pos[0], GL_STREAM_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);

        glBindBuffer(GL_ARRAY_BUFFER, colorVboId);
        glBufferData(GL_ARRAY_BUFFER, col.size() * sizeof(float), &col[0], GL_STREAM_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, (void*)0);

        if (quality->isWiresSet()) glDrawArrays(GL_LINE_LOOP, 0, contour->vertices.size());
        if (quality->isPointsSet()) glDrawArrays(GL_POINTS, 0, contour->vertices.size());
    }

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    glUseProgram(0);
}

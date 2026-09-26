#include "vsdk/toolkit/render/opengl1/OpenGL1Polygon2DRenderer.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MatrixState.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
void OpenGL1Polygon2DRenderer::push3(java::ArrayList<float>& a, float x, float y, float z) { a.add(x); a.add(y); a.add(z); }
void OpenGL1Polygon2DRenderer::tessBegin(GLenum which, void* userData)
{
    TessCollector* c = (TessCollector*)userData;
    c->mode = which;
    c->pending.clear();
}

void OpenGL1Polygon2DRenderer::tessVertex(void* vertexData, void* userData)
{
    TessCollector* c = (TessCollector*)userData;
    double* v = (double*)vertexData;
    c->pending.add((float)v[0]); c->pending.add((float)v[1]); c->pending.add((float)v[2]);
}

void OpenGL1Polygon2DRenderer::tessEnd(void* userData)
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

void OpenGL1Polygon2DRenderer::tessCombine(
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

void OpenGL1Polygon2DRenderer::tessError(GLenum, void*) {}

void OpenGL1Polygon2DRenderer::tessellatePolygonToTriangles(Polygon2D* polygon, java::ArrayList<float>& out)
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

void OpenGL1Polygon2DRenderer::draw(const Matrix4x4d& mvp, Polygon2D* polygon, RendererConfiguration* quality,
    float fillR, float fillG, float fillB, float lineR, float lineG, float lineB)
{
    if (polygon == 0 || quality == 0) return;

    glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT);
    glDisable(GL_LIGHTING);
    glDisable(GL_TEXTURE_2D);
    OpenGL1MatrixState::push(mvp);

    if (quality->isSurfacesSet()) {
        java::ArrayList<float> tri3;
        tessellatePolygonToTriangles(polygon, tri3);
        if (tri3.size() >= 9) {
            glColor3f(fillR, fillG, fillB);
            glBegin(GL_TRIANGLES);
            for (long int i = 0; i + 2 < tri3.size(); i += 3) {
                glVertex3f(tri3[i], tri3[i+1], tri3[i+2]);
            }
            glEnd();
        }
    }

    glColor3f(lineR, lineG, lineB);
    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContour* contour = polygon->loops[i];
        if (contour->vertices.size() <= 0) continue;
        if (quality->isWiresSet()) {
            glBegin(GL_LINE_LOOP);
            for (long int j = 0; j < contour->vertices.size(); ++j) {
                Vertex2D v = contour->vertices[j];
                glVertex3f((float)v.x, 0.0f, (float)v.y);
            }
            glEnd();
        }
        if (quality->isPointsSet()) {
            glBegin(GL_POINTS);
            for (long int j = 0; j < contour->vertices.size(); ++j) {
                Vertex2D v = contour->vertices[j];
                glVertex3f((float)v.x, 0.0f, (float)v.y);
            }
            glEnd();
        }
    }

    OpenGL1MatrixState::pop();
    glPopAttrib();
}

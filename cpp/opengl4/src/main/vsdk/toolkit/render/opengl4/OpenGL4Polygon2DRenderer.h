#ifndef __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4POLYGON2DRENDERER_H__
#define __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4POLYGON2DRENDERER_H__

#include <GL/glew.h>
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"

class OpenGL4Polygon2DRenderer {
public:
    static void draw(
        const Matrix4x4d& mvp,
        Polygon2D* polygon,
        RendererConfiguration* quality,
        float fillR,
        float fillG,
        float fillB,
        float lineR,
        float lineG,
        float lineB,
        GLuint lineProgramId,
        GLuint constantProgramId,
        GLuint vaoId,
        GLuint positionVboId,
        GLuint colorVboId);

private:
    struct TessCollector {
        java::ArrayList<float>* out;
        java::ArrayList<double*> allocs;
        GLenum mode;
        java::ArrayList<float> pending;

        TessCollector() : out(0), mode(GL_TRIANGLES) {}
    };

    static void push3(java::ArrayList<float>& a, float x, float y, float z);
    static void push4(java::ArrayList<float>& a, float x, float y, float z, float w);
    static void toColumnMajor(const Matrix4x4d& m, float out[16]);
    static void tessBegin(GLenum which, void* userData);
    static void tessVertex(void* vertexData, void* userData);
    static void tessEnd(void* userData);
    static void tessCombine(GLdouble coords[3], void* inData[4], GLfloat weights[4], void** outData, void* userData);
    static void tessError(GLenum errorCode, void* userData);
    static void tessellatePolygonToTriangles(Polygon2D* polygon, java::ArrayList<float>& out);
};

#endif

#ifndef __POLYGON_TRIANGULAR_RENDERER__
#define __POLYGON_TRIANGULAR_RENDERER__

#include <GL/glew.h>
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
class PolygonTriangularRenderer {
public:
    static void fillPolygonSurface(
        const Matrix4x4d& mvp,
        Polygon2D* polygon,
        RendererConfiguration* config,
        float fillR, float fillG, float fillB,
        float lineR, float lineG, float lineB,
        GLuint lineProg, GLuint constantProg,
        GLuint vao, GLuint vboP, GLuint vboC);
};

#endif

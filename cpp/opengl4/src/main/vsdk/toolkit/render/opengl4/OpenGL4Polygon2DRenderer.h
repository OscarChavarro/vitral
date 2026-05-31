#ifndef __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4POLYGON2DRENDERER_H__
#define __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4POLYGON2DRENDERER_H__

#include <GL/glew.h>
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
};

#endif

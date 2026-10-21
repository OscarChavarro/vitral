#ifndef __POLYGON_TRIANGULAR_RENDERER__
#define __POLYGON_TRIANGULAR_RENDERER__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"

/**
Draws a polygon triangulated by monotone decomposition (instead of the GLU
tessellator used by `OpenGL1Polygon2DRenderer`), with the OpenGL 1.2 fixed
function pipeline.
*/
class PolygonTriangularRenderer {
public:
    static void fillPolygonSurface(
        const Matrix4x4d& mvp,
        Polygon2D* polygon,
        RendererConfiguration* config,
        float fillR, float fillG, float fillB,
        float lineR, float lineG, float lineB);

    /**
    Draws non indexed vertices (x, y, z) with a color per vertex (r, g, b),
    projected by the given matrix.
    @param mode GL_LINES, GL_POINTS, ...
    */
    static void drawColoredArrays(const Matrix4x4d& mvp, unsigned int mode,
        const float* positions, const float* colors, int vertexCount);
};

#endif

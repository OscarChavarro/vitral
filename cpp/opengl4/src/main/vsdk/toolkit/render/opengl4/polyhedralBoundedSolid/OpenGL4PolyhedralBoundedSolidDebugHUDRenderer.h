#ifndef __OPEN_GL_4_POLYHEDRAL_BOUNDED_SOLID_DEBUG_HUD_RENDERER__
#define __OPEN_GL_4_POLYHEDRAL_BOUNDED_SOLID_DEBUG_HUD_RENDERER__

class Camera;
class OpenGL4LabelCanvas;
class PolyhedralBoundedSolid;

/**
Labels of the debugger of `PolyhedralBoundedSolid`s over the viewport: the
id of a selected face at the middle of its projected vertices, and the ids
of the visible vertices (vertices close in space or in the viewport share
one label).

C++ counterpart of Java's `Jogl4PolyhedralBoundedSolidDebugHUDRenderer`. C++
port note: the texts are written on an `OpenGL4LabelCanvas` (rasterized by
an `OpenGL4LabelImageProvider`) instead of a `java.awt.Graphics2D`.
*/
class OpenGL4PolyhedralBoundedSolidDebugHUDRenderer {
public:
    static void drawSelectedFaceLabel(
        OpenGL4LabelCanvas* g,
        PolyhedralBoundedSolid* solid,
        int faceIndex,
        Camera* camera,
        int viewportWidth,
        int viewportHeight);

    static void drawDebugVertexLabels(
        OpenGL4LabelCanvas* g,
        PolyhedralBoundedSolid* solid,
        Camera* camera,
        int viewportWidth,
        int viewportHeight);

private:
    OpenGL4PolyhedralBoundedSolidDebugHUDRenderer() {}
};

#endif

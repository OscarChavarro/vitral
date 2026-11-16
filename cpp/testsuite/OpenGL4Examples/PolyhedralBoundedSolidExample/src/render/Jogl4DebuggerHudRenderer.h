#ifndef __POLYHEDRAL_EXAMPLE_JOGL4_DEBUGGER_HUD_RENDERER__
#define __POLYHEDRAL_EXAMPLE_JOGL4_DEBUGGER_HUD_RENDERER__

#include "java/lang/String.h"

class DebuggerModel;
class OpenGL4LabelCanvas;
class OpenGL4LabelImageProvider;

/**
C++ port of Java's `render.Jogl4DebuggerHudRenderer` (the Java name is
kept, as in the other OpenGL4 examples).

C++ port notes: the texts are written on an `OpenGL4LabelCanvas`, whose
rasterizer (`OpenGL4LabelImageProvider`) stands for the AWT fonts; there
are no bold fonts, so the "SansSerif" bold 18 of the HUD is a font of size
18. `fillRect` writes the overlay pixels directly.
*/
class Jogl4DebuggerHudRenderer {
public:
    Jogl4DebuggerHudRenderer(DebuggerModel* model,
        OpenGL4LabelImageProvider* labels);
    ~Jogl4DebuggerHudRenderer();

    /// Java: `init(GLAutoDrawable)`, with the size of the drawable
    void init(int surfaceWidth, int surfaceHeight);
    void updateViewportSize(int width, int height);
    void draw();
    void dispose();

private:
    DebuggerModel* model;
    OpenGL4LabelImageProvider* labels;
    /// Java: `overlayImage` and `bufferedOverlay`
    OpenGL4LabelCanvas* overlay;
    int viewportWidth;
    int viewportHeight;

    void drawHudText(OpenGL4LabelCanvas* g, int width, int height);
    void drawCsgStatisticsSummary(OpenGL4LabelCanvas* g, int height);
    void drawDebugVertexLabels(OpenGL4LabelCanvas* g);
    void drawSelectedFaceLabel(OpenGL4LabelCanvas* g);
    java::String formatFaceLoopLabel() const;
    void ensureOverlayBuffers(int width, int height);
    static void fillRect(OpenGL4LabelCanvas* g, int x, int y, int width,
        int height, int r, int gr, int b, int a);
    static void drawTopRight(OpenGL4LabelCanvas* g, int width,
        const java::String& text, int baselineY);
};

#endif

#ifndef __POLYHEDRAL_EXAMPLE_JOGL4_DEBUGGER_RENDERER__
#define __POLYHEDRAL_EXAMPLE_JOGL4_DEBUGGER_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "render/Jogl4DebuggerHudRenderer.h"

class Calligraphic2DBuffer;
class Camera;
class DebuggerModel;
class OpenGL4LabelImageProvider;
class PolyhedralBoundedSolid;
class RGBImageUncompressed;

/**
C++ port of Java's `render.Jogl4DebuggerRenderer` (the Java name is kept,
as in the other OpenGL4 examples).

C++ port notes: the `GLEventListener` methods receive the size of the
drawable instead of the `GLAutoDrawable`, and the OpenGL context is the
current one. The pending screenshot is a path (Java: a `File`, or null).
*/
class Jogl4DebuggerRenderer {
public:
    Jogl4DebuggerRenderer(DebuggerModel* model,
        OpenGL4LabelImageProvider* labels);

    void requestScreenshot(const java::String& outputFile);
    void refreshCanvasAfterWindowModeChange();

    void display(int surfaceWidth, int surfaceHeight);
    void init(int surfaceWidth, int surfaceHeight);
    void dispose();
    void reshape(int x, int y, int width, int height);

    /**
    Java: the private `captureRgbImage` of this class and of
    `Jogl4HeadlessRenderer`.
    @return a new image, owned by the caller
    */
    static RGBImageUncompressed* captureRgbImage(int width, int height);
    /// Java: the private `ensureParentFolder` of the render classes
    static void ensureParentFolder(const java::String& outputFile);

private:
    DebuggerModel* model;
    Jogl4DebuggerHudRenderer hudRenderer;
    SimpleMaterial csgOperandMaterialA;
    SimpleMaterial csgOperandMaterialB;
    bool hasPendingScreenshot;
    java::String pendingScreenshotFile;

    static SimpleMaterial createInsetMaterial(double r, double g, double b);
    static Vector3Dd solidCenter(PolyhedralBoundedSolid* solid);
    static double solidMaxExtent(PolyhedralBoundedSolid* solid);
    static Vector3Dd cameraRelativeAnchor(Camera* camera, double ndcX,
        double ndcY, double depth);
    static Matrix4x4d buildInsetModelMatrix(const Vector3Dd& anchorPoint,
        double scale, const Vector3Dd& center);
    void drawInsetSolid(PolyhedralBoundedSolid* solid,
        const SimpleMaterial& material, const Vector3Dd& anchorPoint,
        double mainSolidExtent);
    void drawCsgOperandInsets(int viewportWidth, int viewportHeight);
    static void appendSegmentedLines(const Calligraphic2DBuffer& source,
        float r, float g, float b,
        java::ArrayList<float>& positions,
        java::ArrayList<float>& colors);
    static void drawBufferedLines(const Calligraphic2DBuffer& lines,
        float r, float g, float b, float lineWidth);
    void renderLinesResult(const Calligraphic2DBuffer& contourLines,
        const Calligraphic2DBuffer& visibleLines,
        const Calligraphic2DBuffer& hiddenLines,
        bool showHiddenLines,
        bool showVisibleLines);
    void drawReferenceFrame(const Matrix4x4d& mvp);
    void drawObjectsGL(int viewportWidth, int viewportHeight);
    void exportPendingScreenshot(int width, int height);
};

#endif

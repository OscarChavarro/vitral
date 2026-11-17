#include <cmath>

#include <glad/gl.h>
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "render/Jogl4DebuggerRenderer.h"

#include "java/io/File.h"
#include "java/lang/Math.h"
#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/LightGizmoStyle.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LightRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RendererConfigurationShaderSelector.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SimpleMaterialRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidRenderer.h"
#include "models/DebuggerModel.h"

static const double HUD_INSET_DEPTH = 2.8;

Jogl4DebuggerRenderer::Jogl4DebuggerRenderer(DebuggerModel* model,
    OpenGL4LabelImageProvider* labels)
    : model(model),
      hudRenderer(model, labels),
      csgOperandMaterialA(createInsetMaterial(1.0, 0.502, 0.502)),
      csgOperandMaterialB(createInsetMaterial(0.502, 1.0, 0.502)),
      hasPendingScreenshot(false)
{
}

void Jogl4DebuggerRenderer::requestScreenshot(const java::String& outputFile)
{
    pendingScreenshotFile = outputFile;
    hasPendingScreenshot = true;
}

SimpleMaterial Jogl4DebuggerRenderer::createInsetMaterial(double r, double g,
    double b)
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.2 * r, 0.2 * g, 0.2 * b));
    m = m.withDiffuse(ColorRgb(r, g, b));
    m = m.withSpecular(ColorRgb(1.0, 1.0, 1.0));
    m = m.withDoubleSided(false);
    m = m.withPhongExponent(100);
    return m;
}

Vector3Dd Jogl4DebuggerRenderer::solidCenter(PolyhedralBoundedSolid* solid)
{
    double* minMax;

    if ( solid == nullptr ) {
        return Vector3Dd(0, 0, 0);
    }
    minMax = solid->getMinMax();
    if ( minMax == nullptr ) {
        return Vector3Dd(0, 0, 0);
    }
    Vector3Dd center(
        (minMax[0] + minMax[3]) / 2.0,
        (minMax[1] + minMax[4]) / 2.0,
        (minMax[2] + minMax[5]) / 2.0);
    delete[] minMax;
    return center;
}

double Jogl4DebuggerRenderer::solidMaxExtent(PolyhedralBoundedSolid* solid)
{
    double* minMax;
    double ex;
    double ey;
    double ez;

    if ( solid == nullptr ) {
        return 1.0;
    }
    minMax = solid->getMinMax();
    if ( minMax == nullptr ) {
        return 1.0;
    }
    ex = std::fabs(minMax[0] - minMax[3]);
    ey = std::fabs(minMax[1] - minMax[4]);
    ez = std::fabs(minMax[2] - minMax[5]);
    delete[] minMax;
    return java::Math::max(ex, java::Math::max(ey, ez));
}

Vector3Dd Jogl4DebuggerRenderer::cameraRelativeAnchor(Camera* camera,
    double ndcX, double ndcY, double depth)
{
    Vector3Dd eye = camera->getPosition();
    Vector3Dd front = camera->getFront().normalized();
    Vector3Dd up = camera->getUp().normalized();
    Vector3Dd right = camera->getLeft().multiply(-1).normalized();
    double viewportY = java::Math::max(camera->getViewportYSize(), 1e-9);
    double aspect = camera->getViewportXSize() / viewportY;
    double offsetX;
    double offsetY;
    double safeDepth = java::Math::max(depth, 1e-9);

    if ( camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        double zoom = java::Math::max(camera->getOrthogonalZoom(), 1e-9);
        offsetX = ndcX * (aspect / zoom);
        offsetY = ndcY * (1.0 / zoom);
    }
    else {
        double halfHeight = safeDepth *
            std::tan(java::Math::toRadians(camera->getFov() / 2.0));
        double halfWidth = halfHeight * aspect;
        offsetX = ndcX * halfWidth;
        offsetY = ndcY * halfHeight;
    }

    return eye.add(front.multiply(safeDepth))
              .add(right.multiply(offsetX))
              .add(up.multiply(offsetY));
}

Matrix4x4d Jogl4DebuggerRenderer::buildInsetModelMatrix(
    const Vector3Dd& anchorPoint, double scale, const Vector3Dd& center)
{
    return Matrix4x4d()
        .translation(anchorPoint)
        .multiply(Matrix4x4d().scale(scale, scale, scale)
            .multiply(Matrix4x4d().translation(-center.x(), -center.y(),
                -center.z())));
}

void Jogl4DebuggerRenderer::drawInsetSolid(PolyhedralBoundedSolid* solid,
    const SimpleMaterial& material, const Vector3Dd& anchorPoint,
    double mainSolidExtent)
{
    Vector3Dd center;
    double extent;
    double scale;

    if ( solid == nullptr ) {
        return;
    }
    center = solidCenter(solid);
    extent = solidMaxExtent(solid);
    if ( extent < 1e-12 ) {
        extent = 1.0;
    }
    if ( mainSolidExtent < 1e-12 ) {
        mainSolidExtent = 1.0;
    }
    scale = 0.75 * (mainSolidExtent / extent);

    Matrix4x4d modelMatrix = buildInsetModelMatrix(anchorPoint, scale, center);
    OpenGL4SimpleMaterialRenderer::activate(&material);
    OpenGL4PolyhedralBoundedSolidRenderer::draw(solid, model->getCamera(),
        model->getQuality(), modelMatrix);
}

void Jogl4DebuggerRenderer::drawCsgOperandInsets(int viewportWidth,
    int viewportHeight)
{
    PolyhedralBoundedSolid* operandA = model->getCsgPreviewOperandA();
    PolyhedralBoundedSolid* operandB = model->getCsgPreviewOperandB();
    PolyhedralBoundedSolid* mainSolid = model->getSolid();
    Camera* camera = model->getCamera();
    double mainExtent;
    Vector3Dd leftAnchor;
    Vector3Dd rightAnchor;

    if ( operandA == nullptr || operandB == nullptr || mainSolid == nullptr ) {
        return;
    }
    if ( viewportWidth <= 0 || viewportHeight <= 0 ) {
        return;
    }
    mainExtent = solidMaxExtent(mainSolid);

    leftAnchor = cameraRelativeAnchor(camera, -0.76, -0.76,
        HUD_INSET_DEPTH);
    rightAnchor = cameraRelativeAnchor(camera, 0.76, -0.76,
        HUD_INSET_DEPTH);

    drawInsetSolid(operandA, csgOperandMaterialA, leftAnchor, mainExtent);
    drawInsetSolid(operandB, csgOperandMaterialB, rightAnchor, mainExtent);
    OpenGL4SimpleMaterialRenderer::activate(model->getMaterial());
}

/**
C++ port note: Java revalidates and repaints the AWT frame and canvas after
a full screen switch; a GLFW window only needs a new frame.
*/
void Jogl4DebuggerRenderer::refreshCanvasAfterWindowModeChange()
{
    if ( model->getCanvas() == nullptr ) {
        return;
    }
    glfwPostEmptyEvent();
}

void Jogl4DebuggerRenderer::appendSegmentedLines(
    const Calligraphic2DBuffer& source,
    float r,
    float g,
    float b,
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors)
{
    int i;
    for ( i = 0; i < source.getNumLines(); i++ ) {
        Vector3Dd p0 = *source.get2DLinePoint0(i);
        Vector3Dd p1 = *source.get2DLinePoint1(i);
        positions.add((float)p0.x());
        positions.add((float)p0.y());
        positions.add((float)p0.z());
        positions.add((float)p1.x());
        positions.add((float)p1.y());
        positions.add((float)p1.z());
        int v;
        for ( v = 0; v < 2; v++ ) {
            colors.add(r);
            colors.add(g);
            colors.add(b);
        }
    }
}

void Jogl4DebuggerRenderer::drawBufferedLines(const Calligraphic2DBuffer& lines,
    float r,
    float g,
    float b,
    float lineWidth)
{
    java::ArrayList<float> positions;
    java::ArrayList<float> colors;

    appendSegmentedLines(lines, r, g, b, positions, colors);
    if ( positions.isEmpty() ) {
        return;
    }
    OpenGL4LineRenderer::drawLines(Matrix4x4d::identityMatrix(),
        positions, colors, lineWidth, -4.0e-4f);
}

void Jogl4DebuggerRenderer::renderLinesResult(
    const Calligraphic2DBuffer& contourLines,
    const Calligraphic2DBuffer& visibleLines,
    const Calligraphic2DBuffer& hiddenLines,
    bool showHiddenLines,
    bool showVisibleLines)
{
    if ( showHiddenLines ) {
        drawBufferedLines(hiddenLines, 0.7f, 0.7f, 0.7f, 1.0f);
    }
    if ( showVisibleLines ) {
        drawBufferedLines(visibleLines, 0.0f, 0.0f, 0.0f, 4.0f);
    }
    drawBufferedLines(contourLines, 0.0f, 0.0f, 0.0f, 8.0f);
}

void Jogl4DebuggerRenderer::drawReferenceFrame(const Matrix4x4d& mvp)
{
    if ( model->getEdgeIndex() <= -3 || !model->isShowCoordinateSystem() ) {
        return;
    }
    static const float POSITIONS[] = {
        0, 0, 0, 1, 0, 0,
        0, 0, 0, 0, 1, 0,
        0, 0, 0, 0, 0, 1
    };
    static const float COLORS[] = {
        1, 0, 0, 1, 0, 0,
        0, 1, 0, 0, 1, 0,
        0, 0, 1, 0, 0, 1
    };
    java::ArrayList<float> positions;
    java::ArrayList<float> colors;
    int i;
    for ( i = 0; i < 18; i++ ) {
        positions.add(POSITIONS[i]);
        colors.add(COLORS[i]);
    }
    OpenGL4LineRenderer::drawLines(mvp, positions, colors, 3.0f, -3.0e-4f);
}

void Jogl4DebuggerRenderer::drawObjectsGL(int /*viewportWidth*/,
    int /*viewportHeight*/)
{
    if ( model->getSolid() == nullptr ) {
        return;
    }
    Matrix4x4d modelMatrix = model->getSolidModelMatrix();
    Matrix4x4d mvp = model->getCamera()->calculateProjectionMatrix()
        .multiply(modelMatrix);

    OpenGL4SimpleMaterialRenderer::activate(model->getMaterial());
    OpenGL4LightRenderer::activate(model->getLight1());
    OpenGL4LightRenderer::draw(model->getLight1(), model->getCamera(),
        LightGizmoStyle::OMNI_BILLBOARD);
    OpenGL4LightRenderer::activate(model->getLight2());
    OpenGL4LightRenderer::draw(model->getLight2(), model->getCamera(),
        LightGizmoStyle::OMNI_BILLBOARD);
    OpenGL4PolyhedralBoundedSolidRenderer::draw(model->getSolid(),
        model->getCamera(), model->getQuality(), modelMatrix);

    drawReferenceFrame(mvp);
    OpenGL4PolyhedralBoundedSolidRenderer::drawDebugFaceBoundary(
        model->getSolid(), model->getFaceIndex(), mvp);
    OpenGL4PolyhedralBoundedSolidRenderer::drawDebugFace(model->getSolid(),
        model->getFaceIndex(), modelMatrix, mvp, model->getCamera());

    if ( model->isDebugEdges() && model->getEdgeIndex() > -3 ) {
        OpenGL4PolyhedralBoundedSolidRenderer::drawDebugEdges(
            model->getSolid(), model->getCamera(), model->getEdgeIndex(), mvp);
    }
    else if ( model->getEdgeIndex() == -3 ||
              model->getAppelDisplayMode() != AppelDisplayMode::OFF ) {
        Calligraphic2DBuffer contourLines;
        Calligraphic2DBuffer visibleLines;
        Calligraphic2DBuffer hiddenLines;
        java::ArrayList<SimpleBody*> bodyArray;
        SimpleBody body;

        // C++ port note: the solid stays owned by the model
        body.setGeometryReference(model->getSolid());
        body.setPosition(Vector3Dd());
        body.setRotation(modelMatrix);
        body.setRotationInverse(modelMatrix.inverse());
        bodyArray.add(&body);
        HiddenLineRenderer::executeAppelAlgorithm(bodyArray, model->getCamera(),
            &contourLines, &visibleLines, &hiddenLines);
        AppelDisplayMode displayMode = model->getAppelDisplayMode();
        bool showHiddenLines =
            displayMode == AppelDisplayMode::EDGES_VISIBLE_HIDDEN;
        bool showVisibleLines =
            displayMode != AppelDisplayMode::EDGES_ONLY;
        renderLinesResult(contourLines, visibleLines, hiddenLines,
            showHiddenLines, showVisibleLines);
    }
}

/** Called by drawable to initiate drawing */
void Jogl4DebuggerRenderer::display(int surfaceWidth, int surfaceHeight)
{
    glClearColor(0.5f, 0.5f, 0.5f, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glEnable(GL_DEPTH_TEST);

    drawObjectsGL(surfaceWidth, surfaceHeight);
    glClear(GL_DEPTH_BUFFER_BIT);
    drawCsgOperandInsets(surfaceWidth, surfaceHeight);
    if ( model->isHudEnabled() ) {
        hudRenderer.draw();
    }
    exportPendingScreenshot(surfaceWidth, surfaceHeight);
}

void Jogl4DebuggerRenderer::init(int surfaceWidth, int surfaceHeight)
{
    hudRenderer.init(surfaceWidth, surfaceHeight);
}

/**
C++ port note: besides the Java releases, the light gizmo objects are
released too (Java's `Jogl4LightRenderer` builds them again in each new
context; the C++ one keeps their names).
*/
void Jogl4DebuggerRenderer::dispose()
{
    OpenGL4LineRenderer::release();
    OpenGL4PolyhedralBoundedSolidRenderer::release();
    OpenGL4ImageRenderer::dispose();
    OpenGL4RendererConfigurationShaderSelector::dispose();
    OpenGL4LightRenderer::dispose();
    hudRenderer.dispose();
}

/** Called to indicate the drawing surface has been moved and/or resized */
void Jogl4DebuggerRenderer::reshape(int /*x*/, int /*y*/, int width, int height)
{
    glViewport(0, 0, width, height);

    model->getCamera()->updateViewportResize(width, height);
    hudRenderer.updateViewportSize(width, height);
}

void Jogl4DebuggerRenderer::exportPendingScreenshot(int width, int height)
{
    if ( !hasPendingScreenshot || width <= 0 || height <= 0 ) {
        return;
    }
    java::String outputFile = pendingScreenshotFile;

    hasPendingScreenshot = false;
    glFinish();
    RGBImageUncompressed* image = captureRgbImage(width, height);
    ensureParentFolder(outputFile);
    ImagePersistence::exportPNG(java::File(outputFile), image);
    delete image;
    java::System::out.println((java::String("[PolyhedralBoundedSolidExample] Exported ") +
        outputFile).c_str());
}

RGBImageUncompressed* Jogl4DebuggerRenderer::captureRgbImage(int width,
    int height)
{
    unsigned char* bb = new unsigned char[(size_t)3 * width * height];
    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, bb);

    RGBImageUncompressed* image = new RGBImageUncompressed();
    image->init(width, height);

    size_t pos = 0;
    int x;
    int y;
    for ( y = image->getYSize() - 1; y >= 0; y-- ) {
        for ( x = 0; x < image->getXSize(); x++ ) {
            image->putPixel(x, y, (char)bb[pos], (char)bb[pos + 1],
                (char)bb[pos + 2]);
            pos += 3;
        }
    }
    delete[] bb;
    return image;
}

void Jogl4DebuggerRenderer::ensureParentFolder(const java::String& outputFile)
{
    // Java: `outputFile.getParentFile()`
    int slash = outputFile.rfind('/');
    if ( slash <= 0 ) {
        return;
    }
    java::File parent(outputFile.substring(0, slash));
    if ( !parent.exists() ) {
        parent.mkdirs();
    }
}

#include "vsdk/toolkit/common/VSDK.h"
#include "render/xlib/XlibDrawingAreaRenderer.h"
#include "vsdk/toolkit/gui/XFontSelection.h"

// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include "vsdk/toolkit/gui/XlibUtf8Text.h"
#include <math.h>
#include <stdio.h>

#include "java/util/ArrayList.txx"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/Scene.h"
#include "model/selection/SceneSelectionEditor.h"
#include "render/DrawingAreaGizmoPresenter.h"
#include "render/DrawingAreaHost.h"
#include "render/FrameBufferSource.h"
#include "render/FrameCaptureService.h"
#include "render/BrepExportCache.h"
#include "render/ProjectedSurfacesViewBuilder.h"
#include "render/xlib/XlibProjectedPolygonRenderer.h"
#include "java/lang/RuntimeException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryCalligraphicExporter.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/media/Calligraphic3DBuffer.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"
#include "vsdk/toolkit/render/hiddenLine/WireframeRenderer.h"
#include "vsdk/toolkit/render/gizmo/CameraControlGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/render/gizmo/InputGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/RotateGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/ScaleGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/TranslateGizmo2DPainter.h"
#include "vsdk/toolkit/render/xlib/XlibGizmo2DDrawListRenderer.h"
#include "vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.h"
#include "java/lang/Math.h"


static unsigned long pixel(Visual* visual, const ColorRgb& color)
{
    return XlibRGBImageUncompressedRenderer::rgbToPixel(visual,
        ((int)(Math::max(0.0, Math::min(1.0, color.r())) * 255)),
        ((int)(Math::max(0.0, Math::min(1.0, color.g())) * 255)),
        ((int)(Math::max(0.0, Math::min(1.0, color.b())) * 255)));
}

static int channel(unsigned long value, unsigned long mask)
{
    if (!mask) return 0;
    int shift = 0;
    while (!(mask & 1)) { mask >>= 1; ++shift; }
    return ((int)(((value >> shift) & mask) * 255 / mask));
}

class XlibFrameBufferSource : public FrameBufferSource {
    Display* display;
    Drawable frame;
    Visual* visual;
    int x, y, width, height;
public:
    XlibFrameBufferSource(Display* d, Drawable f, Visual* v,
                          int x0, int y0, int w, int h)
        : display(d), frame(f), visual(v), x(x0), y(y0), width(w), height(h) {}

    RGBImageUncompressed* readColor()
    {
        RGBImageUncompressed* result = new RGBImageUncompressed();
        result->init(width, height);
        XImage* image = XGetImage(display, frame, x, y, width, height,
                                  AllPlanes, ZPixmap);
        if (!image) return result;
        for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                unsigned long p = XGetPixel(image, col, row);
                result->putPixel(col, row,
                    ((char)(channel(p, visual->red_mask))),
                    ((char)(channel(p, visual->green_mask))),
                    ((char)(channel(p, visual->blue_mask))));
            }
        }
        XDestroyImage(image);
        return result;
    }
    ZBuffer* readDepth() { return NULL; }
};

static void drawLines(Display* display, Drawable frame, GC gc,
               const Calligraphic2DBuffer& lines, int x, int y,
               int width, int height, unsigned long color,
               int lineWidth = 1, BOOLEAN dashed = FALSE)
{
    // Consecutive segments where one starts at the end of the previous one
    // (i.e. the polyline approximating a curve, or a line broken by the
    // hidden line algorithm) are drawn as one polyline, so the pattern of a
    // dashed line continues along it instead of starting again at each
    // (maybe very short) segment (as the Java `AwtProjectedLineLayerRenderer`)
    const double CHAIN_TOLERANCE = 0.01;
    ArrayList<XPoint> chain;
    double lastX = 0;
    double lastY = 0;
    BOOLEAN hasLast = FALSE;
    XPoint point;

    XSetForeground(display, gc, color);
    XSetLineAttributes(display, gc, lineWidth,
        dashed ? LineOnOffDash : LineSolid, CapRound, JoinRound);
    for (int i = 0; i < lines.getNumLines(); ++i) {
        Vector3Dd* a = lines.get2DLinePoint0(i);
        Vector3Dd* b = lines.get2DLinePoint1(i);
        double ax = x + (a->x() + 1) * width / 2;
        double ay = y + (1 - a->y()) * height / 2;
        double bx = x + (b->x() + 1) * width / 2;
        double by = y + (1 - b->y()) * height / 2;
        if (!hasLast || Math::abs(ax - lastX) > CHAIN_TOLERANCE ||
            Math::abs(ay - lastY) > CHAIN_TOLERANCE) {
            if (chain.size() > 1) {
                XDrawLines(display, frame, gc, &chain[0], (int)chain.size(),
                    CoordModeOrigin);
            }
            chain.clear();
            point.x = (short)((int)ax);
            point.y = (short)((int)ay);
            chain.add(point);
        }
        point.x = (short)((int)bx);
        point.y = (short)((int)by);
        chain.add(point);
        lastX = bx;
        lastY = by;
        hasLast = TRUE;
        delete a;
        delete b;
    }
    if (chain.size() > 1) {
        XDrawLines(display, frame, gc, &chain[0], (int)chain.size(),
            CoordModeOrigin);
    }
    XSetLineAttributes(display, gc, 1, LineSolid, CapButt, JoinMiter);
}

static BOOLEAN projectLine(const Matrix4x4d& projection, const Vector3Dd& a,
                 const Vector3Dd& b, Calligraphic2DBuffer& lines)
{
    Vector4Dd p = projection.multiply(Vector4Dd(a));
    Vector4Dd q = projection.multiply(Vector4Dd(b));
    const double planes[6][4] = {
        {1,0,0,1}, {-1,0,0,1}, {0,1,0,1}, {0,-1,0,1},
        {0,0,1,1}, {0,0,-1,1}
    };
    for (int i = 0; i < 6; ++i) {
        const double* n = planes[i];
        double dp = n[0]*p.x()+n[1]*p.y()+n[2]*p.z()+n[3]*p.w();
        double dq = n[0]*q.x()+n[1]*q.y()+n[2]*q.z()+n[3]*q.w();
        if (dp < 0 && dq < 0) return FALSE;
        if (dp < 0 || dq < 0) {
            double t = dp / (dp - dq);
            Vector4Dd cut = p.multiply(1-t).add(q.multiply(t));
            if (dp < 0) p = cut; else q = cut;
        }
    }
    p = p.dividedByW();
    q = q.dividedByW();
    lines.add2DLine(p.x(), p.y(), q.x(), q.y());
    return TRUE;
}

/**
A set of free lines of the drawing views (lines that are not edges of
solids: the reference grid, curves and surfaces), in world coordinates,
drawn with its own color (as the `FreeLineSet` of the Java
`ProjectedLinesViewBuilder`).
*/
class _XlibFreeLineSet {
public:
    Calligraphic3DBuffer lines;
    ColorRgb color;

    _XlibFreeLineSet(const ColorRgb& color) : lines(), color(color) {}
};

static const double ORTHOGONAL_EYE_DISTANCE = 1000.0;

static ColorRgb wireColor(const SimpleMaterial* material)
{
    if (material == NULL) {
        return ColorRgb(0, 0, 0);
    }
    return material->getDiffuse();
}

/**
The reference grid, as two free line sets (lines and axes), so it is hidden
by the solids in the hidden line views.
*/
static void addGrid(ArrayList<_XlibFreeLineSet*>& freeLines, Camera* camera)
{
    Matrix4x4d transform;
    if (camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL) {
        Matrix4x4d rotation = camera->getRotation();
        double yaw = rotation.obtainEulerYawAngle() * 180.0 / M_PI;
        double pitch = rotation.obtainEulerPitchAngle() * 180.0 / M_PI;
        if (pitch > -45 && pitch < 45) {
            if ((yaw > 45 && yaw < 135) || (yaw < -45 && yaw > -135))
                transform = transform.axisRotation(M_PI / 2, 1, 0, 0);
            else
                transform = transform.axisRotation(M_PI / 2, 0, 1, 0);
        }
    }
    _XlibFreeLineSet* grid = new _XlibFreeLineSet(ColorRgb(.37,.37,.37));
    _XlibFreeLineSet* axes = new _XlibFreeLineSet(ColorRgb(0,0,0));
    for (int i = -7; i <= 7; ++i) {
        _XlibFreeLineSet* target = i == 0 ? axes : grid;
        target->lines.add3DLine(transform.multiply(Vector3Dd(i,-7,0)),
                                transform.multiply(Vector3Dd(i,7,0)));
        target->lines.add3DLine(transform.multiply(Vector3Dd(-7,i,0)),
                                transform.multiply(Vector3Dd(7,i,0)));
    }
    freeLines.add(grid);
    freeLines.add(axes);
}

/**
The point of an infinite plane viewed by the camera, in the coordinates of
the plane: where the line of sight crosses it (if it does, in front of the
camera and nearer than the far plane), or else the projection of the eye
onto it.
*/
static Vector3Dd viewedPoint(const InfinitePlane& plane,
                             const Matrix4x4d& transform, Camera* camera)
{
    Matrix4x4d inverse = transform.inverse();
    Vector3Dd eye = inverse.multiply(camera->getPosition());
    Vector3Dd direction = inverse.multiply(
        camera->getPosition().add(camera->getFront())).subtract(eye);
    double towards = plane.getCoefficients().dotProduct(direction);
    double length = direction.length();
    if (towards != 0 && length > 0) {
        double t = -plane.pointDistance(eye) / towards;
        if (t > 0 && t * length <= camera->getFarPlaneDistance()) {
            return eye.add(direction.multiply(t));
        }
    }
    return plane.projectPoint(eye);
}

static Calligraphic3DBuffer* exportToLines(Geometry* geometry,
                                           const Matrix4x4d& transform,
                                           Camera* camera)
{
    if (geometry->instanceOf(InfinitePlane::CLASS)) {
        InfinitePlane* plane = (InfinitePlane*)geometry;
        return GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(
            *plane, viewedPoint(*plane, transform, camera));
    }
    return GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(geometry);
}

static void addFreeLines(ArrayList<_XlibFreeLineSet*>& freeLines,
                         const Calligraphic3DBuffer& local, SimpleBody* body)
{
    Matrix4x4d transform = body->getTransformationMatrix();
    _XlibFreeLineSet* set = new _XlibFreeLineSet(wireColor(body->getMaterial()));
    Vector3Dd ends[2];
    for (int i = 0; i < local.getNumLines(); ++i) {
        local.get3DLine(i, ends);
        set->lines.add3DLine(transform.multiply(ends[0]),
                             transform.multiply(ends[1]));
    }
    freeLines.add(set);
}

/**
A body with the boundary representation of the volume of a body, at the
same place (the body itself when its geometry is the solid).
C++ port note: the surrogate body, owned by the caller, does not own the
solid.
*/
static SimpleBody* brepBody(SimpleBody* body, PolyhedralBoundedSolid* brep)
{
    if (body->getGeometry() == (Geometry*)brep) {
        return body;
    }
    SimpleBody* surrogate = new SimpleBody();
    surrogate->setName(body->getName());
    surrogate->setGeometryReference(brep);
    surrogate->setPosition(body->getPosition());
    surrogate->setRotation(body->getRotation());
    surrogate->setScale(body->getScale());
    return surrogate;
}

XlibDrawingAreaRenderer::XlibDrawingAreaRenderer(
    ApplicationModel* m, DrawingAreaHost* h,
    Consumer<Viewport*>* selected,
    TranslateGizmo* translationGizmo, RotateGizmo* rotateGizmo,
    ScaleGizmo* scaleGizmo)
    : model(m), host(h), frameCapture(new FrameCaptureService(m,h)),
      selectedListener(selected), fontDisplay(NULL), titleFont(NULL),
      titleFontSize(0),
      selectionEditor(new SceneSelectionEditor(m->getScene())),
      gizmoPresenter(NULL), gizmoDrawList(new Gizmo2DDrawList()),
      gizmoRenderer(new XlibGizmo2DDrawListRenderer()),
      cameraControlDrawList(new Gizmo2DDrawList()),
      cameraControlPainter(new CameraControlGizmo2DPainter()),
      brepCache(new BrepExportCache()),
      surfacesBuilder(new ProjectedSurfacesViewBuilder())
{
    gizmoPresenter = new DrawingAreaGizmoPresenter(m->getDrawingArea(),
        selectionEditor, translationGizmo, rotateGizmo, scaleGizmo);
}

XlibDrawingAreaRenderer::~XlibDrawingAreaRenderer()
{
    if (titleFont && fontDisplay) XFreeFontSet(fontDisplay, titleFont);
    delete surfacesBuilder;
    delete brepCache;
    delete cameraControlPainter;
    delete cameraControlDrawList;
    delete gizmoRenderer;
    delete gizmoDrawList;
    delete gizmoPresenter;
    delete selectionEditor;
    delete frameCapture;
}

void XlibDrawingAreaRenderer::drawGizmos(Display* display, Drawable frame,
    GC gc, Visual* visual, Viewport* viewport, int x, int y, int width,
    int height)
{
    Camera* camera = model->getScene()->activeCamera;
    ViewportSet* set = model->getDrawingArea()->getViewportSet();
    DrawingAreaGizmoPresenter::GizmoKind kind;

    gizmoRenderer->setDisplay(display);
    gizmoDrawList->clear();
    kind = gizmoPresenter->prepareForView(camera);
    if (kind == DrawingAreaGizmoPresenter::GizmoKind::TRANSLATE)
        TranslateGizmo2DPainter::paint(gizmoDrawList,
            gizmoPresenter->getTranslationGizmo(), camera,
            set->getElementScaler());
    else if (kind == DrawingAreaGizmoPresenter::GizmoKind::ROTATE)
        RotateGizmo2DPainter::paint(gizmoDrawList,
            gizmoPresenter->getRotateGizmo(), camera,
            set->getElementScaler());
    else if (kind == DrawingAreaGizmoPresenter::GizmoKind::SCALE)
        ScaleGizmo2DPainter::paint(gizmoDrawList,
            gizmoPresenter->getScaleGizmo(), camera);

    // Only the selected viewport shows them: it is the one that receives
    // the keyboard
    InputGizmo* inputGizmo = gizmoPresenter->getInputGizmo();
    if (inputGizmo == NULL && model->getRayGizmo()->isVisible())
        inputGizmo = model->getRayGizmo()->getInputGizmo();
    if (inputGizmo != NULL && set->isSelected(viewport))
        InputGizmo2DPainter::paint(gizmoDrawList, inputGizmo, width, height,
            set->getElementScaler(), *gizmoRenderer);
    gizmoRenderer->draw(frame, gc, visual, *gizmoDrawList, x, y, width,
                        height);

    // The HUD of the selected viewport is over everything
    if (set->isSelected(viewport)) {
        CameraControlGizmo* cameraControl = model->getCameraControlGizmo();

        cameraControlDrawList->clear();
        cameraControl->placeInViewport(viewport, set->getElementScaler());
        cameraControlPainter->paint(cameraControlDrawList, cameraControl,
            set->getElementScaler());
        gizmoRenderer->draw(frame, gc, visual, *cameraControlDrawList, x, y,
                            width, height);
    }
}

void XlibDrawingAreaRenderer::ensureTitleFont(Display* display, int size)
{
    if (titleFont && fontDisplay == display && titleFontSize == size) return;
    if (titleFont && fontDisplay) XFreeFontSet(fontDisplay, titleFont);
    fontDisplay = display;
    titleFontSize = size;
    titleFont = XFontSelection::createSansFontSet(display, size, FALSE);
}

void XlibDrawingAreaRenderer::drawTitle(Display* display, Drawable frame,
    GC gc, Visual* visual, Viewport* viewport, int x, int y)
{
    ViewportSet* set = model->getDrawingArea()->getViewportSet();
    int borderX = set->getElementScaler()->scaleSize(4);
    int borderY = set->getElementScaler()->scaleSize(1);
    ensureTitleFont(display, set->getElementScaler()->scaleSize(14));
    String title = set->getTitleFor(viewport);
    int textWidth = ((int)(title.length())) * 8;
    int textHeight = titleFontSize;
    int baseline = y + borderY + textHeight;
    if (titleFont) {
        XRectangle ink, logical;
        Xutf8TextExtents(titleFont, title.c_str(), title.length(), &ink, &logical);
        textWidth = logical.width;
        textHeight = logical.height;
        baseline = y + borderY - logical.y;
    }
    viewport->setTitleArea(0, 0, textWidth + 2*borderX,
                           textHeight + 2*borderY);
    XSetForeground(display, gc, pixel(visual, set->getTitleColorFor(viewport)));
    if (titleFont)
        Xutf8DrawString(display, frame, titleFont, gc, x+borderX, baseline,
                        title.c_str(), title.length());
    else
        XDrawString(display, frame, gc, x+borderX, baseline,
                    title.c_str(), title.length());
}

void XlibDrawingAreaRenderer::drawView(Display* display, Drawable frame,
    GC gc, Visual* visual, int depth, Viewport* viewport,
    int x, int y, int width, int height)
{
    if (width <= 0 || height <= 0) return;
    Scene* scene = model->getScene();
    Camera* camera = viewport->getActiveCamera();
    RendererConfiguration* quality = viewport->getRendererConfiguration();
    scene->activeCamera = camera;
    scene->qualityTemplate->cloneFrom(*quality);
    BOOLEAN raytraced = viewport->getRenderMode() == Viewport::RENDER_MODE_RAYTRACING;
    BOOLEAN hiddenSurfaces =
        viewport->getRenderMode() == Viewport::RENDER_MODE_HIDDEN_SURFACES;

    if (raytraced) {
        scene->activateSelectedBackground();
        model->setRaytracedImageWidth(width);
        model->setRaytracedImageHeight(height);
        host->raytraceImage();
        RGBImageUncompressed* image = model->getRaytracedImage();
        if (image && image->getXSize() == width && image->getYSize() == height)
            XlibRGBImageUncompressedRenderer::draw(
                display, frame, gc, visual, depth, *image, x, y);
    }
    else {
        XSetForeground(display, gc, pixel(visual,
            scene->simpleBackground->colorInDireccion(camera->getFront())));
        XFillRectangle(display, frame, gc, x, y, width, height);
    }

    if (hiddenSurfaces) {
        drawHiddenSurfaces(display, frame, gc, visual, viewport, x, y, width,
                           height);
    }

    ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    if (!raytraced && !hiddenSurfaces) {
        // As the Java `ProjectedLinesViewBuilder`: the volumes are drawn by
        // the hidden line algorithm over their boundary representation; the
        // curves and surfaces (and the grid) are free lines, hidden by the
        // solids but not hiding anything; other bodies, or all of them when
        // the surfaces are not shown, are drawn as wireframes
        ArrayList<_XlibFreeLineSet*> freeLines;
        ArrayList<SimpleBody*> solids, wires;
        ArrayList<SimpleBody*> surrogates;
        ArrayList<PolyhedralBoundedSolid*> exported;
        long i;
        if (viewport->isShowGrid())
            addGrid(freeLines, camera);
        for (i = 0; i < bodies.size(); ++i) {
            SimpleBody* body = bodies[i];
            if (!body || !body->getGeometry() || scene->isHidden(body)) continue;
            Geometry* geometry = body->getGeometry();
            BOOLEAN callerOwns = FALSE;
            PolyhedralBoundedSolid* brep = brepCache->get(geometry, callerOwns);
            if (brep != NULL && callerOwns) exported.add(brep);
            Calligraphic3DBuffer* lines = NULL;
            if (quality->isSurfacesSet() && brep == NULL) {
                lines = exportToLines(geometry,
                    body->getTransformationMatrix(), camera);
            }
            if (quality->isSurfacesSet() && brep != NULL) {
                SimpleBody* solid = brepBody(body, brep);
                if (solid != body) surrogates.add(solid);
                solids.add(solid);
            }
            else if (lines != NULL) {
                addFreeLines(freeLines, *lines, body);
            }
            else {
                wires.add(body);
            }
            delete lines;
        }
        if (wires.size()) {
            Calligraphic2DBuffer lines;
            WireframeRenderer::execute(&lines, wires, camera);
            drawLines(display, frame, gc, lines, x, y, width, height,
                      pixel(visual, ColorRgb(0,0,0)));
        }
        if (solids.size()) {
            Calligraphic2DBuffer contours, visible, hidden;
            ArrayList<Calligraphic3DBuffer*> freeInput;
            ArrayList<Calligraphic2DBuffer*> freeVisible, freeHidden;
            for (i = 0; i < freeLines.size(); ++i) {
                freeInput.add(&freeLines[i]->lines);
                freeVisible.add(new Calligraphic2DBuffer());
                freeHidden.add(new Calligraphic2DBuffer());
            }
            Camera eye(*camera);
            if (camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL) {
                Vector3Dd front = camera->getFront().normalized();
                eye.setPosition(camera->getPosition().subtract(
                    front.multiply(ORTHOGONAL_EYE_DISTANCE)));
                eye.setNearPlaneDistance(camera->getNearPlaneDistance() +
                    ORTHOGONAL_EYE_DISTANCE);
                eye.setFarPlaneDistance(camera->getFarPlaneDistance() +
                    ORTHOGONAL_EYE_DISTANCE);
            }
            HiddenLineRenderer::executeAppelAlgorithm(solids, &eye,
                &contours, &visible, &hidden, freeInput, freeVisible,
                freeHidden);
            if (quality->isWiresSet()) {
                for (i = 0; i < freeLines.size(); ++i) {
                    drawLines(display, frame, gc, *freeHidden[i], x, y, width,
                              height, pixel(visual, freeLines[i]->color), 1,
                              TRUE);
                }
                drawLines(display, frame, gc, hidden, x, y, width, height,
                          pixel(visual, ColorRgb(.32,.32,.32)), 1, TRUE);
            }
            for (i = 0; i < freeLines.size(); ++i) {
                drawLines(display, frame, gc, *freeVisible[i], x, y, width,
                          height, pixel(visual, freeLines[i]->color));
                delete freeVisible[i];
                delete freeHidden[i];
            }
            drawLines(display, frame, gc, visible, x, y, width, height,
                      pixel(visual, ColorRgb(0,0,0)));
            drawLines(display, frame, gc, contours, x, y, width, height,
                      pixel(visual, ColorRgb(0,0,0)), 2);
        }
        else {
            Matrix4x4d projection = camera->calculateProjectionMatrix();
            Vector3Dd ends[2];
            for (i = 0; i < freeLines.size(); ++i) {
                Calligraphic2DBuffer projected;
                for (int j = 0; j < freeLines[i]->lines.getNumLines(); ++j) {
                    freeLines[i]->lines.get3DLine(j, ends);
                    projectLine(projection, ends[0], ends[1], projected);
                }
                drawLines(display, frame, gc, projected, x, y, width, height,
                          pixel(visual, freeLines[i]->color));
            }
        }
        for (i = 0; i < freeLines.size(); ++i) delete freeLines[i];
        for (i = 0; i < surrogates.size(); ++i) delete surrogates[i];
        for (i = 0; i < exported.size(); ++i) delete exported[i];
    }

    // Must be the last to draw, over the scene
    drawGizmos(display, frame, gc, visual, viewport, x, y, width, height);

    XlibFrameBufferSource source(display, frame, visual, x, y, width, height);
    frameCapture->copyColorBufferIfNeeded(&source,
        model->getDrawingArea()->getViewportSet()->isSelected(viewport));
}

void XlibDrawingAreaRenderer::drawHiddenSurfaces(Display* display,
    Drawable frame, GC gc, Visual* visual, Viewport* viewport, int x, int y,
    int width, int height)
{
    DrawingArea* area = model->getDrawingArea();
    const ArrayList<ProjectedPolygon*>* polygons = NULL;

    try {
        polygons = &surfacesBuilder->buildView(model->getScene(), viewport,
            width, height, area->getContentGeneration(),
            viewport->getRendererConfiguration()->isWithShadowsSet());
    }
    catch (const RuntimeException& e) {
        // As with the lines, a failure over some body must not break the
        // presentation of the whole set
        String message("Can not compute the visible surfaces of viewport \"");
        message += area->getViewportSet()->getTitleFor(viewport);
        message += "\": ";
        message += e.getMessage();
        Logger::reportMessage("XlibDrawingAreaRenderer", Logger::WARNING,
            "drawHiddenSurfaces", message);
        return;
    }
    XlibProjectedPolygonRenderer::draw(display, frame, gc, visual, *polygons,
                                       x, y, width, height);
}

void XlibDrawingAreaRenderer::display(Display* display, Window window,
    Visual* visual, int depth, int width, int height)
{
    if (!display || !window || !visual || width <= 0 || height <= 0) return;
    host->beforeFrame();
    DrawingArea* area = model->getDrawingArea();
    area->updateSurfaceSize(width, height);
    ViewportSet* set = area->getViewportSet();
    set->enforceAvailableRenderModes();
    if (area->isDepthCaptureRequested()) {
        area->setDepthCaptureRequested(FALSE);
        area->setContoursRequested(FALSE);
        host->showStatusMessage("Depth capture is unavailable in Xlib mode");
    }
    if (area->isProjectedViewsDebugRequested()) {
        area->setProjectedViewsDebugRequested(FALSE);
        host->showStatusMessage("Projected views debugging is unavailable in Xlib mode");
    }

    Pixmap frame = XCreatePixmap(display, window, width, height, depth);
    GC gc = XCreateGC(display, frame, 0, NULL);
    XSetForeground(display, gc, pixel(visual, ColorRgb(.77,.77,.77)));
    XFillRectangle(display, frame, gc, 0, 0, width, height);

    const ArrayList<Viewport*>& viewports = set->getViewports();
    BOOLEAN full = host->isFullScreenGuiMode() && set->countActiveViewports() == 1;
    if (!full) {
        for (long i = 0; i < viewports.size(); ++i) {
            Viewport* v = viewports[i];
            if (!v->isActive() || v->getBorder() <= 0) continue;
            int x = ((int)(v->getStartXPercent() * width));
            int y = ((int)((1-v->getStartYPercent()-v->getSizeYPercent()) * height));
            int w = ((int)(v->getSizeXPercent() * width));
            int h = ((int)(v->getSizeYPercent() * height));
            XSetForeground(display, gc, pixel(visual,
                set->isSelected(v) ? ColorRgb(1,.96,0) : ColorRgb(.21,.25,.29)));
            XFillRectangle(display, frame, gc, x, y, w, h);
        }
    }
    for (long i = 0; i < viewports.size(); ++i) {
        Viewport* v = viewports[i];
        if (!v->isActive() || (full && !set->isSelected(v))) continue;
        v->updatePixelArea(width, height);
        int x = full ? 0 : v->getPixelStartX();
        int y = full ? 0 : height-v->getPixelStartY()-v->getPixelSizeY();
        int w = full ? width : v->getPixelSizeX();
        int h = full ? height : v->getPixelSizeY();
        if (set->isSelected(v) && selectedListener) selectedListener->accept(v);
        XRectangle clip = {((short)(x)), ((short)(y)),
                           ((unsigned short)(w)),
                           ((unsigned short)(h))};
        XSetClipRectangles(display, gc, 0, 0, &clip, 1, Unsorted);
        drawView(display, frame, gc, visual, depth, v, x, y, w, h);
        drawTitle(display, frame, gc, visual, v, x, y);
        XSetClipMask(display, gc, None);
    }

    if (frameCapture->isFrameExportPending()) {
        XlibFrameBufferSource source(display, frame, visual, 0, 0, width, height);
        frameCapture->exportPendingFrame(&source);
    }
    XCopyArea(display, frame, window, gc, 0, 0, width, height, 0, 0);
    XFlush(display);
    XFreeGC(display, gc);
    XFreePixmap(display, frame);
}

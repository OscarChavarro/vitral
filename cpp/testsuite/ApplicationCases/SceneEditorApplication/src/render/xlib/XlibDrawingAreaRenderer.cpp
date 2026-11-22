#include "java/lang/Math.h"
#include "java/lang/String.h"
#include "java/util/function/Consumer.h"
#include "render/xlib/XlibDrawingAreaRenderer.h"
#include "vsdk/toolkit/gui/XFontSelection.h"

#include <X11/Xutil.h>
#include <cmath>
#include <cstdio>
#include <exception>
#include <vector>

#include "java/util/ArrayList.txx"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/Scene.h"
#include "model/selection/SceneSelectionEditor.h"
#include "render/DrawingAreaGizmoPresenter.h"
#include "render/DrawingAreaHost.h"
#include "render/FrameBufferSource.h"
#include "render/FrameCaptureService.h"
#include "render/ProjectedLineLayer.h"
#include "render/ProjectedLinesViewBuilder.h"
#include "render/ProjectedPolygon.h"
#include "render/ProjectedPolygonScanConverter.h"
#include "render/ProjectedSurfacesViewBuilder.h"
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
#include "java/lang/Exception.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/render/gizmo/CameraControlGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/gizmo/InputGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/RotateGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/ScaleGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/TranslateGizmo2DPainter.h"
#include "vsdk/toolkit/render/xlib/XlibGizmo2DDrawListRenderer.h"
#include "vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.h"

namespace {

unsigned long pixel(Visual* visual, const ColorRgb& color)
{
    return XlibRGBImageUncompressedRenderer::rgbToPixel(visual,
        static_cast<int>(java::Math::max(0.0, java::Math::min(1.0, color.r())) * 255),
        static_cast<int>(java::Math::max(0.0, java::Math::min(1.0, color.g())) * 255),
        static_cast<int>(java::Math::max(0.0, java::Math::min(1.0, color.b())) * 255));
}

int channel(unsigned long value, unsigned long mask)
{
    if (!mask) return 0;
    int shift = 0;
    while (!(mask & 1)) { mask >>= 1; ++shift; }
    return static_cast<int>(((value >> shift) & mask) * 255 / mask);
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

    RGBImageUncompressed* readColor() override
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
                    static_cast<char>(channel(p, visual->red_mask)),
                    static_cast<char>(channel(p, visual->green_mask)),
                    static_cast<char>(channel(p, visual->blue_mask)));
            }
        }
        XDestroyImage(image);
        return result;
    }
    ZBuffer* readDepth() override { return nullptr; }
};

XPoint toPoint(double x, double y)
{
    XPoint p;
    p.x = static_cast<short>(java::Math::max(-32768.0,
        java::Math::min(32767.0, static_cast<double>(std::lround(x)))));
    p.y = static_cast<short>(java::Math::max(-32768.0,
        java::Math::min(32767.0, static_cast<double>(std::lround(y)))));
    return p;
}

/**
Draws `ProjectedLineLayer`s with Xlib: the last stage of the calligraphic
pipeline, the mapping from normalized device coordinates to the pixels of a
viewport [FOLE1992].6, honoring the color, width and style of each layer
(as the Java `AwtProjectedLineLayerRenderer`). Consecutive segments where
one starts at the end of the previous one (i.e. the polyline approximating a
curve, or a line broken by the hidden line algorithm) are drawn as one
polyline, so the pattern of a dashed line continues along it instead of
starting again at each (maybe very short) segment.
*/
void drawLayers(Display* display, Drawable frame, GC gc, Visual* visual,
                const java::ArrayList<ProjectedLineLayer*>& layers,
                int x0, int y0, int width, int height, double widthScale)
{
    // Distance, in pixels, under which a segment continues the previous one
    const double CHAIN_TOLERANCE = 0.01;
    std::vector<XPoint> polyline;

    for (long l = 0; l < layers.size(); ++l) {
        const ProjectedLineLayer* layer = layers.get(l);
        const Calligraphic2DBuffer* lines = layer->getLines();
        int lineWidth = static_cast<int>(std::lround(
            java::Math::max(1.0, layer->getWidth() * widthScale)));
        XSetForeground(display, gc, pixel(visual, layer->getColor()));

        if (layer->getStyle() == ProjectedLineLayer::POINTS) {
            for (int i = 0; i < lines->getNumLines(); ++i) {
                double ax, ay, bx, by;
                lines->get2DLine(i, ax, ay, bx, by);
                double px = x0 + (ax + 1.0) / 2.0 * width;
                double py = y0 + (1.0 - ay) / 2.0 * height;
                XFillRectangle(display, frame, gc,
                    static_cast<int>(std::lround(px - lineWidth / 2.0)),
                    static_cast<int>(std::lround(py - lineWidth / 2.0)),
                    lineWidth, lineWidth);
            }
            continue;
        }
        if (layer->getStyle() == ProjectedLineLayer::DASHED) {
            char dashes[2] = {
                static_cast<char>(java::Math::min(127, 4 * lineWidth)),
                static_cast<char>(java::Math::min(127, 3 * lineWidth))
            };
            XSetDashes(display, gc, 0, dashes, 2);
            XSetLineAttributes(display, gc, lineWidth, LineOnOffDash,
                               CapButt, JoinMiter);
        }
        else {
            XSetLineAttributes(display, gc, lineWidth, LineSolid,
                               CapRound, JoinRound);
        }

        double lastX = NAN;
        double lastY = NAN;
        polyline.clear();
        for (int i = 0; i < lines->getNumLines(); ++i) {
            double ax, ay, bx, by;
            lines->get2DLine(i, ax, ay, bx, by);
            ax = x0 + (ax + 1.0) / 2.0 * width;
            ay = y0 + (1.0 - ay) / 2.0 * height;
            bx = x0 + (bx + 1.0) / 2.0 * width;
            by = y0 + (1.0 - by) / 2.0 * height;
            if (std::isnan(lastX) || std::fabs(ax - lastX) > CHAIN_TOLERANCE ||
                std::fabs(ay - lastY) > CHAIN_TOLERANCE) {
                if (polyline.size() >= 2)
                    XDrawLines(display, frame, gc, polyline.data(),
                               static_cast<int>(polyline.size()),
                               CoordModeOrigin);
                polyline.clear();
                polyline.push_back(toPoint(ax, ay));
            }
            polyline.push_back(toPoint(bx, by));
            lastX = bx;
            lastY = by;
        }
        if (polyline.size() >= 2)
            XDrawLines(display, frame, gc, polyline.data(),
                       static_cast<int>(polyline.size()), CoordModeOrigin);
    }
    XSetLineAttributes(display, gc, 1, LineSolid, CapButt, JoinMiter);
}

/**
Strokes the contours (outer and holes) of `ProjectedPolygon`s with Xlib, as
lines of the given width (as `drawContours` of the Java
`AwtProjectedPolygonRenderer`).
@param polygons polygons whose contours to draw, in order
@param x0 left pixel of the viewport
@param y0 top pixel of the viewport
@param width width of the viewport, in pixels
@param height height of the viewport, in pixels
@param lineWidth width of the lines, in pixels
@param lineColor color of every contour, or null to draw each one with the
color of its polygon
*/
void drawContours(Display* display, Drawable frame, GC gc, Visual* visual,
                  const java::ArrayList<ProjectedPolygon*>& polygons,
                  int x0, int y0, int width, int height, float lineWidth,
                  const ColorRgb* lineColor)
{
    std::vector<XPoint> path;

    XSetLineAttributes(display, gc,
        static_cast<int>(std::lround(lineWidth)), LineSolid, CapRound,
        JoinRound);
    if (lineColor != nullptr) {
        XSetForeground(display, gc, pixel(visual, *lineColor));
    }
    for (long i = 0; i < polygons.size(); i++) {
        const ProjectedPolygon* polygon = polygons.get(i);
        if (lineColor == nullptr) {
            XSetForeground(display, gc, pixel(visual, polygon->getColor()));
        }
        const Polygon2D* area = polygon->getArea();
        for (long l = 0; l < area->loops.size(); l++) {
            const _Polygon2DContour* loop = area->loops[l];
            path.clear();
            for (long v = 0; v < loop->vertices.size(); v++) {
                path.push_back(toPoint(
                    x0 + (loop->vertices[v].x + 1.0) / 2.0 * width,
                    y0 + (1.0 - loop->vertices[v].y) / 2.0 * height));
            }
            if (!path.empty()) {
                // Closes the contour
                path.push_back(path[0]);
                XDrawLines(display, frame, gc, path.data(),
                           static_cast<int>(path.size()), CoordModeOrigin);
            }
        }
    }
    XSetLineAttributes(display, gc, 1, LineSolid, CapButt, JoinMiter);
}

} // namespace

XlibDrawingAreaRenderer::XlibDrawingAreaRenderer(
    ApplicationModel* m, DrawingAreaHost* h,
    const java::Consumer<Viewport*>& selected,
    TranslateGizmo* translationGizmo, RotateGizmo* rotateGizmo,
    ScaleGizmo* scaleGizmo)
    : model(m), host(h), frameCapture(new FrameCaptureService(m,h)),
      selectedListener(selected), fontDisplay(nullptr), titleFont(nullptr),
      titleFontSize(0),
      selectionEditor(new SceneSelectionEditor(m->getScene())),
      gizmoPresenter(nullptr), gizmoDrawList(new Gizmo2DDrawList()),
      cameraControlDrawList(new Gizmo2DDrawList()),
      cameraControlPainter(new CameraControlGizmo2DPainter()),
      gizmoRenderer(new XlibGizmo2DDrawListRenderer()),
      linesBuilder(new ProjectedLinesViewBuilder()),
      surfacesBuilder(new ProjectedSurfacesViewBuilder())
{
    gizmoPresenter = new DrawingAreaGizmoPresenter(m->getDrawingArea(),
        selectionEditor, translationGizmo, rotateGizmo, scaleGizmo);
}

XlibDrawingAreaRenderer::~XlibDrawingAreaRenderer()
{
    if (titleFont && fontDisplay) XFreeFontSet(fontDisplay, titleFont);
    delete linesBuilder;
    delete surfacesBuilder;
    delete gizmoRenderer;
    delete cameraControlPainter;
    delete cameraControlDrawList;
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

    gizmoRenderer->setDisplay(display);
    gizmoDrawList->clear();
    switch (gizmoPresenter->prepareForView(camera)) {
      case DrawingAreaGizmoPresenter::GizmoKind::TRANSLATE:
        TranslateGizmo2DPainter::paint(gizmoDrawList,
            gizmoPresenter->getTranslationGizmo(), camera,
            set->getElementScaler());
        break;
      case DrawingAreaGizmoPresenter::GizmoKind::ROTATE:
        RotateGizmo2DPainter::paint(gizmoDrawList,
            gizmoPresenter->getRotateGizmo(), camera,
            set->getElementScaler());
        break;
      case DrawingAreaGizmoPresenter::GizmoKind::SCALE:
        ScaleGizmo2DPainter::paint(gizmoDrawList,
            gizmoPresenter->getScaleGizmo(), camera);
        break;
      default:
        break;
    }

    // Only the selected viewport shows them: it is the one that receives
    // the keyboard
    InputGizmo* inputGizmo = gizmoPresenter->getInputGizmo();
    if (inputGizmo == nullptr && model->getRayGizmo()->isVisible())
        inputGizmo = model->getRayGizmo()->getInputGizmo();
    if (inputGizmo != nullptr && set->isSelected(viewport))
        InputGizmo2DPainter::paint(gizmoDrawList, inputGizmo, width, height,
            set->getElementScaler(), *gizmoRenderer);
    gizmoRenderer->draw(frame, gc, visual, *gizmoDrawList, x, y, width,
                        height);

    // The HUD of the selected viewport is over everything
    if ( set->isSelected(viewport) ) {
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
    titleFont = XFontSelection::createSansFontSet(display, size, false);
}

void XlibDrawingAreaRenderer::drawTitle(Display* display, Drawable frame,
    GC gc, Visual* visual, Viewport* viewport, int x, int y)
{
    ViewportSet* set = model->getDrawingArea()->getViewportSet();
    int borderX = set->getElementScaler()->scaleSize(4);
    int borderY = set->getElementScaler()->scaleSize(1);
    ensureTitleFont(display, set->getElementScaler()->scaleSize(14));
    java::String title = set->getTitleFor(viewport);
    int textWidth = static_cast<int>(title.length()) * 8;
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
    bool raytraced = viewport->getRenderMode() == Viewport::RENDER_MODE_RAYTRACING;
    bool hiddenSurfaces =
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

    // The bodies as hidden lines (wireframe, or Appel hidden line removal
    // when surfaces are on) and the annotations, as the AWT presentation of
    // the Java port does
    java::ArrayList<ProjectedLineLayer*> layers;
    try {
        linesBuilder->buildView(scene, viewport,
            host->getBodyEditFeedbackProvider(), !raytraced && !hiddenSurfaces,
            layers);
    }
    catch (const java::Exception& e) {
        // A failure of the line algorithms over some body must not break
        // the presentation of the whole set
        fprintf(stderr, "XlibDrawingAreaRenderer: can not compute the lines "
                "of viewport \"%s\": %s\n",
                model->getDrawingArea()->getViewportSet()->getTitleFor(
                    viewport).c_str(), e.getMessage().c_str());
        ProjectedLineLayer::deleteAll(layers);
    }
    catch (const std::exception& e) {
        fprintf(stderr, "XlibDrawingAreaRenderer: can not compute the lines "
                "of viewport \"%s\": %s\n",
                model->getDrawingArea()->getViewportSet()->getTitleFor(
                    viewport).c_str(), e.what());
        ProjectedLineLayer::deleteAll(layers);
    }
    drawLayers(display, frame, gc, visual, layers, x, y, width, height,
        model->getDrawingArea()->getViewportSet()->getElementScaler()
            ->getScale());
    ProjectedLineLayer::deleteAll(layers);

    // Must be the last to draw, over the scene
    drawGizmos(display, frame, gc, visual, viewport, x, y, width, height);

    XlibFrameBufferSource source(display, frame, visual, x, y, width, height);
    frameCapture->copyColorBufferIfNeeded(&source,
        model->getDrawingArea()->getViewportSet()->isSelected(viewport));
}

namespace {

/**
Fills the spans of a projected polygon with the color of a GC.
*/
class XlibSpanSink : public ProjectedPolygonScanConverter::SpanSink {
private:
    Display* display;
    Drawable frame;
    GC graphicsContext;

public:
    XlibSpanSink(Display* display, Drawable frame, GC gc)
        : display(display), frame(frame), graphicsContext(gc) {}

    virtual void fillSpan(int x0, int x1, int y) override
    {
        XFillRectangle(display, frame, graphicsContext, x0, y, x1 - x0, 1);
    }
};

}

/**
Fills the visible polygons of the solids of a viewport (hidden surface
removal of `HiddenSurfaceRenderer`), as `AwtProjectedPolygonRenderer` does
in the AWT presentation of the Java port. The contours of a polygon are
filled at once with the even-odd rule (`ProjectedPolygonScanConverter`).
When the rendering configuration turns the surfaces off and the lines on,
only the contours of those polygons are drawn: with constant shading (no
light) all of them with the line color of the configuration, otherwise each
one with the color of its polygon.
*/
void XlibDrawingAreaRenderer::drawHiddenSurfaces(Display* display,
    Drawable frame, GC gc, Visual* visual, Viewport* viewport, int x, int y,
    int width, int height)
{
    const java::ArrayList<ProjectedPolygon*>* polygons = nullptr;

    try {
        polygons = &surfacesBuilder->buildView(model->getScene(), viewport,
            width, height, model->getDrawingArea()->getContentGeneration(),
            viewport->getRendererConfiguration()->isWithShadowsSet());
    }
    catch (const java::Exception& e) {
        // As with the lines, a failure over some body must not break the
        // presentation of the whole set
        fprintf(stderr, "XlibDrawingAreaRenderer: can not compute the visible "
                "surfaces of viewport \"%s\": %s\n",
                model->getDrawingArea()->getViewportSet()->getTitleFor(
                    viewport).c_str(), e.getMessage().c_str());
        return;
    }
    catch (const std::exception& e) {
        fprintf(stderr, "XlibDrawingAreaRenderer: can not compute the visible "
                "surfaces of viewport \"%s\": %s\n",
                model->getDrawingArea()->getViewportSet()->getTitleFor(
                    viewport).c_str(), e.what());
        return;
    }

    RendererConfiguration* quality = viewport->getRendererConfiguration();
    if (!quality->isSurfacesSet() && quality->isWiresSet()) {
        ColorRgb wireColor = quality->getWireColor();
        const ColorRgb* lineColor = nullptr;
        if (quality->getShadingType() ==
            RendererConfiguration::SHADING_TYPE_NOLIGHT) {
            lineColor = &wireColor;
        }
        drawContours(display, frame, gc, visual, *polygons, x, y, width,
            height, static_cast<float>(java::Math::max(1.0,
                model->getDrawingArea()->getViewportSet()->getElementScaler()
                    ->getScale())),
            lineColor);
    }
    else {
        XlibSpanSink sink(display, frame, gc);
        for (long i = 0; i < polygons->size(); i++) {
            const ProjectedPolygon* polygon = (*polygons)[i];
            XSetForeground(display, gc, pixel(visual, polygon->getColor()));
            ProjectedPolygonScanConverter::fill(*polygon, x, y, width,
                                                height, sink);
        }
    }
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
        area->setDepthCaptureRequested(false);
        area->setContoursRequested(false);
        host->showStatusMessage("Depth capture is unavailable in Xlib mode");
    }
    if (area->isProjectedViewsDebugRequested()) {
        area->setProjectedViewsDebugRequested(false);
        host->showStatusMessage("Projected views debugging is unavailable in Xlib mode");
    }

    Pixmap frame = XCreatePixmap(display, window, width, height, depth);
    GC gc = XCreateGC(display, frame, 0, nullptr);
    XSetForeground(display, gc, pixel(visual, ColorRgb(.77,.77,.77)));
    XFillRectangle(display, frame, gc, 0, 0, width, height);

    const java::ArrayList<Viewport*>& viewports = set->getViewports();
    bool full = host->isFullScreenGuiMode() && set->countActiveViewports() == 1;
    if (!full) {
        for (long i = 0; i < viewports.size(); ++i) {
            Viewport* v = viewports[i];
            if (!v->isActive() || v->getBorder() <= 0) continue;
            int x = static_cast<int>(v->getStartXPercent() * width);
            int y = static_cast<int>((1-v->getStartYPercent()-v->getSizeYPercent()) * height);
            int w = static_cast<int>(v->getSizeXPercent() * width);
            int h = static_cast<int>(v->getSizeYPercent() * height);
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
        if (set->isSelected(v) && selectedListener) selectedListener.accept(v);
        XRectangle clip = {static_cast<short>(x), static_cast<short>(y),
                           static_cast<unsigned short>(w),
                           static_cast<unsigned short>(h)};
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

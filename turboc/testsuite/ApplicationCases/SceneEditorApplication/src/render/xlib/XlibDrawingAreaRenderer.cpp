#include "render/xlib/XlibDrawingAreaRenderer.h"

#include <X11/Xutil.h>
#include <algorithm>
#include <math.h>
#include <stdio.h>

#include "java/util/ArrayList.txx"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/Scene.h"
#include "render/DrawingAreaHost.h"
#include "render/FrameBufferSource.h"
#include "render/FrameCaptureService.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"
#include "vsdk/toolkit/render/hiddenLine/WireframeRenderer.h"
#include "vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.h"


static unsigned long pixel(Visual* visual, const ColorRgb& color)
{
    return XlibRGBImageUncompressedRenderer::rgbToPixel(visual,
        ((int)(std::max(0.0, std::min(1.0, color.r())) * 255)),
        ((int)(std::max(0.0, std::min(1.0, color.g())) * 255)),
        ((int)(std::max(0.0, std::min(1.0, color.b())) * 255)));
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
               int lineWidth = 1, bool dashed = false)
{
    XSetForeground(display, gc, color);
    XSetLineAttributes(display, gc, lineWidth,
        dashed ? LineOnOffDash : LineSolid, CapRound, JoinRound);
    for (int i = 0; i < lines.getNumLines(); ++i) {
        Vector3Dd* a = lines.get2DLinePoint0(i);
        Vector3Dd* b = lines.get2DLinePoint1(i);
        int x0 = x + ((int)((a->x() + 1) * width / 2));
        int y0 = y + ((int)((1 - a->y()) * height / 2));
        int x1 = x + ((int)((b->x() + 1) * width / 2));
        int y1 = y + ((int)((1 - b->y()) * height / 2));
        XDrawLine(display, frame, gc, x0, y0, x1, y1);
        delete a;
        delete b;
    }
    XSetLineAttributes(display, gc, 1, LineSolid, CapButt, JoinMiter);
}

static bool projectLine(const Matrix4x4d& projection, const Vector3Dd& a,
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
        if (dp < 0 && dq < 0) return false;
        if (dp < 0 || dq < 0) {
            double t = dp / (dp - dq);
            Vector4Dd cut = p.multiply(1-t).add(q.multiply(t));
            if (dp < 0) p = cut; else q = cut;
        }
    }
    p = p.dividedByW();
    q = q.dividedByW();
    lines.add2DLine(p.x(), p.y(), q.x(), q.y());
    return true;
}

static void drawGrid(Display* display, Drawable frame, GC gc, Visual* visual,
              Camera* camera, int x, int y, int width, int height)
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
    Matrix4x4d projection = camera->calculateProjectionMatrix();
    Calligraphic2DBuffer regular, axes;
    for (int i = -7; i <= 7; ++i) {
        Calligraphic2DBuffer& out = i == 0 ? axes : regular;
        projectLine(projection, transform.multiply(Vector3Dd(i,-7,0)),
                    transform.multiply(Vector3Dd(i,7,0)), out);
        projectLine(projection, transform.multiply(Vector3Dd(-7,i,0)),
                    transform.multiply(Vector3Dd(7,i,0)), out);
    }
    drawLines(display, frame, gc, regular, x, y, width, height,
              pixel(visual, ColorRgb(.37,.37,.37)));
    drawLines(display, frame, gc, axes, x, y, width, height,
              pixel(visual, ColorRgb(0,0,0)));
}


XlibDrawingAreaRenderer::XlibDrawingAreaRenderer(
    ApplicationModel* m, DrawingAreaHost* h,
    Consumer<Viewport*>* selected)
    : model(m), host(h), frameCapture(new FrameCaptureService(m,h)),
      selectedListener(selected), fontDisplay(NULL), titleFont(NULL),
      titleFontSize(0) {}

XlibDrawingAreaRenderer::~XlibDrawingAreaRenderer()
{
    if (titleFont && fontDisplay) XFreeFontSet(fontDisplay, titleFont);
    delete frameCapture;
}

void XlibDrawingAreaRenderer::ensureTitleFont(Display* display, int size)
{
    if (titleFont && fontDisplay == display && titleFontSize == size) return;
    if (titleFont && fontDisplay) XFreeFontSet(fontDisplay, titleFont);
    fontDisplay = display;
    titleFontSize = size;
    char pattern[128];
    snprintf(pattern, sizeof(pattern),
        "-adobe-helvetica-medium-r-normal--%d-*-*-*-*-*-iso8859-1", size);
    char** missing = NULL;
    int count = 0;
    char* fallback = NULL;
    titleFont = XCreateFontSet(display, pattern, &missing, &count, &fallback);
    if (missing) XFreeStringList(missing);
    if (!titleFont) {
        missing = NULL;
        titleFont = XCreateFontSet(display, "fixed", &missing, &count, &fallback);
        if (missing) XFreeStringList(missing);
    }
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
    bool raytraced = viewport->getRenderMode() == Viewport::RENDER_MODE_RAYTRACING;

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
        if (viewport->isShowGrid())
            drawGrid(display, frame, gc, visual, camera, x, y, width, height);
    }

    ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    if (!raytraced) {
        ArrayList<SimpleBody*> solids, wires;
        for (long i = 0; i < bodies.size(); ++i) {
            SimpleBody* body = bodies[i];
            if (!body || !body->getGeometry() || scene->isHidden(body)) continue;
            if (dynamic_cast<Volume*>(body->getGeometry()))
                solids.add(body);
            else
                wires.add(body);
        }
        if (wires.size()) {
            Calligraphic2DBuffer lines;
            WireframeRenderer::execute(&lines, wires, camera);
            drawLines(display, frame, gc, lines, x, y, width, height,
                      pixel(visual, ColorRgb(0,0,0)));
        }
        if (solids.size()) {
            Calligraphic2DBuffer contours, visible, hidden;
            Camera eye(*camera);
            if (camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL) {
                Vector3Dd front = camera->getFront().normalized();
                eye.setPosition(camera->getPosition().subtract(front.multiply(1000)));
                eye.setNearPlaneDistance(camera->getNearPlaneDistance() + 1000);
                eye.setFarPlaneDistance(camera->getFarPlaneDistance() + 1000);
            }
            HiddenLineRenderer::executeAppelAlgorithm(solids, &eye,
                &contours, &visible, &hidden);
            drawLines(display, frame, gc, visible, x, y, width, height,
                      pixel(visual, ColorRgb(0,0,0)));
            drawLines(display, frame, gc, contours, x, y, width, height,
                      pixel(visual, ColorRgb(0,0,0)), 2);
        }
    }

    XlibFrameBufferSource source(display, frame, visual, x, y, width, height);
    frameCapture->copyColorBufferIfNeeded(&source,
        model->getDrawingArea()->getViewportSet()->isSelected(viewport));
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
    GC gc = XCreateGC(display, frame, 0, NULL);
    XSetForeground(display, gc, pixel(visual, ColorRgb(.77,.77,.77)));
    XFillRectangle(display, frame, gc, 0, 0, width, height);

    const ArrayList<Viewport*>& viewports = set->getViewports();
    bool full = host->isFullScreenGuiMode() && set->countActiveViewports() == 1;
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

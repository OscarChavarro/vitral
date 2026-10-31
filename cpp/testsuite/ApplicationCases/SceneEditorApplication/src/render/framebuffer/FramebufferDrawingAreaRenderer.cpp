#include "render/framebuffer/FramebufferDrawingAreaRenderer.h"

#include <cmath>
#include "java/util/ArrayList.txx"

#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/Scene.h"
#include "model/selection/SelectionSet.h"
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
#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"
#include "vsdk/toolkit/render/hiddenLine/WireframeRenderer.h"

namespace {

const uint32_t LINE_COLOR = 0x000000;
const uint32_t SELECTION_COLOR = 0xE00000;

/**
Reads a rectangle of the surface for the frame captures.
*/
class FramebufferFrameBufferSource : public FrameBufferSource {
    Framebuffer& surface;
    int x;
    int y;
    int width;
    int height;
public:
    FramebufferFrameBufferSource(Framebuffer& s, int x0, int y0, int w, int h)
        : surface(s), x(x0), y(y0), width(w), height(h) {}

    RGBImageUncompressed* readColor() override
    {
        RGBImageUncompressed* result = new RGBImageUncompressed();
        result->init(width, height);
        java::ArrayList<uint32_t> pixels;
        long count = static_cast<long>(width) * height;
        pixels.reserve(count);
        for ( long i = 0; i < count; i++ ) {
            pixels.add(0);
        }
        if ( !surface.readImage(x, y, width, height, pixels.data()) ) {
            return result;
        }
        for ( int row = 0; row < height; row++ ) {
            for ( int column = 0; column < width; column++ ) {
                uint32_t p = pixels[static_cast<long>(row) * width + column];
                result->putPixel(column, row, static_cast<char>(p >> 16),
                                 static_cast<char>(p >> 8),
                                 static_cast<char>(p));
            }
        }
        return result;
    }

    ZBuffer* readDepth() override { return nullptr; }
};

bool projectLine(const Matrix4x4d& projection, const Vector3Dd& a,
                 const Vector3Dd& b, Calligraphic2DBuffer& lines)
{
    Vector4Dd p = projection.multiply(Vector4Dd(a));
    Vector4Dd q = projection.multiply(Vector4Dd(b));
    const double planes[6][4] = {
        {1, 0, 0, 1}, {-1, 0, 0, 1}, {0, 1, 0, 1}, {0, -1, 0, 1},
        {0, 0, 1, 1}, {0, 0, -1, 1}
    };
    for ( int i = 0; i < 6; i++ ) {
        const double* n = planes[i];
        double dp = n[0] * p.x() + n[1] * p.y() + n[2] * p.z() + n[3] * p.w();
        double dq = n[0] * q.x() + n[1] * q.y() + n[2] * q.z() + n[3] * q.w();
        if ( dp < 0 && dq < 0 ) {
            return false;
        }
        if ( dp < 0 || dq < 0 ) {
            double t = dp / (dp - dq);
            Vector4Dd cut = p.multiply(1 - t).add(q.multiply(t));
            if ( dp < 0 ) {
                p = cut;
            }
            else {
                q = cut;
            }
        }
    }
    p = p.dividedByW();
    q = q.dividedByW();
    lines.add2DLine(p.x(), p.y(), q.x(), q.y());
    return true;
}

}

FramebufferDrawingAreaRenderer::FramebufferDrawingAreaRenderer(
    ApplicationModel* m, DrawingAreaHost* h,
    const std::function<void(Viewport*)>& selected)
    : model(m), host(h), frameCapture(new FrameCaptureService(m, h)),
      selectedListener(selected)
{
}

FramebufferDrawingAreaRenderer::~FramebufferDrawingAreaRenderer()
{
    delete frameCapture;
}

uint32_t FramebufferDrawingAreaRenderer::packed(const ColorRgb& color)
{
    double channels[3] = {color.r(), color.g(), color.b()};
    uint32_t result = 0;
    for ( double c : channels ) {
        c = c < 0 ? 0 : c > 1 ? 1 : c;
        result = result << 8 | static_cast<uint32_t>(c * 255 + 0.5);
    }
    return result;
}

/**
Draws lines in normalized device coordinates on a viewport; lines two pixels
wide are drawn twice, the second time one pixel to the right and below.
*/
void FramebufferDrawingAreaRenderer::drawLines(Framebuffer& surface,
    const Calligraphic2DBuffer& lines, int x, int y, int width, int height,
    uint32_t color, int lineWidth)
{
    for ( int i = 0; i < lines.getNumLines(); i++ ) {
        Vector3Dd* a = lines.get2DLinePoint0(i);
        Vector3Dd* b = lines.get2DLinePoint1(i);
        int x0 = x + static_cast<int>((a->x() + 1) * width / 2);
        int y0 = y + static_cast<int>((1 - a->y()) * height / 2);
        int x1 = x + static_cast<int>((b->x() + 1) * width / 2);
        int y1 = y + static_cast<int>((1 - b->y()) * height / 2);
        surface.line(x0, y0, x1, y1, color);
        if ( lineWidth > 1 ) {
            surface.line(x0 + 1, y0, x1 + 1, y1, color);
            surface.line(x0, y0 + 1, x1, y1 + 1, color);
        }
        delete a;
        delete b;
    }
}

void FramebufferDrawingAreaRenderer::drawGrid(Framebuffer& surface,
    Camera* camera, int x, int y, int width, int height)
{
    Matrix4x4d transform;
    if ( camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        Matrix4x4d rotation = camera->getRotation();
        double yaw = rotation.obtainEulerYawAngle() * 180.0 / M_PI;
        double pitch = rotation.obtainEulerPitchAngle() * 180.0 / M_PI;
        if ( pitch > -45 && pitch < 45 ) {
            if ( (yaw > 45 && yaw < 135) || (yaw < -45 && yaw > -135) ) {
                transform = transform.axisRotation(M_PI / 2, 1, 0, 0);
            }
            else {
                transform = transform.axisRotation(M_PI / 2, 0, 1, 0);
            }
        }
    }
    Matrix4x4d projection = camera->calculateProjectionMatrix();
    Calligraphic2DBuffer regular;
    Calligraphic2DBuffer axes;
    for ( int i = -7; i <= 7; i++ ) {
        Calligraphic2DBuffer& out = i == 0 ? axes : regular;
        projectLine(projection, transform.multiply(Vector3Dd(i, -7, 0)),
                    transform.multiply(Vector3Dd(i, 7, 0)), out);
        projectLine(projection, transform.multiply(Vector3Dd(-7, i, 0)),
                    transform.multiply(Vector3Dd(7, i, 0)), out);
    }
    drawLines(surface, regular, x, y, width, height,
              packed(ColorRgb(.37, .37, .37)), 1);
    drawLines(surface, axes, x, y, width, height, LINE_COLOR, 1);
}

void FramebufferDrawingAreaRenderer::drawTitle(Framebuffer& surface,
    Viewport* viewport, int x, int y)
{
    ViewportSet* set = model->getDrawingArea()->getViewportSet();
    java::String title = set->getTitleFor(viewport);
    int borderX = surface.fontWidth() / 2;
    int borderY = 1;
    int textWidth = surface.textWidth(title.c_str(), -1);
    viewport->setTitleArea(0, 0, textWidth + 2 * borderX,
                           surface.fontHeight() + 2 * borderY);
    surface.text(x + borderX, y + borderY, title.c_str(),
                 packed(set->getTitleColorFor(viewport)));
}

void FramebufferDrawingAreaRenderer::drawView(Framebuffer& surface,
    Viewport* viewport, int x, int y, int width, int height)
{
    if ( width <= 0 || height <= 0 ) {
        return;
    }
    Scene* scene = model->getScene();
    Camera* camera = viewport->getActiveCamera();
    RendererConfiguration* quality = viewport->getRendererConfiguration();
    scene->activeCamera = camera;
    scene->qualityTemplate->cloneFrom(*quality);
    bool raytraced =
        viewport->getRenderMode() == Viewport::RENDER_MODE_RAYTRACING;

    if ( raytraced ) {
        scene->activateSelectedBackground();
        model->setRaytracedImageWidth(width);
        model->setRaytracedImageHeight(height);
        host->raytraceImage();
        RGBImageUncompressed* image = model->getRaytracedImage();
        if ( image != nullptr && image->getXSize() == width &&
             image->getYSize() == height ) {
            java::ArrayList<uint32_t> pixels;
            pixels.reserve(static_cast<long>(width) * height);
            RGBPixel p;
            for ( int row = 0; row < height; row++ ) {
                for ( int column = 0; column < width; column++ ) {
                    image->getPixelRgb(column, row, &p);
                    pixels.add(
                        static_cast<uint32_t>(static_cast<unsigned char>(p.r)) << 16 |
                        static_cast<uint32_t>(static_cast<unsigned char>(p.g)) << 8 |
                        static_cast<unsigned char>(p.b));
                }
            }
            surface.putImage(x, y, width, height, pixels.data());
        }
    }
    else {
        surface.fillRectangle(x, y, width, height, packed(
            scene->simpleBackground->colorInDireccion(camera->getFront())));
        if ( viewport->isShowGrid() ) {
            drawGrid(surface, camera, x, y, width, height);
        }

        // Surfaces as wireframes, volumes as hidden line drawings; the
        // bodies of the selection in another color.
        java::ArrayList<SimpleBody*>& bodies =
            scene->scene->getSimpleBodies();
        java::ArrayList<SimpleBody*> groups[2][2];
        for ( long i = 0; i < bodies.size(); i++ ) {
            SimpleBody* body = bodies[i];
            if ( body == nullptr || body->getGeometry() == nullptr ||
                 scene->isHidden(body) ) {
                continue;
            }
            int selected = scene->selectedThings != nullptr &&
                i < scene->selectedThings->size() &&
                scene->selectedThings->isSelected(static_cast<int>(i)) ? 1 : 0;
            int solid = dynamic_cast<Volume*>(body->getGeometry()) ? 1 : 0;
            groups[selected][solid].add(body);
        }
        for ( int selected = 0; selected < 2; selected++ ) {
            uint32_t color = selected ? SELECTION_COLOR : LINE_COLOR;
            if ( groups[selected][0].size() > 0 ) {
                Calligraphic2DBuffer lines;
                WireframeRenderer::execute(&lines, groups[selected][0], camera);
                drawLines(surface, lines, x, y, width, height, color, 1);
            }
            if ( groups[selected][1].size() > 0 ) {
                Calligraphic2DBuffer contours;
                Calligraphic2DBuffer visible;
                Calligraphic2DBuffer hidden;
                Camera eye(*camera);
                if ( camera->getProjectionMode() ==
                     Camera::PROJECTION_MODE_ORTHOGONAL ) {
                    Vector3Dd front = camera->getFront().normalized();
                    eye.setPosition(camera->getPosition().subtract(
                        front.multiply(1000)));
                    eye.setNearPlaneDistance(
                        camera->getNearPlaneDistance() + 1000);
                    eye.setFarPlaneDistance(
                        camera->getFarPlaneDistance() + 1000);
                }
                HiddenLineRenderer::executeAppelAlgorithm(groups[selected][1],
                    &eye, &contours, &visible, &hidden);
                drawLines(surface, visible, x, y, width, height, color, 1);
                drawLines(surface, contours, x, y, width, height, color, 2);
            }
        }
    }

    FramebufferFrameBufferSource source(surface, x, y, width, height);
    frameCapture->copyColorBufferIfNeeded(&source,
        model->getDrawingArea()->getViewportSet()->isSelected(viewport));
}

void FramebufferDrawingAreaRenderer::display(Framebuffer& surface, int left,
                                             int top, int width, int height)
{
    if ( width <= 0 || height <= 0 ) {
        return;
    }
    host->beforeFrame();
    DrawingArea* area = model->getDrawingArea();
    area->updateSurfaceSize(width, height);
    ViewportSet* set = area->getViewportSet();
    set->enforceAvailableRenderModes();
    if ( area->isDepthCaptureRequested() ) {
        area->setDepthCaptureRequested(false);
        area->setContoursRequested(false);
        host->showStatusMessage(
            "Depth capture is unavailable in the framebuffer GUI");
    }
    if ( area->isProjectedViewsDebugRequested() ) {
        area->setProjectedViewsDebugRequested(false);
        host->showStatusMessage(
            "Projected views debugging is unavailable in the framebuffer GUI");
    }

    surface.beginDrawing();
    surface.setClip(left, top, width, height);
    surface.fillRectangle(left, top, width, height,
                          packed(ColorRgb(.77, .77, .77)));

    const java::ArrayList<Viewport*>& viewports = set->getViewports();
    bool full = host->isFullScreenGuiMode() &&
        set->countActiveViewports() == 1;
    if ( !full ) {
        for ( long i = 0; i < viewports.size(); i++ ) {
            Viewport* v = viewports[i];
            if ( !v->isActive() || v->getBorder() <= 0 ) {
                continue;
            }
            int x = static_cast<int>(v->getStartXPercent() * width);
            int y = static_cast<int>((1 - v->getStartYPercent() -
                                      v->getSizeYPercent()) * height);
            int w = static_cast<int>(v->getSizeXPercent() * width);
            int h = static_cast<int>(v->getSizeYPercent() * height);
            surface.fillRectangle(left + x, top + y, w, h, set->isSelected(v) ?
                packed(ColorRgb(1, .96, 0)) : packed(ColorRgb(.21, .25, .29)));
        }
    }
    for ( long i = 0; i < viewports.size(); i++ ) {
        Viewport* v = viewports[i];
        if ( !v->isActive() || (full && !set->isSelected(v)) ) {
            continue;
        }
        v->updatePixelArea(width, height);
        int x = full ? 0 : v->getPixelStartX();
        int y = full ? 0 : height - v->getPixelStartY() - v->getPixelSizeY();
        int w = full ? width : v->getPixelSizeX();
        int h = full ? height : v->getPixelSizeY();
        if ( set->isSelected(v) && selectedListener ) {
            selectedListener(v);
        }
        surface.setClip(left + x, top + y, w, h);
        drawView(surface, v, left + x, top + y, w, h);
        drawTitle(surface, v, left + x, top + y);
    }
    surface.resetClip();

    if ( frameCapture->isFrameExportPending() ) {
        FramebufferFrameBufferSource source(surface, left, top, width, height);
        frameCapture->exportPendingFrame(&source);
    }
    surface.endDrawing();
}

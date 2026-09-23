#include <cmath>

#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "model/DrawingArea.h"

namespace {
int roundToInt(double value)
{
    // Mimics Java's Math.round
    return (int)std::floor(value + 0.5);
}
}

DrawingArea::DrawingArea(ViewportSet* viewportSet)
    : viewportSet(viewportSet),
      // As in 3ds Max, the application starts in selection mode
      interactionMode(InteractionMode::SELECT),
      lastInteractionMode(InteractionMode::SELECT),
      colorCaptureRequested(false), depthCaptureRequested(false),
      contoursRequested(false), projectedViewsDebugRequested(false),
      hasPendingViewportExport(false), pendingViewportExportJpg(false),
      hasPendingWorkspaceExport(false), canvasWidth(0), canvasHeight(0)
{
}

ViewportSet* DrawingArea::getViewportSet() const
{
    return viewportSet;
}

InteractionMode DrawingArea::getInteractionMode() const
{
    return interactionMode;
}

void DrawingArea::setInteractionMode(InteractionMode interactionMode)
{
    this->interactionMode = interactionMode;
}

InteractionMode DrawingArea::getLastInteractionMode() const
{
    return lastInteractionMode;
}

void DrawingArea::switchInteractionMode(InteractionMode interactionMode)
{
    lastInteractionMode = this->interactionMode;
    this->interactionMode = interactionMode;
}

bool DrawingArea::shouldDrawTranslationGizmo() const
{
    return interactionMode == InteractionMode::TRANSLATE ||
        (interactionMode == InteractionMode::CAMERA &&
         lastInteractionMode == InteractionMode::TRANSLATE);
}

bool DrawingArea::isColorCaptureRequested() const
{
    return colorCaptureRequested;
}

void DrawingArea::setColorCaptureRequested(bool colorCaptureRequested)
{
    this->colorCaptureRequested = colorCaptureRequested;
}

bool DrawingArea::isDepthCaptureRequested() const
{
    return depthCaptureRequested;
}

void DrawingArea::setDepthCaptureRequested(bool depthCaptureRequested)
{
    this->depthCaptureRequested = depthCaptureRequested;
}

bool DrawingArea::isContoursRequested() const
{
    return contoursRequested;
}

void DrawingArea::setContoursRequested(bool contoursRequested)
{
    this->contoursRequested = contoursRequested;
}

bool DrawingArea::isProjectedViewsDebugRequested() const
{
    return projectedViewsDebugRequested;
}

void DrawingArea::setProjectedViewsDebugRequested(
    bool projectedViewsDebugRequested)
{
    this->projectedViewsDebugRequested = projectedViewsDebugRequested;
}

void DrawingArea::requestViewportExport(const java::File& file, bool jpg)
{
    pendingViewportExportFile = java::File(file.getPath());
    hasPendingViewportExport = true;
    pendingViewportExportJpg = jpg;
}

void DrawingArea::requestWorkspaceExport(const java::File& file)
{
    pendingWorkspaceExportFile = java::File(file.getPath());
    hasPendingWorkspaceExport = true;
}

const java::File* DrawingArea::getPendingViewportExportFile() const
{
    return hasPendingViewportExport ? &pendingViewportExportFile : nullptr;
}

bool DrawingArea::isPendingViewportExportJpg() const
{
    return pendingViewportExportJpg;
}

const java::File* DrawingArea::getPendingWorkspaceExportFile() const
{
    return hasPendingWorkspaceExport ? &pendingWorkspaceExportFile : nullptr;
}

void DrawingArea::clearPendingViewportExport()
{
    hasPendingViewportExport = false;
}

void DrawingArea::clearPendingWorkspaceExport()
{
    hasPendingWorkspaceExport = false;
}

void DrawingArea::toggleSelectedViewportGrid()
{
    Viewport* selected = viewportSet->getSelectedViewport();

    if ( selected != nullptr ) {
        selected->toggleGrid();
    }
}

void DrawingArea::addViewport()
{
    viewportSet->addViewport(new Viewport());
}

void DrawingArea::removeLastViewport()
{
    if ( viewportSet->getViewportCount() > 1 ) {
        delete viewportSet->removeViewport(viewportSet->getViewportCount() - 1);
    }
}

int DrawingArea::getCanvasWidth() const
{
    return canvasWidth;
}

int DrawingArea::getCanvasHeight() const
{
    return canvasHeight;
}

void DrawingArea::updateCanvasSize(int width, int height)
{
    if ( width <= 0 || height <= 0 ) {
        return;
    }
    canvasWidth = width;
    canvasHeight = height;
}

void DrawingArea::setCanvasSize(int width, int height)
{
    canvasWidth = width;
    canvasHeight = height;
}

void DrawingArea::updateSurfaceSize(int surfaceWidth, int surfaceHeight)
{
    if ( surfaceWidth <= 0 || surfaceHeight <= 0 ) {
        return;
    }
    if ( surfaceWidth == viewportSet->getSizeXInPixels() &&
         surfaceHeight == viewportSet->getSizeYInPixels() ) {
        return;
    }
    viewportSet->resize(surfaceWidth, surfaceHeight);
}

int DrawingArea::scaleXToSurface(int x) const
{
    if ( canvasWidth <= 0 || viewportSet->getSizeXInPixels() <= 0 ) {
        return x;
    }
    return roundToInt(((double)x * (double)viewportSet->getSizeXInPixels()) /
        (double)canvasWidth);
}

int DrawingArea::scaleYToSurface(int y) const
{
    if ( canvasHeight <= 0 || viewportSet->getSizeYInPixels() <= 0 ) {
        return y;
    }
    return roundToInt(((double)y * (double)viewportSet->getSizeYInPixels()) /
        (double)canvasHeight);
}

int DrawingArea::scaleXToCanvas(int x) const
{
    if ( canvasWidth <= 0 || viewportSet->getSizeXInPixels() <= 0 ) {
        return x;
    }
    return roundToInt(((double)x * (double)canvasWidth) /
        (double)viewportSet->getSizeXInPixels());
}

int DrawingArea::scaleYToCanvas(int y) const
{
    if ( canvasHeight <= 0 || viewportSet->getSizeYInPixels() <= 0 ) {
        return y;
    }
    return roundToInt(((double)y * (double)canvasHeight) /
        (double)viewportSet->getSizeYInPixels());
}

MouseEvent DrawingArea::toSurfaceEvent(const MouseEvent& canvasEvent) const
{
    MouseEvent surfaceEvent;

    surfaceEvent.setX(scaleXToSurface(canvasEvent.getX()));
    surfaceEvent.setY(scaleYToSurface(canvasEvent.getY()));
    surfaceEvent.setButton(canvasEvent.getButton());
    surfaceEvent.setModifiers(canvasEvent.getModifiers());
    surfaceEvent.setClicks(canvasEvent.getClicks());
    return surfaceEvent;
}

bool DrawingArea::projectToCanvas(Viewport* viewport, const Vector3Dd& point,
                                  double outCanvas[2]) const
{
    Camera* camera = viewport->getActiveCamera();
    camera->updateVectors();

    Vector3Dd d = point.subtract(camera->getPosition());
    Vector3Dd front = camera->getFront().normalized();
    Vector3Dd right = camera->getLeft().normalized().multiply(-1);
    Vector3Dd up = camera->getUp().normalized();
    double depth = d.dotProduct(front);

    if ( depth <= 0 ) {
        return false;
    }
    double u = d.dotProduct(right) / depth * 0.5 /
        camera->getRightWithScale().length();
    double v = d.dotProduct(up) / depth * 0.5 /
        camera->getUpWithScale().length();
    double w = camera->getViewportXSize();
    double h = camera->getViewportYSize();
    double surfaceX = viewport->getPixelStartX() + u * w + w / 2.0;
    double surfaceY = viewport->getPixelStartY() + h / 2.0 - 1 - v * h;

    outCanvas[0] = surfaceX * canvasWidth / viewportSet->getSizeXInPixels();
    outCanvas[1] = surfaceY * canvasHeight / viewportSet->getSizeYInPixels();
    return true;
}

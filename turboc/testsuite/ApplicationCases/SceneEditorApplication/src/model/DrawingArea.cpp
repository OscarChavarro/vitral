#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "model/DrawingArea.h"

static int roundToInt(double value)
{
    // Mimics Java's Math.round
    return (int)floor(value + 0.5);
}

DrawingArea::DrawingArea(ViewportSet* viewportSet)
    : viewportSet(viewportSet),
      // As in 3ds Max, the application starts in selection mode
      interactionMode(InteractionMode::SELECT),
      lastInteractionMode(InteractionMode::SELECT),
      colorCaptureRequested(FALSE), depthCaptureRequested(FALSE),
      contoursRequested(FALSE), projectedViewsDebugRequested(FALSE),
      hasPendingViewportExport(FALSE), pendingViewportExportJpg(FALSE),
      hasPendingWorkspaceExport(FALSE), canvasWidth(0), canvasHeight(0)
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

BOOLEAN DrawingArea::shouldDrawTranslationGizmo() const
{
    return interactionMode == InteractionMode::TRANSLATE ||
        (interactionMode == InteractionMode::CAMERA &&
         lastInteractionMode == InteractionMode::TRANSLATE);
}

BOOLEAN DrawingArea::isColorCaptureRequested() const
{
    return colorCaptureRequested;
}

void DrawingArea::setColorCaptureRequested(BOOLEAN colorCaptureRequested)
{
    this->colorCaptureRequested = colorCaptureRequested;
}

BOOLEAN DrawingArea::isDepthCaptureRequested() const
{
    return depthCaptureRequested;
}

void DrawingArea::setDepthCaptureRequested(BOOLEAN depthCaptureRequested)
{
    this->depthCaptureRequested = depthCaptureRequested;
}

BOOLEAN DrawingArea::isContoursRequested() const
{
    return contoursRequested;
}

void DrawingArea::setContoursRequested(BOOLEAN contoursRequested)
{
    this->contoursRequested = contoursRequested;
}

BOOLEAN DrawingArea::isProjectedViewsDebugRequested() const
{
    return projectedViewsDebugRequested;
}

void DrawingArea::setProjectedViewsDebugRequested(
    BOOLEAN projectedViewsDebugRequested)
{
    this->projectedViewsDebugRequested = projectedViewsDebugRequested;
}

void DrawingArea::requestViewportExport(const File& file, BOOLEAN jpg)
{
    pendingViewportExportFile = File(file.getPath());
    hasPendingViewportExport = TRUE;
    pendingViewportExportJpg = jpg;
}

void DrawingArea::requestWorkspaceExport(const File& file)
{
    pendingWorkspaceExportFile = File(file.getPath());
    hasPendingWorkspaceExport = TRUE;
}

const File* DrawingArea::getPendingViewportExportFile() const
{
    return hasPendingViewportExport ? &pendingViewportExportFile : NULL;
}

BOOLEAN DrawingArea::isPendingViewportExportJpg() const
{
    return pendingViewportExportJpg;
}

const File* DrawingArea::getPendingWorkspaceExportFile() const
{
    return hasPendingWorkspaceExport ? &pendingWorkspaceExportFile : NULL;
}

void DrawingArea::clearPendingViewportExport()
{
    hasPendingViewportExport = FALSE;
}

void DrawingArea::clearPendingWorkspaceExport()
{
    hasPendingWorkspaceExport = FALSE;
}

void DrawingArea::toggleSelectedViewportGrid()
{
    Viewport* selected = viewportSet->getSelectedViewport();

    if ( selected != NULL ) {
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

BOOLEAN DrawingArea::projectToCanvas(Viewport* viewport, const Vector3Dd& point,
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
        return FALSE;
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
    return TRUE;
}

#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "model/history/CameraState.h"
#include "model/history/ViewportState.h"

ViewportState::ViewportState(Viewport* viewport)
{
    Camera* cameras[CAMERA_COUNT] = {
        viewport->getPerspectiveCamera(),
        viewport->getTopCamera(),
        viewport->getBottomCamera(),
        viewport->getLeftCamera(),
        viewport->getFrontCamera()
    };
    int i;

    this->viewport = viewport;
    this->activeCamera = viewport->getActiveCamera();
    for ( i = 0; i < CAMERA_COUNT; i++ ) {
        cameraStates[i] = CameraState::capture(cameras[i]);
    }
    this->renderMode = viewport->getRenderMode();
    this->showGrid = viewport->isShowGrid();
    this->requestedSizeXInPixels = viewport->getRequestedSizeXInPixels();
    this->requestedSizeYInPixels = viewport->getRequestedSizeYInPixels();
    this->rendererConfiguration = new RendererConfiguration();
    this->rendererConfiguration->cloneFrom(
        *viewport->getRendererConfiguration());
}

ViewportState::~ViewportState()
{
    int i;
    for ( i = 0; i < CAMERA_COUNT; i++ ) {
        delete cameraStates[i];
    }
    delete rendererConfiguration;
}

ViewportState* ViewportState::capture(Viewport* viewport)
{
    return new ViewportState(viewport);
}

Viewport* ViewportState::getViewport() const
{
    return viewport;
}

void ViewportState::restore() const
{
    int i;

    for ( i = 0; i < CAMERA_COUNT; i++ ) {
        cameraStates[i]->restore();
    }
    if ( activeCamera != nullptr ) {
        viewport->setActiveCamera(activeCamera);
    }
    viewport->setRenderMode(renderMode);
    viewport->setShowGrid(showGrid);
    viewport->setRequestedSizeXInPixels(requestedSizeXInPixels);
    viewport->setRequestedSizeYInPixels(requestedSizeYInPixels);
    viewport->getRendererConfiguration()->cloneFrom(*rendererConfiguration);
}

java::String ViewportState::describeChangeTo(const ViewportState* later) const
{
    int i;

    if ( later->activeCamera != activeCamera ) {
        return "Projection change";
    }
    for ( i = 0; i < CAMERA_COUNT; i++ ) {
        if ( !cameraStates[i]->isSameState(later->cameraStates[i]) ) {
            return "Camera movement";
        }
    }
    return "Display change";
}

bool ViewportState::isSameState(const ViewportState* other) const
{
    int i;

    if ( other == nullptr || other->viewport != viewport ||
         other->activeCamera != activeCamera ||
         other->renderMode != renderMode ||
         other->showGrid != showGrid ||
         other->requestedSizeXInPixels != requestedSizeXInPixels ||
         other->requestedSizeYInPixels != requestedSizeYInPixels ||
         other->rendererConfiguration->compareTo(*rendererConfiguration) != 0 ) {
        return false;
    }
    for ( i = 0; i < CAMERA_COUNT; i++ ) {
        if ( !cameraStates[i]->isSameState(other->cameraStates[i]) ) {
            return false;
        }
    }
    return true;
}

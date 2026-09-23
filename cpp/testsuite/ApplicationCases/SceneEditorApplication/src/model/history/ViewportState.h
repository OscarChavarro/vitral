#ifndef __VIEWPORT_STATE__
#define __VIEWPORT_STATE__

#include "java/lang/String.h"

class Camera;
class CameraState;
class RendererConfiguration;
class Viewport;

/**
How a viewport shows the scene: which of its cameras is active (the
projection location: perspective or one of the parallel projections), the
state of each of its cameras, and its display settings (render mode, grid,
requested image size and renderer configuration). The placement of the
viewport in its viewport set (layout, maximization) is not part of it.
*/
class ViewportState {
public:
    static const int CAMERA_COUNT = 5;

private:
    Viewport* viewport;
    Camera* activeCamera;
    CameraState* cameraStates[CAMERA_COUNT];
    int renderMode;
    bool showGrid;
    int requestedSizeXInPixels;
    int requestedSizeYInPixels;
    RendererConfiguration* rendererConfiguration;

    explicit ViewportState(Viewport* viewport);

    ViewportState(const ViewportState& other);
    ViewportState& operator=(const ViewportState& other);

public:
    virtual ~ViewportState();

    /**
    @param viewport viewport whose state is captured
    @return the current state of the viewport, owned by the caller
    */
    static ViewportState* capture(Viewport* viewport);

    /**
    @return the viewport whose state was captured
    */
    Viewport* getViewport() const;

    /**
    Gives back the captured state to the viewport. Its renderer
    configuration is updated in place, as interaction techniques may hold it.
    */
    void restore() const;

    /**
    @param later state of the same viewport captured after a change
    @return name of the change, for the user: a projection change (other
    active camera), a camera movement, or a display change (render mode,
    grid, image size, renderer configuration)
    */
    java::String describeChangeTo(const ViewportState* later) const;

    /**
    @param other state captured from the same viewport
    @return true if both states show the scene the same way
    */
    bool isSameState(const ViewportState* other) const;
};

#endif

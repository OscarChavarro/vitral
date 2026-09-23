package model.history;

import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.gui.viewport.Viewport;

/**
How a viewport shows the scene: which of its cameras is active (the
projection location: perspective or one of the parallel projections), the
state of each of its cameras, and its display settings (render mode, grid,
requested image size and renderer configuration). The placement of the
viewport in its viewport set (layout, maximization) is not part of it.
*/
public final class ViewportState
{
    private final Viewport viewport;
    private final Camera activeCamera;
    private final CameraState[] cameraStates;
    private final int renderMode;
    private final boolean showGrid;
    private final int requestedSizeXInPixels;
    private final int requestedSizeYInPixels;
    private final RendererConfiguration rendererConfiguration;

    private ViewportState(Viewport viewport)
    {
        Camera[] cameras = camerasOf(viewport);
        int i;

        this.viewport = viewport;
        this.activeCamera = viewport.getActiveCamera();
        this.cameraStates = new CameraState[cameras.length];
        for ( i = 0; i < cameras.length; i++ ) {
            cameraStates[i] = CameraState.capture(cameras[i]);
        }
        this.renderMode = viewport.getRenderMode();
        this.showGrid = viewport.isShowGrid();
        this.requestedSizeXInPixels = viewport.getRequestedSizeXInPixels();
        this.requestedSizeYInPixels = viewport.getRequestedSizeYInPixels();
        this.rendererConfiguration = new RendererConfiguration();
        this.rendererConfiguration.clone(viewport.getRendererConfiguration());
    }

    /**
    @param viewport viewport whose state is captured
    @return the current state of the viewport
    */
    public static ViewportState capture(Viewport viewport)
    {
        return new ViewportState(viewport);
    }

    private static Camera[] camerasOf(Viewport viewport)
    {
        return new Camera[] {
            viewport.getPerspectiveCamera(),
            viewport.getTopCamera(),
            viewport.getBottomCamera(),
            viewport.getLeftCamera(),
            viewport.getFrontCamera()
        };
    }

    /**
    @return the viewport whose state was captured
    */
    public Viewport getViewport()
    {
        return viewport;
    }

    /**
    Gives back the captured state to the viewport. Its renderer configuration
    is updated in place, as interaction techniques may hold it.
    */
    public void restore()
    {
        for ( CameraState state : cameraStates ) {
            state.restore();
        }
        if ( activeCamera != null ) {
            viewport.setActiveCamera(activeCamera);
        }
        viewport.setRenderMode(renderMode);
        viewport.setShowGrid(showGrid);
        viewport.setRequestedSizeXInPixels(requestedSizeXInPixels);
        viewport.setRequestedSizeYInPixels(requestedSizeYInPixels);
        viewport.getRendererConfiguration().clone(rendererConfiguration);
    }

    /**
    @param later state of the same viewport captured after a change
    @return name of the change, for the user: a projection change (other
    active camera), a camera movement, or a display change (render mode,
    grid, image size, renderer configuration)
    */
    public String describeChangeTo(ViewportState later)
    {
        int i;

        if ( later.activeCamera != activeCamera ) {
            return "Projection change";
        }
        for ( i = 0; i < cameraStates.length; i++ ) {
            if ( !cameraStates[i].isSameState(later.cameraStates[i]) ) {
                return "Camera movement";
            }
        }
        return "Display change";
    }

    /**
    @param other state captured from the same viewport
    @return true if both states show the scene the same way
    */
    public boolean isSameState(ViewportState other)
    {
        int i;

        if ( other == null || other.viewport != viewport ||
             other.activeCamera != activeCamera ||
             other.renderMode != renderMode ||
             other.showGrid != showGrid ||
             other.requestedSizeXInPixels != requestedSizeXInPixels ||
             other.requestedSizeYInPixels != requestedSizeYInPixels ||
             other.rendererConfiguration.compareTo(rendererConfiguration) != 0 ) {
            return false;
        }
        for ( i = 0; i < cameraStates.length; i++ ) {
            if ( !cameraStates[i].isSameState(other.cameraStates[i]) ) {
                return false;
            }
        }
        return true;
    }
}

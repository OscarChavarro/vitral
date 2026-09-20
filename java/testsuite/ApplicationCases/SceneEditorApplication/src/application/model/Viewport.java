package application.model;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.RendererConfiguration;

public class Viewport
{
    public static final int RENDER_MODE_ZBUFFER = 1;
    public static final int RENDER_MODE_RAYTRACING = 2;

    private int requestedSizeXInPixels;
    private int requestedSizeYInPixels;
    private Camera activeCamera;
    private final Camera perspectiveCamera;
    private final Camera topCamera;
    private final Camera bottomCamera;
    private final Camera leftCamera;
    private final Camera frontCamera;
    private final RendererConfiguration rendererConfiguration;
    private String title;
    private int renderMode;
    private boolean showGrid;

    public Viewport()
    {
        Matrix4x4d r = new Matrix4x4d();

        requestedSizeXInPixels = 0;
        requestedSizeYInPixels = 0;

        perspectiveCamera = new Camera();
        perspectiveCamera.setPosition(new Vector3Dd(-5, -5, 5));
        r = r.eulerAnglesRotation(Math.toRadians(45), Math.toRadians(-35), 0);
        perspectiveCamera.setRotation(r);
        perspectiveCamera.setName("Perspective");

        topCamera = new Camera();
        topCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        topCamera.setPosition(new Vector3Dd(0, 0, 5));
        r = r.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(-90), 0);
        topCamera.setRotation(r);
        topCamera.setOrthogonalZoom(0.25);
        topCamera.setName("Top");

        bottomCamera = new Camera();
        bottomCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        bottomCamera.setPosition(new Vector3Dd(0, 0, -5));
        r = r.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(90), 0);
        bottomCamera.setRotation(r);
        bottomCamera.setOrthogonalZoom(0.25);
        bottomCamera.setName("Bottom");

        leftCamera = new Camera();
        leftCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        leftCamera.setPosition(new Vector3Dd(-5, 0, 0));
        r = r.identity();
        leftCamera.setRotation(r);
        leftCamera.setOrthogonalZoom(0.25);
        leftCamera.setName("Left");

        frontCamera = new Camera();
        frontCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        frontCamera.setPosition(new Vector3Dd(0, -5, 0));
        r = r.eulerAnglesRotation(Math.toRadians(90), 0, 0);
        frontCamera.setRotation(r);
        frontCamera.setOrthogonalZoom(0.25);
        frontCamera.setName("Front");

        activeCamera = perspectiveCamera;
        rendererConfiguration = new RendererConfiguration();
        title = activeCamera.getName();
        renderMode = RENDER_MODE_ZBUFFER;
        showGrid = true;
    }

    public int getRequestedSizeXInPixels()
    {
        return requestedSizeXInPixels;
    }

    public void setRequestedSizeXInPixels(int requestedSizeXInPixels)
    {
        this.requestedSizeXInPixels = requestedSizeXInPixels;
    }

    public int getRequestedSizeYInPixels()
    {
        return requestedSizeYInPixels;
    }

    public void setRequestedSizeYInPixels(int requestedSizeYInPixels)
    {
        this.requestedSizeYInPixels = requestedSizeYInPixels;
    }

    public Camera getActiveCamera()
    {
        return activeCamera;
    }

    public void setActiveCamera(Camera activeCamera)
    {
        this.activeCamera = activeCamera;
        title = activeCamera.getName();
    }

    public Camera getPerspectiveCamera()
    {
        return perspectiveCamera;
    }

    public Camera getTopCamera()
    {
        return topCamera;
    }

    public Camera getBottomCamera()
    {
        return bottomCamera;
    }

    public Camera getLeftCamera()
    {
        return leftCamera;
    }

    public Camera getFrontCamera()
    {
        return frontCamera;
    }

    public RendererConfiguration getRendererConfiguration()
    {
        return rendererConfiguration;
    }

    public String getTitle()
    {
        return title;
    }

    public int getRenderMode()
    {
        return renderMode;
    }

    public void setRenderMode(int renderMode)
    {
        this.renderMode = renderMode;
    }

    public boolean isShowGrid()
    {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid)
    {
        this.showGrid = showGrid;
    }

    public void toggleGrid()
    {
        showGrid = !showGrid;
    }

    public void cycleRequestedSize()
    {
        switch ( requestedSizeXInPixels ) {
          case 0:
            requestedSizeXInPixels = 320;
            requestedSizeYInPixels = 240;
            break;
          case 320:
            requestedSizeXInPixels = 640;
            requestedSizeYInPixels = 480;
            break;
          case 640:
            requestedSizeXInPixels = 800;
            requestedSizeYInPixels = 600;
            break;
          case 800:
          default:
            requestedSizeXInPixels = 0;
            requestedSizeYInPixels = 0;
            break;
        }
    }

    public void toggleRenderMode()
    {
        if ( renderMode == RENDER_MODE_ZBUFFER ) {
            renderMode = RENDER_MODE_RAYTRACING;
        }
        else {
            renderMode = RENDER_MODE_ZBUFFER;
        }
    }

    public void updateCameraViewports(int width, int height)
    {
        perspectiveCamera.updateViewportResize(width, height);
        topCamera.updateViewportResize(width, height);
        bottomCamera.updateViewportResize(width, height);
        leftCamera.updateViewportResize(width, height);
        frontCamera.updateViewportResize(width, height);
    }
}

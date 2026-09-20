package framework.model;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.RendererConfiguration;

/**
A `Viewport` is one rectangular area of a `ViewportSet`, showing the scene
through one of its cameras. It is a plain model object: it knows nothing about
AWT, Swing or JOGL.

The area occupied is specified in two ways:
  - As percentages of the containing `ViewportSet` area (`startXPercent`,
    `startYPercent`, `sizeXPercent`, `sizeYPercent`), assigned by the layout
    of the `ViewportSet`. Origin is at the lower left corner of the container.
  - As pixels (`pixelStartX`, `pixelStartY`, `pixelSizeX`, `pixelSizeY`),
    calculated from the percentages by `updatePixelArea`. A viewport can
    request an specific size in pixels. If this size gets smaller than the
    percent-based area, the viewport is assigned to match the requested size,
    centered inside the percent-based area. If a requested size dimension is
    greater than the percent-based area, the requested size is ignored. A
    requested size of 0 means the requested size matches the percent-based
    size. This is useful in applications willing to use just a subset of the
    area, for example when previewing slow raytracing visualizations.
*/
public class Viewport
{
    public static final int RENDER_MODE_Z_BUFFER = 1;
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

    private double startXPercent;
    private double startYPercent;
    private double sizeXPercent;
    private double sizeYPercent;
    private int border;
    private boolean active;

    private int pixelStartX;
    private int pixelStartY;
    private int pixelSizeX;
    private int pixelSizeY;

    private int titleAreaStartX;
    private int titleAreaStartY;
    private int titleAreaSizeX;
    private int titleAreaSizeY;

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
        renderMode = RENDER_MODE_Z_BUFFER;
        showGrid = true;

        startXPercent = 0.0;
        startYPercent = 0.0;
        sizeXPercent = 1.0;
        sizeYPercent = 1.0;
        border = 2;
        active = true;

        pixelStartX = 0;
        pixelStartY = 0;
        pixelSizeX = 0;
        pixelSizeY = 0;

        titleAreaStartX = 0;
        titleAreaStartY = 0;
        titleAreaSizeX = 0;
        titleAreaSizeY = 0;
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
        if ( renderMode == RENDER_MODE_Z_BUFFER) {
            renderMode = RENDER_MODE_RAYTRACING;
        }
        else {
            renderMode = RENDER_MODE_Z_BUFFER;
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

    public double getStartXPercent()
    {
        return startXPercent;
    }

    public void setStartXPercent(double startXPercent)
    {
        this.startXPercent = startXPercent;
    }

    public double getStartYPercent()
    {
        return startYPercent;
    }

    public void setStartYPercent(double startYPercent)
    {
        this.startYPercent = startYPercent;
    }

    public double getSizeXPercent()
    {
        return sizeXPercent;
    }

    public void setSizeXPercent(double sizeXPercent)
    {
        this.sizeXPercent = sizeXPercent;
    }

    public double getSizeYPercent()
    {
        return sizeYPercent;
    }

    public void setSizeYPercent(double sizeYPercent)
    {
        this.sizeYPercent = sizeYPercent;
    }

    /**
    @return the border width, in pixels, left around the projection area
    */
    public int getBorder()
    {
        return border;
    }

    public void setBorder(int border)
    {
        this.border = border;
    }

    /**
    An active viewport is visible and should be rendered. An inactive one is
    hidden (for example when another viewport is maximized).
    @return true if this viewport is visible
    */
    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public int getPixelStartX()
    {
        return pixelStartX;
    }

    public int getPixelStartY()
    {
        return pixelStartY;
    }

    public int getPixelSizeX()
    {
        return pixelSizeX;
    }

    public int getPixelSizeY()
    {
        return pixelSizeY;
    }

    /**
    Informs the area, in pixels, where the title of this viewport is presented.
    Only who presents the title knows its real size, so it must be informed
    each time the title is drawn. Coordinates are relative to the viewport,
    with origin at its upper left corner. An empty area (the initial value)
    means the title has not been presented.
    @param startX
    @param startY
    @param sizeX
    @param sizeY
    */
    public void setTitleArea(int startX, int startY, int sizeX, int sizeY)
    {
        titleAreaStartX = startX;
        titleAreaStartY = startY;
        titleAreaSizeX = sizeX;
        titleAreaSizeY = sizeY;
    }

    public int getTitleAreaStartX()
    {
        return titleAreaStartX;
    }

    public int getTitleAreaStartY()
    {
        return titleAreaStartY;
    }

    public int getTitleAreaSizeX()
    {
        return titleAreaSizeX;
    }

    public int getTitleAreaSizeY()
    {
        return titleAreaSizeY;
    }

    /**
    @param x coordinate relative to the viewport, origin at its upper left
    corner
    @param y coordinate relative to the viewport, origin at its upper left
    corner
    @return true if the point is over the area where the title is presented
    */
    public boolean isOverTitle(int x, int y)
    {
        return x >= titleAreaStartX && x < titleAreaStartX + titleAreaSizeX &&
               y >= titleAreaStartY && y < titleAreaStartY + titleAreaSizeY;
    }

    /**
    Assigns the percent-based area of this viewport inside its container.
    @param startXPercent
    @param startYPercent
    @param sizeXPercent
    @param sizeYPercent
    */
    public void setPercentArea(double startXPercent, double startYPercent,
                               double sizeXPercent, double sizeYPercent)
    {
        this.startXPercent = startXPercent;
        this.startYPercent = startYPercent;
        this.sizeXPercent = sizeXPercent;
        this.sizeYPercent = sizeYPercent;
    }

    /**
    Given a point (x, y) in the percent space of the container (origin at
    lower left corner), determines if the point is inside this viewport.
    @param x
    @param y
    @return true if the point is inside the percent-based area
    */
    public boolean contains(double x, double y)
    {
        return x >= startXPercent && x <= startXPercent + sizeXPercent &&
               y >= startYPercent && y <= startYPercent + sizeYPercent;
    }

    /**
    @return true if no specific size in pixels is requested, so the viewport
    uses all the area assigned to it
    */
    public boolean useFullContainerArea()
    {
        return requestedSizeXInPixels == 0 || requestedSizeYInPixels == 0;
    }

    /**
    A container area has valid pixel coordinates from (0, 0) to
    (containerXSize-1, containerYSize-1). This method calculates the pixel
    area of this viewport inside that container from its percent-based area,
    its border and its requested size, and updates the cameras accordingly.
    @param containerXSize
    @param containerYSize
    */
    public void updatePixelArea(int containerXSize, int containerYSize)
    {
        int w;
        int h;
        int subAreaXSize;
        int subAreaYSize;

        pixelStartX = (int)(startXPercent * ((double)containerXSize)) + border + 1;
        pixelStartY = (int)(startYPercent * ((double)containerYSize)) + border + 1;
        subAreaXSize = (int)(sizeXPercent * ((double)containerXSize)) - 2*border - 2;
        subAreaYSize = (int)(sizeYPercent * ((double)containerYSize)) - 2*border - 2;
        if ( useFullContainerArea() ) {
            pixelSizeX = subAreaXSize;
            pixelSizeY = subAreaYSize;
        }
        else {
            if ( requestedSizeXInPixels < subAreaXSize ) {
                w = requestedSizeXInPixels;
            }
            else {
                w = subAreaXSize;
            }
            if ( requestedSizeYInPixels < subAreaYSize ) {
                h = requestedSizeYInPixels;
            }
            else {
                h = subAreaYSize;
            }
            pixelStartX += (subAreaXSize - w) / 2;
            pixelStartY += (subAreaYSize - h) / 2;
            pixelSizeX = w;
            pixelSizeY = h;
        }
        updateCameraViewports(pixelSizeX, pixelSizeY);
    }

    /**
    @return the standard command (see `ViewportSetCommands`) that selects the
    projection location currently used by this viewport
    */
    public String getProjectionLocationCommand()
    {
        if ( activeCamera == topCamera ) {
            return ViewportSetCommands.IDV_PROJECTION_LOCATION_TOP;
        }
        else if ( activeCamera == bottomCamera ) {
            return ViewportSetCommands.IDV_PROJECTION_LOCATION_BOTTOM;
        }
        else if ( activeCamera == leftCamera ) {
            return ViewportSetCommands.IDV_PROJECTION_LOCATION_LEFT;
        }
        else if ( activeCamera == frontCamera ) {
            return ViewportSetCommands.IDV_PROJECTION_LOCATION_FRONT;
        }
        return ViewportSetCommands.IDV_PROJECTION_LOCATION_PERSPECTIVE;
    }

    /**
    Selects the projection location given by one of the standard commands of
    the projection location popup (see `ViewportSetCommands`).
    @param command
    @return true if the command was a projection location one and was applied
    */
    public boolean selectProjectionLocation(String command)
    {
        if ( command == null ) {
            return false;
        }
        switch ( command ) {
          case ViewportSetCommands.IDV_PROJECTION_LOCATION_PERSPECTIVE:
            setActiveCamera(perspectiveCamera);
            return true;
          case ViewportSetCommands.IDV_PROJECTION_LOCATION_TOP:
            setActiveCamera(topCamera);
            return true;
          case ViewportSetCommands.IDV_PROJECTION_LOCATION_BOTTOM:
            setActiveCamera(bottomCamera);
            return true;
          case ViewportSetCommands.IDV_PROJECTION_LOCATION_LEFT:
            setActiveCamera(leftCamera);
            return true;
          case ViewportSetCommands.IDV_PROJECTION_LOCATION_FRONT:
            setActiveCamera(frontCamera);
            return true;
          default:
            return false;
        }
    }

    /**
    Selects a default camera and rendering configuration for this viewport,
    depending on its position in a set of `numViews` viewports.
    With four or more viewports, the standard arrangement is: Left (wires),
    Perspective, Top (wires) and Front (wires).
    @param numViews
    @param id position of this viewport in the set, starting at 0
    */
    public void applyDefaultConfiguration(int numViews, int id)
    {
        if ( numViews < 4 ) {
            return;
        }
        switch ( id ) {
          case 0:
            setActiveCamera(leftCamera);
            rendererConfiguration.setSurfaces(false);
            rendererConfiguration.setWires(true);
            break;
          case 1:
            setActiveCamera(perspectiveCamera);
            break;
          case 2:
            setActiveCamera(topCamera);
            rendererConfiguration.setSurfaces(false);
            rendererConfiguration.setWires(true);
            break;
          case 3: default:
            setActiveCamera(frontCamera);
            rendererConfiguration.setSurfaces(false);
            rendererConfiguration.setWires(true);
            break;
        }
    }
}

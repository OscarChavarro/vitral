import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import { RendererConfiguration } from "../../environment/material/RendererConfiguration.js";
import { ViewportSetCommands } from "./ViewportSetCommands.js";

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
export class Viewport {
    public static readonly RENDER_MODE_Z_BUFFER = 1;
    public static readonly RENDER_MODE_RAYTRACING = 2;

    private requestedSizeXInPixels: number;
    private requestedSizeYInPixels: number;
    private activeCamera: Camera;
    private readonly perspectiveCamera: Camera;
    private readonly topCamera: Camera;
    private readonly bottomCamera: Camera;
    private readonly leftCamera: Camera;
    private readonly frontCamera: Camera;
    private readonly rendererConfiguration: RendererConfiguration;
    private title: string | null;
    private renderMode: number;
    private showGrid: boolean;

    private startXPercent: number;
    private startYPercent: number;
    private sizeXPercent: number;
    private sizeYPercent: number;
    private border: number;
    private active: boolean;

    private pixelStartX: number;
    private pixelStartY: number;
    private pixelSizeX: number;
    private pixelSizeY: number;

    private titleAreaStartX: number;
    private titleAreaStartY: number;
    private titleAreaSizeX: number;
    private titleAreaSizeY: number;

    public constructor() {
        let r = new Matrix4x4d();

        this.requestedSizeXInPixels = 0;
        this.requestedSizeYInPixels = 0;

        this.perspectiveCamera = new Camera();
        this.perspectiveCamera.setPosition(new Vector3Dd(-5, -5, 5));
        r = r.eulerAnglesRotation(JavaMath.toRadians(45), JavaMath.toRadians(-35), 0);
        this.perspectiveCamera.setRotation(r);
        this.perspectiveCamera.setName("Perspective");

        this.topCamera = new Camera();
        this.topCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        this.topCamera.setPosition(new Vector3Dd(0, 0, 5));
        r = r.eulerAnglesRotation(JavaMath.toRadians(90), JavaMath.toRadians(-90), 0);
        this.topCamera.setRotation(r);
        this.topCamera.setOrthogonalZoom(0.25);
        this.topCamera.setName("Top");

        this.bottomCamera = new Camera();
        this.bottomCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        this.bottomCamera.setPosition(new Vector3Dd(0, 0, -5));
        r = r.eulerAnglesRotation(JavaMath.toRadians(90), JavaMath.toRadians(90), 0);
        this.bottomCamera.setRotation(r);
        this.bottomCamera.setOrthogonalZoom(0.25);
        this.bottomCamera.setName("Bottom");

        this.leftCamera = new Camera();
        this.leftCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        this.leftCamera.setPosition(new Vector3Dd(-5, 0, 0));
        r = r.identity();
        this.leftCamera.setRotation(r);
        this.leftCamera.setOrthogonalZoom(0.25);
        this.leftCamera.setName("Left");

        this.frontCamera = new Camera();
        this.frontCamera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        this.frontCamera.setPosition(new Vector3Dd(0, -5, 0));
        r = r.eulerAnglesRotation(JavaMath.toRadians(90), 0, 0);
        this.frontCamera.setRotation(r);
        this.frontCamera.setOrthogonalZoom(0.25);
        this.frontCamera.setName("Front");

        this.activeCamera = this.perspectiveCamera;
        this.rendererConfiguration = new RendererConfiguration();
        // A new viewport starts with the perspective camera, whose surfaces
        // are shaded with Phong instead of the Gouraud default
        this.rendererConfiguration.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
        this.title = this.activeCamera.getName();
        this.renderMode = Viewport.RENDER_MODE_Z_BUFFER;
        this.showGrid = true;

        this.startXPercent = 0.0;
        this.startYPercent = 0.0;
        this.sizeXPercent = 1.0;
        this.sizeYPercent = 1.0;
        this.border = 2;
        this.active = true;

        this.pixelStartX = 0;
        this.pixelStartY = 0;
        this.pixelSizeX = 0;
        this.pixelSizeY = 0;

        this.titleAreaStartX = 0;
        this.titleAreaStartY = 0;
        this.titleAreaSizeX = 0;
        this.titleAreaSizeY = 0;
    }

    public getRequestedSizeXInPixels(): number {
        return this.requestedSizeXInPixels;
    }

    public setRequestedSizeXInPixels(requestedSizeXInPixels: number): void {
        this.requestedSizeXInPixels = requestedSizeXInPixels;
    }

    public getRequestedSizeYInPixels(): number {
        return this.requestedSizeYInPixels;
    }

    public setRequestedSizeYInPixels(requestedSizeYInPixels: number): void {
        this.requestedSizeYInPixels = requestedSizeYInPixels;
    }

    public getActiveCamera(): Camera {
        return this.activeCamera;
    }

    public setActiveCamera(activeCamera: Camera): void {
        this.activeCamera = activeCamera;
        this.title = activeCamera.getName();
    }

    public getPerspectiveCamera(): Camera {
        return this.perspectiveCamera;
    }

    public getTopCamera(): Camera {
        return this.topCamera;
    }

    public getBottomCamera(): Camera {
        return this.bottomCamera;
    }

    public getLeftCamera(): Camera {
        return this.leftCamera;
    }

    public getFrontCamera(): Camera {
        return this.frontCamera;
    }

    public getRendererConfiguration(): RendererConfiguration {
        return this.rendererConfiguration;
    }

    public getTitle(): string | null {
        return this.title;
    }

    public getRenderMode(): number {
        return this.renderMode;
    }

    public setRenderMode(renderMode: number): void {
        this.renderMode = renderMode;
    }

    public isShowGrid(): boolean {
        return this.showGrid;
    }

    public setShowGrid(showGrid: boolean): void {
        this.showGrid = showGrid;
    }

    public toggleGrid(): void {
        this.showGrid = !this.showGrid;
    }

    public cycleRequestedSize(): void {
        switch (this.requestedSizeXInPixels) {
            case 0:
                this.requestedSizeXInPixels = 320;
                this.requestedSizeYInPixels = 240;
                break;
            case 320:
                this.requestedSizeXInPixels = 640;
                this.requestedSizeYInPixels = 480;
                break;
            case 640:
                this.requestedSizeXInPixels = 800;
                this.requestedSizeYInPixels = 600;
                break;
            case 800:
            default:
                this.requestedSizeXInPixels = 0;
                this.requestedSizeYInPixels = 0;
                break;
        }
    }

    public toggleRenderMode(): void {
        if (this.renderMode === Viewport.RENDER_MODE_Z_BUFFER) {
            this.renderMode = Viewport.RENDER_MODE_RAYTRACING;
        } else {
            this.renderMode = Viewport.RENDER_MODE_Z_BUFFER;
        }
    }

    public updateCameraViewports(width: number, height: number): void {
        this.perspectiveCamera.updateViewportResize(width, height);
        this.topCamera.updateViewportResize(width, height);
        this.bottomCamera.updateViewportResize(width, height);
        this.leftCamera.updateViewportResize(width, height);
        this.frontCamera.updateViewportResize(width, height);
    }

    public getStartXPercent(): number {
        return this.startXPercent;
    }

    public setStartXPercent(startXPercent: number): void {
        this.startXPercent = startXPercent;
    }

    public getStartYPercent(): number {
        return this.startYPercent;
    }

    public setStartYPercent(startYPercent: number): void {
        this.startYPercent = startYPercent;
    }

    public getSizeXPercent(): number {
        return this.sizeXPercent;
    }

    public setSizeXPercent(sizeXPercent: number): void {
        this.sizeXPercent = sizeXPercent;
    }

    public getSizeYPercent(): number {
        return this.sizeYPercent;
    }

    public setSizeYPercent(sizeYPercent: number): void {
        this.sizeYPercent = sizeYPercent;
    }

    /**
    @return the border width, in pixels, left around the projection area
    */
    public getBorder(): number {
        return this.border;
    }

    public setBorder(border: number): void {
        this.border = border;
    }

    /**
    An active viewport is visible and should be rendered. An inactive one is
    hidden (for example when another viewport is maximized).
    @return true if this viewport is visible
    */
    public isActive(): boolean {
        return this.active;
    }

    public setActive(active: boolean): void {
        this.active = active;
    }

    public getPixelStartX(): number {
        return this.pixelStartX;
    }

    public getPixelStartY(): number {
        return this.pixelStartY;
    }

    public getPixelSizeX(): number {
        return this.pixelSizeX;
    }

    public getPixelSizeY(): number {
        return this.pixelSizeY;
    }

    /**
    Informs the area, in pixels, where the title of this viewport is presented.
    Only who presents the title knows its real size, so it must be informed
    each time the title is drawn. Coordinates are relative to the viewport,
    with origin at its upper left corner. An empty area (the initial value)
    means the title has not been presented.
    */
    public setTitleArea(startX: number, startY: number, sizeX: number, sizeY: number): void {
        this.titleAreaStartX = startX;
        this.titleAreaStartY = startY;
        this.titleAreaSizeX = sizeX;
        this.titleAreaSizeY = sizeY;
    }

    public getTitleAreaStartX(): number {
        return this.titleAreaStartX;
    }

    public getTitleAreaStartY(): number {
        return this.titleAreaStartY;
    }

    public getTitleAreaSizeX(): number {
        return this.titleAreaSizeX;
    }

    public getTitleAreaSizeY(): number {
        return this.titleAreaSizeY;
    }

    /**
    @param x coordinate relative to the viewport, origin at its upper left
    corner
    @param y coordinate relative to the viewport, origin at its upper left
    corner
    @return true if the point is over the area where the title is presented
    */
    public isOverTitle(x: number, y: number): boolean {
        return (
            x >= this.titleAreaStartX &&
            x < this.titleAreaStartX + this.titleAreaSizeX &&
            y >= this.titleAreaStartY &&
            y < this.titleAreaStartY + this.titleAreaSizeY
        );
    }

    /**
    Assigns the percent-based area of this viewport inside its container.
    */
    public setPercentArea(startXPercent: number, startYPercent: number, sizeXPercent: number, sizeYPercent: number): void {
        this.startXPercent = startXPercent;
        this.startYPercent = startYPercent;
        this.sizeXPercent = sizeXPercent;
        this.sizeYPercent = sizeYPercent;
    }

    /**
    Given a point (x, y) in the percent space of the container (origin at
    lower left corner), determines if the point is inside this viewport.
    @return true if the point is inside the percent-based area
    */
    public contains(x: number, y: number): boolean {
        return (
            x >= this.startXPercent &&
            x <= this.startXPercent + this.sizeXPercent &&
            y >= this.startYPercent &&
            y <= this.startYPercent + this.sizeYPercent
        );
    }

    /**
    @return true if no specific size in pixels is requested, so the viewport
    uses all the area assigned to it
    */
    public useFullContainerArea(): boolean {
        return this.requestedSizeXInPixels === 0 || this.requestedSizeYInPixels === 0;
    }

    /**
    A container area has valid pixel coordinates from (0, 0) to
    (containerXSize-1, containerYSize-1). This method calculates the pixel
    area of this viewport inside that container from its percent-based area,
    its border and its requested size, and updates the cameras accordingly.
    */
    public updatePixelArea(containerXSize: number, containerYSize: number): void {
        let w: number;
        let h: number;

        this.pixelStartX = Math.trunc(this.startXPercent * containerXSize) + this.border + 1;
        this.pixelStartY = Math.trunc(this.startYPercent * containerYSize) + this.border + 1;
        const subAreaXSize: number = Math.trunc(this.sizeXPercent * containerXSize) - 2 * this.border - 2;
        const subAreaYSize: number = Math.trunc(this.sizeYPercent * containerYSize) - 2 * this.border - 2;
        if (this.useFullContainerArea()) {
            this.pixelSizeX = subAreaXSize;
            this.pixelSizeY = subAreaYSize;
        } else {
            if (this.requestedSizeXInPixels < subAreaXSize) {
                w = this.requestedSizeXInPixels;
            } else {
                w = subAreaXSize;
            }
            if (this.requestedSizeYInPixels < subAreaYSize) {
                h = this.requestedSizeYInPixels;
            } else {
                h = subAreaYSize;
            }
            this.pixelStartX += Math.trunc((subAreaXSize - w) / 2);
            this.pixelStartY += Math.trunc((subAreaYSize - h) / 2);
            this.pixelSizeX = w;
            this.pixelSizeY = h;
        }
        this.updateCameraViewports(this.pixelSizeX, this.pixelSizeY);
    }

    /**
    @return the standard command (see `ViewportSetCommands`) that selects the
    projection location currently used by this viewport
    */
    public getProjectionLocationCommand(): string {
        if (this.activeCamera === this.topCamera) {
            return ViewportSetCommands.IDV_PROJECTION_LOCATION_TOP;
        } else if (this.activeCamera === this.bottomCamera) {
            return ViewportSetCommands.IDV_PROJECTION_LOCATION_BOTTOM;
        } else if (this.activeCamera === this.leftCamera) {
            return ViewportSetCommands.IDV_PROJECTION_LOCATION_LEFT;
        } else if (this.activeCamera === this.frontCamera) {
            return ViewportSetCommands.IDV_PROJECTION_LOCATION_FRONT;
        }
        return ViewportSetCommands.IDV_PROJECTION_LOCATION_PERSPECTIVE;
    }

    /**
    @return the standard command of the render mode popup that corresponds to
    the render mode of this viewport (see `ViewportSetCommands`)
    */
    public getRenderModeCommand(): string {
        if (this.renderMode === Viewport.RENDER_MODE_RAYTRACING) {
            return ViewportSetCommands.IDV_RENDER_MODE_CPU;
        }
        return ViewportSetCommands.IDV_RENDER_MODE_GPU;
    }

    /**
    Selects the render mode given by one of the standard commands of the
    render mode popup (see `ViewportSetCommands`): GPU is the z-buffer of the
    graphics API, CPU is raytracing.
    @return true if the command was a render mode one and was applied
    */
    public selectRenderMode(command: string | null): boolean {
        if (ViewportSetCommands.IDV_RENDER_MODE_GPU === command) {
            this.renderMode = Viewport.RENDER_MODE_Z_BUFFER;
            return true;
        }
        if (ViewportSetCommands.IDV_RENDER_MODE_CPU === command) {
            this.renderMode = Viewport.RENDER_MODE_RAYTRACING;
            return true;
        }
        return false;
    }

    /**
    Selects the projection location given by one of the standard commands of
    the projection location popup (see `ViewportSetCommands`).
    @return true if the command was a projection location one and was applied
    */
    public selectProjectionLocation(command: string | null): boolean {
        if (command === null) {
            return false;
        }
        switch (command) {
            case ViewportSetCommands.IDV_PROJECTION_LOCATION_PERSPECTIVE:
                this.setActiveCamera(this.perspectiveCamera);
                return true;
            case ViewportSetCommands.IDV_PROJECTION_LOCATION_TOP:
                this.setActiveCamera(this.topCamera);
                return true;
            case ViewportSetCommands.IDV_PROJECTION_LOCATION_BOTTOM:
                this.setActiveCamera(this.bottomCamera);
                return true;
            case ViewportSetCommands.IDV_PROJECTION_LOCATION_LEFT:
                this.setActiveCamera(this.leftCamera);
                return true;
            case ViewportSetCommands.IDV_PROJECTION_LOCATION_FRONT:
                this.setActiveCamera(this.frontCamera);
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
    public applyDefaultConfiguration(numViews: number, id: number): void {
        if (numViews < 4) {
            return;
        }
        switch (id) {
            case 0:
                this.setActiveCamera(this.leftCamera);
                this.rendererConfiguration.setSurfaces(false);
                this.rendererConfiguration.setWires(true);
                break;
            case 1:
                this.setActiveCamera(this.perspectiveCamera);
                break;
            case 2:
                this.setActiveCamera(this.topCamera);
                this.rendererConfiguration.setSurfaces(false);
                this.rendererConfiguration.setWires(true);
                break;
            case 3:
            default:
                this.setActiveCamera(this.frontCamera);
                this.rendererConfiguration.setSurfaces(false);
                this.rendererConfiguration.setWires(true);
                break;
        }
    }
}

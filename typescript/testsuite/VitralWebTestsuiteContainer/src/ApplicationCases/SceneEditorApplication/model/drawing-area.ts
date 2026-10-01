import { JavaMath, MouseEvent, Viewport, type Camera, type Vector3Dd, type ViewportSet } from '@vitral/base';
import { InteractionMode } from './interaction-mode';

/**
 * Port of `model.DrawingArea`.
 *
 * State of the drawing area of the editor: the area where a `ViewportSet` is
 * presented and manipulated. It is a plain model, independent of the GUI and
 * rendering technologies in use.
 *
 * Two coordinate systems are related here: the canvas (logical pixels of the
 * GUI component, where the pointer is reported) and the surface (physical pixels
 * of the drawing surface, which differ from the canvas ones in high density
 * displays). The viewport set is always expressed in surface pixels.
 *
 * Java's pending export `File`s are file names here: a page hands written files
 * to the user as downloads (see `io/scene-files.ts`).
 */
export class DrawingArea {
  private readonly viewportSet: ViewportSet;

  private interactionMode: InteractionMode;
  private lastInteractionMode: InteractionMode;

  private colorCaptureRequested: boolean;
  private depthCaptureRequested: boolean;
  private contoursRequested: boolean;
  private projectedViewsDebugRequested: boolean;

  private pendingViewportExportFile: string | null;
  private pendingViewportExportJpg: boolean;
  private pendingWorkspaceExportFile: string | null;

  private canvasWidth: number;
  private canvasHeight: number;

  /**
   * @param viewportSet the viewport set presented by this drawing area
   */
  constructor(viewportSet: ViewportSet) {
    this.viewportSet = viewportSet;

    // As in 3ds Max, the application starts in selection mode
    this.interactionMode = InteractionMode.SELECT;
    this.lastInteractionMode = InteractionMode.SELECT;
    this.colorCaptureRequested = false;
    this.depthCaptureRequested = false;
    this.contoursRequested = false;
    this.projectedViewsDebugRequested = false;
    this.pendingViewportExportFile = null;
    this.pendingViewportExportJpg = false;
    this.pendingWorkspaceExportFile = null;
    this.canvasWidth = 0;
    this.canvasHeight = 0;
  }

  getViewportSet(): ViewportSet {
    return this.viewportSet;
  }

  //= Interaction mode ==================================================

  getInteractionMode(): InteractionMode {
    return this.interactionMode;
  }

  setInteractionMode(interactionMode: InteractionMode): void {
    this.interactionMode = interactionMode;
  }

  getLastInteractionMode(): InteractionMode {
    return this.lastInteractionMode;
  }

  /**
   * Changes the interaction mode, remembering the previous one (the camera
   * mode is transient over a translation, for example).
   * @param interactionMode the new mode
   */
  switchInteractionMode(interactionMode: InteractionMode): void {
    this.lastInteractionMode = this.interactionMode;
    this.interactionMode = interactionMode;
  }

  /**
   * @return true if the translation gizmo is to be shown. The selection mode
   * (as in 3ds Max) only marks the selected objects with the selection
   * corners: it shows no gizmo
   */
  shouldDrawTranslationGizmo(): boolean {
    return (
      this.interactionMode === InteractionMode.TRANSLATE ||
      (this.interactionMode === InteractionMode.CAMERA &&
        this.lastInteractionMode === InteractionMode.TRANSLATE)
    );
  }

  //= Pending requests to the renderer ==================================

  isColorCaptureRequested(): boolean {
    return this.colorCaptureRequested;
  }

  setColorCaptureRequested(colorCaptureRequested: boolean): void {
    this.colorCaptureRequested = colorCaptureRequested;
  }

  isDepthCaptureRequested(): boolean {
    return this.depthCaptureRequested;
  }

  setDepthCaptureRequested(depthCaptureRequested: boolean): void {
    this.depthCaptureRequested = depthCaptureRequested;
  }

  /**
   * @return true if the requested depth capture must be presented as a
   * contours (normal map) image
   */
  isContoursRequested(): boolean {
    return this.contoursRequested;
  }

  setContoursRequested(contoursRequested: boolean): void {
    this.contoursRequested = contoursRequested;
  }

  isProjectedViewsDebugRequested(): boolean {
    return this.projectedViewsDebugRequested;
  }

  setProjectedViewsDebugRequested(projectedViewsDebugRequested: boolean): void {
    this.projectedViewsDebugRequested = projectedViewsDebugRequested;
  }

  /**
   * Requests the export of the selected viewport, to be done in the next frame.
   * @param file destination file name
   * @param jpg true for a JPG file, false for a PNG one
   */
  requestViewportExport(file: string, jpg: boolean): void {
    this.pendingViewportExportFile = file;
    this.pendingViewportExportJpg = jpg;
  }

  /**
   * Requests the export of the whole viewport set area as a JPG, to be done in
   * the next frame.
   * @param file destination file name
   */
  requestWorkspaceExport(file: string): void {
    this.pendingWorkspaceExportFile = file;
  }

  getPendingViewportExportFile(): string | null {
    return this.pendingViewportExportFile;
  }

  isPendingViewportExportJpg(): boolean {
    return this.pendingViewportExportJpg;
  }

  getPendingWorkspaceExportFile(): string | null {
    return this.pendingWorkspaceExportFile;
  }

  clearPendingViewportExport(): void {
    this.pendingViewportExportFile = null;
  }

  clearPendingWorkspaceExport(): void {
    this.pendingWorkspaceExportFile = null;
  }

  //= Viewport set operations ===========================================

  toggleSelectedViewportGrid(): void {
    const selected: Viewport | null = this.viewportSet.getSelectedViewport();

    if (selected !== null) {
      selected.toggleGrid();
    }
  }

  addViewport(): void {
    this.viewportSet.addViewport(new Viewport());
  }

  /**
   * Removes the last viewport, if there is more than one.
   */
  removeLastViewport(): void {
    if (this.viewportSet.getViewportCount() > 1) {
      this.viewportSet.removeViewport(this.viewportSet.getViewportCount() - 1);
    }
  }

  //= Canvas / surface relation =========================================

  getCanvasWidth(): number {
    return this.canvasWidth;
  }

  getCanvasHeight(): number {
    return this.canvasHeight;
  }

  /**
   * Records the size of the canvas, in logical pixels. Empty sizes (i.e. from
   * a component not yet laid out) are ignored.
   */
  updateCanvasSize(width: number, height: number): void {
    if (width <= 0 || height <= 0) {
      return;
    }
    this.canvasWidth = width;
    this.canvasHeight = height;
  }

  /**
   * Records the size of the canvas, in logical pixels, as reported by the
   * last resize of the drawing surface.
   */
  setCanvasSize(width: number, height: number): void {
    this.canvasWidth = width;
    this.canvasHeight = height;
  }

  /**
   * Makes the viewport set match the size of the drawing surface.
   * @param surfaceWidth width in physical pixels
   * @param surfaceHeight height in physical pixels
   */
  updateSurfaceSize(surfaceWidth: number, surfaceHeight: number): void {
    if (surfaceWidth <= 0 || surfaceHeight <= 0) {
      return;
    }
    if (
      surfaceWidth === this.viewportSet.getSizeXInPixels() &&
      surfaceHeight === this.viewportSet.getSizeYInPixels()
    ) {
      return;
    }
    this.viewportSet.resize(surfaceWidth, surfaceHeight);
  }

  scaleXToSurface(x: number): number {
    if (this.canvasWidth <= 0 || this.viewportSet.getSizeXInPixels() <= 0) {
      return x;
    }
    return JavaMath.round((x * this.viewportSet.getSizeXInPixels()) / this.canvasWidth);
  }

  scaleYToSurface(y: number): number {
    if (this.canvasHeight <= 0 || this.viewportSet.getSizeYInPixels() <= 0) {
      return y;
    }
    return JavaMath.round((y * this.viewportSet.getSizeYInPixels()) / this.canvasHeight);
  }

  scaleXToCanvas(x: number): number {
    if (this.canvasWidth <= 0 || this.viewportSet.getSizeXInPixels() <= 0) {
      return x;
    }
    return JavaMath.round((x * this.canvasWidth) / this.viewportSet.getSizeXInPixels());
  }

  scaleYToCanvas(y: number): number {
    if (this.canvasHeight <= 0 || this.viewportSet.getSizeYInPixels() <= 0) {
      return y;
    }
    return JavaMath.round((y * this.canvasHeight) / this.viewportSet.getSizeYInPixels());
  }

  /**
   * @param canvasEvent mouse event with coordinates in canvas pixels
   * @return a copy of the event, with coordinates in surface pixels
   */
  toSurfaceEvent(canvasEvent: MouseEvent): MouseEvent {
    const surfaceEvent: MouseEvent = new MouseEvent();

    surfaceEvent.setX(this.scaleXToSurface(canvasEvent.getX()));
    surfaceEvent.setY(this.scaleYToSurface(canvasEvent.getY()));
    surfaceEvent.setButton(canvasEvent.getButton());
    surfaceEvent.setModifiers(canvasEvent.getModifiers());
    surfaceEvent.setClicks(canvasEvent.getClicks());
    return surfaceEvent;
  }

  /**
   * Projects a point of the scene to canvas pixel coordinates using the
   * active camera of a viewport.
   * @param viewport viewport whose camera is used
   * @param point point in world coordinates
   * @return {x, y} in canvas pixels, or null if the point is behind the camera
   */
  projectToCanvas(viewport: Viewport, point: Vector3Dd): number[] | null {
    const camera: Camera = viewport.getActiveCamera();
    camera.updateVectors();

    const d: Vector3Dd = point.subtract(camera.getPosition());
    const front: Vector3Dd = camera.getFront().normalized();
    const right: Vector3Dd = camera.getLeft().normalized().multiply(-1);
    const up: Vector3Dd = camera.getUp().normalized();
    const depth: number = d.dotProduct(front);

    if (depth <= 0) {
      return null;
    }
    const u: number = ((d.dotProduct(right) / depth) * 0.5) / camera.getRightWithScale().length();
    const v: number = ((d.dotProduct(up) / depth) * 0.5) / camera.getUpWithScale().length();
    const w: number = camera.getViewportXSize();
    const h: number = camera.getViewportYSize();
    const surfaceX: number = viewport.getPixelStartX() + u * w + w / 2.0;
    const surfaceY: number = viewport.getPixelStartY() + h / 2.0 - 1 - v * h;

    return [
      (surfaceX * this.canvasWidth) / this.viewportSet.getSizeXInPixels(),
      (surfaceY * this.canvasHeight) / this.viewportSet.getSizeYInPixels(),
    ];
  }
}

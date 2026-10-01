import { RendererConfiguration, type Camera, type Viewport } from '@vitral/base';
import { CameraState } from './camera-state';

/**
 * Port of `model.history.ViewportState`.
 *
 * How a viewport shows the scene: which of its cameras is active (the
 * projection location: perspective or one of the parallel projections), the
 * state of each of its cameras, and its display settings (render mode, grid,
 * requested image size and renderer configuration). The placement of the
 * viewport in its viewport set (layout, maximization) is not part of it.
 */
export class ViewportState {
  private readonly viewport: Viewport;
  private readonly activeCamera: Camera | null;
  private readonly cameraStates: CameraState[];
  private readonly renderMode: number;
  private readonly showGrid: boolean;
  private readonly requestedSizeXInPixels: number;
  private readonly requestedSizeYInPixels: number;
  private readonly rendererConfiguration: RendererConfiguration;

  private constructor(viewport: Viewport) {
    const cameras: Camera[] = ViewportState.camerasOf(viewport);

    this.viewport = viewport;
    this.activeCamera = viewport.getActiveCamera();
    this.cameraStates = cameras.map((camera: Camera): CameraState => CameraState.capture(camera));
    this.renderMode = viewport.getRenderMode();
    this.showGrid = viewport.isShowGrid();
    this.requestedSizeXInPixels = viewport.getRequestedSizeXInPixels();
    this.requestedSizeYInPixels = viewport.getRequestedSizeYInPixels();
    this.rendererConfiguration = new RendererConfiguration();
    this.rendererConfiguration.clone(viewport.getRendererConfiguration());
  }

  /**
   * @param viewport viewport whose state is captured
   * @return the current state of the viewport
   */
  static capture(viewport: Viewport): ViewportState {
    return new ViewportState(viewport);
  }

  private static camerasOf(viewport: Viewport): Camera[] {
    return [
      viewport.getPerspectiveCamera(),
      viewport.getTopCamera(),
      viewport.getBottomCamera(),
      viewport.getLeftCamera(),
      viewport.getFrontCamera(),
    ];
  }

  /**
   * @return the viewport whose state was captured
   */
  getViewport(): Viewport {
    return this.viewport;
  }

  /**
   * Gives back the captured state to the viewport. Its renderer configuration
   * is updated in place, as interaction techniques may hold it.
   */
  restore(): void {
    for (const state of this.cameraStates) {
      state.restore();
    }
    if (this.activeCamera !== null) {
      this.viewport.setActiveCamera(this.activeCamera);
    }
    this.viewport.setRenderMode(this.renderMode);
    this.viewport.setShowGrid(this.showGrid);
    this.viewport.setRequestedSizeXInPixels(this.requestedSizeXInPixels);
    this.viewport.setRequestedSizeYInPixels(this.requestedSizeYInPixels);
    this.viewport.getRendererConfiguration().clone(this.rendererConfiguration);
  }

  /**
   * @param later state of the same viewport captured after a change
   * @return name of the change, for the user: a projection change (other
   * active camera), a camera movement, or a display change (render mode,
   * grid, image size, renderer configuration)
   */
  describeChangeTo(later: ViewportState): string {
    if (later.activeCamera !== this.activeCamera) {
      return 'Projection change';
    }
    for (let i = 0; i < this.cameraStates.length; i++) {
      if (!this.cameraStates[i].isSameState(later.cameraStates[i])) {
        return 'Camera movement';
      }
    }
    return 'Display change';
  }

  /**
   * @param other state captured from the same viewport
   * @return true if both states show the scene the same way
   */
  isSameState(other: ViewportState | null): boolean {
    if (
      other === null ||
      other.viewport !== this.viewport ||
      other.activeCamera !== this.activeCamera ||
      other.renderMode !== this.renderMode ||
      other.showGrid !== this.showGrid ||
      other.requestedSizeXInPixels !== this.requestedSizeXInPixels ||
      other.requestedSizeYInPixels !== this.requestedSizeYInPixels ||
      other.rendererConfiguration.compareTo(this.rendererConfiguration) !== 0
    ) {
      return false;
    }
    for (let i = 0; i < this.cameraStates.length; i++) {
      if (!this.cameraStates[i].isSameState(other.cameraStates[i])) {
        return false;
      }
    }
    return true;
  }
}

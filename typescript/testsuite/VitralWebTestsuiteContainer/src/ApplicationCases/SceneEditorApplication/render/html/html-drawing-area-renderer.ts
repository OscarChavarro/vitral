import {
  CameraControlGizmo2DPainter,
  Gizmo2DDrawList,
  InputGizmo2DPainter,
  Logger,
  RendererConfiguration,
  RotateGizmo2DPainter,
  ScaleGizmo2DPainter,
  TranslateGizmo2DPainter,
  VSDK,
  Viewport,
  type Camera,
  type CameraControlGizmo,
  type ColorRgb,
  type InputGizmo,
  type RotateGizmo,
  type ScaleGizmo,
  type TranslateGizmo,
  type ViewportSet,
} from '@vitral/base';
import { HtmlGizmo2DDrawListRenderer, HtmlImageRenderer } from '@vitral/webgl';
import type { ApplicationModel } from '../../model/application-model';
import type { DrawingArea } from '../../model/drawing-area';
import type { Scene } from '../../model/scene';
import { SceneSelectionEditor } from '../../model/selection/scene-selection-editor';
import { DrawingAreaGizmoPresenter, GizmoKind } from '../drawing-area-gizmo-presenter';
import type { DrawingAreaHost } from '../drawing-area-host';
import { FrameCaptureService } from '../frame-capture-service';
import type { ProjectedLineLayer } from '../projected-line-layer';
import { ProjectedLinesViewBuilder } from '../projected-lines-view-builder';
import type { ProjectedPolygon } from '../projected-polygon';
import { ProjectedSurfacesViewBuilder } from '../projected-surfaces-view-builder';
import { HtmlFrameBufferSource } from './html-frame-buffer-source';
import { HtmlProjectedLineLayerRenderer } from './html-projected-line-layer-renderer';
import { HtmlProjectedPolygonRenderer } from './html-projected-polygon-renderer';
import { HtmlViewportSetRenderer } from './html-viewport-set-renderer';

/**
 * Port of `render.awt.AwtDrawingAreaRenderer`.
 *
 * Draws the scene in the viewports of a `DrawingArea` using only the 2D
 * operations of an HTML canvas (`CanvasRenderingContext2D`), the counterpart
 * of `WebGLDrawingAreaRenderer` for a drawing surface without any 3D API: no
 * WebGL context is created. As a 2D context knows nothing about cameras or
 * depth, every viewport is computed in the processor, in one of the render
 * modes the canvas offers (see `RenderTechnology.CANVAS2D`):
 *   - Hidden lines: the scene is projected as lines by
 *     `ProjectedLinesViewBuilder` (wireframe, or hidden line removal by
 *     Appel's algorithm for the solids when the rendering configuration asks
 *     for surfaces) and drawn by `HtmlProjectedLineLayerRenderer`.
 *   - Raytracing: the raytraced image (computed by the tile parallel
 *     raytracer of the editor, in Web Workers) is copied as is
 *     (`putImageData`), and the editor annotations (selection corners,
 *     lights...) are drawn over it as lines, without depth test.
 *   - Hidden surfaces: the visible polygons of the solids, computed by
 *     `ProjectedSurfacesViewBuilder` (hidden surface removal by polygon area
 *     sorting of Weiler and Atherton, with the shadows of the solids if the
 *     viewport asks for them), are filled by `HtmlProjectedPolygonRenderer`,
 *     and the annotations are drawn over them as over a raytraced image. When
 *     the rendering configuration turns the surfaces off and the lines on,
 *     only the contours of those polygons are drawn: with constant shading
 *     (no light) all of them with the line color of the configuration,
 *     otherwise each one with the color of its polygon.
 *
 * Over the selected viewport the camera control gizmo (ViewCube) is drawn as a
 * HUD at its upper right corner, with the hidden lines of its parts removed by
 * Appel's algorithm (see `CameraControlGizmo2DPainter`).
 *
 * Over both, the manipulation gizmo of the interaction mode and the input
 * gizmo (numeric boxes) of the selected viewport are drawn as 2D primitives
 * computed by the gizmo painters of `vsdk.toolkit.render.gizmo` (hidden
 * surfaces solved with the painter's algorithm) and drawn by
 * `HtmlGizmo2DDrawListRenderer`.
 *
 * The frame is composed in an off-screen canvas with one pixel for each
 * physical pixel of the screen (the "surface" of `DrawingArea`), which is also
 * the frame buffer read by `FrameCaptureService`, and copied to the canvas of
 * the page at its end, so an asynchronous frame (raytracing) never shows half
 * drawn.
 *
 * Not presented yet in this technology: the visual debug ray, the depth buffer
 * captures and the projected views debugger.
 */
export class HtmlDrawingAreaRenderer {
  private static readonly SET_BACKGROUND = 'rgb(196,196,196)';

  private readonly model: ApplicationModel;
  private readonly theScene: Scene;
  private readonly drawingArea: DrawingArea;
  private readonly viewportSet: ViewportSet;
  private readonly host: DrawingAreaHost;
  private readonly frameCapture: FrameCaptureService;
  private readonly linesBuilder: ProjectedLinesViewBuilder;
  private readonly surfacesBuilder: ProjectedSurfacesViewBuilder;
  private readonly viewportSetRenderer: HtmlViewportSetRenderer;
  private readonly gizmoPresenter: DrawingAreaGizmoPresenter;
  private readonly gizmoDrawList: Gizmo2DDrawList;
  private readonly cameraControlDrawList: Gizmo2DDrawList;
  private readonly cameraControlPainter: CameraControlGizmo2DPainter;
  /// Off-screen frame (Java's `BufferedImage`) and its 2D context
  private frame: CanvasRenderingContext2D | null;

  /**
   * @param model application model
   * @param host services from the GUI technology hosting the surface
   * @param selectedViewportListener notified with the viewport about to be
   * drawn when it is the selected one (or the only one shown), so interaction
   * techniques can work over its camera
   * @param translationGizmo gizmo to draw in translation mode
   * @param rotateGizmo gizmo to draw in rotation mode
   * @param scaleGizmo gizmo to draw in scale mode
   */
  constructor(
    model: ApplicationModel,
    host: DrawingAreaHost,
    selectedViewportListener: (viewport: Viewport) => void,
    translationGizmo: TranslateGizmo,
    rotateGizmo: RotateGizmo,
    scaleGizmo: ScaleGizmo,
  ) {
    this.model = model;
    this.theScene = model.getScene();
    this.drawingArea = model.getDrawingArea();
    this.viewportSet = this.drawingArea.getViewportSet();
    this.host = host;
    this.frameCapture = new FrameCaptureService(model, host);
    this.linesBuilder = new ProjectedLinesViewBuilder();
    this.surfacesBuilder = new ProjectedSurfacesViewBuilder();
    this.frame = null;
    this.gizmoPresenter = new DrawingAreaGizmoPresenter(
      this.drawingArea,
      new SceneSelectionEditor(this.theScene),
      translationGizmo,
      rotateGizmo,
      scaleGizmo,
    );
    this.gizmoDrawList = new Gizmo2DDrawList();
    this.cameraControlDrawList = new Gizmo2DDrawList();
    this.cameraControlPainter = new CameraControlGizmo2DPainter();

    this.viewportSetRenderer = new HtmlViewportSetRenderer(this.viewportSet, {
      configureView: (viewport: Viewport): void => {
        selectedViewportListener(viewport);
      },
      drawView: (
        g: CanvasRenderingContext2D,
        viewport: Viewport,
        x: number,
        y: number,
        width: number,
        height: number,
      ): Promise<void> => this.drawView(g, viewport, x, y, width, height),
    });
  }

  /**
   * Draws a frame.
   * @param target 2D context of the canvas presenting the drawing area; its
   * size is the one of the canvas in physical pixels
   * @param canvasWidth width of the canvas, in logical pixels
   * @param canvasHeight height of the canvas, in logical pixels
   * @param pixelScale physical pixels for each logical pixel (i.e. 2 on high
   * density displays)
   */
  async display(
    target: CanvasRenderingContext2D,
    canvasWidth: number,
    canvasHeight: number,
    pixelScale: number,
  ): Promise<void> {
    if (canvasWidth <= 0 || canvasHeight <= 0) {
      return;
    }
    this.host.beforeFrame();
    this.reportUnsupportedRequests();
    this.drawingArea.updateSurfaceSize(
      Math.round(canvasWidth * pixelScale),
      Math.round(canvasHeight * pixelScale),
    );
    this.viewportSet.enforceAvailableRenderModes();

    const width: number = this.viewportSet.getSizeXInPixels();
    const height: number = this.viewportSet.getSizeYInPixels();
    if (
      this.frame === null ||
      this.frame.canvas.width !== width ||
      this.frame.canvas.height !== height
    ) {
      this.frame = HtmlDrawingAreaRenderer.createFrame(width, height);
    }

    const g: CanvasRenderingContext2D = this.frame;
    g.save();
    try {
      g.fillStyle = HtmlDrawingAreaRenderer.SET_BACKGROUND;
      g.fillRect(0, 0, width, height);
      await this.viewportSetRenderer.draw(g, this.host.isFullScreenGuiMode());
    } finally {
      g.restore();
    }

    if (this.frameCapture.isFrameExportPending()) {
      await this.frameCapture.exportPendingFrame(new HtmlFrameBufferSource(g, 0, 0, width, height));
    }

    target.drawImage(g.canvas, 0, 0, target.canvas.width, target.canvas.height);
  }

  /**
   * @return the 2D context of a new off-screen canvas of the given size
   */
  private static createFrame(width: number, height: number): CanvasRenderingContext2D {
    const canvas: HTMLCanvasElement = document.createElement('canvas');
    canvas.width = Math.max(1, width);
    canvas.height = Math.max(1, height);
    // Captures read the frame back (see `HtmlFrameBufferSource`)
    const context: CanvasRenderingContext2D | null = canvas.getContext('2d', {
      willReadFrequently: true,
    });
    if (context === null) {
      throw new Error('The scene editor requires a canvas 2D context.');
    }
    return context;
  }

  private async drawView(
    g: CanvasRenderingContext2D,
    viewport: Viewport,
    x: number,
    y: number,
    width: number,
    height: number,
  ): Promise<void> {
    let layers: ProjectedLineLayer[];
    const raytraced: boolean = viewport.getRenderMode() === Viewport.RENDER_MODE_RAYTRACING;
    const hiddenSurfaces: boolean =
      viewport.getRenderMode() === Viewport.RENDER_MODE_HIDDEN_SURFACES;

    this.theScene.activeCamera = viewport.getActiveCamera();
    this.theScene.qualityTemplate = viewport.getRendererConfiguration();

    if (raytraced) {
      await this.theScene.activateSelectedBackground();
      this.model.setRaytracedImageWidth(width);
      this.model.setRaytracedImageHeight(height);
      await this.host.raytraceImage();
      HtmlImageRenderer.draw(g, this.model.getRaytracedImage(), x, y);
    } else {
      g.fillStyle = HtmlProjectedLineLayerRenderer.toCssColor(
        this.theScene.simpleBackground.colorInDireccion(this.theScene.activeCamera.getFront()),
      );
      g.fillRect(x, y, width, height);
    }

    if (hiddenSurfaces) {
      this.drawHiddenSurfaces(g, viewport, x, y, width, height);
    }

    try {
      layers = this.linesBuilder.buildView(
        this.theScene,
        viewport,
        this.host.getBodyEditFeedbackProvider(),
        !raytraced && !hiddenSurfaces,
      );
    } catch (e) {
      // A failure of the line algorithms over some body must not break
      // the presentation of the whole set
      Logger.reportMessage(
        this,
        VSDK.WARNING,
        'drawView',
        'Can not compute the lines of viewport "' +
          this.viewportSet.getTitleFor(viewport) +
          '": ' +
          e,
      );
      layers = [];
    }
    HtmlProjectedLineLayerRenderer.draw(
      g,
      layers,
      x,
      y,
      width,
      height,
      this.viewportSet.getElementScaler().getScale(),
    );

    // Must be the last to draw, over the scene
    this.drawGizmos(g, viewport, x, y, width, height);

    this.frameCapture.copyColorBufferIfNeeded(
      new HtmlFrameBufferSource(g, x, y, width, height),
      this.viewportSet.isSelected(viewport),
    );
  }

  private drawHiddenSurfaces(
    g: CanvasRenderingContext2D,
    viewport: Viewport,
    x: number,
    y: number,
    width: number,
    height: number,
  ): void {
    let polygons: readonly ProjectedPolygon[];

    try {
      polygons = this.surfacesBuilder.buildView(
        this.theScene,
        viewport,
        width,
        height,
        this.drawingArea.getContentGeneration(),
        viewport.getRendererConfiguration().isWithShadowsSet(),
      );
    } catch (e) {
      // As with the lines, a failure over some body must not break the
      // presentation of the whole set
      Logger.reportMessage(
        this,
        VSDK.WARNING,
        'drawHiddenSurfaces',
        'Can not compute the visible surfaces of viewport "' +
          this.viewportSet.getTitleFor(viewport) +
          '": ' +
          e,
      );
      return;
    }

    const quality: RendererConfiguration = viewport.getRendererConfiguration();
    if (!quality.isSurfacesSet() && quality.isWiresSet()) {
      let lineColor: ColorRgb | null = null;
      if (quality.getShadingType() === RendererConfiguration.SHADING_TYPE_NOLIGHT) {
        lineColor = quality.getWireColor();
      }
      HtmlProjectedPolygonRenderer.drawContours(
        g,
        polygons,
        x,
        y,
        width,
        height,
        Math.max(1.0, this.viewportSet.getElementScaler().getScale()),
        lineColor,
      );
    } else {
      HtmlProjectedPolygonRenderer.draw(g, polygons, x, y, width, height);
    }
  }

  private drawGizmos(
    g: CanvasRenderingContext2D,
    viewport: Viewport,
    x: number,
    y: number,
    width: number,
    height: number,
  ): void {
    const camera: Camera = this.theScene.activeCamera;

    this.gizmoDrawList.clear();
    switch (this.gizmoPresenter.prepareForView(camera)) {
      case GizmoKind.TRANSLATE:
        TranslateGizmo2DPainter.paint(
          this.gizmoDrawList,
          this.gizmoPresenter.getTranslationGizmo(),
          camera,
          this.viewportSet.getElementScaler(),
        );
        break;
      case GizmoKind.ROTATE:
        RotateGizmo2DPainter.paint(
          this.gizmoDrawList,
          this.gizmoPresenter.getRotateGizmo(),
          camera,
          this.viewportSet.getElementScaler(),
        );
        break;
      case GizmoKind.SCALE:
        ScaleGizmo2DPainter.paint(this.gizmoDrawList, this.gizmoPresenter.getScaleGizmo(), camera);
        break;
      default:
        break;
    }

    // Only the selected viewport shows them: it is the one that receives the keyboard
    let inputGizmo: InputGizmo | null = this.gizmoPresenter.getInputGizmo();
    const rayGizmo = this.model.getRayGizmo();
    if (inputGizmo === null && rayGizmo !== null && rayGizmo.isVisible()) {
      inputGizmo = rayGizmo.getInputGizmo();
    }
    if (inputGizmo !== null && this.viewportSet.isSelected(viewport)) {
      InputGizmo2DPainter.paint(
        this.gizmoDrawList,
        inputGizmo,
        width,
        height,
        this.viewportSet.getElementScaler(),
        HtmlGizmo2DDrawListRenderer.TEXT_MEASURER,
      );
    }
    HtmlGizmo2DDrawListRenderer.draw(g, this.gizmoDrawList, x, y);

    // The HUD of the selected viewport is over everything
    if (this.viewportSet.isSelected(viewport)) {
      const cameraControl: CameraControlGizmo = this.model.getCameraControlGizmo();

      this.cameraControlDrawList.clear();
      cameraControl.placeInViewport(viewport, this.viewportSet.getElementScaler());
      this.cameraControlPainter.paint(
        this.cameraControlDrawList,
        cameraControl,
        this.viewportSet.getElementScaler(),
      );
      HtmlGizmo2DDrawListRenderer.draw(g, this.cameraControlDrawList, x, y);
    }
  }

  /**
   * Captures that need a depth buffer or a 3D API are not available in a 2D
   * canvas: they are reported and dropped, instead of being left pending.
   */
  private reportUnsupportedRequests(): void {
    if (this.drawingArea.isDepthCaptureRequested()) {
      this.drawingArea.setDepthCaptureRequested(false);
      this.drawingArea.setContoursRequested(false);
      this.host.showStatusMessage(
        'Depth buffer captures are not available when rendering with the canvas 2D context',
      );
    }
    if (this.drawingArea.isProjectedViewsDebugRequested()) {
      this.drawingArea.setProjectedViewsDebugRequested(false);
      this.host.showStatusMessage(
        'Projected views debugging is not available when rendering with the canvas 2D context',
      );
    }
  }
}

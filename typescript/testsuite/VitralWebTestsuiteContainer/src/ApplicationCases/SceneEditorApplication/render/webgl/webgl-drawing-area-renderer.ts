import type {
  InputGizmo,
  RotateGizmo,
  ScaleGizmo,
  TranslateGizmo,
  Viewport,
  ViewportSet,
} from '@vitral/base';
import {
  WebGLColorDepthImageRenderer,
  WebGLOffscreenFrameBuffer,
  WebGLRayGizmoRenderer,
  WebGLRenderer,
  WebGLRotateGizmoRenderer,
  WebGLScaleGizmoRenderer,
  WebGLSimpleCorridorSample,
  WebGLTranslateGizmoRenderer,
  WebGLViewportSetRenderer,
  WebGLViewportWindow,
  type WebGLLabelImageProvider,
} from '@vitral/webgl';
import type { ApplicationModel } from '../../model/application-model';
import type { DrawingArea } from '../../model/drawing-area';
import type { Scene } from '../../model/scene';
import { SceneSelectionEditor } from '../../model/selection/scene-selection-editor';
import { DrawingAreaGizmoPresenter, GizmoKind } from '../drawing-area-gizmo-presenter';
import type { DrawingAreaHost } from '../drawing-area-host';
import { FrameCaptureService } from '../frame-capture-service';
import { ProjectedViewsDebugger } from '../projected-views-debugger';
import { WebGLFrameBufferSource } from './webgl-frame-buffer-source';
import { WebGLProjectedViewRenderer } from './webgl-projected-view-renderer';
import { WebGLSceneRenderer } from './webgl-scene-renderer';

/**
 * Port of `render.jogl.Jogl4DrawingAreaRenderer`.
 *
 * Draws the scene in the viewports of a `DrawingArea`, using WebGL: the
 * viewports, the manipulation gizmos and the visual debug ray. It does not
 * depend on the GUI technology hosting the drawing surface (see
 * `DrawingAreaHost`). What to draw is decided by technology independent
 * services (`DrawingAreaGizmoPresenter`, `FrameCaptureService`,
 * `ProjectedViewsDebugger`); this class only issues the WebGL calls.
 *
 * Java's `GLEventListener` callbacks are `init`, `display`, `reshape` and
 * `dispose`, called by the canvas host. A WebGL renderer of the toolkit draws
 * asynchronously (a shader source arrives over `fetch`), and the browser
 * presents and clears the canvas whenever a task ends; so each frame is drawn
 * into a `WebGLOffscreenFrameBuffer` and copied to the canvas at its end
 * (`presentToCanvas`), which also gives the depth texture WebGL needs to read
 * the depth buffer (see `WebGLFrameBufferSource`).
 */
export class WebGLDrawingAreaRenderer {
  private readonly theScene: Scene;
  private readonly model: ApplicationModel;
  private readonly drawingArea: DrawingArea;
  private readonly viewportSet: ViewportSet;
  private readonly host: DrawingAreaHost;

  private readonly gizmoPresenter: DrawingAreaGizmoPresenter;
  /// Gizmo drawn in the view being drawn
  private gizmoDrawn: GizmoKind;

  private readonly frameCapture: FrameCaptureService;
  private readonly projectedViewsDebugger: ProjectedViewsDebugger;
  private readonly viewportSetRenderer: WebGLViewportSetRenderer;
  /// Test corridor shown when `Scene.showCorridor` is set
  private readonly corridor: WebGLSimpleCorridorSample;
  /// Frame buffer each frame is drawn into, per context
  private frameBuffer: WebGLOffscreenFrameBuffer | null = null;
  private frameBufferContext: WebGL2RenderingContext | null = null;

  /**
   * @param model application model
   * @param host services from the GUI technology hosting the surface
   * @param labelImageProvider creates the images for the texts of viewports
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
    labelImageProvider: WebGLLabelImageProvider,
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
    this.gizmoPresenter = new DrawingAreaGizmoPresenter(
      this.drawingArea,
      new SceneSelectionEditor(this.theScene),
      translationGizmo,
      rotateGizmo,
      scaleGizmo,
    );
    this.gizmoDrawn = GizmoKind.NONE;

    this.frameCapture = new FrameCaptureService(model, host);
    this.projectedViewsDebugger = new ProjectedViewsDebugger(model, host);
    this.corridor = new WebGLSimpleCorridorSample();

    this.viewportSetRenderer = new WebGLViewportSetRenderer(this.viewportSet, labelImageProvider, {
      configureView: (view: WebGLViewportWindow): void => {
        selectedViewportListener(view.getViewport());
      },
      drawView: async (gl: WebGL2RenderingContext, view: WebGLViewportWindow): Promise<void> => {
        this.theScene.activeCamera = view.getCamera();
        this.theScene.qualityTemplate = view.getRendererConfiguration();
        await this.drawView(gl, view);
      },
    });
  }

  private async drawGizmos(gl: WebGL2RenderingContext): Promise<void> {
    // Pending: Turn off scene light and turn on gizmo specific lighting
    this.gizmoDrawn = this.gizmoPresenter.prepareForView(this.theScene.activeCamera);

    gl.clear(gl.DEPTH_BUFFER_BIT);

    switch (this.gizmoDrawn) {
      case GizmoKind.TRANSLATE:
        await WebGLTranslateGizmoRenderer.draw(gl, this.gizmoPresenter.getTranslationGizmo(), this.theScene.activeCamera);
        break;
      case GizmoKind.ROTATE:
        await WebGLRotateGizmoRenderer.draw(gl, this.gizmoPresenter.getRotateGizmo(), this.theScene.activeCamera);
        break;
      case GizmoKind.SCALE:
        await WebGLScaleGizmoRenderer.draw(gl, this.gizmoPresenter.getScaleGizmo(), this.theScene.activeCamera);
        break;
      default:
        break;
    }
    gl.enable(gl.DEPTH_TEST);
  }

  private async drawView(gl: WebGL2RenderingContext, view: WebGLViewportWindow): Promise<void> {
    if (!view.isActive()) {
      return;
    }

    if (view.getRenderMode() === WebGLViewportWindow.RENDER_MODE_ZBUFFER) {
      await WebGLSceneRenderer.draw(gl, this.theScene, this.host.getBodyEditFeedbackProvider());
      if (this.theScene.showCorridor) {
        await this.corridor.drawGL(gl, this.theScene.activeCamera.calculateProjectionMatrix());
        // The corridor turns on face culling, what follows expects it off
        gl.disable(gl.CULL_FACE);
      }
    } else {
      await this.theScene.activateSelectedBackground();
      this.model.setRaytracedImageWidth(view.getViewportSizeX());
      this.model.setRaytracedImageHeight(view.getViewportSizeY());
      await this.host.raytraceImage();
      // The raytraced image comes with its OpenGL depth: after this,
      // what is rasterized (grid, gizmos, selection) is depth tested
      // against the raytraced bodies as against rasterized ones
      await WebGLColorDepthImageRenderer.draw(gl, this.model.getRaytracedImage(), this.model.getRaytracedDepth());
      await WebGLSceneRenderer.drawEditorOverlays(gl, this.theScene, this.host.getBodyEditFeedbackProvider());
    }

    //-----------------------------------------------------------------
    const rayGizmo = this.model.getRayGizmo()!;
    rayGizmo.acquireSnapshot();
    await WebGLRayGizmoRenderer.draw(gl, rayGizmo, this.theScene.activeCamera,
      this.theScene.scene.getLights().toArray());

    await view.drawGrid(gl);

    //-----------------------------------------------------------------
    // Note that gizmo information will not be reported, as they damage
    // the z-buffer...
    const frameBuffer: WebGLFrameBufferSource = new WebGLFrameBufferSource(gl, this.frameBuffer!);
    await this.frameCapture.copyZBufferIfNeeded(frameBuffer);

    // Must be the last to draw
    await this.drawGizmos(gl);

    this.frameCapture.copyColorBufferIfNeeded(frameBuffer, view.isSelected());

    await view.drawReferenceBase(gl);
    if (this.gizmoDrawn === GizmoKind.TRANSLATE) {
      await view.drawLabelsForTranslateGizmo(gl, this.gizmoPresenter.getTranslationGizmo());
    }
    if (this.gizmoDrawn === GizmoKind.ROTATE) {
      await view.drawLabelForRotateGizmoArc(gl, this.gizmoPresenter.getRotateGizmo());
    }
    // Only the selected viewport shows them: it is the one that receives the keyboard
    let inputGizmo: InputGizmo | null = this.gizmoPresenter.getInputGizmo();
    if (inputGizmo === null && rayGizmo.isVisible()) {
      inputGizmo = rayGizmo.getInputGizmo();
    }
    if (inputGizmo !== null && view.isSelected()) {
      await view.drawInputGizmo(gl, inputGizmo);
    }
  }

  /**
   * Java's `display(GLAutoDrawable)`: draws one frame.
   * @param gl WebGL context of the canvas
   * @param surfaceWidth width of the drawing buffer, in physical pixels
   * @param surfaceHeight height of the drawing buffer, in physical pixels
   */
  async display(gl: WebGL2RenderingContext, surfaceWidth: number, surfaceHeight: number): Promise<void> {
    const target: WebGLOffscreenFrameBuffer = this.obtainFrameBuffer(gl);

    // Text size and viewport state follow the component showing the surface
    this.host.beforeFrame();
    await this.projectedViewsDebugger.debugIfNeeded(new WebGLProjectedViewRenderer(gl));
    this.drawingArea.updateSurfaceSize(surfaceWidth, surfaceHeight);

    //-----------------------------------------------------------------
    target.bind(surfaceWidth, surfaceHeight);
    gl.viewport(0, 0, this.viewportSet.getSizeXInPixels(), this.viewportSet.getSizeYInPixels());
    gl.clearColor(0.77, 0.77, 0.77, 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    gl.enable(gl.DEPTH_TEST);

    await this.viewportSetRenderer.draw(gl, this.host.isFullScreenGuiMode());

    if (this.frameCapture.isFrameExportPending()) {
      gl.viewport(0, 0, this.viewportSet.getSizeXInPixels(), this.viewportSet.getSizeYInPixels());
      await this.frameCapture.exportPendingFrame(new WebGLFrameBufferSource(gl, target));
    }

    target.presentToCanvas();
  }

  /**
   * Java's `init(GLAutoDrawable)`.
   */
  init(): void {
    // A new WebGL context (i.e. a new canvas) does not have the textures
    // created by the previous one
    this.viewportSetRenderer.invalidateGlResources();
  }

  /**
   * Java's `dispose(GLAutoDrawable)`.
   * @param gl WebGL context about to be discarded
   */
  dispose(gl: WebGL2RenderingContext): void {
    this.viewportSetRenderer.disposeGlResources(gl);
    this.corridor.dispose(gl);
    WebGLRayGizmoRenderer.dispose(gl);
    WebGLRenderer.disposeAll(gl);
    if (this.frameBuffer !== null && this.frameBufferContext === gl) {
      this.frameBuffer.release();
      this.frameBuffer = null;
      this.frameBufferContext = null;
    }
  }

  /**
   * Java's `reshape(GLAutoDrawable, x, y, width, height)`.
   * @param width width of the drawing buffer, in physical pixels
   * @param height height of the drawing buffer, in physical pixels
   */
  reshape(width: number, height: number): void {
    this.host.beforeFrame();
    this.drawingArea.updateSurfaceSize(width, height);
  }

  private obtainFrameBuffer(gl: WebGL2RenderingContext): WebGLOffscreenFrameBuffer {
    if (this.frameBuffer === null || this.frameBufferContext !== gl) {
      this.frameBuffer = new WebGLOffscreenFrameBuffer(gl);
      this.frameBufferContext = gl;
    }
    return this.frameBuffer;
  }
}

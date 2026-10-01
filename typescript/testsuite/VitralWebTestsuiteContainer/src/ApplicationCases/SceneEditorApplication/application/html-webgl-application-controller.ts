import type { ColorRgb, RGBAImageUncompressed, Vector3Dd, Viewport } from '@vitral/base';
import { WebSystem } from '@vitral/webgl';
import { DrawingAreaInteractionTechniques } from '../gui/drawing-area-interaction-techniques';
import { HtmlDrawingAreaController } from '../gui/html/html-drawing-area-controller';
import { HtmlDrawingAreaFeedback } from '../gui/html/html-drawing-area-feedback';
import type { ApplicationModel } from '../model/application-model';
import type { DrawingArea } from '../model/drawing-area';
import { WebGLDrawingAreaRenderer } from '../render/webgl/webgl-drawing-area-renderer';
import type { HtmlWebGLSceneEditorApplication } from './html-webgl-scene-editor-application';

/**
 * Port of `application.AwtJogl4ApplicationController`.
 *
 * Composition root of the drawing area of the editor: it creates the WebGL
 * canvas and connects the drawing area model with its renderer, its interaction
 * techniques and the HTML adapters, and offers the operations that need the
 * canvas.
 *
 * Java's `GLCanvas` draws a frame on `repaint` / `display`; here `repaint`
 * schedules one frame for the next animation frame of the browser, frames are
 * drawn one at a time (a frame draws asynchronously, see
 * `WebGLDrawingAreaRenderer`), and a repaint requested while a frame is drawn
 * is drawn after it. The drawing buffer has the size of the canvas in physical
 * pixels, as a `GLCanvas` surface has in a HiDPI display.
 */
export class HtmlWebGLApplicationController {
  private readonly model: ApplicationModel;
  private canvas: HTMLCanvasElement | null = null;
  private gl: WebGL2RenderingContext | null = null;
  private htmlController: HtmlDrawingAreaController | null = null;
  private htmlFeedback: HtmlDrawingAreaFeedback | null = null;
  private renderer: WebGLDrawingAreaRenderer | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private reshaped = false;
  private framePending = false;
  private frameInProgress = false;
  private repaintAfterFrame = false;
  private displayWaiters: (() => void)[] = [];

  constructor(model: ApplicationModel) {
    this.model = model;
  }

  private createDrawingArea(application: HtmlWebGLSceneEditorApplication): void {
    const drawingArea: DrawingArea = this.model.getDrawingArea();

    //-----------------------------------------------------------------
    const canvas: HTMLCanvasElement = document.createElement('canvas');
    canvas.className = 'scene-editor-canvas';
    canvas.style.minWidth = '8px';
    canvas.style.minHeight = '8px';
    const gl: WebGL2RenderingContext | null = canvas.getContext('webgl2', { depth: true, antialias: false });
    if (gl === null) {
      throw new Error('The scene editor requires WebGL2 support.');
    }
    this.canvas = canvas;
    this.gl = gl;

    //-----------------------------------------------------------------
    this.htmlFeedback = new HtmlDrawingAreaFeedback(application, canvas);
    const techniques: DrawingAreaInteractionTechniques = new DrawingAreaInteractionTechniques(
      this.model,
      this.htmlFeedback,
    );
    // While dragging the gizmo, the cursor wraps around its viewport
    techniques.setCursorWrapEnabled(this.htmlFeedback.isCursorWarpAvailable());

    //-----------------------------------------------------------------
    this.renderer = new WebGLDrawingAreaRenderer(
      this.model,
      this.htmlFeedback,
      {
        createLabelImage: (text: string, color: ColorRgb, fontSize: number): RGBAImageUncompressed =>
          WebSystem.calculateLabelImage(text, color, fontSize),
      },
      (viewport: Viewport): void => techniques.activateViewport(viewport),
      techniques.getTranslationGizmo(),
      techniques.getRotateGizmo(),
      techniques.getScaleGizmo(),
    );
    this.renderer.init();

    //-----------------------------------------------------------------
    this.htmlController = new HtmlDrawingAreaController(canvas, drawingArea, techniques, this.htmlFeedback);

    this.resizeObserver = new ResizeObserver((): void => this.reshape());
    this.resizeObserver.observe(canvas);
    canvas.addEventListener('webglcontextlost', (event: Event): void => event.preventDefault());
    canvas.addEventListener('webglcontextrestored', (): void => {
      this.renderer?.init();
      this.repaint();
    });
  }

  private ensureDrawingArea(application: HtmlWebGLSceneEditorApplication): void {
    if (this.canvas === null) {
      this.createDrawingArea(application);
    }
  }

  /**
   * @param application the application, needed to create the drawing area the
   * first time
   * @return the element presenting the drawing area
   */
  getCanvas(application: HtmlWebGLSceneEditorApplication): HTMLCanvasElement {
    this.ensureDrawingArea(application);
    return this.canvas!;
  }

  /**
   * @return true if the canvas of the drawing area was already created
   */
  isDrawingAreaCreated(): boolean {
    return this.canvas !== null;
  }

  requestFocusInWindow(): void {
    if (this.canvas !== null) {
      this.canvas.focus({ preventScroll: true });
    }
  }

  /**
   * Requests a new frame of the drawing area.
   */
  repaint(): void {
    if (this.canvas === null) {
      return;
    }
    if (this.frameInProgress) {
      this.repaintAfterFrame = true;
      return;
    }
    if (this.framePending) {
      return;
    }
    this.framePending = true;
    requestAnimationFrame((): void => {
      this.framePending = false;
      void this.drawFrame();
    });
  }

  /**
   * Java's `GLCanvas.display()`: draws a frame now (after the one in course,
   * if any).
   * @return resolves when the frame is drawn
   */
  display(): Promise<void> {
    return new Promise<void>((resolve): void => {
      this.displayWaiters.push(resolve);
      this.repaint();
    });
  }

  private async drawFrame(): Promise<void> {
    const gl: WebGL2RenderingContext | null = this.gl;
    const canvas: HTMLCanvasElement | null = this.canvas;
    if (gl === null || canvas === null || this.renderer === null || gl.isContextLost()) {
      return;
    }
    this.frameInProgress = true;
    const waiters: (() => void)[] = this.displayWaiters;
    this.displayWaiters = [];
    try {
      // JOGL calls reshape before the first display, and AWT does not paint a
      // component without size: until layout gives the canvas a size (i.e.
      // while the GUI is being rebuilt), there is nothing to draw
      if (this.reshaped && canvas.clientWidth > 0 && canvas.clientHeight > 0) {
        await this.renderer.display(gl, canvas.width, canvas.height);
      }
    } catch (error) {
      console.error(error);
    } finally {
      this.frameInProgress = false;
      for (const waiter of waiters) {
        waiter();
      }
      if (this.repaintAfterFrame) {
        this.repaintAfterFrame = false;
        this.repaint();
      }
    }
  }

  private reshape(): void {
    const canvas: HTMLCanvasElement | null = this.canvas;
    if (canvas === null || this.renderer === null) {
      return;
    }
    if (canvas.clientWidth <= 0 || canvas.clientHeight <= 0) {
      return;
    }
    const ratio: number = window.devicePixelRatio || 1;
    const width: number = Math.max(1, Math.round(canvas.clientWidth * ratio));
    const height: number = Math.max(1, Math.round(canvas.clientHeight * ratio));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
    }
    this.renderer.reshape(width, height);
    this.reshaped = true;
    this.repaint();
  }

  /**
   * Notifies the modify panel of the currently selected target.
   */
  reportTargetToModifyPanel(): void {
    if (this.htmlFeedback !== null) {
      this.htmlFeedback.reportTargetToModifyPanel();
    }
  }

  /**
   * Exports the selected viewport to a PNG file, in the next frame.
   * @param file destination file name
   */
  async exportViewportPng(file: string): Promise<void> {
    this.model.getDrawingArea().requestViewportExport(file, false);
    await this.display();
  }

  /**
   * Exports the selected viewport to a JPG file, in the next frame.
   * @param file destination file name
   */
  async exportViewportJpg(file: string): Promise<void> {
    this.model.getDrawingArea().requestViewportExport(file, true);
    await this.display();
  }

  /**
   * Exports the whole viewport set area to a JPG file, in the next frame.
   * @param file destination file name
   */
  async exportWorkspaceJpg(file: string): Promise<void> {
    this.model.getDrawingArea().requestWorkspaceExport(file);
    await this.display();
  }

  /**
   * Delivers a synthetic mouse event to the canvas (see
   * `HtmlDrawingAreaController.injectMouseEvent`).
   */
  injectMouseEvent(type: string, x: number, y: number, button: number): void {
    this.htmlController!.injectMouseEvent(type, x, y, button);
  }

  /**
   * Delivers a synthetic key press to the canvas, optionally with the CTRL
   * key down (see `HtmlDrawingAreaController.injectKeyEvent`).
   */
  injectKeyEvent(key: string, shift: boolean, ctrl: boolean = false): void {
    this.htmlController!.injectKeyEvent(key, shift, ctrl);
  }

  /**
   * Projects a point of the scene to canvas pixel coordinates.
   * @param viewport viewport whose camera is used
   * @param point point in world coordinates
   * @return {x, y} in canvas pixels, or null if the point is behind the camera
   */
  projectToCanvas(viewport: Viewport, point: Vector3Dd): number[] | null {
    return this.htmlController!.projectToCanvas(viewport, point);
  }

  /**
   * Releases the canvas, its WebGL resources and its listeners (the page of
   * the application is closed).
   */
  dispose(): void {
    this.resizeObserver?.disconnect();
    this.resizeObserver = null;
    this.htmlController?.dispose();
    if (this.gl !== null && this.renderer !== null) {
      this.renderer.dispose(this.gl);
    }
    this.canvas?.remove();
    this.canvas = null;
    this.gl = null;
    this.renderer = null;
  }
}

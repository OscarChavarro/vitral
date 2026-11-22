import type { HtmlDrawingAreaRenderer } from './html-drawing-area-renderer';

/**
 * Port of `render.awt.AwtViewportSetCanvas`.
 *
 * HTML canvas presenting a viewport set drawn by an `HtmlDrawingAreaRenderer`:
 * the canvas 2D counterpart of the WebGL canvas. Only a 2D context is asked to
 * the canvas, never a WebGL one (a canvas element can only give one kind of
 * context in its life). Each `Viewport` of the set is an area of this canvas
 * (see `HtmlViewportSetRenderer`), so the interaction over the set works
 * exactly as over the WebGL canvas.
 *
 * Frames are composed off-screen by the renderer and copied at once, so the
 * canvas is never cleared before painting (no flicker).
 */
export class HtmlViewportSetCanvas {
  private readonly canvas: HTMLCanvasElement;
  private readonly context: CanvasRenderingContext2D;
  private renderer: HtmlDrawingAreaRenderer | null;

  /**
   * @param canvas the element to draw on, whose drawing buffer has its size
   * in physical pixels; no context must have been asked to it yet
   */
  constructor(canvas: HTMLCanvasElement) {
    const context: CanvasRenderingContext2D | null = canvas.getContext('2d');
    if (context === null) {
      throw new Error('The scene editor requires canvas 2D support.');
    }
    this.canvas = canvas;
    this.context = context;
    this.renderer = null;
  }

  /**
   * @return the element presenting the viewport set
   */
  getCanvas(): HTMLCanvasElement {
    return this.canvas;
  }

  /**
   * @param renderer draws the frames; until it is given, nothing is drawn
   */
  setRenderer(renderer: HtmlDrawingAreaRenderer | null): void {
    this.renderer = renderer;
  }

  /**
   * Java's `paint`: draws a frame (as `display` of the WebGL canvas), so the
   * requests of the frame (i.e. exports to files) are done when it resolves.
   */
  async display(): Promise<void> {
    if (this.renderer === null) {
      return;
    }
    await this.renderer.display(
      this.context,
      this.canvas.clientWidth,
      this.canvas.clientHeight,
      this.getPixelScale(),
    );
  }

  /**
   * @return physical pixels for each logical pixel of the canvas (i.e. 2 on
   * high density displays), the one the drawing buffer of the canvas is
   * sized with (see `HtmlWebGLApplicationController`)
   */
  private getPixelScale(): number {
    if (typeof window === 'undefined') {
      return 1.0;
    }
    return window.devicePixelRatio || 1.0;
  }
}

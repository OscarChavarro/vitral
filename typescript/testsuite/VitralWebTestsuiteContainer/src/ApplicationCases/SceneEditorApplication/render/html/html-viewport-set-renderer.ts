import type { Viewport, ViewportElementScaler, ViewportSet } from '@vitral/base';
import { HtmlProjectedLineLayerRenderer } from './html-projected-line-layer-renderer';

/**
 * Draws the scene inside one viewport (Java's
 * `AwtViewportSetRenderer.ViewRenderer`). Drawing is asynchronous: a
 * raytraced viewport waits for the Web Workers of the raytracer.
 */
export interface HtmlViewRenderer {
  /**
   * Called before drawing the selected viewport (or the only one shown).
   * @param viewport
   */
  configureView(viewport: Viewport): void;

  /**
   * Draws a viewport. The context is clipped to its area.
   * @param g 2D context of the whole set
   * @param viewport viewport to draw
   * @param x left pixel of the viewport area, in canvas coordinates
   * @param y top pixel of the viewport area, in canvas coordinates
   * @param width width of the viewport area, in pixels
   * @param height height of the viewport area, in pixels
   */
  drawView(
    g: CanvasRenderingContext2D,
    viewport: Viewport,
    x: number,
    y: number,
    width: number,
    height: number,
  ): Promise<void>;
}

/**
 * Port of `render.awt.AwtViewportSetRenderer`.
 *
 * Canvas 2D presentation of a `ViewportSet`, the 2D counterpart of
 * `WebGLViewportSetRenderer`: it draws the line bordering each viewport
 * (highlighting the selected one), restricts the drawing to the area of each
 * viewport (with a clip region, as `gl.viewport` + scissor do in WebGL), draws
 * the title of each viewport and delegates the drawing of the scene inside each
 * viewport to a `HtmlViewRenderer`.
 *
 * Viewports measure their pixel areas with the origin at the lower left corner
 * of the set (as OpenGL does), while a canvas has it at the upper left corner:
 * this class converts between both, so view renderers receive canvas
 * rectangles.
 */
export class HtmlViewportSetRenderer {
  private static readonly BASE_TITLE_FONT_SIZE = 14;
  private static readonly BASE_TITLE_BORDER_X = 4;
  private static readonly BASE_TITLE_BORDER_Y = 1;
  private static readonly SELECTED_BORDER_COLOR = 'rgb(255,245,0)';
  private static readonly BORDER_COLOR = 'rgb(54,64,74)';

  private readonly viewportSet: ViewportSet;
  private readonly viewRenderer: HtmlViewRenderer;

  /**
   * @param viewportSet set to present
   * @param viewRenderer draws the scene inside each viewport
   */
  constructor(viewportSet: ViewportSet, viewRenderer: HtmlViewRenderer) {
    this.viewportSet = viewportSet;
    this.viewRenderer = viewRenderer;
  }

  /**
   * Draws the whole set. PRE: the set has the size of the area of `g`.
   * @param g 2D context of the area of the set
   * @param fullScreenGuiMode true if the GUI is in full screen mode
   */
  async draw(g: CanvasRenderingContext2D, fullScreenGuiMode: boolean): Promise<void> {
    if (this.viewportSet.countActiveViewports() === 1 && fullScreenGuiMode) {
      await this.drawSelectedViewFullScreen(g);
    } else {
      await this.drawMultipleViews(g);
    }
  }

  private async drawMultipleViews(g: CanvasRenderingContext2D): Promise<void> {
    for (const viewport of this.viewportSet.getViewports()) {
      this.drawBorder(g, viewport);
    }

    for (const viewport of this.viewportSet.getViewports()) {
      if (!viewport.isActive()) {
        continue;
      }
      viewport.updatePixelArea(
        this.viewportSet.getSizeXInPixels(),
        this.viewportSet.getSizeYInPixels(),
      );
      const x: number = viewport.getPixelStartX();
      const y: number = this.toCanvasY(viewport.getPixelStartY() + viewport.getPixelSizeY());
      if (this.viewportSet.isSelected(viewport)) {
        this.viewRenderer.configureView(viewport);
      }
      await this.drawClippedView(
        g,
        viewport,
        x,
        y,
        viewport.getPixelSizeX(),
        viewport.getPixelSizeY(),
      );
    }
  }

  private async drawSelectedViewFullScreen(g: CanvasRenderingContext2D): Promise<void> {
    for (const viewport of this.viewportSet.getViewports()) {
      if (!viewport.isActive() || !this.viewportSet.isSelected(viewport)) {
        continue;
      }
      viewport.updatePixelArea(
        this.viewportSet.getSizeXInPixels(),
        this.viewportSet.getSizeYInPixels(),
      );
      this.viewRenderer.configureView(viewport);
      await this.drawClippedView(
        g,
        viewport,
        0,
        0,
        this.viewportSet.getSizeXInPixels(),
        this.viewportSet.getSizeYInPixels(),
      );
    }
  }

  private async drawClippedView(
    g: CanvasRenderingContext2D,
    viewport: Viewport,
    x: number,
    y: number,
    width: number,
    height: number,
  ): Promise<void> {
    if (width <= 0 || height <= 0) {
      return;
    }
    g.save();
    g.beginPath();
    g.rect(x, y, width, height);
    g.clip();
    try {
      await this.viewRenderer.drawView(g, viewport, x, y, width, height);
      this.drawTitle(g, viewport, x, y);
    } finally {
      g.restore();
    }
  }

  /**
   * Draws the area of a viewport (border included) in the border color: the
   * viewport drawing covers it except for a frame of the border width.
   */
  private drawBorder(g: CanvasRenderingContext2D, viewport: Viewport): void {
    if (!viewport.isActive() || viewport.getBorder() <= 0) {
      return;
    }
    const setWidth: number = this.viewportSet.getSizeXInPixels();
    const setHeight: number = this.viewportSet.getSizeYInPixels();
    const x1: number = Math.round(viewport.getStartXPercent() * setWidth);
    const x2: number = Math.round(
      (viewport.getStartXPercent() + viewport.getSizeXPercent()) * setWidth,
    );
    const y1: number = this.toCanvasY(
      Math.round((viewport.getStartYPercent() + viewport.getSizeYPercent()) * setHeight),
    );
    const y2: number = this.toCanvasY(Math.round(viewport.getStartYPercent() * setHeight));

    g.fillStyle = this.viewportSet.isSelected(viewport)
      ? HtmlViewportSetRenderer.SELECTED_BORDER_COLOR
      : HtmlViewportSetRenderer.BORDER_COLOR;
    g.fillRect(x1, y1, x2 - x1, y2 - y1);
  }

  /**
   * Draws the title of a viewport at its upper left corner, informing the
   * viewport of the area it occupies (for interaction).
   */
  private drawTitle(g: CanvasRenderingContext2D, viewport: Viewport, x: number, y: number): void {
    const elementScaler: ViewportElementScaler = this.viewportSet.getElementScaler();
    const borderX: number = elementScaler.scaleSize(HtmlViewportSetRenderer.BASE_TITLE_BORDER_X);
    const borderY: number = elementScaler.scaleSize(HtmlViewportSetRenderer.BASE_TITLE_BORDER_Y);
    const fontSize: number = elementScaler.scaleSize(HtmlViewportSetRenderer.BASE_TITLE_FONT_SIZE);
    const title: string = this.viewportSet.getTitleFor(viewport);

    g.font = fontSize + 'px sans-serif';
    const metrics: TextMetrics = g.measureText(title);
    const ascent: number = Math.ceil(metrics.fontBoundingBoxAscent);
    const textWidth: number = Math.ceil(metrics.width);
    const textHeight: number = ascent + Math.ceil(metrics.fontBoundingBoxDescent);

    viewport.setTitleArea(0, 0, textWidth + 2 * borderX, textHeight + 2 * borderY);
    g.fillStyle = HtmlProjectedLineLayerRenderer.toCssColor(
      this.viewportSet.getTitleColorFor(viewport),
    );
    g.textBaseline = 'alphabetic';
    g.fillText(title, x + borderX, y + borderY + ascent);
  }

  /**
   * @param y vertical pixel coordinate with origin at the bottom of the set
   * @return the same coordinate with origin at the top of the set
   */
  private toCanvasY(y: number): number {
    return this.viewportSet.getSizeYInPixels() - y;
  }
}

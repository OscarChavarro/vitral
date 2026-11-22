import type { Calligraphic2DBuffer, ColorRgb, Vector3Dd } from '@vitral/base';
import { ProjectedLineLayerStyle, type ProjectedLineLayer } from '../projected-line-layer';

/**
 * Port of `render.awt.AwtProjectedLineLayerRenderer`.
 *
 * Draws `ProjectedLineLayer`s with the 2D operations of an HTML canvas
 * (`CanvasRenderingContext2D`, where Java uses AWT's `Graphics2D`): the last
 * stage of the calligraphic pipeline, the mapping from normalized device
 * coordinates to the pixels of a viewport [FOLE1992].6, as
 * `HtmlCalligraphic2DBufferRenderer` does, but honoring the color, width and
 * style of each layer and with sub-pixel (antialiased) precision.
 */
export class HtmlProjectedLineLayerRenderer {
  /// Distance, in pixels, under which a segment continues the previous one
  private static readonly CHAIN_TOLERANCE = 0.01;

  private constructor() {}

  /**
   * @param g 2D context where to draw
   * @param layers layers to draw, in order
   * @param x0 left pixel of the viewport
   * @param y0 top pixel of the viewport
   * @param width width of the viewport, in pixels
   * @param height height of the viewport, in pixels
   * @param widthScale factor applied to the widths of the layers, given for a
   * standard resolution screen (see `ViewportElementScaler`)
   */
  static draw(
    g: CanvasRenderingContext2D,
    layers: readonly ProjectedLineLayer[],
    x0: number,
    y0: number,
    width: number,
    height: number,
    widthScale: number,
  ): void {
    g.save();
    for (const layer of layers) {
      const lineWidth: number = Math.max(1.0, layer.getWidth() * widthScale);
      const color: string = HtmlProjectedLineLayerRenderer.toCssColor(layer.getColor());
      g.strokeStyle = color;
      g.fillStyle = color;
      switch (layer.getStyle()) {
        case ProjectedLineLayerStyle.DASHED:
          // Java's BasicStroke(CAP_BUTT, JOIN_MITER, miter limit 10, dashes)
          g.lineWidth = lineWidth;
          g.lineCap = 'butt';
          g.lineJoin = 'miter';
          g.miterLimit = 10.0;
          g.setLineDash([4.0 * lineWidth, 3.0 * lineWidth]);
          g.lineDashOffset = 0.0;
          HtmlProjectedLineLayerRenderer.drawLines(g, layer.getLines(), x0, y0, width, height);
          break;
        case ProjectedLineLayerStyle.POINTS:
          HtmlProjectedLineLayerRenderer.drawPoints(
            g,
            layer.getLines(),
            x0,
            y0,
            width,
            height,
            lineWidth,
          );
          break;
        case ProjectedLineLayerStyle.SOLID:
        default:
          g.lineWidth = lineWidth;
          g.lineCap = 'round';
          g.lineJoin = 'round';
          g.setLineDash([]);
          HtmlProjectedLineLayerRenderer.drawLines(g, layer.getLines(), x0, y0, width, height);
          break;
      }
    }
    g.restore();
  }

  /**
   * Consecutive segments where one starts at the end of the previous one (i.e.
   * the polyline approximating a curve, or a line broken by the hidden line
   * algorithm) are drawn as one path, so the pattern of a dashed stroke
   * continues along it instead of starting again at each (maybe very short)
   * segment, which would draw it as a continuous line.
   */
  private static drawLines(
    g: CanvasRenderingContext2D,
    lines: Calligraphic2DBuffer,
    x0: number,
    y0: number,
    width: number,
    height: number,
  ): void {
    let lastX: number = Number.NaN;
    let lastY: number = Number.NaN;
    let i: number;

    g.beginPath();
    for (i = 0; i < lines.getNumLines(); i++) {
      const segment: Vector3Dd[] = lines.get2DLine(i);
      const ax: number = HtmlProjectedLineLayerRenderer.toPixelX(segment[0]!.x(), x0, width);
      const ay: number = HtmlProjectedLineLayerRenderer.toPixelY(segment[0]!.y(), y0, height);
      const bx: number = HtmlProjectedLineLayerRenderer.toPixelX(segment[1]!.x(), x0, width);
      const by: number = HtmlProjectedLineLayerRenderer.toPixelY(segment[1]!.y(), y0, height);

      if (
        Math.abs(ax - lastX) > HtmlProjectedLineLayerRenderer.CHAIN_TOLERANCE ||
        Math.abs(ay - lastY) > HtmlProjectedLineLayerRenderer.CHAIN_TOLERANCE ||
        Number.isNaN(lastX)
      ) {
        g.moveTo(ax, ay);
      }
      g.lineTo(bx, by);
      lastX = bx;
      lastY = by;
    }
    g.stroke();
  }

  private static drawPoints(
    g: CanvasRenderingContext2D,
    lines: Calligraphic2DBuffer,
    x0: number,
    y0: number,
    width: number,
    height: number,
    size: number,
  ): void {
    let i: number;

    for (i = 0; i < lines.getNumLines(); i++) {
      const segment: Vector3Dd[] = lines.get2DLine(i);
      g.fillRect(
        HtmlProjectedLineLayerRenderer.toPixelX(segment[0]!.x(), x0, width) - size / 2.0,
        HtmlProjectedLineLayerRenderer.toPixelY(segment[0]!.y(), y0, height) - size / 2.0,
        size,
        size,
      );
    }
  }

  /**
   * @param ndcX horizontal normalized device coordinate (-1 is the left side)
   */
  static toPixelX(ndcX: number, x0: number, width: number): number {
    return x0 + ((ndcX + 1.0) / 2.0) * width;
  }

  /**
   * @param ndcY vertical normalized device coordinate (-1 is the bottom side,
   * while canvas pixels grow downwards)
   */
  static toPixelY(ndcY: number, y0: number, height: number): number {
    return y0 + ((1.0 - ndcY) / 2.0) * height;
  }

  /**
   * Java's `toAwtColor`.
   * @param color a vitral color, with components between 0 and 1
   * @return the same color, as a CSS color for the canvas
   */
  static toCssColor(color: ColorRgb): string {
    const clamp = HtmlProjectedLineLayerRenderer.clamp;
    return (
      'rgb(' +
      Math.round(clamp(color.r()) * 255) +
      ',' +
      Math.round(clamp(color.g()) * 255) +
      ',' +
      Math.round(clamp(color.b()) * 255) +
      ')'
    );
  }

  private static clamp(value: number): number {
    return Math.max(0.0, Math.min(1.0, value));
  }
}

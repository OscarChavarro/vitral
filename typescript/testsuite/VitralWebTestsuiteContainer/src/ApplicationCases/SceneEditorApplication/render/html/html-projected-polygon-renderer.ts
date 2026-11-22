import type { ProjectedPolygon } from '../projected-polygon';
import { HtmlProjectedLineLayerRenderer } from './html-projected-line-layer-renderer';

/**
 * Port of `render.awt.AwtProjectedPolygonRenderer`.
 *
 * Fills `ProjectedPolygon`s with the 2D operations of an HTML canvas
 * (`CanvasRenderingContext2D`): the mapping from normalized device
 * coordinates to the pixels of a viewport [FOLE1992].6, as
 * `HtmlProjectedLineLayerRenderer` does for lines. Java fills the polygons
 * without antialiasing, so polygons sharing an edge cover its pixels once; a
 * canvas 2D context always antialiases its fills, so each polygon is also
 * outlined with a hairline of its own color, which covers the seams
 * blending each one with the background would leave.
 */
export class HtmlProjectedPolygonRenderer {
  private constructor() {}

  /**
   * @param g 2D context where to draw
   * @param polygons polygons to fill, in order
   * @param x0 left pixel of the viewport
   * @param y0 top pixel of the viewport
   * @param width width of the viewport, in pixels
   * @param height height of the viewport, in pixels
   */
  static draw(
    g: CanvasRenderingContext2D,
    polygons: readonly ProjectedPolygon[],
    x0: number,
    y0: number,
    width: number,
    height: number,
  ): void {
    g.save();
    g.lineWidth = 1.0;
    g.lineJoin = 'round';
    g.setLineDash([]);
    for (const polygon of polygons) {
      g.beginPath();
      for (const loop of polygon.getArea().loops) {
        let first = true;
        for (const v of loop.vertices) {
          const x: number = HtmlProjectedLineLayerRenderer.toPixelX(v.x, x0, width);
          const y: number = HtmlProjectedLineLayerRenderer.toPixelY(v.y, y0, height);
          if (first) {
            g.moveTo(x, y);
            first = false;
          } else {
            g.lineTo(x, y);
          }
        }
        if (!first) {
          g.closePath();
        }
      }
      const color: string = HtmlProjectedLineLayerRenderer.toCssColor(polygon.getColor());
      g.fillStyle = color;
      g.strokeStyle = color;
      g.fill('evenodd');
      g.stroke();
    }
    g.restore();
  }
}

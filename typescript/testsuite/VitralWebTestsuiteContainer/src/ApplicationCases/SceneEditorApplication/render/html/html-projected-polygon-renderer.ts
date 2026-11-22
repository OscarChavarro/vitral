import type { ColorRgb } from '@vitral/base';
import type { ProjectedPolygon } from '../projected-polygon';
import { HtmlProjectedLineLayerRenderer } from './html-projected-line-layer-renderer';

/**
 * Port of `render.awt.AwtProjectedPolygonRenderer`.
 *
 * Draws `ProjectedPolygon`s with the 2D operations of an HTML canvas
 * (`CanvasRenderingContext2D`): the mapping from normalized device
 * coordinates to the pixels of a viewport [FOLE1992].6, as
 * `HtmlProjectedLineLayerRenderer` does for lines. The polygons are filled
 * (`draw`), or only their contours are stroked (`drawContours`). Java fills
 * the polygons without antialiasing, so polygons sharing an edge cover its
 * pixels once; a canvas 2D context always antialiases its fills, so each
 * filled polygon is also outlined with a hairline of its own color, which
 * covers the seams blending each one with the background would leave.
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
      const path: Path2D = HtmlProjectedPolygonRenderer.toPath(polygon, x0, y0, width, height);
      const color: string = HtmlProjectedLineLayerRenderer.toCssColor(polygon.getColor());
      g.fillStyle = color;
      g.strokeStyle = color;
      g.fill(path, 'evenodd');
      g.stroke(path);
    }
    g.restore();
  }

  /**
   * Strokes the contours (outer and holes) of the polygons, with
   * antialiasing, as lines of the given width.
   * @param g 2D context where to draw
   * @param polygons polygons whose contours to draw, in order
   * @param x0 left pixel of the viewport
   * @param y0 top pixel of the viewport
   * @param width width of the viewport, in pixels
   * @param height height of the viewport, in pixels
   * @param lineWidth width of the lines, in pixels
   * @param lineColor color of every contour, or null to draw each one with
   * the color of its polygon
   */
  static drawContours(
    g: CanvasRenderingContext2D,
    polygons: readonly ProjectedPolygon[],
    x0: number,
    y0: number,
    width: number,
    height: number,
    lineWidth: number,
    lineColor: ColorRgb | null,
  ): void {
    // Java's `getStroke` / `setStroke(previousStroke)`
    g.save();

    // Java's BasicStroke(lineWidth, CAP_ROUND, JOIN_ROUND)
    g.lineWidth = lineWidth;
    g.lineCap = 'round';
    g.lineJoin = 'round';
    g.setLineDash([]);
    if (lineColor !== null) {
      g.strokeStyle = HtmlProjectedLineLayerRenderer.toCssColor(lineColor);
    }
    for (const polygon of polygons) {
      if (lineColor === null) {
        g.strokeStyle = HtmlProjectedLineLayerRenderer.toCssColor(polygon.getColor());
      }
      g.stroke(HtmlProjectedPolygonRenderer.toPath(polygon, x0, y0, width, height));
    }
    g.restore();
  }

  /**
   * @return the contours of a polygon, in pixels of the viewport
   */
  private static toPath(
    polygon: ProjectedPolygon,
    x0: number,
    y0: number,
    width: number,
    height: number,
  ): Path2D {
    const path: Path2D = new Path2D();

    for (const loop of polygon.getArea().loops) {
      let first = true;
      for (const v of loop.vertices) {
        const x: number = HtmlProjectedLineLayerRenderer.toPixelX(v.x, x0, width);
        const y: number = HtmlProjectedLineLayerRenderer.toPixelY(v.y, y0, height);
        if (first) {
          path.moveTo(x, y);
          first = false;
        } else {
          path.lineTo(x, y);
        }
      }
      if (!first) {
        path.closePath();
      }
    }
    return path;
  }
}

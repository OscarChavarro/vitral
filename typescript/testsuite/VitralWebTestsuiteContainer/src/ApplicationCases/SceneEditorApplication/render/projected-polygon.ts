import { ColorRgb, type Polygon2D } from '@vitral/base';

/**
 * Port of `render.ProjectedPolygon`.
 *
 * A filled 2D polygon (maybe with holes, and with several main contours),
 * already projected to the normalized device coordinates of a viewport (from
 * <-1, -1> at its lower left corner to <1, 1> at its upper right corner). It
 * is the technology independent output of `ProjectedSurfacesViewBuilder`, as
 * `ProjectedLineLayer` is for lines: any 2D drawing technology can present it
 * by mapping those coordinates to its own pixels. Its contours are filled with
 * the even-odd rule.
 */
export class ProjectedPolygon {
  private readonly area: Polygon2D;
  private readonly color: ColorRgb;

  /**
   * @param area contours of the polygon, in normalized device coordinates
   * @param color fill color
   */
  constructor(area: Polygon2D, color: ColorRgb) {
    this.area = area;
    this.color = new ColorRgb(color);
  }

  getArea(): Polygon2D {
    return this.area;
  }

  getColor(): ColorRgb {
    return this.color;
  }
}

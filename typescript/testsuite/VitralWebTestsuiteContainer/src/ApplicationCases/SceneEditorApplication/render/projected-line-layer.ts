import { Calligraphic2DBuffer, ColorRgb } from '@vitral/base';

/**
 * How the elements of a layer are drawn (Java's enum `ProjectedLineLayer.Style`).
 */
export enum ProjectedLineLayerStyle {
  /** Continuous lines */
  SOLID,
  /** Dashed lines (i.e. hidden lines) */
  DASHED,
  /** Points: each element is a degenerate line, drawn as a small mark */
  POINTS,
}

/**
 * Port of `render.ProjectedLineLayer`.
 *
 * A set of 2D lines (or points) sharing one drawing style, already projected
 * to the normalized device coordinates of a viewport (from <-1, -1> at its
 * lower left corner to <1, 1> at its upper right corner). It is the technology
 * independent output of `ProjectedLinesViewBuilder`: any 2D drawing technology
 * (a canvas 2D context, a raster image, a plotter) can present it by mapping
 * those coordinates to its own pixels, as `Calligraphic2DBuffer` suggests.
 */
export class ProjectedLineLayer {
  private readonly lines: Calligraphic2DBuffer;
  private readonly color: ColorRgb;
  private readonly width: number;
  private readonly style: ProjectedLineLayerStyle;

  /**
   * @param color color of every element of the layer
   * @param width line width (or point size), in pixels of a standard
   * resolution screen (see `ViewportElementScaler`)
   * @param style how the elements are drawn
   */
  constructor(color: ColorRgb, width: number, style: ProjectedLineLayerStyle) {
    this.lines = new Calligraphic2DBuffer();
    this.color = new ColorRgb(color);
    this.width = width;
    this.style = style;
  }

  /**
   * @return the lines of the layer, in normalized device coordinates
   */
  getLines(): Calligraphic2DBuffer {
    return this.lines;
  }

  getColor(): ColorRgb {
    return this.color;
  }

  getWidth(): number {
    return this.width;
  }

  getStyle(): ProjectedLineLayerStyle {
    return this.style;
  }

  /**
   * @return true if the layer has nothing to draw
   */
  isEmpty(): boolean {
    return this.lines.getNumLines() === 0;
  }
}

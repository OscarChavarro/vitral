import { Calligraphic2DBuffer, Camera, HiddenLineRenderer, type ArrayList, type SimpleBody } from '@vitral/base';

/**
 * CPU/Appel hidden-line presentation for an HTML5 2D canvas.  It deliberately
 * has no WebGL dependency: Appel produces normalized 2D segments and this
 * adapter maps them to CanvasRenderingContext2D pixels.
 */
export class HtmlCanvasHiddenLineRenderer {
  render(
    context: CanvasRenderingContext2D,
    bodies: ArrayList<SimpleBody> | readonly SimpleBody[],
    camera: Camera,
    withHiddenLines: boolean,
  ): void {
    const contours = new Calligraphic2DBuffer();
    const visible = new Calligraphic2DBuffer();
    const hidden = new Calligraphic2DBuffer();
    HiddenLineRenderer.executeAppelAlgorithm(bodies, camera, contours, visible, hidden);
    this.draw(context, hidden, '#525252', 1, [5, 4], withHiddenLines);
    this.draw(context, visible, '#000000', 1, [], true);
    this.draw(context, contours, '#000000', 2, [], true);
  }

  private draw(
    context: CanvasRenderingContext2D,
    lines: Calligraphic2DBuffer,
    color: string,
    width: number,
    dash: number[],
    enabled: boolean,
  ): void {
    if (!enabled) return;
    context.save();
    context.strokeStyle = color;
    context.lineWidth = width;
    context.setLineDash(dash);
    context.beginPath();
    for (let i = 0; i < lines.getNumLines(); i++) {
      const [a, b] = lines.get2DLine(i);
      context.moveTo((a.x() + 1) * context.canvas.width / 2, (1 - a.y()) * context.canvas.height / 2);
      context.lineTo((b.x() + 1) * context.canvas.width / 2, (1 - b.y()) * context.canvas.height / 2);
    }
    context.stroke();
    context.restore();
  }
}

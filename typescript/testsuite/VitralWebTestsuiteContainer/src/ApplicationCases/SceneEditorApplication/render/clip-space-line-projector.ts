import {
  VSDK,
  Vector4Dd,
  type Calligraphic2DBuffer,
  type Camera,
  type Matrix4x4d,
  type Vector3Dd,
} from '@vitral/base';

/**
 * Port of `render.ClipSpaceLineProjector`.
 *
 * Projects 3D segments to the 2D normalized device coordinates of a camera, as
 * the geometric stage of a graphics pipeline does before rasterization
 * [FOLE1992].6: points are transformed by the projection matrix of the camera
 * (view and view volume), segments are clipped in homogeneous coordinates
 * against the six planes `-w <= x, y, z <= w` and the perspective division gives
 * the 2D coordinates.
 *
 * Clipping in homogeneous space (instead of the canonical perspective volume of
 * `WireframeRenderer`) works the same for perspective and orthogonal cameras,
 * and is the same scheme used by `HiddenLineRenderer` to project its lines, so
 * wireframes and hidden line drawings of a view match.
 */
export class ClipSpaceLineProjector {
  private static readonly CLIP_PLANES: readonly (readonly number[])[] = [
    [1.0, 0.0, 0.0, 1.0],
    [-1.0, 0.0, 0.0, 1.0],
    [0.0, 1.0, 0.0, 1.0],
    [0.0, -1.0, 0.0, 1.0],
    [0.0, 0.0, 1.0, 1.0],
    [0.0, 0.0, -1.0, 1.0],
  ];

  private readonly projection: Matrix4x4d;

  /**
   * @param camera camera whose view is projected; its viewport size must be
   * already updated (it gives the aspect ratio)
   */
  constructor(camera: Camera) {
    this.projection = camera.calculateProjectionMatrix();
  }

  /**
   * Java overloads `addSegment(out, p0, p1)` (a segment given in world
   * coordinates) and `addSegment(out, transform, p0, p1)` (a segment given in
   * the coordinates of an object, with the transformation from object to
   * world coordinates).
   * @param out buffer receiving the projected segment, if any part of it is
   * inside the view volume
   * @return true if a segment was added
   */
  addSegment(out: Calligraphic2DBuffer, p0: Vector3Dd, p1: Vector3Dd): boolean;
  addSegment(out: Calligraphic2DBuffer, transform: Matrix4x4d, p0: Vector3Dd, p1: Vector3Dd): boolean;
  addSegment(
    out: Calligraphic2DBuffer,
    a: Vector3Dd | Matrix4x4d,
    b: Vector3Dd,
    c?: Vector3Dd,
  ): boolean {
    if (c !== undefined) {
      const transform: Matrix4x4d = a as Matrix4x4d;
      return this.addSegment(out, transform.multiply(b), transform.multiply(c));
    }
    const p0: Vector3Dd = a as Vector3Dd;
    const p1: Vector3Dd = b;
    let start: Vector4Dd = this.projection.multiply(new Vector4Dd(p0));
    let end: Vector4Dd = this.projection.multiply(new Vector4Dd(p1));

    for (const plane of ClipSpaceLineProjector.CLIP_PLANES) {
      const d0: number = ClipSpaceLineProjector.evaluate(plane, start);
      const d1: number = ClipSpaceLineProjector.evaluate(plane, end);

      if (d0 < 0.0 && d1 < 0.0) {
        return false;
      }
      if (d0 < 0.0 || d1 < 0.0) {
        const denominator: number = d0 - d1;
        if (Math.abs(denominator) < VSDK.EPSILON) {
          return false;
        }
        const t: number = d0 / denominator;
        const intersection: Vector4Dd = start.multiply(1.0 - t).add(end.multiply(t));
        if (d0 < 0.0) {
          start = intersection;
        } else {
          end = intersection;
        }
      }
    }

    const ndc0: Vector4Dd = start.dividedByW();
    const ndc1: Vector4Dd = end.dividedByW();
    out.add2DLine(ndc0.x(), ndc0.y(), ndc1.x(), ndc1.y());
    return true;
  }

  /**
   * Projects a point, given in world coordinates, as a degenerate segment.
   * @param out buffer receiving the projected point, if it is inside the view
   * volume
   * @param p the point
   * @return true if the point was added
   */
  addPoint(out: Calligraphic2DBuffer, p: Vector3Dd): boolean {
    const clip: Vector4Dd = this.projection.multiply(new Vector4Dd(p));

    for (const plane of ClipSpaceLineProjector.CLIP_PLANES) {
      if (ClipSpaceLineProjector.evaluate(plane, clip) < 0.0) {
        return false;
      }
    }
    const ndc: Vector4Dd = clip.dividedByW();
    out.add2DLine(ndc.x(), ndc.y(), ndc.x(), ndc.y());
    return true;
  }

  private static evaluate(plane: readonly number[], point: Vector4Dd): number {
    return (
      plane[0]! * point.x() + plane[1]! * point.y() + plane[2]! * point.z() + plane[3]! * point.w()
    );
  }
}

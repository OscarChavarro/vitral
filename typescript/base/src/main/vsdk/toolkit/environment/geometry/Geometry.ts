import { Entity } from "../../common/Entity.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Containment } from "../../processing/Containment.js";

/** Structural intersection contracts implemented by Phase 19 ray elements. */
export interface GeometryRay {}
export interface GeometryRayHit {}

/** Root contract shared by all origin-centered Vitral geometries. */
export abstract class Geometry<TRay extends GeometryRay = GeometryRay, THit extends GeometryRayHit = GeometryRayHit> extends Entity {
  public static readonly INSIDE = Containment.INSIDE;
  public static readonly LIMIT = Containment.LIMIT;
  public static readonly OUTSIDE = Containment.OUTSIDE;

  /** Updates the supplied hit record when this geometry has a first ray hit. */
  public abstract doIntersectionFirstHit(inRay: TRay, outHit: THit): boolean;

  /**
   * Default geometries have no additional hit attributes. Concrete surfaces
   * override this to populate point, normal, UV, or tangent information.
   */
  public doExtraInformation(_inRay: TRay, _inT: number, _outHit: THit): void {}

  /** Default transparent-geometry visibility classification. */
  public computeQuantitativeInvisibility(_origin: Vector3Dd, _p: Vector3Dd): number { return 0; }

  /** Axis-aligned local bounds: min xyz followed by max xyz. */
  public abstract getMinMax(): Float64Array | number[];

  /** Default classification for non-solid geometries. */
  public doContainmentTest(_p: Vector3Dd, _distanceTolerance: number): number { return Geometry.OUTSIDE; }
}

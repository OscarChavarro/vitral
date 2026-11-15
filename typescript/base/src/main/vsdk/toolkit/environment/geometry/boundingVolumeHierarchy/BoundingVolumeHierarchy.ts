import type { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { AxisAlignedBoundingBox } from "./AxisAlignedBoundingBox.js";
import { PreRayHitElement } from "./PreRayHitElement.js";

/**
Bounding volume of a geometry or of a group of geometries. All queries are
conservative: false positives are allowed, false negatives are not.
*/
export abstract class BoundingVolumeHierarchy extends PreRayHitElement {
    public abstract containsPoint(point: Vector3Dd, tolerance: number): boolean;
    public abstract isUnbounded(): boolean;

    /** Conservative axis aligned extent of the whole volume. */
    public abstract axisAlignedExtent(): AxisAlignedBoundingBox;

    /** Caller owns the returned copy. */
    public abstract copy(): BoundingVolumeHierarchy;
}

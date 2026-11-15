import type { Ray } from "../element/Ray.js";
import type { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { AxisAlignedBoundingBox } from "./AxisAlignedBoundingBox.js";
import { BoundingVolumeHierarchy } from "./BoundingVolumeHierarchy.js";

/** Bounding volume made of one axis aligned box. */
export class AabbBoundingVolume extends BoundingVolumeHierarchy {
    private bounds: AxisAlignedBoundingBox;

    public constructor();
    public constructor(bounds: AxisAlignedBoundingBox);
    public constructor(min: Vector3Dd, max: Vector3Dd);
    public constructor(boundsOrMin?: AxisAlignedBoundingBox | Vector3Dd, max?: Vector3Dd) {
        super();
        if (boundsOrMin === undefined) this.bounds = AxisAlignedBoundingBox.unbounded();
        else if (boundsOrMin instanceof AxisAlignedBoundingBox) this.bounds = boundsOrMin;
        else this.bounds = new AxisAlignedBoundingBox(boundsOrMin, max!);
    }

    public getBounds(): AxisAlignedBoundingBox {
        return this.bounds;
    }

    public override mayIntersect(ray: Ray): boolean {
        return this.bounds.intersectsRayForward(ray);
    }

    public override mayIntersectBefore(ray: Ray, maxT: number): boolean {
        return this.bounds.intersectsRayBefore(ray, maxT);
    }

    public override containsPoint(point: Vector3Dd, tolerance: number): boolean {
        return this.bounds.containsPointWithTolerance(point, tolerance);
    }

    public override isUnbounded(): boolean {
        return this.bounds.isUnbounded();
    }

    public override axisAlignedExtent(): AxisAlignedBoundingBox {
        return this.bounds;
    }

    public override copy(): BoundingVolumeHierarchy {
        return new AabbBoundingVolume(this.bounds);
    }
}

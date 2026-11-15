import type { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";

/**
Reciprocals of a ray direction used by the slab test against axis aligned
boxes (Kay and Kajiya 1986). A component with magnitude under `1e-12` is
degenerate: the ray is parallel to that slab, its reciprocal is stored as zero
and the slab test checks the origin coordinate instead. It is computed once
per ray direction on the hottest path of a render.
*/
export class RaySlabReciprocals {
    private inverseX: number;
    private inverseY: number;
    private inverseZ: number;
    private degenerateX: boolean;
    private degenerateY: boolean;
    private degenerateZ: boolean;

    public constructor();
    public constructor(direction: Vector3Dd);
    public constructor(direction?: Vector3Dd) {
        if (direction === undefined) {
            this.inverseX = 0.0;
            this.inverseY = 0.0;
            this.inverseZ = 0.0;
            this.degenerateX = true;
            this.degenerateY = true;
            this.degenerateZ = true;
            return;
        }
        this.degenerateX = direction.x() > -1e-12 && direction.x() < 1e-12;
        this.degenerateY = direction.y() > -1e-12 && direction.y() < 1e-12;
        this.degenerateZ = direction.z() > -1e-12 && direction.z() < 1e-12;
        this.inverseX = this.degenerateX ? 0.0 : 1.0 / direction.x();
        this.inverseY = this.degenerateY ? 0.0 : 1.0 / direction.y();
        this.inverseZ = this.degenerateZ ? 0.0 : 1.0 / direction.z();
    }

    public getInverseX(): number {
        return this.inverseX;
    }
    public getInverseY(): number {
        return this.inverseY;
    }
    public getInverseZ(): number {
        return this.inverseZ;
    }
    public isDegenerateX(): boolean {
        return this.degenerateX;
    }
    public isDegenerateY(): boolean {
        return this.degenerateY;
    }
    public isDegenerateZ(): boolean {
        return this.degenerateZ;
    }
}

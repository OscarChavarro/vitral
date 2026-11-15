import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { Ray } from "../element/Ray.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { GeometryBounds } from "../element/GeometryBounds.js";
import { RaySlabReciprocals } from "./RaySlabReciprocals.js";

/**
Axis aligned box `[min, max]`. Coordinates beyond `1e29` stand for an
unbounded box. The ray tests are the slab method of Kay and Kajiya (1986);
they and the point test are the hottest filters of a render.
*/
export class AxisAlignedBoundingBox {
    private min: Vector3Dd;
    private max: Vector3Dd;

    public constructor();
    public constructor(min: Vector3Dd, max: Vector3Dd);
    public constructor(min?: Vector3Dd, max?: Vector3Dd) {
        this.min = min ?? new Vector3Dd(0.0, 0.0, 0.0);
        this.max = max ?? new Vector3Dd(0.0, 0.0, 0.0);
    }

    public getMin(): Vector3Dd {
        return this.min;
    }

    public getMax(): Vector3Dd {
        return this.max;
    }

    public static unbounded(): AxisAlignedBoundingBox {
        const INF = 1e30;
        return new AxisAlignedBoundingBox(new Vector3Dd(-INF, -INF, -INF), new Vector3Dd(INF, INF, INF));
    }

    public static empty(): AxisAlignedBoundingBox {
        const INF = 1e30;
        return new AxisAlignedBoundingBox(new Vector3Dd(INF, INF, INF), new Vector3Dd(-INF, -INF, -INF));
    }

    public isUnbounded(): boolean {
        const THRESHOLD = 1e29;
        const min = this.min,
            max = this.max;
        return (
            min.x() <= -THRESHOLD ||
            max.x() >= THRESHOLD ||
            min.y() <= -THRESHOLD ||
            max.y() >= THRESHOLD ||
            min.z() <= -THRESHOLD ||
            max.z() >= THRESHOLD
        );
    }

    public enclosing(other: AxisAlignedBoundingBox): AxisAlignedBoundingBox {
        const min = this.min,
            max = this.max;
        return new AxisAlignedBoundingBox(
            new Vector3Dd(
                min.x() < other.min.x() ? min.x() : other.min.x(),
                min.y() < other.min.y() ? min.y() : other.min.y(),
                min.z() < other.min.z() ? min.z() : other.min.z(),
            ),
            new Vector3Dd(
                max.x() > other.max.x() ? max.x() : other.max.x(),
                max.y() > other.max.y() ? max.y() : other.max.y(),
                max.z() > other.max.z() ? max.z() : other.max.z(),
            ),
        );
    }

    public intersection(other: AxisAlignedBoundingBox): AxisAlignedBoundingBox {
        const min = this.min,
            max = this.max;
        return new AxisAlignedBoundingBox(
            new Vector3Dd(
                min.x() > other.min.x() ? min.x() : other.min.x(),
                min.y() > other.min.y() ? min.y() : other.min.y(),
                min.z() > other.min.z() ? min.z() : other.min.z(),
            ),
            new Vector3Dd(
                max.x() < other.max.x() ? max.x() : other.max.x(),
                max.y() < other.max.y() ? max.y() : other.max.y(),
                max.z() < other.max.z() ? max.z() : other.max.z(),
            ),
        );
    }

    public expandedBy(point: Vector3Dd): AxisAlignedBoundingBox {
        const min = this.min,
            max = this.max;
        return new AxisAlignedBoundingBox(
            new Vector3Dd(
                min.x() < point.x() ? min.x() : point.x(),
                min.y() < point.y() ? min.y() : point.y(),
                min.z() < point.z() ? min.z() : point.z(),
            ),
            new Vector3Dd(
                max.x() > point.x() ? max.x() : point.x(),
                max.y() > point.y() ? max.y() : point.y(),
                max.z() > point.z() ? max.z() : point.z(),
            ),
        );
    }

    public centroid(): Vector3Dd {
        const min = this.min,
            max = this.max;
        return new Vector3Dd((min.x() + max.x()) * 0.5, (min.y() + max.y()) * 0.5, (min.z() + max.z()) * 0.5);
    }

    public extent(axis: number): number {
        if (axis === 0) return this.max.x() - this.min.x();
        if (axis === 1) return this.max.y() - this.min.y();
        return this.max.z() - this.min.z();
    }

    /** World box around the eight corners of `[lo, hi]` transformed by
    `matrix` (column vectors, as `GeometryBounds.transformed`); without a
    matrix, the box `[lo, hi]` itself. */
    public static fromTransformedCorners(
        lo: Vector3Dd,
        hi: Vector3Dd,
        matrix: Matrix4x4d | null,
    ): AxisAlignedBoundingBox {
        const local = new GeometryBounds();
        if (!local.setFinite(lo, hi)) return AxisAlignedBoundingBox.unbounded();
        const result = matrix ? local.transformed(matrix) : local;
        if (result.getKind() !== GeometryBounds.Kind.Finite) return AxisAlignedBoundingBox.unbounded();
        return new AxisAlignedBoundingBox(result.getMinimum(), result.getMaximum());
    }

    /** Box of finite geometry bounds; any other kind (empty, unbounded,
    indeterminate, unsupported) gives the unbounded box, which is always
    conservative. */
    public static fromGeometryBounds(bounds: GeometryBounds): AxisAlignedBoundingBox {
        if (bounds.getKind() !== GeometryBounds.Kind.Finite) return AxisAlignedBoundingBox.unbounded();
        return new AxisAlignedBoundingBox(bounds.getMinimum(), bounds.getMaximum());
    }

    /** Slab test for ray parameters in [0, +inf); without `reciprocals` they
    are computed from the ray direction. */
    public intersectsRayForward(ray: Ray, reciprocals?: RaySlabReciprocals): boolean {
        if (reciprocals === undefined)
            return this.intersectsRayForward(ray, new RaySlabReciprocals(ray.getDirection()));
        const origin = ray.getOrigin();
        let tMin = 0.0;
        let tMax = 1e30;

        const updateAxis = (
            originCoord: number,
            invDir: number,
            degenerate: boolean,
            minCoord: number,
            maxCoord: number,
        ): boolean => {
            if (degenerate) {
                return originCoord >= minCoord && originCoord <= maxCoord;
            }
            let nearT = (minCoord - originCoord) * invDir;
            let farT = (maxCoord - originCoord) * invDir;
            if (nearT > farT) {
                const tmp = nearT;
                nearT = farT;
                farT = tmp;
            }
            tMin = nearT > tMin ? nearT : tMin;
            tMax = farT < tMax ? farT : tMax;
            return tMin <= tMax;
        };

        return (
            updateAxis(
                origin.x(),
                reciprocals.getInverseX(),
                reciprocals.isDegenerateX(),
                this.min.x(),
                this.max.x(),
            ) &&
            updateAxis(
                origin.y(),
                reciprocals.getInverseY(),
                reciprocals.isDegenerateY(),
                this.min.y(),
                this.max.y(),
            ) &&
            updateAxis(
                origin.z(),
                reciprocals.getInverseZ(),
                reciprocals.isDegenerateZ(),
                this.min.z(),
                this.max.z(),
            ) &&
            tMax >= 0.0
        );
    }

    /** Slab test for ray parameters in [0, maxT]. */
    public intersectsRayBefore(ray: Ray, maxT: number): boolean {
        const origin = ray.getOrigin();
        const direction = ray.getDirection();
        let tMin = 0.0;
        let tMax = maxT;

        const updateAxis = (
            originCoord: number,
            directionCoord: number,
            minCoord: number,
            maxCoord: number,
        ): boolean => {
            if (directionCoord > -1e-12 && directionCoord < 1e-12) {
                return originCoord >= minCoord && originCoord <= maxCoord;
            }
            const invDir = 1.0 / directionCoord;
            let nearT = (minCoord - originCoord) * invDir;
            let farT = (maxCoord - originCoord) * invDir;
            if (nearT > farT) {
                const tmp = nearT;
                nearT = farT;
                farT = tmp;
            }
            tMin = nearT > tMin ? nearT : tMin;
            tMax = farT < tMax ? farT : tMax;
            return tMin <= tMax;
        };

        return (
            updateAxis(origin.x(), direction.x(), this.min.x(), this.max.x()) &&
            updateAxis(origin.y(), direction.y(), this.min.y(), this.max.y()) &&
            updateAxis(origin.z(), direction.z(), this.min.z(), this.max.z()) &&
            tMax >= 0.0
        );
    }

    public containsPointWithTolerance(point: Vector3Dd, tolerance: number): boolean {
        return (
            point.x() >= this.min.x() - tolerance &&
            point.x() <= this.max.x() + tolerance &&
            point.y() >= this.min.y() - tolerance &&
            point.y() <= this.max.y() + tolerance &&
            point.z() >= this.min.z() - tolerance &&
            point.z() <= this.max.z() + tolerance
        );
    }
}

import { FundamentalEntity } from "../../../common/FundamentalEntity.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { VSDK } from "../../../common/VSDK.js";
import type { GeometryRay } from "../Geometry.js";
export class Ray extends FundamentalEntity implements GeometryRay {
    private readonly origin: Vector3Dd;
    private readonly direction: Vector3Dd;
    private readonly t: number;
    public constructor();
    public constructor(origin: Vector3Dd, direction: Vector3Dd, t?: number);
    public constructor(ray: Ray);
    public constructor(a: Vector3Dd | Ray = new Vector3Dd(), b = new Vector3Dd(1, 0, 0), t = 0) {
        super();
        if (a instanceof Ray) {
            this.origin = a.origin;
            this.direction = a.direction;
            this.t = a.t;
        } else {
            this.origin = a;
            this.direction = b.dotProduct(b) <= VSDK.EPSILON ? b : b.normalized();
            this.t = t;
        }
    }
    public static copyOf(x: Ray) {
        if (x === null) throw new TypeError("Ray to copy cannot be null");
        return x;
    }
    public withOrigin(x: Vector3Dd) {
        return new Ray(x, this.direction, this.t);
    }
    public withDirection(x: Vector3Dd) {
        return new Ray(this.origin, x, this.t);
    }
    public withT(x: number) {
        return Object.is(x, this.t) ? this : new Ray(this.origin, this.direction, x);
    }
    public getOrigin() {
        return this.origin;
    }
    public getDirection() {
        return this.direction;
    }
    public getT() {
        return this.t;
    }
    public equals(x: unknown) {
        return (
            x instanceof Ray &&
            Object.is(this.t, x.t) &&
            this.origin.epsilonEquals(x.origin, 0) &&
            this.direction.epsilonEquals(x.direction, 0)
        );
    }
    public override toString() {
        return `Ray Origin: ${this.origin}; Direction: ${this.direction} T: ${VSDK.formatDouble(this.t)}`;
    }
}

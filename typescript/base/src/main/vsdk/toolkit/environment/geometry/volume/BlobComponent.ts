import { Double } from "../../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";

// Spherical field component of a BlobSolid: strength * (1 - d^2/radius^2)^2
// at distance d < radius from the center, and zero beyond. The density is
// evaluated as c0*d^4 + c1*d^2 + c2 with the PovRay coefficients.
export class BlobComponent {
    private readonly center: Vector3Dd;
    private readonly radius: number;
    private readonly strength: number;
    private readonly radiusSquared: number;
    private readonly coefficients: number[] = [0, 0, 0];

    public constructor();
    public constructor(center: Vector3Dd, radius: number, strength: number);
    public constructor(c?: Vector3Dd, r?: number, s?: number) {
        if (c === undefined || r === undefined || s === undefined) {
            c = new Vector3Dd(0, 0, 0);
            r = 1.0;
            s = 1.0;
        }
        this.center = c;
        this.radius = r;
        this.strength = s;
        this.radiusSquared = r * r;
        this.coefficients[2] = s;
        this.coefficients[1] = -(2.0 * s) / this.radiusSquared;
        this.coefficients[0] = s / (this.radiusSquared * this.radiusSquared);
    }

    public getCenter(): Vector3Dd {
        return this.center;
    }

    public getRadius(): number {
        return this.radius;
    }

    public getStrength(): number {
        return this.strength;
    }

    public getRadiusSquared(): number {
        return this.radiusSquared;
    }

    // c0, c1 and c2 of the density polynomial in d^2.
    public getCoefficients(): readonly number[] {
        return this.coefficients;
    }

    // PovRay rejects radii or strengths below 1e-5 in magnitude.
    public isDegenerate(): boolean {
        return (
            !Double.isFinite(this.radius) ||
            !Double.isFinite(this.strength) ||
            !Double.isFinite(this.center.x()) ||
            !Double.isFinite(this.center.y()) ||
            !Double.isFinite(this.center.z()) ||
            JavaMath.abs(this.strength) < 1.0e-5 ||
            this.radius < 1.0e-5
        );
    }
}
